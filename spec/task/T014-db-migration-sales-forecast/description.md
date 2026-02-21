# T014: Database Migration - Sales Forecast Table

## Context
Spring Boot 3.2.12 + MariaDB 10.11 project using Flyway migrations. This task creates the core table for storing sales forecast data uploaded via Excel or manually entered, supporting versioning and modification tracking.

## Goal
Create a Flyway migration script to establish the `sales_forecast` table that stores detailed sales forecast entries per month, channel, and product, with support for versioning and tracking manual modifications.

## Scope

### In Scope
- Create `sales_forecast` table with all required columns
- Define primary key and indexes for query optimization
- Support for version tracking and modification flags
- Indexes for common query patterns (month+channel, product_code, version)

### Out of Scope
- Foreign key constraints to other tables
- Triggers or stored procedures
- Data seeding
- Archive or history tables

## Requirements
- Table name: `sales_forecast`
- Columns:
  - `id`: INT, PRIMARY KEY, AUTO_INCREMENT
  - `month`: VARCHAR(7), format YYYYMM
  - `channel`: VARCHAR(50), one of 12 channels
  - `category`: VARCHAR(100), 中類名稱
  - `spec`: VARCHAR(200), 貨品規格
  - `product_code`: VARCHAR(50), 品號
  - `product_name`: VARCHAR(200), 品名
  - `warehouse_location`: VARCHAR(50), 庫位
  - `quantity`: DECIMAL(10,2), 箱數小計
  - `version`: VARCHAR(100), format "YYYY/MM/DD HH:MM:SS(通路名稱)"
  - `is_modified`: BOOLEAN, DEFAULT FALSE, indicates manual edit
  - `created_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at`: TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- Composite index on (month, channel) for filtering
- Index on (product_code) for product lookups
- Index on (version) for version queries
- Index on (month, channel, product_code) for duplicate detection
- All text columns use utf8mb4 charset for Chinese characters

## Implementation Notes
- Use Flyway versioned migration V5__create_sales_forecast.sql
- DECIMAL(10,2) for quantity supports up to 99,999,999.99 units
- VARCHAR lengths accommodate longest expected values
- Indexes optimize for: list by month+channel, version history, product search
- Consider index on is_modified for highlighting modified records
- No unique constraints - allow duplicates for different versions
- Use MariaDB-compatible syntax

## Files to Change
- `src/main/resources/db/migration/V5__create_sales_forecast.sql` (new)

## Dependencies
- T001: Foundational database setup must be complete
- Flyway configuration must be working
- MariaDB 10.11 database instance
