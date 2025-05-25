// Filename: CustomerPanel.java
package com.nextque.ui;

import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
// Removed TitledBorder as it's not directly used.
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class CustomerPanel extends JPanel implements QueueManager.QueueUpdateListener { // Implement listener
    private final QueueManager queueManager;
    private JComboBox<ServiceType> serviceTypeComboBox;
    private JTextField customerNameField;
    private JComboBox<Ticket.PriorityReason> priorityReasonComboBox; // For priority selection
    private JButton getTicketButton;
    private JLabel feedbackLabel;
    private JLabel logoLabel; // For "NextQue" logo/title

    // Constructor now takes QueueManager and the listener (MainWindow)
    public CustomerPanel(QueueManager queueManager, QueueManager.FeedbackPromptListener feedbackListener) {
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this); // Register for general updates

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(new EmptyBorder(20, 40, 30, 40)); // Generous padding

        initComponents();
        loadServiceTypes();
        layoutComponents();
        attachListeners();
    }

    private void initComponents() {
        logoLabel = new JLabel("NextQue", SwingConstants.CENTER); // Text logo
        logoLabel.setFont(UITheme.getFont(UITheme.FONT_FAMILY_PRIMARY, Font.BOLD, 60)); // Large logo font
        logoLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        // If you have an actual logo image:
        // Icon actualLogo = UITheme.getIcon("nextque_logo_large.png", 200, 100); // Adjust size
        // if (actualLogo != null) {
        //     logoLabel.setIcon(actualLogo);
        //     logoLabel.setText(""); // Remove text if using image icon
        // }


        serviceTypeComboBox = new JComboBox<>();
        serviceTypeComboBox.setFont(UITheme.FONT_INPUT);
        serviceTypeComboBox.setPreferredSize(new Dimension(350, 40));


        customerNameField = new JTextField(25);
        customerNameField.setFont(UITheme.FONT_INPUT);
        customerNameField.setPreferredSize(new Dimension(350, 40));
        customerNameField.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Optional: Your Name");


        priorityReasonComboBox = new JComboBox<>(Ticket.PriorityReason.values());
        priorityReasonComboBox.setFont(UITheme.FONT_INPUT);
        priorityReasonComboBox.setSelectedItem(Ticket.PriorityReason.NONE); // Default to NONE
        priorityReasonComboBox.setPreferredSize(new Dimension(350, 40));
        // Custom renderer to display PriorityReason's displayName
        priorityReasonComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ticket.PriorityReason) {
                    setText(((Ticket.PriorityReason) value).getDisplayName());
                }
                return this;
            }
        });


        getTicketButton = new JButton("Get My Ticket");
        UITheme.stylePrimaryButton(getTicketButton);
        getTicketButton.setFont(UITheme.getFont(UITheme.FONT_FAMILY_PRIMARY, Font.BOLD, 18)); // Larger button font
        getTicketButton.setIcon(UITheme.getIcon("ticket_get.svg", 20, 20)); // Ensure icon exists
        getTicketButton.setPreferredSize(new Dimension(250, 55));


        feedbackLabel = new JLabel("<html><div style='text-align: center;'>Select your service and get your queue number.<br>Thank you for choosing NextQue!</div></html>", SwingConstants.CENTER);
        feedbackLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        feedbackLabel.setForeground(UITheme.COLOR_TEXT_MEDIUM);
        feedbackLabel.setPreferredSize(new Dimension(500, 80));
    }

    private void loadServiceTypes() {
        List<ServiceType> types = queueManager.getAvailableServiceTypes();
        ServiceType previouslySelected = (ServiceType) serviceTypeComboBox.getSelectedItem();
        serviceTypeComboBox.removeAllItems();

        if (types == null || types.isEmpty()) { // Added null check for types
            serviceTypeComboBox.setEnabled(false);
            priorityReasonComboBox.setEnabled(false);
            customerNameField.setEnabled(false);
            getTicketButton.setEnabled(false);
            feedbackLabel.setText("<html><div style='text-align: center; color: " + UITheme.COLOR_DANGER_HEX() + ";'>No services available at the moment.<br>Please check back later.</div></html>");
        } else {
            for (ServiceType type : types) {
                serviceTypeComboBox.addItem(type);
            }
            serviceTypeComboBox.setEnabled(true);
            priorityReasonComboBox.setEnabled(true);
            customerNameField.setEnabled(true);
            getTicketButton.setEnabled(true);
            feedbackLabel.setText("<html><div style='text-align: center;'>Select your service and get your queue number.<br>Thank you for choosing NextQue!</div></html>");

            // Try to reselect the previously selected item if it's still in the list
            if (previouslySelected != null) {
                for (int i = 0; i < serviceTypeComboBox.getItemCount(); i++) {
                    if (serviceTypeComboBox.getItemAt(i).equals(previouslySelected)) {
                        serviceTypeComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (serviceTypeComboBox.getItemCount() > 0) {
                serviceTypeComboBox.setSelectedIndex(0); // Select first item if nothing was selected or previous selection is gone
            }
        }
    }


    private void layoutComponents() {
        setLayout(new BorderLayout(0, 30)); // Main BorderLayout with vertical gap

        // --- Logo Panel ---
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.add(logoLabel);
        add(logoPanel, BorderLayout.NORTH);

        // --- Main Content Panel (Card like) ---
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true),
                new EmptyBorder(30, 40, 30, 40) // Padding inside card
        ));
        // Apply shadow and rounded corners using FlatLaf client properties
        // This was line 111 in the original error log for CustomerPanel
        cardPanel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                "arc: 20;" + // More rounded
                // "borderColor: #d0d0d0;" + // Using Component.borderColor from UITheme.applyGlobalStyles()
                "shadowType: outside;" +
                "shadowColor: #00000020;" + // Softer shadow
                "shadowSize: 6;" // Slightly smaller shadow
                // "shadowOpacity: 0.15;" // Opacity can also be controlled by alpha in shadowColor
        );


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10); // Consistent insets
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Section Title
        JLabel sectionTitle = new JLabel("Request Your Queue Number");
        sectionTitle.setFont(UITheme.FONT_TITLE_H2);
        sectionTitle.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 25, 0); // Bottom margin for title
        cardPanel.add(sectionTitle, gbc);
        gbc.insets = new Insets(12, 10, 12, 10); // Reset insets
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;


        // Service Type
        JLabel serviceLabel = new JLabel("Service Type:");
        serviceLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        cardPanel.add(serviceLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7; gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(serviceTypeComboBox, gbc);

        // Customer Name
        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        cardPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(customerNameField, gbc);

        // Priority Reason
        JLabel priorityLabel = new JLabel("Priority Status:");
        priorityLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        cardPanel.add(priorityLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(priorityReasonComboBox, gbc);

        // Get Ticket Button - Centered below inputs
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE; // Don't stretch button
        gbc.insets = new Insets(25, 10, 0, 10); // Top margin for button
        cardPanel.add(getTicketButton, gbc);

        // Add cardPanel to the center of the main CustomerPanel
        JPanel centerWrapper = new JPanel(new GridBagLayout()); // To center the card
        centerWrapper.setOpaque(false);
        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.anchor = GridBagConstraints.CENTER;
        // wrapperGbc.weightx = 1.0; // Let card take its preferred size, centered
        // wrapperGbc.weighty = 1.0;
        centerWrapper.add(cardPanel, wrapperGbc);
        add(centerWrapper, BorderLayout.CENTER);

        // Feedback Label at the bottom
        JPanel feedbackPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        feedbackPanel.setOpaque(false);
        feedbackPanel.add(feedbackLabel);
        add(feedbackPanel, BorderLayout.SOUTH);
    }

    private void attachListeners() {
        getTicketButton.addActionListener((ActionEvent e) -> {
            if (!serviceTypeComboBox.isEnabled() || serviceTypeComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this,
                        "No services are currently available or selected. Please check the service list.",
                        "Service Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ServiceType selectedService = (ServiceType) serviceTypeComboBox.getSelectedItem();
            String customerName = customerNameField.getText().trim(); // Trim the name
            if (customerName.isEmpty()) {
                customerName = "Guest"; // Default name if empty
            }
            Ticket.PriorityReason selectedReason = (Ticket.PriorityReason) priorityReasonComboBox.getSelectedItem();

            if (selectedService == null) { // Should be caught by earlier check, but defensive
                 JOptionPane.showMessageDialog(this, "Please select a service type.", "Service Required", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (selectedReason == null) { // Should not happen with JComboBox<Enum>
                 JOptionPane.showMessageDialog(this, "Please select a priority status.", "Priority Required", JOptionPane.ERROR_MESSAGE);
                return;
            }


            Ticket newTicket = queueManager.generateTicket(selectedService, customerName, selectedReason);

            if (newTicket == null) {
                JOptionPane.showMessageDialog(this,
                        "Failed to generate a ticket. Please try again or contact support.",
                        "Ticket Generation Failed", JOptionPane.ERROR_MESSAGE);
                feedbackLabel.setText("<html><div style='text-align: center; color: " + UITheme.COLOR_DANGER_HEX() + ";'>Could not generate ticket. Please try again.</div></html>");
                return;
            }

            String priorityInfo = (selectedReason != Ticket.PriorityReason.NONE) ?
                    "<br>Priority Status: <span style='font-weight: bold; color: " + UITheme.COLOR_ACCENT_GOLD_HEX() + ";'>" + selectedReason.getDisplayName() + "</span>" : "";

            String ticketInfoHtml = String.format(
                    "<html><div style='text-align: center; font-family: Segoe UI, sans-serif;'>" +
                    "<h2 style='margin-bottom: 5px; color: " + UITheme.COLOR_PRIMARY_NAVY_HEX() + ";'>Ticket Issued Successfully!</h2>" +
                    "Your Ticket Number:<br><strong style='font-size: 28px; color: " + UITheme.COLOR_PRIMARY_STEEL_BLUE_HEX() + ";'>%s</strong><br>" +
                    "Service: <span style='font-weight: bold;'>%s</span>" +
                    priorityInfo +
                    "<br>Issued at: <span style='font-weight: bold;'>%s</span><br><br>" +
                    "Please wait for your number to be called.</div></html>",
                    newTicket.getTicketNumber(),
                    newTicket.getServiceType().getDisplayName(),
                    newTicket.getFormattedIssueTime()
            );
            feedbackLabel.setText(ticketInfoHtml); // Update the label on the panel

            // Show JOptionPane with the same info
            JLabel messageLabel = new JLabel(ticketInfoHtml); // Use the HTML directly
            JOptionPane.showMessageDialog(this, messageLabel, "Ticket Confirmation",
                    JOptionPane.INFORMATION_MESSAGE, UITheme.getIcon("ticket_confirm.svg", 48, 48)); // Ensure icon exists

            customerNameField.setText(""); // Clear name field
            priorityReasonComboBox.setSelectedItem(Ticket.PriorityReason.NONE); // Reset priority
            // Consider if serviceTypeComboBox should also be reset or retain selection
        });
    }

    // Implement QueueUpdateListener to refresh service types if they change
    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(this::loadServiceTypes);
    }
}
