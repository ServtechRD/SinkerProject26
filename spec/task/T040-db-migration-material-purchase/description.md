# T040: Database Migration - Material Purchase

## Context
This task creates the database schema for storing material purchase planning data. The material_purchase table holds weekly purchase requirements per factory, including product quantities, semi-product information, BOM calculations (kg per box, basket quantity, barrels required), and ERP integration status. This table supports the material purchase workflow and ERP order triggering.

## Goal
Create a Flyway migration to establish the `material_purchase` table in the MariaDB database with proper indexes for efficient querying by week and factory, and columns to track ERP integration status.

## Scope

### In Scope
- Create `material_purchase` table with all required columns
- Add composite index on (week_start, factory)
- Add index on product_code
- Decimal columns for quantities and calculations with precision 10,2
- Boolean flag for ERP trigger status
- ERP order number storage
- Created_at and updated_at timestamp columns

### Out of Scope
- Data population (handled by weekly schedule integration)
- Application layer entities or repositories
- Foreign key relationships to other tables
- Audit trail for ERP triggers

## Requirements
- Table name: `material_purchase`
- Columns:
  - `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
  - `week_start` (DATE, NOT NULL) - Start date of the production week
  - `factory` (VARCHAR(50), NOT NULL) - Factory identifier
  - `product_code` (VARCHAR(50), NOT NULL) - Product code
  - `product_name` (VARCHAR(200), NOT NULL) - Product description
  - `quantity` (DECIMAL(10,2), NOT NULL) - Production quantity
  - `semi_product_name` (VARCHAR(200), NOT NULL) - Semi-product name
  - `semi_product_code` (VARCHAR(100), NOT NULL) - Semi-product code
  - `kg_per_box` (DECIMAL(10,2), DEFAULT 0) - Kilograms per box from BOM
  - `basket_quantity` (DECIMAL(10,2), DEFAULT 0) - Calculated: quantity × kg_per_box
  - `boxes_per_barrel` (DECIMAL(10,2), DEFAULT 0) - Boxes per barrel from BOM
  - `required_barrels` (DECIMAL(10,2), DEFAULT 0) - Calculated: basket_quantity / boxes_per_barrel
  - `is_erp_triggered` (BOOLEAN, DEFAULT FALSE) - ERP order creation status
  - `erp_order_no` (VARCHAR(100), NULL) - ERP order number
  - `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
  - `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)
- Indexes:
  - Composite index on (week_start, factory)
  - Index on product_code
- Follows MariaDB 10.11 syntax

## Implementation Notes
- Migration file: `V11__create_material_purchase.sql`
- Use DECIMAL(10,2) for all quantity and calculation fields
- Boolean is_erp_triggered defaults to FALSE
- erp_order_no is nullable (only set after ERP trigger)
- Composite index (week_start, factory) optimizes weekly queries
- product_code index supports lookups and filtering

## Files to Change
- Create: `src/main/resources/db/migration/V11__create_material_purchase.sql`

## Dependencies
- T001: Initial database setup and Flyway configuration
