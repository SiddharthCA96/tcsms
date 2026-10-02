package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.SubscriptionHistoryDAO;
import com.amdocs.telecom.dao.impl.SubscriptionHistoryDAOImpl;
import com.amdocs.telecom.model.SubscriptionHistory;
import com.amdocs.telecom.util.DBConnection;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manual integration test for SubscriptionHistoryDAO / SubscriptionHistoryDAOImpl against the
 * real MySQL database.
 *
 * database/seed_data.sql contains no subscription_history rows, so this test creates one
 * temporary record (reusing an existing seeded subscription and existing seeded plan IDs) and
 * uses it to exercise findById() and findBySubscriptionId(), as instructed. Because
 * SubscriptionHistoryDAO is intentionally append-only (no update/delete), the temporary row is
 * removed at the end with a direct, test-only JDBC statement - this cleanup logic is NOT part of
 * the DAO or its implementation.
 */
public class SubscriptionHistoryDAOTest {

    // Reused seeded values from database/seed_data.sql
    private static final Long SEEDED_SUBSCRIPTION_ID = 1L; // SUB-100001, currently on plan_id 1
    private static final Long OLD_PLAN_ID = 1L;             // existing seeded plan (PLAN-101)
    private static final Long NEW_PLAN_ID = 2L;             // existing seeded plan (PLAN-102)
    private static final String TEST_CHANGE_REASON = "DAO_TEST_HISTORY_RECORD";
    private static final String TEST_CHANGED_BY = "dao_test_harness";

    public static void main(String[] args) {
        SubscriptionHistoryDAO historyDAO = new SubscriptionHistoryDAOImpl();

        System.out.println("=== SubscriptionHistoryDAO Test ===");

        Long testHistoryId = null;

        try {
            // Confirm there is no seeded history data, as expected from seed_data.sql.
            List<SubscriptionHistory> existingHistory = historyDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            if (existingHistory.isEmpty()) {
                System.out.println("No seeded subscription_history rows found for subscription ID "
                        + SEEDED_SUBSCRIPTION_ID + ". Creating a temporary record via save() before "
                        + "testing findById() and findBySubscriptionId().");
            }

            // Create the temporary record first (functionally Test 3) so Test 1 / Test 2 have data.
            SubscriptionHistory testHistory = new SubscriptionHistory(
                    SEEDED_SUBSCRIPTION_ID, NEW_PLAN_ID, TEST_CHANGED_BY);
            testHistory.setOldPlanId(OLD_PLAN_ID);
            testHistory.setChangeReason(TEST_CHANGE_REASON);

            historyDAO.save(testHistory);
            testHistoryId = testHistory.getHistoryId();
            final Long generatedHistoryId = testHistoryId;

            // Test 1 - findById
            SubscriptionHistory foundById = historyDAO.findById(testHistoryId);
            if (foundById != null) {
                System.out.println("Test 1 - findById: PASS");
                System.out.println("  historyId: " + foundById.getHistoryId());
                System.out.println("  subscriptionId: " + foundById.getSubscriptionId());
                System.out.println("  oldPlanId: " + foundById.getOldPlanId());
                System.out.println("  newPlanId: " + foundById.getNewPlanId());
                System.out.println("  changeDate: " + foundById.getChangeDate());
                System.out.println("  changeReason: " + foundById.getChangeReason());
                System.out.println("  changedBy: " + foundById.getChangedBy());
            } else {
                System.out.println("Test 1 - findById: FAIL");
            }

            // Test 2 - findBySubscriptionId
            List<SubscriptionHistory> bySubscription = historyDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean listNotNull = bySubscription != null;
            boolean containsTempRecord = listNotNull && bySubscription.stream()
                    .anyMatch(h -> generatedHistoryId.equals(h.getHistoryId()));
            boolean chronologicallyOrdered = true;
            if (listNotNull) {
                for (int i = 1; i < bySubscription.size(); i++) {
                    if (bySubscription.get(i - 1).getChangeDate().isAfter(bySubscription.get(i).getChangeDate())) {
                        chronologicallyOrdered = false;
                        break;
                    }
                }
            }
            System.out.println("Test 2 - findBySubscriptionId: "
                    + (listNotNull && containsTempRecord && chronologicallyOrdered ? "PASS" : "FAIL"));
            System.out.println("  Record count for subscription " + SEEDED_SUBSCRIPTION_ID + ": "
                    + (listNotNull ? bySubscription.size() : 0));

            // Test 3 - save (already executed above; report the outcome here)
            System.out.println("Test 3 - save: " + (testHistoryId != null ? "PASS" : "FAIL"));
            if (testHistoryId != null) {
                System.out.println("Generated history ID: " + testHistoryId);
            }

            // Test 4 - findById for newly created history, verifying saved fields
            SubscriptionHistory newlyCreated = historyDAO.findById(testHistoryId);
            boolean fieldsMatch = newlyCreated != null
                    && SEEDED_SUBSCRIPTION_ID.equals(newlyCreated.getSubscriptionId())
                    && OLD_PLAN_ID.equals(newlyCreated.getOldPlanId())
                    && NEW_PLAN_ID.equals(newlyCreated.getNewPlanId())
                    && TEST_CHANGE_REASON.equals(newlyCreated.getChangeReason())
                    && TEST_CHANGED_BY.equals(newlyCreated.getChangedBy())
                    && newlyCreated.getChangeDate() != null;
            System.out.println("Test 4 - find newly created history (field verification): "
                    + (fieldsMatch ? "PASS" : "FAIL"));

            // Test 5 - findBySubscriptionId after save, verifying presence and chronological order
            List<SubscriptionHistory> afterSave = historyDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean tempAppears = afterSave.stream().anyMatch(h -> generatedHistoryId.equals(h.getHistoryId()));
            boolean stillOrdered = true;
            for (int i = 1; i < afterSave.size(); i++) {
                if (afterSave.get(i - 1).getChangeDate().isAfter(afterSave.get(i).getChangeDate())) {
                    stillOrdered = false;
                    break;
                }
            }
            System.out.println("Test 5 - findBySubscriptionId after save: "
                    + (tempAppears && stillOrdered ? "PASS" : "FAIL"));

            // Test 6 - append-only behavior: the interface must expose only findById,
            // findBySubscriptionId and save - no update or delete.
            Set<String> methodNames = new HashSet<>();
            for (Method method : SubscriptionHistoryDAO.class.getDeclaredMethods()) {
                methodNames.add(method.getName());
            }
            Set<String> expectedMethods = new HashSet<>(Arrays.asList("findById", "findBySubscriptionId", "save"));
            boolean appendOnly = methodNames.equals(expectedMethods);
            System.out.println("Test 6 - append-only behavior (no update/delete on the DAO contract): "
                    + (appendOnly ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
        } finally {
            // Test-only direct cleanup: SubscriptionHistoryDAO deliberately has no delete method,
            // so the temporary row is removed here with a plain JDBC statement, not through the DAO.
            if (testHistoryId != null) {
                boolean cleaned = deleteTestHistoryRow(testHistoryId);
                System.out.println("Temporary test data cleaned up: " + (cleaned ? "PASS" : "FAIL"));
            }
        }
    }

    private static boolean deleteTestHistoryRow(Long historyId) {
        String sql = "DELETE FROM subscription_history WHERE history_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, historyId);
            statement.executeUpdate();

            SubscriptionHistoryDAO historyDAO = new SubscriptionHistoryDAOImpl();
            return historyDAO.findById(historyId) == null;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for history ID: " + historyId + " - " + e.getMessage());
            return false;
        }
    }
}
