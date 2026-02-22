# T037: Database Migration - Material Demand

## Context
This task creates the database schema for storing material demand data calculated from production schedules. The material_demand table holds weekly material requirements per factory, including demand dates, quantities, expected deliveries, and inventory estimates. This data is populated by PDCA integration (T032) and used for material purchase planning.

## Goal
Create a Flyway migration to establish the `material_demand` table in the MariaDB database with proper indexes for efficient querying by week and factory.

## Scope

### In Scope
- Create `material_demand` table with all required columns
- Add composite index on (week_start, factory)
- Add index on material_code
- Decimal columns for quantities with precision 10,2
- Nullable last_purchase_date column
- Created_at and updated_at timestamp columns

### Out of Scope
- Data population (handled by T032 PDCA integration)
- Application layer entities or repositories
- Foreign key relationships to other tables
- Data archival or partitioning strategy

## Requirements
- Table name: `material_demand`
- Columns:
  - `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
  - `week_start` (DATE, NOT NULL) - Start date of the production week
  - `factory` (VARCHAR(50), NOT NULL) - Factory identifier
  - `material_code` (VARCHAR(50), NOT NULL) - Material product code
  - `material_name` (VARCHAR(200), NOT NULL) - Material description
  - `unit` (VARCHAR(20), NOT NULL) - Unit of measure (e.g., kg, pcs)
  - `last_purchase_date` (DATE, NULL) - Last time material was purchased
  - `demand_date` (DATE, NOT NULL) - Date material is needed
  - `expected_delivery` (DECIMAL(10,2), DEFAULT 0) - Expected incoming quantity
  - `demand_quantity` (DECIMAL(10,2), DEFAULT 0) - Required quantity
  - `estimated_inventory` (DECIMAL(10,2), DEFAULT 0) - Estimated stock level
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
  - `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)
- Indexes:
  - Composite index on (week_start, factory) for weekly queries
  - Index on material_code for material lookups
- Follows MariaDB 10.11 syntax

## Implementation Notes
- Migration file: `V10__create_material_demand.sql`
- Use DECIMAL(10,2) for all quantity fields to handle fractional amounts
- Composite index (week_start, factory) optimizes common query pattern
- Material_code index supports lookups and joins
- Default values of 0 for quantity fields simplify calculations
- last_purchase_date is nullable (may not have purchase history)

## Files to Change
- Create: `src/main/resources/db/migration/V10__create_material_demand.sql`

## Dependencies
- T001: Initial database setup and Flyway configuration
