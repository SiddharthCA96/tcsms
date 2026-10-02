package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.ComplaintDAO;
import com.amdocs.telecom.model.Complaint;
import com.amdocs.telecom.service.impl.ComplaintServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-Java unit test for ComplaintServiceImpl business logic, independent of JDBC/MySQL, using
 * an in-memory fake ComplaintDAO defined below (private nested class, test-only).
 */
public class ComplaintServiceTest {

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    private static int counter = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("ComplaintServiceTest");
        System.out.println("========================================");

        testCreateComplaintSuccess();
        testCreateComplaintRequiredFieldValidation();
        testCreateComplaintInvalidCategoryRejected();
        testCreateComplaintValidCategoriesAccepted();
        testGetComplaintById();
        testFindComplaintByNumber();
        testGetCustomerAndSubscriptionComplaints();
        testGetComplaintsByStatusAndPriority();
        testUpdateComplaintEditableFields();
        testUpdateComplaintProtectsStatusAndResolution();
        testResolveComplaint();
        testResolveComplaintAlreadyResolvedRejected();

        System.out.println("========================================");
        System.out.println("Tests executed: " + testsExecuted);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + (testsExecuted - testsPassed));
        System.out.println("========================================");
    }

    // Test 1
    private static void testCreateComplaintSuccess() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "NETWORK", "HIGH", null);
        // status intentionally left unset to verify the OPEN default is applied.
        Complaint created = service.createComplaint(complaint);

        boolean idAssigned = created.getComplaintId() != null;
        boolean storedInDao = idAssigned && dao.findById(created.getComplaintId()) != null;
        boolean statusDefaultedToOpen = "OPEN".equals(created.getStatus());

        check("Test 1 - createComplaint() successful creation with OPEN default",
                idAssigned && storedInDao && statusDefaultedToOpen);
    }

    // Test 2
    private static void testCreateComplaintRequiredFieldValidation() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint invalid = buildComplaint(1L, "NETWORK", "HIGH", null);
        invalid.setDescription("");

        RuntimeException thrown = expectRuntimeException(() -> service.createComplaint(invalid));

        check("Test 2 - createComplaint() rejects a blank required field (description)", thrown != null);
    }

    // Test 3
    private static void testCreateComplaintInvalidCategoryRejected() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint invalid = buildComplaint(1L, "WEATHER", "HIGH", null);

        RuntimeException thrown = expectRuntimeException(() -> service.createComplaint(invalid));

        check("Test 3 - createComplaint() rejects an unsupported category", thrown != null);
    }

    // Test 4
    private static void testCreateComplaintValidCategoriesAccepted() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        boolean allAccepted = true;
        for (String category : new String[] {"BILLING", "NETWORK", "SIM", "PLAN", "PAYMENT", "OTHER"}) {
            Complaint complaint = buildComplaint(1L, category, "LOW", null);
            Complaint created = service.createComplaint(complaint);
            allAccepted &= created.getComplaintId() != null;
        }

        check("Test 4 - createComplaint() accepts every supported category "
                + "(BILLING/NETWORK/SIM/PLAN/PAYMENT/OTHER)", allAccepted);
    }

    // Test 5
    private static void testGetComplaintById() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "BILLING", "MEDIUM", null);
        service.createComplaint(complaint);

        Complaint found = service.getComplaintById(complaint.getComplaintId());
        RuntimeException thrown = expectRuntimeException(() -> service.getComplaintById(999_999L));

        check("Test 5 - getComplaintById() existing complaint returned / missing complaint throws",
                found != null && complaint.getComplaintId().equals(found.getComplaintId()) && thrown != null);
    }

    // Test 6
    private static void testFindComplaintByNumber() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "BILLING", "MEDIUM", null);
        service.createComplaint(complaint);

        Optional<Complaint> found = service.findComplaintByNumber(complaint.getComplaintNumber());
        Optional<Complaint> notFound = service.findComplaintByNumber("NO-SUCH-NUMBER");

        check("Test 6 - findComplaintByNumber() returns Optional present/empty correctly",
                found.isPresent() && complaint.getComplaintId().equals(found.get().getComplaintId())
                        && !notFound.isPresent());
    }

    // Test 7
    private static void testGetCustomerAndSubscriptionComplaints() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(7L, "NETWORK", "HIGH", 3L);
        service.createComplaint(complaint);

        List<Complaint> byCustomer = service.getCustomerComplaints(7L);
        List<Complaint> bySubscription = service.getSubscriptionComplaints(3L);

        check("Test 7 - getCustomerComplaints()/getSubscriptionComplaints() return the complaint",
                byCustomer.size() == 1 && bySubscription.size() == 1);
    }

    // Test 8
    private static void testGetComplaintsByStatusAndPriority() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "SIM", "HIGH", null);
        service.createComplaint(complaint);

        List<Complaint> byStatus = service.getComplaintsByStatus("OPEN");
        List<Complaint> byPriority = service.getComplaintsByPriority("HIGH");

        check("Test 8 - getComplaintsByStatus()/getComplaintsByPriority() filter correctly",
                byStatus.size() == 1 && byPriority.size() == 1);
    }

    // Test 9
    private static void testUpdateComplaintEditableFields() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "NETWORK", "LOW", null);
        service.createComplaint(complaint);

        Complaint updateRequest = cloneForUpdate(complaint);
        updateRequest.setCategory("BILLING");
        updateRequest.setPriority("HIGH");
        updateRequest.setDescription("Updated description");

        service.updateComplaint(updateRequest);

        Complaint persisted = dao.findById(complaint.getComplaintId());
        check("Test 9 - updateComplaint() persists editable field changes",
                "BILLING".equals(persisted.getCategory()) && "HIGH".equals(persisted.getPriority())
                        && "Updated description".equals(persisted.getDescription()));
    }

    // Test 10
    private static void testUpdateComplaintProtectsStatusAndResolution() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "NETWORK", "LOW", null);
        service.createComplaint(complaint);

        Complaint updateRequest = cloneForUpdate(complaint);
        updateRequest.setStatus("RESOLVED");
        updateRequest.setResolution("Sneaky resolution via generic update");
        updateRequest.setCategory("BILLING"); // legitimate editable change

        service.updateComplaint(updateRequest);

        Complaint persisted = dao.findById(complaint.getComplaintId());
        check("Test 10 - updateComplaint() protects status/resolution while still applying editable changes",
                "OPEN".equals(persisted.getStatus()) && persisted.getResolution() == null
                        && "BILLING".equals(persisted.getCategory()));
    }

    // Test 11
    private static void testResolveComplaint() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "NETWORK", "HIGH", null);
        service.createComplaint(complaint);

        service.resolveComplaint(complaint.getComplaintId(), "Issue fixed by field technician.");

        Complaint persisted = dao.findById(complaint.getComplaintId());
        check("Test 11 - resolveComplaint() sets status RESOLVED and records resolution text",
                "RESOLVED".equals(persisted.getStatus())
                        && "Issue fixed by field technician.".equals(persisted.getResolution()));
    }

    // Test 12
    private static void testResolveComplaintAlreadyResolvedRejected() {
        FakeComplaintDAO dao = new FakeComplaintDAO();
        ComplaintService service = new ComplaintServiceImpl(dao);

        Complaint complaint = buildComplaint(1L, "NETWORK", "HIGH", null);
        service.createComplaint(complaint);
        service.resolveComplaint(complaint.getComplaintId(), "First resolution.");

        RuntimeException thrown = expectRuntimeException(() ->
                service.resolveComplaint(complaint.getComplaintId(), "Second resolution."));

        check("Test 12 - resolveComplaint() rejects resolving an already-resolved complaint", thrown != null);
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

    private static Complaint buildComplaint(Long customerId, String category, String priority,
                                             Long subscriptionId) {
        counter++;
        Complaint complaint = new Complaint("COMP-TEST-" + counter, customerId, category,
                "Description " + counter, priority, null);
        complaint.setSubscriptionId(subscriptionId);
        return complaint;
    }

    private static Complaint cloneForUpdate(Complaint source) {
        Complaint copy = new Complaint(source.getComplaintNumber(), source.getCustomerId(), source.getCategory(),
                source.getDescription(), source.getPriority(), source.getStatus());
        copy.setComplaintId(source.getComplaintId());
        copy.setSubscriptionId(source.getSubscriptionId());
        copy.setResolution(source.getResolution());
        copy.setCreatedDate(source.getCreatedDate());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    /** In-memory fake ComplaintDAO for this test only. Not a production class. */
    private static class FakeComplaintDAO implements ComplaintDAO {

        private final Map<Long, Complaint> complaintsById = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Complaint findById(Long complaintId) {
            return complaintsById.get(complaintId);
        }

        @Override
        public Complaint findByComplaintNumber(String complaintNumber) {
            for (Complaint complaint : complaintsById.values()) {
                if (complaint.getComplaintNumber().equals(complaintNumber)) {
                    return complaint;
                }
            }
            return null;
        }

        @Override
        public List<Complaint> findByCustomerId(Long customerId) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint complaint : complaintsById.values()) {
                if (complaint.getCustomerId().equals(customerId)) {
                    result.add(complaint);
                }
            }
            return result;
        }

        @Override
        public List<Complaint> findBySubscriptionId(Long subscriptionId) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint complaint : complaintsById.values()) {
                if (subscriptionId.equals(complaint.getSubscriptionId())) {
                    result.add(complaint);
                }
            }
            return result;
        }

        @Override
        public List<Complaint> findByStatus(String status) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint complaint : complaintsById.values()) {
                if (complaint.getStatus().equals(status)) {
                    result.add(complaint);
                }
            }
            return result;
        }

        @Override
        public List<Complaint> findByPriority(String priority) {
            List<Complaint> result = new ArrayList<>();
            for (Complaint complaint : complaintsById.values()) {
                if (complaint.getPriority().equals(priority)) {
                    result.add(complaint);
                }
            }
            return result;
        }

        @Override
        public void save(Complaint complaint) {
            complaint.setComplaintId(nextId++);
            complaintsById.put(complaint.getComplaintId(), complaint);
        }

        @Override
        public void update(Complaint complaint) {
            complaintsById.put(complaint.getComplaintId(), complaint);
        }
    }
}
