# T007: Test Plan

## Unit Tests
N/A - This task involves only SQL migration scripts. Testing is performed via integration tests.

## Integration Tests

### FlywayMigrationTest (Java/Spring Boot Test)
- **Test: V3 migration runs successfully**
  - Start with clean database with V1 and V2 applied
  - Run Flyway migrate to apply V3
  - Assert migration completes without errors
  - Assert Flyway schema history shows V3 as SUCCESS

- **Test: sales_channels_users table structure**
  - Query INFORMATION_SCHEMA.COLUMNS for sales_channels_users
  - Assert 4 columns exist: id, user_id, channel, created_at
  - Assert id is PRIMARY KEY
  - Assert user_id is INT
  - Assert channel is VARCHAR(50)
  - Assert created_at is TIMESTAMP

- **Test: login_logs table structure**
  - Query INFORMATION_SCHEMA.COLUMNS for login_logs
  - Assert 8 columns exist
  - Assert id is BIGINT
  - Assert username is VARCHAR(50)
  - Assert login_type is ENUM
  - Assert ip_address is VARCHAR(45)
  - Assert user_agent is TEXT
  - Assert failed_reason is VARCHAR(255)

- **Test: sales_channels_users foreign key**
  - Query INFORMATION_SCHEMA.KEY_COLUMN_USAGE
  - Assert FK on user_id references users.id
  - Query INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
  - Assert DELETE_RULE is CASCADE

- **Test: login_logs foreign key**
  - Query INFORMATION_SCHEMA.KEY_COLUMN_USAGE
  - Assert FK on user_id references users.id
  - Query INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
  - Assert DELETE_RULE is SET NULL

- **Test: sales_channels_users unique constraint**
  - Insert record (user_id=1, channel='PX/大全聯')
  - Attempt duplicate insert
  - Assert SQLException with constraint violation
  - Insert record (user_id=1, channel='家樂福')
  - Assert succeeds (different channel)

- **Test: login_logs ENUM constraint**
  - Insert with login_type='success'
  - Assert succeeds
  - Insert with login_type='failed'
  - Assert succeeds
  - Attempt insert with login_type='invalid'
  - Assert SQLException

- **Test: sales_channels_users cascade delete**
  - Create test user
  - Insert sales_channels_users record for user
  - Delete user
  - Assert sales_channels_users record also deleted

- **Test: login_logs SET NULL on user delete**
  - Create test user
  - Insert login_logs record for user
  - Delete user
  - Assert login_logs record still exists
  - Assert user_id is NULL
  - Assert username still preserved

- **Test: indexes exist**
  - Query INFORMATION_SCHEMA.STATISTICS
  - Assert index on sales_channels_users.user_id
  - Assert index on login_logs.user_id
  - Assert index on login_logs.username
  - Assert index on login_logs.created_at
  - Assert index on login_logs.login_type

- **Test: IPv6 address storage**
  - Insert login_logs with IPv4 address
  - Insert login_logs with IPv6 address (max length 45 chars)
  - Query and verify both stored correctly
  - Verify no truncation

## E2E Tests
N/A - Database migrations are tested via integration tests.

## Test Data Setup

### Testcontainers Configuration
- Use MariaDB 10.11 Docker image
- Run V1, V2 migrations before V3
- Clean database before tests

### Test Users
- Use admin user from V2 seed data
- Create additional test users for FK testing

## Mocking Strategy
No mocking required. Use actual MariaDB via Testcontainers for realistic testing.

## Additional Verification

### Manual Verification Steps
1. Run migrations on local MariaDB instance
2. Use MySQL Workbench or DBeaver to inspect schema
3. Verify all indexes created
4. Test cascade behavior manually

### SQL Test Scripts
Create test script to verify:
```sql
-- Test 1: FK constraint on sales_channels_users
INSERT INTO sales_channels_users (user_id, channel) VALUES (999, 'test');
-- Should fail

-- Test 2: UNIQUE constraint
INSERT INTO sales_channels_users (user_id, channel) VALUES (1, 'PX/大全聯');
INSERT INTO sales_channels_users (user_id, channel) VALUES (1, 'PX/大全聯');
-- Second should fail

-- Test 3: CASCADE delete
INSERT INTO users (...) VALUES (...);
INSERT INTO sales_channels_users (user_id, channel) VALUES (new_user_id, 'test');
DELETE FROM users WHERE id=new_user_id;
SELECT * FROM sales_channels_users WHERE user_id=new_user_id;
-- Should return empty

-- Test 4: SET NULL on login_logs
INSERT INTO login_logs (user_id, username, login_type, ip_address)
VALUES (new_user_id, 'test', 'success', '127.0.0.1');
DELETE FROM users WHERE id=new_user_id;
SELECT user_id FROM login_logs WHERE username='test';
-- Should be NULL

-- Test 5: ENUM constraint
INSERT INTO login_logs (username, login_type, ip_address)
VALUES ('test', 'invalid_type', '127.0.0.1');
-- Should fail
```

### Performance Tests
- Measure migration execution time (should be < 5 seconds)
- Verify indexes improve query performance (EXPLAIN queries)
- Test insert performance for login_logs (high volume table)

### Data Integrity Tests
- Verify cascade behavior doesn't orphan records
- Verify SET NULL preserves audit trail
- Verify UNIQUE constraint prevents duplicates
- Verify ENUM constraint prevents invalid values
