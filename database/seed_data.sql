INSERT INTO customers (
    customer_number,
    first_name,
    last_name,
    date_of_birth,
    email,
    mobile_number,
    address,
    city,
    country,
    username,
    password_hash,
    account_status
) VALUES
(
    'CUST100245',
    'Arjun',
    'Mehta',
    '1995-05-15',
    'arjun@example.com',
    '9000000001',
    'Andheri East',
    'Mumbai',
    'India',
    'arjun.mehta',
    'DEV_HASH_001',
    'ACTIVE'
),
(
    'CUST100378',
    'Sarah',
    'Wilson',
    '1992-08-20',
    'sarah@example.com',
    '9000000002',
    'Westminster',
    'London',
    'UK',
    'sarah.wilson',
    'DEV_HASH_002',
    'ACTIVE'
),
(
    'CUST100412',
    'Omar',
    'Hassan',
    '1990-11-10',
    'omar@example.com',
    '9000000003',
    'Al Olaya',
    'Riyadh',
    'Saudi Arabia',
    'omar.hassan',
    'DEV_HASH_003',
    'ACTIVE'
);

INSERT INTO administrators (
    username,
    password_hash,
    first_name,
    last_name,
    email,
    status
) VALUES (
    'admin',
    'DEV_ADMIN_HASH',
    'System',
    'Administrator',
    'admin@example.com',
    'ACTIVE'
);

INSERT INTO telecom_plans (
    plan_code,
    plan_name,
    plan_type,
    monthly_rental,
    data_allowance_gb,
    voice_minutes,
    sms_allowance,
    validity_days,
    international_roaming,
    status
) VALUES
(
    'PLAN-101',
    '5G Premium',
    '5G',
    999.00,
    100.00,
    NULL,
    3000,
    30,
    TRUE,
    'ACTIVE'
),
(
    'PLAN-102',
    '5G Standard',
    '5G',
    699.00,
    50.00,
    1500,
    2000,
    30,
    FALSE,
    'ACTIVE'
),
(
    'PLAN-103',
    'Business Pro',
    'BUSINESS',
    1499.00,
    200.00,
    NULL,
    5000,
    30,
    TRUE,
    'ACTIVE'
),
(
    'PLAN-104',
    'Basic',
    'BASIC',
    399.00,
    20.00,
    500,
    1000,
    30,
    FALSE,
    'ACTIVE'
);
INSERT INTO sim_cards (
    sim_number,
    sim_type,
    status
) VALUES
(
    '899110000000001',
    'PHYSICAL_SIM',
    'ACTIVE'
),
(
    '899110000000002',
    'ESIM',
    'ACTIVE'
),
(
    '899110000000003',
    'ESIM',
    'ACTIVE'
),
(
    '899110000000004',
    'PHYSICAL_SIM',
    'ACTIVE'
);

INSERT INTO mobile_subscriptions (
    subscription_number,
    customer_id,
    mobile_number,
    sim_id,
    plan_id,
    activation_date,
    subscription_type,
    status
) VALUES
(
    'SUB-100001',
    1,
    '+919000000001',
    1,
    1,
    '2026-08-01',
    'POSTPAID',
    'ACTIVE'
),
(
    'SUB-100002',
    1,
    '+919000000002',
    2,
    2,
    '2026-08-05',
    'PREPAID',
    'ACTIVE'
),
(
    'SUB-100003',
    2,
    '+447000000003',
    3,
    3,
    '2026-08-10',
    'POSTPAID',
    'ACTIVE'
),
(
    'SUB-100004',
    3,
    '+966500000004',
    4,
    4,
    '2026-08-15',
    'PREPAID',
    'ACTIVE'
);

INSERT INTO usage_records (
    subscription_id,
    usage_date,
    usage_type,
    quantity,
    unit,
    charge
) VALUES
(1, '2026-08-05 10:00:00', 'DATA',    2.500, 'GB',      0.00),
(1, '2026-08-08 14:30:00', 'VOICE', 120.000, 'MINUTES', 0.00),
(1, '2026-08-10 09:15:00', 'SMS',    45.000, 'SMS',     0.00),
(1, '2026-08-12 18:00:00', 'ROAMING',0.350, 'GB',     100.00),

(2, '2026-08-05 11:00:00', 'DATA',    5.000, 'GB',      0.00),
(2, '2026-08-12 15:00:00', 'VOICE', 200.000, 'MINUTES', 0.00),
(2, '2026-08-20 16:00:00', 'SMS',   100.000, 'SMS',     0.00),

(3, '2026-08-06 12:00:00', 'DATA',   15.000, 'GB',      0.00),
(3, '2026-08-15 13:00:00', 'VOICE', 500.000, 'MINUTES', 0.00),
(3, '2026-08-20 17:00:00', 'SMS',   300.000, 'SMS',     0.00),

(4, '2026-08-07 09:00:00', 'DATA',    8.000, 'GB',      0.00),
(4, '2026-08-18 19:00:00', 'ROAMING',0.350, 'GB',      75.00);

INSERT INTO bills (
    bill_number,
    subscription_id,
    billing_month,
    plan_rental,
    usage_charges,
    tax_amount,
    discount,
    total_amount,
    due_date,
    bill_status
) VALUES
(
    'INV-2026-08-10001',
    1,
    '2026-08-01',
    999.00,
    100.00,
    197.82,
    50.00,
    1246.82,
    '2026-08-20',
    'PAID'
),
(
    'INV-2026-08-10002',
    2,
    '2026-08-01',
    699.00,
    0.00,
    125.82,
    0.00,
    824.82,
    '2026-08-20',
    'UNPAID'
),
(
    'INV-2026-08-10003',
    3,
    '2026-08-01',
    1499.00,
    0.00,
    269.82,
    100.00,
    1668.82,
    '2026-08-20',
    'PAID'
),
(
    'INV-2026-08-10004',
    4,
    '2026-08-01',
    399.00,
    75.00,
    85.32,
    0.00,
    559.32,
    '2026-08-20',
    'OVERDUE'
);

INSERT INTO payments (
    transaction_reference,
    bill_id,
    customer_id,
    amount,
    payment_mode,
    payment_date,
    payment_status
) VALUES
(
    'TXN-202608-0001',
    1,
    1,
    1246.82,
    'UPI',
    '2026-08-15 10:30:00',
    'SUCCESS'
),
(
    'TXN-202608-0002',
    3,
    2,
    1668.82,
    'CARD',
    '2026-08-16 12:15:00',
    'SUCCESS'
);

INSERT INTO complaints (
    complaint_number,
    customer_id,
    subscription_id,
    category,
    description,
    priority,
    status,
    resolution
) VALUES
(
    'COMP-10001',
    1,
    1,
    'NETWORK',
    'Network connectivity issue in Mumbai.',
    'HIGH',
    'OPEN',
    NULL
),
(
    'COMP-10002',
    2,
    3,
    'BILLING',
    'Customer has questions regarding monthly bill.',
    'MEDIUM',
    'RESOLVED',
    'Billing details explained to customer.'
),
(
    'COMP-10003',
    3,
    4,
    'PAYMENT',
    'Payment confirmation not received.',
    'HIGH',
    'OPEN',
    NULL
);

INSERT INTO notifications (
    customer_id,
    notification_type,
    message,
    status,
    sent_at
) VALUES
(
    1,
    'PAYMENT_SUCCESS',
    'Payment received successfully.',
    'SENT',
    '2026-08-15 10:31:00'
),
(
    2,
    'BILL_GENERATED',
    'Your monthly bill has been generated.',
    'SENT',
    '2026-08-01 09:00:00'
),
(
    3,
    'BILL_OVERDUE',
    'Your bill is overdue.',
    'PENDING',
    NULL
);

