package com.amdocs.telecom.dao;

// import com.amdocs.telecom.dao.CustomerDAO;
import com.amdocs.telecom.dao.impl.CustomerDAOImpl;
import com.amdocs.telecom.model.Customer;

import java.time.LocalDate;

/**
 * Manual integration test for CustomerDAO / CustomerDAOImpl against the real MySQL database.
 * Uses seeded customer "arjun.mehta" (customer_id = 1) for the read-only tests,
 * and creates/deletes its own temporary customer for the write tests.
 */
public class CustomerDAOTest {

    // Known seeded values from database/seed_data.sql
    private static final Long SEEDED_CUSTOMER_ID = 1L;
    private static final String SEEDED_USERNAME = "arjun.mehta";
    private static final String SEEDED_EMAIL = "arjun@example.com";
    private static final String SEEDED_MOBILE_NUMBER = "9000000001";

    public static void main(String[] args) {
        CustomerDAO customerDAO = new CustomerDAOImpl();

        System.out.println("=== CustomerDAO Test ===");

        Long testCustomerId = null;

        try {
            // Test 1 - findById
            Customer foundById = customerDAO.findById(SEEDED_CUSTOMER_ID);
            if (foundById != null) {
                System.out.println("Test 1 - findById: PASS");
                System.out.println("  customerId: " + foundById.getCustomerId());
                System.out.println("  customerNumber: " + foundById.getCustomerNumber());
                System.out.println("  firstName: " + foundById.getFirstName());
                System.out.println("  lastName: " + foundById.getLastName());
                System.out.println("  email: " + foundById.getEmail());
                System.out.println("  username: " + foundById.getUsername());
            } else {
                System.out.println("Test 1 - findById: FAIL (seeded customer not found)");
            }

            // Test 2 - findByUsername
            Customer foundByUsername = customerDAO.findByUsername(SEEDED_USERNAME);
            System.out.println("Test 2 - findByUsername: " + (foundByUsername != null ? "PASS" : "FAIL"));

            // Test 3 - findByEmail
            Customer foundByEmail = customerDAO.findByEmail(SEEDED_EMAIL);
            System.out.println("Test 3 - findByEmail: " + (foundByEmail != null ? "PASS" : "FAIL"));

            // Test 4 - existsByMobileNumber
            boolean mobileExists = customerDAO.existsByMobileNumber(SEEDED_MOBILE_NUMBER);
            System.out.println("Test 4 - existsByMobileNumber: " + (mobileExists ? "PASS" : "FAIL"));

            // Test 5 - save
            Customer testCustomer = new Customer(
                    "CUST_DAO_TEST_001",
                    "Test",
                    "Customer",
                    LocalDate.of(2000, 1, 1),
                    "dao_test_customer@example.com",
                    "9999999999",
                    "dao_test_customer",
                    "DEV_HASH_TEST"
            );
            testCustomer.setAddress("Old Address");
            testCustomer.setCity("Old City");
            testCustomer.setCountry("India");
            testCustomer.setAccountStatus("ACTIVE");

            customerDAO.save(testCustomer);
            testCustomerId = testCustomer.getCustomerId();

            if (testCustomerId != null) {
                System.out.println("Test 5 - save: PASS");
                System.out.println("Generated customer ID: " + testCustomerId);
            } else {
                System.out.println("Test 5 - save: FAIL (no generated ID)");
            }

            // Test 6 - find newly created customer
            Customer newlyCreated = customerDAO.findById(testCustomerId);
            System.out.println("Test 6 - find newly created customer: " + (newlyCreated != null ? "PASS" : "FAIL"));

            // Test 7 - update (profile fields only, password hash must remain untouched)
            Customer beforeUpdate = customerDAO.findById(testCustomerId);
            String originalPasswordHash = beforeUpdate.getPasswordHash();

            beforeUpdate.setAddress("New Address");
            beforeUpdate.setCity("New City");
            customerDAO.update(beforeUpdate);

            Customer afterUpdate = customerDAO.findById(testCustomerId);
            boolean profileUpdated = "New Address".equals(afterUpdate.getAddress())
                    && "New City".equals(afterUpdate.getCity());
            boolean passwordHashUnchanged = originalPasswordHash.equals(afterUpdate.getPasswordHash());

            System.out.println("Test 7 - update: " + (profileUpdated ? "PASS" : "FAIL"));
            System.out.println("Password hash unchanged: " + (passwordHashUnchanged ? "PASS" : "FAIL"));

            // Test 8 - delete
            customerDAO.deleteById(testCustomerId);
            Customer afterDelete = customerDAO.findById(testCustomerId);
            System.out.println("Test 8 - delete: " + (afterDelete == null ? "PASS" : "FAIL"));
            testCustomerId = null; // already cleaned up

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
        } finally {
            // Safety net: make sure the temporary test customer never survives a failed run.
            if (testCustomerId != null) {
                try {
                    customerDAO.deleteById(testCustomerId);
                    System.out.println("Cleanup - removed temporary test customer ID: " + testCustomerId);
                } catch (Exception cleanupException) {
                    System.out.println("Cleanup FAILED for test customer ID: " + testCustomerId
                            + " - " + cleanupException.getMessage());
                }
            }
        }
    }
}
