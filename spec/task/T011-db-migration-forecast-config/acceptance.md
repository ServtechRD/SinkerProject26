# T011: Acceptance Criteria

## Functional Acceptance Criteria
- [ ] Migration V4__create_sales_forecast_config.sql executes successfully on clean database
- [ ] Table `sales_forecast_config` exists with correct schema
- [ ] All columns have correct data types and constraints
- [ ] Primary key on `id` column is AUTO_INCREMENT
- [ ] UNIQUE constraint on `month` column prevents duplicate entries
- [ ] Default values are set correctly (auto_close_day=10, is_closed=FALSE)
- [ ] `closed_at` accepts NULL values
- [ ] Timestamps (created_at, updated_at) auto-populate correctly

## Database Schema Validation
```sql
-- Expected schema:
CREATE TABLE sales_forecast_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    month VARCHAR(7) UNIQUE NOT NULL,
    auto_close_day INT DEFAULT 10 NOT NULL,
    is_closed BOOLEAN DEFAULT FALSE NOT NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);
```

## Non-Functional Criteria
- [ ] Migration is idempotent (can check if already applied)
- [ ] Migration executes in under 1 second
- [ ] Follows Flyway naming convention V{version}__{description}.sql
- [ ] SQL syntax is MariaDB 10.11 compatible
- [ ] Indexes are created for performance optimization

## How to Verify
1. Start with clean database or rollback to V3
2. Run Flyway migration: `./mvnw flyway:migrate`
3. Verify migration status: `./mvnw flyway:info` shows V4 as SUCCESS
4. Connect to database and run:
   ```sql
   DESCRIBE sales_forecast_config;
   SHOW INDEX FROM sales_forecast_config;
   ```
5. Test unique constraint:
   ```sql
   INSERT INTO sales_forecast_config (month) VALUES ('202501');
   INSERT INTO sales_forecast_config (month) VALUES ('202501'); -- Should fail
   ```
6. Test default values:
   ```sql
   INSERT INTO sales_forecast_config (month) VALUES ('202502');
   SELECT * FROM sales_forecast_config WHERE month = '202502';
   -- Verify: auto_close_day=10, is_closed=0, created_at populated
   ```
7. Test auto-update timestamp:
   ```sql
   UPDATE sales_forecast_config SET is_closed = TRUE WHERE month = '202502';
   -- Verify: updated_at changed
   ```
