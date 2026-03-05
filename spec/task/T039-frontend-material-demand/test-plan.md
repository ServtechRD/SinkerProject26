# T039: Test Plan

## Unit Tests

### MaterialDemandPage.test.jsx (Vitest + React Testing Library)
- **test_renders_page_title_and_filters**
  - Render component
  - Verify title "Material Demand" present
  - Verify week picker present
  - Verify factory dropdown present

- **test_week_picker_defaults_to_current_week**
  - Render component
  - Verify week picker shows current week start date

- **test_factory_dropdown_defaults_to_first_option**
  - Render component
  - Verify factory dropdown shows first factory

- **test_loads_data_on_mount**
  - Mock API to return test data
  - Render component
  - Verify API called with default week and factory
  - Verify table populated with data

- **test_changing_week_triggers_new_query**
  - Render component with initial data
  - Change week picker
  - Verify API called with new week_start

- **test_changing_factory_triggers_new_query**
  - Render component with initial data
  - Change factory dropdown
  - Verify API called with new factory

- **test_displays_loading_indicator**
  - Mock delayed API response
  - Render component
  - Verify loading indicator visible
  - Wait for API response
  - Verify loading indicator hidden

- **test_displays_empty_state_when_no_data**
  - Mock API to return empty array
  - Render component
  - Verify empty state message shown

- **test_displays_error_toast_on_api_failure**
  - Mock API to return error
  - Render component
  - Verify error toast displayed

- **test_formats_decimal_values**
  - Mock API with decimal data
  - Render component
  - Verify decimals displayed with 2 decimal places

- **test_formats_null_last_purchase_date**
  - Mock API with null lastPurchaseDate
  - Render component
  - Verify "-" displayed in Last Purchase Date column

- **test_highlights_low_inventory_rows**
  - Mock API with row where estimatedInventory < demandQuantity
  - Render component
  - Verify row has warning style/class

### materialDemand.js API tests
- **test_query_material_demand_sends_correct_params**
  - Mock axios
  - Call queryMaterialDemand(weekStart, factory)
  - Verify GET request with correct query params

- **test_query_material_demand_returns_array**
  - Mock axios response
  - Call queryMaterialDemand
  - Verify data parsed correctly

## Integration Tests
N/A - Frontend integration tests covered by E2E

## E2E Tests (Playwright)

### material-demand-page.spec.ts
- **test_complete_query_workflow**
  - Seed database with test data for multiple weeks and factories
  - Navigate to /material-demand
  - Verify default week and factory selected
  - Verify table shows data for default selection
  - Click week picker, select different week
  - Verify table updates with new data
  - Change factory dropdown
  - Verify table updates with factory-specific data

- **test_empty_state_display**
  - Navigate to /material-demand
  - Select week/factory with no data
  - Verify empty state message displayed

- **test_low_inventory_highlighting**
  - Seed data with low inventory item
  - Navigate to /material-demand
  - Select appropriate week/factory
  - Verify row with low inventory highlighted

- **test_decimal_formatting**
  - Seed data with various decimal values
  - Navigate to /material-demand
  - Verify all decimals display 2 decimal places
  - Verify thousand separators in large numbers

- **test_null_date_handling**
  - Seed data with null lastPurchaseDate
  - Navigate to /material-demand
  - Verify "-" displayed in Last Purchase Date column

- **test_permission_denied_handling**
  - Login as user without material_demand.view permission
  - Navigate to /material-demand
  - Verify 403 error or access denied message

## Test Data Setup Notes
- Seed test database with material demand records:
  ```
  Week: 2026-02-17, Factory: F1
    - M001, 原料A, kg, last_purchase: 2026-02-10, demand_date: 2026-02-20, expected: 100.50, demand: 500.00, inventory: 50.25
    - M002, 原料B, pcs, last_purchase: null, demand_date: 2026-02-22, expected: 0.00, demand: 1000.00, inventory: 0.00 (low inventory)

  Week: 2026-02-17, Factory: F2
    - M003, 原料C, kg, last_purchase: 2026-02-12, demand_date: 2026-02-21, expected: 200.00, demand: 300.00, inventory: 150.00

  Week: 2026-02-24, Factory: F1
    - (no data - for empty state test)
  ```
- Mock API responses in unit tests with MSW (Mock Service Worker) or jest.mock
- E2E tests use real backend with test database
- Clean up test data after E2E tests

## Mocking Strategy
- **Unit tests**: Mock axios API calls, mock Material UI date picker if needed
- **E2E tests**: Use real backend API, real database via Testcontainers
- Mock permissions by logging in with different test users
- Use MSW for consistent API mocking in development/testing
