package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.NotificationDAO;
import com.amdocs.telecom.model.Notification;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of NotificationDAO.
 *
 * customer_id, notification_type and message describe what was created and are not touched by
 * update(); only status and sent_at are mutable once a notification exists (its delivery
 * lifecycle), so update() only ever changes those two columns.
 */
public class NotificationDAOImpl implements NotificationDAO {

    private static final String SELECT_COLUMNS =
            "notification_id, customer_id, notification_type, message, status, created_at, sent_at";

    @Override
    public Notification findById(Long notificationId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM notifications WHERE notification_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, notificationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToNotification(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notification by ID: " + notificationId, e);
        }
    }

    @Override
    public List<Notification> findByCustomerId(Long customerId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM notifications WHERE customer_id = ? " +
                "ORDER BY created_at";

        List<Notification> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapRowToNotification(resultSet));
                }
            }
            return notifications;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notifications for customer ID: " + customerId, e);
        }
    }

    @Override
    public List<Notification> findByStatus(String status) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM notifications WHERE status = ? " +
                "ORDER BY created_at";

        List<Notification> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapRowToNotification(resultSet));
                }
            }
            return notifications;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notifications with status: " + status, e);
        }
    }

    @Override
    public void save(Notification notification) {
        // notification_id is AUTO_INCREMENT and created_at defaults to CURRENT_TIMESTAMP in the
        // schema. sent_at is nullable and is only populated once the notification is actually sent.
        String sql = "INSERT INTO notifications (customer_id, notification_type, message, status, sent_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, notification.getCustomerId());
            statement.setString(2, notification.getNotificationType());
            statement.setString(3, notification.getMessage());
            statement.setString(4, notification.getStatus());
            setNullableTimestamp(statement, 5, notification.getSentAt());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setNotificationId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save notification for customer ID: "
                    + notification.getCustomerId(), e);
        }
    }

    @Override
    public void update(Notification notification) {
        // Only status and sent_at are mutable after creation. notification_id, customer_id,
        // notification_type, message and created_at are never touched by an update.
        String sql = "UPDATE notifications SET status = ?, sent_at = ? WHERE notification_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, notification.getStatus());
            setNullableTimestamp(statement, 2, notification.getSentAt());
            statement.setLong(3, notification.getNotificationId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update notification with ID: "
                    + notification.getNotificationId(), e);
        }
    }

    private void setNullableTimestamp(PreparedStatement statement, int parameterIndex, LocalDateTime value)
            throws SQLException {
        if (value != null) {
            statement.setTimestamp(parameterIndex, Timestamp.valueOf(value));
        } else {
            statement.setNull(parameterIndex, Types.TIMESTAMP);
        }
    }

    private Notification mapRowToNotification(ResultSet resultSet) throws SQLException {
        Notification notification = new Notification();
        notification.setNotificationId(resultSet.getLong("notification_id"));
        notification.setCustomerId(resultSet.getLong("customer_id"));
        notification.setNotificationType(resultSet.getString("notification_type"));
        notification.setMessage(resultSet.getString("message"));
        notification.setStatus(resultSet.getString("status"));
        notification.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());

        Timestamp sentAt = resultSet.getTimestamp("sent_at");
        notification.setSentAt(sentAt != null ? sentAt.toLocalDateTime() : null);

        return notification;
    }
}
