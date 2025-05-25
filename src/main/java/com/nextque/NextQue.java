// Filename: NextQue.java
package com.nextque;

import com.formdev.flatlaf.FlatIntelliJLaf;
// import com.formdev.flatlaf.themes.FlatMacLightLaf; // Alternative theme
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
        // Configure SLF4J SimpleLogger (optional, for more detailed logs)
        // System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        // System.setProperty(org.slf4j.simple.SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        // System.setProperty(org.slf4j.simple.SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss:SSS");
        // System.setProperty(org.slf4j.simple.SimpleLogger.LOG_FILE_KEY, "NextQue.log");


        LOGGER.info("Starting NextQue Application...");

        // Set custom FlatLaf properties before setting L&F
        // System.setProperty( "flatlaf.useWindowDecorations", "true" ); // For custom window decorations
        // System.setProperty( "flatlaf.menuBarEmbedded", "true" ); // For macOS style embedded menu bar

        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            // UIManager.setLookAndFeel(new FlatMacLightLaf()); // Alternative
            UITheme.applyGlobalStyles(); 
        } catch (UnsupportedLookAndFeelException e) {
            LOGGER.error("Failed to initialize FlatLaf theme: {}", e.getMessage(), e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.error("Failed to set system Look and Feel: {}", ex.getMessage(), ex);
            }
        }

        SwingUtilities.invokeLater(() -> {
            LOGGER.debug("Initializing DatabaseManager...");
            DatabaseManager dbManager = new DatabaseManager();
            LOGGER.debug("Initializing AuthService...");
            AuthService authService = new AuthService(dbManager);
            LOGGER.debug("Initializing QueueManager...");
            QueueManager queueManager = new QueueManager(dbManager);

            LOGGER.info("Displaying Login Dialog...");
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
