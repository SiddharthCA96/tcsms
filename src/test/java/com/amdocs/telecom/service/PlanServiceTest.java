package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.service.impl.PlanServiceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-Java unit test for PlanServiceImpl business logic, independent of JDBC/MySQL, using an
 * in-memory fake TelecomPlanDAO defined below (private nested class, test-only).
 */
public class PlanServiceTest {

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    private static int planCounter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("PlanServiceTest");
        System.out.println("========================================");

        testGetPlanByIdFoundAndMissing();
        testFindPlanByCode();
        testGetAllActivePlansExcludesInactive();
        testSearchActivePlansByName();
        testFilterActivePlansByMaxPrice();
        testFilterActivePlansByMinDataAllowance();
        testSortActivePlansByPriceAscending();
        testSortActivePlansByPriceDescending();
        testComparePlans();
        testCreatePlanSuccessAndDuplicateCode();
        testCreatePlanRequiredFieldValidation();
        testUpdatePlanDetails();
        testDeactivateThenActivatePlan();

        System.out.println("========================================");
        System.out.println("Tests executed: " + testsExecuted);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsExecuted - testsPassed));
        System.out.println("========================================");
    }

    // Test 1
    private static void testGetPlanByIdFoundAndMissing() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan plan = buildPlan("GETBYID", "500.00", "20.00", "ACTIVE");
        dao.save(plan);

        TelecomPlan found = service.getPlanById(plan.getPlanId());
        boolean foundCorrectly = found != null && plan.getPlanId().equals(found.getPlanId());

        RuntimeException thrown = expectRuntimeException(() -> service.getPlanById(999_999L));

        check("Test 1 - getPlanById() existing plan returned / missing plan throws",
                foundCorrectly && thrown != null);
    }

    // Test 2
    private static void testFindPlanByCode() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan plan = buildPlan("FINDCODE", "500.00", "20.00", "ACTIVE");
        dao.save(plan);

        Optional<TelecomPlan> found = service.findPlanByCode(plan.getPlanCode());
        Optional<TelecomPlan> notFound = service.findPlanByCode("NO-SUCH-CODE");

        check("Test 2 - findPlanByCode() returns Optional present/empty correctly",
                found.isPresent() && plan.getPlanId().equals(found.get().getPlanId()) && !notFound.isPresent());
    }

    // Test 3
    private static void testGetAllActivePlansExcludesInactive() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan activePlan = buildPlan("ACTIVE1", "500.00", "20.00", "ACTIVE");
        TelecomPlan inactivePlan = buildPlan("INACTIVE1", "500.00", "20.00", "INACTIVE");
        dao.save(activePlan);
        dao.save(inactivePlan);

        List<TelecomPlan> activePlans = service.getAllActivePlans();
        boolean containsActive = activePlans.stream().anyMatch(p -> p.getPlanId().equals(activePlan.getPlanId()));
        boolean excludesInactive = activePlans.stream().noneMatch(p -> p.getPlanId().equals(inactivePlan.getPlanId()));

        check("Test 3 - getAllActivePlans() includes active, excludes inactive",
                containsActive && excludesInactive);
    }

    // Test 4
    private static void testSearchActivePlansByName() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan premium = buildPlanNamed("5G Premium Alpha", "999.00", "100.00", "ACTIVE");
        TelecomPlan basic = buildPlanNamed("Basic Beta", "399.00", "20.00", "ACTIVE");
        dao.save(premium);
        dao.save(basic);

        List<TelecomPlan> results = service.searchActivePlansByName("premium");
        boolean onlyPremiumFound = results.size() == 1 && results.get(0).getPlanId().equals(premium.getPlanId());

        check("Test 4 - searchActivePlansByName() case-insensitive match", onlyPremiumFound);
    }

    // Test 5
    private static void testFilterActivePlansByMaxPrice() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan cheap = buildPlan("CHEAP", "300.00", "10.00", "ACTIVE");
        TelecomPlan expensive = buildPlan("EXPENSIVE", "1500.00", "200.00", "ACTIVE");
        dao.save(cheap);
        dao.save(expensive);

        List<TelecomPlan> results = service.filterActivePlansByMaxPrice(new BigDecimal("500.00"));
        boolean onlyCheapFound = results.size() == 1 && results.get(0).getPlanId().equals(cheap.getPlanId());

        check("Test 5 - filterActivePlansByMaxPrice() returns only plans at or below the max price",
                onlyCheapFound);
    }

    // Test 6
    private static void testFilterActivePlansByMinDataAllowance() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan lowData = buildPlan("LOWDATA", "300.00", "5.00", "ACTIVE");
        TelecomPlan highData = buildPlan("HIGHDATA", "1500.00", "200.00", "ACTIVE");
        dao.save(lowData);
        dao.save(highData);

        List<TelecomPlan> results = service.filterActivePlansByMinDataAllowance(new BigDecimal("50.00"));
        boolean onlyHighDataFound = results.size() == 1 && results.get(0).getPlanId().equals(highData.getPlanId());

        check("Test 6 - filterActivePlansByMinDataAllowance() returns only plans at or above the min allowance",
                onlyHighDataFound);
    }

    // Test 7
    private static void testSortActivePlansByPriceAscending() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan mid = buildPlan("MID", "700.00", "50.00", "ACTIVE");
        TelecomPlan low = buildPlan("LOW", "300.00", "10.00", "ACTIVE");
        TelecomPlan high = buildPlan("HIGH", "1500.00", "200.00", "ACTIVE");
        dao.save(mid);
        dao.save(low);
        dao.save(high);

        List<TelecomPlan> sorted = service.sortActivePlansByPriceAscending();
        boolean orderedAscending = sorted.size() == 3
                && sorted.get(0).getPlanId().equals(low.getPlanId())
                && sorted.get(1).getPlanId().equals(mid.getPlanId())
                && sorted.get(2).getPlanId().equals(high.getPlanId());

        check("Test 7 - sortActivePlansByPriceAscending() orders low to high", orderedAscending);
    }

    // Test 8
    private static void testSortActivePlansByPriceDescending() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan mid = buildPlan("MIDD", "700.00", "50.00", "ACTIVE");
        TelecomPlan low = buildPlan("LOWD", "300.00", "10.00", "ACTIVE");
        TelecomPlan high = buildPlan("HIGHD", "1500.00", "200.00", "ACTIVE");
        dao.save(mid);
        dao.save(low);
        dao.save(high);

        List<TelecomPlan> sorted = service.sortActivePlansByPriceDescending();
        boolean orderedDescending = sorted.size() == 3
                && sorted.get(0).getPlanId().equals(high.getPlanId())
                && sorted.get(1).getPlanId().equals(mid.getPlanId())
                && sorted.get(2).getPlanId().equals(low.getPlanId());

        check("Test 8 - sortActivePlansByPriceDescending() orders high to low", orderedDescending);
    }

    // Test 9
    private static void testComparePlans() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan planA = buildPlan("COMPAREA", "500.00", "20.00", "ACTIVE");
        TelecomPlan planB = buildPlan("COMPAREB", "900.00", "80.00", "ACTIVE");
        dao.save(planA);
        dao.save(planB);

        List<TelecomPlan> compared = service.comparePlans(Arrays.asList(planA.getPlanId(), planB.getPlanId()));
        boolean bothReturned = compared.size() == 2
                && compared.stream().anyMatch(p -> p.getPlanId().equals(planA.getPlanId()))
                && compared.stream().anyMatch(p -> p.getPlanId().equals(planB.getPlanId()));

        RuntimeException thrown = expectRuntimeException(() -> service.comparePlans(Arrays.asList(999_999L)));

        check("Test 9 - comparePlans() returns requested plans / throws for unknown planId",
                bothReturned && thrown != null);
    }

    // Test 10
    private static void testCreatePlanSuccessAndDuplicateCode() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan plan = buildPlan("CREATE_OK", "500.00", "20.00", null);
        // status intentionally left unset to verify the ACTIVE default is applied.
        TelecomPlan created = service.createPlan(plan);

        boolean idAssigned = created.getPlanId() != null;
        boolean statusDefaultedToActive = "ACTIVE".equals(created.getStatus());
        boolean storedInDao = idAssigned && dao.findById(created.getPlanId()) != null;

        TelecomPlan duplicate = buildPlan("CREATE_DUP", "500.00", "20.00", null);
        duplicate.setPlanCode(plan.getPlanCode());
        RuntimeException thrown = expectRuntimeException(() -> service.createPlan(duplicate));

        check("Test 10 - createPlan() success with ACTIVE default, duplicate planCode rejected",
                idAssigned && statusDefaultedToActive && storedInDao && thrown != null);
    }

    // Test 11
    private static void testCreatePlanRequiredFieldValidation() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan invalidPlan = buildPlan("REQUIRED", "500.00", "20.00", "ACTIVE");
        invalidPlan.setPlanName("");

        RuntimeException thrown = expectRuntimeException(() -> service.createPlan(invalidPlan));

        check("Test 11 - createPlan() rejects blank required field (planName)", thrown != null);
    }

    // Test 12
    private static void testUpdatePlanDetails() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan plan = buildPlan("UPDATE_OK", "500.00", "20.00", "ACTIVE");
        service.createPlan(plan);

        TelecomPlan updateRequest = clonePlan(plan);
        updateRequest.setMonthlyRental(new BigDecimal("650.00"));
        updateRequest.setStatus("INACTIVE"); // must be ignored - status is lifecycle-only

        service.updatePlanDetails(updateRequest);

        TelecomPlan persisted = dao.findById(plan.getPlanId());
        boolean rentalUpdated = new BigDecimal("650.00").compareTo(persisted.getMonthlyRental()) == 0;
        boolean statusUnaffectedByDetailUpdate = "ACTIVE".equals(persisted.getStatus());

        check("Test 12 - updatePlanDetails() updates editable fields and ignores status changes",
                rentalUpdated && statusUnaffectedByDetailUpdate);
    }

    // Tests 13 - deactivate then activate the same plan
    private static void testDeactivateThenActivatePlan() {
        FakeTelecomPlanDAO dao = new FakeTelecomPlanDAO();
        PlanService service = new PlanServiceImpl(dao);

        TelecomPlan plan = buildPlan("LIFECYCLE", "500.00", "20.00", "ACTIVE");
        service.createPlan(plan);

        service.deactivatePlan(plan.getPlanId());
        TelecomPlan afterDeactivate = dao.findById(plan.getPlanId());
        boolean deactivated = "INACTIVE".equals(afterDeactivate.getStatus());

        service.activatePlan(plan.getPlanId());
        TelecomPlan afterActivate = dao.findById(plan.getPlanId());
        boolean reactivated = "ACTIVE".equals(afterActivate.getStatus());

        check("Test 13 - deactivatePlan()/activatePlan() toggle status correctly",
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

    private static TelecomPlan buildPlan(String label, String monthlyRental, String dataAllowanceGb,
                                          String status) {
        planCounter++;
        String unique = label + "_" + planCounter;
        TelecomPlan plan = new TelecomPlan(
                "PLANCODE-" + unique,
                "Plan " + unique,
                "STANDARD",
                new BigDecimal(monthlyRental),
                new BigDecimal(dataAllowanceGb),
                30,
                status);
        plan.setVoiceMinutes(1000);
        plan.setSmsAllowance(500);
        plan.setInternationalRoaming(false);
        return plan;
    }

    private static TelecomPlan buildPlanNamed(String planName, String monthlyRental, String dataAllowanceGb,
                                               String status) {
        planCounter++;
        TelecomPlan plan = buildPlan(planName.replace(" ", "_"), monthlyRental, dataAllowanceGb, status);
        plan.setPlanName(planName);
        return plan;
    }

    private static TelecomPlan clonePlan(TelecomPlan source) {
        TelecomPlan copy = new TelecomPlan(
                source.getPlanCode(),
                source.getPlanName(),
                source.getPlanType(),
                source.getMonthlyRental(),
                source.getDataAllowanceGb(),
                source.getValidityDays(),
                source.getStatus());
        copy.setPlanId(source.getPlanId());
        copy.setVoiceMinutes(source.getVoiceMinutes());
        copy.setSmsAllowance(source.getSmsAllowance());
        copy.setInternationalRoaming(source.getInternationalRoaming());
        return copy;
    }

    /**
     * In-memory fake TelecomPlanDAO for this test only. Not a production class.
     */
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
            List<TelecomPlan> activePlans = new ArrayList<>();
            for (TelecomPlan plan : plansById.values()) {
                if ("ACTIVE".equals(plan.getStatus())) {
                    activePlans.add(plan);
                }
            }
            return activePlans;
        }
    }
}
