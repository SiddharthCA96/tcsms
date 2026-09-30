-- =========================================================
-- INDEXES FOR TELECOM MANAGEMENT SYSTEM
-- =========================================================

-- MOBILE SUBSCRIPTIONS
CREATE INDEX idx_subscription_customer
ON mobile_subscriptions(customer_id);

CREATE INDEX idx_subscription_plan
ON mobile_subscriptions(plan_id);

CREATE INDEX idx_subscription_sim
ON mobile_subscriptions(sim_id);


-- SUBSCRIPTION HISTORY
CREATE INDEX idx_history_subscription
ON subscription_history(subscription_id);

CREATE INDEX idx_history_change_date
ON subscription_history(change_date);


-- USAGE RECORDS
CREATE INDEX idx_usage_subscription
ON usage_records(subscription_id);

CREATE INDEX idx_usage_date
ON usage_records(usage_date);

CREATE INDEX idx_usage_type
ON usage_records(usage_type);


-- BILLS
CREATE INDEX idx_bill_subscription
ON bills(subscription_id);

CREATE INDEX idx_bill_month
ON bills(billing_month);

CREATE INDEX idx_bill_status
ON bills(bill_status);

CREATE INDEX idx_bill_due_date
ON bills(due_date);


-- PAYMENTS
CREATE INDEX idx_payment_bill
ON payments(bill_id);

CREATE INDEX idx_payment_customer
ON payments(customer_id);

CREATE INDEX idx_payment_date
ON payments(payment_date);


-- COMPLAINTS
CREATE INDEX idx_complaint_customer
ON complaints(customer_id);

CREATE INDEX idx_complaint_subscription
ON complaints(subscription_id);

CREATE INDEX idx_complaint_status
ON complaints(status);


-- NOTIFICATIONS
CREATE INDEX idx_notification_customer
ON notifications(customer_id);

CREATE INDEX idx_notification_status
ON notifications(status);


-- LOGIN HISTORY
CREATE INDEX idx_login_customer
ON login_history(customer_id);

CREATE INDEX idx_login_admin
ON login_history(admin_id);

CREATE INDEX idx_login_time
ON login_history(login_time);


-- PASSWORD RESET OTP
CREATE INDEX idx_otp_customer
ON password_reset_otps(customer_id);

CREATE INDEX idx_otp_expiry
ON password_reset_otps(expires_at);


-- AUDIT LOGS
CREATE INDEX idx_audit_actor
ON audit_logs(actor_type, actor_id);

CREATE INDEX idx_audit_entity
ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_audit_created
ON audit_logs(created_at);