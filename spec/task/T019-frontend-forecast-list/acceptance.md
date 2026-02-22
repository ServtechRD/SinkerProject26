# T019: Acceptance Criteria

## Functional Acceptance Criteria

### Page Access and Filters
- [ ] Route /sales-forecast accessible after authentication
- [ ] User without sales_forecast.view or sales_forecast.view_own redirected or sees access denied
- [ ] Month dropdown shows all configured months
- [ ] Channel dropdown shows user's authorized channels (filtered by permission)
- [ ] Version dropdown disabled until month+channel selected
- [ ] Selecting month+channel loads versions and defaults to latest

### Version Selection
- [ ] Version dropdown populated with all versions for month+channel
- [ ] Versions sorted DESC (newest first)
- [ ] Latest version marked with "(Latest)" label
- [ ] Selecting version loads corresponding data
- [ ] Version info displayed: "Version: {version}, Items: {count}"

### Data Table Display
- [ ] Table shows columns: Category, Spec, Product Code, Product Name, Warehouse Location, Quantity, Actions
- [ ] Data sorted by category, spec, product_code (from backend)
- [ ] Items with is_modified=TRUE highlighted in red (background or text)
- [ ] Loading spinner shown while fetching data
- [ ] Empty state message: "No forecast data for selected month and channel"
- [ ] Row count displayed: "Showing {count} items"

### Inline Quantity Editing
- [ ] Click quantity cell to edit (shows text field)
- [ ] Enter new value and blur saves (calls PUT API)
- [ ] Escape key cancels edit
- [ ] Enter key saves edit
- [ ] Invalid quantity (negative, non-numeric) shows error
- [ ] Success notification: "Quantity updated"
- [ ] Row becomes highlighted red after edit (is_modified=TRUE)
- [ ] Disabled for closed months (sales role)
- [ ] Enabled for closed months (production planner role)

### Add Item
- [ ] "Add Item" button visible to users with sales_forecast.create permission
- [ ] Disabled if month is closed (sales role)
- [ ] Enabled even if month closed (production planner role)
- [ ] Click opens Add Item dialog
- [ ] Dialog has fields: Category, Spec, Product Code, Product Name, Warehouse Location, Quantity
- [ ] All fields required except Category, Spec, Warehouse Location (optional)
- [ ] Quantity must be positive number
- [ ] Save button calls POST API
- [ ] Success adds item to table and shows notification
- [ ] Duplicate product_code shows error
- [ ] Cancel button closes dialog without changes

### Delete Item
- [ ] Delete button in Actions column
- [ ] Visible to users with sales_forecast.delete permission
- [ ] Disabled for closed months (sales role)
- [ ] Enabled for closed months (production planner role)
- [ ] Click shows confirmation dialog: "Delete {product_name}?"
- [ ] Confirm calls DELETE API and removes row
- [ ] Success notification: "Item deleted"
- [ ] Cancel closes dialog without deleting

### Permission-Based Access
- [ ] User with sales_forecast.view can view any channel
- [ ] User with sales_forecast.view_own can only view owned channels
- [ ] Edit/Delete disabled without sales_forecast.edit/delete permissions
- [ ] Add disabled without sales_forecast.create permission

### Read-Only Mode (Closed Months)
- [ ] Sales role: closed month → all edit/add/delete disabled
- [ ] Production planner role: closed month → all edit/add/delete enabled
- [ ] Banner or indicator shows: "Month is closed" for sales role
- [ ] No banner for production planner (always editable)

### Error Handling
- [ ] Network error shows: "Failed to load forecast data"
- [ ] Permission error shows: "Access denied"
- [ ] Validation error shows inline with field
- [ ] API errors displayed in toast notifications

## UI Acceptance Criteria

### Layout
- [ ] Page title: "Sales Forecast"
- [ ] Filters section at top: Month, Channel, Version in row
- [ ] "Add Item" button aligned right
- [ ] Table fills remaining space
- [ ] Responsive: filters stack on mobile

### Table Styling
- [ ] Header row bold with background color
- [ ] Alternating row colors for readability
- [ ] Modified rows (is_modified=TRUE) with red highlight
- [ ] Hover effect on rows
- [ ] Actions column aligned right
- [ ] Quantity column right-aligned (numeric)

### Dialogs
- [ ] Add Item dialog centered, modal (dim background)
- [ ] Form fields stacked vertically
- [ ] Save/Cancel buttons at bottom
- [ ] Delete confirmation dialog centered

### Visual Indicators
- [ ] Red highlighting clearly distinguishes modified items
- [ ] Loading spinner centered in table area
- [ ] Empty state with icon and message

## Non-Functional Criteria
- [ ] Table renders 500 items within 2 seconds
- [ ] Inline edit responsive (no lag on input)
- [ ] Version switching loads new data within 1 second
- [ ] No console errors or warnings
- [ ] Accessible: keyboard navigation, ARIA labels

## How to Verify

### Manual Testing
1. **Page Access:**
   - Login with view permission
   - Navigate to /sales-forecast
   - Verify page loads with filters

2. **Filter and Load Data:**
   - Select month "202601"
   - Select channel "大全聯"
   - Verify versions dropdown populates
   - Verify latest version auto-selected
   - Verify table shows data

3. **Version History:**
   - Upload data (version 1)
   - Edit item (version 2)
   - In forecast list page, verify 2 versions in dropdown
   - Select version 1 - verify old data
   - Select version 2 - verify new data

4. **Modified Items Highlighting:**
   - Upload data (is_modified=FALSE)
   - Edit one item
   - Verify edited item highlighted in red

5. **Inline Edit:**
   - Click quantity cell
   - Change value to 200
   - Press Enter or blur
   - Verify API called
   - Verify success notification
   - Verify row becomes red

6. **Add Item:**
   - Click "Add Item"
   - Fill all fields
   - Click Save
   - Verify item added to table
   - Verify success notification

7. **Delete Item:**
   - Click Delete on any item
   - Verify confirmation dialog
   - Click Confirm
   - Verify item removed from table

8. **Read-Only Mode (Sales Role):**
   - Login as sales role user
   - Select closed month
   - Verify edit/add/delete buttons disabled
   - Verify banner: "Month is closed"

9. **Production Planner Override:**
   - Login as production planner
   - Select closed month
   - Verify edit/add/delete enabled

10. **Permission Tests:**
    - Login without edit permission
    - Verify quantity cells not editable
    - Verify Add/Delete buttons hidden

11. **Validation:**
    - Try inline edit with negative quantity
    - Verify error message
    - Try add with empty required fields
    - Verify validation errors

12. **Empty State:**
    - Select month+channel with no data
    - Verify empty state message

### Automated Testing
- Component render tests
- Table data display tests
- Inline edit flow tests
- Add/delete dialog tests
- Permission-based rendering tests
