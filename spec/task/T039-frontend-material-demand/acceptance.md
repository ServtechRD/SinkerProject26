# T039: Acceptance Criteria

## Functional Acceptance Criteria
1. Page accessible at /material-demand route
2. Week picker displays and allows week selection
3. Factory dropdown displays factory options
4. Data table loads automatically when week or factory changes
5. Table displays all material demand fields
6. Empty state message shown when no data available
7. Loading indicator shown during data fetch
8. Error messages displayed for API failures
9. Decimal values formatted with 2 decimal places
10. Dates formatted in readable format (e.g., 2026-02-17 or Feb 17, 2026)
11. Rows with low inventory (estimatedInventory < demandQuantity) highlighted

## API Contracts
Uses contract from T038:
- GET /api/material-demand?week_start=YYYY-MM-DD&factory=XXX

## UI Acceptance Criteria

### Layout
- Page title: "Material Demand"
- Filter section at top with week picker and factory dropdown (horizontal layout)
- Data table below filters
- Responsive design works on desktop (1920x1080) and tablet (768px)

### Week Picker
- Allows selection of week start date
- Defaults to current week's Monday
- Format: YYYY-MM-DD or localized format
- Calendar popup for selection

### Factory Dropdown
- Shows available factories: F1, F2, F3 (or dynamic from API)
- Defaults to first factory or user's last selection
- Clear label: "Factory"

### Data Table
- Columns:
  - Material Code
  - Material Name
  - Unit
  - Last Purchase Date (nullable, show "-" if null)
  - Demand Date
  - Expected Delivery (right-aligned, 2 decimals)
  - Demand Quantity (right-aligned, 2 decimals)
  - Estimated Inventory (right-aligned, 2 decimals)
- Sortable columns
- Pagination if more than 100 rows
- Row highlighting: yellow/warning color if estimatedInventory < demandQuantity
- Read-only (no edit functionality)

### Feedback
- Loading spinner or skeleton during data fetch
- Empty state: "No material demand data for week [date] and factory [name]"
- Error toast: "Failed to load material demand data: [error message]"
- Toast notification for permission errors

## Non-Functional Criteria
- Page loads in under 2 seconds
- Data table renders smoothly with 1000 rows
- Week/factory changes trigger new query within 200ms
- No console errors or warnings
- Proper error handling for network failures
- Keyboard navigation support
- Mobile-friendly responsive design

## How to Verify
1. Navigate to /material-demand in browser
2. Verify page title and layout render correctly
3. Verify week picker shows current week
4. Verify factory dropdown shows available factories
5. Verify data table loads automatically
6. Click week picker, select different week
7. Verify table refreshes with new data
8. Change factory in dropdown
9. Verify table refreshes with factory-specific data
10. Select week/factory combination with no data
11. Verify empty state message displayed
12. Verify decimal columns show 2 decimal places
13. Verify thousand separators in large numbers (e.g., 1,234.56)
14. Insert test data where estimatedInventory < demandQuantity
15. Verify row highlighted in warning color
16. Verify Last Purchase Date column shows "-" for null values
17. Disconnect network and change filters
18. Verify error toast message appears
19. Test responsive design on tablet/mobile viewport
20. Verify table scrolls horizontally on small screens
21. Test with user lacking material_demand.view permission
22. Verify appropriate error message
