package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of MobileSubscriptionDAO.
 */
public class MobileSubscriptionDAOImpl implements MobileSubscriptionDAO {

    private static final String SELECT_COLUMNS =
            "subscription_id, subscription_number, customer_id, mobile_number, sim_id, plan_id, " +
            "activation_date, subscription_type, status, created_at, updated_at";

    @Override
    public MobileSubscription findById(Long subscriptionId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM mobile_subscriptions WHERE subscription_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToMobileSubscription(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find mobile subscription by ID: " + subscriptionId, e);
        }
    }

    @Override
    public MobileSubscription findBySubscriptionNumber(String subscriptionNumber) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM mobile_subscriptions WHERE subscription_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, subscriptionNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToMobileSubscription(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find mobile subscription by subscription number: "
                    + subscriptionNumber, e);
        }
    }

    @Override
    public List<MobileSubscription> findByCustomerId(Long customerId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM mobile_subscriptions WHERE customer_id = ? " +
                "ORDER BY subscription_id";

        List<MobileSubscription> subscriptions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subscriptions.add(mapRowToMobileSubscription(resultSet));
                }
            }
            return subscriptions;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find mobile subscriptions for customer ID: " + customerId, e);
        }
    }

    @Override
    public MobileSubscription findByMobileNumber(String mobileNumber) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM mobile_subscriptions WHERE mobile_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, mobileNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToMobileSubscription(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find mobile subscription by mobile number: " + mobileNumber, e);
        }
    }

    @Override
    public boolean existsByMobileNumber(String mobileNumber) {
        // "SELECT 1 ... LIMIT 1" avoids pulling back a whole row just to check existence.
        String sql = "SELECT 1 FROM mobile_subscriptions WHERE mobile_number = ? LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, mobileNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of mobile number: " + mobileNumber, e);
        }
    }

    @Override
    public boolean existsByCustomerAndPlan(Long customerId, Long planId) {
        String sql = "SELECT 1 FROM mobile_subscriptions WHERE customer_id = ? AND plan_id = ? LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);
            statement.setLong(2, planId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existing subscription for customer ID: " + customerId
                    + " and plan ID: " + planId, e);
        }
    }

    @Override
    public void save(MobileSubscription subscription) {
        // subscription_id is AUTO_INCREMENT and created_at/updated_at rely on their database defaults.
        String sql = "INSERT INTO mobile_subscriptions (subscription_number, customer_id, mobile_number, " +
                "sim_id, plan_id, activation_date, subscription_type, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, subscription.getSubscriptionNumber());
            statement.setLong(2, subscription.getCustomerId());
            statement.setString(3, subscription.getMobileNumber());
            statement.setLong(4, subscription.getSimId());
            statement.setLong(5, subscription.getPlanId());
            statement.setDate(6, java.sql.Date.valueOf(subscription.getActivationDate()));
            statement.setString(7, subscription.getSubscriptionType());
            statement.setString(8, subscription.getStatus());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    subscription.setSubscriptionId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save mobile subscription with subscription number: "
                    + subscription.getSubscriptionNumber(), e);
        }
    }

    @Override
    public void update(MobileSubscription subscription) {
        // subscription_id is the WHERE key and is never updated. created_at is never touched.
        String sql = "UPDATE mobile_subscriptions SET subscription_number = ?, customer_id = ?, " +
                "mobile_number = ?, sim_id = ?, plan_id = ?, activation_date = ?, subscription_type = ?, " +
                "status = ?, updated_at = CURRENT_TIMESTAMP WHERE subscription_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, subscription.getSubscriptionNumber());
            statement.setLong(2, subscription.getCustomerId());
            statement.setString(3, subscription.getMobileNumber());
            statement.setLong(4, subscription.getSimId());
            statement.setLong(5, subscription.getPlanId());
            statement.setDate(6, java.sql.Date.valueOf(subscription.getActivationDate()));
            statement.setString(7, subscription.getSubscriptionType());
            statement.setString(8, subscription.getStatus());
            statement.setLong(9, subscription.getSubscriptionId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update mobile subscription with ID: "
                    + subscription.getSubscriptionId(), e);
        }
    }

    @Override
    public void deleteById(Long subscriptionId) {
        String sql = "DELETE FROM mobile_subscriptions WHERE subscription_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete mobile subscription with ID: " + subscriptionId, e);
        }
    }

    private MobileSubscription mapRowToMobileSubscription(ResultSet resultSet) throws SQLException {
        MobileSubscription subscription = new MobileSubscription();
        subscription.setSubscriptionId(resultSet.getLong("subscription_id"));
        subscription.setSubscriptionNumber(resultSet.getString("subscription_number"));
        subscription.setCustomerId(resultSet.getLong("customer_id"));
        subscription.setMobileNumber(resultSet.getString("mobile_number"));
        subscription.setSimId(resultSet.getLong("sim_id"));
        subscription.setPlanId(resultSet.getLong("plan_id"));
        subscription.setActivationDate(resultSet.getDate("activation_date").toLocalDate());
        subscription.setSubscriptionType(resultSet.getString("subscription_type"));
        subscription.setStatus(resultSet.getString("status"));
        subscription.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        subscription.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return subscription;
    }
}
