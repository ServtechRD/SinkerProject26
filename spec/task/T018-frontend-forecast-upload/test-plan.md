# T018: Test Plan

## Unit Tests (Vitest + React Testing Library)

### ForecastUploadPage.test.jsx

1. **testPageRendersLoading**
   - Mock API calls to delay
   - Render page
   - Assert loading spinner visible

2. **testPageRendersForm**
   - Mock API to return open months and channels
   - Render page
   - Assert month dropdown, channel dropdown, dropzone, buttons visible

3. **testMonthDropdown_PopulatesOpenMonths**
   - Mock API to return months: [{month: "202601", is_closed: false}, {month: "202512", is_closed: true}]
   - Assert dropdown shows only "202601"

4. **testMonthDropdown_FormatDisplay**
   - Mock month "202601"
   - Assert dropdown option displays "202601 (January 2026)"

5. **testChannelDropdown_FiltersByPermission**
   - Mock user with channels ["大全聯", "家樂福"]
   - Assert dropdown shows only these 2 channels

6. **testUploadButton_DisabledByDefault**
   - Render page
   - Assert Upload button disabled

7. **testUploadButton_EnabledWhenAllFieldsFilled**
   - Select month
   - Select channel
   - Select file
   - Assert Upload button enabled

8. **testFileSelection_Valid**
   - Create mock .xlsx file
   - Simulate file drop
   - Assert file name and size displayed

9. **testFileSelection_InvalidType**
   - Create mock .csv file
   - Simulate file drop
   - Assert error message: "Please upload a valid Excel file"

10. **testFileSelection_TooLarge**
    - Create mock file > 10MB
    - Simulate file drop
    - Assert error message: "File size exceeds 10MB limit"

11. **testFileRemoval**
    - Select file
    - Click remove button
    - Assert file cleared
    - Assert Upload button disabled

12. **testTemplateDownload**
    - Mock API downloadTemplate
    - Select channel "家樂福"
    - Click "Download Template"
    - Assert API called with channel parameter

13. **testTemplateButton_DisabledWithoutChannel**
    - Render page
    - Assert "Download Template" button disabled

14. **testUpload_Success**
    - Mock API to resolve with {rows_processed: 150}
    - Fill all fields
    - Click Upload
    - Assert loading spinner shown
    - Assert success notification: "Successfully uploaded 150 rows"
    - Assert form cleared

15. **testUpload_Error**
    - Mock API to reject with error
    - Fill all fields
    - Click Upload
    - Assert error notification displayed

16. **testUpload_MonthClosed**
    - Mock API to reject with 403 Forbidden
    - Assert error: "Month ... is closed"

17. **testAccessControl_NoPermission**
    - Mock user without sales_forecast.upload
    - Render page
    - Assert access denied message or redirect

### FileDropzone.test.jsx

1. **testDropzoneRenders**
   - Render component
   - Assert dropzone area visible
   - Assert instruction text visible

2. **testHoverEffect**
   - Simulate drag over
   - Assert highlighted style applied

3. **testFileDrop**
   - Create mock file
   - Simulate drop event
   - Assert onFileSelect callback called with file

4. **testClickToBrowse**
   - Click dropzone
   - Assert file input triggered

5. **testFileTypeValidation**
   - Component accepts only .xlsx
   - Drop .csv file
   - Assert onError callback called

6. **testDisplaySelectedFile**
   - Pass selectedFile prop
   - Assert file name and size displayed

7. **testRemoveFile**
   - Pass selectedFile prop
   - Click remove button
   - Assert onFileRemove callback called

### forecast.js API Service Tests

1. **testUploadForecast**
   - Mock axios.post
   - Call uploadForecast(file, month, channel)
   - Assert POST to /api/sales-forecast/upload with FormData

2. **testUploadForecast_IncludesJWT**
   - Call uploadForecast
   - Assert Authorization header with JWT token

3. **testDownloadTemplate**
   - Mock axios.get
   - Call downloadTemplate("大全聯")
   - Assert GET to /api/sales-forecast/template/大全聯
   - Assert responseType: 'blob'

4. **testFetchOpenMonths**
   - Mock axios.get to /api/sales-forecast/config
   - Call fetchOpenMonths()
   - Assert returns only configs with is_closed=false

## Integration Tests
N/A - Frontend integration with backend tested via E2E tests. Component integration tested in unit tests with mocked API.

## E2E Tests (Playwright)

### forecast-upload.spec.ts

1. **testFullUploadWorkflow**
   - Login as user with upload permission for 大全聯
   - Navigate to /sales-forecast/upload
   - Select month "202601"
   - Select channel "大全聯"
   - Upload valid test Excel file (create fixture)
   - Click Upload
   - Verify success notification appears
   - Verify form cleared

2. **testTemplateDownload**
   - Login and navigate to page
   - Select channel "家樂福"
   - Click "Download Template"
   - Verify file downloaded (check downloads folder)

3. **testValidation_NoMonth**
   - Navigate to page
   - Select channel and file
   - Verify Upload button disabled

4. **testValidation_NoChannel**
   - Select month and file
   - Verify Upload button disabled

5. **testValidation_NoFile**
   - Select month and channel
   - Verify Upload button disabled

6. **testValidation_InvalidFileType**
   - Select month and channel
   - Upload .csv file
   - Verify error message displayed

7. **testError_MonthClosed**
   - Close month via config API
   - Attempt upload to closed month
   - Verify error notification

8. **testAccessControl**
   - Login as user without upload permission
   - Navigate to /sales-forecast/upload
   - Verify access denied or redirect

9. **testChannelFiltering**
   - Login as user with permission only for 大全聯
   - Verify channel dropdown shows only 大全聯
   - Attempt to manually upload to 家樂福 (via API)
   - Verify error

10. **testDragAndDrop**
    - Navigate to page
    - Drag test Excel file to dropzone
    - Verify file captured and displayed

## Test Data Setup

### Mock Data
- **Open Months:**
  ```javascript
  [
    {id: 1, month: "202601", is_closed: false},
    {id: 2, month: "202602", is_closed: false},
    {id: 3, month: "202512", is_closed: true}
  ]
  ```

- **User Channels:**
  ```javascript
  ["大全聯", "家樂福", "愛買"]
  ```

- **Mock File:**
  ```javascript
  new File(["test content"], "test.xlsx", {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  })
  ```

### E2E Test Fixtures
- Create sample Excel files in `tests/fixtures/`:
  - `valid_forecast.xlsx` - 10 rows of valid data
  - `large_file.xlsx` - >10MB file for size validation
- Create test users with different channel permissions

## Mocking Strategy

### Unit Tests
- Mock all API calls using Vitest vi.mock() or MSW
- Mock fetch/axios responses for:
  - fetchOpenMonths() - returns array of open months
  - fetchUserChannels() - returns user's authorized channels
  - uploadForecast() - returns success/error
  - downloadTemplate() - returns blob
- Mock authentication context for permission checks
- Mock file objects for upload testing

### E2E Tests
- Use real backend API (test environment)
- Seed test database with:
  - Open and closed months
  - User accounts with various permissions
  - User-channel mappings
- Create real Excel fixtures for upload
- Clean up uploaded data after tests

### File Handling Mocks
- Mock FileReader for client-side file validation
- Mock FormData for upload payload construction
- Mock Blob and URL.createObjectURL for template download
