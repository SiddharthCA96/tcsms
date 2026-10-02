package com.amdocs.telecom.dao;

// import com.amdocs.telecom.dao.SIMCardDAO;
import com.amdocs.telecom.dao.impl.SIMCardDAOImpl;
import com.amdocs.telecom.model.SIMCard;

/**
 * Manual integration test for SIMCardDAO / SIMCardDAOImpl against the real MySQL database.
 * Uses a seeded SIM card for the read-only tests, and creates/deletes its own
 * temporary SIM card (not referenced by any subscription) for the write tests.
 */
public class SIMCardDAOTest {

    // Known seeded value from database/seed_data.sql
    private static final String SEEDED_SIM_NUMBER = "899110000000001";

    public static void main(String[] args) {
        SIMCardDAO simCardDAO = new SIMCardDAOImpl();

        System.out.println("=== SIMCardDAO Test ===");

        Long testSimId = null;
        String testSimNumber = "999999999999999";

        try {
            // Test 1 - findById (via findBySimNumber to get the real seeded simId)
            SIMCard seededSim = simCardDAO.findBySimNumber(SEEDED_SIM_NUMBER);
            if (seededSim == null) {
                System.out.println("Test 1 - findById: FAIL (seeded SIM not found)");
                return;
            }
            SIMCard foundById = simCardDAO.findById(seededSim.getSimId());
            if (foundById != null) {
                System.out.println("Test 1 - findById: PASS");
                System.out.println("  simId: " + foundById.getSimId());
                System.out.println("  simNumber: " + foundById.getSimNumber());
                System.out.println("  simType: " + foundById.getSimType());
                System.out.println("  status: " + foundById.getStatus());
            } else {
                System.out.println("Test 1 - findById: FAIL");
            }

            // Test 2 - findBySimNumber
            System.out.println("Test 2 - findBySimNumber: " + (seededSim != null ? "PASS" : "FAIL"));

            // Test 3 - existsBySimNumber
            boolean existingFound = simCardDAO.existsBySimNumber(SEEDED_SIM_NUMBER);
            boolean fakeNotFound = !simCardDAO.existsBySimNumber("000000000000000");
            System.out.println("Test 3 - existsBySimNumber: " + (existingFound && fakeNotFound ? "PASS" : "FAIL"));

            // Test 4 - save
            SIMCard testSimCard = new SIMCard(testSimNumber, "PHYSICAL_SIM", "AVAILABLE");

            simCardDAO.save(testSimCard);
            testSimId = testSimCard.getSimId();

            if (testSimId != null) {
                System.out.println("Test 4 - save: PASS");
                System.out.println("Generated SIM ID: " + testSimId);
            } else {
                System.out.println("Test 4 - save: FAIL (no generated ID)");
            }

            // Test 5 - find newly created SIM
            SIMCard newlyCreated = simCardDAO.findById(testSimId);
            System.out.println("Test 5 - find newly created SIM: " + (newlyCreated != null ? "PASS" : "FAIL"));

            // Test 6 - update (safe field only: status)
            SIMCard beforeUpdate = simCardDAO.findById(testSimId);
            beforeUpdate.setStatus("INACTIVE");
            simCardDAO.update(beforeUpdate);

            SIMCard afterUpdate = simCardDAO.findById(testSimId);
            boolean statusUpdated = "INACTIVE".equals(afterUpdate.getStatus());
            boolean updatedAtPopulated = afterUpdate.getUpdatedAt() != null;

            System.out.println("Test 6 - update: " + (statusUpdated ? "PASS" : "FAIL"));
            System.out.println("updated_at populated: " + (updatedAtPopulated ? "PASS" : "FAIL"));

            // Test 7 - findBySimNumber after update
            SIMCard foundAfterUpdate = simCardDAO.findBySimNumber(testSimNumber);
            System.out.println("Test 7 - findBySimNumber after update: "
                    + (foundAfterUpdate != null ? "PASS" : "FAIL"));

            // Test 8 - delete (the temporary SIM is not referenced by any subscription)
            simCardDAO.deleteById(testSimId);
            SIMCard afterDelete = simCardDAO.findById(testSimId);
            System.out.println("Test 8 - delete: " + (afterDelete == null ? "PASS" : "FAIL"));
            testSimId = null; // already cleaned up

            // Final cleanup verification
            boolean stillExists = simCardDAO.existsBySimNumber(testSimNumber);
            System.out.println("Temporary test data cleaned up: " + (!stillExists ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
        } finally {
            // Safety net: make sure the temporary test SIM never survives a failed run.
            if (testSimId != null) {
                try {
                    simCardDAO.deleteById(testSimId);
                    System.out.println("Cleanup - removed temporary test SIM ID: " + testSimId);
                } catch (Exception cleanupException) {
                    System.out.println("Cleanup FAILED for test SIM ID: " + testSimId
                            + " - " + cleanupException.getMessage());
                }
            }
        }
    }
}
