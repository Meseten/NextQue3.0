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
    private JTable ticketsTable;
    private DefaultTableModel ticketsTableModel;
    private JTextField ticketSearchField;
    private JButton changeTicketPriorityButton;
    private JTable feedbackTable;
    private DefaultTableModel feedbackTableModel;
    private JTextField feedbackSearchField;
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
        
        adminTabbedPane.addTab("All Tickets", UITheme.getIcon("tab_tickets.svg"), createTicketsPanel());
        adminTabbedPane.addTab("Customer Feedback", UITheme.getIcon("tab_feedback.svg"), createFeedbackPanel());
        adminTabbedPane.addTab("Manage Services", UITheme.getIcon("tab_services.svg"), createServicesPanel());

        add(adminTabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTicketsPanel() {
        JPanel panel = new CardPanel(new BorderLayout(10, 10));
        panel.setBorder(UITheme.BORDER_SECTION_PADDING);

        ticketsTableModel = new DefaultTableModel(
            new String[]{"Ticket No", "Service", "Customer", "Priority", "Issued", "Status", "Agent"}, 0){
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        ticketsTable = new JTable(ticketsTableModel);
        setupTableStyles(ticketsTable);
        
        TableColumnModel tcm = ticketsTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(110);
        tcm.getColumn(1).setPreferredWidth(140);
        tcm.getColumn(2).setPreferredWidth(130);
        tcm.getColumn(3).setPreferredWidth(130);
        tcm.getColumn(4).setPreferredWidth(70);
        tcm.getColumn(5).setPreferredWidth(90);
        tcm.getColumn(6).setPreferredWidth(90);

        ticketSearchField = new JTextField(25);
        ticketSearchField.setFont(UITheme.FONT_INPUT);
        ticketSearchField.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Search tickets...");
        JButton searchBtn = new JButton("Search", UITheme.getIcon("search.svg"));
        UITheme.styleSecondaryButton(searchBtn);

        changeTicketPriorityButton = new JButton("Set Priority", UITheme.getIcon("priority_star.svg"));
        UITheme.styleInfoButton(changeTicketPriorityButton);
        changeTicketPriorityButton.setEnabled(false);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("Filter:"));
        topPanel.add(ticketSearchField);
        topPanel.add(searchBtn);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(changeTicketPriorityButton);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(ticketsTable), BorderLayout.CENTER);
        
        searchBtn.addActionListener(this::filterTicketsTable);
        ticketSearchField.addActionListener(this::filterTicketsTable);
        changeTicketPriorityButton.addActionListener(this::changeSelectedTicketPriority);
        ticketsTable.getSelectionModel().addListSelectionListener(e -> updateTicketButtonState());
        
        return panel;
    }

    private JPanel createFeedbackPanel() {
        JPanel panel = new CardPanel(new BorderLayout(10, 10));
        panel.setBorder(UITheme.BORDER_SECTION_PADDING);

        feedbackTableModel = new DefaultTableModel(new String[]{"ID", "Ticket No", "Rating", "Comments", "Submitted"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        feedbackTable = new JTable(feedbackTableModel);
        setupTableStyles(feedbackTable);
        feedbackTable.getColumnModel().getColumn(3).setCellRenderer(new TextAreaCellRenderer());

        TableColumnModel tcm = feedbackTable.getColumnModel();
        tcm.getColumn(0).setMaxWidth(50);
        tcm.getColumn(1).setPreferredWidth(120);
        tcm.getColumn(2).setPreferredWidth(60);
        tcm.getColumn(3).setPreferredWidth(350);
        tcm.getColumn(4).setPreferredWidth(140);

        feedbackSearchField = new JTextField(25);
        feedbackSearchField.setFont(UITheme.FONT_INPUT);
        feedbackSearchField.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Search feedback...");
        JButton searchBtn = new JButton("Search", UITheme.getIcon("search.svg"));
        UITheme.styleSecondaryButton(searchBtn);
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("Filter:"));
        topPanel.add(feedbackSearchField);
        topPanel.add(searchBtn);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(feedbackTable), BorderLayout.CENTER);
        
        searchBtn.addActionListener(this::filterFeedbackTable);
        feedbackSearchField.addActionListener(this::filterFeedbackTable);

        return panel;
    }
    
    private JPanel createServicesPanel() {
        JPanel panel = new CardPanel(new BorderLayout(10, 10));
        panel.setBorder(UITheme.BORDER_SECTION_PADDING);

        serviceListModel = new DefaultListModel<>();
        serviceTypeList = new JList<>(serviceListModel);
        serviceTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTypeList.setFont(UITheme.FONT_LIST_ITEM);
        serviceTypeList.setCellRenderer(new ServiceTypeListCellRenderer());
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controls.setOpaque(false);
        addServiceButton = new JButton("Add Service", UITheme.getIcon("add_circle.svg"));
        UITheme.styleSuccessButton(addServiceButton);
        editServiceButton = new JButton("Edit Name", UITheme.getIcon("edit_pencil.svg"));
        UITheme.styleInfoButton(editServiceButton);
        removeServiceButton = new JButton("Remove", UITheme.getIcon("delete_trash.svg"));
        UITheme.styleDangerButton(removeServiceButton);
        controls.add(addServiceButton);
        controls.add(editServiceButton);
        controls.add(removeServiceButton);

        panel.add(new JScrollPane(serviceTypeList), BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);
        
        serviceTypeList.addListSelectionListener(e -> updateServiceButtonState());
        addServiceButton.addActionListener(this::addServiceTypeAction);
        editServiceButton.addActionListener(this::editServiceTypeAction);
        removeServiceButton.addActionListener(this::removeServiceTypeAction);
        
        updateServiceButtonState();
        
        return panel;
    }

    private void addServiceTypeAction(ActionEvent e) {
        JTextField internalNameField = new JTextField(20);
        JTextField displayNameField = new JTextField(20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Internal Name (NO SPACES, e.g., NEW_APP):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(internalNameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Display Name (e.g., New Application):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(displayNameField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Service Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = internalNameField.getText().trim().toUpperCase();
            String displayName = displayNameField.getText().trim();

            if (name.isEmpty() || displayName.isEmpty() || name.contains(" ")) {
                JOptionPane.showMessageDialog(this, "Names cannot be empty. Internal name cannot contain spaces.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (dbManager.findServiceTypeByName(name).isPresent()) {
                JOptionPane.showMessageDialog(this, "A service with the internal name '" + name + "' already exists.", "Duplicate Service Name", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dbManager.addServiceType(name, displayName);
            queueManager.servicesConfigurationChanged();
        }
    }

    private void editServiceTypeAction(ActionEvent e) {
        ServiceType selected = serviceTypeList.getSelectedValue();
        if (selected == null) return;

        String newDisplayName = JOptionPane.showInputDialog(this, "Enter new display name for " + selected.getName() + ":", selected.getDisplayName());

        if (newDisplayName != null && !newDisplayName.trim().isEmpty()) {
            dbManager.updateServiceTypeDisplayName(selected.getName(), newDisplayName.trim());
            queueManager.servicesConfigurationChanged();
        }
    }

    private void removeServiceTypeAction(ActionEvent e) {
        ServiceType selected = serviceTypeList.getSelectedValue();
        if (selected == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to PERMANENTLY REMOVE the service:\n'" + selected.getDisplayName() + "'?",
            "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (!dbManager.removeServiceType(selected.getName())) {
                JOptionPane.showMessageDialog(this, "Failed to remove service. It might be in use by existing tickets.", "Removal Failed", JOptionPane.ERROR_MESSAGE);
            } else {
                queueManager.servicesConfigurationChanged();
            }
        }
    }

    private void changeSelectedTicketPriority(ActionEvent e) {
        int selectedRow = ticketsTable.convertRowIndexToModel(ticketsTable.getSelectedRow());
        if (selectedRow == -1) return;
        
        String ticketNumber = ticketsTableModel.getValueAt(selectedRow, 0).toString();

        Ticket.PriorityReason newReason = (Ticket.PriorityReason) JOptionPane.showInputDialog(
            this, "Set Priority for Ticket: " + ticketNumber, "Update Priority",
            JOptionPane.PLAIN_MESSAGE, null,
            Ticket.PriorityReason.values(), Ticket.PriorityReason.NONE);
            
        if (newReason != null) {
            queueManager.updateTicketPriority(ticketNumber, newReason);
        }
    }

    private void loadAllData() {
        loadTickets();
        loadFeedback();
        loadServiceTypesForAdminList();
    }

    private void loadServiceTypesForAdminList() {
        serviceListModel.clear();
        dbManager.getAllServiceTypes().forEach(serviceListModel::addElement);
    }

    private void loadTickets() {
        ticketsTableModel.setRowCount(0);
        List<Ticket> tickets = dbManager.getAllTicketsWithResolvedServiceTypes();
        for (Ticket t : tickets) {
            Vector<Object> row = new Vector<>();
            row.add(t.getTicketNumber());
            row.add(t.getServiceType().getDisplayName());
            row.add(t.getCustomerName());
            row.add(t.getPriorityReason().getDisplayName());
            row.add(t.getFormattedIssueTime());
            row.add(t.getStatus());
            row.add(t.getAgentUsername() != null ? t.getAgentUsername() : "---");
            ticketsTableModel.addRow(row);
        }
    }
    
    private void loadFeedback() {
        feedbackTableModel.setRowCount(0);
        dbManager.getAllFeedback().forEach(f -> {
            Vector<Object> row = new Vector<>();
            row.add(f.getId());
            row.add(f.getTicketNumber());
            row.add(f.getRating());
            row.add(f.getComments());
            row.add(f.getFormattedSubmissionTime());
            feedbackTableModel.addRow(row);
        });
    }
    
    private void filterTicketsTable(ActionEvent e) {
        filterTable(ticketsTable, ticketSearchField.getText());
    }
    
    private void filterFeedbackTable(ActionEvent e) {
        filterTable(feedbackTable, feedbackSearchField.getText());
    }

    private void filterTable(JTable table, String text) {
        TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
        if (text.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
        }
    }
    
    private void updateTicketButtonState() {
        if (ticketsTable.getSelectedRow() == -1) {
            changeTicketPriorityButton.setEnabled(false);
            return;
        }
        int modelRow = ticketsTable.convertRowIndexToModel(ticketsTable.getSelectedRow());
        String status = ticketsTableModel.getValueAt(modelRow, 5).toString();
        changeTicketPriorityButton.setEnabled(Ticket.TicketStatus.WAITING.name().equals(status));
    }
    
    private void updateServiceButtonState() {
        boolean selected = serviceTypeList.getSelectedIndex() != -1;
        editServiceButton.setEnabled(selected);
        removeServiceButton.setEnabled(selected);
    }

    private void setupTableStyles(JTable table) {
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(UITheme.FONT_TABLE_HEADER);
        table.getTableHeader().setBackground(UITheme.COLOR_PRIMARY_STEEL_BLUE);
        table.getTableHeader().setForeground(UITheme.COLOR_TEXT_ON_PRIMARY);
        table.setFont(UITheme.FONT_TABLE_CELL);
        table.setRowHeight(28);
        table.setGridColor(UITheme.COLOR_BORDER);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public void onQueueUpdated() {
         SwingUtilities.invokeLater(this::loadAllData);
    }

    private static class ServiceTypeListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ServiceType) {
                ServiceType st = (ServiceType) value;
                setText("<html><body style='padding: 3px 0px;'><b style='color:" + UITheme.COLOR_PRIMARY_NAVY_HEX() + ";'>" +
                        st.getDisplayName() +
                        "</b> <font color='" + UITheme.COLOR_TEXT_LIGHT_HEX() + "'>(" + st.getName() + ")</font></body></html>");
                setIcon(UITheme.getIcon("service_item_list.svg"));
            }
            setBorder(new EmptyBorder(6,8,6,8));
            return this;
        }
    }

    private static class TextAreaCellRenderer extends JTextArea implements TableCellRenderer {
        public TextAreaCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setFont(UITheme.FONT_TABLE_CELL);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
                setForeground(table.getForeground());
            }
            return this;
        }
    }
}
