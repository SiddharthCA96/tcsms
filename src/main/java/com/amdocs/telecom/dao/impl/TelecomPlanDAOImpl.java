package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of TelecomPlanDAO.
 */
public class TelecomPlanDAOImpl implements TelecomPlanDAO {

    private static final String SELECT_COLUMNS =
            "plan_id, plan_code, plan_name, plan_type, monthly_rental, data_allowance_gb, " +
            "voice_minutes, sms_allowance, validity_days, international_roaming, status, created_at, updated_at";

    @Override
    public TelecomPlan findById(Long planId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM telecom_plans WHERE plan_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, planId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToTelecomPlan(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find telecom plan by ID: " + planId, e);
        }
    }

    @Override
    public TelecomPlan findByPlanCode(String planCode) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM telecom_plans WHERE plan_code = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, planCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToTelecomPlan(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find telecom plan by plan code: " + planCode, e);
        }
    }

    @Override
    public boolean existsByPlanCode(String planCode) {
        // "SELECT 1" avoids pulling back a whole row just to check existence.
        String sql = "SELECT 1 FROM telecom_plans WHERE plan_code = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, planCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of plan code: " + planCode, e);
        }
    }

    @Override
    public void save(TelecomPlan plan) {
        // plan_id is AUTO_INCREMENT and created_at/updated_at rely on their database defaults.
        String sql = "INSERT INTO telecom_plans (plan_code, plan_name, plan_type, monthly_rental, " +
                "data_allowance_gb, voice_minutes, sms_allowance, validity_days, international_roaming, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, plan.getPlanCode());
            statement.setString(2, plan.getPlanName());
            statement.setString(3, plan.getPlanType());
            statement.setBigDecimal(4, plan.getMonthlyRental());
            statement.setBigDecimal(5, plan.getDataAllowanceGb());
            setNullableInt(statement, 6, plan.getVoiceMinutes());
            setNullableInt(statement, 7, plan.getSmsAllowance());
            statement.setInt(8, plan.getValidityDays());
            statement.setBoolean(9, plan.getInternationalRoaming() != null ? plan.getInternationalRoaming() : false);
            statement.setString(10, plan.getStatus());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    plan.setPlanId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save telecom plan with plan code: " + plan.getPlanCode(), e);
        }
    }

    @Override
    public void update(TelecomPlan plan) {
        // plan_id is the WHERE key and is never updated. created_at is never touched.
        String sql = "UPDATE telecom_plans SET plan_code = ?, plan_name = ?, plan_type = ?, " +
                "monthly_rental = ?, data_allowance_gb = ?, voice_minutes = ?, sms_allowance = ?, " +
                "validity_days = ?, international_roaming = ?, status = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE plan_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, plan.getPlanCode());
            statement.setString(2, plan.getPlanName());
            statement.setString(3, plan.getPlanType());
            statement.setBigDecimal(4, plan.getMonthlyRental());
            statement.setBigDecimal(5, plan.getDataAllowanceGb());
            setNullableInt(statement, 6, plan.getVoiceMinutes());
            setNullableInt(statement, 7, plan.getSmsAllowance());
            statement.setInt(8, plan.getValidityDays());
            statement.setBoolean(9, plan.getInternationalRoaming() != null ? plan.getInternationalRoaming() : false);
            statement.setString(10, plan.getStatus());
            statement.setLong(11, plan.getPlanId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update telecom plan with ID: " + plan.getPlanId(), e);
        }
    }

    @Override
    public void deleteById(Long planId) {
        String sql = "DELETE FROM telecom_plans WHERE plan_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, planId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete telecom plan with ID: " + planId, e);
        }
    }

    @Override
    public List<TelecomPlan> findAllActive() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM telecom_plans WHERE status = 'ACTIVE' ORDER BY plan_id";

        List<TelecomPlan> activePlans = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                activePlans.add(mapRowToTelecomPlan(resultSet));
            }
            return activePlans;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active telecom plans", e);
        }
    }

    private void setNullableInt(PreparedStatement statement, int parameterIndex, Integer value) throws SQLException {
        if (value != null) {
            statement.setInt(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, java.sql.Types.INTEGER);
        }
    }

    private TelecomPlan mapRowToTelecomPlan(ResultSet resultSet) throws SQLException {
        TelecomPlan plan = new TelecomPlan();
        plan.setPlanId(resultSet.getLong("plan_id"));
        plan.setPlanCode(resultSet.getString("plan_code"));
        plan.setPlanName(resultSet.getString("plan_name"));
        plan.setPlanType(resultSet.getString("plan_type"));
        plan.setMonthlyRental(resultSet.getBigDecimal("monthly_rental"));
        plan.setDataAllowanceGb(resultSet.getBigDecimal("data_allowance_gb"));
        plan.setVoiceMinutes(resultSet.getObject("voice_minutes", Integer.class));
        plan.setSmsAllowance(resultSet.getObject("sms_allowance", Integer.class));
        plan.setValidityDays(resultSet.getInt("validity_days"));
        plan.setInternationalRoaming(resultSet.getBoolean("international_roaming"));
        plan.setStatus(resultSet.getString("status"));
        plan.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        plan.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return plan;
    }
}
