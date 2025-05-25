// Filename: DisplayPanel.java
package com.nextque.ui;

import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DisplayPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private final Map<ServiceType, JLabel> servingTicketLabels;
    private final Map<ServiceType, JLabel> serviceNameLabelsOnCard;
    private JLabel clockLabel;
    private JPanel servicesGridPanel;
    private JLabel noServicesAvailableLabel; // Label to show when no services are active
    private JPanel contentWrapper; // Field to hold the CardLayout panel

    public DisplayPanel(QueueManager queueManager) {
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this);
        this.servingTicketLabels = new HashMap<>();
        this.serviceNameLabelsOnCard = new HashMap<>();

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING); // Outer padding

        initComponents();
        buildLayout();
        startClock();
        updateServiceDisplayLayout(); // Initial layout of service cards
        updateDisplayData();          // Initial data population
    }

    private void initComponents() {
        clockLabel = new JLabel("--:--:--", SwingConstants.RIGHT);
        clockLabel.setFont(UITheme.FONT_TITLE_H2);
        clockLabel.setForeground(UITheme.COLOR_TEXT_DARK);

        servicesGridPanel = new JPanel();
        servicesGridPanel.setOpaque(false); // Transparent to show main background

        noServicesAvailableLabel = new JLabel("No services are currently configured or active.", SwingConstants.CENTER);
        noServicesAvailableLabel.setFont(UITheme.FONT_TITLE_H3);
        noServicesAvailableLabel.setForeground(UITheme.COLOR_TEXT_LIGHT);
        noServicesAvailableLabel.setVisible(false); // Initially hidden

        contentWrapper = new JPanel(new CardLayout()); // Initialize contentWrapper
        contentWrapper.setOpaque(false);
    }

    private void buildLayout() {
        setLayout(new BorderLayout(15, 15)); // Gaps for main panel sections

        JPanel headerPanel = new JPanel(new BorderLayout(10,0));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Now Serving", SwingConstants.LEFT);
        titleLabel.setFont(UITheme.FONT_TITLE_H1);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY); // Corrected from COLOR_PRIMARY_DARK
        titleLabel.setIcon(UITheme.getIcon("display_board.svg", 32, 32)); // Ensure icon exists
        titleLabel.setIconTextGap(10);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(clockLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // contentWrapper now uses CardLayout to switch between grid and "no services" message
        contentWrapper.add(noServicesAvailableLabel, "NO_SERVICES");

        JScrollPane scrollPane = new JScrollPane(servicesGridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        contentWrapper.add(scrollPane, "SERVICES_GRID");

        add(contentWrapper, BorderLayout.CENTER);
    }

    private void updateServiceDisplayLayout() {
        CardLayout cl = (CardLayout) contentWrapper.getLayout(); // Use the field

        servicesGridPanel.removeAll();
        servingTicketLabels.clear();
        serviceNameLabelsOnCard.clear();

        List<ServiceType> serviceTypes = queueManager.getAvailableServiceTypes();

        if (serviceTypes.isEmpty()) {
            cl.show(contentWrapper, "NO_SERVICES");
            // No need to revalidate/repaint servicesGridPanel if it's not being shown
            // contentWrapper itself will be revalidated/repainted by CardLayout.
            return;
        }

        cl.show(contentWrapper, "SERVICES_GRID");

        int numServices = serviceTypes.size();
        // Dynamic column calculation:
        // 1 service: 1 col
        // 2-3 services: numServices cols (e.g., 2 services = 2 cols, 3 services = 3 cols)
        // 4-8 services: 4 cols (forms 2 rows if 5-8 services)
        // >8 services: 3 cols (to prevent cards from becoming too small if many services)
        // This logic might need tuning based on typical screen sizes and number of services.
        int cols = (numServices <= 1) ? 1 : (numServices <= 3) ? numServices : (numServices <= 8 ? 4 : 3);
        if (cols == 0) cols = 1; // Should not happen if numServices > 0
        int rows = (int) Math.ceil((double) numServices / cols);

        servicesGridPanel.setLayout(new GridLayout(rows, cols, 20, 20)); // Gaps between cards

        // Attempt to load "Digital-7 Mono" font, fallback to a theme font
        Font ticketNumberFont = getLocalFont("Digital-7 Mono", Font.BOLD, 64, UITheme.FONT_TITLE_H1);

        for (ServiceType type : serviceTypes) {
            JPanel serviceCard = new JPanel(new BorderLayout(5, 10)); // vgap between name and number
            serviceCard.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
            serviceCard.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UITheme.COLOR_BORDER, 1, true),
                    new EmptyBorder(20, 20, 20, 20) // Padding inside card
            ));
            // This is line 119 from the original error log for DisplayPanel
            serviceCard.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                    "arc: 15;" +
                    "borderColor: #e0e0e0;" + // Explicit border color for the card
                    "shadowType: outside;" +
                    "shadowColor: #00000020;" + // Subtle shadow color with alpha
                    "shadowSize: 5;"           // Size of the shadow
            );


            JLabel serviceNameLabel = new JLabel(type.getDisplayName(), SwingConstants.CENTER);
            serviceNameLabel.setFont(UITheme.FONT_TITLE_H3);
            serviceNameLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY); // Corrected from COLOR_PRIMARY_DARK
            serviceNameLabel.setOpaque(true);
            serviceNameLabel.setBackground(UITheme.COLOR_BACKGROUND_SECTION);
            serviceNameLabel.setBorder(new EmptyBorder(10,5,10,5)); // Padding for service name label
            serviceNameLabelsOnCard.put(type, serviceNameLabel);

            JLabel servingTicketLabel = new JLabel("---", SwingConstants.CENTER);
            servingTicketLabel.setFont(ticketNumberFont);
            servingTicketLabel.setForeground(UITheme.COLOR_ACCENT_GOLD); // Using specific accent color
            servingTicketLabel.setOpaque(false); // Number label should be transparent over card background
            servingTicketLabels.put(type, servingTicketLabel);

            serviceCard.add(serviceNameLabel, BorderLayout.NORTH);
            serviceCard.add(servingTicketLabel, BorderLayout.CENTER);

            servicesGridPanel.add(serviceCard);
        }
        servicesGridPanel.revalidate();
        servicesGridPanel.repaint();
    }

    // Local font loading method, specific for "Digital-7 Mono" or similar display fonts.
    // If this font is not found, it falls back to the provided fallbackFont.
    private Font getLocalFont(String family, int style, int size, Font fallbackFont) {
        Font font = new Font(family, style, size);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        boolean found = false;
        for (String name : fontNames) {
            if (name.equalsIgnoreCase(family)) {
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("DisplayPanel: Font '" + family + "' not found. Using fallback font.");
            return fallbackFont;
        }
        return font;
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
        timer.setInitialDelay(0); // Start immediately
        timer.start();
    }

    private void updateDisplayData() {
        List<ServiceType> currentServiceTypes = queueManager.getAvailableServiceTypes();

        // If the "no services" label is visible but services are now available,
        // or if it's not visible but services have become unavailable, trigger layout update.
        boolean servicesAvailable = !currentServiceTypes.isEmpty();
        boolean noServicesLabelCurrentlyVisible = !servicesGridPanel.isVisible() && noServicesAvailableLabel.isVisible(); // A way to check CardLayout state indirectly

        if (servicesAvailable && noServicesLabelCurrentlyVisible) {
            updateServiceDisplayLayout(); // Rebuild grid because services are now available
        } else if (!servicesAvailable && !noServicesLabelCurrentlyVisible) {
            updateServiceDisplayLayout(); // Show "no services" because services became unavailable
        }


        for (ServiceType type : currentServiceTypes) {
            Ticket servingTicket = queueManager.getCurrentlyServing(type); // Assuming this method exists
            JLabel ticketLabel = servingTicketLabels.get(type);

            if (ticketLabel != null) {
                ticketLabel.setText(servingTicket != null ? servingTicket.getTicketNumber() : "---");
            } else {
                // This might occur if onQueueUpdated is called, determines layout doesn't need update,
                // but a new service type appeared for which labels were not created.
                // The check in onQueueUpdated for map size vs currentTypesInQueueManager.size() and key existence
                // should ideally prevent this. If it still happens, it means layout logic needs refinement.
                System.err.println("DisplayPanel: No ticket label found for service type: " + type.getDisplayName() +
                                   ". Consider forcing layout update if service set changes.");
            }

            JLabel nameLabel = serviceNameLabelsOnCard.get(type);
            if(nameLabel != null && !nameLabel.getText().equals(type.getDisplayName())) {
                // Update display name if it has changed (e.g. via AdminPanel)
                nameLabel.setText(type.getDisplayName());
            }
        }
    }

    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(() -> {
            List<ServiceType> currentTypesInQueueManager = queueManager.getAvailableServiceTypes();
            boolean layoutNeedsUpdate = false;

            // Check if the number of services or the specific services have changed
            if (currentTypesInQueueManager.size() != servingTicketLabels.size()) {
                layoutNeedsUpdate = true;
            } else {
                for(ServiceType type : currentTypesInQueueManager) {
                    if(!servingTicketLabels.containsKey(type)) { // A new service type appeared
                        layoutNeedsUpdate = true;
                        break;
                    }
                }
                if (!layoutNeedsUpdate) { // Check if any existing service type was removed
                     for(ServiceType existingType : servingTicketLabels.keySet()){
                         if(!currentTypesInQueueManager.contains(existingType)){
                             layoutNeedsUpdate = true;
                             break;
                         }
                     }
                }
            }

            if (layoutNeedsUpdate) {
                updateServiceDisplayLayout(); // Rebuilds the cards if the set of services changed
            }
            updateDisplayData(); // Always update the data on the cards
        });
    }
}
