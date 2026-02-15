# T030: Test Plan

## Unit Tests
Database migrations are typically not unit tested in isolation. Verification is done through integration tests and manual verification.

- N/A for Flyway migrations

## Integration Tests

### FlywayMigrationV8Test.java
- **testMigrationV8Executes**: Verify V8 migration executes without errors
- **testWeeklyScheduleTableExists**: Query information_schema to confirm table creation
- **testAllColumnsExist**: Verify all 10 columns are present with correct data types
- **testPrimaryKeyConstraint**: Verify id is primary key and auto-increments
- **testDefaultQuantity**: Insert row without quantity, verify default 0
- **testDecimalPrecision**: Insert values with 2 decimal places, verify no truncation
- **testTimestampBehavior**: Test created_at defaults and updated_at auto-updates
- **testCompositeIndexExists**: Query information_schema for week_start+factory index
- **testProductIndexExists**: Query information_schema for product_code index
- **testMondayCheckConstraint**: Test Monday validation constraint
- **testIndexPerformance**: Execute queries and verify index usage via EXPLAIN

### MondayValidationTest.java
- **testMondayAccepted**: Insert with Monday date, verify success
- **testTuesdayRejected**: Insert with Tuesday date, verify CHECK constraint failure
- **testWednesdayRejected**: Insert with Wednesday, verify failure
- **testSundayRejected**: Insert with Sunday, verify failure
- **testMultipleMondaysAccepted**: Insert multiple Monday dates, verify all succeed
- **testDateCalculation**: Verify DAYOFWEEK function works correctly for various dates

### Setup
```java
@SpringBootTest
@Testcontainers
class WeeklyScheduleMigrationIT {
    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testTableStructure() {
        // Verify table exists and has correct structure
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = DATABASE() AND table_name = 'production_weekly_schedule'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertEquals(1, count);
    }

    @Test
    void testMondayValidation() {
        // Test Monday (should succeed)
        String sqlMonday = "INSERT INTO production_weekly_schedule " +
                          "(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                          "VALUES ('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100)";
        assertDoesNotThrow(() -> jdbcTemplate.update(sqlMonday));

        // Test Tuesday (should fail)
        String sqlTuesday = "INSERT INTO production_weekly_schedule " +
                           "(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                           "VALUES ('2026-02-03', 'FACTORY-A', '2026-02-04', 'PROD002', 'Product 2', 'WH-A', 100)";
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(sqlTuesday));

        // Test Sunday (should fail)
        String sqlSunday = "INSERT INTO production_weekly_schedule " +
                          "(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                          "VALUES ('2026-02-01', 'FACTORY-A', '2026-02-02', 'PROD003', 'Product 3', 'WH-A', 100)";
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(sqlSunday));
    }

    @Test
    void testCompositeIndex() {
        // Insert test data
        jdbcTemplate.update(
            "INSERT INTO production_weekly_schedule " +
            "(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
            "VALUES ('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100)"
        );

        // Verify index usage with EXPLAIN
        String explain = "EXPLAIN SELECT * FROM production_weekly_schedule " +
                        "WHERE week_start = '2026-02-02' AND factory = 'FACTORY-A'";

        jdbcTemplate.query(explain, (rs) -> {
            String key = rs.getString("key");
            assertEquals("idx_weekly_schedule_week_factory", key);
        });
    }

    @Test
    void testDecimalPrecision() {
        jdbcTemplate.update(
            "INSERT INTO production_weekly_schedule " +
            "(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
            "VALUES ('2026-02-09', 'FACTORY-B', '2026-02-10', 'PROD100', 'Product 100', 'WH-B', 123.45)"
        );

        BigDecimal quantity = jdbcTemplate.queryForObject(
            "SELECT quantity FROM production_weekly_schedule WHERE product_code = 'PROD100'",
            BigDecimal.class
        );

        assertEquals(new BigDecimal("123.45"), quantity);
    }
}
```

## E2E Tests
- N/A (database migration only)

## Test Data Setup Notes

### Minimal Test Record (Monday)
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location)
VALUES
('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD001', 'Product 1', 'WH-A');
-- quantity defaults to 0.00
```

### Full Test Record
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES
('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD002', 'Product 2', 'WH-A', 150.75);
```

### Multiple Factories Same Week
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES
('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD003', 'Product 3', 'WH-A', 100),
('2026-02-02', 'FACTORY-B', '2026-02-03', 'PROD004', 'Product 4', 'WH-B', 200),
('2026-02-02', 'FACTORY-C', '2026-02-04', 'PROD005', 'Product 5', 'WH-C', 150);
```

### Multiple Weeks Same Factory
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES
('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD006', 'Product 6', 'WH-A', 100), -- Week 1
('2026-02-09', 'FACTORY-A', '2026-02-10', 'PROD007', 'Product 7', 'WH-A', 150), -- Week 2
('2026-02-16', 'FACTORY-A', '2026-02-17', 'PROD008', 'Product 8', 'WH-A', 200); -- Week 3
```

### Monday Dates for 2026 (Reference)
```
Jan: 5, 12, 19, 26
Feb: 2, 9, 16, 23
Mar: 2, 9, 16, 23, 30
Apr: 6, 13, 20, 27
May: 4, 11, 18, 25
Jun: 1, 8, 15, 22, 29
Jul: 6, 13, 20, 27
Aug: 3, 10, 17, 24, 31
Sep: 7, 14, 21, 28
Oct: 5, 12, 19, 26
Nov: 2, 9, 16, 23, 30
Dec: 7, 14, 21, 28
```

### Invalid Dates (Non-Monday)
```sql
-- These should all FAIL with CHECK constraint violation
('2026-02-01', ...); -- Sunday
('2026-02-03', ...); -- Tuesday
('2026-02-04', ...); -- Wednesday
('2026-02-05', ...); -- Thursday
('2026-02-06', ...); -- Friday
('2026-02-07', ...); -- Saturday
```

### Test Data for Index Performance
```sql
-- Insert 1000 records across multiple weeks and factories
-- for performance testing
DO $$
DECLARE
    week DATE;
    factory_num INT;
    product_num INT;
BEGIN
    FOR week IN (SELECT generate_series('2026-01-05'::DATE, '2026-12-28'::DATE, '7 days')::DATE)
    LOOP
        FOR factory_num IN 1..5 LOOP
            FOR product_num IN 1..20 LOOP
                INSERT INTO production_weekly_schedule
                (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
                VALUES (
                    week,
                    'FACTORY-' || factory_num,
                    week + 1,
                    'PROD' || LPAD(product_num::TEXT, 3, '0'),
                    'Product ' || product_num,
                    'WH-' || CHR(64 + factory_num),
                    (random() * 1000)::DECIMAL(10,2)
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
```

## Mocking Strategy
- No mocking required for database migration tests
- Use Testcontainers with MariaDB 10.11 image for realistic testing
- Tests run against actual database to verify DDL correctness
- CHECK constraint validation tested with real date inserts

## Performance Benchmarks
- Migration execution: < 5 seconds
- Index creation: < 2 seconds
- Query with composite index on 10,000 rows: < 100ms
- Query with product_code index on 10,000 rows: < 50ms
- CHECK constraint validation on insert: < 10ms

## Edge Cases to Test
1. Week start on first Monday of year (Jan 5, 2026)
2. Week start on last Monday of year (Dec 28, 2026)
3. Leap year February Mondays
4. Multiple records same week+factory different products
5. Same product different weeks
6. Same product different factories
7. Demand date before week_start (unusual but valid)
8. Demand date weeks after week_start (unusual but valid)
9. Maximum quantity value (9999999999.99)
10. Zero quantity (valid, uses default)
