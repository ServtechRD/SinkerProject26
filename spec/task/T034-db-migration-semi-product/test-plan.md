# T034: Test Plan

## Unit Tests
N/A - Database migrations are tested through integration tests

## Integration Tests
### FlywayMigrationTest (or similar existing test class)
- **test_V9_migration_creates_semi_product_table**
  - Start application with test database
  - Verify `semi_product_advance_purchase` table exists
  - Verify all columns are present with correct types

- **test_V9_migration_primary_key_constraint**
  - Insert row without ID
  - Verify ID is auto-generated
  - Attempt to insert row with duplicate ID
  - Verify constraint violation

- **test_V9_migration_unique_constraint_on_product_code**
  - Insert row with product_code "TEST001"
  - Attempt to insert another row with product_code "TEST001"
  - Verify unique constraint violation error

- **test_V9_migration_timestamp_defaults**
  - Insert row without created_at/updated_at
  - Verify created_at is set to current timestamp
  - Verify updated_at is set to current timestamp
  - Update the row
  - Verify updated_at changed, created_at unchanged

- **test_V9_migration_not_null_constraints**
  - Attempt to insert row with NULL product_code
  - Verify constraint violation
  - Attempt to insert row with NULL product_name
  - Verify constraint violation
  - Attempt to insert row with NULL advance_days
  - Verify constraint violation

## E2E Tests
N/A - Database schema migrations do not require E2E testing

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11 image
- Let Flyway run all migrations up to V9
- Test database should start empty for each test
- Use `@Sql` annotations to reset state between tests if needed

## Mocking Strategy
N/A - Integration tests run against real Testcontainers database
