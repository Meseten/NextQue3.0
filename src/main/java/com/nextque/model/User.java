package com.nextque.model;

public class User {
    private String username;
    private String passwordData;
    private UserRole role;
    private String fullName;

    
    public User(String username, String plainPassword, UserRole role, String fullName) {
        this.username = username;
        this.passwordData = plainPassword;
        this.role = role;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }
    
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
    
    public String getHashedPassword() {
        return this.passwordData;
    }
}
