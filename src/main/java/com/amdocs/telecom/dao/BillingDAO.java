package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Bill;

import java.time.LocalDate;
import java.util.List;

public interface BillingDAO {

    Bill findById(Long billId);

    Bill findByBillNumber(String billNumber);

    Bill findBySubscriptionIdAndBillingMonth(Long subscriptionId, LocalDate billingMonth);

    List<Bill> findBySubscriptionId(Long subscriptionId);

    List<Bill> findByStatus(String billStatus);

    void save(Bill bill);

    void update(Bill bill);
}
