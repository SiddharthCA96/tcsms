package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.SubscriptionHistoryDAO;
import com.amdocs.telecom.model.SubscriptionHistory;
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
 * JDBC implementation of SubscriptionHistoryDAO.
 * subscription_history is an append-only audit trail, so only findById, findBySubscriptionId
 * and save are implemented - there is no update or delete.
 */
public class SubscriptionHistoryDAOImpl implements SubscriptionHistoryDAO {

    private static final String SELECT_COLUMNS =
            "history_id, subscription_id, old_plan_id, new_plan_id, change_date, change_reason, changed_by";

    @Override
    public SubscriptionHistory findById(Long historyId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM subscription_history WHERE history_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, historyId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToSubscriptionHistory(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find subscription history by ID: " + historyId, e);
        }
    }

    @Override
    public List<SubscriptionHistory> findBySubscriptionId(Long subscriptionId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM subscription_history WHERE subscription_id = ? " +
                "ORDER BY change_date, history_id";

        List<SubscriptionHistory> historyRecords = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    historyRecords.add(mapRowToSubscriptionHistory(resultSet));
                }
            }
            return historyRecords;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find subscription history for subscription ID: "
                    + subscriptionId, e);
        }
    }

    @Override
    public void save(SubscriptionHistory history) {
        // history_id is AUTO_INCREMENT. change_date defaults to CURRENT_TIMESTAMP in the schema,
        // but if the caller already supplied a value, that value is preserved instead of relying
        // on the database default.
        boolean changeDateSupplied = history.getChangeDate() != null;

        String sql = changeDateSupplied
                ? "INSERT INTO subscription_history (subscription_id, old_plan_id, new_plan_id, " +
                        "change_date, change_reason, changed_by) VALUES (?, ?, ?, ?, ?, ?)"
                : "INSERT INTO subscription_history (subscription_id, old_plan_id, new_plan_id, " +
                        "change_reason, changed_by) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, history.getSubscriptionId());
            setNullableLong(statement, 2, history.getOldPlanId());
            statement.setLong(3, history.getNewPlanId());

            if (changeDateSupplied) {
                statement.setTimestamp(4, Timestamp.valueOf(history.getChangeDate()));
                statement.setString(5, history.getChangeReason());
                statement.setString(6, history.getChangedBy());
            } else {
                statement.setString(4, history.getChangeReason());
                statement.setString(5, history.getChangedBy());
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    history.setHistoryId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save subscription history for subscription ID: "
                    + history.getSubscriptionId(), e);
        }
    }

    private void setNullableLong(PreparedStatement statement, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            statement.setLong(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.BIGINT);
        }
    }

    private SubscriptionHistory mapRowToSubscriptionHistory(ResultSet resultSet) throws SQLException {
        SubscriptionHistory history = new SubscriptionHistory();
        history.setHistoryId(resultSet.getLong("history_id"));
        history.setSubscriptionId(resultSet.getLong("subscription_id"));

        long oldPlanId = resultSet.getLong("old_plan_id");
        history.setOldPlanId(resultSet.wasNull() ? null : oldPlanId);

        history.setNewPlanId(resultSet.getLong("new_plan_id"));
        history.setChangeDate(resultSet.getTimestamp("change_date").toLocalDateTime());
        history.setChangeReason(resultSet.getString("change_reason"));
        history.setChangedBy(resultSet.getString("changed_by"));

        return history;
    }
}
