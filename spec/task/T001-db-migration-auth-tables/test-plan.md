# T001: Test Plan

## Unit Tests
N/A - This task involves only SQL migration scripts. Testing is performed via integration tests.

## Integration Tests

### FlywayMigrationTest (Java/Spring Boot Test)
- **Test: migrations run successfully**
  - Start with clean database (Testcontainers MariaDB)
  - Run Flyway migrate
  - Assert migration completes without errors
  - Assert Flyway schema history shows V1 and V2 as SUCCESS

- **Test: users table structure**
  - Query INFORMATION_SCHEMA.COLUMNS for users table
  - Assert all 15 columns exist with correct data types
  - Assert username has UNIQUE constraint
  - Assert email has UNIQUE constraint

- **Test: roles table structure**
  - Query INFORMATION_SCHEMA.COLUMNS for roles table
  - Assert all 7 columns exist
  - Assert code has UNIQUE constraint

- **Test: permissions table structure**
  - Query INFORMATION_SCHEMA.COLUMNS for permissions table
  - Assert all 7 columns exist
  - Assert code has UNIQUE constraint

- **Test: role_permissions table structure**
  - Query INFORMATION_SCHEMA.COLUMNS
  - Assert foreign keys to roles and permissions exist
  - Assert UNIQUE constraint on (role_id, permission_id)

- **Test: seed data - roles**
  - Query roles table
  - Assert exactly 4 roles exist
  - Assert role codes: admin, sales, production_planner, procurement
  - Assert all have is_system=TRUE and is_active=TRUE

- **Test: seed data - permissions**
  - Query permissions table
  - Assert 29 permissions exist
  - Assert module groupings correct (user: 4, role: 4, etc.)
  - Group by module and assert counts

- **Test: seed data - admin user**
  - Query users table for username='admin'
  - Assert exists
  - Assert is_active=TRUE
  - Assert is_locked=FALSE
  - Assert hashed_password starts with '$2a$' or '$2b$'
  - Assert hashed_password length is 60

- **Test: seed data - admin role permissions**
  - Query role_permissions for admin role
  - Assert count equals total permission count (29)

- **Test: foreign key constraints**
  - Attempt to insert role_permission with invalid role_id
  - Assert SQLException thrown
  - Attempt to insert role_permission with invalid permission_id
  - Assert SQLException thrown

- **Test: unique constraints**
  - Attempt to insert duplicate username
  - Assert SQLException with constraint violation
  - Attempt to insert duplicate email
  - Assert SQLException with constraint violation
  - Attempt to insert duplicate role code
  - Assert SQLException with constraint violation

- **Test: cascade delete**
  - Create test role
  - Create role_permission mapping
  - Delete role
  - Assert role_permission entry also deleted

## E2E Tests
N/A - Database migrations are tested via integration tests.

## Test Data Setup

### Testcontainers Configuration
- Use MariaDB 10.11 Docker image
- Start fresh container for each test class
- Run Flyway migrations before tests
- Clean up after tests

### Test Database
- Database name: test_sinker
- Use test user with full privileges
- Ensure UTF8MB4 charset

## Mocking Strategy
No mocking required. Use actual MariaDB via Testcontainers for realistic testing.

## Additional Verification

### Manual Verification Steps
1. Run migrations on local MariaDB instance
2. Use MySQL Workbench or DBeaver to inspect schema
3. Verify all indexes created
4. Check execution plan for common queries (SELECT by username, email)

### SQL Test Scripts
Create test script to verify:
```sql
-- Test 1: Unique constraint on username
-- Test 2: Unique constraint on email
-- Test 3: FK constraint on users.role_id
-- Test 4: FK constraint on users.created_by
-- Test 5: Cascade delete on role_permissions
-- Test 6: Default values on users table
```

### Performance Tests
- Measure migration execution time (should be < 5 seconds)
- Verify indexes improve query performance (EXPLAIN queries)
