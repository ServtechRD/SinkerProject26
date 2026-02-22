# T030: Database Migration - Weekly Production Schedule Table

## Context
The weekly production schedule module requires a database table to store factory production schedules uploaded from Excel files. The schedules are organized by week (starting Monday) and factory, containing demand dates and product quantities for production planning.

## Goal
Create a Flyway migration to establish the `production_weekly_schedule` table with all necessary columns, indexes, and constraints to support weekly schedule management and queries.

## Scope

### In Scope
- Create `production_weekly_schedule` table with all specified columns
- Composite index on (week_start, factory) for query performance
- Index on product_code for product-based queries
- Set appropriate column types, lengths, and defaults
- Include audit timestamps (created_at, updated_at)
- Date validation constraint (week_start must be Monday)

### Out of Scope
- Data population or seeding
- Stored procedures for week validation
- Triggers for data change tracking
- Views or materialized views
- Data migration from existing tables

## Requirements
- Table name: `production_weekly_schedule`
- Primary key: `id` (INT, auto-increment)
- week_start: DATE, NOT NULL (must be Monday)
- factory: VARCHAR(50), NOT NULL
- demand_date: DATE, NOT NULL (actual production demand date)
- product_code: VARCHAR(50), NOT NULL
- product_name: VARCHAR(200), NOT NULL
- warehouse_location: VARCHAR(50), NOT NULL
- quantity: DECIMAL(10,2), NOT NULL, default 0
- Timestamps: created_at, updated_at
- Composite index: idx_weekly_schedule_week_factory on (week_start, factory)
- Index: idx_weekly_schedule_product on (product_code)
- CHECK constraint: DAYOFWEEK(week_start) = 2 (Monday validation)

## Implementation Notes
- Follow existing Flyway migration naming convention (V8__create_weekly_schedule.sql)
- Use MariaDB 10.11 compatible SQL syntax
- week_start represents the Monday that starts the week
- demand_date is the actual date when production is needed
- quantity uses DECIMAL(10,2) for precise calculations
- CHECK constraint ensures week_start is always Monday (DAYOFWEEK = 2 in MariaDB)
- factory field supports multiple production facilities
- On upload, existing records for week_start + factory combination are deleted and replaced

## Files to Change
- `backend/src/main/resources/db/migration/V8__create_weekly_schedule.sql` (new file)

## Dependencies
- T001: Initial database setup and Flyway configuration must be complete
- MariaDB 10.11 database instance must be available
