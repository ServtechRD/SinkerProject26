# T023: Acceptance Criteria

## Functional Acceptance Criteria

### Migration Execution
- [ ] Flyway migration V6 executes successfully on clean database
- [ ] Migration is idempotent (can be tested in dev environment)
- [ ] Migration executes in under 5 seconds on empty database
- [ ] flyway_schema_history table records successful migration

### Table Structure
- [ ] Table `inventory_sales_forecast` exists in database
- [ ] All 18 columns are present with correct names
- [ ] Primary key constraint exists on `id` column
- [ ] `id` column auto-increments correctly

### Column Specifications
- [ ] month: VARCHAR(7), NOT NULL
- [ ] product_code: VARCHAR(50), NOT NULL
- [ ] product_name: VARCHAR(200), NOT NULL
- [ ] category: VARCHAR(100), NOT NULL
- [ ] spec: VARCHAR(200), NOT NULL
- [ ] warehouse_location: VARCHAR(50), NOT NULL
- [ ] sales_quantity: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] inventory_balance: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] forecast_quantity: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] production_subtotal: DECIMAL(10,2), DEFAULT 0, NOT NULL
- [ ] modified_subtotal: DECIMAL(10,2), NULL (no default)
- [ ] version: VARCHAR(100), NOT NULL
- [ ] query_start_date: VARCHAR(10), NOT NULL
- [ ] query_end_date: VARCHAR(10), NOT NULL
- [ ] created_at: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP, NOT NULL
- [ ] updated_at: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL

### Indexes
- [ ] Index `idx_inventory_forecast_month_product` exists on (month, product_code)
- [ ] Index `idx_inventory_forecast_version` exists on (version)
- [ ] EXPLAIN query shows index usage for month+product_code queries
- [ ] EXPLAIN query shows index usage for version queries

## Non-Functional Criteria
- [ ] Migration follows V{number}__{description}.sql naming convention
- [ ] SQL is formatted and readable
- [ ] Comments explain purpose of indexes
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
DESCRIBE inventory_sales_forecast;
SHOW CREATE TABLE inventory_sales_forecast;
```

### 3. Verify Indexes
```sql
SHOW INDEXES FROM inventory_sales_forecast;
```

### 4. Test Default Values
```sql
INSERT INTO inventory_sales_forecast
(month, product_code, product_name, category, spec, warehouse_location, version, query_start_date, query_end_date)
VALUES ('2026-01', 'TEST001', 'Test Product', 'Test Category', 'Test Spec', 'WH-A', 'v1', '2026-01-01', '2026-01-31');

SELECT sales_quantity, inventory_balance, forecast_quantity, production_subtotal, modified_subtotal
FROM inventory_sales_forecast
WHERE product_code = 'TEST001';
-- Expect: 0.00, 0.00, 0.00, 0.00, NULL
```

### 5. Test Timestamps
```sql
SELECT created_at, updated_at FROM inventory_sales_forecast WHERE product_code = 'TEST001';
-- Both should be populated with current timestamp

UPDATE inventory_sales_forecast SET sales_quantity = 10 WHERE product_code = 'TEST001';
SELECT created_at, updated_at FROM inventory_sales_forecast WHERE product_code = 'TEST001';
-- created_at unchanged, updated_at should be newer
```

### 6. Verify Index Usage
```sql
EXPLAIN SELECT * FROM inventory_sales_forecast WHERE month = '2026-01' AND product_code = 'TEST001';
-- Should show key: idx_inventory_forecast_month_product

EXPLAIN SELECT * FROM inventory_sales_forecast WHERE version = 'v1';
-- Should show key: idx_inventory_forecast_version
```
