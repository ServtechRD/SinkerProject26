# T011: Database Migration - Forecast Config

## Context
Spring Boot 3.2.12 + MariaDB 10.11 project using Flyway for database migrations. This task establishes the foundational table for managing sales forecast monthly configuration, including auto-close functionality.

## Goal
Create a Flyway migration script to establish the `sales_forecast_config` table that stores per-month configuration for sales forecast management, including auto-close day and closure status.

## Scope

### In Scope
- Create `sales_forecast_config` table with all required columns
- Define primary key and unique constraints
- Set appropriate default values
- Create indexes for query optimization

### Out of Scope
- Application code or API layer
- Initial data seeding
- Audit triggers or soft deletes

## Requirements
- Table name: `sales_forecast_config`
- Columns:
  - `id`: INT, PRIMARY KEY, AUTO_INCREMENT
  - `month`: VARCHAR(7), UNIQUE, format YYYYMM (e.g., "202501")
  - `auto_close_day`: INT, DEFAULT 10, valid range 1-31
  - `is_closed`: BOOLEAN, DEFAULT FALSE
  - `closed_at`: TIMESTAMP NULL, populated when is_closed becomes TRUE
  - `created_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- Unique constraint on `month` column to prevent duplicate configurations
- Index on `is_closed` for scheduler queries
- Index on `auto_close_day` for daily auto-close job

## Implementation Notes
- Use Flyway versioned migration V4__create_sales_forecast_config.sql
- Follow project naming conventions for timestamps (created_at, updated_at)
- Use MariaDB-compatible syntax
- Consider adding CHECK constraint for auto_close_day range if MariaDB version supports it
- NULL allowed for closed_at to distinguish between never-closed and closed states

## Files to Change
- `src/main/resources/db/migration/V4__create_sales_forecast_config.sql` (new)

## Dependencies
- T001: Must be completed first (foundational database setup)
- Requires Flyway configuration to be working
- MariaDB 10.11 database instance
