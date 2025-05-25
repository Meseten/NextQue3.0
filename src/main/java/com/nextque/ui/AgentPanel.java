// Filename: AgentPanel.java
package com.nextque.ui;

import com.nextque.auth.AuthService;
import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.model.User;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class AgentPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private final AuthService authService;
    private User currentAgent;

    private JComboBox<ServiceType> serviceTypeComboBox;
    private JButton callNextButton;
    private JButton startServiceButton;
    private JButton completeServiceButton;
    private JTextArea queueDisplayArea;
    private JLabel currentlyServingLabel;
    private JLabel waitingCountLabel;
    private JLabel agentInfoLabel;
    private JPanel cardPanel; // Main content card

    public AgentPanel(QueueManager queueManager, AuthService authService) {
        this.queueManager = queueManager;
        this.authService = authService;
        this.currentAgent = authService.getCurrentUser();
        this.queueManager.addQueueUpdateListener(this);

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING);

        initComponents();
        loadServiceTypes();
        layoutComponents();
        attachListeners();
        updateQueueDisplay();
        updateButtonStates();
    }

    private void initComponents() {
        agentInfoLabel = new JLabel("Agent: " + (currentAgent != null ? currentAgent.getFullName() : "N/A") +
                " (" + (currentAgent != null ? currentAgent.getUsername() : "") + ")",
                SwingConstants.LEFT);
        agentInfoLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        agentInfoLabel.setForeground(UITheme.COLOR_TEXT_MEDIUM);
        agentInfoLabel.setIcon(UITheme.getIcon("agent_profile.svg")); // Ensure this icon exists
        agentInfoLabel.setIconTextGap(8);

        serviceTypeComboBox = new JComboBox<>();
        serviceTypeComboBox.setFont(UITheme.FONT_INPUT);
        serviceTypeComboBox.setPreferredSize(new Dimension(280, 38)); // Adjusted size
        serviceTypeComboBox.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 8");


        callNextButton = new JButton("Call Next Ticket");
        UITheme.stylePrimaryButton(callNextButton);
        callNextButton.setIcon(UITheme.getIcon("call_next_arrow.svg")); // Ensure this icon exists

        startServiceButton = new JButton("Start Service");
        UITheme.styleInfoButton(startServiceButton);
        startServiceButton.setIcon(UITheme.getIcon("play_circle.svg")); // Ensure this icon exists


        completeServiceButton = new JButton("Complete Service");
        UITheme.styleSuccessButton(completeServiceButton);
        completeServiceButton.setIcon(UITheme.getIcon("check_circle.svg")); // Ensure this icon exists


        queueDisplayArea = new JTextArea(10, 50);
        queueDisplayArea.setEditable(false);
        queueDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Monospaced for alignment
        queueDisplayArea.setBackground(UITheme.COLOR_BACKGROUND_SECTION); // Slightly different background
        queueDisplayArea.setForeground(UITheme.COLOR_TEXT_DARK);
        queueDisplayArea.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER),
                new EmptyBorder(8,10,8,10) // Padding inside text area
        ));


        currentlyServingLabel = new JLabel("<html><div style='text-align: center;'>Currently Serving: <b style='font-size:1.2em;'>---</b></div></html>", SwingConstants.CENTER);
        currentlyServingLabel.setFont(UITheme.FONT_TITLE_H3);
        currentlyServingLabel.setForeground(UITheme.COLOR_SUCCESS);
        currentlyServingLabel.setOpaque(true);
        currentlyServingLabel.setBackground(new Color(230, 250, 233)); // Softer green background
        currentlyServingLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0, UITheme.COLOR_BORDER),
                new EmptyBorder(15,15,15,15)
        ));


        waitingCountLabel = new JLabel("Waiting: 0", SwingConstants.LEFT);
        waitingCountLabel.setFont(UITheme.FONT_GENERAL_BOLD);
        waitingCountLabel.setForeground(UITheme.COLOR_TEXT_MEDIUM);
    }

    private void loadServiceTypes() {
        List<ServiceType> types = queueManager.getAvailableServiceTypes();
        serviceTypeComboBox.removeAllItems();
        if (types.isEmpty()) {
            serviceTypeComboBox.setEnabled(false);
            callNextButton.setEnabled(false); // No services, can't call
        } else {
            for (ServiceType type : types) {
                serviceTypeComboBox.addItem(type);
            }
            serviceTypeComboBox.setEnabled(true);
            // callNextButton state depends on if agent is serving, handled in updateButtonStates
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout()); // Main panel uses BorderLayout

        cardPanel = new JPanel(new BorderLayout(15, 15));
        cardPanel.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        cardPanel.setBorder(UITheme.BORDER_PANEL_PADDING);
        // The following line is AgentPanel.java:126 from your original error log for AgentPanel
        cardPanel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                "arc: 15;" +
                "shadowType: outside;" +
                "shadowColor: #00000020;" + // Ensure this color format is correct for FlatLaf
                "shadowSize: 6;"
        );

        // --- Top Control Panel ---
        JPanel topControlsPanel = new JPanel(new GridBagLayout());
        topControlsPanel.setOpaque(false);
        GridBagConstraints gbcTop = new GridBagConstraints();
        gbcTop.insets = new Insets(8, 10, 8, 10); // Consistent insets
        gbcTop.anchor = GridBagConstraints.WEST;

        gbcTop.gridx = 0; gbcTop.gridy = 0; gbcTop.gridwidth = 3; // Span across
        topControlsPanel.add(agentInfoLabel, gbcTop);

        gbcTop.gridy = 1; gbcTop.gridwidth = 1;
        JLabel serviceSelectLabel = new JLabel("Service Queue:");
        serviceSelectLabel.setFont(UITheme.FONT_LABEL);
        topControlsPanel.add(serviceSelectLabel, gbcTop);

        gbcTop.gridx = 1; gbcTop.fill = GridBagConstraints.HORIZONTAL; gbcTop.weightx = 1.0;
        topControlsPanel.add(serviceTypeComboBox, gbcTop);

        gbcTop.gridx = 2; gbcTop.fill = GridBagConstraints.NONE; gbcTop.weightx = 0.0; gbcTop.anchor = GridBagConstraints.EAST;
        topControlsPanel.add(callNextButton, gbcTop);

        // --- Middle Section (Currently Serving Info, Action Buttons) ---
        JPanel middleSectionPanel = new JPanel(new BorderLayout(10,15)); // Increased vgap
        middleSectionPanel.setOpaque(false);
        middleSectionPanel.add(currentlyServingLabel, BorderLayout.NORTH);

        JPanel serviceActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5)); // Increased hgap
        serviceActionPanel.setOpaque(false);
        serviceActionPanel.add(startServiceButton);
        serviceActionPanel.add(completeServiceButton);
        middleSectionPanel.add(serviceActionPanel, BorderLayout.CENTER);

        JPanel waitingCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Align left
        waitingCountPanel.setOpaque(false);
        waitingCountPanel.add(waitingCountLabel);
        middleSectionPanel.add(waitingCountPanel, BorderLayout.SOUTH);


        // --- Main Panel Structure within Card ---
        JPanel headerAndControls = new JPanel(new BorderLayout(10,20)); // Increased vgap
        headerAndControls.setOpaque(false);
        headerAndControls.add(topControlsPanel, BorderLayout.NORTH);
        headerAndControls.add(middleSectionPanel, BorderLayout.CENTER);

        cardPanel.add(headerAndControls, BorderLayout.NORTH);

        // Queue Display Area with Titled Border
        JPanel queueDisplayWrapper = new JPanel(new BorderLayout());
        queueDisplayWrapper.setOpaque(false);
        queueDisplayWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(UITheme.COLOR_BORDER, UITheme.COLOR_BORDER.darker()),
                "Waiting Queue Details",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                UITheme.FONT_TITLE_H3,
                UITheme.COLOR_PRIMARY_NAVY
        ));
        JScrollPane scrollPane = new JScrollPane(queueDisplayArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_SECTION); // Match text area bg
        queueDisplayWrapper.add(scrollPane, BorderLayout.CENTER);

        cardPanel.add(queueDisplayWrapper, BorderLayout.CENTER);
        add(cardPanel, BorderLayout.CENTER); // Add the card to the AgentPanel
    }

    private void attachListeners() {
        serviceTypeComboBox.addActionListener(e -> updateQueueDisplay());

        callNextButton.addActionListener(e -> {
            ServiceType selectedService = (ServiceType) serviceTypeComboBox.getSelectedItem();
            if (selectedService != null && currentAgent != null) {
                Ticket currentTicketByAgent = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());
                if (currentTicketByAgent != null) {
                    JOptionPane.showMessageDialog(this,
                            "You are already serving ticket " + currentTicketByAgent.getTicketNumber() + ".\nPlease complete or cancel it first.",
                            "Service in Progress", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Ticket calledTicket = queueManager.callNextTicket(selectedService, currentAgent);
                if (calledTicket == null && serviceTypeComboBox.isEnabled()) {
                    JOptionPane.showMessageDialog(this, "The queue for " + selectedService.getDisplayName() + " is currently empty.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
                }
                updateButtonStates(); // Called after attempting to call next
            }
        });

        startServiceButton.addActionListener(e -> {
            if (currentAgent != null) {
                queueManager.startService(currentAgent.getUsername());
                updateQueueDisplay(); // Refresh display after starting
                updateButtonStates(); // Update buttons based on new state
            }
        });

        completeServiceButton.addActionListener(e -> {
            if (currentAgent != null) {
                Ticket ticketToComplete = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());
                if (ticketToComplete != null && ticketToComplete.getServiceStartTime() == null) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Service was not explicitly started for ticket " + ticketToComplete.getTicketNumber() + ".\nComplete anyway (start time will be approximated)?",
                            "Confirm Completion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) return;
                }
                queueManager.completeService(currentAgent.getUsername());
                updateQueueDisplay(); // Refresh display after completing
                updateButtonStates(); // Update buttons based on new state
            }
        });
    }

    private void updateQueueDisplay() {
        ServiceType selectedService = (ServiceType) serviceTypeComboBox.getSelectedItem();
        if (selectedService == null && serviceTypeComboBox.getItemCount() > 0) {
            selectedService = serviceTypeComboBox.getItemAt(0);
            serviceTypeComboBox.setSelectedItem(selectedService); // Ensure a selection if possible
        }

        if (selectedService != null) {
            List<Ticket> queue = queueManager.getQueueSnapshot(selectedService);
            StringBuilder sb = new StringBuilder();
            // Updated header to include Priority Reason
            sb.append(String.format("%-16s | %-20s | %-18s | %-8s | %-10s | %s\n",
                    "Ticket No.", "Customer Name", "Priority Reason", "Issued", "Status", "Numeric Pri"));
            sb.append("-".repeat(95)).append("\n");
            if (queue.isEmpty()) {
                sb.append("  Queue is empty for ").append(selectedService.getDisplayName()).append("\n");
            } else {
                for (Ticket ticket : queue) {
                    sb.append(String.format("%-16s | %-20s | %-18s | %-8s | %-10s | %d\n",
                            ticket.getTicketNumber(),
                            ticket.getCustomerName(),
                            ticket.getPriorityReason().getDisplayName(), // Display priority reason
                            ticket.getFormattedIssueTime(),
                            ticket.getStatus(),
                            ticket.getPriority())); // Display numerical priority
                }
            }
            queueDisplayArea.setText(sb.toString());
            queueDisplayArea.setCaretPosition(0); // Scroll to top
            waitingCountLabel.setText("Waiting in \"" + selectedService.getDisplayName() + "\" queue: " + queueManager.getWaitingCount(selectedService));
        } else {
            queueDisplayArea.setText("  Please select a service type to view its queue, or no services are available.");
            waitingCountLabel.setText("Waiting: 0");
        }

        if (currentAgent != null) {
            Ticket agentServingTicket = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());
            if (agentServingTicket != null) {
                String startTimeStr = agentServingTicket.getServiceStartTime() != null ?
                        agentServingTicket.getFormattedTime(agentServingTicket.getServiceStartTime()) : "Not Started";
                String priorityDisplay = (agentServingTicket.getPriorityReason() != Ticket.PriorityReason.NONE) ?
                        "<br>Priority: " + agentServingTicket.getPriorityReason().getDisplayName() : "";
                currentlyServingLabel.setText(String.format("<html><div style='text-align: center;'>" +
                                "Serving: <b style='font-size:1.2em; color:" + UITheme.COLOR_PRIMARY_STEEL_BLUE_HEX() + ";'>%s</b><br>" +
                                "Service: %s %s<br>" +
                                "Status: %s (Started: %s)</div></html>",
                        agentServingTicket.getTicketNumber(),
                        agentServingTicket.getServiceType().getDisplayName(),
                        priorityDisplay, // Show priority reason
                        agentServingTicket.getStatus(),
                        startTimeStr));
            } else {
                currentlyServingLabel.setText("<html><div style='text-align: center;'>Currently Serving: <b style='font-size:1.2em;'>---</b><br><span style='font-size:0.9em; color:" + UITheme.COLOR_TEXT_MEDIUM_HEX() + ";'>Select a service and 'Call Next'</span></div></html>");
            }
        }
        updateButtonStates(); // Ensure buttons are correct after display update
    }

    private void updateButtonStates() {
        if (currentAgent == null) {
            callNextButton.setEnabled(false);
            startServiceButton.setEnabled(false);
            completeServiceButton.setEnabled(false);
            serviceTypeComboBox.setEnabled(false);
            return;
        }

        serviceTypeComboBox.setEnabled(true); // Agent is present, combo should be enabled if services exist
        Ticket ticketBeingServed = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());

        if (ticketBeingServed == null) { // Agent is not currently serving anyone
            callNextButton.setEnabled(serviceTypeComboBox.getItemCount() > 0); // Can call if services exist
            startServiceButton.setEnabled(false);    // Cannot start if not serving
            completeServiceButton.setEnabled(false); // Cannot complete if not serving
        } else { // Agent is serving a ticket
            callNextButton.setEnabled(false); // Cannot call another while serving
            if (ticketBeingServed.getServiceStartTime() == null) { // Service has been called but not started
                startServiceButton.setEnabled(true);
                completeServiceButton.setEnabled(true); // Allow completion even if not formally started (with warning)
            } else { // Service has been started
                startServiceButton.setEnabled(false);
                completeServiceButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(() -> {
            // Check if the list of service types in the JComboBox needs refreshing
            int currentCbCount = serviceTypeComboBox.getItemCount();
            List<ServiceType> availableTypes = queueManager.getAvailableServiceTypes();
            boolean listChanged = false;
            if (currentCbCount != availableTypes.size()) {
                listChanged = true;
            } else {
                // Check if the actual items are different
                for(ServiceType type : availableTypes) {
                    boolean found = false;
                    for(int i=0; i < serviceTypeComboBox.getItemCount(); i++) {
                        if(serviceTypeComboBox.getItemAt(i).equals(type)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        listChanged = true;
                        break;
                    }
                }
            }

            if (listChanged) {
                 ServiceType previouslySelected = (ServiceType) serviceTypeComboBox.getSelectedItem();
                 loadServiceTypes(); // Reloads items
                 if (previouslySelected != null) { // Try to reselect if still available
                     for(int i=0; i < serviceTypeComboBox.getItemCount(); i++) {
                         if(serviceTypeComboBox.getItemAt(i).equals(previouslySelected)) {
                             serviceTypeComboBox.setSelectedIndex(i);
                             break;
                         }
                     }
                 }
            }
            updateQueueDisplay(); // This will also call updateButtonStates
        });
    }
}
