package com.amdocs.telecom.dao;

// import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.dao.impl.TelecomPlanDAOImpl;
import com.amdocs.telecom.model.TelecomPlan;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manual integration test for TelecomPlanDAO / TelecomPlanDAOImpl against the real MySQL database.
 * Uses a seeded plan for the read-only tests, and creates/deletes its own
 * temporary plan for the write tests.
 */
public class TelecomPlanDAOTest {

    // Known seeded value from database/seed_data.sql
    private static final String SEEDED_PLAN_CODE = "PLAN-101";

    public static void main(String[] args) {
        TelecomPlanDAO telecomPlanDAO = new TelecomPlanDAOImpl();

        System.out.println("=== TelecomPlanDAO Test ===");

        Long testPlanId = null;
        String testPlanCode = "PLAN-DAO-TEST-001";

        try {
            // Test 1 - findById (via findByPlanCode to get the real seeded planId)
            TelecomPlan seededPlan = telecomPlanDAO.findByPlanCode(SEEDED_PLAN_CODE);
            if (seededPlan == null) {
                System.out.println("Test 1 - findById: FAIL (seeded plan not found)");
                return;
            }
            TelecomPlan foundById = telecomPlanDAO.findById(seededPlan.getPlanId());
            if (foundById != null) {
                System.out.println("Test 1 - findById: PASS");
                System.out.println("  planId: " + foundById.getPlanId());
                System.out.println("  planCode: " + foundById.getPlanCode());
                System.out.println("  planName: " + foundById.getPlanName());
                System.out.println("  status: " + foundById.getStatus());
            } else {
                System.out.println("Test 1 - findById: FAIL");
            }

            // Test 2 - findByPlanCode
            boolean correctPlanReturned = seededPlan != null && SEEDED_PLAN_CODE.equals(seededPlan.getPlanCode());
            System.out.println("Test 2 - findByPlanCode: " + (correctPlanReturned ? "PASS" : "FAIL"));

            // Test 3 - existsByPlanCode
            boolean existingFound = telecomPlanDAO.existsByPlanCode(SEEDED_PLAN_CODE);
            boolean fakeNotFound = !telecomPlanDAO.existsByPlanCode("PLAN-DOES-NOT-EXIST-999");
            System.out.println("Test 3 - existsByPlanCode: " + (existingFound && fakeNotFound ? "PASS" : "FAIL"));

            // Test 4 - findAllActive
            List<TelecomPlan> activePlans = telecomPlanDAO.findAllActive();
            boolean allActive = activePlans != null
                    && activePlans.stream().allMatch(p -> "ACTIVE".equals(p.getStatus()));
            System.out.println("Test 4 - findAllActive: " + (activePlans != null && allActive ? "PASS" : "FAIL"));
            if (activePlans != null) {
                System.out.println("  Active plan count: " + activePlans.size());
                for (TelecomPlan plan : activePlans) {
                    System.out.println("  - " + plan.getPlanCode() + " / " + plan.getPlanName());
                }
            }

            // Test 5 - save
            TelecomPlan testPlan = new TelecomPlan(
                    testPlanCode,
                    "DAO Test Plan",
                    "TEST",
                    new BigDecimal("499.00"),
                    new BigDecimal("20.00"),
                    30,
                    "ACTIVE"
            );
            testPlan.setVoiceMinutes(500);
            testPlan.setSmsAllowance(100);
            testPlan.setInternationalRoaming(false);

            telecomPlanDAO.save(testPlan);
            testPlanId = testPlan.getPlanId();

            if (testPlanId != null) {
                System.out.println("Test 5 - save: PASS");
                System.out.println("Generated plan ID: " + testPlanId);
            } else {
                System.out.println("Test 5 - save: FAIL (no generated ID)");
            }

            // Test 6 - findById for newly created plan, verifying all field types
            TelecomPlan newlyCreated = telecomPlanDAO.findById(testPlanId);
            boolean fieldsMatch = newlyCreated != null
                    && testPlanCode.equals(newlyCreated.getPlanCode())
                    && "DAO Test Plan".equals(newlyCreated.getPlanName())
                    && "TEST".equals(newlyCreated.getPlanType())
                    && new BigDecimal("499.00").compareTo(newlyCreated.getMonthlyRental()) == 0
                    && new BigDecimal("20.00").compareTo(newlyCreated.getDataAllowanceGb()) == 0
                    && Integer.valueOf(500).equals(newlyCreated.getVoiceMinutes())
                    && Integer.valueOf(100).equals(newlyCreated.getSmsAllowance())
                    && Integer.valueOf(30).equals(newlyCreated.getValidityDays())
                    && Boolean.FALSE.equals(newlyCreated.getInternationalRoaming())
                    && "ACTIVE".equals(newlyCreated.getStatus())
                    && newlyCreated.getCreatedAt() != null;
            System.out.println("Test 6 - find newly created plan (field verification): "
                    + (fieldsMatch ? "PASS" : "FAIL"));

            // Test 7 - update
            TelecomPlan beforeUpdate = telecomPlanDAO.findById(testPlanId);
            beforeUpdate.setPlanName("DAO Test Plan Updated");
            beforeUpdate.setPlanType("TEST_UPDATED");
            beforeUpdate.setMonthlyRental(new BigDecimal("599.00"));
            beforeUpdate.setDataAllowanceGb(new BigDecimal("40.00"));
            beforeUpdate.setVoiceMinutes(1000);
            beforeUpdate.setSmsAllowance(200);
            beforeUpdate.setValidityDays(60);
            beforeUpdate.setInternationalRoaming(true);
            beforeUpdate.setStatus("ACTIVE");
            telecomPlanDAO.update(beforeUpdate);

            TelecomPlan afterUpdate = telecomPlanDAO.findById(testPlanId);
            boolean updateApplied = afterUpdate != null
                    && "DAO Test Plan Updated".equals(afterUpdate.getPlanName())
                    && "TEST_UPDATED".equals(afterUpdate.getPlanType())
                    && new BigDecimal("599.00").compareTo(afterUpdate.getMonthlyRental()) == 0
                    && new BigDecimal("40.00").compareTo(afterUpdate.getDataAllowanceGb()) == 0
                    && Integer.valueOf(1000).equals(afterUpdate.getVoiceMinutes())
                    && Integer.valueOf(200).equals(afterUpdate.getSmsAllowance())
                    && Integer.valueOf(60).equals(afterUpdate.getValidityDays())
                    && Boolean.TRUE.equals(afterUpdate.getInternationalRoaming())
                    && "ACTIVE".equals(afterUpdate.getStatus());
            boolean updatedAtPopulated = afterUpdate != null && afterUpdate.getUpdatedAt() != null;

            System.out.println("Test 7 - update: " + (updateApplied ? "PASS" : "FAIL"));
            System.out.println("updated_at populated: " + (updatedAtPopulated ? "PASS" : "FAIL"));

            // Test 8 - findAllActive after update (ACTIVE -> present, then INACTIVE -> absent)
            List<TelecomPlan> activeAfterUpdate = telecomPlanDAO.findAllActive();
            boolean appearsWhenActive = activeAfterUpdate.stream()
                    .anyMatch(p -> testPlanCode.equals(p.getPlanCode()));

            TelecomPlan toDeactivate = telecomPlanDAO.findById(testPlanId);
            toDeactivate.setStatus("INACTIVE");
            telecomPlanDAO.update(toDeactivate);

            List<TelecomPlan> activeAfterDeactivate = telecomPlanDAO.findAllActive();
            boolean absentWhenInactive = activeAfterDeactivate.stream()
                    .noneMatch(p -> testPlanCode.equals(p.getPlanCode()));

            System.out.println("Test 8 - findAllActive after update: "
                    + (appearsWhenActive && absentWhenInactive ? "PASS" : "FAIL"));

            // Test 9 - delete
            telecomPlanDAO.deleteById(testPlanId);
            TelecomPlan afterDelete = telecomPlanDAO.findById(testPlanId);
            System.out.println("Test 9 - delete: " + (afterDelete == null ? "PASS" : "FAIL"));
            testPlanId = null; // already cleaned up

            // Test 10 - cleanup verification
            boolean stillExists = telecomPlanDAO.existsByPlanCode(testPlanCode);
            System.out.println("Test 10 - cleanup verification: " + (!stillExists ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
        } finally {
            // Safety net: make sure the temporary test plan never survives a failed run.
            if (testPlanId != null) {
                try {
                    telecomPlanDAO.deleteById(testPlanId);
                    System.out.println("Cleanup - removed temporary test plan ID: " + testPlanId);
                } catch (Exception cleanupException) {
                    System.out.println("Cleanup FAILED for test plan ID: " + testPlanId
                            + " - " + cleanupException.getMessage());
                }
            }
        }
    }
}
