# T028: Backend - Production Plan API

## Context
The production planning module enables users to create and manage annual production plans broken down by product, sales channel, and month (February through December). The system automatically calculates total quantities and differences from forecasts, supporting buffer inventory planning and detailed remarks.

## Goal
Implement REST API endpoints to query annual production plans and edit monthly allocations with automatic calculation of totals and differences.

## Scope

### In Scope
- GET endpoint to query production plans by year
- PUT endpoint to update monthly allocations, buffer quantity, and remarks
- Automatic calculation of total_quantity (sum of monthly_allocation values + buffer_quantity)
- Automatic calculation of difference (total_quantity - original_forecast)
- JSON handling for monthly_allocation field (months 2-12)
- Permission-based access control (production_plan.view, production_plan.edit)
- Return data organized by product with all channels
- Validation of monthly allocation data

### Out of Scope
- Bulk import/export functionality
- Copy plan from previous year
- Forecast integration (original_forecast set manually or via separate process)
- Delete operations
- Plan approval workflow
- Reporting/analytics endpoints

## Requirements

### GET /api/production-plan
- **Query Parameters**:
  - year (required): Integer year (e.g., 2026)
- **Response**: List of ProductionPlanDTO grouped by product
- **Permission**: production_plan.view
- **Business Logic**:
  - Fetch all records for specified year
  - Return all channels for each product
  - Order by product_code, then channel

### PUT /api/production-plan/:id
- **Request Body**:
  ```json
  {
    "monthlyAllocation": {
      "2": 100.00,
      "3": 150.00,
      ...
      "12": 200.00
    },
    "bufferQuantity": 50.00,
    "remarks": "Adjusted for seasonal demand"
  }
  ```
- **Permission**: production_plan.edit
- **Business Logic**:
  - Load existing record by ID
  - Update monthly_allocation, buffer_quantity, remarks
  - Calculate total_quantity = sum(monthly_allocation values) + buffer_quantity
  - Calculate difference = total_quantity - original_forecast
  - Save updated record
  - Return updated ProductionPlanDTO
- **Validation**:
  - ID must exist
  - monthlyAllocation keys must be strings "2" through "12" only
  - monthlyAllocation values must be valid DECIMAL(10,2)
  - bufferQuantity must be valid DECIMAL(10,2), >= 0
  - remarks max length 65535 characters (TEXT field)

## Implementation Notes
- Use Spring Data JPA for repository layer
- Map JSON column to Map<String, BigDecimal> in entity using @Convert or @Type
- ProductionPlanDTO should include calculated fields (total_quantity, difference)
- Validation: use @Valid and custom validator for monthly allocation
- Month keys are strings "2"-"12" representing February-December
- Missing months in JSON should be treated as 0
- Use @Transactional for update operations
- Return 404 if ID not found
- Return 400 if validation fails
- Log all updates for audit trail

## Files to Change
- `backend/src/main/java/com/servtech/sinker/controller/ProductionPlanController.java` (new)
- `backend/src/main/java/com/servtech/sinker/service/ProductionPlanService.java` (new)
- `backend/src/main/java/com/servtech/sinker/entity/ProductionPlan.java` (new)
- `backend/src/main/java/com/servtech/sinker/repository/ProductionPlanRepository.java` (new)
- `backend/src/main/java/com/servtech/sinker/dto/ProductionPlanDTO.java` (new)
- `backend/src/main/java/com/servtech/sinker/dto/UpdateProductionPlanRequest.java` (new)
- `backend/src/main/java/com/servtech/sinker/converter/MonthlyAllocationConverter.java` (new, for JSON mapping)
- `backend/src/main/java/com/servtech/sinker/validator/MonthlyAllocationValidator.java` (new)

## Dependencies
- T027: Database table must exist
- T024: Inventory integration may provide original_forecast data (loosely coupled)
- Spring Security for permission checking
- Jackson for JSON serialization/deserialization
