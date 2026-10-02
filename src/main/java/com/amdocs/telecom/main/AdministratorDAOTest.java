package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.AdministratorDAO;
import com.amdocs.telecom.dao.impl.AdministratorDAOImpl;
import com.amdocs.telecom.model.Administrator;

/**
 * Manual integration test for AdministratorDAO / AdministratorDAOImpl against the real MySQL database.
 * Uses the seeded "admin" administrator for the read-only tests,
 * and creates/deletes its own temporary administrator for the write tests.
 */
public class AdministratorDAOTest {

    private static final String SEEDED_USERNAME = "admin";

    public static void main(String[] args) {
        AdministratorDAO administratorDAO = new AdministratorDAOImpl();

        System.out.println("=== AdministratorDAO Test ===");

        Long testAdminId = null;
        String testUsername = "dao_test_admin";

        try {
            // Test 1 - findById (via findByUsername to get the real seeded adminId)
            Administrator seededAdmin = administratorDAO.findByUsername(SEEDED_USERNAME);
            if (seededAdmin == null) {
                System.out.println("Test 1 - findById: FAIL (seeded admin not found)");
                return;
            }
            Administrator foundById = administratorDAO.findById(seededAdmin.getAdminId());
            if (foundById != null) {
                System.out.println("Test 1 - findById: PASS");
                System.out.println("  adminId: " + foundById.getAdminId());
                System.out.println("  username: " + foundById.getUsername());
                System.out.println("  firstName: " + foundById.getFirstName());
                System.out.println("  lastName: " + foundById.getLastName());
                System.out.println("  email: " + foundById.getEmail());
                System.out.println("  status: " + foundById.getStatus());
            } else {
                System.out.println("Test 1 - findById: FAIL");
            }

            // Test 2 - findByUsername
            System.out.println("Test 2 - findByUsername: " + (seededAdmin != null ? "PASS" : "FAIL"));

            // Test 3 - findByEmail
            Administrator foundByEmail = administratorDAO.findByEmail(seededAdmin.getEmail());
            System.out.println("Test 3 - findByEmail: " + (foundByEmail != null ? "PASS" : "FAIL"));

            // Test 4 - save
            Administrator testAdministrator = new Administrator(
                    testUsername,
                    "DEV_HASH_TEST",
                    "Temp",
                    "Admin",
                    "dao_test_admin@example.com",
                    "ACTIVE"
            );

            administratorDAO.save(testAdministrator);
            testAdminId = testAdministrator.getAdminId();

            if (testAdminId != null) {
                System.out.println("Test 4 - save: PASS");
                System.out.println("Generated admin ID: " + testAdminId);
            } else {
                System.out.println("Test 4 - save: FAIL (no generated ID)");
            }

            // Test 5 - find newly created administrator
            Administrator newlyCreated = administratorDAO.findById(testAdminId);
            System.out.println("Test 5 - find newly created administrator: "
                    + (newlyCreated != null ? "PASS" : "FAIL"));

            // Test 6 - update (safe profile fields only, password hash must remain untouched)
            Administrator beforeUpdate = administratorDAO.findById(testAdminId);
            String originalPasswordHash = beforeUpdate.getPasswordHash();

            String updatedEmail = "dao_test_admin_updated@example.com";
            beforeUpdate.setFirstName("TempUpdated");
            beforeUpdate.setLastName("AdminUpdated");
            beforeUpdate.setEmail(updatedEmail);
            administratorDAO.update(beforeUpdate);

            Administrator afterUpdate = administratorDAO.findById(testAdminId);
            boolean profileUpdated = "TempUpdated".equals(afterUpdate.getFirstName())
                    && "AdminUpdated".equals(afterUpdate.getLastName())
                    && updatedEmail.equals(afterUpdate.getEmail());
            boolean passwordHashUnchanged = originalPasswordHash.equals(afterUpdate.getPasswordHash());
            boolean updatedAtPopulated = afterUpdate.getUpdatedAt() != null;

            System.out.println("Test 6 - update: " + (profileUpdated ? "PASS" : "FAIL"));
            System.out.println("Password hash unchanged: " + (passwordHashUnchanged ? "PASS" : "FAIL"));
            System.out.println("updated_at populated: " + (updatedAtPopulated ? "PASS" : "FAIL"));

            // Test 7 - findByEmail after update
            Administrator foundByUpdatedEmail = administratorDAO.findByEmail(updatedEmail);
            System.out.println("Test 7 - findByEmail after update: "
                    + (foundByUpdatedEmail != null ? "PASS" : "FAIL"));

            // Test 8 - delete
            administratorDAO.deleteById(testAdminId);
            Administrator afterDelete = administratorDAO.findById(testAdminId);
            System.out.println("Test 8 - delete: " + (afterDelete == null ? "PASS" : "FAIL"));
            testAdminId = null; // already cleaned up

            // Final cleanup verification
            Administrator shouldNotExist = administratorDAO.findByUsername(testUsername);
            System.out.println("Temporary test data cleaned up: " + (shouldNotExist == null ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
        } finally {
            // Safety net: make sure the temporary test administrator never survives a failed run.
            if (testAdminId != null) {
                try {
                    administratorDAO.deleteById(testAdminId);
                    System.out.println("Cleanup - removed temporary test administrator ID: " + testAdminId);
                } catch (Exception cleanupException) {
                    System.out.println("Cleanup FAILED for test administrator ID: " + testAdminId
                            + " - " + cleanupException.getMessage());
                }
            }
        }
    }
}
