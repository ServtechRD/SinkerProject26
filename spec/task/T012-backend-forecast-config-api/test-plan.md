# T012: Test Plan

## Unit Tests

### SalesForecastConfigServiceTest
1. **testBatchCreateMonths_Success**
   - Input: start_month=202501, end_month=202503
   - Mock repository save operations
   - Assert 3 months created with correct values
   - Verify each month has default auto_close_day=10, is_closed=FALSE

2. **testBatchCreateMonths_SingleMonth**
   - Input: start_month=202501, end_month=202501
   - Assert 1 month created

3. **testBatchCreateMonths_InvalidRange**
   - Input: start_month=202503, end_month=202501
   - Assert throws IllegalArgumentException

4. **testBatchCreateMonths_DuplicateHandling**
   - Mock repository to throw DataIntegrityViolationException for duplicate
   - Assert appropriate exception handling (skip or report)

5. **testUpdateConfig_ChangeClosedToTrue**
   - Existing config: is_closed=FALSE, closed_at=NULL
   - Update: is_closed=TRUE
   - Assert closed_at is set to current timestamp (within 1 second tolerance)

6. **testUpdateConfig_ChangeClosedToFalse**
   - Existing config: is_closed=TRUE, closed_at=2025-01-15T10:00:00
   - Update: is_closed=FALSE
   - Assert closed_at is NULL

7. **testUpdateConfig_ClosedUnchanged**
   - Existing config: is_closed=TRUE, closed_at=2025-01-15T10:00:00
   - Update: auto_close_day=20 (is_closed not changed)
   - Assert closed_at unchanged

8. **testUpdateConfig_AutoCloseDayValidation**
   - Input: auto_close_day=0
   - Assert throws IllegalArgumentException
   - Input: auto_close_day=32
   - Assert throws IllegalArgumentException

9. **testListAllConfigs**
   - Mock repository to return sample configs
   - Assert service returns all configs sorted by month DESC

### AutoCloseSchedulerTest
1. **testAutoCloseScheduler_MatchingDay**
   - Setup: Current day = 15, config with auto_close_day=15, is_closed=FALSE
   - Execute scheduler
   - Assert config is_closed=TRUE, closed_at populated

2. **testAutoCloseScheduler_NoMatch**
   - Setup: Current day = 10, config with auto_close_day=15
   - Execute scheduler
   - Assert config unchanged

3. **testAutoCloseScheduler_AlreadyClosed**
   - Setup: Current day = 10, config with auto_close_day=10, is_closed=TRUE
   - Execute scheduler
   - Assert config unchanged (not closed again)

4. **testAutoCloseScheduler_MultipleMonths**
   - Setup: 3 configs with auto_close_day matching current day
   - Execute scheduler
   - Assert all 3 configs closed

## Integration Tests

### SalesForecastConfigControllerIntegrationTest (Spring Boot Test + Testcontainers)

1. **testCreateMonths_Success**
   - POST /api/sales-forecast/config with valid JWT and permission
   - Request: start_month=202601, end_month=202603
   - Assert 201 Created
   - Verify 3 records in database

2. **testCreateMonths_Unauthorized**
   - POST without JWT token
   - Assert 401 Unauthorized

3. **testCreateMonths_Forbidden**
   - POST with JWT but missing sales_forecast_config.edit permission
   - Assert 403 Forbidden

4. **testCreateMonths_InvalidFormat**
   - POST with start_month="2026-01"
   - Assert 400 Bad Request with validation error

5. **testCreateMonths_Duplicate**
   - POST same month range twice
   - Assert second request handles gracefully (409 or success with skip)

6. **testListConfigs_Success**
   - Create test data: 5 configs
   - GET /api/sales-forecast/config with valid JWT
   - Assert 200 OK with 5 configs sorted DESC

7. **testListConfigs_Forbidden**
   - GET without sales_forecast_config.view permission
   - Assert 403 Forbidden

8. **testUpdateConfig_SetClosed**
   - Create config with is_closed=FALSE
   - PUT /api/sales-forecast/config/{id} with is_closed=TRUE
   - Assert 200 OK
   - Verify closed_at populated in database

9. **testUpdateConfig_SetOpen**
   - Create config with is_closed=TRUE, closed_at populated
   - PUT with is_closed=FALSE
   - Verify closed_at is NULL in database

10. **testUpdateConfig_NotFound**
    - PUT /api/sales-forecast/config/99999
    - Assert 404 Not Found

11. **testUpdateConfig_InvalidAutoCloseDay**
    - PUT with auto_close_day=50
    - Assert 400 Bad Request

12. **testSchedulerIntegration**
    - Create config with auto_close_day matching test execution day
    - Manually trigger scheduler method (or use @Scheduled with test profile)
    - Verify config auto-closed

## E2E Tests
N/A - E2E tests will be implemented in T013 when frontend is complete. Backend API is tested thoroughly via integration tests with real database.

## Test Data Setup
- Use Testcontainers MariaDB 10.11 for integration tests
- Create test user with sales_forecast_config.view and sales_forecast_config.edit permissions
- Generate valid JWT tokens for authentication
- Use @BeforeEach to create clean test data
- Use @AfterEach or @Transactional rollback for cleanup

## Mocking Strategy
- **Unit Tests:** Mock repository layer, use Mockito
- **Integration Tests:** No mocking - use real database via Testcontainers, real Spring Security context
- Mock current time for scheduler tests using Clock or @MockBean for TimeService
- Do not mock Spring framework components in integration tests
