# T041: Backend API - Material Purchase Query

## Context
This task implements a read-only REST API for querying material purchase data that is calculated from weekly production schedules and BOM (Bill of Materials) expansion. Users need to view purchase requirements by week and factory to understand material needs in terms of baskets and barrels. The BOM data source is TBD, so a stub service will be used initially.

## Goal
Create a REST API endpoint to query material purchase data with filtering by week_start and factory, including BOM-based calculations for basket quantities and required barrels.

## Scope

### In Scope
- GET /api/material-purchase - Query material purchase with filters
- Query parameters: week_start, factory
- Read-only endpoint (data populated from schedule + BOM expansion)
- Permission check: material_purchase.view
- JPA entity and repository
- DTO for response data
- BomService stub for BOM data lookup
- Calculations:
  - basket_quantity = quantity × kg_per_box
  - required_barrels = basket_quantity / boxes_per_barrel

### Out of Scope
- BOM data management API (stubbed for now)
- Create/Update/Delete operations
- ERP trigger functionality (T042)
- Excel export
- Frontend implementation (T043)
- Advanced filtering or aggregation

## Requirements
- **Entity**: `MaterialPurchase` with fields matching T040 schema
- **Repository**: `MaterialPurchaseRepository` extending JpaRepository
- **Controller**: `MaterialPurchaseController` with GET endpoint
- **Service**: `MaterialPurchaseService` with query logic
- **BOM Service**: `BomService` stub returning hardcoded BOM data
- **Endpoint**: GET /api/material-purchase?week_start=2026-02-17&factory=F1
- **Permission**: material_purchase.view
- **Response**: List of MaterialPurchaseDTO objects
- **Calculations**: Perform during data population or query time
- **Validation**:
  - week_start required and valid date format
  - factory required and non-empty
- **Error Handling**: Return 400 for invalid parameters, 403 for permission denied

## Implementation Notes
- Use Spring Data JPA repository with custom query: findByWeekStartAndFactory
- BomService stub returns sample data for known product codes, default values for unknown
- Calculations may be stored in database or calculated on-the-fly
- Return DTOs to avoid JPA lazy-loading issues
- Order results by product_code ascending
- Handle empty result set (return empty array)
- Use LocalDate for date fields in Java
- Validate date format with @DateTimeFormat annotation
- Permission check via @PreAuthorize annotation
- Include is_erp_triggered and erp_order_no in response

## Files to Change
- Create: `src/main/java/com/servtech/sinker/entity/MaterialPurchase.java`
- Create: `src/main/java/com/servtech/sinker/repository/MaterialPurchaseRepository.java`
- Create: `src/main/java/com/servtech/sinker/controller/MaterialPurchaseController.java`
- Create: `src/main/java/com/servtech/sinker/service/MaterialPurchaseService.java`
- Create: `src/main/java/com/servtech/sinker/service/BomService.java` (stub)
- Create: `src/main/java/com/servtech/sinker/dto/MaterialPurchaseDTO.java`

## Dependencies
- T040: Database migration for material_purchase table
- T038: Material demand API (conceptual dependency for workflow understanding)
