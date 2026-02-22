# T017: Backend API - Forecast Query and Version History

## Context
Spring Boot 3.2.12 REST API with JWT authentication. This task implements query endpoints for retrieving sales forecast data with version history support and permission-based filtering.

## Goal
Implement RESTful API endpoints for querying sales forecast data by month, channel, and version, with support for listing all versions and permission-based access control.

## Scope

### In Scope
- GET /api/sales-forecast - List forecast items with filters
- GET /api/sales-forecast/versions - List all versions for month+channel
- Filter by month, channel, version (optional, defaults to latest)
- Permission-based filtering: all channels vs. own channels only
- Sort results by category, spec, product_code
- Return all item details including is_modified flag

### Out of Scope
- Pagination (implement in future if needed)
- Advanced filtering (product name search, quantity range)
- Aggregation or summary statistics
- Real-time updates or WebSocket support
- Export functionality (covered in T021)

## Requirements
- **GET /api/sales-forecast**: Accept query params: month (required), channel (required), version (optional)
- If version not specified, return latest version for the month+channel
- Permission check:
  - sales_forecast.view: access all channels
  - sales_forecast.view_own: access only user's own channels
- Sort results: ORDER BY category ASC, spec ASC, product_code ASC
- Return array of forecast items with all fields
- **GET /api/sales-forecast/versions**: Accept query params: month (required), channel (required)
- Return list of distinct versions for the month+channel
- Include metadata: version string, upload/edit timestamp, total items count
- Sort versions DESC (newest first)
- Permission-based filtering same as main query
- Return 400 Bad Request if required params missing
- Return empty array if no data found

## Implementation Notes
- Use Spring Boot @RestController
- Service layer handles query logic and permission filtering
- Repository query methods:
  - findByMonthAndChannelAndVersion()
  - findByMonthAndChannel() with max version subquery
  - findDistinctVersionsByMonthAndChannel()
- For "latest version", use MAX(version) or ORDER BY version DESC LIMIT 1
- Category sorting: natural order (treat as string)
- Apply permission filtering in service layer based on JWT claims
- Use @PreAuthorize for permission checks
- DTOs: ForecastQueryParams, ForecastResponse, VersionInfo
- Consider using Spring Data JPA Specifications for dynamic queries
- Log all query operations for audit trail

## Files to Change
- `src/main/java/com/servtech/sinker/controller/SalesForecastQueryController.java` (new, or extend SalesForecastController)
- `src/main/java/com/servtech/sinker/service/SalesForecastQueryService.java` (new)
- `src/main/java/com/servtech/sinker/repository/SalesForecastRepository.java` (update - add query methods)
- `src/main/java/com/servtech/sinker/dto/forecast/ForecastQueryParams.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/VersionInfo.java` (new)

## Dependencies
- T014: sales_forecast table must exist with version column
- T002: Authentication and permission system
- Spring Data JPA for query methods
