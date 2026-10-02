package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.ComplaintDAO;
import com.amdocs.telecom.model.Complaint;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of ComplaintDAO.
 */
public class ComplaintDAOImpl implements ComplaintDAO {

    private static final String SELECT_COLUMNS =
            "complaint_id, complaint_number, customer_id, subscription_id, category, description, " +
            "priority, created_date, status, resolution, updated_at";

    @Override
    public Complaint findById(Long complaintId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM complaints WHERE complaint_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, complaintId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToComplaint(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find complaint by ID: " + complaintId, e);
        }
    }

    @Override
    public Complaint findByComplaintNumber(String complaintNumber) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM complaints WHERE complaint_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, complaintNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToComplaint(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find complaint by complaint number: " + complaintNumber, e);
        }
    }

    @Override
    public List<Complaint> findByCustomerId(Long customerId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM complaints WHERE customer_id = ? " +
                "ORDER BY created_date";

        List<Complaint> complaints = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    complaints.add(mapRowToComplaint(resultSet));
                }
            }
            return complaints;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find complaints for customer ID: " + customerId, e);
        }
    }

    @Override
    public List<Complaint> findBySubscriptionId(Long subscriptionId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM complaints WHERE subscription_id = ? " +
                "ORDER BY created_date";

        List<Complaint> complaints = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    complaints.add(mapRowToComplaint(resultSet));
                }
            }
            return complaints;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find complaints for subscription ID: " + subscriptionId, e);
        }
    }

    @Override
    public List<Complaint> findByStatus(String status) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM complaints WHERE status = ? " +
                "ORDER BY created_date";

        List<Complaint> complaints = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    complaints.add(mapRowToComplaint(resultSet));
                }
            }
            return complaints;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find complaints with status: " + status, e);
        }
    }

    @Override
    public List<Complaint> findByPriority(String priority) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM complaints WHERE priority = ? " +
                "ORDER BY created_date";

        List<Complaint> complaints = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, priority);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    complaints.add(mapRowToComplaint(resultSet));
                }
            }
            return complaints;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find complaints with priority: " + priority, e);
        }
    }

    @Override
    public void save(Complaint complaint) {
        // complaint_id is AUTO_INCREMENT. created_date and updated_at default to CURRENT_TIMESTAMP
        // in the schema, but if the caller already supplied a created_date, that value is
        // preserved instead of relying on the database default.
        boolean createdDateSupplied = complaint.getCreatedDate() != null;

        String sql = createdDateSupplied
                ? "INSERT INTO complaints (complaint_number, customer_id, subscription_id, category, " +
                        "description, priority, created_date, status, resolution) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO complaints (complaint_number, customer_id, subscription_id, category, " +
                        "description, priority, status, resolution) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, complaint.getComplaintNumber());
            statement.setLong(2, complaint.getCustomerId());
            setNullableLong(statement, 3, complaint.getSubscriptionId());
            statement.setString(4, complaint.getCategory());
            statement.setString(5, complaint.getDescription());
            statement.setString(6, complaint.getPriority());

            if (createdDateSupplied) {
                statement.setTimestamp(7, Timestamp.valueOf(complaint.getCreatedDate()));
                statement.setString(8, complaint.getStatus());
                statement.setString(9, complaint.getResolution());
            } else {
                statement.setString(7, complaint.getStatus());
                statement.setString(8, complaint.getResolution());
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    complaint.setComplaintId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save complaint with complaint number: "
                    + complaint.getComplaintNumber(), e);
        }
    }

    @Override
    public void update(Complaint complaint) {
        // complaint_id is the WHERE key and is never updated. created_date is never touched.
        String sql = "UPDATE complaints SET complaint_number = ?, customer_id = ?, subscription_id = ?, " +
                "category = ?, description = ?, priority = ?, status = ?, resolution = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE complaint_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, complaint.getComplaintNumber());
            statement.setLong(2, complaint.getCustomerId());
            setNullableLong(statement, 3, complaint.getSubscriptionId());
            statement.setString(4, complaint.getCategory());
            statement.setString(5, complaint.getDescription());
            statement.setString(6, complaint.getPriority());
            statement.setString(7, complaint.getStatus());
            statement.setString(8, complaint.getResolution());
            statement.setLong(9, complaint.getComplaintId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update complaint with ID: " + complaint.getComplaintId(), e);
        }
    }

    private void setNullableLong(PreparedStatement statement, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            statement.setLong(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.BIGINT);
        }
    }

    private Complaint mapRowToComplaint(ResultSet resultSet) throws SQLException {
        Complaint complaint = new Complaint();
        complaint.setComplaintId(resultSet.getLong("complaint_id"));
        complaint.setComplaintNumber(resultSet.getString("complaint_number"));
        complaint.setCustomerId(resultSet.getLong("customer_id"));

        long subscriptionId = resultSet.getLong("subscription_id");
        complaint.setSubscriptionId(resultSet.wasNull() ? null : subscriptionId);

        complaint.setCategory(resultSet.getString("category"));
        complaint.setDescription(resultSet.getString("description"));
        complaint.setPriority(resultSet.getString("priority"));
        complaint.setCreatedDate(resultSet.getTimestamp("created_date").toLocalDateTime());
        complaint.setStatus(resultSet.getString("status"));
        complaint.setResolution(resultSet.getString("resolution"));
        complaint.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());

        return complaint;
    }
}
