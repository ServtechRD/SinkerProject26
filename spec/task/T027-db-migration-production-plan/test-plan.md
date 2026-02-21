# T027: Test Plan

## Unit Tests
Database migrations are typically not unit tested in isolation. Verification is done through integration tests and manual verification.

- N/A for Flyway migrations

## Integration Tests

### FlywayMigrationV7Test.java
- **testMigrationV7Executes**: Verify V7 migration executes without errors
- **testProductionPlanTableExists**: Query information_schema to confirm table creation
- **testAllColumnsExist**: Verify all 15 columns are present with correct data types
- **testPrimaryKeyConstraint**: Verify id is primary key and auto-increments
- **testDefaultValues**: Insert row without optional values, verify defaults applied
- **testJsonColumnDefault**: Verify monthly_allocation defaults to empty JSON object '{}'
- **testDecimalPrecision**: Insert values with 2 decimal places, verify no truncation
- **testTextColumn**: Insert long remarks (1000+ chars), verify no truncation
- **testTimestampBehavior**: Test created_at defaults and updated_at auto-updates
- **testCompositeIndexExists**: Query information_schema for year+product_code index
- **testUniqueConstraintExists**: Query information_schema for unique constraint
- **testUniqueConstraintViolation**: Attempt duplicate insert, verify error
- **testDifferentChannelAllowed**: Insert same year+product with different channel, verify success
- **testJsonOperations**: Insert and query JSON data, verify JSON_EXTRACT works
- **testIndexPerformance**: Execute queries and verify index usage via EXPLAIN

### Setup
```java
@SpringBootTest
@Testcontainers
class ProductionPlanMigrationIT {
    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testTableStructure() {
        // Verify table exists and has correct structure
    }

    @Test
    void testUniqueConstraint() {
        // Insert first record
        jdbcTemplate.update(
            "INSERT INTO production_plan (year, product_code, product_name, category, spec, warehouse_location, channel) " +
            "VALUES (2026, 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A', 'CH-DIRECT')"
        );

        // Attempt duplicate insert
        assertThrows(DuplicateKeyException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO production_plan (year, product_code, product_name, category, spec, warehouse_location, channel) " +
                "VALUES (2026, 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A', 'CH-DIRECT')"
            );
        });
    }

    @Test
    void testJsonColumn() {
        jdbcTemplate.update(
            "INSERT INTO production_plan (year, product_code, product_name, category, spec, warehouse_location, channel, monthly_allocation) " +
            "VALUES (2026, 'PROD002', 'Product 2', 'Cat B', 'Spec B', 'WH-B', 'CH-RETAIL', '{\"2\": 100.5, \"3\": 150.75}')"
        );

        String json = jdbcTemplate.queryForObject(
            "SELECT monthly_allocation FROM production_plan WHERE product_code = 'PROD002'",
            String.class
        );

        assertNotNull(json);
        assertTrue(json.contains("\"2\""));

        BigDecimal febQty = jdbcTemplate.queryForObject(
            "SELECT JSON_EXTRACT(monthly_allocation, '$.2') FROM production_plan WHERE product_code = 'PROD002'",
            BigDecimal.class
        );

        assertEquals(new BigDecimal("100.5"), febQty);
    }
}
```

## E2E Tests
- N/A (database migration only)

## Test Data Setup Notes

### Minimal Test Record
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel)
VALUES
(2026, 'PROD001', 'Product 1', 'Category A', 'Spec A', 'WH-A', 'CH-DIRECT');
```

### Full Test Record with JSON
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel,
 monthly_allocation, buffer_quantity, total_quantity, original_forecast, difference, remarks)
VALUES
(2026, 'PROD002', 'Product 2', 'Category B', 'Spec B', 'WH-B', 'CH-RETAIL',
 '{"2": 100.00, "3": 150.00, "4": 200.00, "5": 180.00, "6": 220.00, "7": 250.00, "8": 230.00, "9": 210.00, "10": 240.00, "11": 260.00, "12": 280.00}',
 50.00, 2370.00, 2300.00, 70.00,
 'Adjusted based on Q1 sales performance');
```

### Multiple Channels for Same Product
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel, monthly_allocation)
VALUES
(2026, 'PROD003', 'Product 3', 'Category C', 'Spec C', 'WH-C', 'CH-DIRECT', '{"2": 100, "3": 110, "4": 120, "5": 130, "6": 140, "7": 150, "8": 160, "9": 170, "10": 180, "11": 190, "12": 200}'),
(2026, 'PROD003', 'Product 3', 'Category C', 'Spec C', 'WH-C', 'CH-RETAIL', '{"2": 50, "3": 55, "4": 60, "5": 65, "6": 70, "7": 75, "8": 80, "9": 85, "10": 90, "11": 95, "12": 100}'),
(2026, 'PROD003', 'Product 3', 'Category C', 'Spec C', 'WH-C', 'CH-ONLINE', '{"2": 30, "3": 35, "4": 40, "5": 45, "6": 50, "7": 55, "8": 60, "9": 65, "10": 70, "11": 75, "12": 80}');
```

### JSON Monthly Allocation Structure
```json
{
  "2": 100.00,   // February
  "3": 150.00,   // March
  "4": 200.00,   // April
  "5": 180.00,   // May
  "6": 220.00,   // June
  "7": 250.00,   // July
  "8": 230.00,   // August
  "9": 210.00,   // September
  "10": 240.00,  // October
  "11": 260.00,  // November
  "12": 280.00   // December
}
```

### Index Test Data
```sql
-- Insert multiple years for same product to test year+product_code index
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel)
VALUES
(2025, 'PROD004', 'Product 4', 'Category D', 'Spec D', 'WH-D', 'CH-DIRECT'),
(2026, 'PROD004', 'Product 4', 'Category D', 'Spec D', 'WH-D', 'CH-DIRECT'),
(2027, 'PROD004', 'Product 4', 'Category D', 'Spec D', 'WH-D', 'CH-DIRECT');
```

## Mocking Strategy
- No mocking required for database migration tests
- Use Testcontainers with MariaDB 10.11 image for realistic testing
- Tests run against actual database to verify DDL correctness and JSON support

## Performance Benchmarks
- Migration execution: < 5 seconds
- Index creation: < 2 seconds
- Query with composite index on 10,000 rows: < 100ms
- Unique constraint check on insert: < 50ms
- JSON extraction query on 10,000 rows: < 200ms

## JSON Validation Notes
- Application layer should validate JSON structure (months 2-12 only)
- Application layer should validate all values are valid DECIMAL(10,2)
- Database accepts any valid JSON; schema validation is not enforced at DB level
- Consider adding CHECK constraint in future migration if MariaDB version supports JSON schema validation
