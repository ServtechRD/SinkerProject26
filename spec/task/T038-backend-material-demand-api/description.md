# T038: Backend API - Material Demand Query

## Context
This task implements a read-only REST API for querying material demand data that is populated by the PDCA integration system (T032). Users need to query material requirements by week and factory to understand what materials are needed and when. The demand_date is calculated by subtracting advance_days (from semi_product_advance_purchase table) from the production demand date.

## Goal
Create a REST API endpoint to query material demand data with filtering by week_start and factory, including calculation of demand_date based on production schedules and advance purchase days.

## Scope

### In Scope
- GET /api/material-demand - Query material demand with filters
- Query parameters: week_start, factory
- Read-only endpoint (data populated by T032)
- Permission check: material_demand.view
- JPA entity and repository
- DTO for response data
- Demand_date calculation: production demand_date - advance_days

### Out of Scope
- Create/Update/Delete operations (data managed by PDCA integration)
- Excel export (may be added later)
- Advanced filtering (material_code, date ranges)
- Aggregation or summary calculations
- Frontend implementation (T039)

## Requirements
- **Entity**: `MaterialDemand` with fields matching T037 schema
- **Repository**: `MaterialDemandRepository` extending JpaRepository
- **Controller**: `MaterialDemandController` with GET endpoint
- **Service**: `MaterialDemandService` with query logic
- **Endpoint**: GET /api/material-demand?week_start=2026-02-17&factory=F1
- **Permission**: material_demand.view
- **Response**: List of MaterialDemandDTO objects
- **Calculation**: demand_date = production_demand_date - advance_days (from semi_product_advance_purchase)
- **Validation**:
  - week_start required and valid date format
  - factory required and non-empty
- **Error Handling**: Return 400 for invalid parameters, 403 for permission denied

## Implementation Notes
- Use Spring Data JPA repository with custom query method: findByWeekStartAndFactory
- Demand_date calculation performed during data population (T032), not at query time
- Return DTOs to avoid lazy-loading issues with JPA entities
- Order results by material_code ascending
- Handle empty result set (return empty array, not error)
- Use LocalDate for date fields in Java
- Validate date format in controller with @DateTimeFormat annotation
- Permission check via @PreAuthorize annotation

## Files to Change
- Create: `src/main/java/com/servtech/sinker/entity/MaterialDemand.java`
- Create: `src/main/java/com/servtech/sinker/repository/MaterialDemandRepository.java`
- Create: `src/main/java/com/servtech/sinker/controller/MaterialDemandController.java`
- Create: `src/main/java/com/servtech/sinker/service/MaterialDemandService.java`
- Create: `src/main/java/com/servtech/sinker/dto/MaterialDemandDTO.java`

## Dependencies
- T037: Database migration for material_demand table
- T032: PDCA integration that populates the data
