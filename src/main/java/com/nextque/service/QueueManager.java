// Filename: QueueManager.java
package com.nextque.service;

import com.nextque.db.DatabaseManager;
import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final Map<ServiceType, Ticket> currentlyServingByService;
    private final Map<String, Ticket> currentlyServingByAgent;
    private final List<QueueUpdateListener> listeners; // General queue update listeners
    private FeedbackPromptListener feedbackListener; // Dedicated listener for feedback prompts
    private final DatabaseManager dbManager;

    public QueueManager(DatabaseManager dbManager) {
        if (dbManager == null) {
            LOGGER.error("DatabaseManager cannot be null. QueueManager initialization failed.");
            throw new IllegalArgumentException("DatabaseManager cannot be null for QueueManager");
        }
        this.dbManager = dbManager;
        this.serviceQueues = new HashMap<>();
        this.currentlyServingByService = new HashMap<>();
        this.currentlyServingByAgent = new HashMap<>();
        this.listeners = new LinkedList<>();
        // this.feedbackListener is initialized to null by default

        List<ServiceType> systemServiceTypes = dbManager.getAllServiceTypes();
        boolean schemaJustCreated = dbManager.isSchemaJustCreated();
        // Use ServiceType.values() to get all enum constants as the initial set
        ServiceType[] initialTypesFromEnum = ServiceType.values();

        if (systemServiceTypes.isEmpty() && schemaJustCreated && initialTypesFromEnum != null && initialTypesFromEnum.length > 0) {
            LOGGER.warn("No service types found in DB and schema was just created. Populating QueueManager with initial enum values from ServiceType.values().");
            systemServiceTypes = List.of(initialTypesFromEnum);
            // Optionally, ensure these initial types are in the DB if not already handled by schema creation
            // for (ServiceType initialType : systemServiceTypes) {
            //     dbManager.addServiceTypeIfNotExists(initialType.name(), initialType.getDisplayName()); // Assuming such a method
            // }
        } else if (systemServiceTypes.isEmpty()){
             LOGGER.warn("No service types configured in the database. QueueManager will operate without service queues initially.");
        }


        for (ServiceType type : systemServiceTypes) {
            serviceQueues.put(type, new PriorityQueue<>());
            currentlyServingByService.put(type, null);
        }
        LOGGER.info("QueueManager initialized for {} service types.", systemServiceTypes.size());
        // TODO: Load pending (WAITING) tickets from DB into memory queues on startup
        // This would involve querying DB for tickets with status WAITING and adding them to respective serviceQueues
    }

    public void addQueueUpdateListener(QueueUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void setFeedbackPromptListener(FeedbackPromptListener listener) {
        this.feedbackListener = listener;
        if (listener != null) {
            LOGGER.info("FeedbackPromptListener registered: {}", listener.getClass().getSimpleName());
        } else {
            LOGGER.warn("FeedbackPromptListener unregistered (set to null).");
        }
    }


    private void notifyListeners() {
        List<QueueUpdateListener> listenersCopy = new ArrayList<>(listeners); // Avoid ConcurrentModificationException
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

    public synchronized Ticket generateTicket(ServiceType serviceType, String customerName, Ticket.PriorityReason reason) {
        if (serviceType == null || customerName == null || customerName.trim().isEmpty() || reason == null) {
            LOGGER.error("Invalid parameters for generating ticket: serviceType={}, customerName='{}', reason={}", serviceType, customerName, reason);
            return null;
        }
        Ticket newTicket = new Ticket(serviceType, customerName.trim(), reason);

        serviceQueues.putIfAbsent(serviceType, new PriorityQueue<>()); // Ensure queue exists for the service type
        serviceQueues.get(serviceType).add(newTicket);

        dbManager.saveTicket(newTicket);
        LOGGER.info("Generated & Saved Ticket: {} for {} (Priority: {})",
                newTicket.getTicketNumber(), serviceType.getDisplayName(), reason.getDisplayName());
        notifyListeners();
        return newTicket;
    }

    public synchronized Ticket callNextTicket(ServiceType serviceType, User agent) {
        if (serviceType == null || agent == null || agent.getUsername() == null) {
            LOGGER.error("ServiceType or Agent (or agent username) cannot be null when calling next ticket. ServiceType: {}, Agent: {}", serviceType, agent);
            return null;
        }

        // Prevent an agent from calling a new ticket if they are already serving one
        if (currentlyServingByAgent.containsKey(agent.getUsername())) {
            Ticket existingTicket = currentlyServingByAgent.get(agent.getUsername());
            LOGGER.warn("Agent {} is already serving ticket {}. Cannot call a new ticket.", agent.getUsername(), existingTicket.getTicketNumber());
            // Optionally, return existingTicket or a custom error object/exception to UI
            return null; // Or throw new IllegalStateException("Agent already serving a ticket");
        }
        
        serviceQueues.putIfAbsent(serviceType, new PriorityQueue<>()); // Ensure queue exists
        PriorityQueue<Ticket> queue = serviceQueues.get(serviceType);

        if (queue.isEmpty()) { // Check after ensuring queue object exists
            LOGGER.info("Queue for {} is empty.", serviceType.getDisplayName());
            currentlyServingByService.put(serviceType, null);
            notifyListeners();
            return null;
        }

        Ticket nextTicket = queue.poll();
        if (nextTicket != null) { // Should not be null if queue was not empty, but good check
            nextTicket.setStatus(Ticket.TicketStatus.SERVING);
            nextTicket.setCallTime(LocalDateTime.now());
            nextTicket.setAgentUsername(agent.getUsername());

            currentlyServingByService.put(serviceType, nextTicket);
            currentlyServingByAgent.put(agent.getUsername(), nextTicket);

            dbManager.updateTicketStatus(nextTicket.getTicketNumber(), Ticket.TicketStatus.SERVING, agent.getUsername());
            dbManager.updateTicketTimes(nextTicket.getTicketNumber(), nextTicket.getCallTime(), null, null);
            LOGGER.info("{} calling next for {}: {} (Priority: {})",
                    agent.getUsername(), serviceType.getDisplayName(), nextTicket.getTicketNumber(), nextTicket.getPriorityReason().getDisplayName());
        } else {
             // This case should ideally not be reached if queue.isEmpty() was false.
             currentlyServingByService.put(serviceType, null);
        }
        notifyListeners();
        return nextTicket;
    }

    public synchronized void startService(String agentUsername) {
        if (agentUsername == null || agentUsername.trim().isEmpty()) {
            LOGGER.error("Agent username cannot be null or empty for starting service.");
            return;
        }
        Ticket ticket = currentlyServingByAgent.get(agentUsername);
        if (ticket != null && ticket.getStatus() == Ticket.TicketStatus.SERVING) {
            if (ticket.getServiceStartTime() == null) {
                ticket.setServiceStartTime(LocalDateTime.now());
                dbManager.updateTicketTimes(ticket.getTicketNumber(), null, ticket.getServiceStartTime(), null);
                LOGGER.info("Service started for ticket {} by agent {}", ticket.getTicketNumber(), agentUsername);
                notifyListeners();
            } else {
                LOGGER.warn("Attempted to re-start service for ticket {} which already started.", ticket.getTicketNumber());
            }
        } else {
            LOGGER.warn("No ticket found for agent {} to start service, or ticket not in SERVING state.", agentUsername);
        }
    }

    public synchronized void completeService(String agentUsername) {
        if (agentUsername == null || agentUsername.trim().isEmpty()) {
            LOGGER.error("Agent username cannot be null or empty for completing service.");
            return;
        }
        Ticket ticket = currentlyServingByAgent.get(agentUsername);
        if (ticket != null && ticket.getStatus() == Ticket.TicketStatus.SERVING) {
            ticket.setServiceEndTime(LocalDateTime.now());
            ticket.setStatus(Ticket.TicketStatus.COMPLETED);

            if (ticket.getServiceStartTime() == null) {
                 ticket.setServiceStartTime(ticket.getCallTime() != null ? ticket.getCallTime() : ticket.getServiceEndTime().minusSeconds(1));
                 LOGGER.warn("Service start time was not set for ticket {}, approximating based on call/completion time.", ticket.getTicketNumber());
            }
            // Update DB for status and all relevant times
            dbManager.updateTicketStatus(ticket.getTicketNumber(), Ticket.TicketStatus.COMPLETED, agentUsername);
            dbManager.updateTicketTimes(ticket.getTicketNumber(), ticket.getCallTime(), ticket.getServiceStartTime(), ticket.getServiceEndTime());


            currentlyServingByService.put(ticket.getServiceType(), null);
            currentlyServingByAgent.remove(agentUsername);

            LOGGER.info("Service completed for {} by {}", ticket.getTicketNumber(), agentUsername);
            notifyListeners();
            promptForFeedback(ticket.getTicketNumber());
        } else {
            LOGGER.warn("No ticket found for agent {} to complete service, or ticket not in SERVING state.", agentUsername);
        }
    }

    public synchronized boolean updateTicketPriority(String ticketNumber, Ticket.PriorityReason newReason) {
        if (ticketNumber == null || ticketNumber.trim().isEmpty() || newReason == null) {
            LOGGER.error("Invalid parameters for updating ticket priority: ticketNumber='{}', newReason={}", ticketNumber, newReason);
            return false;
        }
        Ticket foundTicket = null;
        ServiceType originalServiceType = null;

        for (Map.Entry<ServiceType, PriorityQueue<Ticket>> entry : serviceQueues.entrySet()) {
            Optional<Ticket> ticketOpt = entry.getValue().stream()
                    .filter(t -> t.getTicketNumber().equals(ticketNumber) && t.getStatus() == Ticket.TicketStatus.WAITING)
                    .findFirst();
            if (ticketOpt.isPresent()) {
                foundTicket = ticketOpt.get();
                originalServiceType = entry.getKey();
                break;
            }
        }

        if (foundTicket != null && originalServiceType != null) {
            PriorityQueue<Ticket> queue = serviceQueues.get(originalServiceType);
            if (queue == null || !queue.remove(foundTicket)) { // Ensure queue exists and removal was successful
                 LOGGER.error("Failed to remove ticket {} from in-memory queue for priority update. Queue for {} might be missing or ticket not found directly.", ticketNumber, originalServiceType);
                 return false;
            }
            Ticket.PriorityReason oldReason = foundTicket.getPriorityReason();
            foundTicket.setPriorityReason(newReason); // This also updates numerical priority in Ticket model

            if (dbManager.updateTicketPriority(ticketNumber, newReason)) {
                queue.add(foundTicket); // Re-add to queue with new priority
                LOGGER.info("Priority for ticket {} updated to {} by admin/agent.", ticketNumber, newReason.getDisplayName());
                notifyListeners();
                return true;
            } else {
                LOGGER.error("Failed to update priority for ticket {} in database. Rolling back local changes.", ticketNumber);
                foundTicket.setPriorityReason(oldReason); // Revert to old priority
                queue.add(foundTicket); // Re-add with old priority
                notifyListeners(); // Notify listeners that an attempted update (which might have briefly changed order) is reverted
                return false;
            }
        }
        LOGGER.warn("Ticket {} not found in waiting queues or cannot be updated for priority change.", ticketNumber);
        return false;
    }

    private void promptForFeedback(String ticketNumber) {
        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            LOGGER.warn("Cannot prompt for feedback with null or empty ticket number.");
            return;
        }
        LOGGER.debug("Attempting to prompt for feedback for ticket {}.", ticketNumber);
        if (this.feedbackListener != null) {
            try {
                this.feedbackListener.onServiceCompletedForFeedback(ticketNumber);
            } catch (Exception e) {
                LOGGER.error("Error prompting feedback listener for ticket {}: {}", ticketNumber, e.getMessage(), e);
            }
        } else {
            LOGGER.warn("No FeedbackPromptListener registered. Cannot prompt for feedback for ticket {}.", ticketNumber);
        }
    }

    public synchronized Ticket getCurrentlyServing(ServiceType serviceType) {
        if (serviceType == null) return null;
        return currentlyServingByService.get(serviceType);
    }

    public synchronized Ticket getTicketBeingServedByAgent(String agentUsername) {
        if (agentUsername == null || agentUsername.trim().isEmpty()) return null;
        return currentlyServingByAgent.get(agentUsername);
    }

    public synchronized List<Ticket> getQueueSnapshot(ServiceType serviceType) {
        if (serviceType == null) return new ArrayList<>();
        PriorityQueue<Ticket> queue = serviceQueues.get(serviceType);
        if (queue == null) {
            return new ArrayList<>();
        }
        List<Ticket> snapshot = new ArrayList<>(queue);
        snapshot.sort(null); // Uses Ticket's compareTo method for consistent ordering
        return snapshot;
    }

    public synchronized int getWaitingCount(ServiceType serviceType) {
        if (serviceType == null) return 0;
        Queue<Ticket> queue = serviceQueues.get(serviceType);
        return queue != null ? queue.size() : 0;
    }

    public synchronized int getTotalWaitingCount() {
        return serviceQueues.values().stream().mapToInt(Queue::size).sum();
    }

    public List<ServiceType> getAvailableServiceTypes() {
        List<ServiceType> dbTypes = dbManager.getAllServiceTypes();
        // Use ServiceType.values() to get all enum constants as the initial set
        ServiceType[] initialTypesArray = ServiceType.values(); 

        if(dbTypes.isEmpty() && dbManager.isSchemaJustCreated() && initialTypesArray != null && initialTypesArray.length > 0){
            LOGGER.info("getAvailableServiceTypes: DB empty and schema just created, returning initial enum types as defined in ServiceType.values().");
            return List.of(initialTypesArray);
        }
        return dbTypes; // Primary source of truth is the database
    }

    public void onQueueUpdated() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // Interface for general queue updates
    public interface QueueUpdateListener {
        void onQueueUpdated();
    }

    // Interface for feedback prompts
    public interface FeedbackPromptListener {
        void onServiceCompletedForFeedback(String ticketNumber);
    }
}
