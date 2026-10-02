package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Customer;

public interface CustomerDAO {

    Customer findById(Long customerId);

    Customer findByUsername(String username);

    Customer findByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    void save(Customer customer);

    void update(Customer customer);

    void deleteById(Long customerId);
}
