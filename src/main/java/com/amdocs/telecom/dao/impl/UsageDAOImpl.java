package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.UsageDAO;
import com.amdocs.telecom.model.UsageRecord;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of UsageDAO.
 * usage_records is append-only, so only findById, findBySubscriptionId,
 * findBySubscriptionIdAndUsageType and save are implemented - there is no update or delete.
 */
public class UsageDAOImpl implements UsageDAO {

    private static final String SELECT_COLUMNS =
            "usage_id, subscription_id, usage_date, usage_type, quantity, unit, charge, created_at";

    @Override
    public UsageRecord findById(Long usageId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM usage_records WHERE usage_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, usageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToUsageRecord(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find usage record by ID: " + usageId, e);
        }
    }

    @Override
    public List<UsageRecord> findBySubscriptionId(Long subscriptionId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM usage_records WHERE subscription_id = ? " +
                "ORDER BY usage_date, usage_id";

        List<UsageRecord> usageRecords = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    usageRecords.add(mapRowToUsageRecord(resultSet));
                }
            }
            return usageRecords;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find usage records for subscription ID: "
                    + subscriptionId, e);
        }
    }

    @Override
    public List<UsageRecord> findBySubscriptionIdAndUsageType(Long subscriptionId, String usageType) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM usage_records WHERE subscription_id = ? " +
                "AND usage_type = ? ORDER BY usage_date, usage_id";

        List<UsageRecord> usageRecords = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);
            statement.setString(2, usageType);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    usageRecords.add(mapRowToUsageRecord(resultSet));
                }
            }
            return usageRecords;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find usage records for subscription ID: "
                    + subscriptionId + " and usage type: " + usageType, e);
        }
    }

    @Override
    public void save(UsageRecord usageRecord) {
        // usage_id is AUTO_INCREMENT and created_at defaults to CURRENT_TIMESTAMP in the schema.
        String sql = "INSERT INTO usage_records (subscription_id, usage_date, usage_type, quantity, " +
                "unit, charge) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, usageRecord.getSubscriptionId());
            statement.setTimestamp(2, Timestamp.valueOf(usageRecord.getUsageDate()));
            statement.setString(3, usageRecord.getUsageType());
            statement.setBigDecimal(4, usageRecord.getQuantity());
            statement.setString(5, usageRecord.getUnit());
            statement.setBigDecimal(6, usageRecord.getCharge());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usageRecord.setUsageId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save usage record for subscription ID: "
                    + usageRecord.getSubscriptionId(), e);
        }
    }

    private UsageRecord mapRowToUsageRecord(ResultSet resultSet) throws SQLException {
        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setUsageId(resultSet.getLong("usage_id"));
        usageRecord.setSubscriptionId(resultSet.getLong("subscription_id"));
        usageRecord.setUsageDate(resultSet.getTimestamp("usage_date").toLocalDateTime());
        usageRecord.setUsageType(resultSet.getString("usage_type"));
        usageRecord.setQuantity(resultSet.getBigDecimal("quantity"));
        usageRecord.setUnit(resultSet.getString("unit"));
        usageRecord.setCharge(resultSet.getBigDecimal("charge"));
        usageRecord.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        return usageRecord;
    }
}
