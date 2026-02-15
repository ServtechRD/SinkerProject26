# T011: Test Plan

## Unit Tests
N/A - Database migration scripts are validated through integration tests and manual verification.

## Integration Tests
**Test Class**: `SalesForecastConfigMigrationTest` (Spring Boot Test + Testcontainers)

### Test Cases
1. **testMigrationV4ExecutesSuccessfully**
   - Start Testcontainer with MariaDB 10.11
   - Run Flyway migrations up to V4
   - Assert migration status is SUCCESS
   - Assert no errors in Flyway history

2. **testSalesForecastConfigTableExists**
   - Execute migration
   - Query information_schema to verify table exists
   - Assert table name is `sales_forecast_config`

3. **testTableSchemaIsCorrect**
   - Execute migration
   - Query DESCRIBE sales_forecast_config
   - Assert all columns exist with correct types:
     - id: int, PK, auto_increment
     - month: varchar(7), unique, not null
     - auto_close_day: int, default 10, not null
     - is_closed: tinyint(1), default 0, not null
     - closed_at: timestamp, nullable
     - created_at: timestamp, default current_timestamp
     - updated_at: timestamp, on update current_timestamp

4. **testUniqueConstraintOnMonth**
   - Insert record with month='202501'
   - Attempt to insert duplicate month='202501'
   - Assert SQLException with duplicate key error

5. **testDefaultValues**
   - Insert minimal record: `INSERT INTO sales_forecast_config (month) VALUES ('202503')`
   - Select the record
   - Assert auto_close_day = 10
   - Assert is_closed = FALSE
   - Assert created_at is populated
   - Assert updated_at is populated
   - Assert closed_at is NULL

6. **testAutoUpdateTimestamp**
   - Insert record with month='202504'
   - Sleep 1 second
   - Update is_closed to TRUE
   - Select the record
   - Assert updated_at is greater than created_at

7. **testClosedAtAcceptsNull**
   - Insert record with closed_at=NULL
   - Assert insertion succeeds
   - Update closed_at to CURRENT_TIMESTAMP
   - Assert update succeeds
   - Update closed_at back to NULL
   - Assert update succeeds

8. **testAutoCloseDayRange**
   - Insert records with auto_close_day values: 1, 10, 31
   - Assert all insertions succeed
   - Insert record with auto_close_day = 0 (if CHECK constraint exists, should fail)
   - Insert record with auto_close_day = 32 (if CHECK constraint exists, should fail)

## E2E Tests
N/A - Database migration testing is covered by integration tests with Testcontainers. E2E tests for this functionality will be part of T012 and T013 when the API and UI are implemented.

## Test Data Setup
- Use Testcontainers with MariaDB 10.11 image
- Start with empty database or migrations V1-V3 applied
- Create test data inline within each test method
- Clean up after each test (Testcontainers handles container lifecycle)

## Mocking Strategy
- No mocking required - use real MariaDB via Testcontainers
- Flyway runs against real database container
- All SQL operations execute against real database for accurate testing
- Each test runs in transaction with rollback (if using @Transactional) or uses fresh container
