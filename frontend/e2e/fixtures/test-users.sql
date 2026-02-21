-- E2E test users (password for all: "password")
-- bcrypt hash: $2b$10$rrtnWZRUiq1XBX9hyM3tIOeugiLEbg04K7nPZUjgdoIhyCD8HmbiS

-- Clean up previous test users (idempotent)
DELETE FROM users WHERE username IN ('locked_user', 'inactive_user');

-- Locked user (active but locked)
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
VALUES ('locked_user', 'locked@sinker.local',
        '$2b$10$rrtnWZRUiq1XBX9hyM3tIOeugiLEbg04K7nPZUjgdoIhyCD8HmbiS',
        'Locked User',
        (SELECT id FROM roles WHERE code = 'admin'),
        TRUE, TRUE, 5);

-- Inactive user (not active)
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count)
VALUES ('inactive_user', 'inactive@sinker.local',
        '$2b$10$rrtnWZRUiq1XBX9hyM3tIOeugiLEbg04K7nPZUjgdoIhyCD8HmbiS',
        'Inactive User',
        (SELECT id FROM roles WHERE code = 'admin'),
        FALSE, FALSE, 0);
