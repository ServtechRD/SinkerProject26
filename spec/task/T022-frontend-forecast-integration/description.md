# T022: Frontend - Forecast Integration Page

## Context
React 18 + Material UI v5 application with JWT authentication. This task creates the final frontend page for viewing the 12-channel integrated sales forecast data with Excel export functionality.

## Goal
Implement a frontend page at /sales-forecast/integration that displays the consolidated 12-channel forecast data in a wide table format with version selection and Excel export capability.

## Scope

### In Scope
- Route /sales-forecast/integration in React Router
- Month selector dropdown
- Version selector dropdown
- Wide data table displaying all 12 channels + metadata + calculations
- Column headers for all fields
- "Export Excel" button
- Loading states and error handling
- Responsive layout (horizontal scroll for wide table on small screens)
- Integration with backend integration query and export APIs

### Out of Scope
- Editing data from integration page (use forecast list page)
- Filtering or searching products
- Charting or visualization
- Automatic refresh or real-time updates
- Pagination (load all data at once)

## Requirements
- Month selector: dropdown with all configured months
- Version selector: dropdown populated after month selected, defaults to latest
- Data table columns (in order):
  - 庫位 (Warehouse Location)
  - 中類名稱 (Category)
  - 貨品規格 (Spec)
  - 品名 (Product Name)
  - 品號 (Product Code)
  - PX/大全聯, 家樂福, 愛買, 711, 全家, OK/萊爾富, 好市多, 楓康, 美聯社, 康是美, 電商, 市面經銷 (12 channels)
  - 原始小計 (Original Subtotal)
  - 差異 (Difference)
  - 備註 (Remarks)
- Table displays all products sorted by category code (from backend)
- Highlight difference column: green for positive, red for negative, gray for zero
- "Export Excel" button calls GET /api/sales-forecast/integration/export
- Download Excel file with proper filename
- Permission check: sales_forecast.view required
- Display row count: "Showing {count} products"
- Display version info: "Version: {version}"
- Loading spinner while fetching data
- Error notifications for failed requests
- Empty state message if no data

## Implementation Notes
- Use MUI Select for month and version dropdowns
- Use MUI DataGrid for table (supports wide tables, horizontal scroll, frozen columns)
- Or use MUI Table with TableContainer for horizontal scroll
- Consider freezing first few columns (metadata) for better UX
- API service: src/api/forecastIntegration.js
- Functions: fetchIntegrationData(month, version), exportIntegrationExcel(month, version)
- Export button triggers file download via blob URL
- Number formatting: display quantities with thousand separator and 2 decimals
- Difference column: conditional styling based on value (positive/negative/zero)
- Remarks column: display auto-generated remarks from backend
- Use MUI Chip or Badge for difference highlighting
- Responsive: table scrolls horizontally on small screens
- Show table summary: total products, total subtotal across all products

## Files to Change
- `src/pages/forecast/ForecastIntegrationPage.jsx` (new)
- `src/api/forecastIntegration.js` (new)
- `src/routes/index.jsx` (update - add route)
- `src/App.jsx` (update if needed)

## Dependencies
- T020: Backend integration query API
- T021: Backend Excel export API
- T003: Frontend authentication and permission system
- Material UI v5 DataGrid or Table
- React Router
- Axios for HTTP requests
