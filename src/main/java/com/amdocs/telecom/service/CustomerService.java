package com.amdocs.telecom.service;

import com.amdocs.telecom.model.Customer;

/**
 * Business-level contract for customer management.
 * Authentication, password hashing, login-attempt tracking, CAPTCHA and OTP recovery are
 * handled by dedicated security components, not here.
 */
public interface CustomerService {

    /**
     * Registers a new customer after verifying username/email/mobile number uniqueness and
     * applying registration defaults (e.g. initial account status).
     */
    Customer registerCustomer(Customer customer);

    /**
     * Retrieves a customer's profile for display.
     */
    Customer getCustomerProfile(Long customerId);

    /**
     * Updates a customer's own profile details (name, contact info, address).
     * Does not change passwordHash or accountStatus - those have their own dedicated flows.
     */
    void updateCustomerProfile(Customer customer);

    boolean isUsernameAvailable(String username);

    boolean isEmailAvailable(String email);

    boolean isMobileNumberAvailable(String mobileNumber);

    /**
     * Deactivates a customer's account (account_status transition), rather than deleting the
     * record - customers have subscriptions, bills, payments, complaints and notifications tied
     * to them by foreign key, so a hard delete is not the business-appropriate operation here.
     */
    void deactivateCustomer(Long customerId);

    /**
     * Reactivates a previously deactivated customer's account.
     */
    void reactivateCustomer(Long customerId);
}
