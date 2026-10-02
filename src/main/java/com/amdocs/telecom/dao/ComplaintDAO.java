package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Complaint;

import java.util.List;

public interface ComplaintDAO {

    Complaint findById(Long complaintId);

    Complaint findByComplaintNumber(String complaintNumber);

    List<Complaint> findByCustomerId(Long customerId);

    List<Complaint> findBySubscriptionId(Long subscriptionId);

    List<Complaint> findByStatus(String status);

    List<Complaint> findByPriority(String priority);

    void save(Complaint complaint);

    void update(Complaint complaint);
}
