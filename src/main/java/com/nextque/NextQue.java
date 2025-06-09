package com.nextque;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.nextque.auth.AuthService;
import com.nextque.db.DatabaseManager;
import com.nextque.service.QueueManager;
import com.nextque.ui.LoginDialog;
import com.nextque.ui.MainWindow;
import com.nextque.ui.UITheme;

import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextQue {
    private static final Logger LOGGER = LoggerFactory.getLogger(NextQue.class);

    public static void main(String[] args) {
        LOGGER.info("Starting NextQue Application...");

        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            UITheme.applyGlobalStyles();
        } catch (UnsupportedLookAndFeelException e) {
            LOGGER.error("Failed to initialize FlatLaf theme: {}", e.getMessage(), e);
        }

        SwingUtilities.invokeLater(() -> {
            DatabaseManager dbManager = new DatabaseManager();
            AuthService authService = new AuthService(dbManager);
            QueueManager queueManager = new QueueManager(dbManager);

            LoginDialog loginDialog = new LoginDialog(null, authService);
            loginDialog.setVisible(true);

            if (loginDialog.isAuthenticated()) {
                LOGGER.info("User {} authenticated successfully with role {}.",
                        authService.getCurrentUser().getUsername(),
                        authService.getCurrentUser().getRole());
                MainWindow mainWindow = new MainWindow(queueManager, authService, dbManager);
                mainWindow.display();
            } else {
                LOGGER.info("Login cancelled or failed. Exiting application.");
                System.exit(0);
            }
        });
    }
}
