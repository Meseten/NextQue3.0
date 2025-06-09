package com.nextque.service;

import com.nextque.db.DatabaseManager;
import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueManager.class);
    private final Map<ServiceType, PriorityQueue<Ticket>> serviceQueues;
    private final Map<String, Ticket> currentlyServingByAgent;
    private final List<QueueUpdateListener> listeners;
    private FeedbackPromptListener feedbackListener;
    private final DatabaseManager dbManager;

    public QueueManager(DatabaseManager dbManager) {
        if (dbManager == null) {
            throw new IllegalArgumentException("DatabaseManager cannot be null");
        }
        this.dbManager = dbManager;
        this.serviceQueues = new HashMap<>();
        this.currentlyServingByAgent = new HashMap<>();
        this.listeners = new LinkedList<>();

        int lastTicketNumber = dbManager.getHighestTicketNumberSuffix();
        Ticket.initializeCounter(lastTicketNumber);

        loadServicesAndTickets();
    }
    
    private void loadServicesAndTickets() {
        serviceQueues.clear();
        List<ServiceType> systemServiceTypes = dbManager.getAllServiceTypes();

        if (systemServiceTypes.isEmpty()){
             LOGGER.warn("No service types are configured in the database.");
        }

        for (ServiceType type : systemServiceTypes) {
            serviceQueues.put(type, new PriorityQueue<>());
        }
        LOGGER.info("QueueManager initialized for {} service types.", serviceQueues.size());
        
        List<Ticket> allTickets = dbManager.getAllTicketsWithResolvedServiceTypes();
        int pendingCount = 0;
        for (Ticket ticket : allTickets) {
            if (ticket.getStatus() == Ticket.TicketStatus.WAITING) {
                PriorityQueue<Ticket> queue = serviceQueues.get(ticket.getServiceType());
                if (queue != null) {
                    queue.add(ticket);
                    pendingCount++;
                } else {
                    LOGGER.warn("Found WAITING ticket {} for an inactive service type '{}'.", ticket.getTicketNumber(), ticket.getServiceType().getName());
                }
            }
        }
        LOGGER.info("Loaded {} pending tickets into active queues.", pendingCount);
    }

    public void addQueueUpdateListener(QueueUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void setFeedbackPromptListener(FeedbackPromptListener listener) {
        this.feedbackListener = listener;
    }

    public void notifyListeners() {
        List<QueueUpdateListener> listenersCopy = new ArrayList<>(listeners);
        for (QueueUpdateListener listener : listenersCopy) {
            if (listener != null) {
                try {
                    listener.onQueueUpdated();
                } catch (Exception e) {
                    LOGGER.error("Error notifying listener {}: {}", listener.getClass().getName(), e.getMessage(), e);
                }
            }
        }
    }
    
    public synchronized void servicesConfigurationChanged() {
        LOGGER.info("Service configuration has changed. Reloading services and notifying all listeners.");
        loadServicesAndTickets();
        notifyListeners();
    }

    public synchronized Ticket generateTicket(ServiceType serviceType, String customerName, Ticket.PriorityReason reason) {
        if (serviceType == null || reason == null) {
            return null;
        }
        Ticket newTicket = new Ticket(serviceType, customerName, reason);

        serviceQueues.computeIfAbsent(serviceType, k -> new PriorityQueue<>()).add(newTicket);

        dbManager.saveTicket(newTicket);
        LOGGER.info("Generated & Saved Ticket: {} for {}", newTicket.getTicketNumber(), serviceType.getDisplayName());
        notifyListeners();
        return newTicket;
    }

    public synchronized Ticket callNextTicket(ServiceType serviceType, User agent) {
        if (serviceType == null || agent == null || agent.getUsername() == null) {
            return null;
        }

        if (currentlyServingByAgent.containsKey(agent.getUsername())) {
            return null;
        }

        PriorityQueue<Ticket> queue = serviceQueues.get(serviceType);

        if (queue == null || queue.isEmpty()) {
            notifyListeners();
            return null;
        }

        Ticket nextTicket = queue.poll();
        nextTicket.setStatus(Ticket.TicketStatus.SERVING);
        nextTicket.setCallTime(LocalDateTime.now());
        nextTicket.setAgentUsername(agent.getUsername());

        currentlyServingByAgent.put(agent.getUsername(), nextTicket);

        dbManager.updateTicketStatus(nextTicket.getTicketNumber(), Ticket.TicketStatus.SERVING, agent.getUsername());
        dbManager.updateTicketTimes(nextTicket.getTicketNumber(), nextTicket.getCallTime(), null, null);
        
        notifyListeners();
        return nextTicket;
    }

    public synchronized void startService(String agentUsername) {
        if (agentUsername == null || agentUsername.trim().isEmpty()) {
            return;
        }
        Ticket ticket = currentlyServingByAgent.get(agentUsername);
        if (ticket != null && ticket.getStatus() == Ticket.TicketStatus.SERVING) {
            if (ticket.getServiceStartTime() == null) {
                ticket.setServiceStartTime(LocalDateTime.now());
                dbManager.updateTicketTimes(ticket.getTicketNumber(), null, ticket.getServiceStartTime(), null);
                notifyListeners();
            }
        }
    }

    public synchronized void completeService(String agentUsername) {
        if (agentUsername == null || agentUsername.trim().isEmpty()) {
            return;
        }
        Ticket ticket = currentlyServingByAgent.get(agentUsername);
        if (ticket != null && ticket.getStatus() == Ticket.TicketStatus.SERVING) {
            ticket.setServiceEndTime(LocalDateTime.now());
            ticket.setStatus(Ticket.TicketStatus.COMPLETED);

            if (ticket.getServiceStartTime() == null) {
                 ticket.setServiceStartTime(ticket.getCallTime() != null ? ticket.getCallTime() : ticket.getServiceEndTime().minusSeconds(1));
            }

            dbManager.updateTicketStatus(ticket.getTicketNumber(), Ticket.TicketStatus.COMPLETED, agentUsername);
            dbManager.updateTicketTimes(ticket.getTicketNumber(), ticket.getCallTime(), ticket.getServiceStartTime(), ticket.getServiceEndTime());

            currentlyServingByAgent.remove(agentUsername);

            notifyListeners();
            promptForFeedback(ticket.getTicketNumber());
        }
    }

    public synchronized boolean updateTicketPriority(String ticketNumber, Ticket.PriorityReason newReason) {
        if (ticketNumber == null || newReason == null) {
            return false;
        }
        Optional<Ticket> foundTicketOpt = serviceQueues.values().stream()
            .flatMap(Queue::stream)
            .filter(t -> t.getTicketNumber().equals(ticketNumber) && t.getStatus() == Ticket.TicketStatus.WAITING)
            .findFirst();

        if (foundTicketOpt.isPresent()) {
            Ticket foundTicket = foundTicketOpt.get();
            PriorityQueue<Ticket> queue = serviceQueues.get(foundTicket.getServiceType());
            
            if (queue != null && queue.remove(foundTicket)) {
                foundTicket.setPriorityReason(newReason);
                if (dbManager.updateTicketPriority(ticketNumber, newReason)) {
                    queue.add(foundTicket);
                    notifyListeners();
                    return true;
                }
            }
        }
        return false;
    }

    private void promptForFeedback(String ticketNumber) {
        if (ticketNumber != null && !ticketNumber.trim().isEmpty() && this.feedbackListener != null) {
            try {
                this.feedbackListener.onServiceCompletedForFeedback(ticketNumber);
            } catch (Exception e) {
                LOGGER.error("Error prompting feedback listener for ticket {}: {}", ticketNumber, e.getMessage(), e);
            }
        }
    }

    public synchronized Ticket getCurrentlyServing(ServiceType serviceType) {
        if (serviceType == null) return null;
        return currentlyServingByAgent.values().stream()
            .filter(ticket -> serviceType.equals(ticket.getServiceType()) && ticket.getStatus() == Ticket.TicketStatus.SERVING)
            .findFirst()
            .orElse(null);
    }

    public synchronized Ticket getTicketBeingServedByAgent(String agentUsername) {
        if (agentUsername == null || agentUsername.trim().isEmpty()) return null;
        return currentlyServingByAgent.get(agentUsername);
    }

    public synchronized List<Ticket> getQueueSnapshot(ServiceType serviceType) {
        if (serviceType == null) return new ArrayList<>();
        Queue<Ticket> queue = serviceQueues.get(serviceType);
        if (queue == null) return new ArrayList<>();
        
        return queue.stream().sorted().collect(Collectors.toList());
    }

    public synchronized int getWaitingCount(ServiceType serviceType) {
        if (serviceType == null) return 0;
        Queue<Ticket> queue = serviceQueues.get(serviceType);
        return queue != null ? queue.size() : 0;
    }

    public synchronized int getTotalWaitingCount() {
        return serviceQueues.values().stream().mapToInt(Queue::size).sum();
    }

    public synchronized List<ServiceType> getAvailableServiceTypes() {
        return serviceQueues.keySet().stream()
            .sorted(Comparator.comparing(ServiceType::getDisplayName))
            .collect(Collectors.toList());
    }

    public interface QueueUpdateListener {
        void onQueueUpdated();
    }

    public interface FeedbackPromptListener {
        void onServiceCompletedForFeedback(String ticketNumber);
    }
}
