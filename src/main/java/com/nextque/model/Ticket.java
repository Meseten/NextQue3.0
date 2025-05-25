// Filename: Ticket.java
package com.nextque.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class Ticket implements Comparable<Ticket> {

    public enum TicketStatus {
        WAITING, SERVING, COMPLETED, CANCELLED
    }

    public enum PriorityReason {
        NONE("Regular"),
        SENIOR_CITIZEN("Senior Citizen"),
        PWD("Person with Disability"),
        PREGNANT("Pregnant Woman");

        private final String displayName;
        PriorityReason(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    private final String ticketNumber;
    private final ServiceType serviceType;
    private final LocalDateTime issueTime;
    private String customerName;
    private int priority; // Actual numerical priority for sorting
    private PriorityReason priorityReason; // User-friendly reason
    private TicketStatus status;
    
    private LocalDateTime callTime;
    private LocalDateTime serviceStartTime;
    private LocalDateTime serviceEndTime;
    private String agentUsername; 

    private static final AtomicInteger generalCounter = new AtomicInteger(0);

    // Constructor for new tickets
    public Ticket(ServiceType serviceType, String customerName, PriorityReason reason) {
        String servicePrefix = serviceType.name().substring(0, Math.min(serviceType.name().length(), 3)).toUpperCase();
        this.ticketNumber = String.format("%s-%03d", servicePrefix, generalCounter.incrementAndGet());
        
        this.serviceType = serviceType;
        this.customerName = (customerName == null || customerName.trim().isEmpty()) ? "N/A" : customerName;
        this.issueTime = LocalDateTime.now();
        this.priorityReason = (reason == null) ? PriorityReason.NONE : reason;
        this.priority = calculateNumericalPriority(this.priorityReason); // Calculate numerical priority
        this.status = TicketStatus.WAITING;
    }
    
    // Constructor for loading from DB (or when ticket number is predefined)
    public Ticket(String ticketNumberFromDB, ServiceType serviceType, String customerName, LocalDateTime issueTime, PriorityReason reason, int numericalPriority) {
        this.ticketNumber = ticketNumberFromDB; 
        this.serviceType = serviceType;
        this.customerName = (customerName == null || customerName.trim().isEmpty()) ? "N/A" : customerName;
        this.issueTime = issueTime;
        this.priorityReason = (reason == null) ? PriorityReason.NONE : reason;
        this.priority = numericalPriority; // Use stored numerical priority
        this.status = TicketStatus.WAITING; 
    }

    private int calculateNumericalPriority(PriorityReason reason) {
        if (reason == null || reason == PriorityReason.NONE) {
            return 0; // Normal priority
        }
        return 10; // Higher value for priority customers
    }

    public String getTicketNumber() { return ticketNumber; }
    public ServiceType getServiceType() { return serviceType; }
    public LocalDateTime getIssueTime() { return issueTime; }
    public String getFormattedIssueTime() { return issueTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")); }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public int getPriority() { return priority; } // Numerical priority
    public void setPriority(int priority) { this.priority = priority; } // Allow admin to override numerical

    public PriorityReason getPriorityReason() { return priorityReason; }
    public void setPriorityReason(PriorityReason priorityReason) {
        this.priorityReason = (priorityReason == null) ? PriorityReason.NONE : priorityReason;
        this.priority = calculateNumericalPriority(this.priorityReason); // Recalculate numerical priority
    }
    
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public LocalDateTime getCallTime() { return callTime; }
    public void setCallTime(LocalDateTime callTime) { this.callTime = callTime; }
    public LocalDateTime getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(LocalDateTime serviceStartTime) { this.serviceStartTime = serviceStartTime; }
    public LocalDateTime getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(LocalDateTime serviceEndTime) { this.serviceEndTime = serviceEndTime; }
    public String getAgentUsername() { return agentUsername; }
    public void setAgentUsername(String agentUsername) { this.agentUsername = agentUsername; }
    
    public String getFormattedTime(LocalDateTime dateTime) {
        if (dateTime == null) return "---";
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        return ticketNumber + " (" + serviceType.getDisplayName() + 
               (priorityReason != PriorityReason.NONE ? " - " + priorityReason.getDisplayName() : "") +
               ", Status: " + status + ")";
    }

    // Higher numerical priority means it comes earlier in the queue
    @Override
    public int compareTo(Ticket other) {
        int priorityCompare = Integer.compare(other.priority, this.priority); // Higher number = higher priority
        if (priorityCompare != 0) return priorityCompare;
        return this.issueTime.compareTo(other.issueTime); // If same priority, older ticket first
    }
}
