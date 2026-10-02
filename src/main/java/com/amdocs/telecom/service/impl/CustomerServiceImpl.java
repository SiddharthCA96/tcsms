package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.CustomerDAO;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.service.CustomerService;

/**
 * Business logic implementation of CustomerService.
 *
 * Only 'ACTIVE' appears in the seeded account_status data and the schema places no CHECK
 * constraint on the column, so 'INACTIVE' is used here as the symmetric counterpart, following
 * the same uppercase-single-word convention used elsewhere in the schema (bill_status,
 * complaint status, notification status, etc.).
 */
public class CustomerServiceImpl implements CustomerService {

    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";
    private static final String ACCOUNT_STATUS_INACTIVE = "INACTIVE";

    private final CustomerDAO customerDAO;

    public CustomerServiceImpl(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Override
    public Customer registerCustomer(Customer customer) {
        if (customer == null) {
            throw new RuntimeException("Failed to register customer: customer must not be null.");
        }
        validateRequiredFieldsForRegistration(customer);

        if (customerDAO.findByUsername(customer.getUsername()) != null) {
            throw new RuntimeException("Failed to register customer: username '"
                    + customer.getUsername() + "' is already in use.");
        }
        if (customerDAO.findByEmail(customer.getEmail()) != null) {
            throw new RuntimeException("Failed to register customer: email '"
                    + customer.getEmail() + "' is already in use.");
        }
        if (customerDAO.existsByMobileNumber(customer.getMobileNumber())) {
            throw new RuntimeException("Failed to register customer: mobile number '"
                    + customer.getMobileNumber() + "' is already in use.");
        }

        // registrationDate is a database default (CURRENT_TIMESTAMP) that CustomerDAOImpl does not
        // accept on insert, so it cannot be set from here - only accountStatus has a business
        // default to apply, and only when the caller hasn't already supplied one.
        if (customer.getAccountStatus() == null || customer.getAccountStatus().trim().isEmpty()) {
            customer.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        }

        customerDAO.save(customer);
        return customer;
    }

    @Override
    public Customer getCustomerProfile(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Failed to get customer profile: customerId must not be null.");
        }

        Customer customer = customerDAO.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("Failed to get customer profile: no customer found with ID: " + customerId);
        }
        return customer;
    }

    @Override
    public void updateCustomerProfile(Customer customer) {
        if (customer == null || customer.getCustomerId() == null) {
            throw new RuntimeException("Failed to update customer profile: customer and customerId must not be null.");
        }

        Customer existingCustomer = customerDAO.findById(customer.getCustomerId());
        if (existingCustomer == null) {
            throw new RuntimeException("Failed to update customer profile: no customer found with ID: "
                    + customer.getCustomerId());
        }

        validateRequiredFieldsForProfileUpdate(customer);

        if (!existingCustomer.getEmail().equals(customer.getEmail())) {
            Customer customerWithEmail = customerDAO.findByEmail(customer.getEmail());
            if (customerWithEmail != null && !customerWithEmail.getCustomerId().equals(customer.getCustomerId())) {
                throw new RuntimeException("Failed to update customer profile: email '"
                        + customer.getEmail() + "' is already in use by another customer.");
            }
        }

        if (!existingCustomer.getMobileNumber().equals(customer.getMobileNumber())
                && customerDAO.existsByMobileNumber(customer.getMobileNumber())) {
            throw new RuntimeException("Failed to update customer profile: mobile number '"
                    + customer.getMobileNumber() + "' is already in use by another customer.");
        }

        if (!existingCustomer.getUsername().equals(customer.getUsername())) {
            Customer customerWithUsername = customerDAO.findByUsername(customer.getUsername());
            if (customerWithUsername != null
                    && !customerWithUsername.getCustomerId().equals(customer.getCustomerId())) {
                throw new RuntimeException("Failed to update customer profile: username '"
                        + customer.getUsername() + "' is already in use by another customer.");
            }
        }

        // Protect fields that a profile update must never change.
        customer.setPasswordHash(existingCustomer.getPasswordHash());
        customer.setAccountStatus(existingCustomer.getAccountStatus());
        customer.setRegistrationDate(existingCustomer.getRegistrationDate());

        customerDAO.update(customer);
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Failed to check username availability: username must not be null or empty.");
        }
        return customerDAO.findByUsername(username) == null;
    }

    @Override
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Failed to check email availability: email must not be null or empty.");
        }
        return customerDAO.findByEmail(email) == null;
    }

    @Override
    public boolean isMobileNumberAvailable(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new RuntimeException(
                    "Failed to check mobile number availability: mobile number must not be null or empty.");
        }
        return !customerDAO.existsByMobileNumber(mobileNumber);
    }

    @Override
    public void deactivateCustomer(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Failed to deactivate customer: customerId must not be null.");
        }

        Customer customer = customerDAO.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("Failed to deactivate customer: no customer found with ID: " + customerId);
        }

        customer.setAccountStatus(ACCOUNT_STATUS_INACTIVE);
        customerDAO.update(customer);
    }

    @Override
    public void reactivateCustomer(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Failed to reactivate customer: customerId must not be null.");
        }

        Customer customer = customerDAO.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("Failed to reactivate customer: no customer found with ID: " + customerId);
        }

        customer.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        customerDAO.update(customer);
    }

    private void validateRequiredFieldsForRegistration(Customer customer) {
        requireNonBlank(customer.getCustomerNumber(), "customerNumber");
        requireNonBlank(customer.getFirstName(), "firstName");
        requireNonBlank(customer.getLastName(), "lastName");
        requireDateOfBirth(customer);
        requireNonBlank(customer.getEmail(), "email");
        requireNonBlank(customer.getMobileNumber(), "mobileNumber");
        requireNonBlank(customer.getUsername(), "username");
        requireNonBlank(customer.getPasswordHash(), "passwordHash");
    }

    private void validateRequiredFieldsForProfileUpdate(Customer customer) {
        requireNonBlank(customer.getFirstName(), "firstName");
        requireNonBlank(customer.getLastName(), "lastName");
        requireDateOfBirth(customer);
        requireNonBlank(customer.getEmail(), "email");
        requireNonBlank(customer.getMobileNumber(), "mobileNumber");
        requireNonBlank(customer.getUsername(), "username");
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Customer " + fieldName + " must not be null or empty.");
        }
    }

    private void requireDateOfBirth(Customer customer) {
        if (customer.getDateOfBirth() == null) {
            throw new RuntimeException("Customer dateOfBirth must not be null.");
        }
    }
}
