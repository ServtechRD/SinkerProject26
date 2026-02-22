# T037: Test Plan

## Unit Tests
N/A - Database migrations are tested through integration tests

## Integration Tests

### FlywayMigrationTest (or similar existing test class)
- **test_V10_migration_creates_material_demand_table**
  - Start application with test database
  - Verify `material_demand` table exists
  - Verify all columns present with correct types

- **test_V10_migration_primary_key_constraint**
  - Insert row without ID
  - Verify ID is auto-generated
  - Attempt to insert row with duplicate ID
  - Verify constraint violation

- **test_V10_migration_composite_index_exists**
  - Run SHOW INDEX query
  - Verify composite index on (week_start, factory)

- **test_V10_migration_material_code_index_exists**
  - Run SHOW INDEX query
  - Verify index on material_code

- **test_V10_migration_decimal_precision**
  - Insert row with decimal values (e.g., 123.45)
  - Query and verify precision preserved
  - Insert value with more than 2 decimal places
  - Verify rounding to 2 decimal places

- **test_V10_migration_default_values**
  - Insert row without specifying quantity columns
  - Verify expected_delivery, demand_quantity, estimated_inventory default to 0

- **test_V10_migration_nullable_last_purchase_date**
  - Insert row with NULL last_purchase_date
  - Verify accepted
  - Insert row without last_purchase_date
  - Verify defaults to NULL

- **test_V10_migration_not_null_constraints**
  - Attempt to insert row with NULL week_start
  - Verify constraint violation
  - Attempt to insert row with NULL factory
  - Verify constraint violation
  - Attempt to insert row with NULL material_code
  - Verify constraint violation
  - Attempt to insert row with NULL demand_date
  - Verify constraint violation

- **test_V10_migration_timestamp_defaults**
  - Insert row without created_at/updated_at
  - Verify created_at set to current timestamp
  - Verify updated_at set to current timestamp
  - Update the row
  - Verify updated_at changed, created_at unchanged

- **test_V10_migration_composite_index_performance**
  - Insert 10,000 rows with various week_start and factory values
  - Query with WHERE week_start = ? AND factory = ?
  - Verify query uses composite index (EXPLAIN query)
  - Verify query completes in under 100ms

## E2E Tests
N/A - Database schema migrations do not require E2E testing

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11 image
- Let Flyway run all migrations up to V10
- Test database should start empty for each test
- Sample test data:
  - week_start: 2026-02-17, factory: "F1", material_code: "M001", material_name: "原料A"
  - week_start: 2026-02-17, factory: "F2", material_code: "M002", material_name: "原料B"
  - Decimal quantities: 123.45, 0.50, 9999.99
- Use @Sql annotations to reset state between tests if needed

## Mocking Strategy
N/A - Integration tests run against real Testcontainers database
