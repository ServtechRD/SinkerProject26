# T026: Test Plan

## Unit Tests

### InventoryIntegrationPage.test.jsx
- **testPageRenders**: Verify page renders without crashing
- **testQueryFormRenders**: Verify month picker, date pickers, and query button present
- **testVersionDropdownRenders**: Verify version selector present
- **testDefaultMonthValue**: Verify month defaults to current month
- **testDefaultDateValues**: Verify dates default to month boundaries
- **testQueryButtonDisabled**: Verify query button disabled when month empty
- **testLoadVersionButtonDisabled**: Verify load version disabled when no version selected

### InventoryDataTable.test.jsx
- **testTableColumns**: Verify all columns render correctly
- **testEmptyState**: Verify "No data" message when data array empty
- **testNumberFormatting**: Verify numbers display with 2 decimals and commas
- **testRowHighlight**: Verify rows highlighted when modified_subtotal ≠ production_subtotal
- **testSorting**: Verify clicking column header sorts data
- **testEditIconVisibility**: Verify edit icon shows on hover (with permission)
- **testEditIconHidden**: Verify edit icon hidden (without permission)

### QueryForm.test.jsx
- **testMonthChange**: Verify changing month updates state
- **testDateRangeChange**: Verify date pickers update state
- **testFormSubmit**: Verify form submission calls onSubmit prop
- **testVersionSelection**: Verify version dropdown calls onChange
- **testDateValidation**: Verify start_date < end_date validation
- **testMonthBoundaryValidation**: Verify dates must be within selected month

### useInventoryIntegration.test.js (custom hook)
- **testFetchData**: Verify hook calls API with correct parameters
- **testLoadingState**: Verify loading state true during fetch
- **testErrorState**: Verify error state set on API failure
- **testSuccessState**: Verify data state populated on success
- **testVersionStorage**: Verify version saved to localStorage after query

### inventoryApi.test.js
- **testGetInventoryIntegration**: Verify API function builds correct URL with query params
- **testUpdateModifiedSubtotal**: Verify PUT request with correct body
- **testAuthHeaders**: Verify JWT token included in request headers
- **testErrorHandling**: Verify API errors properly thrown

## Integration Tests

### InventoryIntegrationFlow.test.jsx
- **testRealTimeQueryFlow**:
  1. Render page
  2. Select month
  3. Click query
  4. Mock API response
  5. Verify table populated
  6. Verify version dropdown updated
- **testVersionQueryFlow**:
  1. Render page with existing versions in localStorage
  2. Select version from dropdown
  3. Click load version
  4. Mock API response
  5. Verify table shows historical data
- **testEditFlow**:
  1. Load data
  2. Click editable cell
  3. Enter new value
  4. Verify save button appears
  5. Click save
  6. Mock PUT API
  7. Verify refresh with new data
- **testPermissionFlow**:
  1. Mock user without edit permission
  2. Render page
  3. Verify edit controls hidden
  4. Attempt to click cell
  5. Verify no edit mode entered

### Setup
```jsx
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { AuthContext } from '../../contexts/AuthContext';
import InventoryIntegrationPage from './InventoryIntegrationPage';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  rest.get('/api/inventory-integration', (req, res, ctx) => {
    const month = req.url.searchParams.get('month');
    const version = req.url.searchParams.get('version');

    return res(ctx.json([
      {
        id: 1,
        month: month,
        productCode: 'PROD001',
        productName: 'Product 1',
        category: 'Cat A',
        spec: 'Spec A',
        warehouseLocation: 'WH-A',
        salesQuantity: 100.00,
        inventoryBalance: 250.00,
        forecastQuantity: 500.00,
        productionSubtotal: 150.00,
        modifiedSubtotal: null,
        version: version || 'v20260115120000',
        queryStartDate: `${month}-01`,
        queryEndDate: `${month}-31`,
        createdAt: '2026-01-15T12:00:00',
        updatedAt: '2026-01-15T12:00:00'
      }
    ]));
  }),

  rest.put('/api/inventory-integration/:id', (req, res, ctx) => {
    const { id } = req.params;
    const { modifiedSubtotal } = req.body;

    return res(ctx.json({
      id: parseInt(id) + 1000,
      modifiedSubtotal,
      version: 'v20260115130000'
    }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const renderWithProviders = (ui, { user = mockUserWithEditPermission } = {}) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } }
  });

  return render(
    <AuthContext.Provider value={{ user, isAuthenticated: true }}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          {ui}
        </BrowserRouter>
      </QueryClientProvider>
    </AuthContext.Provider>
  );
};

const mockUserWithEditPermission = {
  username: 'testuser',
  permissions: ['inventory.view', 'inventory.edit']
};

const mockUserViewOnly = {
  username: 'viewer',
  permissions: ['inventory.view']
};
```

## E2E Tests

### inventory-integration.spec.js (Playwright)
- **testCompleteQueryFlow**:
  - Login
  - Navigate to inventory integration page
  - Select month
  - Click query
  - Verify data appears in table
  - Verify version in dropdown
- **testEditAndSave**:
  - Login as editor user
  - Load data
  - Click modified subtotal cell
  - Enter new value
  - Click save
  - Verify success message
  - Verify page refreshes with new version
- **testVersionHistory**:
  - Perform 3 queries
  - Open version dropdown
  - Verify 3 versions listed
  - Select oldest version
  - Click load version
  - Verify old data displayed
- **testPermissionDenied**:
  - Login as view-only user
  - Navigate to page
  - Verify edit icons not present
  - Verify save button not present
- **testUnsavedChangesWarning**:
  - Edit a cell
  - Attempt to navigate away
  - Verify browser confirmation dialog
  - Accept dialog
  - Verify navigation completed
- **testResponsive**:
  - Test on mobile viewport (375x667)
  - Verify query controls stack
  - Verify table scrolls horizontally
  - Test on tablet (768x1024)
  - Test on desktop (1920x1080)

### Playwright Test Example
```javascript
import { test, expect } from '@playwright/test';

test.describe('Inventory Integration Page', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('[name="username"]', 'testuser');
    await page.fill('[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  });

  test('should query and display inventory data', async ({ page }) => {
    await page.goto('/inventory-integration');

    // Select month
    await page.click('[data-testid="month-picker"]');
    await page.click('text=January 2026');

    // Click query
    await page.click('button:has-text("Query")');

    // Wait for data to load
    await expect(page.locator('[data-testid="loading-spinner"]')).toBeHidden();

    // Verify table has data
    await expect(page.locator('table tbody tr')).toHaveCount.greaterThan(0);

    // Verify version dropdown updated
    await expect(page.locator('[data-testid="version-select"] option')).toHaveCount.greaterThan(0);
  });

  test('should edit modified subtotal', async ({ page }) => {
    await page.goto('/inventory-integration');

    // Load data first
    await page.click('[data-testid="month-picker"]');
    await page.click('text=January 2026');
    await page.click('button:has-text("Query")');
    await page.waitForSelector('table tbody tr');

    // Click modified subtotal cell
    const cell = page.locator('table tbody tr:first-child td:nth-child(10)'); // Modified Subtotal column
    await cell.hover();
    await cell.click();

    // Enter new value
    await page.fill('[data-testid="modified-subtotal-input"]', '200.50');
    await page.press('[data-testid="modified-subtotal-input"]', 'Enter');

    // Verify save button appears
    await expect(page.locator('button:has-text("Save")')).toBeVisible();

    // Click save
    await page.click('button:has-text("Save")');

    // Verify success message
    await expect(page.locator('.MuiSnackbar-root:has-text("saved successfully")')).toBeVisible();

    // Verify page refreshed with new version
    await expect(page.locator('[data-testid="current-version"]')).not.toHaveText('v20260115120000');
  });
});
```

## Test Data Setup Notes

### Mock API Responses
```javascript
// Real-time query response
const mockQueryResponse = [
  {
    id: 1,
    month: '2026-01',
    productCode: 'PROD001',
    productName: 'Product 1',
    category: 'Category A',
    spec: 'Spec A',
    warehouseLocation: 'WH-A',
    salesQuantity: 100.00,
    inventoryBalance: 250.00,
    forecastQuantity: 500.00,
    productionSubtotal: 150.00,
    modifiedSubtotal: null,
    version: 'v20260115120000',
    queryStartDate: '2026-01-01',
    queryEndDate: '2026-01-31',
    createdAt: '2026-01-15T12:00:00',
    updatedAt: '2026-01-15T12:00:00'
  },
  {
    id: 2,
    month: '2026-01',
    productCode: 'PROD002',
    productName: 'Product 2',
    category: 'Category B',
    spec: 'Spec B',
    warehouseLocation: 'WH-B',
    salesQuantity: 50.00,
    inventoryBalance: 100.00,
    forecastQuantity: 300.00,
    productionSubtotal: 150.00,
    modifiedSubtotal: 200.00, // Modified, should be highlighted
    version: 'v20260115120000',
    queryStartDate: '2026-01-01',
    queryEndDate: '2026-01-31',
    createdAt: '2026-01-15T12:00:00',
    updatedAt: '2026-01-15T12:00:00'
  }
];

// Version query response (historical)
const mockVersionResponse = [
  // Same structure but with older version
  { ...mockQueryResponse[0], version: 'v20260101100000' }
];

// PUT response
const mockUpdateResponse = {
  ...mockQueryResponse[0],
  id: 1001,
  modifiedSubtotal: 200.50,
  version: 'v20260115130000'
};
```

### LocalStorage Mock
```javascript
const mockVersionHistory = [
  { version: 'v20260115120000', month: '2026-01', timestamp: '2026-01-15T12:00:00' },
  { version: 'v20260114110000', month: '2026-01', timestamp: '2026-01-14T11:00:00' },
  { version: 'v20260113100000', month: '2025-12', timestamp: '2026-01-13T10:00:00' }
];

localStorage.setItem('inventoryVersions', JSON.stringify(mockVersionHistory));
```

### User Permissions Mock
```javascript
const mockUsers = {
  editor: {
    username: 'editor',
    permissions: ['inventory.view', 'inventory.edit'],
    token: 'mock-jwt-token-editor'
  },
  viewer: {
    username: 'viewer',
    permissions: ['inventory.view'],
    token: 'mock-jwt-token-viewer'
  },
  noPermission: {
    username: 'basic',
    permissions: [],
    token: 'mock-jwt-token-basic'
  }
};
```

## Mocking Strategy

### Unit Tests
- Mock API functions from inventory.js
- Mock React Router hooks (useNavigate, useLocation)
- Mock AuthContext for user permissions
- Mock Material UI components if needed for simpler testing
- Use jest.fn() for event handlers

### Integration Tests
- Mock HTTP requests with MSW (Mock Service Worker)
- Mock localStorage
- Mock window.confirm for unsaved changes warning
- Real React components and hooks
- Real Material UI components

### E2E Tests
- Real backend API (or staging environment)
- Real database with test data
- Real authentication system
- Test user accounts with different permissions

## Performance Benchmarks
- Initial page load: < 2 seconds
- Query execution (100 products): < 1.5 seconds
- Table render (1000 rows): < 1 second
- Sort operation (1000 rows): < 500ms
- Edit cell interaction: < 100ms (instant feeling)
- Save operation: < 1 second

## Accessibility Testing
- Keyboard navigation: Tab through all controls, Enter to activate
- Screen reader: All fields have proper labels and ARIA attributes
- Color contrast: All text meets WCAG AA standards (4.5:1 ratio)
- Focus indicators: Visible focus outline on all interactive elements
- Error announcements: Screen reader announces validation errors
