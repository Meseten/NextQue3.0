package com.nextque.ui;

import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private JPanel statsCardsPanel;
    private JLabel totalWaitingLabel;
    private JLabel lastUpdatedLabel;

    public DashboardPanel(QueueManager queueManager) {
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this);

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING);
        setLayout(new BorderLayout(15, 15));

        initComponents();
        layoutComponents();
        updateDashboard();
    }

    private void initComponents() {
        statsCardsPanel = new JPanel();
        statsCardsPanel.setOpaque(false);

        totalWaitingLabel = new JLabel("Total People Waiting Across All Services: 0", SwingConstants.CENTER);
        totalWaitingLabel.setFont(UITheme.FONT_TITLE_H2);
        totalWaitingLabel.setForeground(UITheme.COLOR_DANGER);
        totalWaitingLabel.setBorder(new EmptyBorder(10,0,10,0));

        lastUpdatedLabel = new JLabel("Last updated: --:--:--", SwingConstants.RIGHT);
        lastUpdatedLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        lastUpdatedLabel.setForeground(UITheme.COLOR_TEXT_LIGHT);
    }

    private void layoutComponents() {
        JPanel headerPanel = new JPanel(new BorderLayout(10,0));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Live Queue Analytics", SwingConstants.LEFT);
        titleLabel.setFont(UITheme.FONT_TITLE_H1);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        titleLabel.setIcon(UITheme.getIcon("dashboard_stats.svg", 32, 32));
        titleLabel.setIconTextGap(10);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(lastUpdatedLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(statsCardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        add(scrollPane, BorderLayout.CENTER);
        add(totalWaitingLabel, BorderLayout.SOUTH);
    }

    private void updateDashboard() {
        statsCardsPanel.removeAll();
        List<ServiceType> serviceTypes = queueManager.getAvailableServiceTypes();

        if (serviceTypes.isEmpty()) {
            JLabel noServicesLabel = new JLabel("No services available to display statistics.", SwingConstants.CENTER);
            noServicesLabel.setFont(UITheme.FONT_TITLE_H3);
            noServicesLabel.setForeground(UITheme.COLOR_TEXT_LIGHT);
            statsCardsPanel.setLayout(new BorderLayout());
            statsCardsPanel.add(noServicesLabel, BorderLayout.CENTER);
        } else {
            int numServices = serviceTypes.size();
            int cols = (numServices <= 1) ? 1 : (numServices <= 4 ? 2 : (numServices <= 9 ? 3 : 4));
            statsCardsPanel.setLayout(new GridLayout(0, cols, 15, 15));

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
        CardPanel card = new CardPanel(new BorderLayout(5,5));
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel serviceNameLabel = new JLabel(type.getDisplayName(), SwingConstants.LEFT);
        serviceNameLabel.setFont(UITheme.FONT_TITLE_H3);
        serviceNameLabel.setForeground(UITheme.COLOR_PRIMARY_STEEL_BLUE);
        serviceNameLabel.setIcon(UITheme.getIcon("service_tag.svg"));
        serviceNameLabel.setIconTextGap(8);
        card.add(serviceNameLabel, BorderLayout.NORTH);

        JPanel detailsPanel = new JPanel(new GridLayout(0, 1, 3, 3));
        detailsPanel.setOpaque(false);

        int waitingCount = queueManager.getWaitingCount(type);
        Ticket current = queueManager.getCurrentlyServing(type);
        String servingInfo = (current != null) ? current.getTicketNumber() : "---";

        detailsPanel.add(createStatLabel("Waiting:", String.valueOf(waitingCount)));
        detailsPanel.add(createStatLabel("Now Serving:", servingInfo));

        card.add(detailsPanel, BorderLayout.CENTER);
        return card;
    }

    private JLabel createStatLabel(String title, String value) {
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
