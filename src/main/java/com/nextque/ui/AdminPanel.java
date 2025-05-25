// Filename: AdminPanel.java
package com.nextque.ui;

import com.nextque.db.DatabaseManager;
import com.nextque.model.Feedback;
import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.service.QueueManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

public class AdminPanel extends JPanel implements QueueManager.QueueUpdateListener {
    private final DatabaseManager dbManager;
    private final QueueManager queueManager;

    private JTabbedPane adminTabbedPane;

    // Tickets Tab
    private JTable ticketsTable;
    private DefaultTableModel ticketsTableModel;
    private JTextField ticketSearchField;
    private JButton changeTicketPriorityButton;

    // Feedback Tab
    private JTable feedbackTable;
    private DefaultTableModel feedbackTableModel;
    private JTextField feedbackSearchField;

    // Services Tab
    private JList<ServiceType> serviceTypeList;
    private DefaultListModel<ServiceType> serviceListModel;
    private JButton addServiceButton, editServiceButton, removeServiceButton;


    public AdminPanel(DatabaseManager dbManager, QueueManager queueManager) {
        this.dbManager = dbManager;
        this.queueManager = queueManager;
        this.queueManager.addQueueUpdateListener(this);

        setLayout(new BorderLayout(10,10));
        setBorder(UITheme.BORDER_PANEL_PADDING);
        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        initComponents();
        loadAllData();
    }

    private void initComponents() {
        adminTabbedPane = new JTabbedPane();
        adminTabbedPane.setTabPlacement(JTabbedPane.TOP);
        adminTabbedPane.setFont(UITheme.FONT_GENERAL_BOLD);
        adminTabbedPane.setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        adminTabbedPane.setForeground(UITheme.COLOR_TEXT_DARK);


        Font searchLabelFont = UITheme.FONT_LABEL;
        Font tableHeaderFont = UITheme.FONT_TABLE_HEADER;
        Font tableCellFont = UITheme.FONT_TABLE_CELL;

        // --- Tickets Tab ---
        JPanel ticketsPanel = new JPanel(new BorderLayout(10, 10));
        ticketsPanel.setBorder(UITheme.BORDER_SECTION_PADDING);
        ticketsPanel.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        ticketsPanel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 10");

        // Added "Priority Reason" column
        ticketsTableModel = new DefaultTableModel(
            new String[]{"Ticket No", "Service", "Customer", "Priority Reason", "Issued", "Called", "Svc Start", "Svc End", "Status", "Agent", "Num. Pri"}, 0){
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        ticketsTable = new JTable(ticketsTableModel);
        ticketsTable.setFillsViewportHeight(true);
        ticketsTable.setAutoCreateRowSorter(true);
        ticketsTable.getTableHeader().setFont(tableHeaderFont);
        // Ensure UITheme.COLOR_PRIMARY_MEDIUM is defined and initialized in UITheme.java
        ticketsTable.getTableHeader().setBackground(UITheme.COLOR_PRIMARY_MEDIUM);
        ticketsTable.getTableHeader().setForeground(UITheme.COLOR_TEXT_ON_PRIMARY);
        ticketsTable.setFont(tableCellFont);
        ticketsTable.setRowHeight(24);
        ticketsTable.setGridColor(UITheme.COLOR_BORDER);
        ticketsTable.setShowVerticalLines(false);
        ticketsTable.setIntercellSpacing(new Dimension(0, 1));
        ticketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Allow selecting one ticket


        TableColumnModel tcmTickets = ticketsTable.getColumnModel();
        tcmTickets.getColumn(0).setPreferredWidth(110); // Ticket No
        tcmTickets.getColumn(1).setPreferredWidth(140); // Service
        tcmTickets.getColumn(2).setPreferredWidth(130); // Customer
        tcmTickets.getColumn(3).setPreferredWidth(130); // Priority Reason (New)
        tcmTickets.getColumn(4).setPreferredWidth(70);  // Issued
        tcmTickets.getColumn(5).setPreferredWidth(70);  // Called
        tcmTickets.getColumn(6).setPreferredWidth(70);  // Svc Start
        tcmTickets.getColumn(7).setPreferredWidth(70);  // Svc End
        tcmTickets.getColumn(8).setPreferredWidth(90);  // Status
        tcmTickets.getColumn(9).setPreferredWidth(90); // Agent
        tcmTickets.getColumn(10).setPreferredWidth(60);  // Num. Pri


        ticketSearchField = new JTextField(25);
        ticketSearchField.setFont(UITheme.FONT_INPUT);
        ticketSearchField.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Search tickets...");
        JButton ticketSearchButton = new JButton("Search");
        UITheme.styleSecondaryButton(ticketSearchButton);
        ticketSearchButton.setIcon(UITheme.getIcon("search.svg")); // Ensure icon exists
        ticketSearchButton.addActionListener(this::filterTicketsTable);
        ticketSearchField.addActionListener(this::filterTicketsTable);

        changeTicketPriorityButton = new JButton("Set Priority");
        UITheme.styleInfoButton(changeTicketPriorityButton);
        changeTicketPriorityButton.setIcon(UITheme.getIcon("priority_star.svg")); // Ensure icon exists
        changeTicketPriorityButton.setEnabled(false); // Enabled when a WAITING ticket is selected
        changeTicketPriorityButton.addActionListener(this::changeSelectedTicketPriority);

        ticketsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = ticketsTable.getSelectedRow();
                if (selectedRow != -1) {
                    int modelRow = ticketsTable.convertRowIndexToModel(selectedRow);
                    String status = ticketsTableModel.getValueAt(modelRow, 8).toString(); // Status column
                    changeTicketPriorityButton.setEnabled(Ticket.TicketStatus.WAITING.name().equals(status));
                } else {
                    changeTicketPriorityButton.setEnabled(false);
                }
            }
        });


        JPanel ticketSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        ticketSearchPanel.setOpaque(false);
        JLabel ticketSearchLabel = new JLabel("Filter:");
        ticketSearchLabel.setFont(searchLabelFont);
        ticketSearchPanel.add(ticketSearchLabel);
        ticketSearchPanel.add(ticketSearchField);
        ticketSearchPanel.add(ticketSearchButton);
        ticketSearchPanel.add(Box.createHorizontalStrut(10)); // Spacer
        ticketSearchPanel.add(changeTicketPriorityButton);

        ticketsPanel.add(ticketSearchPanel, BorderLayout.NORTH);
        JScrollPane ticketScrollPane = new JScrollPane(ticketsTable);
        ticketScrollPane.setBorder(BorderFactory.createLineBorder(UITheme.COLOR_BORDER));
        ticketScrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        ticketsPanel.add(ticketScrollPane, BorderLayout.CENTER);
        adminTabbedPane.addTab("All Tickets", UITheme.getIcon("tab_tickets.svg"), ticketsPanel); // Ensure icon exists

        // --- Feedback Tab ---
        JPanel feedbackPanel = new JPanel(new BorderLayout(10, 10));
        feedbackPanel.setBorder(UITheme.BORDER_SECTION_PADDING);
        feedbackPanel.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        feedbackPanel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 10");

        feedbackTableModel = new DefaultTableModel(new String[]{"ID", "Ticket No", "Rating", "Comments", "Submitted"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        feedbackTable = new JTable(feedbackTableModel);
        feedbackTable.setFillsViewportHeight(true);
        feedbackTable.setAutoCreateRowSorter(true);
        feedbackTable.getTableHeader().setFont(tableHeaderFont);
        // Ensure UITheme.COLOR_PRIMARY_MEDIUM is defined and initialized in UITheme.java
        feedbackTable.getTableHeader().setBackground(UITheme.COLOR_PRIMARY_MEDIUM);
        feedbackTable.getTableHeader().setForeground(UITheme.COLOR_TEXT_ON_PRIMARY);
        feedbackTable.setFont(tableCellFont);
        feedbackTable.setRowHeight(24);
        feedbackTable.setGridColor(UITheme.COLOR_BORDER);
        feedbackTable.setShowVerticalLines(false);
        feedbackTable.setIntercellSpacing(new Dimension(0, 1));

        TableColumnModel tcmFeedback = feedbackTable.getColumnModel();
        tcmFeedback.getColumn(0).setPreferredWidth(40);
        tcmFeedback.getColumn(1).setPreferredWidth(120);
        tcmFeedback.getColumn(2).setPreferredWidth(60);
        tcmFeedback.getColumn(3).setPreferredWidth(350);
        tcmFeedback.getColumn(4).setPreferredWidth(140);
        feedbackTable.getColumnModel().getColumn(3).setCellRenderer(new TextAreaCellRenderer());

        feedbackSearchField = new JTextField(25);
        feedbackSearchField.setFont(UITheme.FONT_INPUT);
        feedbackSearchField.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Search feedback...");
        JButton feedbackSearchButton = new JButton("Search");
        UITheme.styleSecondaryButton(feedbackSearchButton);
        feedbackSearchButton.setIcon(UITheme.getIcon("search.svg")); // Ensure icon exists
        feedbackSearchButton.addActionListener(this::filterFeedbackTable);
        feedbackSearchField.addActionListener(this::filterFeedbackTable);

        JPanel feedbackSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        feedbackSearchPanel.setOpaque(false);
        JLabel feedbackSearchLabel = new JLabel("Filter:");
        feedbackSearchLabel.setFont(searchLabelFont);
        feedbackSearchPanel.add(feedbackSearchLabel);
        feedbackSearchPanel.add(feedbackSearchField);
        feedbackSearchPanel.add(feedbackSearchButton);
        feedbackPanel.add(feedbackSearchPanel, BorderLayout.NORTH);
        JScrollPane feedbackScrollPane = new JScrollPane(feedbackTable);
        feedbackScrollPane.setBorder(BorderFactory.createLineBorder(UITheme.COLOR_BORDER));
        feedbackScrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        feedbackPanel.add(feedbackScrollPane, BorderLayout.CENTER);
        adminTabbedPane.addTab("Customer Feedback", UITheme.getIcon("tab_feedback.svg"), feedbackPanel); // Ensure icon exists

        // --- Service Management Tab ---
        JPanel servicesPanel = new JPanel(new BorderLayout(10, 10));
        servicesPanel.setBorder(UITheme.BORDER_SECTION_PADDING);
        servicesPanel.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        servicesPanel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 10");

        serviceListModel = new DefaultListModel<>();
        serviceTypeList = new JList<>(serviceListModel);
        serviceTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTypeList.setFont(UITheme.FONT_LIST_ITEM);
        serviceTypeList.setCellRenderer(new ServiceTypeListCellRenderer());
        serviceTypeList.setBackground(UITheme.COLOR_BACKGROUND_SECTION);
        JScrollPane serviceListScrollPane = new JScrollPane(serviceTypeList);
        serviceListScrollPane.setBorder(BorderFactory.createLineBorder(UITheme.COLOR_BORDER));
        servicesPanel.add(serviceListScrollPane, BorderLayout.CENTER);

        JPanel serviceControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        serviceControlPanel.setOpaque(false);
        addServiceButton = new JButton("Add Service");
        UITheme.styleSuccessButton(addServiceButton);
        addServiceButton.setIcon(UITheme.getIcon("add_circle.svg")); // Ensure icon exists
        editServiceButton = new JButton("Edit Display Name");
        UITheme.styleInfoButton(editServiceButton);
        editServiceButton.setIcon(UITheme.getIcon("edit_pencil.svg")); // Ensure icon exists
        removeServiceButton = new JButton("Remove Selected");
        UITheme.styleDangerButton(removeServiceButton);
        removeServiceButton.setIcon(UITheme.getIcon("delete_trash.svg")); // Ensure icon exists

        serviceTypeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selected = serviceTypeList.getSelectedIndex() != -1;
                editServiceButton.setEnabled(selected);
                removeServiceButton.setEnabled(selected);
            }
        });
        editServiceButton.setEnabled(false);
        removeServiceButton.setEnabled(false);

        addServiceButton.addActionListener(this::addServiceTypeAction);
        editServiceButton.addActionListener(this::editServiceTypeAction);
        removeServiceButton.addActionListener(this::removeServiceTypeAction);

        serviceControlPanel.add(addServiceButton);
        serviceControlPanel.add(editServiceButton);
        serviceControlPanel.add(removeServiceButton);
        servicesPanel.add(serviceControlPanel, BorderLayout.SOUTH);
        adminTabbedPane.addTab("Manage Services", UITheme.getIcon("tab_services.svg"), servicesPanel); // Ensure icon exists

        add(adminTabbedPane, BorderLayout.CENTER);
    }

    private void addServiceTypeAction(ActionEvent e) {
        JTextField internalNameField = new JTextField(20);
        internalNameField.setFont(UITheme.FONT_INPUT);
        JTextField displayNameField = new JTextField(20);
        displayNameField.setFont(UITheme.FONT_INPUT);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcDialog = new GridBagConstraints();
        gbcDialog.insets = new Insets(5,5,5,5);
        gbcDialog.fill = GridBagConstraints.HORIZONTAL;
        gbcDialog.anchor = GridBagConstraints.WEST;

        gbcDialog.gridx = 0; gbcDialog.gridy = 0;
        panel.add(new JLabel("Internal Name (e.g., NEW_DOCS, A-Z_0-9):"), gbcDialog);
        gbcDialog.gridx = 1; gbcDialog.gridy = 0;
        panel.add(internalNameField, gbcDialog);

        gbcDialog.gridx = 0; gbcDialog.gridy = 1;
        panel.add(new JLabel("Display Name (e.g., New Documents):"), gbcDialog);
        gbcDialog.gridx = 1; gbcDialog.gridy = 1;
        panel.add(displayNameField, gbcDialog);

        UIManager.put("OptionPane.messageFont", UITheme.FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", UITheme.FONT_BUTTON);
        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Service Type",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newServiceName = internalNameField.getText().trim();
            String newServiceDisplayName = displayNameField.getText().trim();

            if (newServiceName.isEmpty() || newServiceDisplayName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Both internal name and display name are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newServiceName.matches("[A-Z_0-9]+")) {
                JOptionPane.showMessageDialog(this, "Internal name must be uppercase letters, numbers, and underscores only.", "Invalid Name Format", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String internalNameUpper = newServiceName.toUpperCase();
                boolean exists = dbManager.getAllServiceTypes().stream()
                                    .anyMatch(st -> st.name().equalsIgnoreCase(internalNameUpper));
                if (exists) {
                    JOptionPane.showMessageDialog(this, "A service with the internal name '" + internalNameUpper + "' already exists.", "Duplicate Service Name", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                dbManager.addServiceType(internalNameUpper, newServiceDisplayName);
                loadServiceTypesForAdminList();
                queueManager.onQueueUpdated(); // Notify all listeners including AgentPanel, CustomerPanel etc.
                JOptionPane.showMessageDialog(this, "Service type '" + newServiceDisplayName + "' added successfully.", "Service Added", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding service: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void editServiceTypeAction(ActionEvent e) {
        ServiceType selectedService = serviceTypeList.getSelectedValue();
        if (selectedService == null) {
            JOptionPane.showMessageDialog(this, "Please select a service to edit.", "No Service Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField displayNameField = new JTextField(selectedService.getDisplayName(), 20);
        displayNameField.setFont(UITheme.FONT_INPUT);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("New Display Name for " + selectedService.name() + ":"));
        panel.add(displayNameField);

        UIManager.put("OptionPane.messageFont", UITheme.FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", UITheme.FONT_BUTTON);
        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Service Display Name",
                                                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newDisplayName = displayNameField.getText().trim();
            if (!newDisplayName.isEmpty() && !newDisplayName.equals(selectedService.getDisplayName())) {
                if (dbManager.updateServiceTypeDisplayName(selectedService.name(), newDisplayName)) {
                    loadServiceTypesForAdminList();
                    queueManager.onQueueUpdated(); // Notify all listeners
                    JOptionPane.showMessageDialog(this, "Display name for '" + selectedService.name() + "' updated successfully.", "Service Updated", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update display name for '" + selectedService.name() + "'.", "Update Failed", JOptionPane.ERROR_MESSAGE);
                }
            } else if (newDisplayName.isEmpty()){
                JOptionPane.showMessageDialog(this, "Display name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeServiceTypeAction(ActionEvent e) {
        ServiceType selectedService = serviceTypeList.getSelectedValue();
        if (selectedService == null) {
            JOptionPane.showMessageDialog(this, "Please select a service to remove.", "No Service Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UIManager.put("OptionPane.messageFont", UITheme.FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", UITheme.FONT_BUTTON);
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Are you sure you want to <b>PERMANENTLY REMOVE</b> the service:<br>'" +
            selectedService.getDisplayName() + "' (Internal: " + selectedService.name() + ")?<br><br>" +
            "<b>This action cannot be undone.</b><br>" +
            "This will fail if the service is currently associated with any tickets.</html>",
            "Confirm Permanent Removal",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (dbManager.removeServiceType(selectedService.name())) {
                loadServiceTypesForAdminList();
                queueManager.onQueueUpdated(); // Notify all listeners
                JOptionPane.showMessageDialog(this, "Service '" + selectedService.getDisplayName() + "' removed successfully.", "Service Removed", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove service '" + selectedService.getDisplayName() + "'. It might be in use by existing tickets or another error occurred.", "Removal Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changeSelectedTicketPriority(ActionEvent e) {
        int selectedRow = ticketsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a ticket from the table first.", "No Ticket Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = ticketsTable.convertRowIndexToModel(selectedRow);
        String ticketNumber = ticketsTableModel.getValueAt(modelRow, 0).toString();
        String currentStatusStr = ticketsTableModel.getValueAt(modelRow, 8).toString(); // Status column

        if (!Ticket.TicketStatus.WAITING.name().equals(currentStatusStr)) {
             JOptionPane.showMessageDialog(this, "Priority can only be changed for tickets that are currently WAITING.", "Invalid Ticket Status", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get current priority reason to pre-select in dialog
        Ticket.PriorityReason currentReason = Ticket.PriorityReason.NONE; // Default
        String currentReasonStr = ticketsTableModel.getValueAt(modelRow, 3).toString();
        for (Ticket.PriorityReason reason : Ticket.PriorityReason.values()) {
            if (reason.getDisplayName().equals(currentReasonStr)) {
                currentReason = reason;
                break;
            }
        }

        JComboBox<Ticket.PriorityReason> priorityComboBox = new JComboBox<>(Ticket.PriorityReason.values());
        priorityComboBox.setSelectedItem(currentReason);
        priorityComboBox.setFont(UITheme.FONT_INPUT);

        UIManager.put("OptionPane.messageFont", UITheme.FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", UITheme.FONT_BUTTON);
        int result = JOptionPane.showConfirmDialog(this, priorityComboBox, "Set Priority for Ticket: " + ticketNumber,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Ticket.PriorityReason newReason = (Ticket.PriorityReason) priorityComboBox.getSelectedItem();
            if (queueManager.updateTicketPriority(ticketNumber, newReason)) {
                JOptionPane.showMessageDialog(this, "Priority for ticket " + ticketNumber + " updated to: " + newReason.getDisplayName(), "Priority Updated", JOptionPane.INFORMATION_MESSAGE);
                loadTickets(); // Refresh the tickets table
                queueManager.onQueueUpdated(); // Notify other listeners (like AgentPanel)
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update priority for ticket " + ticketNumber + ".", "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadAllData() {
        loadTickets();
        loadFeedback();
        loadServiceTypesForAdminList();
    }

    private void loadServiceTypesForAdminList() {
        serviceListModel.clear();
        List<ServiceType> typesFromDb = dbManager.getAllServiceTypes();
        // If DB is empty (e.g. first run), populate from Enum as a fallback or initial set.
        // However, typically, services should be managed purely via DB after initial setup.
        // This logic might need refinement based on desired behavior for an empty DB.
        if (typesFromDb.isEmpty() && ServiceType.values().length > 0 && dbManager.isSchemaJustCreated()) { // Condition for initial population
            System.out.println("AdminPanel: No service types in DB, populating from Enum (initial setup).");
            for (ServiceType type : ServiceType.getInitialServiceTypes()) { // Assuming a method to get only initial/default types
                serviceListModel.addElement(type);
                // Optionally, add them to DB here if they aren't already during schema creation
                // dbManager.addServiceType(type.name(), type.getDisplayName()); // Be careful about duplicates
            }
        } else {
            for (ServiceType type : typesFromDb) {
                serviceListModel.addElement(type);
            }
        }
    }

    private void loadTickets() {
        ticketsTableModel.setRowCount(0);
        List<Ticket> tickets = dbManager.getAllTickets();
        for (Ticket t : tickets) {
            Vector<Object> row = new Vector<>();
            row.add(t.getTicketNumber());
            row.add(t.getServiceType().getDisplayName());
            row.add(t.getCustomerName());
            row.add(t.getPriorityReason().getDisplayName()); // Display priority reason
            row.add(t.getFormattedIssueTime());
            row.add(t.getCallTime() != null ? t.getFormattedTime(t.getCallTime()) : "---");
            row.add(t.getServiceStartTime() != null ? t.getFormattedTime(t.getServiceStartTime()) : "---");
            row.add(t.getServiceEndTime() != null ? t.getFormattedTime(t.getServiceEndTime()) : "---");
            row.add(t.getStatus());
            row.add(t.getAgentUsername() != null ? t.getAgentUsername() : "---");
            row.add(t.getPriority()); // Numerical priority
            ticketsTableModel.addRow(row);
        }
    }

    @SuppressWarnings("unchecked")
    private void filterTicketsTable(ActionEvent e) {
        String searchText = ticketSearchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) ticketsTable.getRowSorter();
        if (searchText.length() == 0) {
            sorter.setRowFilter(null);
        } else {
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(searchText)));
            } catch (java.util.regex.PatternSyntaxException pse) {
                System.err.println("Invalid regex pattern in ticket search: " + pse.getMessage());
                sorter.setRowFilter(null); // Clear filter on error
            }
        }
    }

    private void loadFeedback() {
        feedbackTableModel.setRowCount(0);
        List<Feedback> feedbackList = dbManager.getAllFeedback();
        for (Feedback f : feedbackList) {
            Vector<Object> row = new Vector<>();
            row.add(f.getId());
            row.add(f.getTicketNumber());
            row.add(f.getRating());
            row.add(f.getComments());
            row.add(f.getFormattedSubmissionTime());
            feedbackTableModel.addRow(row);
        }
    }

    @SuppressWarnings("unchecked")
    private void filterFeedbackTable(ActionEvent e) {
        String searchText = feedbackSearchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) feedbackTable.getRowSorter();
        if (searchText.length() == 0) {
            sorter.setRowFilter(null);
        } else {
             try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(searchText)));
            } catch (java.util.regex.PatternSyntaxException pse) {
                System.err.println("Invalid regex pattern in feedback search: " + pse.getMessage());
                sorter.setRowFilter(null); // Clear filter on error
            }
        }
    }

    @Override
    public void onQueueUpdated() {
         SwingUtilities.invokeLater(() -> {
            // Save current selection in tickets table if any
            int selectedTicketRow = ticketsTable.getSelectedRow();
            String selectedTicketNumber = null;
            if (selectedTicketRow != -1) {
                selectedTicketNumber = ticketsTable.getValueAt(selectedTicketRow, ticketsTable.convertColumnIndexToView(0)).toString();
            }

            loadTickets(); // Reload all tickets
            loadServiceTypesForAdminList(); // Reload service types

            // Try to reselect the previously selected ticket
            if (selectedTicketNumber != null) {
                for (int i = 0; i < ticketsTable.getRowCount(); i++) {
                    if (ticketsTable.getValueAt(i, ticketsTable.convertColumnIndexToView(0)).toString().equals(selectedTicketNumber)) {
                        ticketsTable.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
            // Note: Feedback is not directly affected by queue updates, so not reloading it here unless necessary.
        });
    }

    // Custom cell renderer for JList to show ServiceType display name and internal name
    private static class ServiceTypeListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ServiceType) {
                ServiceType serviceType = (ServiceType) value;
                // Using UITheme HEX methods for HTML styling
                setText("<html><body style='width: 200px; padding: 3px 0px;'><b style='color:" + UITheme.COLOR_PRIMARY_NAVY_HEX() + ";'>" +
                        serviceType.getDisplayName() +
                        "</b> <font color='" + UITheme.COLOR_TEXT_LIGHT_HEX() + "'>(" + serviceType.name() + ")</font></body></html>");
                setIcon(UITheme.getIcon("service_item_list.svg")); // Ensure icon exists
            }
            setFont(UITheme.FONT_LIST_ITEM);
            setBorder(new EmptyBorder(6,8,6,8)); // Padding for each item
            if (isSelected) {
                setBackground(UITheme.COLOR_PRIMARY_LIGHT_SKY); // Use a theme color for selection
                setForeground(UITheme.COLOR_PRIMARY_NAVY); // Contrasting text for selection
            } else {
                setBackground(UITheme.COLOR_BACKGROUND_SECTION); // Default item background
                setForeground(UITheme.COLOR_TEXT_DARK); // Default item text color
            }
            return this;
        }
    }

    // Custom cell renderer for JTable to allow multi-line text in cells (e.g., feedback comments)
    private static class TextAreaCellRenderer extends JTextArea implements TableCellRenderer {
        public TextAreaCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(new EmptyBorder(5, 5, 5, 5)); // Padding within the cell
            setFont(UITheme.FONT_TABLE_CELL); // Use consistent table cell font
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText((value == null) ? "" : value.toString());

            // Handle selection and striping colors
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                // Apply striping if enabled in UIManager
                if (UIManager.getBoolean("Table.striped") && row % 2 == 1) {
                    setBackground(UIManager.getColor("Table.alternateRowColor"));
                } else {
                    setBackground(table.getBackground());
                }
                setForeground(table.getForeground());
            }
            // Adjust height of row to fit content - This is tricky with JTable's default row height.
            // For dynamic row heights based on content, more complex logic or custom TableUI might be needed.
            // A simpler approach is to set a taller fixed row height if comments are often long.
            // table.setRowHeight(row, getPreferredSize().height); // This can be problematic and slow.
            return this;
        }
    }
}
