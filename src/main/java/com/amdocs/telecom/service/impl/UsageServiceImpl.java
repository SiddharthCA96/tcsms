package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.UsageDAO;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.UsageRecord;
import com.amdocs.telecom.service.UsageService;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic implementation of UsageService.
 *
 * Supported usage types are fixed to VOICE/SMS/DATA/ROAMING, matching the case study and the
 * values already present in seed_data.sql; the schema places no CHECK constraint on usage_type,
 * so this set is enforced in the service rather than the database.
 */
public class UsageServiceImpl implements UsageService {

    private static final Set<String> SUPPORTED_USAGE_TYPES = new HashSet<>(
            java.util.Arrays.asList("VOICE", "SMS", "DATA", "ROAMING"));

    private final UsageDAO usageDAO;
    private final MobileSubscriptionDAO subscriptionDAO;

    public UsageServiceImpl(UsageDAO usageDAO, MobileSubscriptionDAO subscriptionDAO) {
        this.usageDAO = usageDAO;
        this.subscriptionDAO = subscriptionDAO;
    }

    @Override
    public UsageRecord recordUsage(UsageRecord usageRecord) {
        if (usageRecord == null) {
            throw new RuntimeException("Failed to record usage: usageRecord must not be null.");
        }
        if (usageRecord.getSubscriptionId() == null) {
            throw new RuntimeException("Failed to record usage: subscriptionId must not be null.");
        }
        if (usageRecord.getUsageDate() == null) {
            throw new RuntimeException("Failed to record usage: usageDate must not be null.");
        }
        validateUsageType(usageRecord.getUsageType());
        if (usageRecord.getQuantity() == null) {
            throw new RuntimeException("Failed to record usage: quantity must not be null.");
        }
        if (usageRecord.getUnit() == null || usageRecord.getUnit().trim().isEmpty()) {
            throw new RuntimeException("Failed to record usage: unit must not be null or empty.");
        }

        usageDAO.save(usageRecord);
        return usageRecord;
    }

    @Override
    public List<UsageRecord> getSubscriptionUsage(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new RuntimeException("Failed to get subscription usage: subscriptionId must not be null.");
        }
        return usageDAO.findBySubscriptionId(subscriptionId);
    }

    @Override
    public List<UsageRecord> getSubscriptionUsageByType(Long subscriptionId, String usageType) {
        if (subscriptionId == null) {
            throw new RuntimeException("Failed to get subscription usage: subscriptionId must not be null.");
        }
        validateUsageType(usageType);
        return usageDAO.findBySubscriptionIdAndUsageType(subscriptionId, usageType);
    }

    @Override
    public Map<String, BigDecimal> calculateTotalUsageByType(Long subscriptionId) {
        List<UsageRecord> records = getSubscriptionUsage(subscriptionId);

        return records.stream()
                .collect(Collectors.groupingBy(
                        UsageRecord::getUsageType,
                        Collectors.reducing(BigDecimal.ZERO, UsageRecord::getQuantity, BigDecimal::add)));
    }

    @Override
    public Map<String, BigDecimal> calculateMonthlyUsageByType(Long subscriptionId, int year, int month) {
        List<UsageRecord> records = getSubscriptionUsage(subscriptionId);

        return records.stream()
                .filter(record -> record.getUsageDate().getYear() == year
                        && record.getUsageDate().getMonthValue() == month)
                .collect(Collectors.groupingBy(
                        UsageRecord::getUsageType,
                        Collectors.reducing(BigDecimal.ZERO, UsageRecord::getQuantity, BigDecimal::add)));
    }

    @Override
    public DoubleSummaryStatistics summarizeUsageQuantity(Long subscriptionId, String usageType) {
        List<UsageRecord> records = getSubscriptionUsageByType(subscriptionId, usageType);

        return records.stream()
                .collect(Collectors.summarizingDouble(record -> record.getQuantity().doubleValue()));
    }

    @Override
    public Optional<UsageRecord> findMostRecentUsage(Long subscriptionId, String usageType) {
        List<UsageRecord> records = getSubscriptionUsageByType(subscriptionId, usageType);

        return records.stream()
                .max(Comparator.comparing(UsageRecord::getUsageDate));
    }

    @Override
    public List<Map.Entry<Long, BigDecimal>> getTopUsageCustomers(List<Long> customerIds, String usageType,
                                                                    int topN) {
        if (customerIds == null || customerIds.isEmpty()) {
            throw new RuntimeException("Failed to rank customers by usage: customerIds must not be empty.");
        }
        validateUsageType(usageType);
        if (topN <= 0) {
            throw new RuntimeException("Failed to rank customers by usage: topN must be positive.");
        }

        return customerIds.stream()
                .map(customerId -> new AbstractMap.SimpleEntry<>(customerId, totalUsageForCustomer(customerId, usageType)))
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    private BigDecimal totalUsageForCustomer(Long customerId, String usageType) {
        List<MobileSubscription> subscriptions = subscriptionDAO.findByCustomerId(customerId);

        return subscriptions.stream()
                .flatMap(subscription ->
                        usageDAO.findBySubscriptionIdAndUsageType(subscription.getSubscriptionId(), usageType)
                                .stream())
                .map(UsageRecord::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateUsageType(String usageType) {
        if (usageType == null || !SUPPORTED_USAGE_TYPES.contains(usageType)) {
            throw new RuntimeException("'" + usageType + "' is not a supported usage type "
                    + "(expected one of " + SUPPORTED_USAGE_TYPES + ").");
        }
    }
}
