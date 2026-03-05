# T012: Backend API - Forecast Config Management

## Context
Spring Boot 3.2.12 REST API with JWT authentication. This task builds the backend API layer for managing monthly sales forecast configuration, including batch month creation, editing, and scheduled auto-close functionality.

## Goal
Implement RESTful API endpoints for sales forecast configuration management and a scheduled task that automatically closes months based on configured auto_close_day settings.

## Scope

### In Scope
- POST /api/sales-forecast/config - Batch create months
- GET /api/sales-forecast/config - List all configurations
- PUT /api/sales-forecast/config/:id - Update configuration
- Scheduled task for auto-closing months (daily at 00:00)
- Permission checks: sales_forecast_config.view, sales_forecast_config.edit
- Business logic for batch month generation
- Automatic closed_at timestamp management

### Out of Scope
- Frontend UI implementation
- Excel upload/download functionality
- Actual sales forecast data management
- Manual month deletion

## Requirements
- **POST /api/sales-forecast/config**: Accept start_month and end_month (YYYYMM format), create all months in range with default values
- **GET /api/sales-forecast/config**: Return all months sorted by month DESC
- **PUT /api/sales-forecast/config/:id**: Update auto_close_day (1-31) and/or is_closed (boolean)
- When is_closed changes from FALSE to TRUE, set closed_at to current timestamp
- When is_closed changes from TRUE to FALSE, clear closed_at to NULL
- Scheduled task runs daily at 00:00 using @Scheduled with cron expression
- Scheduler finds all open months where current day matches auto_close_day, sets is_closed=TRUE and closed_at
- Permission validation using Spring Security
- Input validation: month format YYYYMM, auto_close_day range 1-31
- Prevent duplicate month creation (handle gracefully)

## Implementation Notes
- Use Spring Boot @RestController for API endpoints
- Service layer handles business logic
- JPA Entity for SalesForecastConfig with @Entity annotation
- JpaRepository for database operations
- DTOs for request/response: CreateMonthsRequest, UpdateConfigRequest, ConfigResponse
- Use @Scheduled(cron = "0 0 0 * * *") for daily midnight execution
- Use @Transactional for data consistency
- Apply @PreAuthorize for permission checks
- Validate month format with regex: ^\d{6}$
- Handle unique constraint violations gracefully
- Log scheduler execution and auto-close actions

## Files to Change
- `src/main/java/com/servtech/sinker/controller/SalesForecastConfigController.java` (new)
- `src/main/java/com/servtech/sinker/service/SalesForecastConfigService.java` (new)
- `src/main/java/com/servtech/sinker/entity/SalesForecastConfig.java` (new)
- `src/main/java/com/servtech/sinker/repository/SalesForecastConfigRepository.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/CreateMonthsRequest.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/UpdateConfigRequest.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/ConfigResponse.java` (new)
- `src/main/java/com/servtech/sinker/scheduler/AutoCloseScheduler.java` (new)

## Dependencies
- T011: Database table must exist
- T002: Authentication and permission system must be in place
- Spring Boot Starter Web, Data JPA, Security
- MariaDB connector
