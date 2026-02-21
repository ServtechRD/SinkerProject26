# Acceptance Criteria — T033

## Functional
1. Page accessible at `/weekly-schedule` for authenticated users with `weekly_schedule.view` permission.
2. Week picker only allows selecting Monday dates.
3. Factory dropdown lists available factories.
4. Selecting week + factory and clicking "Query" loads schedule data for that week/factory.
5. Drag-and-drop .xlsx upload works; non-.xlsx files are rejected with error message.
6. Template download button downloads an .xlsx template file.
7. After upload, data table refreshes with new data.
8. Upload errors (validation failures) displayed clearly showing row-level issues.
9. Inline edit: clicking quantity or demand_date cell enters edit mode.
10. Save sends updated data to backend; success shows confirmation snackbar.
11. Users without `weekly_schedule.upload` permission cannot see upload area.
12. Users without `weekly_schedule.edit` permission see read-only table.

## UI Acceptance
- Week picker shows calendar with non-Monday dates greyed out / disabled
- Factory dropdown is a standard MUI Select
- Upload area shows drag-and-drop zone with file type hint
- Data table columns: Demand Date, Product Code, Product Name, Warehouse, Quantity
- Edit mode: cell shows input field; Enter confirms, Escape cancels
- Loading spinner during upload and query
- Responsive layout for common screen sizes

## Non-Functional
- Upload files up to 5MB accepted
- Table handles up to 500 rows without performance issues
- Form validation: quantity must be positive number, demand_date must be within the selected week

## How to Verify
1. Start frontend dev server: `cd frontend && npm run dev`
2. Login as production planner user
3. Navigate to /weekly-schedule via sidebar
4. Select a Monday date, select factory, click Query
5. Upload an xlsx file — verify data appears in table
6. Edit a quantity cell, save — verify API call succeeds
7. Verify non-Monday dates are not selectable
8. Verify .csv or .xls files are rejected
