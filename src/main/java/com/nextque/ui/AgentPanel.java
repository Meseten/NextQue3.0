package com.nextque.ui;

import com.nextque.auth.AuthService;
import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.model.User;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class AgentPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final QueueManager queueManager;
    private final AuthService authService;
    private User currentAgent;

    private JComboBox<ServiceType> serviceTypeComboBox;
    private JButton callNextButton;
    private JButton startServiceButton;
    private JButton completeServiceButton;
    private JTable queueTable;
    private DefaultTableModel queueTableModel;
    private JLabel currentlyServingLabel;
    private JLabel waitingCountLabel;

    public AgentPanel(QueueManager queueManager, AuthService authService) {
        this.queueManager = queueManager;
        this.authService = authService;
        this.currentAgent = authService.getCurrentUser();
        this.queueManager.addQueueUpdateListener(this);

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING);

        initComponents();
        layoutComponents();
        attachListeners();
        
        loadServiceTypes();
        updateQueueDisplay();
    }

    private void initComponents() {
        serviceTypeComboBox = new JComboBox<>();
        serviceTypeComboBox.setFont(UITheme.FONT_INPUT);
        serviceTypeComboBox.setPreferredSize(new Dimension(280, 38));

        callNextButton = new JButton("Call Next Ticket");
        UITheme.stylePrimaryButton(callNextButton);
        callNextButton.setIcon(UITheme.getIcon("call_next_arrow.svg"));

        startServiceButton = new JButton("Start Service");
        UITheme.styleInfoButton(startServiceButton);
        startServiceButton.setIcon(UITheme.getIcon("play_circle.svg"));

        completeServiceButton = new JButton("Complete Service");
        UITheme.styleSuccessButton(completeServiceButton);
        completeServiceButton.setIcon(UITheme.getIcon("check_circle.svg"));
        
        queueTableModel = new DefaultTableModel(
            new String[]{"Ticket No.", "Customer Name", "Priority Reason", "Issued", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        queueTable = new JTable(queueTableModel);
        queueTable.setFillsViewportHeight(true);
        queueTable.getTableHeader().setFont(UITheme.FONT_TABLE_HEADER);
        queueTable.getTableHeader().setBackground(UITheme.COLOR_PRIMARY_STEEL_BLUE);
        queueTable.getTableHeader().setForeground(UITheme.COLOR_TEXT_ON_PRIMARY);
        queueTable.setFont(UITheme.FONT_TABLE_CELL);
        queueTable.setRowHeight(24);
        
        TableColumnModel tcm = queueTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(120);
        tcm.getColumn(1).setPreferredWidth(180);
        tcm.getColumn(2).setPreferredWidth(150);
        tcm.getColumn(3).setPreferredWidth(80);
        tcm.getColumn(4).setPreferredWidth(100);

        currentlyServingLabel = new JLabel("<html><div style='text-align: center;'>Currently Serving: <b style='font-size:1.2em;'>---</b></div></html>", SwingConstants.CENTER);
        currentlyServingLabel.setFont(UITheme.FONT_TITLE_H3);
        currentlyServingLabel.setForeground(UITheme.COLOR_SUCCESS);
        currentlyServingLabel.setOpaque(true);
        currentlyServingLabel.setBackground(new Color(230, 250, 233));
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
        ServiceType previouslySelected = (ServiceType) serviceTypeComboBox.getSelectedItem();
        
        serviceTypeComboBox.removeAllItems();
        
        if (types.isEmpty()) {
            serviceTypeComboBox.setEnabled(false);
        } else {
            serviceTypeComboBox.setEnabled(true);
            for (ServiceType type : types) {
                serviceTypeComboBox.addItem(type);
            }
            if (previouslySelected != null && types.contains(previouslySelected)) {
                serviceTypeComboBox.setSelectedItem(previouslySelected);
            }
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        CardPanel cardPanel = new CardPanel(new BorderLayout(15, 15));
        cardPanel.setBorder(UITheme.BORDER_SECTION_PADDING);

        JPanel topControlsPanel = new JPanel(new GridBagLayout());
        topControlsPanel.setOpaque(false);
        GridBagConstraints gbcTop = new GridBagConstraints();
        gbcTop.insets = new Insets(8, 10, 8, 10);
        gbcTop.anchor = GridBagConstraints.WEST;

        JLabel agentInfoLabel = new JLabel("Agent: " + currentAgent.getFullName(), SwingConstants.LEFT);
        agentInfoLabel.setFont(UITheme.FONT_GENERAL_REGULAR);
        agentInfoLabel.setForeground(UITheme.COLOR_TEXT_MEDIUM);
        agentInfoLabel.setIcon(UITheme.getIcon("agent_profile.svg"));
        agentInfoLabel.setIconTextGap(8);
        gbcTop.gridx = 0; gbcTop.gridy = 0; gbcTop.gridwidth = 3;
        topControlsPanel.add(agentInfoLabel, gbcTop);

        gbcTop.gridy = 1; gbcTop.gridwidth = 1;
        JLabel serviceSelectLabel = new JLabel("Service Queue:");
        serviceSelectLabel.setFont(UITheme.FONT_LABEL);
        topControlsPanel.add(serviceSelectLabel, gbcTop);

        gbcTop.gridx = 1; gbcTop.fill = GridBagConstraints.HORIZONTAL; gbcTop.weightx = 1.0;
        topControlsPanel.add(serviceTypeComboBox, gbcTop);

        gbcTop.gridx = 2; gbcTop.fill = GridBagConstraints.NONE; gbcTop.weightx = 0.0; gbcTop.anchor = GridBagConstraints.EAST;
        topControlsPanel.add(callNextButton, gbcTop);

        JPanel middleSectionPanel = new JPanel(new BorderLayout(10,15));
        middleSectionPanel.setOpaque(false);
        middleSectionPanel.add(currentlyServingLabel, BorderLayout.NORTH);

        JPanel serviceActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        serviceActionPanel.setOpaque(false);
        serviceActionPanel.add(startServiceButton);
        serviceActionPanel.add(completeServiceButton);
        middleSectionPanel.add(serviceActionPanel, BorderLayout.CENTER);
        
        JPanel bottomInfoPanel = new JPanel(new BorderLayout());
        bottomInfoPanel.setOpaque(false);
        bottomInfoPanel.setBorder(new EmptyBorder(10,5,0,5));
        bottomInfoPanel.add(waitingCountLabel, BorderLayout.WEST);

        JPanel headerAndControls = new JPanel(new BorderLayout(10,10));
        headerAndControls.setOpaque(false);
        headerAndControls.add(topControlsPanel, BorderLayout.NORTH);
        headerAndControls.add(middleSectionPanel, BorderLayout.CENTER);
        headerAndControls.add(bottomInfoPanel, BorderLayout.SOUTH);

        cardPanel.add(headerAndControls, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(queueTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.COLOR_BORDER));
        scrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(cardPanel, BorderLayout.CENTER);
    }

    private void attachListeners() {
        serviceTypeComboBox.addActionListener(e -> updateQueueDisplay());

        callNextButton.addActionListener(e -> {
            ServiceType selectedService = (ServiceType) serviceTypeComboBox.getSelectedItem();
            if (selectedService != null) {
                Ticket calledTicket = queueManager.callNextTicket(selectedService, currentAgent);
                if (calledTicket == null) {
                    JOptionPane.showMessageDialog(this, "The queue for " + selectedService.getDisplayName() + " is empty.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        startServiceButton.addActionListener(e -> queueManager.startService(currentAgent.getUsername()));
        completeServiceButton.addActionListener(e -> {
            Ticket ticketToComplete = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());
            if (ticketToComplete != null && ticketToComplete.getServiceStartTime() == null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Service was not explicitly started. Complete anyway?",
                        "Confirm Completion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
            queueManager.completeService(currentAgent.getUsername());
        });
    }

    private void updateQueueDisplay() {
        ServiceType selectedService = (ServiceType) serviceTypeComboBox.getSelectedItem();
        
        queueTableModel.setRowCount(0);
        
        if (selectedService != null) {
            List<Ticket> queue = queueManager.getQueueSnapshot(selectedService);
            for (Ticket ticket : queue) {
                Vector<Object> row = new Vector<>();
                row.add(ticket.getTicketNumber());
                row.add(ticket.getCustomerName());
                row.add(ticket.getPriorityReason().getDisplayName());
                row.add(ticket.getFormattedIssueTime());
                row.add(ticket.getStatus());
                queueTableModel.addRow(row);
            }
            waitingCountLabel.setText("Waiting in \"" + selectedService.getDisplayName() + "\" queue: " + queue.size());
        } else {
            waitingCountLabel.setText("Waiting: 0");
        }

        Ticket agentServingTicket = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());
        if (agentServingTicket != null) {
            String startTimeStr = agentServingTicket.getServiceStartTime() != null ? agentServingTicket.getFormattedTime(agentServingTicket.getServiceStartTime()) : "Not Started";
            String priorityDisplay = (agentServingTicket.getPriorityReason() != Ticket.PriorityReason.NONE) ? "<br>Priority: " + agentServingTicket.getPriorityReason().getDisplayName() : "";
            currentlyServingLabel.setText(String.format("<html><div style='text-align: center;'>" +
                            "Serving: <b style='font-size:1.2em; color:" + UITheme.COLOR_PRIMARY_STEEL_BLUE_HEX() + ";'>%s</b><br>" +
                            "Service: %s %s<br>" +
                            "Status: %s (Started: %s)</div></html>",
                    agentServingTicket.getTicketNumber(),
                    agentServingTicket.getServiceType().getDisplayName(),
                    priorityDisplay,
                    agentServingTicket.getStatus(),
                    startTimeStr));
        } else {
            currentlyServingLabel.setText("<html><div style='text-align: center;'>Currently Serving: <b style='font-size:1.2em;'>---</b><br><span style='font-size:0.9em; color:" + UITheme.COLOR_TEXT_MEDIUM_HEX() + ";'>Select a service and 'Call Next'</span></div></html>");
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        Ticket ticketBeingServed = queueManager.getTicketBeingServedByAgent(currentAgent.getUsername());
        boolean isServing = ticketBeingServed != null;
        boolean servicesAvailable = serviceTypeComboBox.getItemCount() > 0;

        callNextButton.setEnabled(!isServing && servicesAvailable);
        startServiceButton.setEnabled(isServing && ticketBeingServed.getServiceStartTime() == null);
        completeServiceButton.setEnabled(isServing);
        serviceTypeComboBox.setEnabled(!isServing);
    }

    @Override
    public void onQueueUpdated() {
        SwingUtilities.invokeLater(() -> {
            loadServiceTypes();
            updateQueueDisplay();
        });
    }
}
