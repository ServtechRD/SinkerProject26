# T037: Acceptance Criteria

## Functional Acceptance Criteria
1. Migration file `V10__create_material_demand.sql` exists in `src/main/resources/db/migration/`
2. Migration executes successfully when application starts
3. Table `material_demand` is created with all required columns
4. Primary key constraint exists on `id` column
5. Composite index exists on (week_start, factory)
6. Index exists on material_code
7. All columns have correct data types and constraints
8. Decimal columns have precision 10,2
9. Default values are set correctly for quantity columns
10. Timestamp columns have proper default values and auto-update behavior

## Database Schema Verification
- Table name: `material_demand`
- Column specifications:
  - `id`: INT, PRIMARY KEY, AUTO_INCREMENT
  - `week_start`: DATE, NOT NULL
  - `factory`: VARCHAR(50), NOT NULL
  - `material_code`: VARCHAR(50), NOT NULL
  - `material_name`: VARCHAR(200), NOT NULL
  - `unit`: VARCHAR(20), NOT NULL
  - `last_purchase_date`: DATE, NULL
  - `demand_date`: DATE, NOT NULL
  - `expected_delivery`: DECIMAL(10,2), DEFAULT 0
  - `demand_quantity`: DECIMAL(10,2), DEFAULT 0
  - `estimated_inventory`: DECIMAL(10,2), DEFAULT 0
  - `created_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- Indexes:
  - Primary key on `id`
  - Composite index on (week_start, factory)
  - Index on material_code

## Non-Functional Criteria
- Migration completes in under 1 second on empty database
- Migration is idempotent or fails gracefully if table already exists
- Migration follows Flyway naming conventions
- SQL syntax is compatible with MariaDB 10.11
- Indexes improve query performance for weekly lookups

## How to Verify
1. Start Spring Boot application with Flyway enabled
2. Check application logs for successful migration: "Migrating schema to version 10"
3. Connect to MariaDB database
4. Run: `DESCRIBE material_demand;`
5. Verify all columns exist with correct types and constraints
6. Run: `SHOW CREATE TABLE material_demand;`
7. Verify decimal columns show DECIMAL(10,2)
8. Verify default values for expected_delivery, demand_quantity, estimated_inventory
9. Run: `SHOW INDEX FROM material_demand;`
10. Verify composite index on (week_start, factory)
11. Verify index on material_code
12. Insert test row with all required fields
13. Verify default values applied to quantity columns
14. Insert test row with NULL last_purchase_date
15. Verify NULL accepted for last_purchase_date
16. Update a row and verify updated_at timestamp changed
17. Run: `SELECT * FROM flyway_schema_history WHERE version = '10';`
18. Verify migration recorded with success status
