package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.impl.MobileSubscriptionDAOImpl;
import com.amdocs.telecom.model.MobileSubscription;

import java.time.LocalDate;
import java.util.List;

/**
 * Manual integration test for MobileSubscriptionDAO / MobileSubscriptionDAOImpl against the real
 * MySQL database. Uses seeded subscriptions/customer/sim/plan for the read-only tests, and
 * creates/deletes its own temporary subscription (reusing existing seeded foreign keys) for the
 * write tests.
 */
public class MobileSubscriptionDAOTest {

    // Known seeded values from database/seed_data.sql
    private static final String SEEDED_SUBSCRIPTION_NUMBER = "SUB-100001";
    private static final String SEEDED_MOBILE_NUMBER = "+919000000001";
    private static final Long SEEDED_CUSTOMER_ID = 1L;        // arjun.mehta, has SUB-100001 and SUB-100002
    private static final Long SEEDED_PLAN_ID_FOR_CUSTOMER = 1L; // customer 1 already subscribes to plan 1

    // Reused seeded foreign keys for the temporary test subscription
    private static final Long TEST_SIM_ID = 1L;
    private static final Long TEST_PLAN_ID = 3L; // customer 1 does NOT already have a subscription on plan 3

    public static void main(String[] args) {
        MobileSubscriptionDAO subscriptionDAO = new MobileSubscriptionDAOImpl();

        System.out.println("=== MobileSubscriptionDAO Test ===");

        Long testSubscriptionId = null;
        String testSubscriptionNumber = "SUB-DAO-TEST-001";
        String testMobileNumber = "+910000000099";
        String updatedMobileNumber = "+910000000098";

        try {
            // Test 1 - findById (via findBySubscriptionNumber to get the real seeded subscriptionId)
            MobileSubscription seededSubscription = subscriptionDAO.findBySubscriptionNumber(
                    SEEDED_SUBSCRIPTION_NUMBER);
            if (seededSubscription == null) {
                System.out.println("Test 1 - findById: FAIL (seeded subscription not found)");
                return;
            }
            MobileSubscription foundById = subscriptionDAO.findById(seededSubscription.getSubscriptionId());
            if (foundById != null) {
                System.out.println("Test 1 - findById: PASS");
                System.out.println("  subscriptionId: " + foundById.getSubscriptionId());
                System.out.println("  subscriptionNumber: " + foundById.getSubscriptionNumber());
                System.out.println("  customerId: " + foundById.getCustomerId());
                System.out.println("  mobileNumber: " + foundById.getMobileNumber());
                System.out.println("  simId: " + foundById.getSimId());
                System.out.println("  planId: " + foundById.getPlanId());
                System.out.println("  activationDate: " + foundById.getActivationDate());
                System.out.println("  subscriptionType: " + foundById.getSubscriptionType());
                System.out.println("  status: " + foundById.getStatus());
            } else {
                System.out.println("Test 1 - findById: FAIL");
            }

            // Test 2 - findBySubscriptionNumber
            boolean correctSubscriptionReturned = seededSubscription != null
                    && SEEDED_SUBSCRIPTION_NUMBER.equals(seededSubscription.getSubscriptionNumber());
            System.out.println("Test 2 - findBySubscriptionNumber: " + (correctSubscriptionReturned ? "PASS" : "FAIL"));

            // Test 3 - findByCustomerId
            List<MobileSubscription> customerSubscriptions = subscriptionDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean listNotNull = customerSubscriptions != null;
            boolean containsSeeded = listNotNull && customerSubscriptions.stream()
                    .anyMatch(s -> SEEDED_SUBSCRIPTION_NUMBER.equals(s.getSubscriptionNumber()));
            boolean isOrdered = true;
            if (listNotNull) {
                for (int i = 1; i < customerSubscriptions.size(); i++) {
                    if (customerSubscriptions.get(i - 1).getSubscriptionId()
                            > customerSubscriptions.get(i).getSubscriptionId()) {
                        isOrdered = false;
                        break;
                    }
                }
            }
            System.out.println("Test 3 - findByCustomerId: "
                    + (listNotNull && containsSeeded && isOrdered ? "PASS" : "FAIL"));
            System.out.println("  Subscription count for customer " + SEEDED_CUSTOMER_ID + ": "
                    + (listNotNull ? customerSubscriptions.size() : 0));

            // Test 4 - findByMobileNumber
            MobileSubscription foundByMobile = subscriptionDAO.findByMobileNumber(SEEDED_MOBILE_NUMBER);
            boolean correctByMobile = foundByMobile != null
                    && SEEDED_SUBSCRIPTION_NUMBER.equals(foundByMobile.getSubscriptionNumber());
            System.out.println("Test 4 - findByMobileNumber: " + (correctByMobile ? "PASS" : "FAIL"));

            // Test 5 - existsByMobileNumber
            boolean mobileExists = subscriptionDAO.existsByMobileNumber(SEEDED_MOBILE_NUMBER);
            boolean fakeMobileNotExists = !subscriptionDAO.existsByMobileNumber("+000000000000");
            System.out.println("Test 5 - existsByMobileNumber: "
                    + (mobileExists && fakeMobileNotExists ? "PASS" : "FAIL"));

            // Test 6 - existsByCustomerAndPlan
            boolean existingCombo = subscriptionDAO.existsByCustomerAndPlan(
                    SEEDED_CUSTOMER_ID, SEEDED_PLAN_ID_FOR_CUSTOMER);
            boolean nonExistingCombo = !subscriptionDAO.existsByCustomerAndPlan(2L, SEEDED_PLAN_ID_FOR_CUSTOMER);
            System.out.println("Test 6 - existsByCustomerAndPlan: "
                    + (existingCombo && nonExistingCombo ? "PASS" : "FAIL"));

            // Test 7 - save (reusing existing seeded customer, SIM, and plan)
            MobileSubscription testSubscription = new MobileSubscription(
                    testSubscriptionNumber,
                    SEEDED_CUSTOMER_ID,
                    testMobileNumber,
                    TEST_SIM_ID,
                    TEST_PLAN_ID,
                    LocalDate.of(2026, 9, 1),
                    "PREPAID",
                    "ACTIVE"
            );

            subscriptionDAO.save(testSubscription);
            testSubscriptionId = testSubscription.getSubscriptionId();

            if (testSubscriptionId != null) {
                System.out.println("Test 7 - save: PASS");
                System.out.println("Generated subscription ID: " + testSubscriptionId);
            } else {
                System.out.println("Test 7 - save: FAIL (no generated ID)");
            }

            // Test 8 - findById for newly created subscription, verifying saved fields
            MobileSubscription newlyCreated = subscriptionDAO.findById(testSubscriptionId);
            boolean fieldsMatch = newlyCreated != null
                    && testSubscriptionNumber.equals(newlyCreated.getSubscriptionNumber())
                    && SEEDED_CUSTOMER_ID.equals(newlyCreated.getCustomerId())
                    && testMobileNumber.equals(newlyCreated.getMobileNumber())
                    && TEST_SIM_ID.equals(newlyCreated.getSimId())
                    && TEST_PLAN_ID.equals(newlyCreated.getPlanId())
                    && LocalDate.of(2026, 9, 1).equals(newlyCreated.getActivationDate())
                    && "PREPAID".equals(newlyCreated.getSubscriptionType())
                    && "ACTIVE".equals(newlyCreated.getStatus())
                    && newlyCreated.getCreatedAt() != null;
            System.out.println("Test 8 - find newly created subscription (field verification): "
                    + (fieldsMatch ? "PASS" : "FAIL"));

            // Test 9 - findByCustomerId after save
            List<MobileSubscription> afterSaveList = subscriptionDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean tempAppears = afterSaveList.stream()
                    .anyMatch(s -> testSubscriptionNumber.equals(s.getSubscriptionNumber()));
            System.out.println("Test 9 - findByCustomerId after save: " + (tempAppears ? "PASS" : "FAIL"));

            // Test 10 - update
            MobileSubscription beforeUpdate = subscriptionDAO.findById(testSubscriptionId);
            java.time.LocalDateTime originalCreatedAt = beforeUpdate.getCreatedAt();

            beforeUpdate.setMobileNumber(updatedMobileNumber);
            beforeUpdate.setSubscriptionType("POSTPAID");
            beforeUpdate.setStatus("SUSPENDED");
            subscriptionDAO.update(beforeUpdate);

            MobileSubscription afterUpdate = subscriptionDAO.findById(testSubscriptionId);
            boolean updateApplied = afterUpdate != null
                    && updatedMobileNumber.equals(afterUpdate.getMobileNumber())
                    && "POSTPAID".equals(afterUpdate.getSubscriptionType())
                    && "SUSPENDED".equals(afterUpdate.getStatus());
            boolean updatedAtPopulated = afterUpdate != null && afterUpdate.getUpdatedAt() != null;
            boolean subscriptionIdUnchanged = afterUpdate != null
                    && testSubscriptionId.equals(afterUpdate.getSubscriptionId());
            boolean createdAtUnchanged = afterUpdate != null
                    && originalCreatedAt.equals(afterUpdate.getCreatedAt());

            System.out.println("Test 10 - update: " + (updateApplied ? "PASS" : "FAIL"));
            System.out.println("  updated_at populated: " + (updatedAtPopulated ? "PASS" : "FAIL"));
            System.out.println("  subscription_id unchanged: " + (subscriptionIdUnchanged ? "PASS" : "FAIL"));
            System.out.println("  created_at unchanged: " + (createdAtUnchanged ? "PASS" : "FAIL"));

            // Test 11 - findByMobileNumber after update
            MobileSubscription foundByNewMobile = subscriptionDAO.findByMobileNumber(updatedMobileNumber);
            MobileSubscription foundByOldMobile = subscriptionDAO.findByMobileNumber(testMobileNumber);
            boolean newMobileWorks = foundByNewMobile != null
                    && testSubscriptionId.equals(foundByNewMobile.getSubscriptionId());
            boolean oldMobileGone = foundByOldMobile == null;
            System.out.println("Test 11 - findByMobileNumber after update: "
                    + (newMobileWorks && oldMobileGone ? "PASS" : "FAIL"));

            // Test 12 - existsByMobileNumber after update
            boolean newMobileExists = subscriptionDAO.existsByMobileNumber(updatedMobileNumber);
            boolean oldMobileNotExists = !subscriptionDAO.existsByMobileNumber(testMobileNumber);
            System.out.println("Test 12 - existsByMobileNumber after update: "
                    + (newMobileExists && oldMobileNotExists ? "PASS" : "FAIL"));

            // Test 13 - existsByCustomerAndPlan after save
            boolean comboNowExists = subscriptionDAO.existsByCustomerAndPlan(SEEDED_CUSTOMER_ID, TEST_PLAN_ID);
            System.out.println("Test 13 - existsByCustomerAndPlan after save: "
                    + (comboNowExists ? "PASS" : "FAIL"));

            // Test 14 - delete
            subscriptionDAO.deleteById(testSubscriptionId);
            MobileSubscription afterDelete = subscriptionDAO.findById(testSubscriptionId);
            System.out.println("Test 14 - delete: " + (afterDelete == null ? "PASS" : "FAIL"));
            testSubscriptionId = null; // already cleaned up

            // Test 15 - cleanup verification
            boolean subscriptionNumberGone = subscriptionDAO.findBySubscriptionNumber(testSubscriptionNumber) == null;
            boolean mobileNumberGone = !subscriptionDAO.existsByMobileNumber(updatedMobileNumber)
                    && !subscriptionDAO.existsByMobileNumber(testMobileNumber);
            System.out.println("Test 15 - cleanup verification: "
                    + (subscriptionNumberGone && mobileNumberGone ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
        } finally {
            // Safety net: make sure the temporary test subscription never survives a failed run.
            if (testSubscriptionId != null) {
                try {
                    subscriptionDAO.deleteById(testSubscriptionId);
                    System.out.println("Cleanup - removed temporary test subscription ID: " + testSubscriptionId);
                } catch (Exception cleanupException) {
                    System.out.println("Cleanup FAILED for test subscription ID: " + testSubscriptionId
                            + " - " + cleanupException.getMessage());
                }
            }
        }
    }
}
