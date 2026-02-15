# T036: Acceptance Criteria

## Functional Acceptance Criteria
1. Page accessible at /semi-product route
2. Upload zone accepts Excel files via click and drag-and-drop
3. Upload zone rejects non-.xlsx files with error message
4. Template download button downloads Excel file with correct headers
5. Data table displays all semi-products after page load
6. Table shows columns: Product Code, Product Name, Advance Days
7. Clicking Advance Days cell enables inline editing
8. Saving edited value calls PUT API and updates table
9. Invalid advance days (zero, negative, non-numeric) shows validation error
10. Confirmation dialog appears before upload
11. Upload replaces all data and refreshes table
12. Success/error messages appear for all operations

## API Contracts
Uses contracts from T035:
- POST /api/semi-product/upload (multipart)
- GET /api/semi-product
- PUT /api/semi-product/{id}
- GET /api/semi-product/template

## UI Acceptance Criteria

### Layout
- Page title: "Semi Product Advance Purchase Configuration"
- Upload section at top with drag-and-drop zone
- Template download button next to upload zone
- Data table below upload section
- Responsive design works on desktop (1920x1080) and tablet (768px)

### Upload Zone
- Dashed border rectangle with "Drop Excel file here or click to browse" text
- Shows file name when file selected
- Shows progress spinner during upload
- Clear/remove file button when file selected

### Data Table
- Columns: Product Code, Product Name, Advance Days, Last Updated
- Sortable columns
- Pagination if more than 100 rows
- Advance Days column has edit icon indicator
- Inline edit: click cell → input field → save on Enter or blur, cancel on Escape

### Feedback
- Toast notification: "Upload successful: X products loaded"
- Toast notification: "Advance days updated successfully"
- Error toast: "Upload failed: [error details]"
- Confirmation dialog: "Uploading will replace all existing data. Continue?"
- Loading spinner during data fetch

## Non-Functional Criteria
- Page loads in under 2 seconds
- Upload of 1000-row file completes in under 10 seconds
- Table renders smoothly with 5000 rows (virtualization if needed)
- No console errors or warnings
- Proper error handling for network failures
- Accessible keyboard navigation for editing
- Mobile-friendly responsive design

## How to Verify
1. Navigate to /semi-product in browser
2. Verify page title and layout render correctly
3. Click "Download Template" button
4. Verify Excel file downloads with headers: 品號, 品名, 提前日數
5. Add sample data to template (10 rows)
6. Drag Excel file onto upload zone
7. Verify file name appears
8. Click upload button
9. Verify confirmation dialog appears
10. Confirm upload
11. Verify progress indicator appears
12. Verify success toast with "10 products loaded"
13. Verify table displays 10 rows
14. Click on an Advance Days cell
15. Verify input field appears
16. Enter new value (e.g., 15)
17. Press Enter
18. Verify cell updates and success toast appears
19. Try entering invalid value (e.g., -5)
20. Verify validation error message
21. Upload new file with 5 rows
22. Verify table now shows only 5 rows (previous 10 replaced)
23. Test drag-and-drop: drag Excel file onto zone
24. Verify file accepted and name shown
25. Try uploading .pdf file
26. Verify error message "Only .xlsx files are allowed"
