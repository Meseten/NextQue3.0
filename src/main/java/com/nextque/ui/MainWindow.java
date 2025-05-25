// Filename: MainWindow.java
package com.nextque.ui;

import com.nextque.NextQue;
import com.nextque.auth.AuthService;
import com.nextque.db.DatabaseManager;
import com.nextque.model.UserRole;
import com.nextque.service.QueueManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent; // For KeyStroke
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.border.EmptyBorder;

public class MainWindow extends JFrame implements QueueManager.QueueUpdateListener, QueueManager.FeedbackPromptListener {
    private final QueueManager queueManager;
    private final AuthService authService;
    private final DatabaseManager dbManager;

    private JTabbedPane tabbedPane;
    private CustomerPanel customerPanel;
    private AgentPanel agentPanel;
    private DisplayPanel displayPanel;
    private DashboardPanel dashboardPanel;
    private AdminPanel adminPanel;
    private FeedbackPanel feedbackPanel;

    public MainWindow(QueueManager queueManager, AuthService authService, DatabaseManager dbManager) {
        this.queueManager = queueManager;
        this.authService = authService;
        this.dbManager = dbManager;

        // Register MainWindow as a listener for general queue updates AND feedback prompts
        this.queueManager.addQueueUpdateListener(this);
        this.queueManager.setFeedbackPromptListener(this); // Make sure this method exists in QueueManager

        setTitle("NextQue - Queuing Management System"); // Slightly more descriptive title
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performLogout();
            }
        });
        setSize(1366, 768); // Common resolution
        setMinimumSize(new Dimension(1024, 700)); // Set a reasonable minimum size
        setLocationRelativeTo(null); // Center on screen

        // Apply main background to the content pane
        getContentPane().setBackground(UITheme.COLOR_BACKGROUND_MAIN);

        initComponents();
        layoutComponents(); // This will add the mainContentPanel to the JFrame
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setFont(UITheme.FONT_GENERAL_BOLD); // Use theme font for tabs
        // Optional FlatLaf styling for tabbed pane:
        // tabbedPane.putClientProperty( "JTabbedPane.tabHeight", 38 );
        // tabbedPane.putClientProperty( "JTabbedPane.showTabSeparators", true );
        // tabbedPane.putClientProperty( "JTabbedPane.hasFullBorder", true );
        // tabbedPane.putClientProperty( "JTabbedPane.tabAreaAlignment", "fill" ); // Example
        // tabbedPane.putClientProperty( "JTabbedPane.tabAlignment", SwingConstants.LEADING ); // Example


        UserRole currentRole = authService.getCurrentUser().getRole();

        // Icons from UITheme
        Icon customerIcon = UITheme.getIcon("kiosk.svg");               // Ensure icon exists
        Icon displayIcon = UITheme.getIcon("display_screen.svg");       // Ensure icon exists
        Icon feedbackIcon = UITheme.getIcon("feedback_bubbles.svg");    // Ensure icon exists
        Icon agentIcon = UITheme.getIcon("agent_headset.svg");          // Ensure icon exists
        Icon dashboardIcon = UITheme.getIcon("dashboard_chart.svg");    // Ensure icon exists
        Icon adminIcon = UITheme.getIcon("admin_settings.svg");         // Ensure icon exists


        // Initialize panels based on role
        // Customer Kiosk: Accessible to all authenticated users for now (or specific roles)
        // The original error log pointed to CustomerPanel constructor (CustomerPanel.java:33)
        // and this line (MainWindow.java:77 in original, now adjusted)
        customerPanel = new CustomerPanel(queueManager, this); // 'this' is the FeedbackPromptListener
        tabbedPane.addTab("Customer Kiosk", customerIcon, customerPanel, "Access customer ticket services");

        displayPanel = new DisplayPanel(queueManager);
        tabbedPane.addTab("Public Display", displayIcon, displayPanel, "View current queue status");

        feedbackPanel = new FeedbackPanel(dbManager);
        tabbedPane.addTab("Provide Feedback", feedbackIcon, feedbackPanel, "Submit feedback for services");
        // Initially, feedback tab might be disabled or hidden until a service is completed.
        // For simplicity, it's always visible here. Logic to enable/show it can be added.

        if (currentRole == UserRole.AGENT || currentRole == UserRole.ADMIN) {
            agentPanel = new AgentPanel(queueManager, authService);
            tabbedPane.addTab("Agent Desk", agentIcon, agentPanel, "Manage queues and serve tickets");
        }

        if (currentRole == UserRole.ADMIN) {
            dashboardPanel = new DashboardPanel(queueManager);
            tabbedPane.addTab("Dashboard", dashboardIcon, dashboardPanel, "View queue analytics");

            adminPanel = new AdminPanel(dbManager, queueManager);
            tabbedPane.addTab("Admin Console", adminIcon, adminPanel, "System administration");
        }

        // Set default selected tab based on role
        if (currentRole == UserRole.CUSTOMER) {
            if (customerPanel != null) tabbedPane.setSelectedComponent(customerPanel);
        } else if (currentRole == UserRole.AGENT) {
            if (agentPanel != null) tabbedPane.setSelectedComponent(agentPanel);
        } else if (currentRole == UserRole.ADMIN) {
            if (dashboardPanel != null) tabbedPane.setSelectedComponent(dashboardPanel); // Default to dashboard for admin
            else if (adminPanel != null) tabbedPane.setSelectedComponent(adminPanel);
        } else {
            // Fallback or if a role has no specific default panel (e.g. just display)
            if (displayPanel != null) tabbedPane.setSelectedComponent(displayPanel);
        }
    }

    private void performLogout() {
        // Use themed JOptionPane
        UIManager.put("OptionPane.messageFont", UITheme.FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", UITheme.FONT_BUTTON);

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout and exit NextQue?",
                "Logout Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UITheme.getIcon("logout_question.svg", 32,32)); // Ensure icon exists

        if (confirmation == JOptionPane.YES_OPTION) {
            authService.logout();
            System.out.println("User logged out. Exiting application.");
            dispose();
            // Restarting the application by calling NextQue.main(null) from here can be problematic
            // and might lead to issues with resource cleanup or multiple application instances.
            // It's generally better to exit and let the user restart manually if needed.
            // For a clean restart, the application should fully terminate.
            System.exit(0); // Cleanly exit the application
            // NextQue.main(null); // Avoid this if possible
        }
    }

    private void layoutComponents() {
        JMenuBar menuBar = new JMenuBar();
        // Optional: Style the menu bar itself
        // menuBar.setBackground(UITheme.COLOR_PRIMARY_NAVY);
        // menuBar.setBorder(BorderFactory.createEmptyBorder());

        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(UITheme.FONT_GENERAL_BOLD);
        // fileMenu.setForeground(UITheme.COLOR_TEXT_ON_PRIMARY); // If menu bar has dark background
        fileMenu.setMnemonic(KeyEvent.VK_F); // Use KeyEvent constants

        JMenuItem logoutItem = new JMenuItem("Logout & Exit");
        logoutItem.setFont(UITheme.FONT_GENERAL_REGULAR);
        logoutItem.setIcon(UITheme.getIcon("logout.svg")); // Ensure icon exists
        logoutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())); // Changed shortcut
        logoutItem.addActionListener(e -> performLogout());

        fileMenu.add(logoutItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Header panel for user info
        JPanel topHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topHeaderPanel.setBackground(UITheme.COLOR_PRIMARY_NAVY); // Corrected from COLOR_BACKGROUND_DARK
        topHeaderPanel.setBorder(new EmptyBorder(5,10,5,10)); // Padding for the header
        JLabel userInfoLabel = new JLabel("User: " + authService.getCurrentUser().getFullName() + " (" + authService.getCurrentUser().getRole() + ")");
        userInfoLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        userInfoLabel.setForeground(UITheme.COLOR_TEXT_ON_PRIMARY); // Text color for dark background
        topHeaderPanel.add(userInfoLabel);

        // Main content panel that holds the header and tabbed pane
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(UITheme.COLOR_BACKGROUND_MAIN); // Match JFrame content pane
        mainContentPanel.add(topHeaderPanel, BorderLayout.NORTH);
        mainContentPanel.add(tabbedPane, BorderLayout.CENTER);

        // Add the main content panel to the JFrame's content pane
        // getContentPane().add(mainContentPanel, BorderLayout.CENTER); // This is implicit if using `add()` on JFrame
        add(mainContentPanel, BorderLayout.CENTER);
    }

    public void display() {
        setVisible(true);
    }

    @Override
    public void onQueueUpdated() {
        // This MainWindow listener for QueueManager.QueueUpdateListener might not be strictly necessary
        // if individual panels (AgentPanel, DisplayPanel, DashboardPanel, AdminPanel)
        // already register themselves as listeners and update their own views.
        // However, it can be a central point if some global UI element needs to react.
        // For now, individual panels handle their specific updates.
        // System.out.println("MainWindow received onQueueUpdated notification.");
    }

    @Override
    public void onServiceCompletedForFeedback(String ticketNumber) {
        SwingUtilities.invokeLater(() -> {
            if (feedbackPanel != null && tabbedPane.isAncestorOf(feedbackPanel)) { // Check if feedbackPanel is part of tabbedPane
                tabbedPane.setSelectedComponent(feedbackPanel);
                feedbackPanel.prepareForFeedback(ticketNumber);

                UIManager.put("OptionPane.messageFont", UITheme.FONT_GENERAL_REGULAR);
                UIManager.put("OptionPane.buttonFont", UITheme.FONT_BUTTON);
                JOptionPane.showMessageDialog(this,
                        "<html>Service for ticket <b>" + ticketNumber + "</b> is complete.<br>Please provide your feedback.</html>",
                        "Feedback Time",
                        JOptionPane.INFORMATION_MESSAGE,
                        UITheme.getIcon("feedback_notification.svg", 32, 32)); // Ensure icon exists
            } else {
                System.err.println("FeedbackPanel not available or not added to tabbed pane. Cannot prompt for feedback for ticket: " + ticketNumber);
            }
        });
    }
}
