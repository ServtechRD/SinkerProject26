# Test Plan — T033

## Unit Tests (Vitest + React Testing Library)

### WeeklySchedulePage
- Renders week picker, factory dropdown, query button
- Renders upload area when user has upload permission
- Hides upload area when user lacks upload permission
- Renders data table after query
- Shows loading state during query
- Shows error message on query failure

### WeekPicker
- Only Monday dates are selectable
- Non-Monday dates are disabled
- Selected date value is a Monday
- onChange callback fires with correct date

### Upload behavior
- Accepts .xlsx files
- Rejects non-.xlsx files with error message
- Shows upload progress indicator
- Displays row-level errors from backend response
- Refreshes table data after successful upload

### Inline edit
- Clicking quantity cell enters edit mode
- Enter key confirms edit
- Escape key cancels edit
- Invalid quantity (negative, non-numeric) shows validation error
- Save button sends PUT request with changed values

## Integration Tests
- N/A (frontend unit tests cover component behavior; API integration tested in T031)

## E2E Tests (Playwright)

### Weekly schedule workflow
- Login as production planner → navigate to /weekly-schedule
- Select Monday date + factory → click Query → table loads
- Upload .xlsx file → verify table refreshes with data
- Edit quantity cell → save → verify value persisted
- Verify sidebar link navigates to correct page

### Permission tests
- Login as sales user (no weekly_schedule permission) → /weekly-schedule not accessible
- Login as production planner → upload area visible
- Login as viewer-only → table is read-only

## Test Data Setup
- Test user with weekly_schedule.view/upload/edit permissions
- Test .xlsx file with valid schedule data (3-5 rows)
- Test .xlsx file with invalid data (missing columns, negative quantities)
- Backend API running (or MSW mock for isolated frontend tests)

## Mocking Strategy
- **API calls:** MSW (Mock Service Worker) for unit tests
- **Auth context:** Mock AuthContext providing test user with appropriate permissions
- **File upload:** Create test .xlsx fixtures in e2e/fixtures/
- **Backend:** Real backend for E2E tests; MSW for unit tests
