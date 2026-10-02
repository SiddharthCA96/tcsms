package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.ComplaintDAO;
import com.amdocs.telecom.dao.impl.ComplaintDAOImpl;
import com.amdocs.telecom.model.Complaint;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Manual integration test for ComplaintDAO / ComplaintDAOImpl against the real MySQL database.
 *
 * database/seed_data.sql already contains 3 complaints (complaint_id 1-3) for customers 1-3 and
 * subscriptions 1, 3, 4, so findById/findByComplaintNumber/findByCustomerId/findBySubscriptionId/
 * findByStatus/findByPriority are first exercised against that seeded data. Two temporary
 * complaints are then created via save() - reusing seeded customer/subscription IDs - to exercise
 * save(), update() and the nullable subscription_id column. ComplaintDAO intentionally exposes no
 * delete method, so both temporary rows are removed at the end with a direct, test-only JDBC
 * statement - this cleanup logic is NOT part of the DAO or its implementation.
 */
public class ComplaintDAOTest {

    // Reused seeded values from database/seed_data.sql
    private static final Long SEEDED_COMPLAINT_ID = 1L;
    private static final String SEEDED_COMPLAINT_NUMBER = "COMP-10001";
    private static final Long SEEDED_CUSTOMER_ID = 1L;
    private static final Long SEEDED_SUBSCRIPTION_ID = 1L;
    private static final String SEEDED_STATUS = "OPEN";           // complaint_id 1 and 3
    private static final String SEEDED_PRIORITY = "HIGH";         // complaint_id 1 and 3
    private static final String NON_MATCHING_STATUS = "DAO_TEST_NONEXISTENT_STATUS";
    private static final String NON_MATCHING_PRIORITY = "DAO_TEST_NONEXISTENT_PRIORITY";

    // Temporary complaint #1 - has a subscription_id
    private static final String TEMP_COMPLAINT_NUMBER = "COMP-DAO-TEST-0001";
    private static final String TEMP_CATEGORY = "NETWORK";
    private static final String TEMP_DESCRIPTION = "DAO_TEST complaint - temporary record for integration testing.";
    private static final String TEMP_PRIORITY = "LOW";
    private static final String TEMP_STATUS = "OPEN";

    // Values used to exercise update() on temporary complaint #1
    private static final String UPDATED_CATEGORY = "BILLING";
    private static final String UPDATED_DESCRIPTION = "DAO_TEST complaint - updated description.";
    private static final String UPDATED_PRIORITY = "MEDIUM";
    private static final String UPDATED_STATUS = "RESOLVED";
    private static final String UPDATED_RESOLUTION = "Resolved via DAO test.";

    // Temporary complaint #2 - deliberately has a null subscription_id
    private static final String TEMP2_COMPLAINT_NUMBER = "COMP-DAO-TEST-0002";
    private static final String TEMP2_CATEGORY = "BILLING";
    private static final String TEMP2_DESCRIPTION = "DAO_TEST complaint - temporary record with null subscriptionId.";
    private static final String TEMP2_PRIORITY = "LOW";
    private static final String TEMP2_STATUS = "OPEN";

    public static void main(String[] args) {
        ComplaintDAO complaintDAO = new ComplaintDAOImpl();

        System.out.println("=== ComplaintDAO Test ===");

        Long tempComplaintId = null;
        Long tempComplaintId2 = null;

        try {
            // Test 1 - findById using an existing seeded complaint
            Complaint seededComplaint = complaintDAO.findById(SEEDED_COMPLAINT_ID);
            boolean test1Pass = seededComplaint != null;
            System.out.println("Test 1 - findById (seeded complaint): " + (test1Pass ? "PASS" : "FAIL"));
            if (test1Pass) {
                System.out.println("  complaintId: " + seededComplaint.getComplaintId());
                System.out.println("  complaintNumber: " + seededComplaint.getComplaintNumber());
                System.out.println("  customerId: " + seededComplaint.getCustomerId());
                System.out.println("  subscriptionId: " + seededComplaint.getSubscriptionId());
                System.out.println("  category: " + seededComplaint.getCategory());
                System.out.println("  description: " + seededComplaint.getDescription());
                System.out.println("  priority: " + seededComplaint.getPriority());
                System.out.println("  createdDate: " + seededComplaint.getCreatedDate());
                System.out.println("  status: " + seededComplaint.getStatus());
                System.out.println("  resolution: " + seededComplaint.getResolution());
                System.out.println("  updatedAt: " + seededComplaint.getUpdatedAt());
                System.out.println("  (resolution is nullable and is null for this seeded complaint: "
                        + (seededComplaint.getResolution() == null) + ")");
            }

            // Test 2 - findByComplaintNumber using an existing seeded complaint number
            Complaint byComplaintNumber = complaintDAO.findByComplaintNumber(SEEDED_COMPLAINT_NUMBER);
            boolean test2Pass = byComplaintNumber != null
                    && SEEDED_COMPLAINT_ID.equals(byComplaintNumber.getComplaintId());
            System.out.println("Test 2 - findByComplaintNumber (seeded complaint): " + (test2Pass ? "PASS" : "FAIL"));

            // Test 3 - findByCustomerId using a seeded customer with complaints
            List<Complaint> byCustomerId = complaintDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean test3ListNotNull = byCustomerId != null;
            boolean test3HasExpected = test3ListNotNull && byCustomerId.stream()
                    .anyMatch(c -> SEEDED_COMPLAINT_ID.equals(c.getComplaintId()));
            boolean test3Ordered = isChronologicallyOrdered(byCustomerId);
            boolean test3Pass = test3ListNotNull && test3HasExpected && test3Ordered;
            System.out.println("Test 3 - findByCustomerId (seeded): " + (test3Pass ? "PASS" : "FAIL"));
            System.out.println("  Complaint count for customer " + SEEDED_CUSTOMER_ID + ": "
                    + (test3ListNotNull ? byCustomerId.size() : 0));

            // Test 4 - findBySubscriptionId using a seeded subscription with a complaint
            List<Complaint> bySubscriptionId = complaintDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean test4ListNotNull = bySubscriptionId != null;
            boolean test4HasExpected = test4ListNotNull && bySubscriptionId.stream()
                    .anyMatch(c -> SEEDED_COMPLAINT_ID.equals(c.getComplaintId()));
            boolean test4Ordered = isChronologicallyOrdered(bySubscriptionId);
            boolean test4Pass = test4ListNotNull && test4HasExpected && test4Ordered;
            System.out.println("Test 4 - findBySubscriptionId (seeded): " + (test4Pass ? "PASS" : "FAIL"));
            System.out.println("  Complaint count for subscription " + SEEDED_SUBSCRIPTION_ID + ": "
                    + (test4ListNotNull ? bySubscriptionId.size() : 0));

            // Test 5 - findByStatus: a matching status and a non-matching status
            List<Complaint> byStatus = complaintDAO.findByStatus(SEEDED_STATUS);
            boolean test5ListNotNull = byStatus != null;
            boolean test5AllMatch = test5ListNotNull
                    && byStatus.stream().allMatch(c -> SEEDED_STATUS.equals(c.getStatus()));
            List<Complaint> byNonMatchingStatus = complaintDAO.findByStatus(NON_MATCHING_STATUS);
            boolean test5EmptyForNonMatch = byNonMatchingStatus != null && byNonMatchingStatus.isEmpty();
            boolean test5Pass = test5ListNotNull && test5AllMatch && test5EmptyForNonMatch;
            System.out.println("Test 5 - findByStatus (seeded): " + (test5Pass ? "PASS" : "FAIL"));
            System.out.println("  Complaints with status '" + SEEDED_STATUS + "': "
                    + (test5ListNotNull ? byStatus.size() : 0));

            // Test 6 - findByPriority: a matching priority and a non-matching priority
            List<Complaint> byPriority = complaintDAO.findByPriority(SEEDED_PRIORITY);
            boolean test6ListNotNull = byPriority != null;
            boolean test6AllMatch = test6ListNotNull
                    && byPriority.stream().allMatch(c -> SEEDED_PRIORITY.equals(c.getPriority()));
            List<Complaint> byNonMatchingPriority = complaintDAO.findByPriority(NON_MATCHING_PRIORITY);
            boolean test6EmptyForNonMatch = byNonMatchingPriority != null && byNonMatchingPriority.isEmpty();
            boolean test6Pass = test6ListNotNull && test6AllMatch && test6EmptyForNonMatch;
            System.out.println("Test 6 - findByPriority (seeded): " + (test6Pass ? "PASS" : "FAIL"));
            System.out.println("  Complaints with priority '" + SEEDED_PRIORITY + "': "
                    + (test6ListNotNull ? byPriority.size() : 0));

            // Test 7 - save a temporary complaint with a valid subscription_id
            Complaint tempComplaint = new Complaint(TEMP_COMPLAINT_NUMBER, SEEDED_CUSTOMER_ID, TEMP_CATEGORY,
                    TEMP_DESCRIPTION, TEMP_PRIORITY, TEMP_STATUS);
            tempComplaint.setSubscriptionId(SEEDED_SUBSCRIPTION_ID);

            complaintDAO.save(tempComplaint);
            tempComplaintId = tempComplaint.getComplaintId();
            final Long generatedComplaintId = tempComplaintId;

            boolean test7Pass = tempComplaintId != null;
            System.out.println("Test 7 - save (temporary complaint): " + (test7Pass ? "PASS" : "FAIL"));
            if (test7Pass) {
                System.out.println("  Generated complaintId: " + tempComplaintId);
            }

            // Test 8 - findById for the newly created complaint, verifying all important fields
            Complaint newlyCreated = complaintDAO.findById(tempComplaintId);
            boolean test8Pass = newlyCreated != null
                    && TEMP_COMPLAINT_NUMBER.equals(newlyCreated.getComplaintNumber())
                    && SEEDED_CUSTOMER_ID.equals(newlyCreated.getCustomerId())
                    && SEEDED_SUBSCRIPTION_ID.equals(newlyCreated.getSubscriptionId())
                    && TEMP_CATEGORY.equals(newlyCreated.getCategory())
                    && TEMP_DESCRIPTION.equals(newlyCreated.getDescription())
                    && TEMP_PRIORITY.equals(newlyCreated.getPriority())
                    && TEMP_STATUS.equals(newlyCreated.getStatus())
                    && newlyCreated.getResolution() == null
                    && newlyCreated.getCreatedDate() != null
                    && newlyCreated.getUpdatedAt() != null;
            System.out.println("Test 8 - findById (newly created complaint, field verification): "
                    + (test8Pass ? "PASS" : "FAIL"));

            // Test 9 - findByComplaintNumber for the newly created complaint
            Complaint foundByTempNumber = complaintDAO.findByComplaintNumber(TEMP_COMPLAINT_NUMBER);
            boolean test9Pass = foundByTempNumber != null
                    && generatedComplaintId.equals(foundByTempNumber.getComplaintId());
            System.out.println("Test 9 - findByComplaintNumber (newly created complaint): "
                    + (test9Pass ? "PASS" : "FAIL"));

            // Test 10 - findByCustomerId after save: the temporary complaint must appear
            List<Complaint> afterSaveByCustomerId = complaintDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean test10Pass = afterSaveByCustomerId != null
                    && afterSaveByCustomerId.stream().anyMatch(c -> generatedComplaintId.equals(c.getComplaintId()));
            System.out.println("Test 10 - findByCustomerId after save: " + (test10Pass ? "PASS" : "FAIL"));

            // Test 11 - findBySubscriptionId after save: the temporary complaint must appear
            List<Complaint> afterSaveBySubscriptionId = complaintDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean test11Pass = afterSaveBySubscriptionId != null
                    && afterSaveBySubscriptionId.stream()
                            .anyMatch(c -> generatedComplaintId.equals(c.getComplaintId()));
            System.out.println("Test 11 - findBySubscriptionId after save: " + (test11Pass ? "PASS" : "FAIL"));

            // Test 12 - update the temporary complaint's mutable fields
            newlyCreated.setCategory(UPDATED_CATEGORY);
            newlyCreated.setDescription(UPDATED_DESCRIPTION);
            newlyCreated.setPriority(UPDATED_PRIORITY);
            newlyCreated.setStatus(UPDATED_STATUS);
            newlyCreated.setResolution(UPDATED_RESOLUTION);

            complaintDAO.update(newlyCreated);

            Complaint afterUpdate = complaintDAO.findById(tempComplaintId);
            boolean test12Pass = afterUpdate != null
                    && UPDATED_CATEGORY.equals(afterUpdate.getCategory())
                    && UPDATED_DESCRIPTION.equals(afterUpdate.getDescription())
                    && UPDATED_PRIORITY.equals(afterUpdate.getPriority())
                    && UPDATED_STATUS.equals(afterUpdate.getStatus())
                    && UPDATED_RESOLUTION.equals(afterUpdate.getResolution())
                    && generatedComplaintId.equals(afterUpdate.getComplaintId())
                    && newlyCreated.getCreatedDate().equals(afterUpdate.getCreatedDate())
                    && afterUpdate.getUpdatedAt() != null;
            System.out.println("Test 12 - update (temporary complaint): " + (test12Pass ? "PASS" : "FAIL"));

            // Test 13 - findByStatus after update: present under NEW status, absent under OLD status
            List<Complaint> byNewStatus = complaintDAO.findByStatus(UPDATED_STATUS);
            boolean test13PresentInNew = byNewStatus != null
                    && byNewStatus.stream().anyMatch(c -> generatedComplaintId.equals(c.getComplaintId()));

            List<Complaint> byOldStatus = complaintDAO.findByStatus(TEMP_STATUS);
            boolean test13AbsentFromOld = byOldStatus != null
                    && byOldStatus.stream().noneMatch(c -> generatedComplaintId.equals(c.getComplaintId()));

            boolean test13Pass = test13PresentInNew && test13AbsentFromOld;
            System.out.println("Test 13 - findByStatus after update: " + (test13Pass ? "PASS" : "FAIL"));

            // Test 14 - findByPriority after update: present under NEW priority
            List<Complaint> byNewPriority = complaintDAO.findByPriority(UPDATED_PRIORITY);
            boolean test14Pass = byNewPriority != null
                    && byNewPriority.stream().anyMatch(c -> generatedComplaintId.equals(c.getComplaintId()));
            System.out.println("Test 14 - findByPriority after update: " + (test14Pass ? "PASS" : "FAIL"));

            // Test 15 - nullable subscription_id handling: a second temporary complaint with no subscription
            Complaint tempComplaint2 = new Complaint(TEMP2_COMPLAINT_NUMBER, SEEDED_CUSTOMER_ID, TEMP2_CATEGORY,
                    TEMP2_DESCRIPTION, TEMP2_PRIORITY, TEMP2_STATUS);
            // subscriptionId intentionally left null

            complaintDAO.save(tempComplaint2);
            tempComplaintId2 = tempComplaint2.getComplaintId();

            Complaint foundTemp2 = complaintDAO.findById(tempComplaintId2);
            boolean test15Pass = tempComplaintId2 != null
                    && foundTemp2 != null
                    && foundTemp2.getSubscriptionId() == null
                    && TEMP2_COMPLAINT_NUMBER.equals(foundTemp2.getComplaintNumber());
            System.out.println("Test 15 - save/findById with null subscriptionId: " + (test15Pass ? "PASS" : "FAIL"));
            if (tempComplaintId2 != null) {
                System.out.println("  Generated complaintId (null-subscription complaint): " + tempComplaintId2);
            }

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup: ComplaintDAO deliberately has no delete method, so both temporary rows are
            // removed here with a plain JDBC statement, not through the DAO.
            boolean cleanedFirst = tempComplaintId == null || deleteTestComplaintRow(tempComplaintId);
            boolean cleanedSecond = tempComplaintId2 == null || deleteTestComplaintRow(tempComplaintId2);
            System.out.println("Cleanup (direct JDBC DELETE for both temporary complaints): "
                    + (cleanedFirst && cleanedSecond ? "PASS" : "FAIL"));

            ComplaintDAO verifyDAO = new ComplaintDAOImpl();
            boolean firstGone = tempComplaintId == null || verifyDAO.findById(tempComplaintId) == null;
            boolean secondGone = tempComplaintId2 == null || verifyDAO.findById(tempComplaintId2) == null;
            System.out.println("Cleanup verification (no temporary complaints remain): "
                    + (firstGone && secondGone ? "PASS" : "FAIL"));
        }
    }

    private static boolean isChronologicallyOrdered(List<Complaint> complaints) {
        if (complaints == null) {
            return false;
        }
        for (int i = 1; i < complaints.size(); i++) {
            if (complaints.get(i - 1).getCreatedDate().isAfter(complaints.get(i).getCreatedDate())) {
                return false;
            }
        }
        return true;
    }

    private static boolean deleteTestComplaintRow(Long complaintId) {
        String sql = "DELETE FROM complaints WHERE complaint_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, complaintId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for complaint ID: " + complaintId + " - " + e.getMessage());
            return false;
        }
    }
}
