# T014: Acceptance Criteria

## Functional Acceptance Criteria
- [ ] Migration V5__create_sales_forecast.sql executes successfully
- [ ] Table `sales_forecast` exists with correct schema
- [ ] All columns have correct data types, lengths, and constraints
- [ ] Primary key on `id` column is AUTO_INCREMENT
- [ ] Default value for is_modified is FALSE
- [ ] Timestamps auto-populate correctly
- [ ] All required indexes are created
- [ ] Table supports utf8mb4 charset for Chinese characters

## Database Schema Validation
```sql
-- Expected schema:
CREATE TABLE sales_forecast (
    id INT PRIMARY KEY AUTO_INCREMENT,
    month VARCHAR(7) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    spec VARCHAR(200),
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(200),
    warehouse_location VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL,
    version VARCHAR(100) NOT NULL,
    is_modified BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_month_channel (month, channel),
    INDEX idx_product_code (product_code),
    INDEX idx_version (version),
    INDEX idx_month_channel_product (month, channel, product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Index Validation
- [ ] Index `idx_month_channel` on (month, channel) exists
- [ ] Index `idx_product_code` on (product_code) exists
- [ ] Index `idx_version` on (version) exists
- [ ] Index `idx_month_channel_product` on (month, channel, product_code) exists

## Non-Functional Criteria
- [ ] Migration executes within 1 second on empty database
- [ ] Table uses InnoDB engine
- [ ] Character set is utf8mb4 with utf8mb4_unicode_ci collation
- [ ] Migration follows Flyway naming convention
- [ ] SQL syntax is MariaDB 10.11 compatible

## How to Verify

### Migration Execution
1. Start with database at V4 (or rollback to V4)
2. Run Flyway migration: `./mvnw flyway:migrate`
3. Verify migration status: `./mvnw flyway:info` shows V5 as SUCCESS
4. Check for any errors in console output

### Schema Verification
```sql
-- Describe table structure
DESCRIBE sales_forecast;

-- Expected output should show all columns with correct types:
-- id: int, PK, auto_increment
-- month: varchar(7)
-- channel: varchar(50)
-- category: varchar(100)
-- spec: varchar(200)
-- product_code: varchar(50)
-- product_name: varchar(200)
-- warehouse_location: varchar(50)
-- quantity: decimal(10,2)
-- version: varchar(100)
-- is_modified: tinyint(1), default 0
-- created_at: timestamp, default CURRENT_TIMESTAMP
-- updated_at: timestamp, default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
```

### Index Verification
```sql
-- Show all indexes
SHOW INDEX FROM sales_forecast;

-- Verify these indexes exist:
-- PRIMARY on (id)
-- idx_month_channel on (month, channel)
-- idx_product_code on (product_code)
-- idx_version on (version)
-- idx_month_channel_product on (month, channel, product_code)
```

### Character Set Verification
```sql
-- Check table charset
SHOW CREATE TABLE sales_forecast;

-- Should show: CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

### Data Insertion Test
```sql
-- Test insert with Chinese characters
INSERT INTO sales_forecast (
    month, channel, category, spec, product_code, product_name,
    warehouse_location, quantity, version
) VALUES (
    '202601', '大全聯', '飲料類', '600ml*24入', 'P001', '可口可樂',
    'A01', 100.50, '2026/01/15 10:30:00(大全聯)'
);

-- Verify insertion
SELECT * FROM sales_forecast WHERE product_code = 'P001';

-- Verify Chinese characters display correctly
-- Verify quantity as decimal: 100.50
-- Verify is_modified defaults to 0
-- Verify timestamps populated
```

### Index Performance Test
```sql
-- Test query with month+channel index
EXPLAIN SELECT * FROM sales_forecast
WHERE month = '202601' AND channel = '大全聯';
-- Should show "Using index" or use idx_month_channel

-- Test query with product_code index
EXPLAIN SELECT * FROM sales_forecast
WHERE product_code = 'P001';
-- Should use idx_product_code

-- Test query with version index
EXPLAIN SELECT * FROM sales_forecast
WHERE version = '2026/01/15 10:30:00(大全聯)';
-- Should use idx_version
```

### Default Values Test
```sql
-- Insert minimal record
INSERT INTO sales_forecast (
    month, channel, product_code, quantity, version
) VALUES (
    '202602', '家樂福', 'P002', 50.00, '2026/02/01 08:00:00(家樂福)'
);

-- Verify defaults
SELECT is_modified, created_at, updated_at
FROM sales_forecast WHERE product_code = 'P002';

-- is_modified should be 0 (FALSE)
-- created_at should be populated
-- updated_at should equal created_at
```

### Update Timestamp Test
```sql
-- Update a record
UPDATE sales_forecast
SET quantity = 75.00
WHERE product_code = 'P002';

-- Verify updated_at changed
SELECT created_at, updated_at
FROM sales_forecast WHERE product_code = 'P002';

-- updated_at should be greater than created_at
```
