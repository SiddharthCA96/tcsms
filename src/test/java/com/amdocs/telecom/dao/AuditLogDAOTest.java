package com.amdocs.telecom.dao;

import com.amdocs.telecom.dao.impl.AuditLogDAOImpl;
import com.amdocs.telecom.model.AuditLog;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Manual integration test for AuditLogDAO / AuditLogDAOImpl against the real MySQL database.
 * The project does not use JUnit, so this follows the same plain main()/PASS-FAIL convention as
 * the other DAO integration tests (e.g. NotificationDAOTest, SubscriptionHistoryDAOTest).
 *
 * database/seed_data.sql contains no audit_logs rows, so this test creates its own temporary
 * records via save() to exercise findById(), findByActor() and findByEntity(), including the
 * nullable actor_id/entity_type/entity_id/description columns. AuditLogDAO intentionally exposes
 * no delete method (audit_logs is append-only), so all temporary rows are removed at the end with
 * a direct, test-only JDBC statement - this cleanup logic is NOT part of the DAO or its
 * implementation.
 */
public class AuditLogDAOTest {

    // Temporary record #1 - fully populated, used for save()/findById()
    private static final String TEMP1_ACTOR_TYPE = "CUSTOMER";
    private static final Long TEMP1_ACTOR_ID = 1L;
    private static final String TEMP1_ACTION = "DAO_TEST_ACTION_FINDBYID";
    private static final String TEMP1_ENTITY_TYPE = "BILL";
    private static final Long TEMP1_ENTITY_ID = 1L;
    private static final String TEMP1_DESCRIPTION = "DAO_TEST temporary audit record for findById/save.";

    // Temporary record #2 - used for findByActor()
    private static final String TEMP2_ACTOR_TYPE = "ADMIN";
    private static final Long TEMP2_ACTOR_ID = 777L;
    private static final String TEMP2_ACTION = "DAO_TEST_ACTION_FINDBYACTOR";
    private static final String TEMP2_ENTITY_TYPE = "COMPLAINT";
    private static final Long TEMP2_ENTITY_ID = 2L;
    private static final String TEMP2_DESCRIPTION = "DAO_TEST temporary audit record for findByActor.";

    // Temporary record #3 - used for findByEntity()
    private static final String TEMP3_ACTOR_TYPE = "SYSTEM";
    private static final Long TEMP3_ACTOR_ID = 1L;
    private static final String TEMP3_ACTION = "DAO_TEST_ACTION_FINDBYENTITY";
    private static final String TEMP3_ENTITY_TYPE = "DAO_TEST_ENTITY";
    private static final Long TEMP3_ENTITY_ID = 555L;
    private static final String TEMP3_DESCRIPTION = "DAO_TEST temporary audit record for findByEntity.";

    // Temporary record #4 - nullable actor_id / entity_type / entity_id / description
    private static final String TEMP4_ACTOR_TYPE = "SYSTEM";
    private static final Long TEMP4_ACTOR_ID = null;
    private static final String TEMP4_ACTION = "DAO_TEST_ACTION_NULLABLE";
    private static final String TEMP4_ENTITY_TYPE = null;
    private static final Long TEMP4_ENTITY_ID = null;
    private static final String TEMP4_DESCRIPTION = null;

    public static void main(String[] args) {
        AuditLogDAO auditLogDAO = new AuditLogDAOImpl();

        System.out.println("=== AuditLogDAO Test ===");

        int testsExecuted = 0;
        int testsPassed = 0;

        Long temp1Id = null;
        Long temp2Id = null;
        Long temp3Id = null;
        Long temp4Id = null;

        try {
            // Test 1 - save() a fully-populated temporary audit record
            testsExecuted++;
            AuditLog temp1 = new AuditLog(TEMP1_ACTOR_TYPE, TEMP1_ACTOR_ID, TEMP1_ACTION,
                    TEMP1_ENTITY_TYPE, TEMP1_ENTITY_ID, TEMP1_DESCRIPTION);
            auditLogDAO.save(temp1);
            temp1Id = temp1.getAuditId();
            boolean test1Pass = temp1Id != null;
            if (test1Pass) {
                testsPassed++;
            }
            System.out.println("Test 1 - save (temporary audit record #1): " + (test1Pass ? "PASS" : "FAIL"));
            if (temp1Id != null) {
                System.out.println("  Generated auditId: " + temp1Id);
            }

            // Test 2 - findById() for the record just saved, verifying every field
            testsExecuted++;
            AuditLog foundById = temp1Id != null ? auditLogDAO.findById(temp1Id) : null;
            boolean test2Pass = foundById != null
                    && temp1Id.equals(foundById.getAuditId())
                    && TEMP1_ACTOR_TYPE.equals(foundById.getActorType())
                    && TEMP1_ACTOR_ID.equals(foundById.getActorId())
                    && TEMP1_ACTION.equals(foundById.getAction())
                    && TEMP1_ENTITY_TYPE.equals(foundById.getEntityType())
                    && TEMP1_ENTITY_ID.equals(foundById.getEntityId())
                    && TEMP1_DESCRIPTION.equals(foundById.getDescription())
                    && foundById.getCreatedAt() != null;
            if (test2Pass) {
                testsPassed++;
            }
            System.out.println("Test 2 - findById (temporary record #1, full field verification): "
                    + (test2Pass ? "PASS" : "FAIL"));
            if (foundById != null) {
                System.out.println("  auditId: " + foundById.getAuditId());
                System.out.println("  actorType: " + foundById.getActorType());
                System.out.println("  actorId: " + foundById.getActorId());
                System.out.println("  action: " + foundById.getAction());
                System.out.println("  entityType: " + foundById.getEntityType());
                System.out.println("  entityId: " + foundById.getEntityId());
                System.out.println("  description: " + foundById.getDescription());
                System.out.println("  createdAt: " + foundById.getCreatedAt());
            }

            // Test 3 - findByActor() using a temporary record with a known actorType/actorId
            testsExecuted++;
            AuditLog temp2 = new AuditLog(TEMP2_ACTOR_TYPE, TEMP2_ACTOR_ID, TEMP2_ACTION,
                    TEMP2_ENTITY_TYPE, TEMP2_ENTITY_ID, TEMP2_DESCRIPTION);
            auditLogDAO.save(temp2);
            temp2Id = temp2.getAuditId();
            final Long generatedTemp2Id = temp2Id;

            List<AuditLog> byActor = auditLogDAO.findByActor(TEMP2_ACTOR_TYPE, TEMP2_ACTOR_ID);
            boolean test3Pass = temp2Id != null && byActor != null && byActor.stream().anyMatch(a ->
                    generatedTemp2Id.equals(a.getAuditId())
                            && TEMP2_ACTOR_TYPE.equals(a.getActorType())
                            && TEMP2_ACTOR_ID.equals(a.getActorId())
                            && TEMP2_ACTION.equals(a.getAction())
                            && TEMP2_ENTITY_TYPE.equals(a.getEntityType())
                            && TEMP2_ENTITY_ID.equals(a.getEntityId())
                            && TEMP2_DESCRIPTION.equals(a.getDescription()));
            if (test3Pass) {
                testsPassed++;
            }
            System.out.println("Test 3 - findByActor (temporary record #2): " + (test3Pass ? "PASS" : "FAIL"));

            // Test 4 - findByEntity() using a temporary record with a known entityType/entityId
            testsExecuted++;
            AuditLog temp3 = new AuditLog(TEMP3_ACTOR_TYPE, TEMP3_ACTOR_ID, TEMP3_ACTION,
                    TEMP3_ENTITY_TYPE, TEMP3_ENTITY_ID, TEMP3_DESCRIPTION);
            auditLogDAO.save(temp3);
            temp3Id = temp3.getAuditId();
            final Long generatedTemp3Id = temp3Id;

            List<AuditLog> byEntity = auditLogDAO.findByEntity(TEMP3_ENTITY_TYPE, TEMP3_ENTITY_ID);
            boolean test4Pass = temp3Id != null && byEntity != null
                    && byEntity.stream().anyMatch(a -> generatedTemp3Id.equals(a.getAuditId()));
            if (test4Pass) {
                testsPassed++;
            }
            System.out.println("Test 4 - findByEntity (temporary record #3): " + (test4Pass ? "PASS" : "FAIL"));

            // Test 5 - nullable fields: actorId, entityType, entityId and description all null
            testsExecuted++;
            AuditLog temp4 = new AuditLog(TEMP4_ACTOR_TYPE, TEMP4_ACTOR_ID, TEMP4_ACTION,
                    TEMP4_ENTITY_TYPE, TEMP4_ENTITY_ID, TEMP4_DESCRIPTION);
            auditLogDAO.save(temp4);
            temp4Id = temp4.getAuditId();
            final Long generatedTemp4Id = temp4Id;

            AuditLog foundTemp4 = temp4Id != null ? auditLogDAO.findById(temp4Id) : null;
            boolean test5Pass = temp4Id != null
                    && foundTemp4 != null
                    && TEMP4_ACTOR_TYPE.equals(foundTemp4.getActorType())
                    && foundTemp4.getActorId() == null
                    && TEMP4_ACTION.equals(foundTemp4.getAction())
                    && foundTemp4.getEntityType() == null
                    && foundTemp4.getEntityId() == null
                    && foundTemp4.getDescription() == null
                    && foundTemp4.getCreatedAt() != null;
            if (test5Pass) {
                testsPassed++;
            }
            System.out.println("Test 5 - save/findById with null actorId/entityType/entityId/description: "
                    + (test5Pass ? "PASS" : "FAIL"));

            // Test 6 - findByActor("SYSTEM", null) must match actor_id IS NULL, not "= NULL"
            testsExecuted++;
            List<AuditLog> byActorNullId = auditLogDAO.findByActor(TEMP4_ACTOR_TYPE, null);
            boolean test6Pass = byActorNullId != null
                    && byActorNullId.stream().anyMatch(a -> generatedTemp4Id.equals(a.getAuditId()))
                    && byActorNullId.stream().allMatch(a -> TEMP4_ACTOR_TYPE.equals(a.getActorType())
                            && a.getActorId() == null);
            if (test6Pass) {
                testsPassed++;
            }
            System.out.println("Test 6 - findByActor(\"SYSTEM\", null) matches actor_id IS NULL: "
                    + (test6Pass ? "PASS" : "FAIL"));

            // Test 7 - findByEntity(null, null) must match entity_type IS NULL AND entity_id IS NULL
            testsExecuted++;
            List<AuditLog> byEntityAllNull = auditLogDAO.findByEntity(null, null);
            boolean test7Pass = byEntityAllNull != null
                    && byEntityAllNull.stream().anyMatch(a -> generatedTemp4Id.equals(a.getAuditId()))
                    && byEntityAllNull.stream().allMatch(a -> a.getEntityType() == null && a.getEntityId() == null);
            if (test7Pass) {
                testsPassed++;
            }
            System.out.println("Test 7 - findByEntity(null, null) matches entity_type/entity_id IS NULL: "
                    + (test7Pass ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup: AuditLogDAO deliberately has no delete method, so all temporary rows are
            // removed here with a plain JDBC statement, not through the DAO.
            boolean cleaned = true;
            cleaned &= temp1Id == null || deleteTestAuditLogRow(temp1Id);
            cleaned &= temp2Id == null || deleteTestAuditLogRow(temp2Id);
            cleaned &= temp3Id == null || deleteTestAuditLogRow(temp3Id);
            cleaned &= temp4Id == null || deleteTestAuditLogRow(temp4Id);
            System.out.println("Cleanup (direct JDBC DELETE for all temporary audit records): "
                    + (cleaned ? "PASS" : "FAIL"));

            AuditLogDAO verifyDAO = new AuditLogDAOImpl();
            boolean allGone = (temp1Id == null || verifyDAO.findById(temp1Id) == null)
                    && (temp2Id == null || verifyDAO.findById(temp2Id) == null)
                    && (temp3Id == null || verifyDAO.findById(temp3Id) == null)
                    && (temp4Id == null || verifyDAO.findById(temp4Id) == null);
            System.out.println("Cleanup verification (no temporary audit records remain): "
                    + (allGone ? "PASS" : "FAIL"));

            System.out.println("=== Summary: " + testsPassed + "/" + testsExecuted + " tests passed ===");
        }
    }

    private static boolean deleteTestAuditLogRow(Long auditId) {
        String sql = "DELETE FROM audit_logs WHERE audit_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, auditId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for audit ID: " + auditId + " - " + e.getMessage());
            return false;
        }
    }
}
