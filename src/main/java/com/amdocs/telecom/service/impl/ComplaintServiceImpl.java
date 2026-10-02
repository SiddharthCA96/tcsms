package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.ComplaintDAO;
import com.amdocs.telecom.model.Complaint;
import com.amdocs.telecom.service.ComplaintService;

import java.util.List;
import java.util.Optional;

/**
 * Business logic implementation of ComplaintService.
 *
 * 'OPEN' is used as the initial status for a newly created complaint and 'RESOLVED' as the
 * resolved status - both are actual values already used in seed_data.sql, not invented ones.
 */
public class ComplaintServiceImpl implements ComplaintService {

    private static final String COMPLAINT_STATUS_OPEN = "OPEN";
    private static final String COMPLAINT_STATUS_RESOLVED = "RESOLVED";

    private final ComplaintDAO complaintDAO;

    public ComplaintServiceImpl(ComplaintDAO complaintDAO) {
        this.complaintDAO = complaintDAO;
    }

    @Override
    public Complaint createComplaint(Complaint complaint) {
        if (complaint == null) {
            throw new RuntimeException("Failed to create complaint: complaint must not be null.");
        }
        validateRequiredFields(complaint);
        validateCategory(complaint.getCategory());

        if (complaint.getStatus() == null || complaint.getStatus().trim().isEmpty()) {
            complaint.setStatus(COMPLAINT_STATUS_OPEN);
        }

        complaintDAO.save(complaint);
        return complaint;
    }

    @Override
    public Complaint getComplaintById(Long complaintId) {
        if (complaintId == null) {
            throw new RuntimeException("Failed to get complaint: complaintId must not be null.");
        }

        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null) {
            throw new RuntimeException("Failed to get complaint: no complaint found with ID: " + complaintId);
        }
        return complaint;
    }

    @Override
    public Optional<Complaint> findComplaintByNumber(String complaintNumber) {
        if (complaintNumber == null || complaintNumber.trim().isEmpty()) {
            throw new RuntimeException("Failed to find complaint: complaintNumber must not be null or empty.");
        }
        return Optional.ofNullable(complaintDAO.findByComplaintNumber(complaintNumber));
    }

    @Override
    public List<Complaint> getCustomerComplaints(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Failed to get customer complaints: customerId must not be null.");
        }
        return complaintDAO.findByCustomerId(customerId);
    }

    @Override
    public List<Complaint> getSubscriptionComplaints(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new RuntimeException("Failed to get subscription complaints: subscriptionId must not be null.");
        }
        return complaintDAO.findBySubscriptionId(subscriptionId);
    }

    @Override
    public List<Complaint> getComplaintsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new RuntimeException("Failed to get complaints: status must not be null or empty.");
        }
        return complaintDAO.findByStatus(status);
    }

    @Override
    public List<Complaint> getComplaintsByPriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) {
            throw new RuntimeException("Failed to get complaints: priority must not be null or empty.");
        }
        return complaintDAO.findByPriority(priority);
    }

    @Override
    public void updateComplaint(Complaint complaint) {
        if (complaint == null || complaint.getComplaintId() == null) {
            throw new RuntimeException("Failed to update complaint: complaint and complaintId must not be null.");
        }

        Complaint existingComplaint = complaintDAO.findById(complaint.getComplaintId());
        if (existingComplaint == null) {
            throw new RuntimeException("Failed to update complaint: no complaint found with ID: "
                    + complaint.getComplaintId());
        }

        validateRequiredFields(complaint);
        validateCategory(complaint.getCategory());

        // Protect fields that a detail update must never change - resolution is its own flow.
        complaint.setStatus(existingComplaint.getStatus());
        complaint.setResolution(existingComplaint.getResolution());
        complaint.setCreatedDate(existingComplaint.getCreatedDate());

        complaintDAO.update(complaint);
    }

    @Override
    public void resolveComplaint(Long complaintId, String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new RuntimeException("Failed to resolve complaint: resolution must not be null or empty.");
        }

        Complaint complaint = getComplaintById(complaintId);
        if (COMPLAINT_STATUS_RESOLVED.equals(complaint.getStatus())) {
            throw new RuntimeException("Failed to resolve complaint: complaint ID " + complaintId
                    + " is already resolved.");
        }

        complaint.setStatus(COMPLAINT_STATUS_RESOLVED);
        complaint.setResolution(resolution);
        complaintDAO.update(complaint);
    }

    private void validateRequiredFields(Complaint complaint) {
        if (complaint.getCustomerId() == null) {
            throw new RuntimeException("Complaint customerId must not be null.");
        }
        requireNonBlank(complaint.getComplaintNumber(), "complaintNumber");
        requireNonBlank(complaint.getCategory(), "category");
        requireNonBlank(complaint.getDescription(), "description");
        requireNonBlank(complaint.getPriority(), "priority");
    }

    private void validateCategory(String category) {
        if (!SUPPORTED_CATEGORIES.contains(category)) {
            throw new RuntimeException("'" + category + "' is not a supported complaint category "
                    + "(expected one of " + SUPPORTED_CATEGORIES + ").");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Complaint " + fieldName + " must not be null or empty.");
        }
    }
}
