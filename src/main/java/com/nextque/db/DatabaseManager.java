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

    public DatabaseManager() {
        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
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
                "FOREIGN KEY (serviceTypeName) REFERENCES service_types(name) ON DELETE RESTRICT ON UPDATE CASCADE" +
                ");";
        String createFeedbackTable = "CREATE TABLE IF NOT EXISTS feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ticketNumber TEXT," +
                "rating INTEGER," +
                "comments TEXT," +
                "submissionTime TEXT NOT NULL," +
                "FOREIGN KEY (ticketNumber) REFERENCES tickets(ticketNumber) ON DELETE SET NULL ON UPDATE CASCADE" +
                ");";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUserTable);
                stmt.execute(createServiceTypeTable);
                stmt.execute(createTicketTable);
                stmt.execute(createFeedbackTable);
            }

            addDefaultUserIfNotExists(conn, "admin", "admin123", UserRole.ADMIN, "System Administrator");
            addDefaultUserIfNotExists(conn, "agent1", "agent123", UserRole.AGENT, "Default Agent");
            addDefaultServiceTypesIfEmpty(conn);

            conn.commit();
        } catch (SQLException e) {
            LOGGER.error("CRITICAL: Database initialization error: {}.", e.getMessage(), e);
        }
    }

    public int getHighestTicketNumberSuffix() {
        String sql = "SELECT CAST(SUBSTR(ticketNumber, INSTR(ticketNumber, '-') + 1) AS INTEGER) as num FROM tickets WHERE ticketNumber LIKE '%-%'";
        int maxNum = 0;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int currentNum = rs.getInt("num");
                if (currentNum > maxNum) {
                    maxNum = currentNum;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching highest ticket number suffix from DB. Counter will start at 0. Error: {}", e.getMessage(), e);
        }
        return maxNum;
    }

    private void addDefaultUserIfNotExists(Connection conn, String username, String password, UserRole role, String fullName) throws SQLException {
        if (getUser(conn, username).isEmpty()) {
            addUser(conn, new User(username, password, role, fullName));
        }
    }

    private void addDefaultServiceTypesIfEmpty(Connection conn) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) AS count FROM service_types";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt("count") == 0) {
                addServiceType(conn, "NEW_APP", "New Application");
                addServiceType(conn, "RENEWAL", "Renewal");
                addServiceType(conn, "PAYMENT", "Payment");
                addServiceType(conn, "INQUIRY", "Inquiry");
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    private void addUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users(username, password, role, fullName) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getHashedPassword());
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getFullName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }

    private Optional<User> getUser(Connection conn, String username) throws SQLException {
        String sql = "SELECT password, role, fullName FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(username, rs.getString("password"),
                            UserRole.valueOf(rs.getString("role")), rs.getString("fullName")));
                }
            }
        }
        return Optional.empty();
    }

    private void addServiceType(Connection conn, String name, String displayName) throws SQLException {
        String sql = "INSERT INTO service_types(name, displayName) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toUpperCase());
            pstmt.setString(2, displayName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }

    public void addUser(User user) {
        String sql = "INSERT INTO users(username, password, role, fullName) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getHashedPassword());
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getFullName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error adding user {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    public Optional<User> getUser(String username) {
        String sql = "SELECT password, role, fullName FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(username, rs.getString("password"),
                            UserRole.valueOf(rs.getString("role")), rs.getString("fullName")));
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Error fetching user {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }

    public void addServiceType(String name, String displayName) {
        if (name == null || name.trim().isEmpty() || displayName == null || displayName.trim().isEmpty()) {
            return;
        }
        String internalName = name.trim().toUpperCase();
        String display = displayName.trim();
        String sql = "INSERT INTO service_types(name, displayName) VALUES(?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, internalName);
            pstmt.setString(2, display);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("DB Error adding service type '{}': {}", internalName, e.getMessage(), e);
        }
    }

    public List<ServiceType> getAllServiceTypes() {
        List<ServiceType> serviceTypes = new ArrayList<>();
        String sql = "SELECT name, displayName FROM service_types ORDER BY displayName";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                serviceTypes.add(new ServiceType(rs.getString("name"), rs.getString("displayName")));
            }
        } catch (SQLException e) {
            LOGGER.error("DB Error fetching service types: {}", e.getMessage(), e);
        }
        return serviceTypes;
    }
    
    public Optional<ServiceType> findServiceTypeByName(String name) {
        String sql = "SELECT name, displayName FROM service_types WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ServiceType(rs.getString("name"), rs.getString("displayName")));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("DB Error finding service type by name '{}': {}", name, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean updateServiceTypeDisplayName(String internalName, String newDisplayName) {
        if (internalName == null || internalName.trim().isEmpty() || newDisplayName == null || newDisplayName.trim().isEmpty()) {
            return false;
        }
        String sql = "UPDATE service_types SET displayName = ? WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newDisplayName.trim());
            pstmt.setString(2, internalName.trim().toUpperCase());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("DB Error updating display name for service '{}': {}", internalName.trim().toUpperCase(), e.getMessage(), e);
            return false;
        }
    }

    public boolean removeServiceType(String internalName) {
        if (internalName == null || internalName.trim().isEmpty()){
            return false;
        }
        String internalNameToDelete = internalName.trim().toUpperCase();
        String sqlCheckTickets = "SELECT COUNT(*) AS count FROM tickets WHERE serviceTypeName = ?";
        String sqlDeleteService = "DELETE FROM service_types WHERE name = ?";
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckTickets)) {
                pstmtCheck.setString(1, internalNameToDelete);
                try (ResultSet rs = pstmtCheck.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }
            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteService)) {
                pstmtDelete.setString(1, internalNameToDelete);
                int affectedRows = pstmtDelete.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("DB Error removing service type '{}': {}", internalNameToDelete, e.getMessage(), e);
            return false;
        }
    }

    public void saveTicket(Ticket ticket) {
        if (ticket == null) {
            return;
        }
        String sql = "INSERT INTO tickets(ticketNumber, serviceTypeName, customerName, issueTime, status, priority, priorityReason, agentUsername, callTime, serviceStartTime, serviceEndTime) " +
                     "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticket.getTicketNumber());
            pstmt.setString(2, ticket.getServiceType().getName());
            pstmt.setString(3, ticket.getCustomerName());
            pstmt.setString(4, ticket.getIssueTime().format(ISO_LOCAL_DATE_TIME_FORMATTER));
            pstmt.setString(5, ticket.getStatus().name());
            pstmt.setInt(6, ticket.getPriority());
            pstmt.setString(7, ticket.getPriorityReason().name());
            pstmt.setString(8, ticket.getAgentUsername());
            pstmt.setString(9, ticket.getCallTime() != null ? ticket.getCallTime().format(ISO_LOCAL_DATE_TIME_FORMATTER) : null);
            pstmt.setString(10, ticket.getServiceStartTime() != null ? ticket.getServiceStartTime().format(ISO_LOCAL_DATE_TIME_FORMATTER) : null);
            pstmt.setString(11, ticket.getServiceEndTime() != null ? ticket.getServiceEndTime().format(ISO_LOCAL_DATE_TIME_FORMATTER) : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error saving ticket {}: {}", ticket.getTicketNumber(), e.getMessage(), e);
        }
    }

    public void updateTicketStatus(String ticketNumber, Ticket.TicketStatus status, String agentUsername) {
         if (ticketNumber == null || status == null) {
            return;
        }
        String sql = "UPDATE tickets SET status = ?, agentUsername = ? WHERE ticketNumber = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, agentUsername);
            pstmt.setString(3, ticketNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error updating ticket status {}: {}", ticketNumber, e.getMessage(), e);
        }
    }

    public void updateTicketTimes(String ticketNumber, LocalDateTime callTime, LocalDateTime serviceStartTime, LocalDateTime serviceEndTime) {
        if (ticketNumber == null) {
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
        } catch (SQLException e) {
            LOGGER.error("Error updating ticket times for {}: {}", ticketNumber, e.getMessage(), e);
        }
    }

    public boolean updateTicketPriority(String ticketNumber, Ticket.PriorityReason reason) {
         if (ticketNumber == null || reason == null) {
            return false;
        }
        Ticket tempTicket = new Ticket(new ServiceType("TEMP", "Temp"), null, reason);
        int numericalPriority = tempTicket.getPriority();

        String sql = "UPDATE tickets SET priority = ?, priorityReason = ? WHERE ticketNumber = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, numericalPriority);
            pstmt.setString(2, reason.name());
            pstmt.setString(3, ticketNumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Error updating priority for ticket {}: {}", ticketNumber, e.getMessage(), e);
            return false;
        }
    }

    public List<Ticket> getAllTicketsWithResolvedServiceTypes() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets ORDER BY issueTime DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String serviceTypeName = rs.getString("serviceTypeName");
                Optional<ServiceType> stOpt = findServiceTypeByName(serviceTypeName);

                if (stOpt.isEmpty()) {
                    LOGGER.error("Could not resolve ServiceType for name '{}' in ticket {}. Skipping.", serviceTypeName, rs.getString("ticketNumber"));
                    continue;
                }
                ServiceType st = stOpt.get();

                Ticket.PriorityReason reason = Ticket.PriorityReason.NONE;
                String reasonStr = rs.getString("priorityReason");
                if (reasonStr != null) {
                    try {
                        reason = Ticket.PriorityReason.valueOf(reasonStr);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid priorityReason '{}' in database for ticket {}. Defaulting to NONE.", reasonStr, rs.getString("ticketNumber"));
                    }
                }

                Ticket ticket = new Ticket(
                        rs.getString("ticketNumber"),
                        st,
                        rs.getString("customerName"),
                        LocalDateTime.parse(rs.getString("issueTime"), ISO_LOCAL_DATE_TIME_FORMATTER),
                        reason
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
            return;
        }
        String sql = "INSERT INTO feedback(ticketNumber, rating, comments, submissionTime) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, feedback.getTicketNumber());
            pstmt.setInt(2, feedback.getRating());
            pstmt.setString(3, feedback.getComments());
            pstmt.setString(4, feedback.getSubmissionTime().format(ISO_LOCAL_DATE_TIME_FORMATTER));
            pstmt.executeUpdate();
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
