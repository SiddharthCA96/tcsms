package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.AuditLogDAO;
import com.amdocs.telecom.model.AuditLog;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of AuditLogDAO.
 * audit_logs is append-only, so only findById, findByActor, findByEntity and save are
 * implemented - there is no update or delete.
 */
public class AuditLogDAOImpl implements AuditLogDAO {

    private static final String SELECT_COLUMNS =
            "audit_id, actor_type, actor_id, action, entity_type, entity_id, description, created_at";

    @Override
    public AuditLog findById(Long auditId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM audit_logs WHERE audit_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, auditId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToAuditLog(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find audit log by ID: " + auditId, e);
        }
    }

    @Override
    public List<AuditLog> findByActor(String actorType, Long actorId) {
        // actor_id is nullable (e.g. a SYSTEM actor may have no ID), so a null actorId is
        // matched with "IS NULL" rather than "= ?", which would never match in SQL.
        String actorIdClause = actorId != null ? "actor_id = ?" : "actor_id IS NULL";
        String sql = "SELECT " + SELECT_COLUMNS + " FROM audit_logs WHERE actor_type = ? AND " +
                actorIdClause + " ORDER BY created_at";

        List<AuditLog> auditLogs = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, actorType);
            if (actorId != null) {
                statement.setLong(2, actorId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapRowToAuditLog(resultSet));
                }
            }
            return auditLogs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find audit logs for actor type: " + actorType
                    + " and actor ID: " + actorId, e);
        }
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        // entity_type and entity_id are both nullable, so a null value is matched with
        // "IS NULL" rather than "= ?", which would never match in SQL.
        String entityTypeClause = entityType != null ? "entity_type = ?" : "entity_type IS NULL";
        String entityIdClause = entityId != null ? "entity_id = ?" : "entity_id IS NULL";
        String sql = "SELECT " + SELECT_COLUMNS + " FROM audit_logs WHERE " + entityTypeClause +
                " AND " + entityIdClause + " ORDER BY created_at";

        List<AuditLog> auditLogs = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int parameterIndex = 1;
            if (entityType != null) {
                statement.setString(parameterIndex++, entityType);
            }
            if (entityId != null) {
                statement.setLong(parameterIndex, entityId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapRowToAuditLog(resultSet));
                }
            }
            return auditLogs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find audit logs for entity type: " + entityType
                    + " and entity ID: " + entityId, e);
        }
    }

    @Override
    public void save(AuditLog auditLog) {
        // audit_id is AUTO_INCREMENT and created_at defaults to CURRENT_TIMESTAMP in the schema.
        String sql = "INSERT INTO audit_logs (actor_type, actor_id, action, entity_type, entity_id, " +
                "description) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, auditLog.getActorType());
            setNullableLong(statement, 2, auditLog.getActorId());
            statement.setString(3, auditLog.getAction());
            statement.setString(4, auditLog.getEntityType());
            setNullableLong(statement, 5, auditLog.getEntityId());
            statement.setString(6, auditLog.getDescription());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    auditLog.setAuditId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save audit log for action: " + auditLog.getAction(), e);
        }
    }

    private void setNullableLong(PreparedStatement statement, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            statement.setLong(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.BIGINT);
        }
    }

    private AuditLog mapRowToAuditLog(ResultSet resultSet) throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditId(resultSet.getLong("audit_id"));
        auditLog.setActorType(resultSet.getString("actor_type"));

        long actorId = resultSet.getLong("actor_id");
        auditLog.setActorId(resultSet.wasNull() ? null : actorId);

        auditLog.setAction(resultSet.getString("action"));
        auditLog.setEntityType(resultSet.getString("entity_type"));

        long entityId = resultSet.getLong("entity_id");
        auditLog.setEntityId(resultSet.wasNull() ? null : entityId);

        auditLog.setDescription(resultSet.getString("description"));
        auditLog.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());

        return auditLog;
    }
}
