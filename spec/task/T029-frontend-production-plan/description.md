# T029: Frontend - Production Plan Page

## Context
The production planning page provides a comprehensive interface for managing annual production plans. Users view and edit monthly allocations across all sales channels for each product in a wide grid layout, with automatic calculation of totals and differences from forecasts.

## Goal
Create a React frontend page for production planning with year selection, wide grid displaying products × channels × months (2-12), inline editing for monthly values and buffer quantities, and automatic total calculations.

## Scope

### In Scope
- React page component at route /production-plan
- Year selector (dropdown or picker)
- Wide data grid with horizontal scrolling
- Grid structure: Rows = Product × Channel combinations, Columns = Months 2-12, Buffer, Total, Forecast, Difference, Remarks
- Inline editing for monthly allocation cells, buffer quantity, and remarks
- Real-time calculation of total and difference as user types
- Save button to persist all changes
- Permission-based UI (edit controls require production_plan.edit)
- Responsive layout with horizontal scroll for wide grid
- Loading states and error handling
- Highlight cells with unsaved changes

### Out of Scope
- Copy plan from previous year
- Export to Excel functionality
- Data visualization/charts
- Bulk edit operations
- Plan approval workflow
- Comparison between years
- Forecast integration UI

## Requirements

### Page Layout
- Year selector at top (defaults to current year)
- "Load" button to fetch data for selected year
- Grid displaying all products and channels
- Save/Cancel buttons appear when changes exist
- Total count of products/channels displayed

### Data Grid Structure
**Columns**:
1. Product Code (fixed, frozen)
2. Product Name (fixed, frozen)
3. Category
4. Spec
5. Warehouse Location
6. Channel
7. Feb (Month 2) - editable
8. Mar (Month 3) - editable
9. Apr (Month 4) - editable
10. May (Month 5) - editable
11. Jun (Month 6) - editable
12. Jul (Month 7) - editable
13. Aug (Month 8) - editable
14. Sep (Month 9) - editable
15. Oct (Month 10) - editable
16. Nov (Month 11) - editable
17. Dec (Month 12) - editable
18. Buffer - editable
19. Total - calculated, read-only
20. Forecast - read-only
21. Difference - calculated, read-only
22. Remarks - editable

### Inline Editing
- Click cell to edit (if permission granted)
- Input validates decimal format (max 10 digits, 2 decimal places)
- Tab key moves to next editable cell
- Enter key saves cell and moves down
- ESC key cancels edit
- Changes highlighted (yellow background)
- Totals recalculate immediately when monthly values or buffer changes

### Calculation Display
- Total = sum(Feb through Dec) + Buffer
- Difference = Total - Forecast
- Update in real-time as user edits
- Format with 2 decimal places

### Save/Cancel Operations
- Save button sends PUT request for each modified row
- Progress indicator during save
- Success: show snackbar, refresh data
- Error: show error, keep changes for retry
- Cancel: discard all changes, revert to original data
- Warn before navigating away with unsaved changes

### Permissions
- Page requires production_plan.view to access
- Edit controls require production_plan.edit
- Read-only mode if edit permission missing

### UI/UX
- Frozen first 2 columns (Product Code, Product Name) for horizontal scroll
- Month columns have short headers (Feb, Mar, etc.)
- Numeric values right-aligned with 2 decimal formatting
- Empty cells show as "-" or "0.00"
- Highlight negative differences in red
- Highlight positive differences in green
- Responsive: minimum width with horizontal scroll on smaller screens

## Implementation Notes
- Use Material UI DataGrid with column freezing
- Use React Hook Form or local state for dirty tracking
- Validate on blur with real-time feedback
- Debounce calculation updates (100ms)
- Store unsaved changes in component state
- Use window.beforeunload for navigation warning
- Format numbers with Intl.NumberFormat
- Grid virtualization for performance with many rows
- Consider using AG Grid or similar for advanced grid features
- Month keys stored as "2"-"12" strings in API requests

## Files to Change
- `frontend/src/pages/production/ProductionPlanPage.jsx` (new)
- `frontend/src/components/production/ProductionPlanGrid.jsx` (new)
- `frontend/src/components/production/EditableCell.jsx` (new, optional)
- `frontend/src/api/productionPlan.js` (new)
- `frontend/src/App.jsx` (update routes)
- `frontend/src/utils/calculations.js` (new, for total/difference calc)

## Dependencies
- T028: Backend API must be available
- T003: Authentication and permission system
- Material UI v5 DataGrid or AG Grid
- React Router for navigation
- React Hook Form (optional)
