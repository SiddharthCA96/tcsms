package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.SubscriptionHistoryDAO;
import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.SubscriptionHistory;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.service.impl.SubscriptionServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-Java unit test for SubscriptionServiceImpl business logic, independent of JDBC/MySQL,
 * using in-memory fake DAOs defined below (private nested classes, test-only).
 */
public class SubscriptionServiceTest {

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    private static int counter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("SubscriptionServiceTest");
        System.out.println("========================================");

        testSubscribeToPlanSuccess();
        testSubscribeToPlanDuplicateRejected();
        testSubscribeToPlanInactivePlanRejected();
        testSubscribeToPlanDuplicateMobileRejected();
        testGetSubscriptionByIdAndNumber();
        testGetCustomerSubscriptions();
        testChangePlanSuccessAndHistoryCreated();
        testChangePlanSamePlanRejected();
        testChangePlanInactiveNewPlanRejected();
        testUpgradePlanSuccess();
        testUpgradePlanRejectsCheaperPlan();
        testDowngradePlanSuccess();
        testDowngradePlanRejectsMoreExpensivePlan();
        testChangeSubscriptionType();
        testChangeSubscriptionTypeInvalidValueRejected();
        testActivateThenDeactivateSubscription();

        System.out.println("========================================");
        System.out.println("Tests executed: " + testsExecuted);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsExecuted - testsPassed));
        System.out.println("========================================");
    }

    // Test 1
    private static void testSubscribeToPlanSuccess() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("SUBOK", "500.00", "ACTIVE");
        planDAO.save(plan);

        MobileSubscription subscription = buildSubscription(1L, plan.getPlanId(), "SUBOK");
        MobileSubscription saved = service.subscribeToPlan(subscription);

        boolean idAssigned = saved.getSubscriptionId() != null;
        boolean storedInDao = idAssigned && subscriptionDAO.findById(saved.getSubscriptionId()) != null;
        boolean statusDefaultedToActive = "ACTIVE".equals(saved.getStatus());

        check("Test 1 - subscribeToPlan() successful subscription with ACTIVE default",
                idAssigned && storedInDao && statusDefaultedToActive);
    }

    // Test 2
    private static void testSubscribeToPlanDuplicateRejected() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("SUBDUP", "500.00", "ACTIVE");
        planDAO.save(plan);

        MobileSubscription first = buildSubscription(1L, plan.getPlanId(), "SUBDUP1");
        service.subscribeToPlan(first);

        MobileSubscription second = buildSubscription(1L, plan.getPlanId(), "SUBDUP2");
        RuntimeException thrown = expectRuntimeException(() -> service.subscribeToPlan(second));

        check("Test 2 - subscribeToPlan() rejects same customer subscribing to same plan twice",
                thrown != null);
    }

    // Test 3
    private static void testSubscribeToPlanInactivePlanRejected() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan inactivePlan = buildPlan("SUBINACTIVE", "500.00", "INACTIVE");
        planDAO.save(inactivePlan);

        MobileSubscription subscription = buildSubscription(1L, inactivePlan.getPlanId(), "SUBINACTIVE1");
        RuntimeException thrown = expectRuntimeException(() -> service.subscribeToPlan(subscription));

        check("Test 3 - subscribeToPlan() rejects an inactive plan", thrown != null);
    }

    // Test 4
    private static void testSubscribeToPlanDuplicateMobileRejected() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan planA = buildPlan("MOBILEA", "500.00", "ACTIVE");
        TelecomPlan planB = buildPlan("MOBILEB", "500.00", "ACTIVE");
        planDAO.save(planA);
        planDAO.save(planB);

        MobileSubscription first = buildSubscription(1L, planA.getPlanId(), "MOBILEDUP");
        service.subscribeToPlan(first);

        MobileSubscription second = buildSubscription(2L, planB.getPlanId(), "MOBILEDUP2");
        second.setMobileNumber(first.getMobileNumber());

        RuntimeException thrown = expectRuntimeException(() -> service.subscribeToPlan(second));

        check("Test 4 - subscribeToPlan() rejects an already-used mobile number", thrown != null);
    }

    // Test 5
    private static void testGetSubscriptionByIdAndNumber() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("LOOKUP", "500.00", "ACTIVE");
        planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(1L, plan.getPlanId(), "LOOKUP1");
        service.subscribeToPlan(subscription);

        MobileSubscription byId = service.getSubscriptionById(subscription.getSubscriptionId());
        boolean foundById = byId != null && subscription.getSubscriptionId().equals(byId.getSubscriptionId());

        Optional<MobileSubscription> byNumber = service.findSubscriptionByNumber(subscription.getSubscriptionNumber());
        Optional<MobileSubscription> byUnknownNumber = service.findSubscriptionByNumber("NO-SUCH-NUMBER");

        RuntimeException thrown = expectRuntimeException(() -> service.getSubscriptionById(999_999L));

        check("Test 5 - getSubscriptionById()/findSubscriptionByNumber() behave correctly",
                foundById && byNumber.isPresent() && !byUnknownNumber.isPresent() && thrown != null);
    }

    // Test 6
    private static void testGetCustomerSubscriptions() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan planA = buildPlan("CUSTSUBA", "500.00", "ACTIVE");
        TelecomPlan planB = buildPlan("CUSTSUBB", "700.00", "ACTIVE");
        planDAO.save(planA);
        planDAO.save(planB);

        MobileSubscription subA = buildSubscription(5L, planA.getPlanId(), "CUSTSUB1");
        MobileSubscription subB = buildSubscription(5L, planB.getPlanId(), "CUSTSUB2");
        service.subscribeToPlan(subA);
        service.subscribeToPlan(subB);

        List<MobileSubscription> customerSubs = service.getCustomerSubscriptions(5L);

        check("Test 6 - getCustomerSubscriptions() returns all subscriptions for the customer",
                customerSubs.size() == 2);
    }

    // Test 7
    private static void testChangePlanSuccessAndHistoryCreated() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan oldPlan = buildPlan("CHANGEOLD", "500.00", "ACTIVE");
        TelecomPlan newPlan = buildPlan("CHANGENEW", "700.00", "ACTIVE");
        planDAO.save(oldPlan);
        planDAO.save(newPlan);

        MobileSubscription subscription = buildSubscription(1L, oldPlan.getPlanId(), "CHANGE1");
        service.subscribeToPlan(subscription);

        service.changePlan(subscription.getSubscriptionId(), newPlan.getPlanId(), "admin", "Customer request");

        MobileSubscription updated = subscriptionDAO.findById(subscription.getSubscriptionId());
        boolean planIdUpdated = newPlan.getPlanId().equals(updated.getPlanId());

        List<SubscriptionHistory> history = service.getSubscriptionHistory(subscription.getSubscriptionId());
        boolean historyRecorded = history.size() == 1
                && oldPlan.getPlanId().equals(history.get(0).getOldPlanId())
                && newPlan.getPlanId().equals(history.get(0).getNewPlanId());

        check("Test 7 - changePlan() updates subscription and records SubscriptionHistory",
                planIdUpdated && historyRecorded);
    }

    // Test 8
    private static void testChangePlanSamePlanRejected() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("SAMEPLAN", "500.00", "ACTIVE");
        planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(1L, plan.getPlanId(), "SAMEPLAN1");
        service.subscribeToPlan(subscription);

        RuntimeException thrown = expectRuntimeException(() ->
                service.changePlan(subscription.getSubscriptionId(), plan.getPlanId(), "admin", "no-op"));

        check("Test 8 - changePlan() rejects changing to the same plan", thrown != null);
    }

    // Test 9
    private static void testChangePlanInactiveNewPlanRejected() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan oldPlan = buildPlan("INACTOLD", "500.00", "ACTIVE");
        TelecomPlan inactiveNewPlan = buildPlan("INACTNEW", "700.00", "INACTIVE");
        planDAO.save(oldPlan);
        planDAO.save(inactiveNewPlan);

        MobileSubscription subscription = buildSubscription(1L, oldPlan.getPlanId(), "INACT1");
        service.subscribeToPlan(subscription);

        RuntimeException thrown = expectRuntimeException(() ->
                service.changePlan(subscription.getSubscriptionId(), inactiveNewPlan.getPlanId(), "admin", "x"));

        check("Test 9 - changePlan() rejects changing to an inactive plan", thrown != null);
    }

    // Test 10
    private static void testUpgradePlanSuccess() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan cheapPlan = buildPlan("UPGOLD", "500.00", "ACTIVE");
        TelecomPlan expensivePlan = buildPlan("UPGNEW", "900.00", "ACTIVE");
        planDAO.save(cheapPlan);
        planDAO.save(expensivePlan);

        MobileSubscription subscription = buildSubscription(1L, cheapPlan.getPlanId(), "UPG1");
        service.subscribeToPlan(subscription);

        service.upgradePlan(subscription.getSubscriptionId(), expensivePlan.getPlanId(), "admin");

        MobileSubscription updated = subscriptionDAO.findById(subscription.getSubscriptionId());
        check("Test 10 - upgradePlan() succeeds when the new plan is more expensive",
                expensivePlan.getPlanId().equals(updated.getPlanId()));
    }

    // Test 11
    private static void testUpgradePlanRejectsCheaperPlan() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan expensivePlan = buildPlan("UPGREJOLD", "900.00", "ACTIVE");
        TelecomPlan cheapPlan = buildPlan("UPGREJNEW", "500.00", "ACTIVE");
        planDAO.save(expensivePlan);
        planDAO.save(cheapPlan);

        MobileSubscription subscription = buildSubscription(1L, expensivePlan.getPlanId(), "UPGREJ1");
        service.subscribeToPlan(subscription);

        RuntimeException thrown = expectRuntimeException(() ->
                service.upgradePlan(subscription.getSubscriptionId(), cheapPlan.getPlanId(), "admin"));

        check("Test 11 - upgradePlan() rejects a plan that is not more expensive", thrown != null);
    }

    // Test 12
    private static void testDowngradePlanSuccess() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan expensivePlan = buildPlan("DOWNOLD", "900.00", "ACTIVE");
        TelecomPlan cheapPlan = buildPlan("DOWNNEW", "500.00", "ACTIVE");
        planDAO.save(expensivePlan);
        planDAO.save(cheapPlan);

        MobileSubscription subscription = buildSubscription(1L, expensivePlan.getPlanId(), "DOWN1");
        service.subscribeToPlan(subscription);

        service.downgradePlan(subscription.getSubscriptionId(), cheapPlan.getPlanId(), "admin");

        MobileSubscription updated = subscriptionDAO.findById(subscription.getSubscriptionId());
        check("Test 12 - downgradePlan() succeeds when the new plan is cheaper",
                cheapPlan.getPlanId().equals(updated.getPlanId()));
    }

    // Test 13
    private static void testDowngradePlanRejectsMoreExpensivePlan() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan cheapPlan = buildPlan("DOWNREJOLD", "500.00", "ACTIVE");
        TelecomPlan expensivePlan = buildPlan("DOWNREJNEW", "900.00", "ACTIVE");
        planDAO.save(cheapPlan);
        planDAO.save(expensivePlan);

        MobileSubscription subscription = buildSubscription(1L, cheapPlan.getPlanId(), "DOWNREJ1");
        service.subscribeToPlan(subscription);

        RuntimeException thrown = expectRuntimeException(() ->
                service.downgradePlan(subscription.getSubscriptionId(), expensivePlan.getPlanId(), "admin"));

        check("Test 13 - downgradePlan() rejects a plan that is not cheaper", thrown != null);
    }

    // Test 14
    private static void testChangeSubscriptionType() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("TYPECHANGE", "500.00", "ACTIVE");
        planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(1L, plan.getPlanId(), "TYPECHANGE1");
        subscription.setSubscriptionType("PREPAID");
        service.subscribeToPlan(subscription);

        service.changeSubscriptionType(subscription.getSubscriptionId(), "POSTPAID");

        MobileSubscription updated = subscriptionDAO.findById(subscription.getSubscriptionId());
        check("Test 14 - changeSubscriptionType() switches PREPAID to POSTPAID",
                "POSTPAID".equals(updated.getSubscriptionType()));
    }

    // Test 15
    private static void testChangeSubscriptionTypeInvalidValueRejected() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("TYPEINVALID", "500.00", "ACTIVE");
        planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(1L, plan.getPlanId(), "TYPEINVALID1");
        service.subscribeToPlan(subscription);

        RuntimeException thrown = expectRuntimeException(() ->
                service.changeSubscriptionType(subscription.getSubscriptionId(), "UNKNOWN_TYPE"));

        check("Test 15 - changeSubscriptionType() rejects an unsupported type value", thrown != null);
    }

    // Test 16
    private static void testActivateThenDeactivateSubscription() {
        FakeMobileSubscriptionDAO subscriptionDAO = new FakeMobileSubscriptionDAO();
        FakeTelecomPlanDAO planDAO = new FakeTelecomPlanDAO();
        FakeSubscriptionHistoryDAO historyDAO = new FakeSubscriptionHistoryDAO();
        SubscriptionService service = new SubscriptionServiceImpl(subscriptionDAO, planDAO, historyDAO);

        TelecomPlan plan = buildPlan("LIFECYCLE", "500.00", "ACTIVE");
        planDAO.save(plan);
        MobileSubscription subscription = buildSubscription(1L, plan.getPlanId(), "LIFECYCLE1");
        service.subscribeToPlan(subscription);

        service.deactivateSubscription(subscription.getSubscriptionId());
        boolean deactivated = "INACTIVE".equals(subscriptionDAO.findById(subscription.getSubscriptionId()).getStatus());

        service.activateSubscription(subscription.getSubscriptionId());
        boolean reactivated = "ACTIVE".equals(subscriptionDAO.findById(subscription.getSubscriptionId()).getStatus());

        check("Test 16 - deactivateSubscription()/activateSubscription() toggle status correctly",
                deactivated && reactivated);
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

    private static TelecomPlan buildPlan(String label, String monthlyRental, String status) {
        counter++;
        TelecomPlan plan = new TelecomPlan(
                "PLANCODE-" + label + "_" + counter,
                "Plan " + label,
                "STANDARD",
                new BigDecimal(monthlyRental),
                new BigDecimal("20.00"),
                30,
                status);
        return plan;
    }

    private static MobileSubscription buildSubscription(Long customerId, Long planId, String label) {
        counter++;
        return new MobileSubscription(
                "SUBNUM-" + label + "_" + counter,
                customerId,
                "9" + String.format("%09d", counter),
                (long) counter,
                planId,
                LocalDate.of(2026, 1, 1),
                "POSTPAID",
                null);
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

    /** In-memory fake SubscriptionHistoryDAO for this test only. Not a production class. */
    private static class FakeSubscriptionHistoryDAO implements SubscriptionHistoryDAO {

        private final Map<Long, SubscriptionHistory> historyById = new HashMap<>();
        private long nextId = 1L;

        @Override
        public SubscriptionHistory findById(Long historyId) {
            return historyById.get(historyId);
        }

        @Override
        public List<SubscriptionHistory> findBySubscriptionId(Long subscriptionId) {
            List<SubscriptionHistory> result = new ArrayList<>();
            for (SubscriptionHistory history : historyById.values()) {
                if (history.getSubscriptionId().equals(subscriptionId)) {
                    result.add(history);
                }
            }
            return result;
        }

        @Override
        public void save(SubscriptionHistory history) {
            history.setHistoryId(nextId++);
            historyById.put(history.getHistoryId(), history);
        }
    }
}
