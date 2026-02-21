# T007: Acceptance Criteria

## Functional Acceptance Criteria

### Database Schema
- [ ] sales_channels_users table created with 3 columns (id, user_id, channel, created_at)
- [ ] login_logs table created with 8 columns (id, user_id, username, login_type, ip_address, user_agent, failed_reason, created_at)
- [ ] All PRIMARY KEY constraints applied
- [ ] All FOREIGN KEY constraints applied with correct cascade behavior
- [ ] UNIQUE constraint on (user_id, channel) in sales_channels_users
- [ ] ENUM constraint on login_type ('success', 'failed')

### Foreign Key Constraints
- [ ] sales_channels_users.user_id references users.id
- [ ] sales_channels_users FK has ON DELETE CASCADE
- [ ] login_logs.user_id references users.id
- [ ] login_logs FK has ON DELETE SET NULL

### Indexes
- [ ] Index on sales_channels_users.user_id
- [ ] Index on login_logs.user_id
- [ ] Index on login_logs.username
- [ ] Index on login_logs.created_at
- [ ] Index on login_logs.login_type
- [ ] Composite index on (user_id, created_at) in login_logs (optional but recommended)

### Data Types
- [ ] sales_channels_users.channel is VARCHAR(50)
- [ ] login_logs.id is BIGINT AUTO_INCREMENT
- [ ] login_logs.username is VARCHAR(50)
- [ ] login_logs.ip_address is VARCHAR(45)
- [ ] login_logs.user_agent is TEXT
- [ ] login_logs.failed_reason is VARCHAR(255)

## Non-Functional Criteria

### Data Integrity
- [ ] Cannot insert duplicate (user_id, channel) pair in sales_channels_users
- [ ] Cannot insert sales_channels_users with non-existent user_id
- [ ] Can insert login_logs with NULL user_id
- [ ] Deleting user cascades to sales_channels_users
- [ ] Deleting user sets login_logs.user_id to NULL but preserves log
- [ ] login_type only accepts 'success' or 'failed'

### Performance
- [ ] Migration completes in under 5 seconds
- [ ] Indexes created successfully
- [ ] No syntax errors

### IPv6 Support
- [ ] ip_address VARCHAR(45) can store IPv6 addresses
- [ ] ip_address can store IPv4 addresses

## How to Verify

### Run Migration
```bash
cd backend
./mvnw flyway:migrate
```

### Verify Tables Created
```sql
SHOW TABLES;
-- Should include: sales_channels_users, login_logs

DESCRIBE sales_channels_users;
DESCRIBE login_logs;
```

### Verify Constraints
```sql
SHOW CREATE TABLE sales_channels_users;
SHOW CREATE TABLE login_logs;

-- Check FK constraints
SELECT
  TABLE_NAME,
  COLUMN_NAME,
  CONSTRAINT_NAME,
  REFERENCED_TABLE_NAME,
  REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'your_db_name'
  AND TABLE_NAME IN ('sales_channels_users', 'login_logs')
  AND REFERENCED_TABLE_NAME IS NOT NULL;
```

### Verify Indexes
```sql
SHOW INDEX FROM sales_channels_users;
SHOW INDEX FROM login_logs;
```

### Test Foreign Key Constraints
```sql
-- Test sales_channels_users FK
INSERT INTO sales_channels_users (user_id, channel)
VALUES (999, 'PX/大全聯');
-- Should fail: user_id 999 doesn't exist

-- Insert valid record
INSERT INTO sales_channels_users (user_id, channel)
VALUES (1, 'PX/大全聯');

-- Test CASCADE delete
DELETE FROM users WHERE id=<test_user_id>;
-- Should also delete from sales_channels_users

-- Test login_logs SET NULL
INSERT INTO login_logs (user_id, username, login_type, ip_address)
VALUES (1, 'admin', 'success', '127.0.0.1');

DELETE FROM users WHERE id=1;
-- login_logs record should remain, but user_id should be NULL
SELECT user_id, username FROM login_logs WHERE username='admin';
```

### Test UNIQUE Constraint
```sql
-- Insert channel for user
INSERT INTO sales_channels_users (user_id, channel)
VALUES (1, 'PX/大全聯');

-- Try duplicate
INSERT INTO sales_channels_users (user_id, channel)
VALUES (1, 'PX/大全聯');
-- Should fail with UNIQUE constraint violation

-- Different channel for same user (should succeed)
INSERT INTO sales_channels_users (user_id, channel)
VALUES (1, '家樂福');
```

### Test ENUM Constraint
```sql
-- Valid login_type
INSERT INTO login_logs (username, login_type, ip_address)
VALUES ('test', 'success', '127.0.0.1');

-- Invalid login_type
INSERT INTO login_logs (username, login_type, ip_address)
VALUES ('test', 'invalid', '127.0.0.1');
-- Should fail with ENUM constraint violation
```

### Test IPv6 Support
```sql
-- IPv4 address
INSERT INTO login_logs (username, login_type, ip_address)
VALUES ('test', 'success', '192.168.1.1');

-- IPv6 address
INSERT INTO login_logs (username, login_type, ip_address)
VALUES ('test', 'success', '2001:0db8:85a3:0000:0000:8a2e:0370:7334');

-- Verify both stored correctly
SELECT ip_address FROM login_logs ORDER BY id DESC LIMIT 2;
```

### Verify Migration in Flyway History
```sql
SELECT * FROM flyway_schema_history WHERE version = '3';
-- Should show success
```
