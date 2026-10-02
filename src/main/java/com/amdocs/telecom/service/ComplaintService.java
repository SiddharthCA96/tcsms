package com.amdocs.telecom.service;

import com.amdocs.telecom.model.Complaint;

import java.util.List;
import java.util.Optional;

/**
 * Business-level contract for complaint management.
 *
 * Status transitions (resolving a complaint) are handled by resolveComplaint(), not by
 * updateComplaint() - the same "protect the lifecycle field" pattern used elsewhere in this
 * project (e.g. CustomerService protects accountStatus from a generic profile update).
 */
public interface ComplaintService {

    /**
     * Supported complaint categories, per the case study. Not an enum because the schema stores
     * category as a plain VARCHAR with no CHECK constraint - validated here instead.
     */
    List<String> SUPPORTED_CATEGORIES = java.util.Collections.unmodifiableList(java.util.Arrays.asList(
            "BILLING", "NETWORK", "SIM", "PLAN", "PAYMENT", "OTHER"));

    Complaint createComplaint(Complaint complaint);

    Complaint getComplaintById(Long complaintId);

    Optional<Complaint> findComplaintByNumber(String complaintNumber);

    List<Complaint> getCustomerComplaints(Long customerId);

    List<Complaint> getSubscriptionComplaints(Long subscriptionId);

    List<Complaint> getComplaintsByStatus(String status);

    List<Complaint> getComplaintsByPriority(String priority);

    /**
     * Updates category/description/priority. Does not change status or resolution.
     */
    void updateComplaint(Complaint complaint);

    void resolveComplaint(Long complaintId, String resolution);
}
