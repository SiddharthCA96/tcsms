package com.amdocs.telecom.dao;

import com.amdocs.telecom.dao.impl.NotificationDAOImpl;
import com.amdocs.telecom.model.Notification;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manual integration test for NotificationDAO / NotificationDAOImpl against the real MySQL
 * database. The project does not use JUnit, so this follows the same plain main()/PASS-FAIL
 * convention as the other DAO integration tests (e.g. ComplaintDAOTest, PaymentDAOTest).
 *
 * database/seed_data.sql already contains 3 notifications (notification_id 1-3) for customers
 * 1-3, so findById/findByCustomerId/findByStatus are first exercised against that seeded data. A
 * temporary Notification is then created via save() - reusing seeded customer_id 1 - to exercise
 * save() and update(). NotificationDAO intentionally exposes no delete method, so the temporary
 * row is removed at the end with a direct, test-only JDBC statement - this cleanup logic is NOT
 * part of the DAO or its implementation.
 */
public class NotificationDAOTest {

    // Reused seeded values from database/seed_data.sql
    private static final Long SEEDED_NOTIFICATION_ID = 1L;
    private static final Long SEEDED_CUSTOMER_ID = 1L;
    private static final String SEEDED_NOTIFICATION_TYPE = "PAYMENT_SUCCESS";
    private static final String SEEDED_MESSAGE = "Payment received successfully.";
    private static final String SEEDED_STATUS = "SENT"; // notification_id 1 and 2

    // Temporary notification used to test save() and update()
    private static final String TEMP_NOTIFICATION_TYPE = "DAO_TEST_TYPE";
    private static final String TEMP_MESSAGE = "DAO_TEST temporary notification.";
    private static final String TEMP_STATUS = "PENDING";

    // Values used to exercise update()
    private static final String UPDATED_STATUS = "SENT";
    private static final LocalDateTime UPDATED_SENT_AT = LocalDateTime.of(2026, 9, 20, 9, 0, 0);

    public static void main(String[] args) {
        NotificationDAO notificationDAO = new NotificationDAOImpl();

        System.out.println("=== NotificationDAO Test ===");

        int testsExecuted = 0;
        int testsPassed = 0;
        Long tempNotificationId = null;

        try {
            // Test 1 - findById using an existing seeded notification
            testsExecuted++;
            Notification seededNotification = notificationDAO.findById(SEEDED_NOTIFICATION_ID);
            boolean test1Pass = seededNotification != null
                    && SEEDED_CUSTOMER_ID.equals(seededNotification.getCustomerId())
                    && SEEDED_NOTIFICATION_TYPE.equals(seededNotification.getNotificationType())
                    && SEEDED_MESSAGE.equals(seededNotification.getMessage())
                    && SEEDED_STATUS.equals(seededNotification.getStatus())
                    && seededNotification.getCreatedAt() != null
                    && seededNotification.getSentAt() != null;
            if (test1Pass) {
                testsPassed++;
            }
            System.out.println("Test 1 - findById (seeded notification): " + (test1Pass ? "PASS" : "FAIL"));
            if (seededNotification != null) {
                System.out.println("  notificationId: " + seededNotification.getNotificationId());
                System.out.println("  customerId: " + seededNotification.getCustomerId());
                System.out.println("  notificationType: " + seededNotification.getNotificationType());
                System.out.println("  message: " + seededNotification.getMessage());
                System.out.println("  status: " + seededNotification.getStatus());
                System.out.println("  createdAt: " + seededNotification.getCreatedAt());
                System.out.println("  sentAt: " + seededNotification.getSentAt());
            }

            // Test 2 - findByCustomerId using a seeded customer
            testsExecuted++;
            List<Notification> byCustomerId = notificationDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean test2Pass = byCustomerId != null
                    && byCustomerId.stream().anyMatch(n -> SEEDED_NOTIFICATION_ID.equals(n.getNotificationId()));
            if (test2Pass) {
                testsPassed++;
            }
            System.out.println("Test 2 - findByCustomerId (seeded): " + (test2Pass ? "PASS" : "FAIL"));
            System.out.println("  Notification count for customer " + SEEDED_CUSTOMER_ID + ": "
                    + (byCustomerId != null ? byCustomerId.size() : 0));

            // Test 3 - findByStatus using a known seeded status
            testsExecuted++;
            List<Notification> byStatus = notificationDAO.findByStatus(SEEDED_STATUS);
            boolean test3ListNotNull = byStatus != null;
            boolean test3AllMatch = test3ListNotNull
                    && byStatus.stream().allMatch(n -> SEEDED_STATUS.equals(n.getStatus()));
            boolean test3HasExpected = test3ListNotNull
                    && byStatus.stream().anyMatch(n -> SEEDED_NOTIFICATION_ID.equals(n.getNotificationId()));
            boolean test3Pass = test3ListNotNull && test3AllMatch && test3HasExpected;
            if (test3Pass) {
                testsPassed++;
            }
            System.out.println("Test 3 - findByStatus (seeded): " + (test3Pass ? "PASS" : "FAIL"));
            System.out.println("  Notifications with status '" + SEEDED_STATUS + "': "
                    + (test3ListNotNull ? byStatus.size() : 0));

            // Test 4 - save a temporary notification, reusing a seeded customer_id
            testsExecuted++;
            Notification tempNotification = new Notification(SEEDED_CUSTOMER_ID, TEMP_NOTIFICATION_TYPE,
                    TEMP_MESSAGE, TEMP_STATUS);

            notificationDAO.save(tempNotification);
            tempNotificationId = tempNotification.getNotificationId();
            final Long generatedNotificationId = tempNotificationId;

            Notification newlyCreated = tempNotificationId != null
                    ? notificationDAO.findById(tempNotificationId) : null;
            boolean test4Pass = tempNotificationId != null
                    && newlyCreated != null
                    && SEEDED_CUSTOMER_ID.equals(newlyCreated.getCustomerId())
                    && TEMP_NOTIFICATION_TYPE.equals(newlyCreated.getNotificationType())
                    && TEMP_MESSAGE.equals(newlyCreated.getMessage())
                    && TEMP_STATUS.equals(newlyCreated.getStatus())
                    && newlyCreated.getSentAt() == null
                    && newlyCreated.getCreatedAt() != null;
            if (test4Pass) {
                testsPassed++;
            }
            System.out.println("Test 4 - save (temporary notification) + findById verification: "
                    + (test4Pass ? "PASS" : "FAIL"));
            if (tempNotificationId != null) {
                System.out.println("  Generated notificationId: " + tempNotificationId);
            }

            // Test 5 - update the temporary notification's status and sentAt
            testsExecuted++;
            if (newlyCreated != null) {
                LocalDateTime originalCreatedAt = newlyCreated.getCreatedAt();

                newlyCreated.setStatus(UPDATED_STATUS);
                newlyCreated.setSentAt(UPDATED_SENT_AT);
                notificationDAO.update(newlyCreated);

                Notification afterUpdate = notificationDAO.findById(generatedNotificationId);
                boolean test5Pass = afterUpdate != null
                        && UPDATED_STATUS.equals(afterUpdate.getStatus())
                        && UPDATED_SENT_AT.equals(afterUpdate.getSentAt())
                        && originalCreatedAt.equals(afterUpdate.getCreatedAt())
                        && TEMP_NOTIFICATION_TYPE.equals(afterUpdate.getNotificationType())
                        && TEMP_MESSAGE.equals(afterUpdate.getMessage());
                if (test5Pass) {
                    testsPassed++;
                }
                System.out.println("Test 5 - update (status/sentAt changed, createdAt/type/message preserved): "
                        + (test5Pass ? "PASS" : "FAIL"));
            } else {
                System.out.println("Test 5 - update: FAIL (temporary notification was not created)");
            }

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup: NotificationDAO deliberately has no delete method, so the temporary row is
            // removed here with a plain JDBC statement, not through the DAO.
            if (tempNotificationId != null) {
                boolean cleaned = deleteTestNotificationRow(tempNotificationId);
                System.out.println("Cleanup (direct JDBC DELETE): " + (cleaned ? "PASS" : "FAIL"));

                NotificationDAO verifyDAO = new NotificationDAOImpl();
                boolean cleanupVerified = verifyDAO.findById(tempNotificationId) == null;
                System.out.println("Cleanup verification (no temporary notification remains): "
                        + (cleanupVerified ? "PASS" : "FAIL"));
            }

            System.out.println("=== Summary: " + testsPassed + "/" + testsExecuted + " tests passed ===");
        }
    }

    private static boolean deleteTestNotificationRow(Long notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, notificationId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for notification ID: " + notificationId + " - " + e.getMessage());
            return false;
        }
    }
}
