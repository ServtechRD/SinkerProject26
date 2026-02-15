# T043: Acceptance Criteria

## Functional Acceptance Criteria
1. Page accessible at /material-purchase route
2. Week picker displays and allows week selection
3. Factory dropdown displays factory options
4. Data table loads automatically when week or factory changes
5. Table displays all material purchase fields including calculated columns
6. "Trigger ERP" button appears for items with is_erp_triggered=false
7. "Triggered: [order_no]" badge appears for items with is_erp_triggered=true
8. Clicking "Trigger ERP" shows confirmation dialog
9. Confirming dialog triggers ERP API call
10. Successful trigger shows success toast and refreshes table
11. Failed trigger shows error toast
12. Empty state message shown when no data available
13. Loading indicator shown during data fetch and ERP trigger
14. Decimal values formatted with 2 decimal places

## API Contracts
Uses contracts from T041 and T042:
- GET /api/material-purchase?week_start=YYYY-MM-DD&factory=XXX
- POST /api/material-purchase/{id}/trigger-erp

## UI Acceptance Criteria

### Layout
- Page title: "Material Purchase Planning"
- Filter section at top with week picker and factory dropdown (horizontal layout)
- Data table below filters
- Responsive design works on desktop (1920x1080) and tablet (768px)

### Week Picker
- Allows selection of week start date
- Defaults to current week's Monday
- Format: YYYY-MM-DD or localized format
- Calendar popup for selection

### Factory Dropdown
- Shows available factories: F1, F2, F3 (or dynamic)
- Defaults to first factory or user's last selection
- Clear label: "Factory"

### Data Table
- Columns:
  - Product Code
  - Product Name
  - Quantity (right-aligned, 2 decimals)
  - Semi-Product Code
  - Semi-Product Name
  - Kg/Box (right-aligned, 2 decimals)
  - Basket Qty (right-aligned, 2 decimals)
  - Boxes/Barrel (right-aligned, 2 decimals)
  - Required Barrels (right-aligned, 2 decimals)
  - Status (chip/badge)
  - Action (button or badge)
- Sortable columns
- Pagination if more than 100 rows

### Action Column
- If is_erp_triggered=false:
  - "Trigger ERP" button (primary color)
  - Button disabled during API call (loading state)
- If is_erp_triggered=true:
  - "Triggered" badge or chip (success color)
  - Display order number: "ERP-XXXX"
  - No button (read-only)

### Confirmation Dialog
- Title: "Confirm ERP Trigger"
- Message: "Trigger ERP purchase order for [product_name]?"
- Show key details: Product Code, Quantity, Semi-Product
- Buttons: "Cancel" (secondary), "Confirm" (primary)

### Feedback
- Loading spinner or skeleton during data fetch
- Empty state: "No material purchase data for week [date] and factory [name]"
- Success toast: "ERP order created successfully: [order_no]"
- Error toast: "Failed to trigger ERP order: [error message]"
- 409 error toast: "Order already triggered: [order_no]"

## Non-Functional Criteria
- Page loads in under 2 seconds
- Data table renders smoothly with 1000 rows
- Week/factory changes trigger new query within 200ms
- ERP trigger completes in under 3 seconds
- No console errors or warnings
- Proper error handling for network failures
- Keyboard navigation support
- Mobile-friendly responsive design

## How to Verify
1. Navigate to /material-purchase in browser
2. Verify page title and layout render correctly
3. Verify week picker shows current week
4. Verify factory dropdown shows available factories
5. Verify data table loads automatically
6. Verify table shows calculated columns with 2 decimals
7. Verify "Trigger ERP" button appears for non-triggered items
8. Click "Trigger ERP" button
9. Verify confirmation dialog appears with product details
10. Click "Cancel" and verify dialog closes, no API call
11. Click "Trigger ERP" again and confirm
12. Verify loading state on button during API call
13. Verify success toast appears
14. Verify table refreshes automatically
15. Verify row now shows "Triggered: [order_no]" badge
16. Verify button replaced with badge (no longer clickable)
17. Change week picker to different week
18. Verify table refreshes with new data
19. Change factory dropdown
20. Verify table refreshes with factory-specific data
21. Select week/factory with no data
22. Verify empty state message displayed
23. Disconnect network and try to trigger ERP
24. Verify error toast appears
25. Test responsive design on tablet/mobile viewport
26. Verify table scrolls horizontally on small screens
27. Test with user lacking material_purchase.trigger_erp permission
28. Verify trigger button disabled or hidden with permission message
29. Test 409 error by triggering already-triggered item via API directly
30. Verify appropriate error message in toast
