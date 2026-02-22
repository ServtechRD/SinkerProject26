# T034: Acceptance Criteria

## Functional Acceptance Criteria
1. Migration file `V9__create_semi_product.sql` exists in `src/main/resources/db/migration/`
2. Migration executes successfully when application starts
3. Table `semi_product_advance_purchase` is created with all required columns
4. Primary key constraint exists on `id` column
5. Unique constraint exists on `product_code` column
6. All columns have correct data types and constraints
7. Timestamp columns have proper default values and auto-update behavior

## Database Schema Verification
- Table name: `semi_product_advance_purchase`
- Column specifications:
  - `id`: INT, PRIMARY KEY, AUTO_INCREMENT
  - `product_code`: VARCHAR(50), UNIQUE, NOT NULL
  - `product_name`: VARCHAR(200), NOT NULL
  - `advance_days`: INT, NOT NULL
  - `created_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

## Non-Functional Criteria
- Migration completes in under 1 second on empty database
- Migration is idempotent or fails gracefully if table already exists
- Migration follows Flyway naming conventions
- SQL syntax is compatible with MariaDB 10.11

## How to Verify
1. Start the Spring Boot application with Flyway enabled
2. Check application logs for successful migration: "Migrating schema to version 9"
3. Connect to MariaDB database
4. Run: `DESCRIBE semi_product_advance_purchase;`
5. Verify all columns exist with correct types
6. Run: `SHOW CREATE TABLE semi_product_advance_purchase;`
7. Verify UNIQUE constraint on `product_code`
8. Run: `SELECT * FROM flyway_schema_history WHERE version = '9';`
9. Verify migration is recorded with success status
10. Attempt to insert duplicate `product_code` and verify UNIQUE constraint error
