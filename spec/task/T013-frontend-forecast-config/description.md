# T013: Frontend - Forecast Config Management Page

## Context
React 18 + Material UI v5 application with JWT authentication. This task creates the frontend interface for managing monthly sales forecast configuration, allowing users to batch create months and edit settings.

## Goal
Implement a frontend page at /sales-forecast/config that allows authorized users to view, create, and edit monthly forecast configurations through a user-friendly Material UI interface.

## Scope

### In Scope
- Route /sales-forecast/config in React Router
- Main page component displaying list of month configurations
- Batch month creation dialog with start/end month pickers
- Edit configuration dialog with auto_close_day input and is_closed toggle
- Integration with backend API endpoints from T012
- Permission-based UI rendering
- Loading states and error handling
- Responsive table layout

### Out of Scope
- Actual forecast data management
- Excel upload/download functionality
- Real-time updates or WebSocket integration
- Month deletion functionality

## Requirements
- Display all month configurations in a Material UI Table (DataGrid)
- Table columns: Month, Auto Close Day, Status (Open/Closed), Closed At, Actions (Edit button)
- "Create Months" button opens dialog with:
  - Start Month picker (YYYYMM format)
  - End Month picker (YYYYMM format)
  - Create button (disabled if start > end)
- Edit dialog shows:
  - Month (read-only display)
  - Auto Close Day (number input, range 1-31)
  - Is Closed (toggle switch)
  - Save/Cancel buttons
- Visual indicators: Open months in green, Closed months in red/gray
- Show closed_at timestamp for closed months
- Permission check: require sales_forecast_config.view to view, sales_forecast_config.edit to create/edit
- Loading spinner during API calls
- Success/error toast notifications
- Form validation: month format, auto_close_day range

## Implementation Notes
- Use MUI DatePicker with custom format for YYYYMM month selection
- Use MUI DataGrid or Table component for listing
- Use MUI Dialog for modals
- Use MUI Switch for is_closed toggle
- API service module: src/api/forecastConfig.js with axios calls
- Handle 403 Forbidden by hiding create/edit buttons or redirecting
- Use React hooks: useState for state, useEffect for data fetching
- Implement optimistic updates or refetch after create/edit
- Format dates using date-fns or dayjs
- Store JWT token in context/localStorage and include in API headers

## Files to Change
- `src/pages/forecast/ForecastConfigPage.jsx` (new) - Main page component
- `src/components/forecast/CreateMonthsDialog.jsx` (new) - Batch creation dialog
- `src/components/forecast/EditConfigDialog.jsx` (new) - Edit configuration dialog
- `src/api/forecastConfig.js` (new) - API service functions
- `src/routes/index.jsx` (update) - Add route definition
- `src/App.jsx` (update if needed) - Route integration

## Dependencies
- T012: Backend API must be implemented and deployed
- T003: Frontend authentication and permission system must be in place
- Material UI v5 installed
- React Router configured
- Axios or fetch for HTTP requests
