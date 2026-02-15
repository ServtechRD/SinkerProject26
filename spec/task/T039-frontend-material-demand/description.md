# T039: Frontend - Material Demand Page

## Context
This task implements the frontend interface for viewing material demand data. Users need to query material requirements by selecting a week and factory, then view the results in a read-only data table. This page helps production planners understand what materials are needed and when based on production schedules.

## Goal
Create a React-based frontend page at /material-demand with week picker, factory dropdown filter, and read-only data table displaying material requirements.

## Scope

### In Scope
- React page component at /material-demand route
- Week picker (select week start date)
- Factory dropdown selector
- Read-only data table showing material demand
- Auto-query on week/factory change
- Loading state indicator
- Empty state message when no data
- Error handling and user feedback
- Integration with T038 backend API
- Material UI v5 components

### Out of Scope
- Data editing capabilities (read-only)
- Excel export (may be added later)
- Advanced filtering (material code search)
- Aggregation or summary views
- Print functionality
- Permission management UI

## Requirements
- **Route**: /material-demand
- **Components**:
  - MaterialDemandPage.jsx (main page component)
  - Week picker control (date picker for week start)
  - Factory dropdown (populated from API or config)
  - Data table with columns: Material Code, Material Name, Unit, Last Purchase Date, Demand Date, Expected Delivery, Demand Quantity, Estimated Inventory
- **API Integration**:
  - Query: GET /api/material-demand?week_start=&factory=
- **UI/UX**:
  - Week picker defaults to current week
  - Factory dropdown defaults to first factory or user's default
  - Table auto-loads when week or factory changes
  - Loading spinner during data fetch
  - Empty state: "No material demand data for selected week and factory"
  - Responsive design
  - Decimal values display with 2 decimal places
  - Dates display in localized format

## Implementation Notes
- Use Material UI DatePicker with week picker mode or custom week selector
- Factory dropdown: hardcoded list ["F1", "F2", "F3"] or fetch from API if available
- Use Material UI DataGrid or Table component for data display
- Auto-query on week/factory change using useEffect
- Display quantities with thousand separators and 2 decimals
- Highlight rows where estimatedInventory < demandQuantity (low stock warning)
- Use axios for API calls in src/api/materialDemand.js
- Error handling: toast notification for API errors
- Implement loading state with skeleton or spinner

## Files to Change
- Create: `src/pages/material/MaterialDemandPage.jsx`
- Create: `src/api/materialDemand.js`
- Update: `src/App.jsx` or routing configuration to add /material-demand route

## Dependencies
- T038: Backend API must be implemented
- T003: Authentication and base layout infrastructure
