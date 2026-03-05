# T019: Test Plan

## Unit Tests (Vitest + React Testing Library)

### ForecastListPage.test.jsx

1. **testPageRendersFilters**
   - Render page
   - Assert month, channel, version dropdowns visible

2. **testMonthChannelSelection_LoadsVersions**
   - Mock fetchVersions API
   - Select month and channel
   - Assert fetchVersions called with correct params
   - Assert version dropdown populated

3. **testVersionSelection_LoadsData**
   - Mock fetchForecast API
   - Select month, channel, version
   - Assert fetchForecast called
   - Assert table displays data

4. **testDefaultsToLatestVersion**
   - Mock versions: [v1, v2, v3]
   - Select month+channel
   - Assert v3 (latest) auto-selected
   - Assert data fetched for v3

5. **testTableDisplaysData**
   - Mock API to return sample items
   - Render page with data
   - Assert table shows correct rows and columns

6. **testModifiedItemsHighlighted**
   - Mock data with is_modified=TRUE item
   - Assert row has red styling

7. **testInlineEdit_Success**
   - Mock updateForecast API
   - Click quantity cell
   - Change value
   - Blur or press Enter
   - Assert API called
   - Assert success notification

8. **testInlineEdit_Validation**
   - Click quantity cell
   - Enter negative value
   - Assert validation error shown

9. **testInlineEdit_Escape**
   - Click quantity cell
   - Change value
   - Press Escape
   - Assert edit cancelled, no API call

10. **testAddItemButton_OpensDialog**
    - Mock user with create permission
    - Click "Add Item"
    - Assert dialog visible

11. **testAddItem_Success**
    - Open Add Item dialog
    - Fill form
    - Mock createForecast API to resolve
    - Click Save
    - Assert API called
    - Assert dialog closes
    - Assert success notification

12. **testAddItem_Validation**
    - Open dialog
    - Leave required fields empty
    - Click Save
    - Assert validation errors shown

13. **testAddItem_Duplicate**
    - Mock API to reject with 409 Conflict
    - Fill and submit form
    - Assert error notification: "Product already exists"

14. **testDeleteItem_Confirmation**
    - Click Delete button
    - Assert confirmation dialog appears

15. **testDeleteItem_Confirm**
    - Mock deleteForecast API
    - Open delete confirmation
    - Click Confirm
    - Assert API called
    - Assert item removed from table
    - Assert success notification

16. **testDeleteItem_Cancel**
    - Open delete confirmation
    - Click Cancel
    - Assert dialog closes, no API call

17. **testReadOnlyMode_ClosedMonth_SalesRole**
    - Mock user with sales role
    - Mock closed month
    - Render page
    - Assert edit/add/delete disabled

18. **testProductionPlannerOverride_ClosedMonth**
    - Mock user with production_planner role
    - Mock closed month
    - Assert edit/add/delete enabled

19. **testPermissionViewOnly**
    - Mock user with only view permission
    - Assert Add/Delete buttons hidden
    - Assert quantity cells not editable

20. **testPermissionViewOwn_WrongChannel**
    - Mock user with view_own for different channel
    - Select channel user doesn't own
    - Assert access denied or empty state

21. **testEmptyState**
    - Mock API to return empty array
    - Assert empty state message displayed

22. **testErrorHandling**
    - Mock API to reject
    - Assert error notification shown

### AddItemDialog.test.jsx

1. **testDialogRenders**
   - Render with open=true
   - Assert all form fields visible

2. **testFormValidation_RequiredFields**
   - Leave product_code empty
   - Submit form
   - Assert validation error

3. **testFormValidation_QuantityPositive**
   - Enter quantity=-5
   - Assert error: "Quantity must be positive"

4. **testFormSubmit_Success**
   - Fill all fields
   - Mock API to resolve
   - Submit form
   - Assert onSuccess callback called

5. **testFormSubmit_Error**
   - Mock API to reject
   - Submit form
   - Assert error displayed

6. **testCancelButton**
   - Click Cancel
   - Assert onClose callback called

### forecast.js API Service Tests

1. **testFetchForecast**
   - Mock axios.get
   - Call fetchForecast(month, channel, version)
   - Assert GET /api/sales-forecast with query params

2. **testFetchVersions**
   - Mock axios.get
   - Call fetchVersions(month, channel)
   - Assert GET /api/sales-forecast/versions

3. **testUpdateForecast**
   - Mock axios.put
   - Call updateForecast(id, quantity)
   - Assert PUT /api/sales-forecast/:id

4. **testCreateForecast**
   - Mock axios.post
   - Call createForecast(data)
   - Assert POST /api/sales-forecast

5. **testDeleteForecast**
   - Mock axios.delete
   - Call deleteForecast(id)
   - Assert DELETE /api/sales-forecast/:id

## Integration Tests
N/A - Frontend integration with backend tested via E2E tests. Component integration tested in unit tests with mocked API.

## E2E Tests (Playwright)

### forecast-list.spec.ts

1. **testFullViewWorkflow**
   - Login as user with view permission
   - Navigate to /sales-forecast
   - Select month "202601"
   - Select channel "大全聯"
   - Verify table displays data
   - Verify version dropdown shows all versions

2. **testVersionHistory**
   - Upload data (creates version 1)
   - Edit item (creates version 2)
   - Navigate to forecast list
   - Verify 2 versions in dropdown
   - Select version 1
   - Verify table shows old data
   - Select version 2
   - Verify table shows new data

3. **testInlineEdit**
   - Navigate to page with data
   - Click quantity cell
   - Enter new value "250"
   - Press Enter
   - Verify success notification
   - Verify cell shows new value
   - Verify row highlighted red

4. **testAddItem**
   - Navigate to page
   - Click "Add Item"
   - Fill form with valid data
   - Click Save
   - Verify dialog closes
   - Verify new row appears in table

5. **testDeleteItem**
   - Navigate to page with data
   - Click Delete on first item
   - Verify confirmation dialog
   - Click Confirm
   - Verify item removed from table

6. **testModifiedHighlight**
   - Upload data (all items is_modified=FALSE)
   - Edit one item
   - Navigate to forecast list
   - Verify edited item has red highlight

7. **testReadOnlyMode_SalesRole**
   - Close month via config API
   - Login as sales role user
   - Navigate to forecast list
   - Select closed month
   - Verify edit/add/delete disabled
   - Verify banner: "Month is closed"

8. **testProductionPlannerOverride**
   - Login as production planner
   - Select closed month
   - Verify edit/add/delete enabled
   - Edit item successfully

9. **testPermissionViewOwn**
   - Login as user with view_own for 大全聯
   - Verify channel dropdown shows only 大全聯
   - Select 大全聯 - data loads
   - Manually attempt to load 家樂福 - access denied

10. **testEmptyState**
    - Select month+channel with no data
    - Verify empty state message

## Test Data Setup

### Mock Data
- **Sample Forecast Items:**
  ```javascript
  [
    {
      id: 1,
      month: "202601",
      channel: "大全聯",
      category: "飲料類",
      spec: "600ml*24入",
      product_code: "P001",
      product_name: "可口可樂",
      warehouse_location: "A01",
      quantity: 100.50,
      version: "2026/01/15 14:30:00(大全聯)",
      is_modified: false
    },
    {
      id: 2,
      month: "202601",
      channel: "大全聯",
      category: "零食類",
      spec: "150g*12包",
      product_code: "P002",
      product_name: "樂事洋芋片",
      warehouse_location: "B02",
      quantity: 50.00,
      version: "2026/01/15 14:30:00(大全聯)",
      is_modified: true
    }
  ]
  ```

- **Versions:**
  ```javascript
  [
    {version: "2026/01/15 15:00:00(大全聯)", item_count: 150},
    {version: "2026/01/15 14:30:00(大全聯)", item_count: 148}
  ]
  ```

### E2E Test Data
- Create test users:
  - Sales role with view permission
  - Production planner role
  - Channel-specific user (view_own)
- Seed database with:
  - Open and closed months
  - Multiple versions of forecast data
  - Items with is_modified=TRUE and FALSE

## Mocking Strategy

### Unit Tests
- Mock all API calls: fetchForecast, fetchVersions, updateForecast, createForecast, deleteForecast
- Mock authentication context for role and permission checks
- Mock user channel ownership
- Use MSW or Vitest mocks for HTTP requests
- Mock MUI components if needed for specific tests

### E2E Tests
- Use real backend API (test environment)
- Seed database with test data
- Create real users with different roles and permissions
- Clean up test data after each test
- Use Playwright fixtures for common setup
