# T019: Frontend - Forecast List and Edit Page

## Context
React 18 + Material UI v5 application with JWT authentication. This task creates the main forecast data viewing and editing interface with version history support and modification tracking.

## Goal
Implement a frontend page at /sales-forecast that allows users to view, edit, add, and delete sales forecast data with version history navigation and visual indicators for modified items.

## Scope

### In Scope
- Route /sales-forecast in React Router
- Query filters: month and channel dropdowns
- Version selector dropdown showing all versions for selected month+channel
- Data table displaying all forecast items
- Visual highlighting (red) for is_modified=TRUE items
- Inline quantity editing
- Add new item dialog
- Delete item confirmation
- Permission-based UI: read-only for closed months (sales role), editable for production planner
- Integration with backend query and CRUD APIs

### Out of Scope
- Bulk editing (edit multiple items at once)
- Export functionality (covered in T021)
- Data validation against ERP in real-time
- Filtering/searching within table
- Pagination (implement if performance requires)

## Requirements
- Month dropdown: all configured months (open and closed)
- Channel dropdown: user's authorized channels or all channels (based on permissions)
- Version dropdown:
  - Populated after month+channel selected
  - Shows version strings sorted DESC
  - Defaults to latest version
  - Shows "Latest" label for current version
- Data table columns: Category, Spec, Product Code, Product Name, Warehouse Location, Quantity, Actions (Edit/Delete)
- Items with is_modified=TRUE highlighted with red row background or text color
- Quantity column: inline editable (click to edit, blur to save)
- Edit triggers PUT /api/sales-forecast/:id
- Delete button in Actions column with confirmation dialog
- "Add Item" button opens dialog with form: category, spec, product_code, product_name, warehouse_location, quantity
- Add dialog calls POST /api/sales-forecast
- Read-only mode:
  - For closed months: sales role users cannot edit/delete
  - Production planner role can edit even closed months
- Permission checks: sales_forecast.view/view_own, sales_forecast.edit, sales_forecast.delete, sales_forecast.create
- Loading states and error handling
- Success/error toast notifications

## Implementation Notes
- Use MUI DataGrid or Table component for data display
- Use MUI Dialog for Add Item modal
- Use MUI TextField for inline editing (controlled component)
- Fetch versions via GET /api/sales-forecast/versions?month=&channel=
- Fetch data via GET /api/sales-forecast?month=&channel=&version=
- Highlight modified rows using MUI sx prop or custom CSS class
- Implement debounce for inline edit to avoid excessive API calls
- Delete confirmation: use MUI Dialog with "Are you sure?" message
- Check user role and month status to enable/disable edit controls
- Use optimistic updates or refetch after edit/add/delete
- Display row count and version info above table
- Handle empty state: "No forecast data for selected month and channel"

## Files to Change
- `src/pages/forecast/ForecastListPage.jsx` (new)
- `src/components/forecast/AddItemDialog.jsx` (new)
- `src/api/forecast.js` (update - add query and CRUD functions)
- `src/routes/index.jsx` (update - add route)

## Dependencies
- T016: Backend CRUD API for add/edit/delete
- T017: Backend query API for listing and versions
- T003: Frontend authentication and permission system
- Material UI v5 DataGrid or Table
- React Router
