// Filename: DashboardPanel.java
package com.nextque.ui;

import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
// Removed TitledBorder import as it's not used directly here.
import java.awt.*;
import java.util.List; // For service types
import java.time.LocalTime; // For last updated timestamp
import java.time.format.DateTimeFormatter; // For formatting time

public class DashboardPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private JPanel statsCardsPanel;
    private JLabel totalWaitingLabel;
    private JLabel lastUpdatedLabel; // To show when data was last refreshed

    public DashboardPanel(QueueManager queueManager) {
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this);

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING); // Outer padding for the whole panel
        setLayout(new BorderLayout(15, 15)); // Gaps between main sections

        initComponents();
        layoutComponents();
        updateDashboard();
    }

    private void initComponents() {
        statsCardsPanel = new JPanel(); // Will use dynamic grid layout
        statsCardsPanel.setOpaque(false); // Transparent to show main panel background

        totalWaitingLabel = new JLabel("Total People Waiting Across All Services: 0", SwingConstants.CENTER);
        totalWaitingLabel.setFont(UITheme.FONT_TITLE_H2);
        totalWaitingLabel.setForeground(UITheme.COLOR_DANGER); // Use danger color for high waiting count
        totalWaitingLabel.setBorder(new EmptyBorder(10,0,10,0)); // Padding for the total label

        lastUpdatedLabel = new JLabel("Last updated: --:--:--", SwingConstants.RIGHT);
        lastUpdatedLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        lastUpdatedLabel.setForeground(UITheme.COLOR_TEXT_LIGHT);
    }

    private void layoutComponents() {
        // --- Header Panel (Title and Last Updated) ---
        JPanel headerPanel = new JPanel(new BorderLayout(10,0));
        headerPanel.setOpaque(false); // Transparent background

        JLabel titleLabel = new JLabel("Live Queue Analytics", SwingConstants.LEFT);
        titleLabel.setFont(UITheme.FONT_TITLE_H1);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY); // Corrected from COLOR_PRIMARY_DARK
        titleLabel.setIcon(UITheme.getIcon("dashboard_stats.svg", 32, 32)); // Ensure icon exists
        titleLabel.setIconTextGap(10);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(lastUpdatedLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Scrollable area for stats cards ---
        JScrollPane scrollPane = new JScrollPane(statsCardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // No border for the scroll pane itself
        scrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_MAIN); // Match main bg
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // --- Footer (Total Waiting) ---
        add(totalWaitingLabel, BorderLayout.SOUTH);
    }

    private void updateDashboard() {
        statsCardsPanel.removeAll();
        List<ServiceType> serviceTypes = queueManager.getAvailableServiceTypes();

        if (serviceTypes.isEmpty()) {
            JLabel noServicesLabel = new JLabel("No services available to display statistics.", SwingConstants.CENTER);
            noServicesLabel.setFont(UITheme.FONT_TITLE_H3);
            noServicesLabel.setForeground(UITheme.COLOR_TEXT_LIGHT);
            statsCardsPanel.setLayout(new BorderLayout()); // Simple layout for the message
            statsCardsPanel.add(noServicesLabel, BorderLayout.CENTER);
        } else {
            // Dynamic grid layout for cards
            int numServices = serviceTypes.size();
            // Adjust columns based on number of services to prevent overly wide cards or too many rows
            int cols = (numServices <= 1) ? 1 : (numServices <= 4 ? 2 : (numServices <= 9 ? 3 : 4)); // Max 3 or 4 columns
            int rows = (int) Math.ceil((double) numServices / cols);
            statsCardsPanel.setLayout(new GridLayout(rows, cols, 15, 15)); // Gaps between cards

            for (ServiceType type : serviceTypes) {
                statsCardsPanel.add(createServiceStatCard(type));
            }
        }

        totalWaitingLabel.setText("Total People Waiting Across All Services: " + queueManager.getTotalWaitingCount());
        lastUpdatedLabel.setText("Last updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        statsCardsPanel.revalidate();
        statsCardsPanel.repaint();
    }

    private JPanel createServiceStatCard(ServiceType type) {
        JPanel card = new JPanel(new BorderLayout(5,5)); // Small gaps within card
        card.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true), // Card border
                new EmptyBorder(12, 12, 12, 12) // Padding inside card
        ));
        card.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 10"); // Rounded corners

        JLabel serviceNameLabel = new JLabel(type.getDisplayName(), SwingConstants.LEFT);
        serviceNameLabel.setFont(UITheme.FONT_TITLE_H3);
        serviceNameLabel.setForeground(UITheme.COLOR_PRIMARY_STEEL_BLUE); // Corrected from COLOR_PRIMARY_MEDIUM
        serviceNameLabel.setIcon(UITheme.getIcon("service_tag.svg")); // Ensure icon exists; generic service icon
        serviceNameLabel.setIconTextGap(8);
        card.add(serviceNameLabel, BorderLayout.NORTH);

        JPanel detailsPanel = new JPanel(new GridLayout(0, 1, 3, 3)); // Vertical layout for stats, 0 rows means as many as needed
        detailsPanel.setOpaque(false); // Transparent background

        int waitingCount = queueManager.getWaitingCount(type);
        Ticket current = queueManager.getCurrentlyServing(type); // Assuming this method exists and returns the ticket or null
        String servingInfo = (current != null) ? current.getTicketNumber() : "---";
        // Consider adding customer name or other details if available and relevant for "Now Serving"

        detailsPanel.add(createStatLabel("Waiting:", String.valueOf(waitingCount)));
        detailsPanel.add(createStatLabel("Now Serving:", servingInfo));
        // Example for future stats:
        // String avgWaitTime = queueManager.getAverageWaitTime(type); // Assuming method exists
        // detailsPanel.add(createStatLabel("Avg. Wait Time:", avgWaitTime != null ? avgWaitTime : "N/A"));

        card.add(detailsPanel, BorderLayout.CENTER);
        return card;
    }

    private JLabel createStatLabel(String title, String value) {
        // Using HTML for simple bolding of the title part
        JLabel label = new JLabel("<html><body style='width: 150px'><b>" + title + "</b> " + value + "</body></html>");
        label.setFont(UITheme.FONT_GENERAL_REGULAR);
        label.setForeground(UITheme.COLOR_TEXT_DARK);
        return label;
    }

    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(this::updateDashboard);
    }
}
