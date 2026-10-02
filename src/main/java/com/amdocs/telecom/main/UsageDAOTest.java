package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.UsageDAO;
import com.amdocs.telecom.dao.impl.UsageDAOImpl;
import com.amdocs.telecom.model.UsageRecord;
import com.amdocs.telecom.util.DBConnection;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manual integration test for UsageDAO / UsageDAOImpl against the real MySQL database.
 *
 * database/seed_data.sql already contains usage_records for subscription ID 1 (DATA, VOICE, SMS,
 * ROAMING), so findById/findBySubscriptionId/findBySubscriptionIdAndUsageType are first exercised
 * against that seeded data. A temporary usage record is then created via save() to exercise the
 * write path and re-verify all read methods against it. Because usage_records is intentionally
 * append-only (no update/delete on the DAO contract), the temporary row is removed at the end with
 * a direct, test-only JDBC statement - this cleanup logic is NOT part of the DAO or its
 * implementation.
 */
public class UsageDAOTest {

    // Reused seeded values from database/seed_data.sql
    private static final Long SEEDED_SUBSCRIPTION_ID = 1L; // SUB-100001: has DATA, VOICE, SMS, ROAMING usage
    private static final Long SEEDED_USAGE_ID = 1L;        // usage_id 1: DATA, 2.500 GB
    private static final String SEEDED_USAGE_TYPE = "SMS"; // subscription 1 has exactly one SMS row (usage_id 3)
    private static final String NON_MATCHING_USAGE_TYPE = "DAO_TEST_NONEXISTENT_TYPE";

    // Temporary record used to test save() and the append-only re-reads
    private static final String TEMP_USAGE_TYPE = "DATA";
    private static final String TEMP_UNIT = "GB";
    private static final BigDecimal TEMP_QUANTITY = new BigDecimal("9.999");
    private static final BigDecimal TEMP_CHARGE = new BigDecimal("49.99");
    private static final LocalDateTime TEMP_USAGE_DATE = LocalDateTime.of(2030, 1, 1, 0, 0, 0);

    public static void main(String[] args) {
        UsageDAO usageDAO = new UsageDAOImpl();

        System.out.println("=== UsageDAO Test ===");

        Long tempUsageId = null;

        try {
            // Test 1 - findById using an existing seeded usage record
            UsageRecord seededRecord = usageDAO.findById(SEEDED_USAGE_ID);
            boolean test1Pass = seededRecord != null;
            System.out.println("Test 1 - findById (seeded record): " + (test1Pass ? "PASS" : "FAIL"));
            if (test1Pass) {
                System.out.println("  usageId: " + seededRecord.getUsageId());
                System.out.println("  subscriptionId: " + seededRecord.getSubscriptionId());
                System.out.println("  usageDate: " + seededRecord.getUsageDate());
                System.out.println("  usageType: " + seededRecord.getUsageType());
                System.out.println("  quantity: " + seededRecord.getQuantity());
                System.out.println("  unit: " + seededRecord.getUnit());
                System.out.println("  charge: " + seededRecord.getCharge());
                System.out.println("  createdAt: " + seededRecord.getCreatedAt());
            }

            // Test 2 - findBySubscriptionId using the seeded subscription
            List<UsageRecord> bySubscription = usageDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean test2ListNotNull = bySubscription != null;
            boolean test2HasExpectedRecord = test2ListNotNull && bySubscription.stream()
                    .anyMatch(r -> SEEDED_USAGE_ID.equals(r.getUsageId()));
            boolean test2Ordered = isChronologicallyOrdered(bySubscription);
            boolean test2Pass = test2ListNotNull && test2HasExpectedRecord && test2Ordered;
            System.out.println("Test 2 - findBySubscriptionId (seeded): " + (test2Pass ? "PASS" : "FAIL"));
            System.out.println("  Record count for subscription " + SEEDED_SUBSCRIPTION_ID + ": "
                    + (test2ListNotNull ? bySubscription.size() : 0));

            // Test 3 - findBySubscriptionIdAndUsageType: a matching type and a non-matching type
            List<UsageRecord> byType = usageDAO.findBySubscriptionIdAndUsageType(
                    SEEDED_SUBSCRIPTION_ID, SEEDED_USAGE_TYPE);
            boolean test3TypeNotNull = byType != null;
            boolean test3AllMatchType = test3TypeNotNull
                    && byType.stream().allMatch(r -> SEEDED_USAGE_TYPE.equals(r.getUsageType()));
            boolean test3Ordered = isChronologicallyOrdered(byType);

            List<UsageRecord> byNonMatchingType = usageDAO.findBySubscriptionIdAndUsageType(
                    SEEDED_SUBSCRIPTION_ID, NON_MATCHING_USAGE_TYPE);
            boolean test3EmptyForNonMatch = byNonMatchingType != null && byNonMatchingType.isEmpty();

            boolean test3Pass = test3TypeNotNull && test3AllMatchType && test3Ordered && test3EmptyForNonMatch;
            System.out.println("Test 3 - findBySubscriptionIdAndUsageType (seeded): "
                    + (test3Pass ? "PASS" : "FAIL"));
            System.out.println("  Records for type '" + SEEDED_USAGE_TYPE + "': "
                    + (test3TypeNotNull ? byType.size() : 0));
            System.out.println("  Records for non-matching type '" + NON_MATCHING_USAGE_TYPE + "': "
                    + (byNonMatchingType != null ? byNonMatchingType.size() : -1));

            // Test 4 - save a temporary usage record for the seeded subscription
            UsageRecord tempRecord = new UsageRecord(
                    SEEDED_SUBSCRIPTION_ID, TEMP_USAGE_DATE, TEMP_USAGE_TYPE, TEMP_QUANTITY, TEMP_UNIT);
            tempRecord.setCharge(TEMP_CHARGE);

            usageDAO.save(tempRecord);
            tempUsageId = tempRecord.getUsageId();
            final Long generatedUsageId = tempUsageId;

            boolean test4Pass = tempUsageId != null;
            System.out.println("Test 4 - save (temporary record): " + (test4Pass ? "PASS" : "FAIL"));
            if (test4Pass) {
                System.out.println("  Generated usageId: " + tempUsageId);
            }

            // Test 5 - findById for the newly created record, verifying all important fields
            UsageRecord newlyCreated = usageDAO.findById(tempUsageId);
            boolean test5Pass = newlyCreated != null
                    && SEEDED_SUBSCRIPTION_ID.equals(newlyCreated.getSubscriptionId())
                    && TEMP_USAGE_DATE.equals(newlyCreated.getUsageDate())
                    && TEMP_USAGE_TYPE.equals(newlyCreated.getUsageType())
                    && TEMP_QUANTITY.compareTo(newlyCreated.getQuantity()) == 0
                    && TEMP_UNIT.equals(newlyCreated.getUnit())
                    && TEMP_CHARGE.compareTo(newlyCreated.getCharge()) == 0
                    && newlyCreated.getCreatedAt() != null;
            System.out.println("Test 5 - findById (newly created record, field verification): "
                    + (test5Pass ? "PASS" : "FAIL"));

            // Test 6 - findBySubscriptionId after save: the temporary record must appear
            List<UsageRecord> afterSaveBySubscription = usageDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean test6Pass = afterSaveBySubscription != null
                    && afterSaveBySubscription.stream().anyMatch(r -> generatedUsageId.equals(r.getUsageId()));
            System.out.println("Test 6 - findBySubscriptionId after save: " + (test6Pass ? "PASS" : "FAIL"));

            // Test 7 - findBySubscriptionIdAndUsageType after save: temp record must appear,
            // and every returned record must have the requested usageType
            List<UsageRecord> afterSaveByType = usageDAO.findBySubscriptionIdAndUsageType(
                    SEEDED_SUBSCRIPTION_ID, TEMP_USAGE_TYPE);
            boolean test7TempAppears = afterSaveByType != null
                    && afterSaveByType.stream().anyMatch(r -> generatedUsageId.equals(r.getUsageId()));
            boolean test7AllMatchType = afterSaveByType != null
                    && afterSaveByType.stream().allMatch(r -> TEMP_USAGE_TYPE.equals(r.getUsageType()));
            boolean test7Pass = test7TempAppears && test7AllMatchType;
            System.out.println("Test 7 - findBySubscriptionIdAndUsageType after save: "
                    + (test7Pass ? "PASS" : "FAIL"));

            // Test 8 - append-only behavior: the interface must expose only findById,
            // findBySubscriptionId, findBySubscriptionIdAndUsageType and save - no update or delete.
            Set<String> methodNames = new HashSet<>();
            for (Method method : UsageDAO.class.getDeclaredMethods()) {
                methodNames.add(method.getName());
            }
            Set<String> expectedMethods = new HashSet<>(Arrays.asList(
                    "findById", "findBySubscriptionId", "findBySubscriptionIdAndUsageType", "save"));
            boolean test8Pass = methodNames.equals(expectedMethods);
            System.out.println("Test 8 - append-only behavior (no update/delete on the DAO contract): "
                    + (test8Pass ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Test-only direct cleanup: UsageDAO deliberately has no delete method, so the
            // temporary row is removed here with a plain JDBC statement, not through the DAO.
            if (tempUsageId != null) {
                boolean cleaned = deleteTestUsageRow(tempUsageId);
                System.out.println("Temporary test data cleaned up: " + (cleaned ? "PASS" : "FAIL"));
            }
        }
    }

    private static boolean isChronologicallyOrdered(List<UsageRecord> records) {
        if (records == null) {
            return false;
        }
        for (int i = 1; i < records.size(); i++) {
            if (records.get(i - 1).getUsageDate().isAfter(records.get(i).getUsageDate())) {
                return false;
            }
        }
        return true;
    }

    private static boolean deleteTestUsageRow(Long usageId) {
        String sql = "DELETE FROM usage_records WHERE usage_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, usageId);
            statement.executeUpdate();

            UsageDAO usageDAO = new UsageDAOImpl();
            return usageDAO.findById(usageId) == null;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for usage ID: " + usageId + " - " + e.getMessage());
            return false;
        }
    }
}
