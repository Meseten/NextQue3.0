// Filename: DatabaseManager.java
package com.nextque.db;

import com.nextque.model.ServiceType;
import com.nextque.model.Ticket;
import com.nextque.model.User;
import com.nextque.model.UserRole;
import com.nextque.model.Feedback;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:nextque.db";
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Flag to indicate if default service types were added in this session (implies new/empty schema)
    private boolean schemaWasPopulatedWithDefaultsThisSession = false;

    public boolean isSchemaJustCreated() {
        return schemaWasPopulatedWithDefaultsThisSession;
    }

    public static class ServiceTypeRecord {
        private final String name;
        private final String displayName;
        public ServiceTypeRecord(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
    }

    public DatabaseManager() {
        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        // Consider configuring connection properties if needed, e.g., foreign_keys=ON for SQLite
        Connection conn = DriverManager.getConnection(DB_URL);
        // For SQLite, it's good practice to enable foreign key constraints explicitly if not enabled by default
        // try (Statement stmt = conn.createStatement()) {
        //     stmt.execute("PRAGMA foreign_keys = ON;");
        // }
        return conn;
    }

    public void initializeDatabase() {
        String createUserTable = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "fullName TEXT" +
                ");";
        String createServiceTypeTable = "CREATE TABLE IF NOT EXISTS service_types (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "displayName TEXT NOT NULL" +
                ");";
        String createTicketTable = "CREATE TABLE IF NOT EXISTS tickets (" +
                "ticketNumber TEXT PRIMARY KEY," +
                "serviceTypeName TEXT NOT NULL," +
                "customerName TEXT," +
                "issueTime TEXT NOT NULL," +
                "callTime TEXT," +
                "serviceStartTime TEXT," +
                "serviceEndTime TEXT," +
                "status TEXT NOT NULL, " +
                "priority INTEGER DEFAULT 0," +
                "priorityReason TEXT DEFAULT 'NONE'," +
                "agentUsername TEXT," +
                "FOREIGN KEY (serviceTypeName) REFERENCES service_types(name) ON DELETE RESTRICT ON UPDATE CASCADE" + // Added FK constraints
                ");";
        String createFeedbackTable = "CREATE TABLE IF NOT EXISTS feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ticketNumber TEXT," +
                "rating INTEGER," +
                "comments TEXT," +
                "submissionTime TEXT NOT NULL," +
                "FOREIGN KEY (ticketNumber) REFERENCES tickets(ticketNumber) ON DELETE SET NULL ON UPDATE CASCADE" + // Added FK constraints
                ");";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                LOGGER.info("Initializing database schema (with priorityReason in tickets)...");
                stmt.execute(createUserTable);
                stmt.execute(createServiceTypeTable);
                stmt.execute(createTicketTable);
                stmt.execute(createFeedbackTable);
            }

            addDefaultUserIfNotExists(conn, "admin", "admin123", UserRole.ADMIN, "System Administrator");
            addDefaultUserIfNotExists(conn, "agent1", "agent123", UserRole.AGENT, "Default Agent");
            // addDefaultServiceTypesIfEmpty will set the schemaWasPopulatedWithDefaultsThisSession flag
            addDefaultServiceTypesIfEmpty(conn); 

            conn.commit();
            LOGGER.info("Database schema initialization complete and committed.");

        } catch (SQLException e) {
            LOGGER.error("CRITICAL: Database initialization error: {}.", e.getMessage(), e);
            // Consider how to handle this critical failure. Application might not be usable.
        }
    }

    private void addDefaultUserIfNotExists(Connection conn, String username, String password, UserRole role, String fullName) throws SQLException {
        if (getUser(conn, username).isEmpty()) { // Uses internal getUser that takes a connection
            addUser(conn, new User(username, password, role, fullName));
        }
    }

    private void addDefaultServiceTypesIfEmpty(Connection conn) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) AS count FROM service_types";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt("count") == 0) {
                LOGGER.info("No service types found in DB, adding defaults from enum using existing connection...");
                ServiceType[] defaultTypes = ServiceType.values(); // Assuming ServiceType is an enum
                if (defaultTypes != null && defaultTypes.length > 0) {
                    for (ServiceType st : defaultTypes) {
                        addServiceType(conn, st.name(), st.getDisplayName());
                    }
                    this.schemaWasPopulatedWithDefaultsThisSession = true; // Set the flag here
                    LOGGER.info("Default service types populated. schemaWasPopulatedWithDefaultsThisSession set to true.");
                } else {
                    LOGGER.warn("ServiceType.values() returned null or empty, no default service types added.");
                }
            } else {
                // Service types table already has entries, or rs.next() was false (should not happen for COUNT(*))
                 LOGGER.debug("Service types table is not empty or check failed. Default service types not added. Count: {}", rs.next() ? rs.getInt("count") : "N/A");
                 this.schemaWasPopulatedWithDefaultsThisSession = false; // Explicitly false if not populated
            }
        } catch (SQLException e) {
            LOGGER.error("Error checking/adding default service types with existing connection: {}", e.getMessage(), e);
            this.schemaWasPopulatedWithDefaultsThisSession = false; // Ensure flag is false on error too
            throw e;
        }
    }

    // Internal helper that uses provided connection
    private void addUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users(username, password, role, fullName) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getHashedPassword()); // Assuming User model now stores hashed password
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getFullName());
            pstmt.executeUpdate();
            LOGGER.info("User added (init context): {}", user.getUsername());
        } catch (SQLException e) {
            LOGGER.error("Error adding user {} (init context): {}", user.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    // Internal helper that uses provided connection
    private Optional<User> getUser(Connection conn, String username) throws SQLException {
        String sql = "SELECT password, role, fullName FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new User(username, rs.getString("password"), // This password is hashed
                        UserRole.valueOf(rs.getString("role")), rs.getString("fullName")));
            }
        } catch (SQLException e) {
            // Log specific error but don't rethrow if it's just a "not found" scenario during check
            // However, for other SQL errors, rethrowing might be appropriate.
            LOGGER.warn("SQL Exception fetching user {} (init context), may indicate other issues or just not found: {}", username, e.getMessage());
            // throw e; // Decide if all SQLExceptions should propagate from this internal check
        }
        return Optional.empty();
    }

    // Internal helper that uses provided connection
    private void addServiceType(Connection conn, String name, String displayName) throws SQLException {
        String sql = "INSERT INTO service_types(name, displayName) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toUpperCase());
            pstmt.setString(2, displayName);
            pstmt.executeUpdate();
            LOGGER.info("Service type added to DB (init context): {} ({})", name.toUpperCase(), displayName);
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 && e.getMessage() != null && e.getMessage().toLowerCase().contains("unique constraint")) { // SQLite error code for unique constraint
                LOGGER.warn("Service type with internal name '{}' already exists in DB (init context).", name.toUpperCase());
            } else {
                LOGGER.error("DB Error adding service type '{}' (init context): {}", name.toUpperCase(), e.getMessage(), e);
                throw e; // Rethrow other SQL errors
            }
        }
    }

    // --- Public CRUD methods that manage their own connections ---
    public void addUser(User user) {
        String sql = "INSERT INTO users(username, password, role, fullName) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getHashedPassword()); // Use hashed password
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getFullName());
            pstmt.executeUpdate();
            LOGGER.info("User added: {}", user.getUsername());
        } catch (SQLException e) {
            LOGGER.error("Error adding user {}: {}", user.getUsername(), e.getMessage(), e);
            // Consider throwing a custom application exception
        }
    }

    public Optional<User> getUser(String username) {
        String sql = "SELECT password, role, fullName FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new User(username, rs.getString("password"), // This is hashed
                        UserRole.valueOf(rs.getString("role")), rs.getString("fullName")));
            }
        } catch (SQLException e) {
            LOGGER.warn("Error fetching user {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }

    public void addServiceType(String name, String displayName) {
        if (name == null || name.trim().isEmpty() || displayName == null || displayName.trim().isEmpty()) {
            LOGGER.error("Service name and display name cannot be empty.");
            return;
        }
        String internalName = name.trim().toUpperCase();
        String display = displayName.trim();

        String sql = "INSERT INTO service_types(name, displayName) VALUES(?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, internalName);
            pstmt.setString(2, display);
            pstmt.executeUpdate();
            LOGGER.info("Service type added to DB: {} ({})", internalName, display);
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 && e.getMessage() != null && e.getMessage().toLowerCase().contains("unique constraint")) {
                LOGGER.warn("Service type with internal name '{}' already exists in DB.", internalName);
            } else {
                LOGGER.error("DB Error adding service type '{}': {}", internalName, e.getMessage(), e);
            }
            // Consider throwing a custom application exception
        }
    }

    public List<ServiceType> getAllServiceTypes() {
        List<ServiceType> serviceTypes = new ArrayList<>();
        String sql = "SELECT name, displayName FROM service_types ORDER BY displayName";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    // Assuming ServiceType enum has a valueOf method that can reconstruct
                    // or a static factory method that also updates/uses the displayName from DB.
                    // For simplicity, if ServiceType enum only cares about the 'name' (enum constant name):
                    ServiceType st = ServiceType.valueOf(rs.getString("name"));
                    // If your ServiceType enum stores displayName and you want it from DB:
                    // ServiceType st = ServiceType.valueOf(rs.getString("name"));
                    // st.setDisplayName(rs.getString("displayName")); // If ServiceType is mutable (not typical for enums)
                    // Or, better, if ServiceType constructor or a factory can take both.
                    // For now, assuming valueOf(name) is sufficient and displayName in enum is primary.
                    serviceTypes.add(st);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("DB service type '{}' (Display: '{}') not found in ServiceType enum. Skipping.", rs.getString("name"), rs.getString("displayName"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("DB Error fetching service types: {}", e.getMessage(), e);
        }
        // Fallback if DB is empty but enums exist (especially for UI display before QueueManager is fully synced)
        // This logic is also in QueueManager; ensure consistency or a single source of truth.
        if (serviceTypes.isEmpty() && ServiceType.values().length > 0) {
            LOGGER.warn("No service types fetched from DB for getAllServiceTypes, attempting to return all ServiceType enum values as a fallback.");
            return List.of(ServiceType.values());
        }
        LOGGER.debug("Fetched {} service types from DB.", serviceTypes.size());
        return serviceTypes;
    }

    public List<ServiceTypeRecord> getAllServiceTypeRecordsForAdmin() {
        List<ServiceTypeRecord> records = new ArrayList<>();
        String sql = "SELECT name, displayName FROM service_types ORDER BY displayName";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(new ServiceTypeRecord(
                        rs.getString("name"),
                        rs.getString("displayName")
                ));
            }
        } catch (SQLException e) {
            LOGGER.error("DB Error fetching all service type records for admin: {}", e.getMessage(), e);
        }
        return records;
    }

    public boolean updateServiceTypeDisplayName(String internalName, String newDisplayName) {
        if (internalName == null || internalName.trim().isEmpty() || newDisplayName == null || newDisplayName.trim().isEmpty()) {
            LOGGER.error("Internal name and new display name cannot be empty for update.");
            return false;
        }
        String sql = "UPDATE service_types SET displayName = ? WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newDisplayName.trim());
            pstmt.setString(2, internalName.trim().toUpperCase());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) LOGGER.info("Updated display name for service '{}' to '{}'", internalName.trim().toUpperCase(), newDisplayName.trim());
            return affectedRows > 0;
        } catch (SQLException e) {
            LOGGER.error("DB Error updating display name for service '{}': {}", internalName.trim().toUpperCase(), e.getMessage(), e);
            return false;
        }
    }

    public boolean removeServiceType(String internalName) {
        if (internalName == null || internalName.trim().isEmpty()){
            LOGGER.error("Internal name cannot be empty for removing service type.");
            return false;
        }
        String internalNameToDelete = internalName.trim().toUpperCase();
        String sqlCheckTickets = "SELECT COUNT(*) AS count FROM tickets WHERE serviceTypeName = ?";
        String sqlDeleteService = "DELETE FROM service_types WHERE name = ?";
        Connection conn = null;
        try {
            conn = connect();
            conn.setAutoCommit(false); // Start transaction

            // Check if any tickets are associated with this service type
            try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckTickets)) {
                pstmtCheck.setString(1, internalNameToDelete);
                ResultSet rs = pstmtCheck.executeQuery();
                if (rs.next() && rs.getInt("count") > 0) {
                    LOGGER.warn("Cannot remove service type '{}' as it is associated with {} existing tickets.", internalNameToDelete, rs.getInt("count"));
                    conn.rollback(); // Rollback transaction
                    return false;
                }
            }

            // If no tickets, proceed to delete
            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteService)) {
                pstmtDelete.setString(1, internalNameToDelete);
                int affectedRows = pstmtDelete.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit(); // Commit transaction
                    LOGGER.info("Removed service type '{}' from DB.", internalNameToDelete);
                    return true;
                } else {
                    conn.rollback(); // Rollback if no rows affected (service type didn't exist)
                    LOGGER.warn("No service type found with name '{}' to remove from DB.", internalNameToDelete);
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("DB Error removing service type '{}': {}", internalNameToDelete, e.getMessage(), e);
            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) { // Check if we are in a transaction
                        conn.rollback();
                        LOGGER.debug("Transaction rolled back for removeServiceType due to error.");
                    }
                } catch (SQLException ex) {
                    LOGGER.error("Rollback failed for removeServiceType: {}", ex.getMessage(), ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) { // If we started a transaction, ensure auto-commit is reset
                         conn.setAutoCommit(true);
                    }
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.error("Error closing connection/resetting auto-commit in removeServiceType: {}", ex.getMessage(), ex);
                }
            }
        }
    }

    // --- Ticket Methods ---
    public void saveTicket(Ticket ticket) {
        if (ticket == null) {
            LOGGER.error("Cannot save a null ticket.");
            return;
        }
        String sql = "INSERT INTO tickets(ticketNumber, serviceTypeName, customerName, issueTime, status, priority, priorityReason) " +
                     "VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticket.getTicketNumber());
            pstmt.setString(2, ticket.getServiceType().name());
            pstmt.setString(3, ticket.getCustomerName());
            pstmt.setString(4, ticket.getIssueTime().format(ISO_LOCAL_DATE_TIME_FORMATTER));
            pstmt.setString(5, ticket.getStatus().name());
            pstmt.setInt(6, ticket.getPriority());
            pstmt.setString(7, ticket.getPriorityReason().name());
            pstmt.executeUpdate();
            LOGGER.debug("Ticket saved: {} with priority reason {}", ticket.getTicketNumber(), ticket.getPriorityReason());
        } catch (SQLException e) {
            LOGGER.error("Error saving ticket {}: {}", ticket.getTicketNumber(), e.getMessage(), e);
        }
    }

    public void updateTicketStatus(String ticketNumber, Ticket.TicketStatus status, String agentUsername) {
         if (ticketNumber == null || status == null) {
            LOGGER.error("Ticket number or status cannot be null for updateTicketStatus.");
            return;
        }
        String sql = "UPDATE tickets SET status = ?, agentUsername = ? WHERE ticketNumber = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, agentUsername); // agentUsername can be null if ticket is moved back to WAITING by system
            pstmt.setString(3, ticketNumber);
            pstmt.executeUpdate();
            LOGGER.debug("Updated status for ticket {}: {} by agent {}", ticketNumber, status, agentUsername);
        } catch (SQLException e) {
            LOGGER.error("Error updating ticket status {}: {}", ticketNumber, e.getMessage(), e);
        }
    }

    public void updateTicketTimes(String ticketNumber, LocalDateTime callTime, LocalDateTime serviceStartTime, LocalDateTime serviceEndTime) {
        if (ticketNumber == null) {
            LOGGER.error("Ticket number cannot be null for updateTicketTimes.");
            return;
        }
        StringBuilder sqlBuilder = new StringBuilder("UPDATE tickets SET ");
        List<String> params = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (callTime != null) {
            params.add("callTime = ?");
            values.add(callTime.format(ISO_LOCAL_DATE_TIME_FORMATTER));
        }
        if (serviceStartTime != null) {
            params.add("serviceStartTime = ?");
            values.add(serviceStartTime.format(ISO_LOCAL_DATE_TIME_FORMATTER));
        }
        if (serviceEndTime != null) {
            params.add("serviceEndTime = ?");
            values.add(serviceEndTime.format(ISO_LOCAL_DATE_TIME_FORMATTER));
        }

        if (params.isEmpty()) {
            LOGGER.warn("updateTicketTimes called for {} with no times to update.", ticketNumber);
            return;
        }

        sqlBuilder.append(String.join(", ", params));
        sqlBuilder.append(" WHERE ticketNumber = ?");
        values.add(ticketNumber);

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
            LOGGER.debug("Updated times for ticket {}: {}", ticketNumber, params);
        } catch (SQLException e) {
            LOGGER.error("Error updating ticket times for {}: {}", ticketNumber, e.getMessage(), e);
        }
    }

    public boolean updateTicketPriority(String ticketNumber, Ticket.PriorityReason reason) {
         if (ticketNumber == null || reason == null) {
            LOGGER.error("Ticket number or reason cannot be null for updateTicketPriority.");
            return false;
        }
        // The numerical priority is now derived within the Ticket model itself when priorityReason is set.
        // We just need to store the reason. The Ticket constructor/setter should handle the numerical value.
        // However, the DB schema still has a separate 'priority' column.
        // Let's assume Ticket.getPriority() gives the correct numerical value based on the reason.
        Ticket tempTicket = new Ticket(null, null, null, null, reason, 0); // Temporary to get numerical priority
        int numericalPriority = tempTicket.getPriority();


        String sql = "UPDATE tickets SET priority = ?, priorityReason = ? WHERE ticketNumber = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, numericalPriority);
            pstmt.setString(2, reason.name());
            pstmt.setString(3, ticketNumber);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("Updated priority for ticket {} to numerical {} (Reason: {})", ticketNumber, numericalPriority, reason);
                return true;
            }
            LOGGER.warn("No ticket found with number {} to update priority or priority unchanged.", ticketNumber);
            return false;
        } catch (SQLException e) {
            LOGGER.error("Error updating priority for ticket {}: {}", ticketNumber, e.getMessage(), e);
            return false;
        }
    }


    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT ticketNumber, serviceTypeName, customerName, issueTime, callTime, serviceStartTime, serviceEndTime, status, priority, priorityReason, agentUsername FROM tickets ORDER BY issueTime DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ServiceType st = null;
                try {
                    st = ServiceType.valueOf(rs.getString("serviceTypeName"));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid serviceTypeName '{}' in database for ticket {}. Skipping ticket.", rs.getString("serviceTypeName"), rs.getString("ticketNumber"));
                    continue; // Skip this ticket if service type is invalid
                }

                Ticket.PriorityReason reason = Ticket.PriorityReason.NONE; // Default
                String reasonStr = rs.getString("priorityReason");
                if (reasonStr != null) {
                    try {
                        reason = Ticket.PriorityReason.valueOf(reasonStr);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid priorityReason '{}' in database for ticket {}. Defaulting to NONE.", reasonStr, rs.getString("ticketNumber"));
                    }
                }
                int numericalPriority = rs.getInt("priority");

                Ticket ticket = new Ticket(
                        rs.getString("ticketNumber"),
                        st,
                        rs.getString("customerName"),
                        LocalDateTime.parse(rs.getString("issueTime"), ISO_LOCAL_DATE_TIME_FORMATTER),
                        reason,
                        numericalPriority
                );
                ticket.setStatus(Ticket.TicketStatus.valueOf(rs.getString("status")));
                ticket.setAgentUsername(rs.getString("agentUsername"));

                String callTimeStr = rs.getString("callTime");
                if (callTimeStr != null) ticket.setCallTime(LocalDateTime.parse(callTimeStr, ISO_LOCAL_DATE_TIME_FORMATTER));

                String serviceStartTimeStr = rs.getString("serviceStartTime");
                if (serviceStartTimeStr != null) ticket.setServiceStartTime(LocalDateTime.parse(serviceStartTimeStr, ISO_LOCAL_DATE_TIME_FORMATTER));

                String serviceEndTimeStr = rs.getString("serviceEndTime");
                if (serviceEndTimeStr != null) ticket.setServiceEndTime(LocalDateTime.parse(serviceEndTimeStr, ISO_LOCAL_DATE_TIME_FORMATTER));

                tickets.add(ticket);
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching all tickets: {}", e.getMessage(), e);
        }
        return tickets;
    }

    public void saveFeedback(Feedback feedback) {
        if (feedback == null) {
            LOGGER.error("Cannot save null feedback.");
            return;
        }
        String sql = "INSERT INTO feedback(ticketNumber, rating, comments, submissionTime) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, feedback.getTicketNumber());
            pstmt.setInt(2, feedback.getRating());
            pstmt.setString(3, feedback.getComments());
            pstmt.setString(4, feedback.getSubmissionTime().format(ISO_LOCAL_DATE_TIME_FORMATTER));
            pstmt.executeUpdate();
            LOGGER.debug("Feedback saved for ticket {}", feedback.getTicketNumber());
        } catch (SQLException e) {
            LOGGER.error("Error saving feedback for ticket {}: {}", feedback.getTicketNumber(), e.getMessage(), e);
        }
    }

    public List<Feedback> getAllFeedback() {
        List<Feedback> feedbackList = new ArrayList<>();
        String sql = "SELECT id, ticketNumber, rating, comments, submissionTime FROM feedback ORDER BY submissionTime DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Feedback feedback = new Feedback(
                        rs.getInt("id"),
                        rs.getString("ticketNumber"),
                        rs.getInt("rating"),
                        rs.getString("comments"),
                        LocalDateTime.parse(rs.getString("submissionTime"), ISO_LOCAL_DATE_TIME_FORMATTER)
                );
                feedbackList.add(feedback);
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching all feedback: {}", e.getMessage(), e);
        }
        return feedbackList;
    }
}
