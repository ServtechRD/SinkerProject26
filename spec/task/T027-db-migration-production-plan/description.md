# T027: Database Migration - Production Plan Table

## Context
The production planning module requires a database table to store annual production plans organized by product, channel, and month. The table supports monthly allocation data stored as JSON to accommodate flexible month-by-month planning from February to December (excluding January which is typically reserved for planning).

## Goal
Create a Flyway migration to establish the `production_plan` table with all necessary columns, indexes, and constraints to support annual production planning with monthly breakdowns across multiple sales channels.

## Scope

### In Scope
- Create `production_plan` table with all specified columns
- JSON column for monthly_allocation (months 2-12)
- Composite index on (year, product_code) for query performance
- Unique constraint on (year, product_code, channel) to prevent duplicates
- Set appropriate column types, lengths, and defaults
- Include audit timestamps (created_at, updated_at)

### Out of Scope
- Data population or seeding
- JSON schema validation (handled at application layer)
- Stored procedures or triggers
- Views or materialized views
- Data migration from existing tables

## Requirements
- Table name: `production_plan`
- Primary key: `id` (INT, auto-increment)
- Year field: year (INT, NOT NULL)
- Product fields: product_code (VARCHAR 50), product_name (VARCHAR 200), category (VARCHAR 100), spec (VARCHAR 200)
- Location field: warehouse_location (VARCHAR 50)
- Channel field: channel (VARCHAR 50, NOT NULL) - represents sales channel
- monthly_allocation: JSON, default '{}', stores month keys 2-12 with quantity values
- buffer_quantity: DECIMAL(10,2), default 0, NOT NULL
- total_quantity: DECIMAL(10,2), default 0, NOT NULL (calculated field)
- original_forecast: DECIMAL(10,2), default 0, NOT NULL
- difference: DECIMAL(10,2), default 0, NOT NULL (calculated: total_quantity - original_forecast)
- remarks: TEXT, nullable
- Timestamps: created_at, updated_at
- Composite index: idx_production_plan_year_product on (year, product_code)
- Unique constraint: uk_production_plan_year_product_channel on (year, product_code, channel)

## Implementation Notes
- Follow existing Flyway migration naming convention (V7__create_production_plan.sql)
- Use MariaDB 10.11 compatible SQL syntax with JSON support
- JSON column stores structure: {"2": 100.00, "3": 150.00, ..., "12": 200.00}
- Month keys are strings "2" through "12" (February through December)
- total_quantity and difference are calculated fields but stored for query performance
- Unique constraint prevents duplicate channel entries for same year/product combination
- VARCHAR lengths accommodate multi-byte characters

## Files to Change
- `backend/src/main/resources/db/migration/V7__create_production_plan.sql` (new file)

## Dependencies
- T001: Initial database setup and Flyway configuration must be complete
- MariaDB 10.11 with JSON support
