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
            if (user.getPassword().equals(password)) {
                this.currentUser = user;
                return true;
            }
        }
        this.currentUser = null;
        return false;
    }

    public boolean signUp(User newUser) {
        if (dbManager.getUser(newUser.getUsername()).isPresent()) {
            return false;
        }
        try {
            dbManager.addUser(newUser);
            return true;
        } catch (Exception e) {
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
