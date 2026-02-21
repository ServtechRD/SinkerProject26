# T016: Test Plan

## Unit Tests

### SalesForecastServiceTest

1. **testCreateForecast_Success**
   - Mock ErpProductService to return true
   - Mock config repository to return open month
   - Mock user owns channel
   - Call createForecast()
   - Assert repository save() called with correct data
   - Assert is_modified=TRUE
   - Assert version format correct

2. **testCreateForecast_MonthClosed**
   - Mock config with is_closed=TRUE
   - Assert throws ForbiddenException

3. **testCreateForecast_UserDoesNotOwnChannel**
   - Mock user channels without target channel
   - Assert throws ForbiddenException

4. **testCreateForecast_InvalidProduct**
   - Mock ErpProductService to return false
   - Assert throws BadRequestException with product code

5. **testCreateForecast_DuplicateProduct**
   - Mock repository findByMonthAndChannelAndProductCode() to return existing item
   - Assert throws ConflictException

6. **testCreateForecast_NegativeQuantity**
   - Call with quantity=-10
   - Assert throws BadRequestException

7. **testCreateForecast_GeneratesVersion**
   - Mock current timestamp: 2026-01-15 14:30:00
   - Call with channel="家樂福"
   - Assert version = "2026/01/15 14:30:00(家樂福)"

8. **testUpdateForecast_Success**
   - Mock repository findById() to return existing item
   - Mock month is open
   - Call updateForecast(id, new_quantity)
   - Assert quantity updated
   - Assert is_modified=TRUE
   - Assert new version generated

9. **testUpdateForecast_NotFound**
   - Mock repository findById() to return empty
   - Assert throws NotFoundException

10. **testUpdateForecast_MonthClosed**
    - Mock existing item with closed month
    - Assert throws ForbiddenException

11. **testUpdateForecast_UserDoesNotOwnChannel**
    - Mock existing item with different channel
    - Assert throws ForbiddenException

12. **testDeleteForecast_Success**
    - Mock repository findById() to return existing item
    - Mock month is open
    - Call deleteForecast(id)
    - Assert repository delete() called

13. **testDeleteForecast_NotFound**
    - Mock repository findById() to return empty
    - Assert throws NotFoundException

14. **testDeleteForecast_MonthClosed**
    - Mock existing item with closed month
    - Assert throws ForbiddenException

## Integration Tests

### SalesForecastControllerIntegrationTest (Spring Boot Test + Testcontainers)

1. **testCreateForecast_Success**
   - Create open month in database
   - POST /api/sales-forecast with valid JWT and data
   - Assert 201 Created
   - Assert response contains all fields
   - Query database and verify item created with is_modified=TRUE

2. **testCreateForecast_Unauthorized**
   - POST without JWT
   - Assert 401 Unauthorized

3. **testCreateForecast_NoPermission**
   - POST with JWT missing sales_forecast.create permission
   - Assert 403 Forbidden

4. **testCreateForecast_DoesNotOwnChannel**
   - POST with user who owns different channel
   - Assert 403 Forbidden

5. **testCreateForecast_MonthClosed**
   - Create closed month
   - POST to create item
   - Assert 403 Forbidden

6. **testCreateForecast_Duplicate**
   - Create item with P001 for 202601 + 大全聯
   - POST same product again
   - Assert 409 Conflict

7. **testCreateForecast_InvalidProduct**
   - Mock ErpProductService to return false
   - POST with invalid product
   - Assert 400 Bad Request

8. **testCreateForecast_NegativeQuantity**
   - POST with quantity=-5
   - Assert 400 Bad Request

9. **testUpdateForecast_Success**
   - Create item with quantity=100
   - PUT /api/sales-forecast/:id with quantity=150
   - Assert 200 OK
   - Assert response shows quantity=150
   - Query database and verify update

10. **testUpdateForecast_NotFound**
    - PUT /api/sales-forecast/99999
    - Assert 404 Not Found

11. **testUpdateForecast_MonthClosed**
    - Create item in open month
    - Close the month
    - Attempt update
    - Assert 403 Forbidden

12. **testUpdateForecast_NoPermission**
    - PUT with JWT missing sales_forecast.edit permission
    - Assert 403 Forbidden

13. **testUpdateForecast_GeneratesNewVersion**
    - Create item
    - Wait 1 second
    - Update item
    - Query database
    - Assert version timestamp is later than original

14. **testDeleteForecast_Success**
    - Create item
    - DELETE /api/sales-forecast/:id
    - Assert 204 No Content
    - Query database and verify item deleted

15. **testDeleteForecast_NotFound**
    - DELETE /api/sales-forecast/99999
    - Assert 404 Not Found

16. **testDeleteForecast_MonthClosed**
    - Create item in open month
    - Close the month
    - Attempt delete
    - Assert 403 Forbidden

17. **testDeleteForecast_NoPermission**
    - DELETE with JWT missing sales_forecast.delete permission
    - Assert 403 Forbidden

18. **testIsModifiedFlag**
    - Create item via POST
    - Query database
    - Assert is_modified=TRUE
    - Update item via PUT
    - Query database
    - Assert is_modified still TRUE

## E2E Tests
N/A - Backend CRUD operations are thoroughly tested via integration tests. E2E tests will cover the full workflow including frontend in T019.

## Test Data Setup
- Use Testcontainers MariaDB 10.11
- Create test users with various permissions:
  - User with all permissions (create/edit/delete)
  - User with only view permission
  - User with create but not edit
- Create user-channel mappings
- Create open and closed month configurations
- Sample forecast data:
  ```json
  {
    "month": "202601",
    "channel": "大全聯",
    "category": "飲料類",
    "spec": "600ml*24入",
    "product_code": "P001",
    "product_name": "可口可樂",
    "warehouse_location": "A01",
    "quantity": 100.50
  }
  ```

## Mocking Strategy
- **Unit Tests:**
  - Mock SalesForecastRepository
  - Mock SalesForecastConfigRepository
  - Mock ErpProductService (stub always returns true)
  - Mock authentication context for channel ownership
  - Mock Clock for consistent version timestamps

- **Integration Tests:**
  - No mocking of Spring components
  - Use real database via Testcontainers
  - Use real transaction management
  - Mock ErpProductService as @MockBean (stub)
  - Use real JWT tokens for authentication
  - Test with actual permissions in security context
