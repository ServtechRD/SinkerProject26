# T043: Frontend - Material Purchase Page

## Context
This task implements the frontend interface for viewing material purchase planning data and triggering ERP purchase orders. Users need to query purchase requirements by week and factory, view BOM calculations (baskets, barrels), and trigger ERP order creation for individual items. The page provides visual feedback for already-triggered items.

## Goal
Create a React-based frontend page at /material-purchase with week picker, factory dropdown, read-only data table with calculated columns, and per-row "Trigger ERP" button with confirmation dialog.

## Scope

### In Scope
- React page component at /material-purchase route
- Week picker (select week start date)
- Factory dropdown selector
- Read-only data table showing purchase items with calculated columns
- "Trigger ERP" button per row
- Button disabled if already triggered (shows order number instead)
- Confirmation dialog before ERP trigger
- Loading states and error handling
- Integration with T041 and T042 backend APIs
- Material UI v5 components

### Out of Scope
- Batch trigger (trigger all items at once)
- ERP order status tracking
- Excel export
- Data editing capabilities
- Advanced filtering or search
- Permission management UI

## Requirements
- **Route**: /material-purchase
- **Components**:
  - MaterialPurchasePage.jsx (main page component)
  - Week picker control
  - Factory dropdown
  - Data table with columns: Product Code, Product Name, Quantity, Semi-Product, Kg/Box, Basket Qty, Boxes/Barrel, Required Barrels, Status, Action
  - "Trigger ERP" button (or "Triggered: [order_no]" badge)
  - Confirmation dialog component
- **API Integration**:
  - Query: GET /api/material-purchase?week_start=&factory=
  - Trigger: POST /api/material-purchase/:id/trigger-erp
- **UI/UX**:
  - Week picker defaults to current week
  - Factory dropdown defaults to first factory
  - Table auto-loads when week or factory changes
  - Loading spinner during data fetch
  - "Trigger ERP" button: enabled if is_erp_triggered=false
  - "Triggered: ERP-XXX" badge if is_erp_triggered=true
  - Confirmation dialog: "Trigger ERP purchase order for [product_name]?"
  - Success toast after successful trigger
  - Table refreshes after successful trigger
  - Responsive design

## Implementation Notes
- Use Material UI DatePicker with week picker mode
- Factory dropdown: hardcoded list or dynamic from API
- Use Material UI DataGrid or Table component
- Trigger button in each table row (Actions column)
- Confirmation dialog using Material UI Dialog
- After successful trigger, refresh table data to show updated status
- Display decimal values with 2 decimal places and thousand separators
- Use axios for API calls in src/api/materialPurchase.js
- Toast notifications using notistack or Material UI Snackbar
- Handle 409 error (already triggered) with appropriate message
- Disable trigger button during API call (prevent double-click)

## Files to Change
- Create: `src/pages/material/MaterialPurchasePage.jsx`
- Create: `src/api/materialPurchase.js`
- Update: `src/App.jsx` or routing configuration to add /material-purchase route

## Dependencies
- T041: Backend material purchase query API
- T042: Backend ERP trigger API
- T003: Authentication and base layout infrastructure
