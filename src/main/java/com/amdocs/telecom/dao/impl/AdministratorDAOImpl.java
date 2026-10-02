package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.AdministratorDAO;
import com.amdocs.telecom.model.Administrator;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * JDBC implementation of AdministratorDAO.
 */
public class AdministratorDAOImpl implements AdministratorDAO {

    private static final String SELECT_COLUMNS =
            "admin_id, username, password_hash, first_name, last_name, email, status, " +
            "failed_login_attempts, locked_until, created_at, updated_at";

    @Override
    public Administrator findById(Long adminId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM administrators WHERE admin_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, adminId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToAdministrator(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find administrator by ID: " + adminId, e);
        }
    }

    @Override
    public Administrator findByUsername(String username) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM administrators WHERE username = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToAdministrator(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find administrator by username: " + username, e);
        }
    }

    @Override
    public Administrator findByEmail(String email) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM administrators WHERE email = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToAdministrator(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find administrator by email: " + email, e);
        }
    }

    @Override
    public void save(Administrator administrator) {
        // admin_id is AUTO_INCREMENT; failed_login_attempts, locked_until and created_at
        // all rely on their database defaults for a newly created administrator.
        String sql = "INSERT INTO administrators (username, password_hash, first_name, last_name, email, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, administrator.getUsername());
            statement.setString(2, administrator.getPasswordHash());
            statement.setString(3, administrator.getFirstName());
            statement.setString(4, administrator.getLastName());
            statement.setString(5, administrator.getEmail());
            statement.setString(6, administrator.getStatus());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    administrator.setAdminId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save administrator with username: "
                    + administrator.getUsername(), e);
        }
    }

    @Override
    public void update(Administrator administrator) {
        // admin_id is the WHERE key and is never updated.
        // password_hash is intentionally excluded: password changes are handled by a
        // dedicated security/service workflow, not a general profile update.
        String sql = "UPDATE administrators SET username = ?, first_name = ?, last_name = ?, email = ?, " +
                "status = ?, failed_login_attempts = ?, locked_until = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE admin_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, administrator.getUsername());
            statement.setString(2, administrator.getFirstName());
            statement.setString(3, administrator.getLastName());
            statement.setString(4, administrator.getEmail());
            statement.setString(5, administrator.getStatus());
            statement.setInt(6, administrator.getFailedLoginAttempts());

            if (administrator.getLockedUntil() != null) {
                statement.setTimestamp(7, Timestamp.valueOf(administrator.getLockedUntil()));
            } else {
                statement.setNull(7, java.sql.Types.TIMESTAMP);
            }

            statement.setLong(8, administrator.getAdminId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update administrator with ID: "
                    + administrator.getAdminId(), e);
        }
    }

    @Override
    public void deleteById(Long adminId) {
        String sql = "DELETE FROM administrators WHERE admin_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, adminId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete administrator with ID: " + adminId, e);
        }
    }

    private Administrator mapRowToAdministrator(ResultSet resultSet) throws SQLException {
        Administrator administrator = new Administrator();
        administrator.setAdminId(resultSet.getLong("admin_id"));
        administrator.setUsername(resultSet.getString("username"));
        administrator.setPasswordHash(resultSet.getString("password_hash"));
        administrator.setFirstName(resultSet.getString("first_name"));
        administrator.setLastName(resultSet.getString("last_name"));
        administrator.setEmail(resultSet.getString("email"));
        administrator.setStatus(resultSet.getString("status"));
        administrator.setFailedLoginAttempts(resultSet.getInt("failed_login_attempts"));

        Timestamp lockedUntil = resultSet.getTimestamp("locked_until");
        administrator.setLockedUntil(lockedUntil != null ? lockedUntil.toLocalDateTime() : null);

        administrator.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        administrator.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());

        return administrator;
    }
}
