package com.nextque.ui;

import com.nextque.auth.AuthService;
import com.nextque.db.DatabaseManager;
import com.nextque.model.UserRole;
import com.nextque.service.QueueManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.border.EmptyBorder;

public class MainWindow extends JFrame implements QueueManager.FeedbackPromptListener {
    private final QueueManager queueManager;
    private final AuthService authService;
    private final DatabaseManager dbManager;

    private JTabbedPane tabbedPane;
    private CustomerPanel customerPanel;
    private FeedbackPanel feedbackPanel;

    public MainWindow(QueueManager queueManager, AuthService authService, DatabaseManager dbManager) {
        this.queueManager = queueManager;
        this.authService = authService;
        this.dbManager = dbManager;

        this.queueManager.setFeedbackPromptListener(this);

        setTitle("NextQue - Queuing Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performLogout();
            }
        });
        setSize(1366, 768);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        getContentPane().setBackground(UITheme.COLOR_BACKGROUND_MAIN);

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setFont(UITheme.FONT_GENERAL_BOLD);
        
        UserRole currentRole = authService.getCurrentUser().getRole();

        customerPanel = new CustomerPanel(queueManager);
        tabbedPane.addTab("Customer Kiosk", UITheme.getIcon("kiosk.svg"), customerPanel, "Access customer ticket services");

        DisplayPanel displayPanel = new DisplayPanel(queueManager);
        tabbedPane.addTab("Public Display", UITheme.getIcon("display_screen.svg"), displayPanel, "View current queue status");

        feedbackPanel = new FeedbackPanel(dbManager);
        tabbedPane.addTab("Provide Feedback", UITheme.getIcon("feedback_bubbles.svg"), feedbackPanel, "Submit feedback for services");

        if (currentRole == UserRole.AGENT || currentRole == UserRole.ADMIN) {
            AgentPanel agentPanel = new AgentPanel(queueManager, authService);
            tabbedPane.addTab("Agent Desk", UITheme.getIcon("agent_headset.svg"), agentPanel, "Manage queues and serve tickets");
        }

        if (currentRole == UserRole.ADMIN) {
            DashboardPanel dashboardPanel = new DashboardPanel(queueManager);
            tabbedPane.addTab("Dashboard", UITheme.getIcon("dashboard_chart.svg"), dashboardPanel, "View queue analytics");

            AdminPanel adminPanel = new AdminPanel(dbManager, queueManager);
            tabbedPane.addTab("Admin Console", UITheme.getIcon("admin_settings.svg"), adminPanel, "System administration");
        }

        if (currentRole == UserRole.AGENT) {
            tabbedPane.setSelectedIndex(3);
        } else if (currentRole == UserRole.ADMIN) {
            tabbedPane.setSelectedIndex(4);
        }
    }

    private void performLogout() {
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout and exit NextQue?",
                "Logout Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UITheme.getIcon("logout_question.svg", 32,32));

        if (confirmation == JOptionPane.YES_OPTION) {
            authService.logout();
            System.out.println("User logged out. Exiting application.");
            dispose();
            System.exit(0);
        }
    }

    private void layoutComponents() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(UITheme.FONT_GENERAL_BOLD);
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem logoutItem = new JMenuItem("Logout & Exit", UITheme.getIcon("logout.svg"));
        logoutItem.setFont(UITheme.FONT_GENERAL_REGULAR);
        logoutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        logoutItem.addActionListener(e -> performLogout());

        fileMenu.add(logoutItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel topHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topHeaderPanel.setBackground(UITheme.COLOR_PRIMARY_NAVY);
        topHeaderPanel.setBorder(new EmptyBorder(5,10,5,10));
        JLabel userInfoLabel = new JLabel("User: " + authService.getCurrentUser().getFullName() + " (" + authService.getCurrentUser().getRole() + ")");
        userInfoLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        userInfoLabel.setForeground(UITheme.COLOR_TEXT_ON_PRIMARY);
        topHeaderPanel.add(userInfoLabel);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        mainContentPanel.add(topHeaderPanel, BorderLayout.NORTH);
        mainContentPanel.add(tabbedPane, BorderLayout.CENTER);
        
        add(mainContentPanel, BorderLayout.CENTER);
    }

    public void display() {
        setVisible(true);
    }
    
    @Override
    public void onServiceCompletedForFeedback(String ticketNumber) {
        SwingUtilities.invokeLater(() -> {
            if (feedbackPanel != null && tabbedPane.isAncestorOf(feedbackPanel)) {
                tabbedPane.setSelectedComponent(feedbackPanel);
                feedbackPanel.prepareForFeedback(ticketNumber);
                JOptionPane.showMessageDialog(this,
                        "<html>Service for ticket <b>" + ticketNumber + "</b> is complete.<br>Please provide your feedback.</html>",
                        "Feedback Time",
                        JOptionPane.INFORMATION_MESSAGE,
                        UITheme.getIcon("feedback_notification.svg", 32, 32));
            } else {
                System.err.println("FeedbackPanel not available. Cannot prompt for feedback for ticket: " + ticketNumber);
            }
        });
    }
}
