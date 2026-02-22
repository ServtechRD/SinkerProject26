# T030: Acceptance Criteria

## Functional Acceptance Criteria

### Migration Execution
- [ ] Flyway migration V8 executes successfully on clean database
- [ ] Migration is idempotent (can be tested in dev environment)
- [ ] Migration executes in under 5 seconds on empty database
- [ ] flyway_schema_history table records successful migration

### Table Structure
- [ ] Table `production_weekly_schedule` exists in database
- [ ] All 10 columns are present with correct names
- [ ] Primary key constraint exists on `id` column
- [ ] `id` column auto-increments correctly

### Column Specifications
- [ ] week_start: DATE, NOT NULL
- [ ] factory: VARCHAR(50), NOT NULL
- [ ] demand_date: DATE, NOT NULL
- [ ] product_code: VARCHAR(50), NOT NULL
- [ ] product_name: VARCHAR(200), NOT NULL
- [ ] warehouse_location: VARCHAR(50), NOT NULL
- [ ] quantity: DECIMAL(10,2), NOT NULL, DEFAULT 0
- [ ] created_at: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP, NOT NULL
- [ ] updated_at: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL

### Indexes and Constraints
- [ ] Index `idx_weekly_schedule_week_factory` exists on (week_start, factory)
- [ ] Index `idx_weekly_schedule_product` exists on (product_code)
- [ ] CHECK constraint exists to ensure week_start is Monday
- [ ] EXPLAIN query shows index usage for week_start+factory queries
- [ ] EXPLAIN query shows index usage for product_code queries

### Monday Validation
- [ ] Insert with Monday date succeeds
- [ ] Insert with Tuesday date fails (CHECK constraint violation)
- [ ] Insert with Sunday date fails (CHECK constraint violation)

## Non-Functional Criteria
- [ ] Migration follows V{number}__{description}.sql naming convention
- [ ] SQL is formatted and readable
- [ ] Comments explain purpose of indexes and CHECK constraint
- [ ] Compatible with MariaDB 10.11
- [ ] No use of deprecated SQL features

## How to Verify

### 1. Run Migration
```bash
cd backend
./mvnw flyway:migrate
```

### 2. Verify Table Structure
```sql
DESCRIBE production_weekly_schedule;
SHOW CREATE TABLE production_weekly_schedule;
```

### 3. Verify Indexes
```sql
SHOW INDEXES FROM production_weekly_schedule;
-- Expect: idx_weekly_schedule_week_factory, idx_weekly_schedule_product
```

### 4. Test Default Values
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location)
VALUES ('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD001', 'Product 1', 'WH-A');
-- 2026-02-02 is a Monday

SELECT quantity FROM production_weekly_schedule WHERE product_code = 'PROD001';
-- Expect: 0.00 (default)
```

### 5. Test Monday Validation (Success)
```sql
-- Test with various Mondays
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES
('2026-01-05', 'FACTORY-A', '2026-01-06', 'PROD002', 'Product 2', 'WH-A', 100.00), -- Monday
('2026-01-12', 'FACTORY-A', '2026-01-13', 'PROD003', 'Product 3', 'WH-A', 150.00), -- Monday
('2026-01-19', 'FACTORY-A', '2026-01-20', 'PROD004', 'Product 4', 'WH-A', 200.00); -- Monday

SELECT COUNT(*) FROM production_weekly_schedule;
-- Should succeed and return 4 (including first test record)
```

### 6. Test Monday Validation (Failure - Tuesday)
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES ('2026-01-06', 'FACTORY-A', '2026-01-07', 'PROD005', 'Product 5', 'WH-A', 100.00);
-- 2026-01-06 is a Tuesday
-- Expect: ERROR 4025 (23000): CONSTRAINT `production_weekly_schedule_chk_1` failed for `...`
```

### 7. Test Monday Validation (Failure - Sunday)
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES ('2026-01-04', 'FACTORY-A', '2026-01-05', 'PROD006', 'Product 6', 'WH-A', 100.00);
-- 2026-01-04 is a Sunday
-- Expect: CHECK constraint failure
```

### 8. Test Timestamps
```sql
SELECT created_at, updated_at FROM production_weekly_schedule WHERE product_code = 'PROD001';
-- Both should be populated with current timestamp

UPDATE production_weekly_schedule SET quantity = 50 WHERE product_code = 'PROD001';
SELECT created_at, updated_at FROM production_weekly_schedule WHERE product_code = 'PROD001';
-- created_at unchanged, updated_at should be newer
```

### 9. Verify Index Usage
```sql
EXPLAIN SELECT * FROM production_weekly_schedule
WHERE week_start = '2026-02-02' AND factory = 'FACTORY-A';
-- Should show key: idx_weekly_schedule_week_factory

EXPLAIN SELECT * FROM production_weekly_schedule WHERE product_code = 'PROD001';
-- Should show key: idx_weekly_schedule_product
```

### 10. Test Composite Index Query
```sql
-- Insert multiple weeks and factories
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES
('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD010', 'Product 10', 'WH-A', 100),
('2026-02-02', 'FACTORY-B', '2026-02-03', 'PROD011', 'Product 11', 'WH-B', 200),
('2026-02-09', 'FACTORY-A', '2026-02-10', 'PROD012', 'Product 12', 'WH-A', 150);

SELECT COUNT(*) FROM production_weekly_schedule
WHERE week_start = '2026-02-02' AND factory = 'FACTORY-A';
-- Should return 2 (PROD001 from earlier + PROD010)
```

### 11. Verify Decimal Precision
```sql
INSERT INTO production_weekly_schedule
(week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity)
VALUES ('2026-02-02', 'FACTORY-A', '2026-02-03', 'PROD020', 'Product 20', 'WH-A', 123.45);

SELECT quantity FROM production_weekly_schedule WHERE product_code = 'PROD020';
-- Expect: 123.45 (no truncation)
```

### 12. Test Date Range
```sql
SELECT * FROM production_weekly_schedule
WHERE week_start BETWEEN '2026-01-01' AND '2026-12-31';
-- Should return all 2026 records
```
