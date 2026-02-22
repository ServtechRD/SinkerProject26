# T016: Backend API - Forecast CRUD Operations

## Context
Spring Boot 3.2.12 REST API with JWT authentication. This task implements single-item CRUD operations for sales forecast data, allowing manual addition, editing, and deletion of individual forecast entries with proper version tracking.

## Goal
Implement RESTful API endpoints for creating, updating, and deleting individual sales forecast items, with automatic version generation and modification tracking.

## Scope

### In Scope
- POST /api/sales-forecast - Add single forecast item
- PUT /api/sales-forecast/:id - Edit quantity of existing item
- DELETE /api/sales-forecast/:id - Delete single item
- Set is_modified=TRUE for all manual operations
- Generate new version string for each operation
- Permission checks: sales_forecast.create/edit/delete + channel ownership
- Month open/closed validation
- Product validation via ERP (stub)
- Duplicate detection for add operation

### Out of Scope
- Batch operations (handled by Excel upload)
- Editing fields other than quantity
- Soft delete (use hard delete)
- Version history browsing (covered in T017)
- Undo/redo functionality

## Requirements
- **POST /api/sales-forecast**: Accept month, channel, category, spec, product_code, product_name, warehouse_location, quantity
- Validate product_code via ErpProductService
- Check for duplicate: same month+channel+product_code already exists
- Generate version string: "YYYY/MM/DD HH:MM:SS(通路名稱)"
- Set is_modified=TRUE
- Return created item with ID
- **PUT /api/sales-forecast/:id**: Accept only quantity field
- Update quantity and set is_modified=TRUE
- Generate new version string
- Return updated item
- **DELETE /api/sales-forecast/:id**: Hard delete from database
- Validate month is open for all operations
- Validate user owns channel for all operations
- Require appropriate permissions: sales_forecast.create, sales_forecast.edit, sales_forecast.delete
- Validate quantity is positive decimal
- Return 404 if item not found for PUT/DELETE

## Implementation Notes
- Use Spring Boot @RestController
- Service layer handles business logic and validation
- Repository layer uses SalesForecastRepository from T015
- DTOs: CreateForecastRequest, UpdateForecastRequest, ForecastResponse
- Apply @PreAuthorize for permission checks
- Additional channel ownership validation in service layer
- Use @Transactional for data consistency
- When creating/updating, generate new version with current timestamp
- Version format identical to upload: "YYYY/MM/DD HH:MM:SS(通路名稱)"
- Log all CRUD operations: user, action, item ID, timestamp
- Duplicate check query: findByMonthAndChannelAndProductCode()

## Files to Change
- `src/main/java/com/servtech/sinker/controller/SalesForecastController.java` (new)
- `src/main/java/com/servtech/sinker/service/SalesForecastService.java` (new)
- `src/main/java/com/servtech/sinker/repository/SalesForecastRepository.java` (update - add query methods)
- `src/main/java/com/servtech/sinker/dto/forecast/CreateForecastRequest.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/UpdateForecastRequest.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/ForecastResponse.java` (new)

## Dependencies
- T014: sales_forecast table must exist
- T012: sales_forecast_config for month validation
- T002: Authentication and permission system
- ErpProductService from T015 for product validation
