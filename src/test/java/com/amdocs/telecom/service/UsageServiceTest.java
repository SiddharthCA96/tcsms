package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.UsageDAO;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.UsageRecord;
import com.amdocs.telecom.service.impl.UsageServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-Java unit test for UsageServiceImpl business logic, independent of JDBC/MySQL, using
 * in-memory fake DAOs defined below (private nested classes, test-only).
 */
public class UsageServiceTest {

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    private static int counter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("UsageServiceTest");
        System.out.println("========================================");

        testRecordUsageSuccess();
        testRecordUsageInvalidTypeRejected();
        testRecordUsageMissingFieldRejected();
        testGetSubscriptionUsage();
        testGetSubscriptionUsageByType();
        testCalculateTotalUsageByType();
        testCalculateMonthlyUsageByType();
        testSummarizeUsageQuantity();
        testFindMostRecentUsage();
        testGetTopUsageCustomers();

        System.out.println("========================================");
        System.out.println("Tests executed: " + testsExecuted);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsExecuted - testsPassed));
        System.out.println("========================================");
    }

    // Test 1
    private static void testRecordUsageSuccess() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        UsageRecord record = buildUsage(1L, "DATA", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0));
        UsageRecord saved = service.recordUsage(record);

        boolean idAssigned = saved.getUsageId() != null;
        boolean storedInDao = idAssigned && usageDAO.findById(saved.getUsageId()) != null;

        check("Test 1 - recordUsage() successful recording", idAssigned && storedInDao);
    }

    // Test 2
    private static void testRecordUsageInvalidTypeRejected() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        UsageRecord record = buildUsage(1L, "UNKNOWN_TYPE", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0));
        RuntimeException thrown = expectRuntimeException(() -> service.recordUsage(record));

        check("Test 2 - recordUsage() rejects an unsupported usage type", thrown != null);
    }

    // Test 3
    private static void testRecordUsageMissingFieldRejected() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        UsageRecord record = buildUsage(1L, "DATA", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0));
        record.setUnit(null);
        RuntimeException thrown = expectRuntimeException(() -> service.recordUsage(record));

        check("Test 3 - recordUsage() rejects a missing required field (unit)", thrown != null);
    }

    // Test 4
    private static void testGetSubscriptionUsage() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        service.recordUsage(buildUsage(1L, "DATA", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(1L, "VOICE", "120.000", LocalDateTime.of(2026, 8, 6, 10, 0)));

        List<UsageRecord> records = service.getSubscriptionUsage(1L);

        check("Test 4 - getSubscriptionUsage() returns all records for the subscription",
                records.size() == 2);
    }

    // Test 5
    private static void testGetSubscriptionUsageByType() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        service.recordUsage(buildUsage(1L, "DATA", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(1L, "VOICE", "120.000", LocalDateTime.of(2026, 8, 6, 10, 0)));

        List<UsageRecord> dataRecords = service.getSubscriptionUsageByType(1L, "DATA");

        check("Test 5 - getSubscriptionUsageByType() filters to the requested type only",
                dataRecords.size() == 1 && "DATA".equals(dataRecords.get(0).getUsageType()));
    }

    // Test 6
    private static void testCalculateTotalUsageByType() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        service.recordUsage(buildUsage(1L, "DATA", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(1L, "DATA", "1.500", LocalDateTime.of(2026, 8, 10, 10, 0)));
        service.recordUsage(buildUsage(1L, "VOICE", "120.000", LocalDateTime.of(2026, 8, 6, 10, 0)));

        Map<String, BigDecimal> totals = service.calculateTotalUsageByType(1L);

        boolean dataTotalCorrect = new BigDecimal("4.000").compareTo(totals.get("DATA")) == 0;
        boolean voiceTotalCorrect = new BigDecimal("120.000").compareTo(totals.get("VOICE")) == 0;

        check("Test 6 - calculateTotalUsageByType() sums quantity per type correctly",
                dataTotalCorrect && voiceTotalCorrect);
    }

    // Test 7
    private static void testCalculateMonthlyUsageByType() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        service.recordUsage(buildUsage(1L, "DATA", "2.500", LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(1L, "DATA", "9.000", LocalDateTime.of(2026, 9, 5, 10, 0)));

        Map<String, BigDecimal> augustTotals = service.calculateMonthlyUsageByType(1L, 2026, 8);

        check("Test 7 - calculateMonthlyUsageByType() restricts totals to the given month",
                augustTotals.size() == 1 && new BigDecimal("2.500").compareTo(augustTotals.get("DATA")) == 0);
    }

    // Test 8
    private static void testSummarizeUsageQuantity() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        service.recordUsage(buildUsage(1L, "DATA", "2.000", LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(1L, "DATA", "8.000", LocalDateTime.of(2026, 8, 10, 10, 0)));

        DoubleSummaryStatistics stats = service.summarizeUsageQuantity(1L, "DATA");

        check("Test 8 - summarizeUsageQuantity() computes correct count/sum/average",
                stats.getCount() == 2 && stats.getSum() == 10.0 && stats.getAverage() == 5.0);
    }

    // Test 9
    private static void testFindMostRecentUsage() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        service.recordUsage(buildUsage(1L, "DATA", "2.000", LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(1L, "DATA", "8.000", LocalDateTime.of(2026, 8, 20, 10, 0)));

        Optional<UsageRecord> mostRecent = service.findMostRecentUsage(1L, "DATA");
        Optional<UsageRecord> noneForVoice = service.findMostRecentUsage(1L, "VOICE");

        check("Test 9 - findMostRecentUsage() returns the latest record / empty when none exist",
                mostRecent.isPresent()
                        && new BigDecimal("8.000").compareTo(mostRecent.get().getQuantity()) == 0
                        && !noneForVoice.isPresent());
    }

    // Test 10
    private static void testGetTopUsageCustomers() {
        FakeUsageDAO usageDAO = new FakeUsageDAO();
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        UsageService service = new UsageServiceImpl(usageDAO, subscriptionDAO);

        MobileSubscription subForCustomer1 = buildSubscription(1L);
        MobileSubscription subForCustomer2 = buildSubscription(2L);
        subscriptionDAO.save(subForCustomer1);
        subscriptionDAO.save(subForCustomer2);

        service.recordUsage(buildUsage(subForCustomer1.getSubscriptionId(), "DATA", "5.000",
                LocalDateTime.of(2026, 8, 5, 10, 0)));
        service.recordUsage(buildUsage(subForCustomer2.getSubscriptionId(), "DATA", "20.000",
                LocalDateTime.of(2026, 8, 5, 10, 0)));

        List<Map.Entry<Long, BigDecimal>> ranked = service.getTopUsageCustomers(Arrays.asList(1L, 2L), "DATA", 2);

        boolean highestFirst = ranked.size() == 2
                && ranked.get(0).getKey().equals(2L)
                && new BigDecimal("20.000").compareTo(ranked.get(0).getValue()) == 0
                && ranked.get(1).getKey().equals(1L);

        check("Test 10 - getTopUsageCustomers() ranks customers by total usage, highest first",
                highestFirst);
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

    private static UsageRecord buildUsage(Long subscriptionId, String usageType, String quantity,
                                           LocalDateTime usageDate) {
        String unit;
        switch (usageType) {
            case "DATA":
            case "ROAMING":
                unit = "GB";
                break;
            case "VOICE":
                unit = "MINUTES";
                break;
            default:
                unit = "SMS";
        }
        return new UsageRecord(subscriptionId, usageDate, usageType, new BigDecimal(quantity), unit);
    }

    private static MobileSubscription buildSubscription(Long customerId) {
        counter++;
        return new MobileSubscription(
                "SUBNUM-USAGE_" + counter,
                customerId,
                "9" + String.format("%09d", counter),
                (long) counter,
                1L,
                LocalDate.of(2026, 1, 1),
                "POSTPAID",
                "ACTIVE");
    }

    /** In-memory fake UsageDAO for this test only. Not a production class. */
    private static class FakeUsageDAO implements UsageDAO {

        private final Map<Long, UsageRecord> recordsById = new HashMap<>();
        private long nextId = 1L;

        @Override
        public UsageRecord findById(Long usageId) {
            return recordsById.get(usageId);
        }

        @Override
        public List<UsageRecord> findBySubscriptionId(Long subscriptionId) {
            List<UsageRecord> result = new ArrayList<>();
            for (UsageRecord record : recordsById.values()) {
                if (record.getSubscriptionId().equals(subscriptionId)) {
                    result.add(record);
                }
            }
            return result;
        }

        @Override
        public List<UsageRecord> findBySubscriptionIdAndUsageType(Long subscriptionId, String usageType) {
            List<UsageRecord> result = new ArrayList<>();
            for (UsageRecord record : recordsById.values()) {
                if (record.getSubscriptionId().equals(subscriptionId) && record.getUsageType().equals(usageType)) {
                    result.add(record);
                }
            }
            return result;
        }

        @Override
        public void save(UsageRecord usageRecord) {
            usageRecord.setUsageId(nextId++);
            recordsById.put(usageRecord.getUsageId(), usageRecord);
        }
    }

    /** In-memory fake MobileSubscriptionDAO for this test only. Not a production class. */
    private static class FakeMobileSubscriptionDAO implements MobileSubscriptionDAO {

        private final Map<Long, MobileSubscription> subscriptionsById = new HashMap<>();
        private long nextId = 1L;

        @Override
        public MobileSubscription findById(Long subscriptionId) {
            return subscriptionsById.get(subscriptionId);
        }

        @Override
        public MobileSubscription findBySubscriptionNumber(String subscriptionNumber) {
            for (MobileSubscription s : subscriptionsById.values()) {
                if (s.getSubscriptionNumber().equals(subscriptionNumber)) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public List<MobileSubscription> findByCustomerId(Long customerId) {
            List<MobileSubscription> result = new ArrayList<>();
            for (MobileSubscription s : subscriptionsById.values()) {
                if (s.getCustomerId().equals(customerId)) {
                    result.add(s);
                }
            }
            return result;
        }

        @Override
        public MobileSubscription findByMobileNumber(String mobileNumber) {
            for (MobileSubscription s : subscriptionsById.values()) {
                if (s.getMobileNumber().equals(mobileNumber)) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public boolean existsByMobileNumber(String mobileNumber) {
            return findByMobileNumber(mobileNumber) != null;
        }

        @Override
        public boolean existsByCustomerAndPlan(Long customerId, Long planId) {
            for (MobileSubscription s : subscriptionsById.values()) {
                if (s.getCustomerId().equals(customerId) && s.getPlanId().equals(planId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void save(MobileSubscription subscription) {
            subscription.setSubscriptionId(nextId++);
            subscriptionsById.put(subscription.getSubscriptionId(), subscription);
        }

        @Override
        public void update(MobileSubscription subscription) {
            subscriptionsById.put(subscription.getSubscriptionId(), subscription);
        }

        @Override
        public void deleteById(Long subscriptionId) {
            subscriptionsById.remove(subscriptionId);
        }
    }
}
