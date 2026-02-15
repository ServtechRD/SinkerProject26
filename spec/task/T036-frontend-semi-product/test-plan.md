# T036: Test Plan

## Unit Tests

### SemiProductPage.test.jsx (Vitest + React Testing Library)
- **test_renders_page_title_and_layout**
  - Render component
  - Verify title "Semi Product Advance Purchase Configuration" present
  - Verify upload zone present
  - Verify template download button present

- **test_upload_zone_accepts_xlsx_files**
  - Create .xlsx file mock
  - Simulate file drop
  - Verify file name displayed

- **test_upload_zone_rejects_non_xlsx_files**
  - Create .pdf file mock
  - Simulate file drop
  - Verify error message shown

- **test_template_download_triggers_api_call**
  - Mock API
  - Click download button
  - Verify GET /api/semi-product/template called

- **test_loads_products_on_mount**
  - Mock API to return test data
  - Render component
  - Verify API called on mount
  - Verify table populated with data

- **test_upload_shows_confirmation_dialog**
  - Select file
  - Click upload button
  - Verify confirmation dialog appears

- **test_upload_calls_api_and_refreshes_data**
  - Mock upload API
  - Mock list API
  - Confirm upload
  - Verify POST /api/semi-product/upload called
  - Verify GET /api/semi-product called after upload

- **test_inline_edit_saves_advance_days**
  - Render table with data
  - Click advance days cell
  - Enter new value
  - Press Enter
  - Verify PUT API called with new value

- **test_inline_edit_validates_positive_integer**
  - Click advance days cell
  - Enter negative value
  - Attempt to save
  - Verify validation error shown
  - Verify API not called

- **test_shows_error_toast_on_upload_failure**
  - Mock API to return error
  - Upload file
  - Verify error toast displayed with message

- **test_shows_success_toast_on_upload_success**
  - Mock successful upload
  - Upload file
  - Verify success toast with count

### semiProduct.js API tests
- **test_uploadSemiProducts_sends_multipart_formdata**
  - Mock axios
  - Call uploadSemiProducts with file
  - Verify multipart/form-data request sent

- **test_listSemiProducts_returns_array**
  - Mock axios response
  - Call listSemiProducts
  - Verify data parsed correctly

- **test_updateSemiProduct_sends_put_request**
  - Mock axios
  - Call updateSemiProduct(id, advanceDays)
  - Verify PUT request with correct payload

- **test_downloadTemplate_triggers_download**
  - Mock axios blob response
  - Call downloadTemplate
  - Verify blob URL created and download triggered

## Integration Tests
N/A - Frontend integration tests covered by E2E

## E2E Tests (Playwright)

### semi-product-page.spec.ts
- **test_complete_upload_workflow**
  - Navigate to /semi-product
  - Download template
  - Create Excel file with test data programmatically
  - Upload file via drag-and-drop
  - Confirm dialog
  - Wait for success message
  - Verify table contains uploaded data

- **test_inline_edit_workflow**
  - Navigate to /semi-product with existing data
  - Click advance days cell for first row
  - Enter new value
  - Press Enter
  - Verify cell updated
  - Refresh page
  - Verify value persisted

- **test_upload_replaces_data**
  - Upload file with 10 rows
  - Verify 10 rows in table
  - Upload file with 5 rows
  - Verify only 5 rows in table

- **test_validation_error_handling**
  - Upload file
  - Edit advance days to negative value
  - Verify error message appears
  - Verify value not saved

- **test_permission_denied_handling**
  - Login as user without semi_product.view permission
  - Navigate to /semi-product
  - Verify 403 error or access denied message

## Test Data Setup Notes
- Create test Excel files programmatically using SheetJS (xlsx library) in tests
- Sample data:
  ```
  品號      品名        提前日數
  SP001    半成品A      7
  SP002    半成品B      14
  SP003    半成品C      3
  ```
- Mock API responses in unit tests with MSW (Mock Service Worker) or jest.mock
- E2E tests use real backend with test database
- Seed test database with known data before E2E tests
- Clean up uploaded files after tests

## Mocking Strategy
- **Unit tests**: Mock axios API calls, mock file objects, mock Material UI components if needed
- **E2E tests**: Use real backend API, real database via Testcontainers
- Mock permissions by logging in with different test users
- Use MSW for consistent API mocking in development/testing
