# T029: Test Plan

## Unit Tests

### ProductionPlanPage.test.jsx
- **testPageRenders**: Verify page renders without crashing
- **testYearSelectorRenders**: Verify year dropdown present
- **testLoadButtonDisabled**: Verify load button disabled when no year selected
- **testDefaultYearValue**: Verify year defaults to current year
- **testLoadDataTriggered**: Verify clicking load calls API

### ProductionPlanGrid.test.jsx
- **testGridColumns**: Verify all 22 columns render
- **testFrozenColumns**: Verify Product Code and Name columns frozen
- **testEmptyState**: Verify empty state message when no data
- **testRowsRender**: Verify rows render with correct data
- **testNumberFormatting**: Verify numeric values formatted with 2 decimals
- **testDifferenceColor**: Verify positive=green, negative=red
- **testMultipleChannels**: Verify multiple channels for same product shown separately

### EditableCell.test.jsx
- **testCellClickEntersEdit**: Verify clicking cell shows input
- **testInputValidation**: Verify decimal validation
- **testTabNavigation**: Verify Tab moves to next cell
- **testEnterNavigation**: Verify Enter saves and moves down
- **testEscCancel**: Verify ESC cancels edit
- **testHighlightOnChange**: Verify modified cell highlighted
- **testReadOnlyMode**: Verify cell non-editable without permission

### calculations.test.js
- **testCalculateTotal**: Verify total = sum(months) + buffer
- **testCalculateTotalSparse**: Verify missing months treated as 0
- **testCalculateTotalEmpty**: Verify empty allocation returns buffer only
- **testCalculateDifference**: Verify difference = total - forecast
- **testDecimalPrecision**: Verify 2 decimal precision maintained

### productionPlanApi.test.js
- **testGetPlansByYear**: Verify API builds correct URL
- **testUpdatePlan**: Verify PUT request with correct body structure
- **testMonthlyAllocationFormat**: Verify months sent as strings "2"-"12"
- **testAuthHeaders**: Verify JWT token included

## Integration Tests

### ProductionPlanFlow.test.jsx
- **testLoadDataFlow**:
  1. Render page
  2. Select year
  3. Click load
  4. Mock API response
  5. Verify grid populated
- **testEditAndSaveFlow**:
  1. Load data
  2. Edit multiple cells
  3. Verify real-time calculation updates
  4. Click save
  5. Mock PUT responses
  6. Verify success handling
- **testCalculationFlow**:
  1. Load data
  2. Edit Feb value
  3. Verify Total updates
  4. Verify Difference updates
  5. Edit Buffer
  6. Verify Total updates again
- **testPermissionFlow**:
  1. Mock user without edit permission
  2. Render page
  3. Verify cells non-editable
  4. Verify no save buttons

### Setup
```jsx
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { AuthContext } from '../../contexts/AuthContext';
import ProductionPlanPage from './ProductionPlanPage';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  rest.get('/api/production-plan', (req, res, ctx) => {
    const year = req.url.searchParams.get('year');

    return res(ctx.json([
      {
        id: 1,
        year: parseInt(year),
        productCode: 'PROD001',
        productName: 'Product 1',
        category: 'Cat A',
        spec: 'Spec A',
        warehouseLocation: 'WH-A',
        channel: 'CH-DIRECT',
        monthlyAllocation: {
          '2': 100.00,
          '3': 150.00,
          '4': 200.00
        },
        bufferQuantity: 50.00,
        totalQuantity: 500.00,
        originalForecast: 480.00,
        difference: 20.00,
        remarks: null,
        createdAt: '2026-01-15T10:00:00',
        updatedAt: '2026-01-15T10:00:00'
      },
      {
        id: 2,
        year: parseInt(year),
        productCode: 'PROD001',
        productName: 'Product 1',
        category: 'Cat A',
        spec: 'Spec A',
        warehouseLocation: 'WH-A',
        channel: 'CH-RETAIL',
        monthlyAllocation: {
          '2': 50.00,
          '3': 75.00
        },
        bufferQuantity: 25.00,
        totalQuantity: 150.00,
        originalForecast: 200.00,
        difference: -50.00,
        remarks: null,
        createdAt: '2026-01-15T10:00:00',
        updatedAt: '2026-01-15T10:00:00'
      }
    ]));
  }),

  rest.put('/api/production-plan/:id', (req, res, ctx) => {
    const { id } = req.params;
    const { monthlyAllocation, bufferQuantity, remarks } = req.body;

    // Calculate total
    const monthlySum = Object.values(monthlyAllocation || {})
      .reduce((sum, val) => sum + parseFloat(val), 0);
    const total = monthlySum + parseFloat(bufferQuantity || 0);

    return res(ctx.json({
      id: parseInt(id),
      monthlyAllocation,
      bufferQuantity,
      totalQuantity: total,
      difference: total - 480.00, // Mock forecast
      remarks,
      updatedAt: new Date().toISOString()
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
  username: 'planner',
  permissions: ['production_plan.view', 'production_plan.edit']
};

const mockUserViewOnly = {
  username: 'viewer',
  permissions: ['production_plan.view']
};

describe('ProductionPlanPage', () => {
  test('should load and display production plan data', async () => {
    renderWithProviders(<ProductionPlanPage />);

    // Select year
    const yearSelect = screen.getByLabelText(/year/i);
    await userEvent.selectOptions(yearSelect, '2026');

    // Click load
    const loadButton = screen.getByRole('button', { name: /load/i });
    await userEvent.click(loadButton);

    // Wait for data
    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument();
    });

    // Verify both channels displayed
    const rows = screen.getAllByText('PROD001');
    expect(rows).toHaveLength(2);
  });

  test('should calculate total when editing monthly value', async () => {
    renderWithProviders(<ProductionPlanPage />);

    // Load data first
    await userEvent.selectOptions(screen.getByLabelText(/year/i), '2026');
    await userEvent.click(screen.getByRole('button', { name: /load/i }));
    await waitFor(() => screen.getByText('PROD001'));

    // Find first row Feb cell (month 2)
    const rows = screen.getAllByRole('row');
    const firstDataRow = rows[1]; // Skip header
    const febCell = within(firstDataRow).getByTestId('month-2-cell');

    // Click and edit
    await userEvent.click(febCell);
    const input = within(febCell).getByRole('textbox');
    await userEvent.clear(input);
    await userEvent.type(input, '200');

    // Verify total updated (was 500, now 500 - 100 + 200 = 600)
    await waitFor(() => {
      const totalCell = within(firstDataRow).getByTestId('total-cell');
      expect(totalCell).toHaveTextContent('600.00');
    });

    // Verify difference updated (600 - 480 = 120)
    const diffCell = within(firstDataRow).getByTestId('difference-cell');
    expect(diffCell).toHaveTextContent('120.00');
  });
});
```

## E2E Tests

### production-plan.spec.js (Playwright)
- **testCompleteEditFlow**: Login, load data, edit cells, save, verify persistence
- **testKeyboardNavigation**: Tab through cells, verify navigation correct
- **testCalculationAccuracy**: Edit multiple months and buffer, verify totals exact
- **testMultipleChannels**: Verify editing one channel doesn't affect others
- **testFrozenColumns**: Scroll horizontally, verify frozen columns stay visible
- **testUnsavedWarning**: Edit cells, navigate away, verify warning
- **testPermissionDenied**: Login without edit, verify read-only
- **testLargeDataset**: Load 500 rows, verify performance acceptable

### Playwright Test Example
```javascript
import { test, expect } from '@playwright/test';

test.describe('Production Plan Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'planner');
    await page.fill('[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  });

  test('should load and edit production plan', async ({ page }) => {
    await page.goto('/production-plan');

    // Select year
    await page.selectOption('[data-testid="year-select"]', '2026');
    await page.click('button:has-text("Load")');

    // Wait for data
    await expect(page.locator('table tbody tr')).toHaveCount.greaterThan(0);

    // Edit February cell in first row
    const firstFebCell = page.locator('tbody tr:first-child [data-month="2"]');
    await firstFebCell.click();

    // Type new value
    await page.fill('[data-testid="month-input"]', '250');
    await page.press('[data-testid="month-input"]', 'Enter');

    // Verify total updated
    const totalCell = page.locator('tbody tr:first-child [data-testid="total-cell"]');
    await expect(totalCell).toContainText('.00'); // Should have recalculated

    // Verify save button appears
    await expect(page.locator('button:has-text("Save")')).toBeVisible();

    // Click save
    await page.click('button:has-text("Save")');

    // Verify success message
    await expect(page.locator('.MuiSnackbar-root:has-text("saved successfully")')).toBeVisible();
  });

  test('should maintain frozen columns during scroll', async ({ page }) => {
    await page.goto('/production-plan');

    await page.selectOption('[data-testid="year-select"]', '2026');
    await page.click('button:has-text("Load")');
    await page.waitForSelector('table tbody tr');

    // Get initial position of Product Code column
    const productCodeCell = page.locator('tbody tr:first-child td:first-child');
    const initialBoundingBox = await productCodeCell.boundingBox();

    // Scroll grid horizontally
    await page.locator('[data-testid="grid-container"]').evaluate((el) => {
      el.scrollLeft = 500;
    });

    // Wait a moment
    await page.waitForTimeout(500);

    // Verify Product Code still at same position (frozen)
    const afterScrollBoundingBox = await productCodeCell.boundingBox();
    expect(afterScrollBoundingBox.x).toBe(initialBoundingBox.x);
  });

  test('should warn before navigation with unsaved changes', async ({ page }) => {
    await page.goto('/production-plan');

    await page.selectOption('[data-testid="year-select"]', '2026');
    await page.click('button:has-text("Load")');
    await page.waitForSelector('table tbody tr');

    // Edit a cell
    const firstFebCell = page.locator('tbody tr:first-child [data-month="2"]');
    await firstFebCell.click();
    await page.fill('[data-testid="month-input"]', '250');
    await page.press('[data-testid="month-input"]', 'Enter');

    // Set up dialog handler
    page.on('dialog', dialog => {
      expect(dialog.type()).toBe('confirm');
      expect(dialog.message()).toContain('unsaved');
      dialog.dismiss();
    });

    // Try to navigate away
    await page.click('a:has-text("Dashboard")');

    // Should still be on production plan page
    await expect(page).toHaveURL(/production-plan/);
  });
});
```

## Test Data Setup Notes

### Mock API Response Data
```javascript
const mockProductionPlans = [
  {
    id: 1,
    year: 2026,
    productCode: 'PROD001',
    productName: 'Product 1',
    category: 'Category A',
    spec: 'Spec A',
    warehouseLocation: 'WH-A',
    channel: 'CH-DIRECT',
    monthlyAllocation: {
      '2': 100.00,
      '3': 150.00,
      '4': 200.00,
      '5': 180.00,
      '6': 220.00,
      '7': 250.00,
      '8': 230.00,
      '9': 210.00,
      '10': 240.00,
      '11': 260.00,
      '12': 280.00
    },
    bufferQuantity: 50.00,
    totalQuantity: 2370.00,
    originalForecast: 2300.00,
    difference: 70.00,
    remarks: null
  },
  // Multiple channels for same product
  {
    id: 2,
    year: 2026,
    productCode: 'PROD001',
    productName: 'Product 1',
    category: 'Category A',
    spec: 'Spec A',
    warehouseLocation: 'WH-A',
    channel: 'CH-RETAIL',
    monthlyAllocation: {
      '2': 50.00,
      '3': 75.00,
      '12': 100.00
    },
    bufferQuantity: 25.00,
    totalQuantity: 250.00,
    originalForecast: 300.00,
    difference: -50.00,
    remarks: null
  },
  {
    id: 3,
    year: 2026,
    productCode: 'PROD001',
    productName: 'Product 1',
    category: 'Category A',
    spec: 'Spec A',
    warehouseLocation: 'WH-A',
    channel: 'CH-ONLINE',
    monthlyAllocation: {
      '2': 30.00,
      '3': 40.00,
      '4': 50.00
    },
    bufferQuantity: 20.00,
    totalQuantity: 140.00,
    originalForecast: 150.00,
    difference: -10.00,
    remarks: 'Conservative forecast'
  }
];
```

### Calculation Test Cases
```javascript
// Test case 1: Full year allocation
const fullYear = {
  monthlyAllocation: {
    '2': 100, '3': 110, '4': 120, '5': 130, '6': 140,
    '7': 150, '8': 160, '9': 170, '10': 180, '11': 190, '12': 200
  },
  bufferQuantity: 100,
  expectedTotal: 1750 // sum = 1650, + buffer 100
};

// Test case 2: Sparse allocation
const sparse = {
  monthlyAllocation: { '2': 100, '12': 200 },
  bufferQuantity: 50,
  expectedTotal: 350
};

// Test case 3: Empty allocation
const empty = {
  monthlyAllocation: {},
  bufferQuantity: 100,
  expectedTotal: 100
};

// Test case 4: No buffer
const noBuffer = {
  monthlyAllocation: { '2': 100, '3': 150, '4': 200 },
  bufferQuantity: 0,
  expectedTotal: 450
};
```

### User Permissions Mock
```javascript
const mockUsers = {
  planner: {
    username: 'planner',
    permissions: ['production_plan.view', 'production_plan.edit'],
    token: 'mock-jwt-planner'
  },
  viewer: {
    username: 'viewer',
    permissions: ['production_plan.view'],
    token: 'mock-jwt-viewer'
  },
  noAccess: {
    username: 'basic',
    permissions: [],
    token: 'mock-jwt-basic'
  }
};
```

## Mocking Strategy

### Unit Tests
- Mock API functions from productionPlan.js
- Mock calculation utilities
- Mock React Router hooks
- Mock AuthContext for permissions
- Use jest.fn() for callbacks

### Integration Tests
- Mock HTTP with MSW (Mock Service Worker)
- Real React components
- Real Material UI DataGrid
- Real calculation logic
- Mock localStorage

### E2E Tests
- Real backend API or staging environment
- Real database with test data
- Real authentication
- Test user accounts with different permissions

## Performance Benchmarks
- Page load: < 2 seconds
- Load 100 rows: < 1 second
- Load 500 rows: < 2 seconds
- Load 1000 rows: < 3 seconds
- Cell edit interaction: < 50ms (instant feel)
- Calculation update: < 100ms
- Horizontal scroll: 60fps
- Vertical scroll: 60fps
- Save 100 rows: < 5 seconds

## Accessibility Testing
- Keyboard navigation through all editable cells
- Screen reader announces cell values and states
- Focus indicators visible on all interactive elements
- ARIA labels on grid and columns
- Color contrast meets WCAG AA (4.5:1)
- Error messages announced to screen readers
