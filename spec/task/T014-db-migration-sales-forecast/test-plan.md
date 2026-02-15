# T014: Test Plan

## Unit Tests
N/A - Database migration scripts are validated through integration tests with real database.

## Integration Tests

### SalesForecastMigrationTest (Spring Boot Test + Testcontainers)

1. **testMigrationV5ExecutesSuccessfully**
   - Start Testcontainer with MariaDB 10.11
   - Run Flyway migrations up to V5
   - Assert migration status is SUCCESS
   - Assert no errors in Flyway history

2. **testSalesForecastTableExists**
   - Execute migration
   - Query information_schema.tables
   - Assert table 'sales_forecast' exists

3. **testTableSchemaCorrect**
   - Execute migration
   - Query DESCRIBE sales_forecast
   - Assert all columns exist with correct types and constraints:
     - id: int, PK, auto_increment
     - month: varchar(7), not null
     - channel: varchar(50), not null
     - category: varchar(100), nullable
     - spec: varchar(200), nullable
     - product_code: varchar(50), not null
     - product_name: varchar(200), nullable
     - warehouse_location: varchar(50), nullable
     - quantity: decimal(10,2), not null
     - version: varchar(100), not null
     - is_modified: tinyint(1), default 0, not null
     - created_at: timestamp, default CURRENT_TIMESTAMP
     - updated_at: timestamp, on update CURRENT_TIMESTAMP

4. **testAllIndexesCreated**
   - Execute migration
   - Query SHOW INDEX FROM sales_forecast
   - Assert indexes exist:
     - PRIMARY on (id)
     - idx_month_channel on (month, channel)
     - idx_product_code on (product_code)
     - idx_version on (version)
     - idx_month_channel_product on (month, channel, product_code)

5. **testCharsetIsUtf8mb4**
   - Execute migration
   - Query SHOW CREATE TABLE sales_forecast
   - Assert CHARSET=utf8mb4
   - Assert COLLATE=utf8mb4_unicode_ci

6. **testInsertWithChineseCharacters**
   - Execute migration
   - Insert record with Chinese characters in category, product_name, warehouse_location, version
   - Select the record
   - Assert Chinese characters stored and retrieved correctly

7. **testDecimalPrecision**
   - Insert record with quantity = 12345678.99
   - Select the record
   - Assert quantity = 12345678.99 (exact match)
   - Insert record with quantity = 0.01
   - Assert quantity = 0.01

8. **testDefaultValues**
   - Insert minimal record (only required fields)
   - Select the record
   - Assert is_modified = FALSE (0)
   - Assert created_at is populated
   - Assert updated_at equals created_at

9. **testAutoUpdateTimestamp**
   - Insert record
   - Sleep 1 second
   - Update quantity
   - Select the record
   - Assert updated_at > created_at

10. **testIndexPerformance_MonthChannel**
    - Insert 1000 test records with various month+channel combinations
    - Run EXPLAIN SELECT * WHERE month='202601' AND channel='大全聯'
    - Assert query uses idx_month_channel (check EXPLAIN output)

11. **testIndexPerformance_ProductCode**
    - Insert 1000 test records
    - Run EXPLAIN SELECT * WHERE product_code='P001'
    - Assert query uses idx_product_code

12. **testIndexPerformance_Version**
    - Insert records with multiple versions
    - Run EXPLAIN SELECT * WHERE version='2026/01/15 10:00:00(大全聯)'
    - Assert query uses idx_version

13. **testCompositeIndexForDuplicateDetection**
    - Run EXPLAIN SELECT * WHERE month='202601' AND channel='大全聯' AND product_code='P001'
    - Assert query uses idx_month_channel_product

14. **testMultipleVersionsSameProduct**
    - Insert same product with different versions
    - Assert all inserts succeed (no unique constraint preventing this)
    - Query by month+channel+product_code
    - Assert multiple versions returned

15. **testIsModifiedFlag**
    - Insert record with is_modified=FALSE
    - Update is_modified to TRUE
    - Select the record
    - Assert is_modified = TRUE (1)

16. **testNullableColumns**
    - Insert record with NULL values for category, spec, product_name, warehouse_location
    - Assert insert succeeds
    - Select the record
    - Assert nullable columns are NULL

17. **testRequiredColumns**
    - Attempt to insert without month
    - Assert SQLException (column cannot be null)
    - Attempt to insert without channel
    - Assert SQLException
    - Attempt to insert without product_code
    - Assert SQLException
    - Attempt to insert without quantity
    - Assert SQLException
    - Attempt to insert without version
    - Assert SQLException

## E2E Tests
N/A - Database migration testing is fully covered by integration tests with Testcontainers. E2E tests for forecast data will be implemented in subsequent tasks (T015-T022).

## Test Data Setup
- Use Testcontainers with MariaDB 10.11 image
- Run migrations V1-V4 before testing V5
- Create test data inline within each test method
- Use @BeforeEach to ensure clean state
- Sample test data:
  ```java
  // Sample forecast entry
  month: "202601"
  channel: "大全聯"
  category: "飲料類"
  spec: "600ml*24入"
  product_code: "P001"
  product_name: "可口可樂"
  warehouse_location: "A01"
  quantity: 100.50
  version: "2026/01/15 10:30:00(大全聯)"
  is_modified: false
  ```

## Mocking Strategy
- No mocking - use real MariaDB via Testcontainers
- All SQL operations execute against real database
- Flyway runs against real database container
- Use JDBC template or JPA for test queries
- Each test should be independent (use @Transactional rollback or recreate container)
