package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Notification;

import java.util.List;

/**
 * DAO contract for persisting and retrieving notifications.
 */
public interface NotificationDAO {

    Notification findById(Long notificationId);

    List<Notification> findByCustomerId(Long customerId);

    List<Notification> findByStatus(String status);

    void save(Notification notification);

    void update(Notification notification);
}
