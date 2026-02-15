# T029: Acceptance Criteria

## Functional Acceptance Criteria

### Page Access
- [ ] Route /production-plan accessible when logged in with production_plan.view
- [ ] Redirects to login if not authenticated
- [ ] Shows "Access Denied" if missing production_plan.view permission
- [ ] Navigation menu includes link to production plan page

### Year Selection and Data Loading
- [ ] Year selector defaults to current year
- [ ] Year selector shows years 2020-2030 (or configurable range)
- [ ] "Load" button fetches data for selected year
- [ ] Loading spinner shows during API call
- [ ] Success: grid populated with data
- [ ] Error: error message displayed
- [ ] Empty state shown if no data for year

### Grid Display
- [ ] All 22 columns displayed with correct headers
- [ ] Product Code and Product Name columns frozen (stay visible during horizontal scroll)
- [ ] Months labeled as Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
- [ ] Numeric columns (months, buffer, total, forecast, difference) right-aligned
- [ ] Values formatted with 2 decimal places and thousands separators
- [ ] Empty/null monthly values show as "0.00" or "-"
- [ ] Total column shows calculated sum
- [ ] Difference column shows calculated difference
- [ ] Negative differences displayed in red
- [ ] Positive differences displayed in green
- [ ] Horizontal scroll works smoothly
- [ ] Multiple channels for same product shown in separate rows

### Inline Editing (with production_plan.edit permission)
- [ ] Monthly allocation cells editable on click
- [ ] Buffer quantity cell editable on click
- [ ] Remarks cell editable on click
- [ ] Total and Forecast cells are read-only
- [ ] Difference cell is read-only
- [ ] Product info cells are read-only
- [ ] Clicking cell shows input field
- [ ] Input validates decimal format in real-time
- [ ] Invalid input shows error indicator
- [ ] Tab key moves to next editable cell horizontally
- [ ] Enter key saves and moves down to same column
- [ ] ESC key cancels edit and reverts value
- [ ] Modified cells highlighted (yellow background)

### Real-Time Calculations
- [ ] Changing any monthly value immediately updates Total
- [ ] Changing buffer quantity immediately updates Total
- [ ] Total updates within 100ms of input change
- [ ] Difference recalculates when Total changes
- [ ] All calculations preserve 2 decimal precision
- [ ] Calculations correct for sparse allocations (missing months = 0)

### Save/Cancel Operations
- [ ] Save and Cancel buttons appear when any cell modified
- [ ] Button count shows number of modified rows
- [ ] Clicking Save calls PUT API for each modified row
- [ ] Progress bar shows during save operation
- [ ] Success: green snackbar "X records saved successfully"
- [ ] Success: grid refreshes with server data
- [ ] Error: red snackbar with error message
- [ ] Error: changes preserved for retry
- [ ] Cancel: confirmation dialog if >5 rows modified
- [ ] Cancel: all changes reverted to original values
- [ ] Cancel: highlights removed

### Read-Only Mode (without production_plan.edit permission)
- [ ] All cells non-editable
- [ ] Clicking cells does nothing
- [ ] No Save/Cancel buttons visible
- [ ] Grid displays normally with all data

### Navigation Warning
- [ ] Browser warns before navigation if unsaved changes exist
- [ ] Warning shows count of unsaved changes
- [ ] Accepting warning discards changes
- [ ] Canceling warning stays on page

### Responsive Design
- [ ] Desktop (>1200px): Full grid visible with minimal scroll
- [ ] Tablet (768-1200px): Grid scrolls horizontally, all functions work
- [ ] Mobile (<768px): Frozen columns help, horizontal scroll, edit modal instead of inline (optional)

## UI Acceptance Criteria

### Layout
```
+---------------------------------------------------------------------------------+
|  Production Plan - 2026                                     [Save] [Cancel]     |
|                                                                                 |
|  [Year: 2026 ▼] [Load]                              Total: 150 products/channels |
+---------------------------------------------------------------------------------+
| Code   | Name    | Cat | Spec | WH | Ch | Feb | Mar |...| Dec | Buf | Total | Fcst | Diff | Remarks |
+---------------------------------------------------------------------------------+
| PROD001| Prod 1  | A   | S1   | WH | D  | 100 | 150 |...| 200 | 50  | 2370  | 2300 | +70  | Adjusted |
| PROD001| Prod 1  | A   | S1   | WH | R  | 50  | 75  |...| 100 | 25  | 1150  | 1200 | -50  |          |
+---------------------------------------------------------------------------------+
       ↑                                                   ↑
   Frozen columns                          Horizontal scroll →
```

### Color Scheme
- Modified cells: Light yellow (#FFF9C4)
- Negative difference: Red text (#F44336)
- Positive difference: Green text (#4CAF50)
- Frozen columns: Light gray background (#F5F5F5)
- Header: Dark gray background (#E0E0E0)
- Edit focus: Blue border (#2196F3)
- Invalid input: Red border (#F44336)

### Interactions
- Hover on editable cell: Subtle border
- Click cell: Replace with TextField
- Invalid input: Red border + shake animation
- Save success: Green snackbar bottom center
- Save error: Red snackbar bottom center
- Loading: Linear progress bar at top

## Non-Functional Criteria
- [ ] Grid renders 500 rows without lag
- [ ] Scroll performance smooth (60fps)
- [ ] Calculation updates feel instant (<100ms)
- [ ] Save operation completes in <5 seconds for 100 rows
- [ ] No console errors or warnings
- [ ] Keyboard navigation fully functional
- [ ] Screen reader announces editable cells and values
- [ ] Column headers have proper ARIA labels

## How to Verify

### 1. Test Page Access
```
1. Login as user with production_plan.view
2. Navigate to /production-plan
3. Verify page loads successfully
4. Verify year selector and load button present
```

### 2. Test Data Loading
```
1. Select year: 2026
2. Click "Load"
3. Verify API call: GET /api/production-plan?year=2026
4. Verify grid populated with data
5. Verify multiple channels for same product shown separately
```

### 3. Test Frozen Columns
```
1. Load data with wide grid
2. Scroll horizontally to right
3. Verify Product Code and Product Name stay visible
4. Verify month columns scroll out of view
```

### 4. Test Inline Edit with Permission
```
1. Login as user with production_plan.edit
2. Load data
3. Click on a February cell (month 2)
4. Verify input field appears
5. Type "150.50"
6. Press Enter or Tab
7. Verify value updated
8. Verify cell highlighted yellow
9. Verify Total recalculated
10. Verify Difference recalculated
11. Verify Save button appears
```

### 5. Test Real-Time Calculation
```
1. Load data
2. Note original Total and Difference for a row
3. Edit February value from 100 to 200
4. Verify Total increases by 100
5. Verify Difference increases by 100
6. Edit Buffer from 50 to 100
7. Verify Total increases by 50
8. Verify Difference increases by 50
9. Verify calculations accurate to 2 decimals
```

### 6. Test Save Operation
```
1. Edit 3 cells in different rows
2. Verify "Save (3)" button shows count
3. Click Save
4. Verify progress indicator
5. Verify 3 PUT API calls made
6. Verify success snackbar
7. Verify grid refreshes
8. Verify highlights removed
```

### 7. Test Cancel Operation
```
1. Edit 2 cells
2. Click Cancel
3. Verify confirmation dialog (if >0 changes)
4. Accept cancellation
5. Verify values reverted to original
6. Verify highlights removed
7. Verify Save/Cancel buttons hidden
```

### 8. Test Validation
```
1. Click monthly value cell
2. Type "abc"
3. Verify error indicator (red border)
4. Type "123.456" (too many decimals)
5. Verify error indicator
6. Type "12345678901.99" (too many digits)
7. Verify error indicator
8. Type "150.50" (valid)
9. Verify no error
```

### 9. Test Read-Only Mode
```
1. Login as user without production_plan.edit
2. Load data
3. Click on editable cells
4. Verify nothing happens
5. Verify no edit icons or indicators
6. Verify no Save/Cancel buttons
7. Verify all data displayed correctly
```

### 10. Test Keyboard Navigation
```
1. Click first editable cell
2. Press Tab repeatedly
3. Verify focus moves right through editable cells
4. Verify skips read-only cells
5. At end of row, wraps to next row
6. Press Shift+Tab
7. Verify moves backward
```

### 11. Test Unsaved Changes Warning
```
1. Edit a cell
2. Attempt to navigate to different page
3. Verify browser confirmation dialog
4. Cancel navigation
5. Verify still on production plan page
6. Click Save or Cancel
7. Navigate away
8. Verify no warning
```

### 12. Test Difference Color Coding
```
1. Load data with positive difference row
2. Verify difference shown in green
3. Load/edit data to create negative difference
4. Verify difference shown in red
5. Load/edit data to create zero difference
6. Verify difference shown in default color (black/gray)
```

### 13. Test Multiple Channels
```
1. Load data for product with 3 channels (Direct, Retail, Online)
2. Verify 3 rows shown for same product code
3. Verify each row has different channel value
4. Edit value in one channel
5. Verify other channels unchanged
6. Save
7. Verify only edited channel updated in database
```

### 14. Test Empty Year
```
1. Select year with no data (e.g., 2030)
2. Click Load
3. Verify empty state message shown
4. Verify no grid or empty grid with headers only
5. Verify no errors
```

### 15. Test Large Dataset
```
1. Load year with 500+ product/channel combinations
2. Verify grid renders without lag
3. Scroll vertically and horizontally
4. Verify smooth scrolling (60fps)
5. Edit cell
6. Verify editing remains responsive
```

### 16. Test Sparse Allocation
```
1. Edit row to have only Feb=100, Dec=200, all others empty
2. Verify Total = 100 + 200 + buffer
3. Verify empty cells treated as 0
4. Save
5. Reload data
6. Verify sparse data displayed correctly
```

### 17. Test Remarks Field
```
1. Click Remarks cell
2. Type long text (500+ characters)
3. Verify text accepts input
4. Save
5. Reload
6. Verify full text preserved
```
