# T026: Frontend - Inventory Integration Page

## Context
Users need a web interface to query and view integrated inventory, sales, and forecast data. The page supports two query modes: real-time queries (by month and date range) and historical version queries. Users with edit permissions can modify the production subtotals, which generates new versions.

## Goal
Create a React frontend page for the inventory integration module with query controls, data table display, inline editing capability for modified_subtotal, and version management.

## Scope

### In Scope
- React page component at route /inventory-integration
- Query form with month picker, date range pickers, and version selector
- Real-time query mode: month + optional date range
- Version query mode: version dropdown populated from previous queries
- Data table with all integration fields
- Inline edit for modified_subtotal column (users with inventory.edit permission)
- Save button to persist changes (generates new version)
- Loading states and error handling
- Permission-based UI (hide edit controls if no inventory.edit)
- Responsive layout using Material UI

### Out of Scope
- Export to Excel functionality
- Data visualization/charts
- Bulk edit operations
- Version comparison UI
- Delete/archive functionality
- Print functionality

## Requirements

### Query Controls
- Month picker (YYYY-MM format)
- Start date picker (optional, defaults to first day of month)
- End date picker (optional, defaults to last day of month)
- Version selector (dropdown, populated from API or local storage)
- "Query" button to execute real-time query
- "Load Version" button to load historical data

### Data Table
- Columns: Product Code, Product Name, Category, Spec, Warehouse Location, Sales Quantity, Inventory Balance, Forecast Quantity, Production Subtotal, Modified Subtotal, Version
- Display all numeric values with 2 decimal places
- Modified Subtotal column editable (if user has inventory.edit permission)
- Highlight rows where modified_subtotal differs from production_subtotal
- Sort by product code by default
- Support client-side sorting on all columns

### Edit Functionality
- Click modified_subtotal cell to edit (if permission granted)
- Input validates decimal format (max 10 digits, 2 decimal places)
- "Save" button appears when changes exist
- Save calls PUT /api/inventory-integration/:id for each changed row
- After save, new version is generated, page refreshes with new data
- Cancel button discards changes

### Version Management
- After successful query, version is stored in state
- Version dropdown shows recent versions (last 10, stored in localStorage)
- Selecting version loads that snapshot
- Display current version prominently

### Permissions
- Page requires inventory.view permission to access
- Edit controls require inventory.edit permission
- Hide/disable edit features if permission missing

### State Management
- Loading states during API calls
- Error states with user-friendly messages
- Dirty state tracking for unsaved changes
- Warn user before navigating away with unsaved changes

## Implementation Notes
- Use Material UI DatePicker for month/date selection
- Use Material UI Select for version dropdown
- Use Material UI DataGrid or custom table with inline edit
- Store recent versions in localStorage (max 10)
- Validate dates: start_date < end_date, both within selected month
- Use React Query or SWR for API state management
- Debounce input validation
- Use React Context for permission checking
- Format numbers with Intl.NumberFormat
- Responsive breakpoints: mobile (<600px), tablet (600-960px), desktop (>960px)

## Files to Change
- `frontend/src/pages/inventory/InventoryIntegrationPage.jsx` (new)
- `frontend/src/api/inventory.js` (new)
- `frontend/src/components/inventory/InventoryDataTable.jsx` (new, optional)
- `frontend/src/components/inventory/QueryForm.jsx` (new, optional)
- `frontend/src/App.jsx` (update routes)

## Dependencies
- T024: Backend API for fetching inventory integration data
- T025: Backend API for editing modified_subtotal
- T003: Authentication and permission system
- Material UI v5 components
- React Router for navigation
- React Hook Form for form handling (optional)
