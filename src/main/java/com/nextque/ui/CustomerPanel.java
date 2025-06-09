package com.nextque.ui;

import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class CustomerPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private JComboBox<ServiceType> serviceTypeComboBox;
    private JTextField customerNameField;
    private JComboBox<Ticket.PriorityReason> priorityReasonComboBox;
    private JButton getTicketButton;
    private JLabel feedbackLabel;
    private JLabel logoLabel;

    public CustomerPanel(QueueManager queueManager) {
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this);

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(new EmptyBorder(20, 40, 30, 40));

        initComponents();
        loadServiceTypes();
        layoutComponents();
        attachListeners();
    }

    private void initComponents() {
        logoLabel = new JLabel("NextQue", SwingConstants.CENTER);
        logoLabel.setFont(UITheme.getFont(UITheme.FONT_FAMILY_PRIMARY, Font.BOLD, 60));
        logoLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);

        serviceTypeComboBox = new JComboBox<>();
        serviceTypeComboBox.setFont(UITheme.FONT_INPUT);
        serviceTypeComboBox.setPreferredSize(new Dimension(350, 40));

        customerNameField = new JTextField(25);
        customerNameField.setFont(UITheme.FONT_INPUT);
        customerNameField.setPreferredSize(new Dimension(350, 40));
        customerNameField.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Optional: Your Name");

        priorityReasonComboBox = new JComboBox<>(Ticket.PriorityReason.values());
        priorityReasonComboBox.setFont(UITheme.FONT_INPUT);
        priorityReasonComboBox.setSelectedItem(Ticket.PriorityReason.NONE);
        priorityReasonComboBox.setPreferredSize(new Dimension(350, 40));
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
        getTicketButton.setFont(UITheme.getFont(UITheme.FONT_FAMILY_PRIMARY, Font.BOLD, 18));
        getTicketButton.setIcon(UITheme.getIcon("ticket_get.svg", 20, 20));
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

        boolean hasServices = types != null && !types.isEmpty();
        
        serviceTypeComboBox.setEnabled(hasServices);
        priorityReasonComboBox.setEnabled(hasServices);
        customerNameField.setEnabled(hasServices);
        getTicketButton.setEnabled(hasServices);

        if (hasServices) {
            for (ServiceType type : types) {
                serviceTypeComboBox.addItem(type);
            }
            feedbackLabel.setText("<html><div style='text-align: center;'>Select your service and get your queue number.<br>Thank you for choosing NextQue!</div></html>");

            if (previouslySelected != null) {
                serviceTypeComboBox.setSelectedItem(previouslySelected);
            } else if (serviceTypeComboBox.getItemCount() > 0) {
                serviceTypeComboBox.setSelectedIndex(0);
            }
        } else {
            feedbackLabel.setText("<html><div style='text-align: center; color: " + UITheme.COLOR_DANGER_HEX() + ";'>No services available at the moment.<br>Please check back later.</div></html>");
        }
    }


    private void layoutComponents() {
        setLayout(new BorderLayout(0, 30));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.add(logoLabel);
        add(logoPanel, BorderLayout.NORTH);

        CardPanel cardPanel = new CardPanel(new GridBagLayout());
        cardPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel sectionTitle = new JLabel("Request Your Queue Number");
        sectionTitle.setFont(UITheme.FONT_TITLE_H2);
        sectionTitle.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 25, 0);
        cardPanel.add(sectionTitle, gbc);
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;

        JLabel serviceLabel = new JLabel("Service Type:");
        serviceLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        cardPanel.add(serviceLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7; gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(serviceTypeComboBox, gbc);

        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        cardPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(customerNameField, gbc);

        JLabel priorityLabel = new JLabel("Priority Status:");
        priorityLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        cardPanel.add(priorityLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        cardPanel.add(priorityReasonComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(25, 10, 0, 10);
        cardPanel.add(getTicketButton, gbc);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(cardPanel, new GridBagConstraints());
        add(centerWrapper, BorderLayout.CENTER);

        JPanel feedbackPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        feedbackPanel.setOpaque(false);
        feedbackPanel.add(feedbackLabel);
        add(feedbackPanel, BorderLayout.SOUTH);
    }

    private void attachListeners() {
        getTicketButton.addActionListener((ActionEvent e) -> {
            ServiceType selectedService = (ServiceType) serviceTypeComboBox.getSelectedItem();
            if (selectedService == null) {
                JOptionPane.showMessageDialog(this, "No services are currently available or selected.", "Service Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String customerName = customerNameField.getText().trim();
            Ticket.PriorityReason selectedReason = (Ticket.PriorityReason) priorityReasonComboBox.getSelectedItem();
            
            Ticket newTicket = queueManager.generateTicket(selectedService, customerName, selectedReason);

            if (newTicket == null) {
                JOptionPane.showMessageDialog(this, "Failed to generate a ticket. Please try again.", "Ticket Generation Failed", JOptionPane.ERROR_MESSAGE);
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
            
            JOptionPane.showMessageDialog(this, new JLabel(ticketInfoHtml), "Ticket Confirmation",
                    JOptionPane.INFORMATION_MESSAGE, UITheme.getIcon("ticket_confirm.svg", 48, 48));

            customerNameField.setText("");
            priorityReasonComboBox.setSelectedItem(Ticket.PriorityReason.NONE);
        });
    }

    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(this::loadServiceTypes);
    }
}
