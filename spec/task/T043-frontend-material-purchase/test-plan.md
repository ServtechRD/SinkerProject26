# T043: Test Plan

## Unit Tests

### MaterialPurchasePage.test.jsx (Vitest + React Testing Library)
- **test_renders_page_title_and_filters**
  - Render component
  - Verify title "Material Purchase Planning" present
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

- **test_displays_trigger_button_for_non_triggered_items**
  - Mock API with item where is_erp_triggered=false
  - Render component
  - Verify "Trigger ERP" button visible

- **test_displays_triggered_badge_for_triggered_items**
  - Mock API with item where is_erp_triggered=true, erp_order_no="ERP-123"
  - Render component
  - Verify "Triggered: ERP-123" badge visible
  - Verify no trigger button

- **test_clicking_trigger_button_shows_confirmation_dialog**
  - Mock API with non-triggered item
  - Render component
  - Click "Trigger ERP" button
  - Verify confirmation dialog appears

- **test_confirmation_dialog_cancel_closes_without_api_call**
  - Mock API
  - Render component, click trigger button
  - Click "Cancel" in dialog
  - Verify dialog closes
  - Verify trigger API not called

- **test_confirmation_dialog_confirm_calls_trigger_api**
  - Mock query and trigger APIs
  - Render component, click trigger button
  - Click "Confirm" in dialog
  - Verify POST /api/material-purchase/:id/trigger-erp called

- **test_successful_trigger_shows_toast_and_refreshes**
  - Mock successful trigger API
  - Trigger ERP
  - Verify success toast displayed
  - Verify query API called again (refresh)

- **test_failed_trigger_shows_error_toast**
  - Mock trigger API to return error
  - Trigger ERP
  - Verify error toast displayed

- **test_409_error_shows_appropriate_message**
  - Mock trigger API to return 409
  - Trigger ERP
  - Verify toast shows "already triggered" message

- **test_trigger_button_disabled_during_api_call**
  - Mock delayed trigger API
  - Click trigger button and confirm
  - Verify button shows loading state
  - Verify button disabled during API call

- **test_displays_loading_indicator**
  - Mock delayed API response
  - Render component
  - Verify loading indicator visible

- **test_displays_empty_state_when_no_data**
  - Mock API to return empty array
  - Render component
  - Verify empty state message shown

- **test_formats_decimal_values**
  - Mock API with decimal data
  - Render component
  - Verify decimals displayed with 2 decimal places
  - Verify thousand separators

### materialPurchase.js API tests
- **test_query_material_purchase_sends_correct_params**
  - Mock axios
  - Call queryMaterialPurchase(weekStart, factory)
  - Verify GET request with correct query params

- **test_trigger_erp_sends_post_request**
  - Mock axios
  - Call triggerErp(id)
  - Verify POST /api/material-purchase/{id}/trigger-erp called

- **test_query_material_purchase_returns_array**
  - Mock axios response
  - Call queryMaterialPurchase
  - Verify data parsed correctly

- **test_trigger_erp_returns_updated_entity**
  - Mock axios response
  - Call triggerErp(id)
  - Verify updated entity returned

## Integration Tests
N/A - Frontend integration tests covered by E2E

## E2E Tests (Playwright)

### material-purchase-page.spec.ts
- **test_complete_query_workflow**
  - Seed database with test data for multiple weeks and factories
  - Navigate to /material-purchase
  - Verify default week and factory selected
  - Verify table shows data for default selection
  - Click week picker, select different week
  - Verify table updates with new data
  - Change factory dropdown
  - Verify table updates with factory-specific data

- **test_trigger_erp_complete_workflow**
  - Seed database with non-triggered item
  - Navigate to /material-purchase
  - Select appropriate week/factory
  - Verify "Trigger ERP" button visible
  - Click "Trigger ERP" button
  - Verify confirmation dialog appears with product details
  - Click "Confirm"
  - Wait for success toast
  - Verify toast shows order number
  - Verify table refreshes
  - Verify row now shows "Triggered: [order_no]" badge
  - Verify button replaced with badge

- **test_trigger_erp_cancel_workflow**
  - Navigate to /material-purchase
  - Click "Trigger ERP" button
  - Click "Cancel" in dialog
  - Verify dialog closes
  - Verify row still shows trigger button (unchanged)

- **test_triggered_items_display_correctly**
  - Seed database with triggered item (is_erp_triggered=true, erp_order_no="ERP-123")
  - Navigate to /material-purchase
  - Select appropriate week/factory
  - Verify "Triggered: ERP-123" badge visible
  - Verify no trigger button present

- **test_empty_state_display**
  - Navigate to /material-purchase
  - Select week/factory with no data
  - Verify empty state message displayed

- **test_decimal_formatting**
  - Seed data with various decimal values
  - Navigate to /material-purchase
  - Verify all decimals display 2 decimal places
  - Verify thousand separators in large numbers

- **test_calculated_columns_display**
  - Seed data with known calculations
  - Navigate to /material-purchase
  - Verify Basket Qty, Required Barrels calculated correctly
  - Verify values match expected calculations

- **test_permission_denied_handling**
  - Login as user without material_purchase.view permission
  - Navigate to /material-purchase
  - Verify 403 error or access denied message

- **test_trigger_permission_denied**
  - Login as user with view but without trigger_erp permission
  - Navigate to /material-purchase
  - Verify trigger button disabled or hidden with tooltip

## Test Data Setup Notes
- Seed test database with material purchase records:
  ```
  Week: 2026-02-17, Factory: F1
    - P001, 產品A, qty: 1000, sp: 半成品A (SP001), kg_per_box: 5.50, basket: 5500, boxes_per_barrel: 20, barrels: 275, is_erp_triggered: false
    - P002, 產品B, qty: 500, sp: 半成品B (SP002), kg_per_box: 3.00, basket: 1500, boxes_per_barrel: 15, barrels: 100, is_erp_triggered: true, erp_order_no: "ERP-2026-001"

  Week: 2026-02-17, Factory: F2
    - P003, 產品C, qty: 750, sp: 半成品C (SP003), kg_per_box: 4.00, basket: 3000, boxes_per_barrel: 25, barrels: 120, is_erp_triggered: false

  Week: 2026-02-24, Factory: F1
    - (no data - for empty state test)
  ```
- Mock API responses in unit tests with MSW (Mock Service Worker) or jest.mock
- E2E tests use real backend with test database
- Clean up test data after E2E tests
- Create test users with different permission sets

## Mocking Strategy
- **Unit tests**: Mock axios API calls, mock Material UI components if needed
- **E2E tests**: Use real backend API, real database via Testcontainers
- Mock permissions by logging in with different test users
- Use MSW for consistent API mocking in development/testing
- Mock confirmation dialog responses in unit tests
