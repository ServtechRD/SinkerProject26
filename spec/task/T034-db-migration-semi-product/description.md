# T034: Database Migration - Semi Product Advance Purchase

## Context
This task creates the database schema for storing semi-product advance purchase configuration data. This table will hold product codes, names, and the number of days in advance that materials need to be purchased for each semi-product. This data is essential for calculating material demand dates in the production planning workflow.

## Goal
Create a Flyway migration to establish the `semi_product_advance_purchase` table in the MariaDB database, enabling storage and retrieval of semi-product advance purchase day configurations.

## Scope

### In Scope
- Create `semi_product_advance_purchase` table with required columns
- Add unique constraint on `product_code`
- Add created_at and updated_at timestamp columns
- Flyway migration script following project conventions

### Out of Scope
- Data population (handled by T035 upload API)
- Application layer entities or repositories
- Relationships to other tables

## Requirements
- Table name: `semi_product_advance_purchase`
- Columns:
  - `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
  - `product_code` (VARCHAR(50), UNIQUE, NOT NULL)
  - `product_name` (VARCHAR(200), NOT NULL)
  - `advance_days` (INT, NOT NULL)
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
  - `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)
- Unique constraint on `product_code` to prevent duplicate entries
- Follows MariaDB 10.11 syntax and conventions

## Implementation Notes
- Migration file should be named `V9__create_semi_product.sql` following Flyway versioning
- Use standard MariaDB table creation syntax with InnoDB engine
- Ensure proper indexing on `product_code` via UNIQUE constraint
- Include IF NOT EXISTS checks if appropriate for idempotency
- Follow existing migration patterns from T001

## Files to Change
- Create: `src/main/resources/db/migration/V9__create_semi_product.sql`

## Dependencies
- T001: Initial database setup and Flyway configuration must be complete
