package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.SIMCardDAO;
import com.amdocs.telecom.model.SIMCard;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC implementation of SIMCardDAO.
 */
public class SIMCardDAOImpl implements SIMCardDAO {

    private static final String SELECT_COLUMNS =
            "sim_id, sim_number, sim_type, status, created_at, updated_at";

    @Override
    public SIMCard findById(Long simId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sim_cards WHERE sim_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, simId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToSIMCard(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SIM card by ID: " + simId, e);
        }
    }

    @Override
    public SIMCard findBySimNumber(String simNumber) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sim_cards WHERE sim_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, simNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToSIMCard(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SIM card by SIM number: " + simNumber, e);
        }
    }

    @Override
    public boolean existsBySimNumber(String simNumber) {
        // "SELECT 1" avoids pulling back a whole row just to check existence.
        String sql = "SELECT 1 FROM sim_cards WHERE sim_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, simNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of SIM number: " + simNumber, e);
        }
    }

    @Override
    public void save(SIMCard simCard) {
        // sim_id is AUTO_INCREMENT and created_at/updated_at rely on their database defaults.
        String sql = "INSERT INTO sim_cards (sim_number, sim_type, status) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, simCard.getSimNumber());
            statement.setString(2, simCard.getSimType());
            statement.setString(3, simCard.getStatus());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    simCard.setSimId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save SIM card with SIM number: " + simCard.getSimNumber(), e);
        }
    }

    @Override
    public void update(SIMCard simCard) {
        // sim_id is the WHERE key and is never updated.
        String sql = "UPDATE sim_cards SET sim_number = ?, sim_type = ?, status = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE sim_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, simCard.getSimNumber());
            statement.setString(2, simCard.getSimType());
            statement.setString(3, simCard.getStatus());
            statement.setLong(4, simCard.getSimId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update SIM card with ID: " + simCard.getSimId(), e);
        }
    }

    @Override
    public void deleteById(Long simId) {
        String sql = "DELETE FROM sim_cards WHERE sim_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, simId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete SIM card with ID: " + simId, e);
        }
    }

    private SIMCard mapRowToSIMCard(ResultSet resultSet) throws SQLException {
        SIMCard simCard = new SIMCard();
        simCard.setSimId(resultSet.getLong("sim_id"));
        simCard.setSimNumber(resultSet.getString("sim_number"));
        simCard.setSimType(resultSet.getString("sim_type"));
        simCard.setStatus(resultSet.getString("status"));
        simCard.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        simCard.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return simCard;
    }
}
