
-- 1. customers
-- 2. administrators
-- 3. sim_cards
-- 4. telecom_plans
-- 5. mobile_subscriptions
-- 6. subscription_history
-- 7. usage_records
-- 8. bills
-- 9. payments
-- 10. complaints
-- 11. notifications
-- 12. login_history
-- 13. audit_logs
-- 14. password_reset_otps

-- Database design
--       ↓
-- Create tables
--       ↓
-- Verify tables
--       ↓
-- Create constraints
--       ↓
-- Create indexes
--       ↓
-- Insert seed data
--       ↓
-- Test SQL queries
--       ↓
-- JDBC connection
--       ↓
-- Java model
--       ↓
-- DAO
--       ↓
-- Service
--       ↓
-- Controller
CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_number VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_status VARCHAR(30) NOT NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE administrators (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sim_cards (
    sim_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sim_number VARCHAR(30) NOT NULL UNIQUE,
    sim_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE telecom_plans (
    plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(30) NOT NULL UNIQUE,
    plan_name VARCHAR(100) NOT NULL,
    plan_type VARCHAR(50) NOT NULL,
    monthly_rental DECIMAL(12,2) NOT NULL,
    data_allowance_gb DECIMAL(10,2) NOT NULL,
    voice_minutes INT,
    sms_allowance INT,
    validity_days INT NOT NULL,
    international_roaming BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE mobile_subscriptions (
    subscription_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    sim_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    activation_date DATE NOT NULL,
    subscription_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscription_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_subscription_sim
        FOREIGN KEY (sim_id)
        REFERENCES sim_cards(sim_id),

    CONSTRAINT fk_subscription_plan
        FOREIGN KEY (plan_id)
        REFERENCES telecom_plans(plan_id)
);

CREATE TABLE subscription_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    old_plan_id BIGINT,
    new_plan_id BIGINT NOT NULL,
    change_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason VARCHAR(255),
    changed_by VARCHAR(100) NOT NULL,

    CONSTRAINT fk_history_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES mobile_subscriptions(subscription_id),

    CONSTRAINT fk_history_old_plan
        FOREIGN KEY (old_plan_id)
        REFERENCES telecom_plans(plan_id),

    CONSTRAINT fk_history_new_plan
        FOREIGN KEY (new_plan_id)
        REFERENCES telecom_plans(plan_id)
);

CREATE TABLE usage_records (
    usage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    usage_date TIMESTAMP NOT NULL,
    usage_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    charge DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usage_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES mobile_subscriptions(subscription_id)
);

CREATE TABLE bills (
    bill_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_number VARCHAR(50) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    billing_month DATE NOT NULL,
    plan_rental DECIMAL(12,2) NOT NULL,
    usage_charges DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    due_date DATE NOT NULL,
    bill_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bill_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES mobile_subscriptions(subscription_id),

    CONSTRAINT uq_subscription_billing_month
        UNIQUE (subscription_id, billing_month)
);

CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    bill_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(bill_id),

    CONSTRAINT fk_payment_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
);

CREATE TABLE complaints (
    complaint_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    complaint_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    subscription_id BIGINT,
    category VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    resolution TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_complaint_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_complaint_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES mobile_subscriptions(subscription_id)
);

CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,

    CONSTRAINT fk_notification_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
);

CREATE TABLE login_history (
    login_history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    admin_id BIGINT,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    ip_address VARCHAR(45),
    login_status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),

    CONSTRAINT fk_login_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_login_admin
        FOREIGN KEY (admin_id)
        REFERENCES administrators(admin_id)
);

CREATE TABLE audit_logs (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_type VARCHAR(30) NOT NULL,
    actor_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE password_reset_otps (
    otp_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_otp_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
);

