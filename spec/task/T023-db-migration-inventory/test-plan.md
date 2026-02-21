# T023: Test Plan

## Unit Tests
Database migrations are typically not unit tested in isolation. Verification is done through integration tests and manual verification.

- N/A for Flyway migrations

## Integration Tests

### FlywayMigrationTest.java
- **testMigrationV6Executes**: Verify V6 migration executes without errors
- **testInventorySalesForecastTableExists**: Query information_schema to confirm table creation
- **testAllColumnsExist**: Verify all 18 columns are present with correct data types
- **testPrimaryKeyConstraint**: Verify id is primary key and auto-increments
- **testDefaultValues**: Insert row without optional values, verify defaults applied
- **testModifiedSubtotalNullable**: Verify modified_subtotal accepts NULL
- **testDecimalPrecision**: Insert values with 2 decimal places, verify no truncation
- **testTimestampBehavior**: Test created_at defaults and updated_at auto-updates
- **testCompositeIndexExists**: Query information_schema for month+product_code index
- **testVersionIndexExists**: Query information_schema for version index
- **testIndexPerformance**: Execute queries and verify index usage via EXPLAIN

### Setup
```java
@SpringBootTest
@Testcontainers
class InventorySalesForecastMigrationIT {
    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testTableStructure() {
        // Verify table exists and has correct structure
    }
}
```

## E2E Tests
- N/A (database migration only)

## Test Data Setup Notes

### Minimal Test Record
```sql
INSERT INTO inventory_sales_forecast
(month, product_code, product_name, category, spec, warehouse_location, version, query_start_date, query_end_date)
VALUES
('2026-01', 'PROD001', 'Product 1', 'Category A', 'Spec A', 'WH-A', 'v1', '2026-01-01', '2026-01-31');
```

### Full Test Record
```sql
INSERT INTO inventory_sales_forecast
(month, product_code, product_name, category, spec, warehouse_location,
 sales_quantity, inventory_balance, forecast_quantity, production_subtotal, modified_subtotal,
 version, query_start_date, query_end_date)
VALUES
('2026-02', 'PROD002', 'Product 2', 'Category B', 'Spec B', 'WH-B',
 100.50, 250.75, 500.00, 149.25, 200.00,
 'v2', '2026-02-01', '2026-02-28');
```

### Index Test Data
```sql
-- Insert multiple records for same month to test composite index
INSERT INTO inventory_sales_forecast
(month, product_code, product_name, category, spec, warehouse_location, version, query_start_date, query_end_date)
VALUES
('2026-03', 'PROD003', 'Product 3', 'Category C', 'Spec C', 'WH-C', 'v3', '2026-03-01', '2026-03-31'),
('2026-03', 'PROD004', 'Product 4', 'Category C', 'Spec C', 'WH-C', 'v3', '2026-03-01', '2026-03-31'),
('2026-03', 'PROD005', 'Product 5', 'Category C', 'Spec C', 'WH-C', 'v4', '2026-03-01', '2026-03-31');
```

## Mocking Strategy
- No mocking required for database migration tests
- Use Testcontainers with MariaDB 10.11 image for realistic testing
- Tests run against actual database to verify DDL correctness

## Performance Benchmarks
- Migration execution: < 5 seconds
- Index creation: < 2 seconds
- Query with composite index on 10,000 rows: < 100ms
- Query with version index on 10,000 rows: < 50ms
