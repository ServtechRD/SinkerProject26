# T022: Acceptance Criteria

## Functional Acceptance Criteria

### Page Access
- [ ] Route /sales-forecast/integration accessible after authentication
- [ ] User without sales_forecast.view permission redirected or sees access denied
- [ ] Page displays loading state while fetching initial data

### Month and Version Selection
- [ ] Month dropdown populated with all configured months
- [ ] Selecting month loads versions for that month
- [ ] Version dropdown shows all versions sorted DESC (newest first)
- [ ] Latest version auto-selected by default
- [ ] Selecting different version reloads table data
- [ ] Version info displayed: "Version: {version}, Products: {count}"

### Data Table Display
- [ ] Table displays 20 columns in correct order
- [ ] Columns: 庫位, 中類名稱, 貨品規格, 品名, 品號, 12 channel names, 原始小計, 差異, 備註
- [ ] All data loaded and displayed correctly
- [ ] Products sorted by category code (from backend)
- [ ] Quantity columns formatted with thousand separator and 2 decimals (e.g., 1,234.56)
- [ ] Zero values displayed as "0.00" (not empty)
- [ ] Table scrolls horizontally on small screens
- [ ] Header row sticky/frozen when scrolling vertically

### Difference Column Highlighting
- [ ] Positive difference (> 0) shown in green
- [ ] Negative difference (< 0) shown in red
- [ ] Zero difference shown in gray or no color
- [ ] Color coding clear and visible

### Remarks Column
- [ ] Displays auto-generated remarks from backend
- [ ] Common values: 新增產品, 數量增加, 數量減少, 無變化
- [ ] Text readable and not truncated

### Excel Export
- [ ] "Export Excel" button visible
- [ ] Button disabled while data loading
- [ ] Click triggers file download
- [ ] Downloaded file named "sales_forecast_integration_{month}_{timestamp}.xlsx"
- [ ] Excel file contains same data as displayed in table
- [ ] Export works for different months and versions

### Loading and Error States
- [ ] Loading spinner shown while fetching data
- [ ] Table disabled during loading
- [ ] Error notification if data fetch fails
- [ ] Error notification if export fails
- [ ] Retry mechanism or clear error message

### Empty State
- [ ] If no data for month+version, show message: "No integration data available"
- [ ] Empty state includes month and version info

### Permission
- [ ] User without sales_forecast.view cannot access page
- [ ] Permission checked on mount and data fetch

## UI Acceptance Criteria

### Layout
- [ ] Page title: "Sales Forecast Integration - 12 Channels"
- [ ] Filters section: Month and Version selectors in row
- [ ] "Export Excel" button aligned right
- [ ] Info row: version info and product count
- [ ] Table fills remaining vertical space
- [ ] Responsive: filters stack on mobile

### Table Styling
- [ ] Header row bold with background color
- [ ] Channel columns clearly labeled with Chinese names
- [ ] Numeric columns (12 channels + subtotal + difference) right-aligned
- [ ] Text columns (metadata, remarks) left-aligned
- [ ] Alternating row colors for readability
- [ ] Hover effect on rows
- [ ] Table borders for clarity

### Number Formatting
- [ ] Quantities display as "1,234.56" format
- [ ] Consistent decimal places (2 digits)
- [ ] No scientific notation for large numbers

### Difference Highlighting
- [ ] Green badge or text for positive values: "+50.00"
- [ ] Red badge or text for negative values: "-30.00"
- [ ] Gray or plain text for zero: "0.00"

### Responsive Behavior
- [ ] Table container has horizontal scroll on small screens
- [ ] First few columns (metadata) frozen when scrolling horizontally (optional but nice UX)
- [ ] Export button accessible on mobile
- [ ] Page usable on tablet and phone

## Non-Functional Criteria
- [ ] Table renders 500 products within 3 seconds
- [ ] Smooth scrolling (no lag)
- [ ] Excel export triggers within 1 second
- [ ] No console errors or warnings
- [ ] Accessible: table headers have proper ARIA labels
- [ ] Keyboard navigation works

## How to Verify

### Manual Testing
1. **Page Access:**
   - Login with sales_forecast.view permission
   - Navigate to /sales-forecast/integration
   - Verify page loads

2. **Month and Version Selection:**
   - Select month "202601"
   - Verify version dropdown populates
   - Verify latest version auto-selected
   - Verify table displays data

3. **Version History:**
   - Upload data (version 1)
   - Edit data (version 2)
   - Navigate to integration page
   - Verify 2 versions in dropdown
   - Select version 1 - verify old data
   - Select version 2 - verify new data

4. **Table Display:**
   - Verify all 20 columns visible (may need horizontal scroll)
   - Verify column headers in Chinese
   - Verify data matches integration API response
   - Verify sorting by category code

5. **Number Formatting:**
   - Verify quantities show thousand separators: "1,234.56"
   - Verify consistent 2 decimal places

6. **Difference Highlighting:**
   - Verify products with positive difference show green
   - Verify products with negative difference show red
   - Verify products with zero difference show gray

7. **Remarks Display:**
   - Verify remarks column shows values from API
   - Check for "新增產品", "數量增加", etc.

8. **Excel Export:**
   - Click "Export Excel"
   - Verify file downloads
   - Open file in Excel
   - Verify data matches table display
   - Verify formatting applied

9. **Responsive:**
   - Resize browser to mobile width
   - Verify table scrolls horizontally
   - Verify filters accessible
   - Verify export button accessible

10. **Permission Test:**
    - Login without sales_forecast.view
    - Navigate to /sales-forecast/integration
    - Verify access denied

11. **Empty State:**
    - Select month with no data
    - Verify empty state message

12. **Error Handling:**
    - Disconnect network
    - Attempt to load data
    - Verify error notification
    - Reconnect and retry

13. **All 12 Channels:**
    - Upload data to all 12 channels
    - Verify all channel columns show data
    - Verify subtotal calculation correct

14. **Large Dataset:**
    - Load 500 products
    - Verify table renders smoothly
    - Verify scrolling performs well

### Automated Testing
- Component render tests
- Table data display tests
- Export button functionality tests
- Version selection tests
- Permission-based rendering tests
