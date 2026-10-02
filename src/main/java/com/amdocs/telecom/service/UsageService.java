package com.amdocs.telecom.service;

import com.amdocs.telecom.model.UsageRecord;

import java.math.BigDecimal;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business-level contract for usage recording and reporting.
 *
 * Note on "total usage": VOICE (minutes), SMS (count), DATA (GB) and ROAMING (GB) use different
 * units, so a single cross-type sum would not be a meaningful number. Totals are therefore always
 * computed per usage type (a Map keyed by usageType), never summed across types.
 */
public interface UsageService {

    UsageRecord recordUsage(UsageRecord usageRecord);

    List<UsageRecord> getSubscriptionUsage(Long subscriptionId);

    List<UsageRecord> getSubscriptionUsageByType(Long subscriptionId, String usageType);

    /**
     * Total quantity per usage type (VOICE/SMS/DATA/ROAMING) for a subscription.
     */
    Map<String, BigDecimal> calculateTotalUsageByType(Long subscriptionId);

    /**
     * Total quantity per usage type for a subscription, restricted to the given calendar month.
     */
    Map<String, BigDecimal> calculateMonthlyUsageByType(Long subscriptionId, int year, int month);

    /**
     * Descriptive statistics (count/min/max/average/sum) of quantity for a subscription's usage
     * of one type. Quantity is a measurement, not a monetary amount, so double precision is
     * acceptable here.
     */
    DoubleSummaryStatistics summarizeUsageQuantity(Long subscriptionId, String usageType);

    Optional<UsageRecord> findMostRecentUsage(Long subscriptionId, String usageType);

    /**
     * Ranks the given customers by their total usage of one type (highest first), returning at
     * most topN entries of customerId -> total quantity.
     */
    List<Map.Entry<Long, BigDecimal>> getTopUsageCustomers(List<Long> customerIds, String usageType, int topN);
}
