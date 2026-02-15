# T013: Test Plan

## Unit Tests (Vitest + React Testing Library)

### ForecastConfigPage.test.jsx
1. **testPageRendersLoading**
   - Mock API to delay response
   - Render ForecastConfigPage
   - Assert loading spinner is visible

2. **testPageRendersConfigTable**
   - Mock API to return sample configs
   - Render page
   - Assert table displays with correct data
   - Assert columns: Month, Auto Close Day, Status, Closed At, Actions

3. **testOpenStatusDisplaysGreen**
   - Mock config with is_closed=false
   - Assert status badge has green color and "Open" text

4. **testClosedStatusDisplaysRed**
   - Mock config with is_closed=true, closed_at populated
   - Assert status badge has red/gray color and "Closed" text
   - Assert closed_at formatted correctly

5. **testCreateButtonVisibleWithPermission**
   - Mock user with sales_forecast_config.edit permission
   - Assert "Create Months" button is visible

6. **testCreateButtonHiddenWithoutPermission**
   - Mock user without sales_forecast_config.edit permission
   - Assert "Create Months" button not rendered

7. **testEditButtonsHiddenWithoutPermission**
   - Mock user without sales_forecast_config.edit permission
   - Assert Edit buttons not rendered in Actions column

8. **testEmptyStateMessage**
   - Mock API to return empty array
   - Assert empty state message displayed

9. **testErrorHandling**
   - Mock API to reject with error
   - Assert error toast displayed with message

### CreateMonthsDialog.test.jsx
1. **testDialogOpensAndCloses**
   - Render with open=true
   - Assert dialog visible
   - Click Cancel
   - Assert onClose callback called

2. **testFormValidation_EmptyFields**
   - Render dialog
   - Leave fields empty
   - Assert Create button disabled

3. **testFormValidation_InvalidRange**
   - Enter start_month=202603, end_month=202601
   - Assert Create button disabled or error message shown

4. **testFormValidation_ValidInput**
   - Enter start_month=202601, end_month=202603
   - Assert Create button enabled

5. **testSuccessfulCreation**
   - Mock API to resolve successfully
   - Fill form and submit
   - Assert API called with correct data
   - Assert success toast shown
   - Assert onSuccess callback called

6. **testCreationError**
   - Mock API to reject with 409 Conflict
   - Submit form
   - Assert error toast shows appropriate message

7. **testMonthPickerFormat**
   - Render dialog
   - Assert month pickers accept YYYYMM format
   - Enter invalid format "2026-01"
   - Assert validation error

### EditConfigDialog.test.jsx
1. **testDialogDisplaysCurrentValues**
   - Pass config: month=202601, auto_close_day=10, is_closed=false
   - Assert all fields populated correctly

2. **testMonthIsReadOnly**
   - Render dialog
   - Assert month field is disabled or read-only

3. **testAutoCloseDayValidation_TooLow**
   - Enter auto_close_day=0
   - Assert validation error message

4. **testAutoCloseDayValidation_TooHigh**
   - Enter auto_close_day=32
   - Assert validation error message

5. **testAutoCloseDayValidation_Valid**
   - Enter auto_close_day=15
   - Assert no validation error

6. **testIsClosedToggle**
   - Initial: is_closed=false
   - Click toggle
   - Assert switch shows ON state

7. **testSaveButtonDisabledWhenNoChanges**
   - Render with initial values
   - Assert Save button disabled
   - Change auto_close_day
   - Assert Save button enabled

8. **testSuccessfulUpdate**
   - Mock API to resolve
   - Change values and submit
   - Assert API called with updated data
   - Assert success toast shown
   - Assert onSuccess callback called

9. **testUpdateError**
   - Mock API to reject with 400 Bad Request
   - Submit form
   - Assert error toast with error message

### forecastConfig.js (API Service) Tests
1. **testCreateMonthsAPI**
   - Mock axios.post
   - Call createMonths(202601, 202603)
   - Assert POST to /api/sales-forecast/config with correct payload
   - Assert JWT token in headers

2. **testListConfigsAPI**
   - Mock axios.get
   - Call listConfigs()
   - Assert GET to /api/sales-forecast/config
   - Assert returns data array

3. **testUpdateConfigAPI**
   - Mock axios.put
   - Call updateConfig(1, {auto_close_day: 20, is_closed: true})
   - Assert PUT to /api/sales-forecast/config/1 with correct payload

4. **testAPIErrorHandling**
   - Mock axios to reject with network error
   - Call any API function
   - Assert error is thrown or returned

## Integration Tests
N/A - Frontend integration with backend is covered by E2E tests. Component integration is tested via unit tests with mocked API.

## E2E Tests (Playwright)

### forecast-config.spec.ts
1. **testFullWorkflow_CreateAndEdit**
   - Login as admin with all permissions
   - Navigate to /sales-forecast/config
   - Click "Create Months"
   - Enter start=202601, end=202603
   - Click Create
   - Verify 3 new rows in table
   - Click Edit on first month
   - Change auto_close_day to 25
   - Toggle is_closed
   - Click Save
   - Verify row updated in table

2. **testAccessControl_NoViewPermission**
   - Login as user without sales_forecast_config.view
   - Navigate to /sales-forecast/config
   - Verify access denied or redirect to home

3. **testAccessControl_ViewOnly**
   - Login as user with view but not edit permission
   - Navigate to /sales-forecast/config
   - Verify table displays
   - Verify "Create Months" button not visible
   - Verify Edit buttons not visible

4. **testValidationErrors**
   - Login and navigate to page
   - Click "Create Months"
   - Enter start=202605, end=202603 (invalid range)
   - Verify Create button disabled
   - Click Edit on month
   - Enter auto_close_day=50
   - Verify validation error shown

5. **testErrorHandling_DuplicateMonth**
   - Create months 202601-202603
   - Attempt to create 202602 again
   - Verify appropriate error message

## Test Data Setup
- Mock API responses using MSW (Mock Service Worker) or Vitest mocks
- Sample configs:
  - Open month: {id: 1, month: "202601", auto_close_day: 10, is_closed: false, closed_at: null}
  - Closed month: {id: 2, month: "202512", auto_close_day: 15, is_closed: true, closed_at: "2025-12-15T00:00:00"}
- Mock user permissions in authentication context
- For E2E: Use test database with seeded data or API mocking

## Mocking Strategy
- **Unit Tests:** Mock all API calls using Vitest vi.mock() or MSW
- **Component Tests:** Mock authentication context to control permissions
- **API Service Tests:** Mock axios using vi.mock('axios')
- **E2E Tests:** Use real backend API (test environment) or Playwright request interception for controlled scenarios
- Do not mock Material UI components - render actual components for accurate testing
