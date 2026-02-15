# T040: Acceptance Criteria

## Functional Acceptance Criteria
1. Migration file `V11__create_material_purchase.sql` exists in `src/main/resources/db/migration/`
2. Migration executes successfully when application starts
3. Table `material_purchase` is created with all required columns
4. Primary key constraint exists on `id` column
5. Composite index exists on (week_start, factory)
6. Index exists on product_code
7. All columns have correct data types and constraints
8. Decimal columns have precision 10,2
9. Boolean column defaults to FALSE
10. Default values are set correctly for quantity calculation columns
11. Timestamp columns have proper default values and auto-update behavior

## Database Schema Verification
- Table name: `material_purchase`
- Column specifications:
  - `id`: INT, PRIMARY KEY, AUTO_INCREMENT
  - `week_start`: DATE, NOT NULL
  - `factory`: VARCHAR(50), NOT NULL
  - `product_code`: VARCHAR(50), NOT NULL
  - `product_name`: VARCHAR(200), NOT NULL
  - `quantity`: DECIMAL(10,2), NOT NULL
  - `semi_product_name`: VARCHAR(200), NOT NULL
  - `semi_product_code`: VARCHAR(100), NOT NULL
  - `kg_per_box`: DECIMAL(10,2), DEFAULT 0
  - `basket_quantity`: DECIMAL(10,2), DEFAULT 0
  - `boxes_per_barrel`: DECIMAL(10,2), DEFAULT 0
  - `required_barrels`: DECIMAL(10,2), DEFAULT 0
  - `is_erp_triggered`: BOOLEAN, DEFAULT FALSE
  - `erp_order_no`: VARCHAR(100), NULL
  - `created_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- Indexes:
  - Primary key on `id`
  - Composite index on (week_start, factory)
  - Index on product_code

## Non-Functional Criteria
- Migration completes in under 1 second on empty database
- Migration is idempotent or fails gracefully if table already exists
- Migration follows Flyway naming conventions
- SQL syntax is compatible with MariaDB 10.11
- Indexes improve query performance for weekly lookups

## How to Verify
1. Start Spring Boot application with Flyway enabled
2. Check application logs for successful migration: "Migrating schema to version 11"
3. Connect to MariaDB database
4. Run: `DESCRIBE material_purchase;`
5. Verify all columns exist with correct types and constraints
6. Run: `SHOW CREATE TABLE material_purchase;`
7. Verify decimal columns show DECIMAL(10,2)
8. Verify default values for kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, is_erp_triggered
9. Run: `SHOW INDEX FROM material_purchase;`
10. Verify composite index on (week_start, factory)
11. Verify index on product_code
12. Insert test row with all required fields only
13. Verify default values applied to calculation columns
14. Verify is_erp_triggered defaults to FALSE (0)
15. Verify erp_order_no defaults to NULL
16. Insert test row with is_erp_triggered = TRUE and erp_order_no = "ERP-12345"
17. Verify boolean and varchar values stored correctly
18. Update a row and verify updated_at timestamp changed
19. Run: `SELECT * FROM flyway_schema_history WHERE version = '11';`
20. Verify migration recorded with success status
