package com.amdocs.telecom.service;

import com.amdocs.telecom.model.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Business-level contract for payment processing.
 *
 * processPayment() is the one case-study-mandated atomic transaction (validate bill, validate
 * amount, create payment, update bill status, create audit record, commit/rollback as a unit).
 * PaymentDAO/BillingDAO/AuditLogDAO each open and commit their own JDBC connection independently,
 * so they cannot be composed into one transaction as-is; PaymentServiceImpl performs that one
 * operation with its own JDBC transaction instead of delegating it piecemeal to those DAOs. See
 * PaymentServiceImpl's class Javadoc for the full explanation.
 *
 * Note: the schema has no partial-payment/balance column and bill_status has no "PARTIALLY_PAID"
 * value, so a payment is assumed to settle a bill in full - the amount must exactly match the
 * bill's totalAmount.
 */
public interface PaymentService {

    Payment processPayment(Long billId, Long customerId, BigDecimal amount, String paymentMode,
                            String transactionReference);

    Payment getPaymentById(Long paymentId);

    Optional<Payment> findPaymentByTransactionReference(String transactionReference);

    List<Payment> getBillPayments(Long billId);

    List<Payment> getCustomerPayments(Long customerId);
}
