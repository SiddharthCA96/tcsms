package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Payment;

import java.util.List;

public interface PaymentDAO {

    Payment findById(Long paymentId);

    Payment findByTransactionReference(String transactionReference);

    List<Payment> findByBillId(Long billId);

    List<Payment> findByCustomerId(Long customerId);

    boolean existsByTransactionReference(String transactionReference);

    void save(Payment payment);
}
