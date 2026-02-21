# T027: Acceptance Criteria

## Functional Acceptance Criteria

### Migration Execution
- [ ] Flyway migration V7 executes successfully on clean database
- [ ] Migration is idempotent (can be tested in dev environment)
- [ ] Migration executes in under 5 seconds on empty database
- [ ] flyway_schema_history table records successful migration

### Table Structure
- [ ] Table `production_plan` exists in database
- [ ] All 15 columns are present with correct names
- [ ] Primary key constraint exists on `id` column
- [ ] `id` column auto-increments correctly

### Column Specifications
- [ ] year: INT, NOT NULL
- [ ] product_code: VARCHAR(50), NOT NULL
- [ ] product_name: VARCHAR(200), NOT NULL
- [ ] category: VARCHAR(100), NOT NULL
- [ ] spec: VARCHAR(200), NOT NULL
- [ ] warehouse_location: VARCHAR(50), NOT NULL
- [ ] channel: VARCHAR(50), NOT NULL
- [ ] monthly_allocation: JSON, DEFAULT '{}', NOT NULL
- [ ] buffer_quantity: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] total_quantity: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] original_forecast: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] difference: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] remarks: TEXT, NULL
- [ ] created_at: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP, NOT NULL
- [ ] updated_at: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL

### Indexes and Constraints
- [ ] Index `idx_production_plan_year_product` exists on (year, product_code)
- [ ] Unique constraint `uk_production_plan_year_product_channel` exists on (year, product_code, channel)
- [ ] EXPLAIN query shows index usage for year+product_code queries
- [ ] Duplicate insert with same year+product_code+channel fails with unique constraint violation

## Non-Functional Criteria
- [ ] Migration follows V{number}__{description}.sql naming convention
- [ ] SQL is formatted and readable
- [ ] Comments explain purpose of indexes and constraints
- [ ] Compatible with MariaDB 10.11 JSON features
- [ ] No use of deprecated SQL features

## How to Verify

### 1. Run Migration
```bash
cd backend
./mvnw flyway:migrate
```

### 2. Verify Table Structure
```sql
DESCRIBE production_plan;
SHOW CREATE TABLE production_plan;
```

### 3. Verify Indexes and Constraints
```sql
SHOW INDEXES FROM production_plan;
SHOW CREATE TABLE production_plan;
-- Look for UNIQUE KEY and KEY definitions
```

### 4. Test Default Values
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel)
VALUES (2026, 'PROD001', 'Product 1', 'Category A', 'Spec A', 'WH-A', 'CH-DIRECT');

SELECT buffer_quantity, total_quantity, original_forecast, difference, monthly_allocation, remarks
FROM production_plan
WHERE product_code = 'PROD001';
-- Expect: 0.00, 0.00, 0.00, 0.00, '{}', NULL
```

### 5. Test JSON Column
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel, monthly_allocation)
VALUES (2026, 'PROD002', 'Product 2', 'Category B', 'Spec B', 'WH-B', 'CH-RETAIL',
        '{"2": 100.50, "3": 150.75, "12": 200.00}');

SELECT monthly_allocation FROM production_plan WHERE product_code = 'PROD002';
-- Should return valid JSON

SELECT JSON_EXTRACT(monthly_allocation, '$.2') as feb_quantity
FROM production_plan WHERE product_code = 'PROD002';
-- Should return 100.50
```

### 6. Test Unique Constraint
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel)
VALUES (2026, 'PROD001', 'Product 1', 'Category A', 'Spec A', 'WH-A', 'CH-DIRECT');

-- Try to insert duplicate
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel)
VALUES (2026, 'PROD001', 'Product 1', 'Category A', 'Spec A', 'WH-A', 'CH-DIRECT');
-- Expect: Error 1062 - Duplicate entry for key 'uk_production_plan_year_product_channel'
```

### 7. Test Same Product Different Channel (Should Succeed)
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel)
VALUES (2026, 'PROD001', 'Product 1', 'Category A', 'Spec A', 'WH-A', 'CH-RETAIL');
-- Should succeed (different channel)
```

### 8. Test Timestamps
```sql
SELECT created_at, updated_at FROM production_plan WHERE product_code = 'PROD001' LIMIT 1;
-- Both should be populated with current timestamp

UPDATE production_plan SET buffer_quantity = 10 WHERE product_code = 'PROD001' LIMIT 1;
SELECT created_at, updated_at FROM production_plan WHERE product_code = 'PROD001' LIMIT 1;
-- created_at unchanged, updated_at should be newer
```

### 9. Verify Index Usage
```sql
EXPLAIN SELECT * FROM production_plan WHERE year = 2026 AND product_code = 'PROD001';
-- Should show key: idx_production_plan_year_product

EXPLAIN SELECT * FROM production_plan WHERE year = 2026 AND product_code = 'PROD001' AND channel = 'CH-DIRECT';
-- Should show key: uk_production_plan_year_product_channel (unique index)
```

### 10. Test TEXT Column
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel, remarks)
VALUES (2026, 'PROD003', 'Product 3', 'Category C', 'Spec C', 'WH-C', 'CH-ONLINE',
        'This is a very long remark with multiple sentences. It can contain detailed notes about the production plan. ' ||
        'Special considerations, adjustments, or any other relevant information can be stored here.');

SELECT LENGTH(remarks) FROM production_plan WHERE product_code = 'PROD003';
-- Should return length > 100 (TEXT supports 65,535 bytes)
```
