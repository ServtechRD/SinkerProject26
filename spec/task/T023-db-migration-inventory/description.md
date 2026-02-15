# T023: Database Migration - Inventory Sales Forecast Table

## Context
The inventory integration module requires a database table to store combined inventory, sales, and forecast data. This table will be populated by real-time queries to the ERP system and sales forecast data, enabling production planning based on actual inventory levels and sales trends.

## Goal
Create a Flyway migration to establish the `inventory_sales_forecast` table with all necessary columns, indexes, and constraints to support inventory integration queries and version tracking.

## Scope

### In Scope
- Create `inventory_sales_forecast` table with all specified columns
- Add composite index on (month, product_code) for query performance
- Add index on version for version-based queries
- Set appropriate column types, lengths, and defaults
- Include audit timestamps (created_at, updated_at)

### Out of Scope
- Data population or seeding
- Stored procedures or triggers
- Integration with application code
- Data migration from existing tables

## Requirements
- Table name: `inventory_sales_forecast`
- Primary key: `id` (INT, auto-increment)
- Month field: VARCHAR(7) format YYYY-MM
- Product fields: product_code (VARCHAR 50), product_name (VARCHAR 200), category (VARCHAR 100), spec (VARCHAR 200)
- Location field: warehouse_location (VARCHAR 50)
- Numeric fields with DECIMAL(10,2): sales_quantity, inventory_balance, forecast_quantity, production_subtotal (all default 0)
- modified_subtotal: DECIMAL(10,2), nullable, no default
- Version tracking: version (VARCHAR 100), query_start_date (VARCHAR 10), query_end_date (VARCHAR 10)
- Timestamps: created_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP), updated_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)
- Composite index: idx_inventory_forecast_month_product on (month, product_code)
- Single index: idx_inventory_forecast_version on (version)

## Implementation Notes
- Follow existing Flyway migration naming convention (V6__create_inventory_sales_forecast.sql)
- Use MariaDB 10.11 compatible SQL syntax
- Ensure column types match the precision requirements for financial calculations
- modified_subtotal is nullable to distinguish between "not modified" and "modified to 0"
- Version field stores UUID or timestamp-based version identifier
- Date fields stored as VARCHAR for flexible query parameter matching

## Files to Change
- `backend/src/main/resources/db/migration/V6__create_inventory_sales_forecast.sql` (new file)

## Dependencies
- T001: Initial database setup and Flyway configuration must be complete
- MariaDB 10.11 database instance must be available
