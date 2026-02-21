# T024: Backend - Inventory Integration API

## Context
The inventory integration module combines sales forecast data (from T020), real-time ERP inventory data, and real-time sales data to generate production planning insights. The system supports both real-time queries (which fetch fresh data and save it with a version) and historical version-based queries (which retrieve previously saved snapshots).

## Goal
Implement a REST API endpoint that aggregates forecast, inventory, and sales data, calculates production requirements, and manages versioned snapshots of this integrated data.

## Scope

### In Scope
- GET endpoint with query parameters: month, start_date, end_date, version
- Real-time query mode: aggregates 12-channel forecast, calls ERP APIs, calculates production_subtotal
- Version query mode: retrieves previously saved data
- Automatic version generation and data persistence
- Permission-based access control (inventory.view)
- Integration with sales forecast service (T020)
- ERP API stub implementation for inventory and sales data

### Out of Scope
- Actual ERP system integration (stub only)
- Editing of integrated data (covered in T025)
- Frontend implementation
- Export functionality
- Batch processing

## Requirements
- **Endpoint**: GET /api/inventory-integration
- **Query Parameters**:
  - month (required): YYYY-MM format
  - start_date (optional): YYYY-MM-DD format
  - end_date (optional): YYYY-MM-DD format
  - version (optional): version identifier
- **Query Logic**:
  - If version is provided: load data from inventory_sales_forecast table by version
  - If version is NOT provided (real-time query):
    - Fetch forecast data from sales_forecast table, sum all 12 channels per product
    - Call ErpInventoryService.getInventoryBalance(productCode, month)
    - Call ErpInventoryService.getSalesQuantity(productCode, startDate, endDate)
    - Calculate: production_subtotal = forecast_quantity - inventory_balance - sales_quantity
    - Generate new version identifier (timestamp-based UUID)
    - Save all results to inventory_sales_forecast table with version
    - Return saved data
- **Permission**: Requires inventory.view permission
- **Response**: List of InventoryIntegrationDTO with all fields from table
- **Calculation Formula**: production_subtotal = forecast_quantity - inventory_balance - sales_quantity

## Implementation Notes
- Use Spring Security for permission checking (@PreAuthorize)
- Version format: "v{timestamp}" or UUID for uniqueness
- ERP stub returns mock data based on product code patterns
- Transaction management: save all records in single transaction
- Error handling: if ERP call fails, log error and use 0 as default
- Aggregate forecast: SUM(ch1+ch2+...+ch12) GROUP BY product_code, product_name, category, spec, warehouse_location
- Date range for ERP sales query defaults to month boundaries if not provided
- Return results sorted by product_code

## Files to Change
- `backend/src/main/java/com/servtech/sinker/controller/InventoryIntegrationController.java` (new)
- `backend/src/main/java/com/servtech/sinker/service/InventoryIntegrationService.java` (new)
- `backend/src/main/java/com/servtech/sinker/service/ErpInventoryService.java` (new, stub)
- `backend/src/main/java/com/servtech/sinker/entity/InventorySalesForecast.java` (new)
- `backend/src/main/java/com/servtech/sinker/repository/InventorySalesForecastRepository.java` (new)
- `backend/src/main/java/com/servtech/sinker/dto/InventoryIntegrationDTO.java` (new)

## Dependencies
- T023: Database table must exist
- T020: Sales forecast API and data must be available
- Spring Security configuration for permission checking
- Spring Data JPA for repository
