// Filename: ServiceType.java
package com.nextque.model;

/**
 * Enum representing different types of government services.
 * These can also be managed in the database for more dynamic configuration.
 */
public enum ServiceType {
    NEW_APPLICATION("New Application"),
    RENEWAL("Renewal"),
    PAYMENT("Payment"),
    INQUIRY("Inquiry"),
    CLAIMS("Claims"),
    PERMITS("Permits"),
    COMPLAINTS("Complaints"), // Added new type
    INFORMATION("Information Desk"); // Added new type

    public static Iterable<ServiceType> getInitialServiceTypes() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    private final String displayName;
    // private final String name; // Enum 'name()' method provides the internal name

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    // public String getName() { return name(); } // Default enum method

    @Override
    public String toString() {
        return displayName;
    }
}
