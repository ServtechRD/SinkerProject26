# T022: Test Plan

## Unit Tests (Vitest + React Testing Library)

### ForecastIntegrationPage.test.jsx

1. **testPageRendersFilters**
   - Render page
   - Assert month and version dropdowns visible

2. **testMonthSelection_LoadsVersions**
   - Mock fetchVersions API
   - Select month
   - Assert fetchVersions called
   - Assert version dropdown populated

3. **testVersionSelection_LoadsData**
   - Mock fetchIntegrationData API
   - Select month and version
   - Assert API called with correct params
   - Assert table displays data

4. **testDefaultsToLatestVersion**
   - Mock versions: [v1, v2, v3]
   - Select month
   - Assert v3 auto-selected
   - Assert data fetched for v3

5. **testTableDisplaysAllColumns**
   - Mock API to return sample data
   - Render page
   - Assert 20 columns visible (or in DOM)
   - Verify column headers

6. **testTableDisplaysData**
   - Mock 3 products
   - Assert table shows 3 rows
   - Verify each row has correct product data

7. **testNumberFormatting**
   - Mock product with qty_px = 1234.56
   - Assert table cell displays "1,234.56"

8. **testDifferenceHighlighting_Positive**
   - Mock product with difference = 50
   - Assert cell has green styling

9. **testDifferenceHighlighting_Negative**
   - Mock product with difference = -30
   - Assert cell has red styling

10. **testDifferenceHighlighting_Zero**
    - Mock product with difference = 0
    - Assert cell has gray or neutral styling

11. **testRemarksDisplay**
    - Mock product with remarks = "新增產品"
    - Assert remarks column displays "新增產品"

12. **testExportButton_Visible**
    - Render page
    - Assert "Export Excel" button visible

13. **testExportButton_TriggersDownload**
    - Mock exportIntegrationExcel API
    - Click "Export Excel"
    - Assert API called
    - Assert file download triggered (mock URL.createObjectURL)

14. **testExportButton_DisabledWhileLoading**
    - Mock API to delay
    - Trigger data load
    - Assert Export button disabled during loading

15. **testLoadingState**
    - Mock API to delay
    - Render page and select month
    - Assert loading spinner visible

16. **testErrorHandling**
    - Mock API to reject with error
    - Select month
    - Assert error notification displayed

17. **testEmptyState**
    - Mock API to return empty array
    - Assert empty state message displayed

18. **testPermissionCheck**
    - Mock user without sales_forecast.view
    - Render page
    - Assert access denied message or redirect

19. **testVersionInfo_Display**
    - Mock version and product count
    - Assert displays "Version: {version}, Products: {count}"

20. **testSorting**
    - Mock products in unsorted order
    - Assert table displays in sorted order (from backend)

### forecastIntegration.js API Service Tests

1. **testFetchIntegrationData**
   - Mock axios.get
   - Call fetchIntegrationData(month, version)
   - Assert GET /api/sales-forecast/integration with query params

2. **testFetchIntegrationData_LatestVersion**
   - Call with version=null
   - Assert version param omitted from request

3. **testExportIntegrationExcel**
   - Mock axios.get with responseType blob
   - Call exportIntegrationExcel(month, version)
   - Assert GET /api/sales-forecast/integration/export

4. **testExportIntegrationExcel_TriggersDownload**
   - Mock blob response
   - Call export function
   - Assert URL.createObjectURL called
   - Assert link click triggered

5. **testAPIErrorHandling**
   - Mock axios to reject
   - Call fetchIntegrationData
   - Assert error thrown or returned

## Integration Tests
N/A - Frontend integration with backend tested via E2E tests. Component integration tested in unit tests with mocked API.

## E2E Tests (Playwright)

### forecast-integration.spec.ts

1. **testFullViewWorkflow**
   - Login as user with view permission
   - Navigate to /sales-forecast/integration
   - Select month "202601"
   - Verify version dropdown populates
   - Verify table displays data
   - Verify all 20 columns visible

2. **testVersionSelection**
   - Upload 2 versions of data
   - Navigate to integration page
   - Verify 2 versions in dropdown
   - Select version 1
   - Verify table shows version 1 data
   - Select version 2
   - Verify table shows version 2 data

3. **testTableData**
   - Upload known test data
   - Navigate to page
   - Verify table displays correct products
   - Verify quantities match uploaded data
   - Verify subtotals calculated correctly

4. **testDifferenceHighlighting**
   - Upload version 1 with product P001 total=300
   - Upload version 2 with product P001 total=350
   - Navigate to page, select version 2
   - Verify P001 difference cell shows green "+50.00"

5. **testExportExcel**
   - Navigate to page with data
   - Click "Export Excel"
   - Wait for download
   - Verify file downloaded (check downloads folder)
   - Verify filename contains month

6. **testAll12Channels**
   - Upload data to all 12 channels
   - Navigate to page
   - Verify all 12 channel columns visible
   - Verify all columns have data

7. **testResponsive_HorizontalScroll**
   - Resize browser to mobile width
   - Navigate to page
   - Verify table has horizontal scroll
   - Scroll right
   - Verify all columns accessible

8. **testPermissionCheck**
   - Login without sales_forecast.view
   - Navigate to /sales-forecast/integration
   - Verify access denied or redirect

9. **testEmptyState**
   - Select month with no data
   - Verify empty state message

10. **testErrorHandling**
    - Mock API to return error
    - Navigate to page
    - Verify error notification

11. **testLargeDataset**
    - Upload 500 products
    - Navigate to page
    - Verify table renders
    - Verify scrolling smooth

12. **testNumberFormatting**
    - Upload product with quantity 1234.56
    - Verify table displays "1,234.56"

## Test Data Setup

### Mock Data
- **Sample Integration Row:**
  ```javascript
  {
    warehouse_location: "A01",
    category: "01飲料類",
    spec: "600ml*24入",
    product_name: "可口可樂",
    product_code: "P001",
    qty_px: 100.50,
    qty_carrefour: 80.00,
    qty_aimall: 60.00,
    qty_711: 120.00,
    qty_familymart: 110.00,
    qty_ok: 50.00,
    qty_costco: 200.00,
    qty_fkmart: 40.00,
    qty_wellsociety: 30.00,
    qty_cosmed: 25.00,
    qty_ecommerce: 90.00,
    qty_distributor: 70.00,
    original_subtotal: 975.50,
    difference: 50.00,
    remarks: "數量增加"
  }
  ```

- **Mock Versions:**
  ```javascript
  [
    {version: "2026/01/15 15:00:00", item_count: 150},
    {version: "2026/01/15 14:30:00", item_count: 148}
  ]
  ```

### E2E Test Data
- Create test users with sales_forecast.view permission
- Seed database with:
  - Multiple months of data
  - Multiple versions per month
  - Products distributed across all 12 channels
  - Varying differences (positive, negative, zero)
- Sample products:
  - Product with data in all 12 channels
  - Product in only 2-3 channels
  - New product (high positive difference)
  - Reduced product (negative difference)

## Mocking Strategy

### Unit Tests
- Mock all API calls: fetchIntegrationData, exportIntegrationExcel
- Mock authentication context for permission checks
- Mock URL.createObjectURL and link.click() for export tests
- Use MSW or Vitest mocks for HTTP requests
- Mock window.location for file download verification

### E2E Tests
- Use real backend API (test environment)
- Seed database with comprehensive test data
- Use real authentication and permissions
- Verify actual file downloads
- Clean up test data after tests

### Number Formatting Helper
Create utility function for consistent number display:
```javascript
const formatQuantity = (value) => {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
};
```

## Performance Testing
- Load test with 500-1000 products
- Measure:
  - Initial render time
  - Table scroll performance
  - Export trigger time
- Optimize if rendering exceeds 3 seconds
- Consider virtualization for very large datasets (react-window or MUI DataGrid virtualization)
