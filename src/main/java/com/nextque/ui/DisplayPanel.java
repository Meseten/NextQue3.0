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
import java.util.Map;
import java.util.HashMap;

public class DisplayPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private final Map<ServiceType, JLabel> servingTicketLabels = new HashMap<>();
    private JLabel clockLabel;
    private JPanel servicesGridPanel;
    private final CardLayout contentCardLayout = new CardLayout();
    private final JPanel contentWrapper = new JPanel(contentCardLayout);

    public DisplayPanel(QueueManager queueManager) {
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this);

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING);

        buildLayout();
        startClock();
        updateServiceDisplayLayout();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(15, 15));

        JPanel headerPanel = new JPanel(new BorderLayout(10,0));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Now Serving", SwingConstants.LEFT);
        titleLabel.setFont(UITheme.FONT_TITLE_H1);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        titleLabel.setIcon(UITheme.getIcon("display_board.svg", 32, 32));
        titleLabel.setIconTextGap(10);
        
        clockLabel = new JLabel("--:--:--", SwingConstants.RIGHT);
        clockLabel.setFont(UITheme.FONT_TITLE_H2);
        clockLabel.setForeground(UITheme.COLOR_TEXT_DARK);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(clockLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JLabel noServicesAvailableLabel = new JLabel("No services are currently configured or active.", SwingConstants.CENTER);
        noServicesAvailableLabel.setFont(UITheme.FONT_TITLE_H3);
        noServicesAvailableLabel.setForeground(UITheme.COLOR_TEXT_LIGHT);
        
        servicesGridPanel = new JPanel();
        servicesGridPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(servicesGridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        
        contentWrapper.setOpaque(false);
        contentWrapper.add(noServicesAvailableLabel, "NO_SERVICES");
        contentWrapper.add(scrollPane, "SERVICES_GRID");
        add(contentWrapper, BorderLayout.CENTER);
    }

    private void updateServiceDisplayLayout() {
        servingTicketLabels.clear();
        servicesGridPanel.removeAll();
        
        List<ServiceType> serviceTypes = queueManager.getAvailableServiceTypes();

        if (serviceTypes.isEmpty()) {
            contentCardLayout.show(contentWrapper, "NO_SERVICES");
            return;
        }

        contentCardLayout.show(contentWrapper, "SERVICES_GRID");
        
        int numServices = serviceTypes.size();
        int cols = (numServices <= 1) ? 1 : (numServices <= 3) ? numServices : 4;
        int rows = (int) Math.ceil((double) numServices / cols);
        servicesGridPanel.setLayout(new GridLayout(rows, cols, 20, 20));

        Font ticketNumberFont = getLocalFont("Digital-7 Mono", Font.BOLD, 64, UITheme.FONT_TITLE_H1);

        for (ServiceType type : serviceTypes) {
            CardPanel serviceCard = new CardPanel(new BorderLayout(5, 10));
            serviceCard.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel serviceNameLabel = new JLabel(type.getDisplayName(), SwingConstants.CENTER);
            serviceNameLabel.setFont(UITheme.FONT_TITLE_H3);
            serviceNameLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
            serviceNameLabel.setOpaque(true);
            serviceNameLabel.setBackground(UITheme.COLOR_BACKGROUND_SECTION);
            serviceNameLabel.setBorder(new EmptyBorder(10,5,10,5));

            JLabel servingTicketLabel = new JLabel("---", SwingConstants.CENTER);
            servingTicketLabel.setFont(ticketNumberFont);
            servingTicketLabel.setForeground(UITheme.COLOR_ACCENT_GOLD);
            
            servingTicketLabels.put(type, servingTicketLabel);

            serviceCard.add(serviceNameLabel, BorderLayout.NORTH);
            serviceCard.add(servingTicketLabel, BorderLayout.CENTER);
            servicesGridPanel.add(serviceCard);
        }
        servicesGridPanel.revalidate();
        servicesGridPanel.repaint();
        updateDisplayData();
    }

    private Font getLocalFont(String family, int style, int size, Font fallbackFont) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String name : ge.getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(family)) {
                return new Font(family, style, size);
            }
        }
        return fallbackFont;
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timer.setInitialDelay(0);
        timer.start();
    }

    private void updateDisplayData() {
        for (Map.Entry<ServiceType, JLabel> entry : servingTicketLabels.entrySet()) {
            ServiceType type = entry.getKey();
            JLabel ticketLabel = entry.getValue();
            Ticket servingTicket = queueManager.getCurrentlyServing(type);
            ticketLabel.setText(servingTicket != null ? servingTicket.getTicketNumber() : "---");
        }
    }

    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(() -> {
            boolean layoutNeedsUpdate = servingTicketLabels.size() != queueManager.getAvailableServiceTypes().size();
            if (layoutNeedsUpdate) {
                updateServiceDisplayLayout();
            } else {
                updateDisplayData();
            }
        });
    }
}
