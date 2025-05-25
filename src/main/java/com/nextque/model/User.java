// Filename: User.java
package com.nextque.model;

public class User {
    private String username;
    // This field will store what DatabaseManager expects to be the "hashed" password.
    // For now, it will be the plain password due to getHashedPassword() implementation.
    private String passwordData; // Renamed to avoid confusion with plain password concept
    private UserRole role;
    private String fullName;

    /**
     * Constructor.
     * @param username The username.
     * @param plainPassword The plain text password. This will be "hashed" (or stored as is for now).
     * @param role The user's role.
     * @param fullName The user's full name.
     */
    public User(String username, String plainPassword, UserRole role, String fullName) {
        this.username = username;
        // TODO: Implement proper, secure password hashing (e.g., BCrypt, SCrypt, Argon2).
        // For now, we are storing the provided password directly.
        // In a real application, you would hash plainPassword here:
        // this.passwordData = MyHashingLibrary.hash(plainPassword);
        this.passwordData = plainPassword; // TEMPORARY: Storing plain text as "passwordData"
        this.role = role;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Returns the password data stored for this user.
     * IMPORTANT: In its current state for debugging, this returns the plain password.
     * In a secure system, this would return the HASHED password.
     * This method is used by AuthService for login comparison (where the input password would be hashed first).
     */
    public String getPassword() {
        return passwordData;
    }

    public UserRole getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + " - " + role + ")";
    }

    /**
     * Returns the password data that should be stored in the database.
     * IMPORTANT: This method currently returns the plain password due to the temporary
     * implementation for getting the application running.
     * This MUST be changed to return a securely hashed password in a production system.
     */
    public String getHashedPassword() {
        // TODO: Ensure this returns a securely hashed password.
        // For now, returning the stored password data directly as DatabaseManager expects.
        return this.passwordData; // This fulfills the DatabaseManager's expectation for now.
        // throw new UnsupportedOperationException("Not supported yet."); // Original line
    }
}
