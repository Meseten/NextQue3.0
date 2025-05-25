// Filename: AuthService.java
package com.nextque.auth;

import com.nextque.db.DatabaseManager;
import com.nextque.model.User;
import com.nextque.model.UserRole; 

import java.util.Optional;

public class AuthService {
    private final DatabaseManager dbManager;
    private User currentUser;

    public AuthService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean login(String username, String password) {
        Optional<User> userOpt = dbManager.getUser(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // IMPORTANT: In a real application, compare hashed passwords!
            if (user.getPassword().equals(password)) { // Simplified check
                this.currentUser = user;
                return true;
            }
        }
        this.currentUser = null;
        return false;
    }

    public boolean signUp(User newUser) {
        // In a real application, HASH the newUser.getPassword() before saving.
        // For example: newUser.setPassword(PasswordHasher.hash(newUser.getPassword()));
        // Ensure username is not taken (can be checked here or rely on DB unique constraint)
        if (dbManager.getUser(newUser.getUsername()).isPresent()) {
            System.err.println("Signup failed: Username already exists.");
            return false; // Username taken
        }
        try {
            dbManager.addUser(newUser);
            return true;
        } catch (Exception e) {
            System.err.println("Error during signup: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isUsernameTaken(String username) {
        return dbManager.getUser(username).isPresent();
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }
    
    public boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}
