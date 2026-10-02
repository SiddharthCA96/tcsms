package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.AuditLog;

import java.util.List;

/**
 * DAO contract for persisting and retrieving audit log records.
 * audit_logs is append-only, so this interface has no update()/delete().
 */
public interface AuditLogDAO {

    AuditLog findById(Long auditId);

    List<AuditLog> findByActor(String actorType, Long actorId);

    List<AuditLog> findByEntity(String entityType, Long entityId);

    void save(AuditLog auditLog);
}
