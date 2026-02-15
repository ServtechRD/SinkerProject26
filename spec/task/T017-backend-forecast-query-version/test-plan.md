# T017: Test Plan

## Unit Tests

### SalesForecastQueryServiceTest

1. **testQueryForecast_WithVersion**
   - Mock repository findByMonthAndChannelAndVersion()
   - Call queryForecast(month, channel, version)
   - Assert returns items from specific version
   - Assert sorted by category, spec, product_code

2. **testQueryForecast_LatestVersion**
   - Mock repository to return items with multiple versions
   - Call queryForecast(month, channel, null)
   - Assert returns only items from latest version
   - Verify latest determined by MAX(version)

3. **testQueryForecast_PermissionViewAll**
   - Mock user has sales_forecast.view permission
   - Call with any channel
   - Assert succeeds

4. **testQueryForecast_PermissionViewOwn_OwnChannel**
   - Mock user has sales_forecast.view_own and owns channel
   - Call with owned channel
   - Assert succeeds

5. **testQueryForecast_PermissionViewOwn_OtherChannel**
   - Mock user has sales_forecast.view_own but doesn't own channel
   - Call with non-owned channel
   - Assert throws ForbiddenException

6. **testQueryForecast_NoPermission**
   - Mock user without view permissions
   - Assert throws ForbiddenException

7. **testQueryForecast_EmptyResult**
   - Mock repository to return empty list
   - Call queryForecast()
   - Assert returns empty list (not error)

8. **testQueryForecast_Sorting**
   - Mock repository to return unsorted items
   - Call queryForecast()
   - Assert result sorted: category ASC, spec ASC, product_code ASC

9. **testQueryVersions_Success**
   - Mock repository findDistinctVersionsByMonthAndChannel()
   - Call queryVersions(month, channel)
   - Assert returns list of versions sorted DESC

10. **testQueryVersions_WithItemCount**
    - Mock repository to return versions with counts
    - Call queryVersions()
    - Assert each version has correct item_count

11. **testQueryVersions_PermissionFiltering**
    - Mock user with view_own permission
    - Call with non-owned channel
    - Assert throws ForbiddenException

12. **testQueryVersions_EmptyResult**
    - Mock repository to return empty list
    - Assert returns empty list

## Integration Tests

### SalesForecastQueryControllerIntegrationTest (Spring Boot Test + Testcontainers)

1. **testQueryForecast_Success**
   - Insert test data with version "2026/01/15 14:30:00(大全聯)"
   - GET /api/sales-forecast?month=202601&channel=大全聯
   - Assert 200 OK
   - Assert returns all items
   - Verify sorted correctly

2. **testQueryForecast_SpecificVersion**
   - Insert data with 2 versions
   - GET with version parameter for older version
   - Assert returns only old version data

3. **testQueryForecast_LatestVersion**
   - Insert data with 3 versions
   - GET without version parameter
   - Assert returns only latest version

4. **testQueryForecast_Unauthorized**
   - GET without JWT
   - Assert 401 Unauthorized

5. **testQueryForecast_NoViewPermission**
   - GET with JWT missing view permissions
   - Assert 403 Forbidden

6. **testQueryForecast_ViewAllPermission**
   - Login with sales_forecast.view
   - GET for any channel
   - Assert 200 OK

7. **testQueryForecast_ViewOwnPermission_OwnChannel**
   - Login with sales_forecast.view_own for 家樂福
   - GET for channel 家樂福
   - Assert 200 OK

8. **testQueryForecast_ViewOwnPermission_OtherChannel**
   - Login with sales_forecast.view_own for 家樂福
   - GET for channel 大全聯
   - Assert 403 Forbidden

9. **testQueryForecast_MissingMonth**
   - GET /api/sales-forecast?channel=大全聯
   - Assert 400 Bad Request

10. **testQueryForecast_MissingChannel**
    - GET /api/sales-forecast?month=202601
    - Assert 400 Bad Request

11. **testQueryForecast_InvalidMonth**
    - GET /api/sales-forecast?month=2026-01&channel=大全聯
    - Assert 400 Bad Request

12. **testQueryForecast_EmptyResult**
    - GET for non-existent month+channel
    - Assert 200 OK with empty array

13. **testQueryForecast_Sorting**
    - Insert items: category C, B, A; spec 3, 2, 1; product P3, P2, P1
    - GET data
    - Assert sorted: category A (spec 1, 2, 3), B (spec 1, 2, 3), C (...)

14. **testQueryForecast_IsModifiedFlag**
    - Insert item with is_modified=FALSE
    - Insert item with is_modified=TRUE
    - GET data
    - Verify is_modified values in response

15. **testQueryVersions_Success**
    - Insert data with 3 different versions
    - GET /api/sales-forecast/versions?month=202601&channel=大全聯
    - Assert 200 OK
    - Assert returns 3 versions sorted DESC

16. **testQueryVersions_ItemCount**
    - Insert 10 items with version1
    - Insert 15 items with version2
    - GET versions
    - Assert version1 has count=10, version2 has count=15

17. **testQueryVersions_PermissionFiltering**
    - Login with view_own for different channel
    - GET versions
    - Assert 403 Forbidden

18. **testQueryVersions_MissingParams**
    - GET /api/sales-forecast/versions (no params)
    - Assert 400 Bad Request

19. **testQueryVersions_EmptyResult**
    - GET versions for non-existent month+channel
    - Assert 200 OK with empty array

20. **testPerformance_LargeDataset**
    - Insert 1000 items for same month+channel
    - GET data
    - Assert response time < 1 second

## E2E Tests
N/A - Backend query functionality is thoroughly tested via integration tests. E2E tests will cover frontend data display in T019.

## Test Data Setup
- Use Testcontainers MariaDB 10.11
- Create test users with different permissions:
  - Admin: sales_forecast.view (all channels)
  - Channel manager: sales_forecast.view_own (specific channels)
  - No permission user
- Create multiple versions of forecast data:
  - Version 1: Upload 10 items on 2026-01-10
  - Version 2: Upload 12 items on 2026-01-15
  - Version 3: Edit 1 item on 2026-01-20 (is_modified=TRUE)
- Sample data with various categories for sorting tests:
  ```
  Category: "飲料類", "零食類", "日用品"
  Spec: "600ml", "300ml", "1L"
  Product codes: "P001", "P002", "P003"
  ```

## Mocking Strategy
- **Unit Tests:**
  - Mock SalesForecastRepository query methods
  - Mock authentication context for permission checks
  - Mock user channel ownership data
  - Use ArgumentCaptor to verify query parameters

- **Integration Tests:**
  - No mocking of Spring components
  - Use real database via Testcontainers
  - Use real JWT tokens with embedded permissions
  - Test actual SQL query execution and performance
  - Use database indexes to optimize queries
