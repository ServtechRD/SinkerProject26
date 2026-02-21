# T040: Test Plan

## Unit Tests
N/A - Database migrations are tested through integration tests

## Integration Tests

### FlywayMigrationTest (or similar existing test class)
- **test_V11_migration_creates_material_purchase_table**
  - Start application with test database
  - Verify `material_purchase` table exists
  - Verify all columns present with correct types

- **test_V11_migration_primary_key_constraint**
  - Insert row without ID
  - Verify ID is auto-generated

- **test_V11_migration_composite_index_exists**
  - Run SHOW INDEX query
  - Verify composite index on (week_start, factory)

- **test_V11_migration_product_code_index_exists**
  - Run SHOW INDEX query
  - Verify index on product_code

- **test_V11_migration_decimal_precision**
  - Insert row with decimal values (e.g., 123.45)
  - Query and verify precision preserved
  - Insert value with more than 2 decimal places
  - Verify rounding to 2 decimal places

- **test_V11_migration_default_values**
  - Insert row with only required fields
  - Verify kg_per_box, basket_quantity, boxes_per_barrel, required_barrels default to 0
  - Verify is_erp_triggered defaults to FALSE
  - Verify erp_order_no defaults to NULL

- **test_V11_migration_boolean_storage**
  - Insert row with is_erp_triggered = TRUE
  - Query and verify value is TRUE (1)
  - Insert row with is_erp_triggered = FALSE
  - Query and verify value is FALSE (0)

- **test_V11_migration_nullable_erp_order_no**
  - Insert row with NULL erp_order_no
  - Verify accepted
  - Insert row with erp_order_no = "ERP-12345"
  - Verify stored correctly

- **test_V11_migration_not_null_constraints**
  - Attempt to insert row with NULL week_start
  - Verify constraint violation
  - Attempt to insert row with NULL factory
  - Verify constraint violation
  - Attempt to insert row with NULL product_code
  - Verify constraint violation
  - Attempt to insert row with NULL quantity
  - Verify constraint violation

- **test_V11_migration_timestamp_defaults**
  - Insert row without created_at/updated_at
  - Verify created_at set to current timestamp
  - Verify updated_at set to current timestamp
  - Update the row
  - Verify updated_at changed, created_at unchanged

- **test_V11_migration_composite_index_performance**
  - Insert 10,000 rows with various week_start and factory values
  - Query with WHERE week_start = ? AND factory = ?
  - Verify query uses composite index (EXPLAIN query)
  - Verify query completes in under 100ms

## E2E Tests
N/A - Database schema migrations do not require E2E testing

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11 image
- Let Flyway run all migrations up to V11
- Test database should start empty for each test
- Sample test data:
  ```
  week_start: 2026-02-17
  factory: "F1"
  product_code: "P001"
  product_name: "產品A"
  quantity: 1000.00
  semi_product_name: "半成品A"
  semi_product_code: "SP001"
  kg_per_box: 5.50
  basket_quantity: 5500.00
  boxes_per_barrel: 20.00
  required_barrels: 275.00
  is_erp_triggered: FALSE
  erp_order_no: NULL
  ```
- Include test cases with is_erp_triggered = TRUE and erp_order_no set
- Use @Sql annotations to reset state between tests if needed

## Mocking Strategy
N/A - Integration tests run against real Testcontainers database
