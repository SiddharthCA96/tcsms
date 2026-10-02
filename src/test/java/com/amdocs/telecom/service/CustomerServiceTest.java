package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.CustomerDAO;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.service.impl.CustomerServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Plain-Java unit test for CustomerServiceImpl business logic, independent of JDBC/MySQL.
 * DAO persistence itself is already covered by CustomerDAOTest; this test exercises only the
 * business rules CustomerServiceImpl adds on top of CustomerDAO (uniqueness checks, required-field
 * validation, protected-field handling, and the deactivate/reactivate lifecycle), using an
 * in-memory fake CustomerDAO defined below rather than a real database connection.
 */
public class CustomerServiceTest {

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    private static int customerCounter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("CustomerServiceTest");
        System.out.println("========================================");

        testRegisterCustomerSuccess();
        testRegisterCustomerDuplicateUsername();
        testRegisterCustomerDuplicateEmail();
        testRegisterCustomerDuplicateMobile();
        testRegisterCustomerRequiredFieldValidation();
        testGetCustomerProfile();
        testIsUsernameAvailable();
        testIsEmailAvailable();
        testIsMobileNumberAvailable();
        testUpdateCustomerProfileSuccess();
        testUpdateCustomerProfileDuplicateEmail();
        testUpdateCustomerProfileDuplicateMobile();
        testUpdateCustomerProfileDuplicateUsername();
        testUpdateCustomerProfileProtectedFields();
        testDeactivateThenReactivateCustomer();
        testDeactivateMissingCustomer();
        testReactivateMissingCustomer();

        System.out.println("========================================");
        System.out.println("Tests executed: " + testsExecuted);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsExecuted - testsPassed));
        System.out.println("========================================");
    }

    // Test 1
    private static void testRegisterCustomerSuccess() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("REGISTER_OK");
        // accountStatus intentionally left unset to verify the ACTIVE default is applied.

        Customer registered = service.registerCustomer(customer);

        boolean idAssigned = registered.getCustomerId() != null;
        boolean storedInDao = idAssigned && dao.findById(registered.getCustomerId()) != null;
        boolean statusDefaultedToActive = "ACTIVE".equals(registered.getAccountStatus());

        check("Test 1 - registerCustomer() successful registration",
                idAssigned && storedInDao && statusDefaultedToActive);
    }

    // Test 2
    private static void testRegisterCustomerDuplicateUsername() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer existing = buildValidCustomer("DUP_USERNAME_EXISTING");
        service.registerCustomer(existing);

        Customer duplicate = buildValidCustomer("DUP_USERNAME_NEW");
        duplicate.setUsername(existing.getUsername());

        RuntimeException thrown = expectRuntimeException(() -> service.registerCustomer(duplicate));
        boolean mentionsUsername = thrown != null && thrown.getMessage() != null
                && thrown.getMessage().toLowerCase().contains("username");

        check("Test 2 - registerCustomer() duplicate username rejected", thrown != null && mentionsUsername);
    }

    // Test 3
    private static void testRegisterCustomerDuplicateEmail() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer existing = buildValidCustomer("DUP_EMAIL_EXISTING");
        service.registerCustomer(existing);

        Customer duplicate = buildValidCustomer("DUP_EMAIL_NEW");
        duplicate.setEmail(existing.getEmail());

        RuntimeException thrown = expectRuntimeException(() -> service.registerCustomer(duplicate));
        boolean mentionsEmail = thrown != null && thrown.getMessage() != null
                && thrown.getMessage().toLowerCase().contains("email");

        check("Test 3 - registerCustomer() duplicate email rejected", thrown != null && mentionsEmail);
    }

    // Test 4
    private static void testRegisterCustomerDuplicateMobile() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer existing = buildValidCustomer("DUP_MOBILE_EXISTING");
        service.registerCustomer(existing);

        Customer duplicate = buildValidCustomer("DUP_MOBILE_NEW");
        duplicate.setMobileNumber(existing.getMobileNumber());

        RuntimeException thrown = expectRuntimeException(() -> service.registerCustomer(duplicate));
        boolean mentionsMobile = thrown != null && thrown.getMessage() != null
                && thrown.getMessage().toLowerCase().contains("mobile");

        check("Test 4 - registerCustomer() duplicate mobile number rejected", thrown != null && mentionsMobile);
    }

    // Test 5
    private static void testRegisterCustomerRequiredFieldValidation() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer invalid = buildValidCustomer("REQUIRED_FIELD");
        invalid.setFirstName(""); // blank required field, per CustomerServiceImpl.requireNonBlank

        RuntimeException thrown = expectRuntimeException(() -> service.registerCustomer(invalid));

        check("Test 5 - registerCustomer() rejects blank required field (firstName)", thrown != null);
    }

    // Test 6
    private static void testGetCustomerProfile() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("PROFILE");
        service.registerCustomer(customer);

        Customer fetched = service.getCustomerProfile(customer.getCustomerId());
        boolean correctCustomerReturned = fetched != null
                && customer.getCustomerId().equals(fetched.getCustomerId())
                && customer.getUsername().equals(fetched.getUsername());

        RuntimeException thrown = expectRuntimeException(() -> service.getCustomerProfile(999_999L));

        check("Test 6 - getCustomerProfile() existing customer returned / missing customer throws",
                correctCustomerReturned && thrown != null);
    }

    // Test 7
    private static void testIsUsernameAvailable() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("USERNAME_AVAIL");
        service.registerCustomer(customer);

        boolean existingIsUnavailable = !service.isUsernameAvailable(customer.getUsername());
        boolean newIsAvailable = service.isUsernameAvailable("never-registered-username");

        check("Test 7 - isUsernameAvailable() existing=false, new=true", existingIsUnavailable && newIsAvailable);
    }

    // Test 8
    private static void testIsEmailAvailable() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("EMAIL_AVAIL");
        service.registerCustomer(customer);

        boolean existingIsUnavailable = !service.isEmailAvailable(customer.getEmail());
        boolean newIsAvailable = service.isEmailAvailable("never-registered@example.com");

        check("Test 8 - isEmailAvailable() existing=false, new=true", existingIsUnavailable && newIsAvailable);
    }

    // Test 9
    private static void testIsMobileNumberAvailable() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("MOBILE_AVAIL");
        service.registerCustomer(customer);

        boolean existingIsUnavailable = !service.isMobileNumberAvailable(customer.getMobileNumber());
        boolean newIsAvailable = service.isMobileNumberAvailable("9999999999");

        check("Test 9 - isMobileNumberAvailable() existing=false, new=true",
                existingIsUnavailable && newIsAvailable);
    }

    // Test 10
    private static void testUpdateCustomerProfileSuccess() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("UPDATE_OK");
        service.registerCustomer(customer);

        Customer updateRequest = cloneForUpdate(customer);
        updateRequest.setFirstName("UpdatedFirstName");
        updateRequest.setCity("UpdatedCity");

        service.updateCustomerProfile(updateRequest);

        Customer persisted = dao.findById(customer.getCustomerId());
        boolean firstNameUpdated = "UpdatedFirstName".equals(persisted.getFirstName());
        boolean cityUpdated = "UpdatedCity".equals(persisted.getCity());

        check("Test 10 - updateCustomerProfile() successful update persists editable fields",
                firstNameUpdated && cityUpdated);
    }

    // Test 11
    private static void testUpdateCustomerProfileDuplicateEmail() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customerA = buildValidCustomer("UPDATE_DUP_EMAIL_A");
        Customer customerB = buildValidCustomer("UPDATE_DUP_EMAIL_B");
        service.registerCustomer(customerA);
        service.registerCustomer(customerB);

        Customer updateRequest = cloneForUpdate(customerB);
        updateRequest.setEmail(customerA.getEmail());

        RuntimeException thrown = expectRuntimeException(() -> service.updateCustomerProfile(updateRequest));

        check("Test 11 - updateCustomerProfile() duplicate email rejected", thrown != null);
    }

    // Test 12
    private static void testUpdateCustomerProfileDuplicateMobile() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customerA = buildValidCustomer("UPDATE_DUP_MOBILE_A");
        Customer customerB = buildValidCustomer("UPDATE_DUP_MOBILE_B");
        service.registerCustomer(customerA);
        service.registerCustomer(customerB);

        Customer updateRequest = cloneForUpdate(customerB);
        updateRequest.setMobileNumber(customerA.getMobileNumber());

        RuntimeException thrown = expectRuntimeException(() -> service.updateCustomerProfile(updateRequest));

        check("Test 12 - updateCustomerProfile() duplicate mobile number rejected", thrown != null);
    }

    // Test 13
    private static void testUpdateCustomerProfileDuplicateUsername() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customerA = buildValidCustomer("UPDATE_DUP_USERNAME_A");
        Customer customerB = buildValidCustomer("UPDATE_DUP_USERNAME_B");
        service.registerCustomer(customerA);
        service.registerCustomer(customerB);

        Customer updateRequest = cloneForUpdate(customerB);
        updateRequest.setUsername(customerA.getUsername());

        RuntimeException thrown = expectRuntimeException(() -> service.updateCustomerProfile(updateRequest));

        check("Test 13 - updateCustomerProfile() duplicate username rejected", thrown != null);
    }

    // Test 14
    private static void testUpdateCustomerProfileProtectedFields() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("PROTECTED_FIELDS");
        customer.setPasswordHash("originalHash");
        customer.setAccountStatus("ACTIVE");
        LocalDateTime originalRegistrationDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        customer.setRegistrationDate(originalRegistrationDate);
        service.registerCustomer(customer);

        Customer updateRequest = cloneForUpdate(customer);
        updateRequest.setFirstName("ChangedFirstName"); // legitimate editable change
        updateRequest.setPasswordHash("changedHash");
        updateRequest.setAccountStatus("INACTIVE");
        updateRequest.setRegistrationDate(LocalDateTime.of(2025, 6, 6, 6, 6));

        service.updateCustomerProfile(updateRequest);

        Customer persisted = dao.findById(customer.getCustomerId());
        boolean passwordHashProtected = "originalHash".equals(persisted.getPasswordHash());
        boolean accountStatusProtected = "ACTIVE".equals(persisted.getAccountStatus());
        boolean registrationDateProtected = originalRegistrationDate.equals(persisted.getRegistrationDate());
        boolean editableFieldStillApplied = "ChangedFirstName".equals(persisted.getFirstName());

        check("Test 14 - updateCustomerProfile() protects passwordHash/accountStatus/registrationDate "
                        + "while still applying editable changes",
                passwordHashProtected && accountStatusProtected && registrationDateProtected
                        && editableFieldStillApplied);
    }

    // Tests 15 & 16 - share the same customer to exercise the deactivate -> reactivate lifecycle
    private static void testDeactivateThenReactivateCustomer() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        Customer customer = buildValidCustomer("LIFECYCLE");
        customer.setAccountStatus("ACTIVE");
        service.registerCustomer(customer);

        service.deactivateCustomer(customer.getCustomerId());
        Customer afterDeactivate = dao.findById(customer.getCustomerId());
        boolean deactivatedCorrectly = afterDeactivate != null
                && "INACTIVE".equals(afterDeactivate.getAccountStatus())
                && !dao.wasDeleteByIdCalled();

        check("Test 15 - deactivateCustomer() sets INACTIVE, record kept, deleteById() not called",
                deactivatedCorrectly);

        service.reactivateCustomer(customer.getCustomerId());
        Customer afterReactivate = dao.findById(customer.getCustomerId());
        boolean reactivatedCorrectly = afterReactivate != null
                && "ACTIVE".equals(afterReactivate.getAccountStatus());

        check("Test 16 - reactivateCustomer() sets ACTIVE, record kept", reactivatedCorrectly);
    }

    // Test 17
    private static void testDeactivateMissingCustomer() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        RuntimeException thrown = expectRuntimeException(() -> service.deactivateCustomer(999_999L));

        check("Test 17 - deactivateCustomer() missing customer throws RuntimeException", thrown != null);
    }

    // Test 18
    private static void testReactivateMissingCustomer() {
        FakeCustomerDAO dao = new FakeCustomerDAO();
        CustomerService service = new CustomerServiceImpl(dao);

        RuntimeException thrown = expectRuntimeException(() -> service.reactivateCustomer(999_999L));

        check("Test 18 - reactivateCustomer() missing customer throws RuntimeException", thrown != null);
    }

    // ---- test helpers ----

    private static void check(String testName, boolean condition) {
        testsExecuted++;
        if (condition) {
            testsPassed++;
            System.out.println(testName + ": PASS");
        } else {
            System.out.println(testName + ": FAIL");
        }
    }

    private interface ThrowingAction {
        void run();
    }

    private static RuntimeException expectRuntimeException(ThrowingAction action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static Customer buildValidCustomer(String label) {
        customerCounter++;
        String unique = label + "_" + customerCounter;

        Customer customer = new Customer(
                "CUSTNUM-" + unique,
                "First-" + unique,
                "Last-" + unique,
                LocalDate.of(1990, 1, 1),
                "user-" + unique + "@example.com",
                "9" + String.format("%09d", customerCounter),
                "username-" + unique,
                "hash-" + unique);
        customer.setAddress("Address " + unique);
        customer.setCity("City " + unique);
        customer.setCountry("Country " + unique);
        return customer;
    }

    /** Builds an independent Customer object carrying the same values, for use as an update request. */
    private static Customer cloneForUpdate(Customer source) {
        Customer copy = new Customer(
                source.getCustomerNumber(),
                source.getFirstName(),
                source.getLastName(),
                source.getDateOfBirth(),
                source.getEmail(),
                source.getMobileNumber(),
                source.getUsername(),
                source.getPasswordHash());
        copy.setCustomerId(source.getCustomerId());
        copy.setAddress(source.getAddress());
        copy.setCity(source.getCity());
        copy.setCountry(source.getCountry());
        copy.setAccountStatus(source.getAccountStatus());
        copy.setRegistrationDate(source.getRegistrationDate());
        return copy;
    }

    /**
     * In-memory fake CustomerDAO for this test only. Not a production class and not used anywhere
     * outside CustomerServiceTest.
     */
    private static class FakeCustomerDAO implements CustomerDAO {

        private final Map<Long, Customer> customersById = new HashMap<>();
        private long nextId = 1L;
        private boolean deleteByIdCalled = false;

        @Override
        public Customer findById(Long customerId) {
            return customersById.get(customerId);
        }

        @Override
        public Customer findByUsername(String username) {
            for (Customer customer : customersById.values()) {
                if (customer.getUsername().equals(username)) {
                    return customer;
                }
            }
            return null;
        }

        @Override
        public Customer findByEmail(String email) {
            for (Customer customer : customersById.values()) {
                if (customer.getEmail().equals(email)) {
                    return customer;
                }
            }
            return null;
        }

        @Override
        public boolean existsByMobileNumber(String mobileNumber) {
            for (Customer customer : customersById.values()) {
                if (customer.getMobileNumber().equals(mobileNumber)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void save(Customer customer) {
            customer.setCustomerId(nextId++);
            customersById.put(customer.getCustomerId(), customer);
        }

        @Override
        public void update(Customer customer) {
            customersById.put(customer.getCustomerId(), customer);
        }

        @Override
        public void deleteById(Long customerId) {
            deleteByIdCalled = true;
            customersById.remove(customerId);
        }

        boolean wasDeleteByIdCalled() {
            return deleteByIdCalled;
        }
    }
}
