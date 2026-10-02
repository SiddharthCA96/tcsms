package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.BillingDAO;
import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.dao.UsageDAO;
import com.amdocs.telecom.model.Bill;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.model.UsageRecord;
import com.amdocs.telecom.service.impl.BillingServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-Java unit test for BillingServiceImpl business logic, independent of JDBC/MySQL, using
 * in-memory fake DAOs defined below (private nested classes, test-only).
 */
public class BillingServiceTest {

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    private static int counter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("BillingServiceTest");
        System.out.println("========================================");

        testCalculatePlanRental();
        testCalculateUsageChargesRestrictedToMonth();
        testGenerateMonthlyBillSuccess();
        testGenerateMonthlyBillDuplicateRejected();
        testGetBillById();
        testFindBillByNumberAndForMonth();
        testGetBillsByStatus();
        testMarkBillOverdue();
        testMarkBillOverdueRejectsAlreadyPaidBill();

        System.out.println("========================================");
        System.out.println("Tests executed: " + testsExecuted);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsExecuted - testsPassed));
        System.out.println("========================================");
    }

    // Test 1
    private static void testCalculatePlanRental() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        BigDecimal rental = fixture.service.calculatePlanRental(subscription.getSubscriptionId());

        check("Test 1 - calculatePlanRental() returns the subscription's plan monthlyRental",
                new BigDecimal("700.00").compareTo(rental) == 0);
    }

    // Test 2
    private static void testCalculateUsageChargesRestrictedToMonth() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        saveUsage(fixture.usageDAO, subscription.getSubscriptionId(), "100.00", LocalDateTime.of(2026, 8, 5, 10, 0));
        saveUsage(fixture.usageDAO, subscription.getSubscriptionId(), "50.00", LocalDateTime.of(2026, 8, 20, 10, 0));
        saveUsage(fixture.usageDAO, subscription.getSubscriptionId(), "9999.00", LocalDateTime.of(2026, 9, 1, 10, 0));

        BigDecimal augustCharges = fixture.service.calculateUsageCharges(subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1));

        check("Test 2 - calculateUsageCharges() sums only charges within the billing month",
                new BigDecimal("150.00").compareTo(augustCharges) == 0);
    }

    // Test 3
    private static void testGenerateMonthlyBillSuccess() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);
        saveUsage(fixture.usageDAO, subscription.getSubscriptionId(), "100.00", LocalDateTime.of(2026, 8, 5, 10, 0));

        Bill bill = fixture.service.generateMonthlyBill("INV-TEST-0001", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), new BigDecimal("30.00"), new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 20));

        boolean idAssigned = bill.getBillId() != null;
        boolean planRentalCorrect = new BigDecimal("700.00").compareTo(bill.getPlanRental()) == 0;
        boolean usageChargesCorrect = new BigDecimal("100.00").compareTo(bill.getUsageCharges()) == 0;
        // total = 700.00 (rental) + 100.00 (usage) + 30.00 (tax) - 10.00 (discount) = 820.00
        boolean totalCorrect = new BigDecimal("820.00").compareTo(bill.getTotalAmount()) == 0;
        boolean statusUnpaid = "UNPAID".equals(bill.getBillStatus());

        check("Test 3 - generateMonthlyBill() computes rental/usage/total correctly with UNPAID status",
                idAssigned && planRentalCorrect && usageChargesCorrect && totalCorrect && statusUnpaid);
    }

    // Test 4
    private static void testGenerateMonthlyBillDuplicateRejected() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        fixture.service.generateMonthlyBill("INV-TEST-DUP1", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20));

        RuntimeException thrown = expectRuntimeException(() ->
                fixture.service.generateMonthlyBill("INV-TEST-DUP2", subscription.getSubscriptionId(),
                        LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20)));

        check("Test 4 - generateMonthlyBill() rejects a duplicate subscription/billing-month bill",
                thrown != null);
    }

    // Test 5
    private static void testGetBillById() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        Bill bill = fixture.service.generateMonthlyBill("INV-TEST-GETBYID", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20));

        Bill found = fixture.service.getBillById(bill.getBillId());
        RuntimeException thrown = expectRuntimeException(() -> fixture.service.getBillById(999_999L));

        check("Test 5 - getBillById() existing bill returned / missing bill throws",
                found != null && bill.getBillId().equals(found.getBillId()) && thrown != null);
    }

    // Test 6
    private static void testFindBillByNumberAndForMonth() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        Bill bill = fixture.service.generateMonthlyBill("INV-TEST-NUMBER", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20));

        Optional<Bill> byNumber = fixture.service.findBillByNumber("INV-TEST-NUMBER");
        Optional<Bill> byMonth = fixture.service.findBillForMonth(subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1));
        Optional<Bill> byUnknownMonth = fixture.service.findBillForMonth(subscription.getSubscriptionId(),
                LocalDate.of(2026, 9, 1));

        check("Test 6 - findBillByNumber()/findBillForMonth() behave correctly",
                byNumber.isPresent() && bill.getBillId().equals(byNumber.get().getBillId())
                        && byMonth.isPresent() && !byUnknownMonth.isPresent());
    }

    // Test 7
    private static void testGetBillsByStatus() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        fixture.service.generateMonthlyBill("INV-TEST-STATUS", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20));

        List<Bill> unpaidBills = fixture.service.getBillsByStatus("UNPAID");

        check("Test 7 - getBillsByStatus() returns bills matching the requested status",
                unpaidBills.size() == 1 && "UNPAID".equals(unpaidBills.get(0).getBillStatus()));
    }

    // Test 8
    private static void testMarkBillOverdue() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        Bill bill = fixture.service.generateMonthlyBill("INV-TEST-OVERDUE", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20));

        fixture.service.markBillOverdue(bill.getBillId());

        Bill updated = fixture.billingDAO.findById(bill.getBillId());
        check("Test 8 - markBillOverdue() transitions status to OVERDUE", "OVERDUE".equals(updated.getBillStatus()));
    }

    // Test 9
    private static void testMarkBillOverdueRejectsAlreadyPaidBill() {
        Fixture fixture = newFixture();
        TelecomPlan plan = buildPlan("700.00");
        fixture.planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(plan.getPlanId());
        fixture.subscriptionDAO.save(subscription);

        Bill bill = fixture.service.generateMonthlyBill("INV-TEST-PAID", subscription.getSubscriptionId(),
                LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 20));
        bill.setBillStatus("PAID");
        fixture.billingDAO.update(bill);

        RuntimeException thrown = expectRuntimeException(() -> fixture.service.markBillOverdue(bill.getBillId()));

        check("Test 9 - markBillOverdue() rejects a bill that is already PAID", thrown != null);
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

    private static class Fixture {
        FakeBillingDAO billingDAO;
        FakeMobileSubscriptionDAO subscriptionDAO;
        FakeTelecomPlanDAO planDAO;
        FakeUsageDAO usageDAO;
        BillingService service;
    }

    private static Fixture newFixture() {
        Fixture fixture = new Fixture();
        fixture.billingDAO = new FakeBillingDAO();
        fixture.subscriptionDAO = new FakeMobileSubscriptionDAO();
        fixture.planDAO = new FakeTelecomPlanDAO();
        fixture.usageDAO = new FakeUsageDAO();
        fixture.service = new BillingServiceImpl(fixture.billingDAO, fixture.subscriptionDAO, fixture.planDAO,
                fixture.usageDAO);
        return fixture;
    }

    private static TelecomPlan buildPlan(String monthlyRental) {
        counter++;
        return new TelecomPlan("PLANCODE-BILL_" + counter, "Plan " + counter, "STANDARD",
                new BigDecimal(monthlyRental), new BigDecimal("20.00"), 30, "ACTIVE");
    }

    private static MobileSubscription buildSubscription(Long planId) {
        counter++;
        MobileSubscription subscription = new MobileSubscription("SUBNUM-BILL_" + counter, 1L,
                "9" + String.format("%09d", counter), (long) counter, planId, LocalDate.of(2026, 1, 1),
                "POSTPAID", "ACTIVE");
        return subscription;
    }

    private static void saveUsage(FakeUsageDAO usageDAO, Long subscriptionId, String charge,
                                   LocalDateTime usageDate) {
        UsageRecord record = new UsageRecord(subscriptionId, usageDate, "ROAMING", new BigDecimal("0.100"), "GB");
        record.setCharge(new BigDecimal(charge));
        usageDAO.save(record);
    }

    /** In-memory fake BillingDAO for this test only. Not a production class. */
    private static class FakeBillingDAO implements BillingDAO {

        private final Map<Long, Bill> billsById = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Bill findById(Long billId) {
            return billsById.get(billId);
        }

        @Override
        public Bill findByBillNumber(String billNumber) {
            for (Bill bill : billsById.values()) {
                if (bill.getBillNumber().equals(billNumber)) {
                    return bill;
                }
            }
            return null;
        }

        @Override
        public Bill findBySubscriptionIdAndBillingMonth(Long subscriptionId, LocalDate billingMonth) {
            for (Bill bill : billsById.values()) {
                if (bill.getSubscriptionId().equals(subscriptionId) && bill.getBillingMonth().equals(billingMonth)) {
                    return bill;
                }
            }
            return null;
        }

        @Override
        public List<Bill> findBySubscriptionId(Long subscriptionId) {
            List<Bill> result = new ArrayList<>();
            for (Bill bill : billsById.values()) {
                if (bill.getSubscriptionId().equals(subscriptionId)) {
                    result.add(bill);
                }
            }
            return result;
        }

        @Override
        public List<Bill> findByStatus(String billStatus) {
            List<Bill> result = new ArrayList<>();
            for (Bill bill : billsById.values()) {
                if (bill.getBillStatus().equals(billStatus)) {
                    result.add(bill);
                }
            }
            return result;
        }

        @Override
        public void save(Bill bill) {
            bill.setBillId(nextId++);
            billsById.put(bill.getBillId(), bill);
        }

        @Override
        public void update(Bill bill) {
            billsById.put(bill.getBillId(), bill);
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

    /** In-memory fake TelecomPlanDAO for this test only. Not a production class. */
    private static class FakeTelecomPlanDAO implements TelecomPlanDAO {

        private final Map<Long, TelecomPlan> plansById = new HashMap<>();
        private long nextId = 1L;

        @Override
        public TelecomPlan findById(Long planId) {
            return plansById.get(planId);
        }

        @Override
        public TelecomPlan findByPlanCode(String planCode) {
            for (TelecomPlan plan : plansById.values()) {
                if (plan.getPlanCode().equals(planCode)) {
                    return plan;
                }
            }
            return null;
        }

        @Override
        public boolean existsByPlanCode(String planCode) {
            return findByPlanCode(planCode) != null;
        }

        @Override
        public void save(TelecomPlan plan) {
            plan.setPlanId(nextId++);
            plansById.put(plan.getPlanId(), plan);
        }

        @Override
        public void update(TelecomPlan plan) {
            plansById.put(plan.getPlanId(), plan);
        }

        @Override
        public void deleteById(Long planId) {
            plansById.remove(planId);
        }

        @Override
        public List<TelecomPlan> findAllActive() {
            List<TelecomPlan> result = new ArrayList<>();
            for (TelecomPlan plan : plansById.values()) {
                if ("ACTIVE".equals(plan.getStatus())) {
                    result.add(plan);
                }
            }
            return result;
        }
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
}
