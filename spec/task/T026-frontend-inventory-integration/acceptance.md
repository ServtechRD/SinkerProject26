# T026: Acceptance Criteria

## Functional Acceptance Criteria

### Page Access
- [ ] Route /inventory-integration accessible when logged in with inventory.view permission
- [ ] Redirects to login if not authenticated
- [ ] Shows "Access Denied" if missing inventory.view permission
- [ ] Navigation menu includes link to inventory integration page

### Query Controls
- [ ] Month picker defaults to current month
- [ ] Start date picker defaults to first day of selected month
- [ ] End date picker defaults to last day of selected month
- [ ] Changing month updates date pickers accordingly
- [ ] Version dropdown initially empty, populated after first query
- [ ] "Query" button disabled if month not selected
- [ ] "Load Version" button disabled if no version selected

### Real-Time Query
- [ ] Clicking "Query" with month only calls API with month parameter
- [ ] Clicking "Query" with dates calls API with month, start_date, end_date
- [ ] Loading spinner shows during API call
- [ ] Success: data table populated with results
- [ ] Success: version stored and added to dropdown
- [ ] Error: error message displayed to user
- [ ] Error: table shows empty state

### Version Query
- [ ] Selecting version from dropdown enables "Load Version" button
- [ ] Clicking "Load Version" calls API with month and version parameters
- [ ] Data table shows historical snapshot
- [ ] Current version indicator shows selected version
- [ ] Version query does not trigger new version creation

### Data Table Display
- [ ] All columns displayed: Product Code, Product Name, Category, Spec, Warehouse, Sales Qty, Inventory Balance, Forecast Qty, Production Subtotal, Modified Subtotal, Version
- [ ] Numeric values formatted with 2 decimal places and thousands separators
- [ ] Rows where modified_subtotal differs from production_subtotal highlighted (e.g., yellow background)
- [ ] Sorted by product code ascending by default
- [ ] Column headers clickable for sorting
- [ ] Empty state shows "No data" message when no results

### Edit Functionality (with inventory.edit permission)
- [ ] Modified Subtotal cells show edit icon on hover
- [ ] Clicking cell enters edit mode with input field
- [ ] Input validates decimal format in real-time
- [ ] Invalid input shows error message below cell
- [ ] Pressing Enter saves cell value
- [ ] Pressing Escape cancels edit
- [ ] "Save" button appears at top when any cell modified
- [ ] "Cancel" button appears alongside Save button
- [ ] Clicking Save calls PUT API for each modified row
- [ ] Success: page refreshes with new version data
- [ ] Error: shows error message, keeps changes for retry
- [ ] Browser warns before navigation if unsaved changes exist

### Edit Functionality (without inventory.edit permission)
- [ ] Modified Subtotal column is read-only
- [ ] No edit icons shown
- [ ] No Save/Cancel buttons visible
- [ ] Clicking cells does nothing

### Permissions
- [ ] Edit controls hidden if user lacks inventory.edit
- [ ] API errors for permission denied show user-friendly message

### Responsive Design
- [ ] Mobile (<600px): Query controls stack vertically, table scrolls horizontally
- [ ] Tablet (600-960px): Query controls in 2 columns, table fits with scroll
- [ ] Desktop (>960px): All controls in single row, table fills viewport

## UI Acceptance Criteria

### Layout
```
+----------------------------------------------------------+
|  Inventory Integration                                    |
|                                                           |
|  [Month Picker] [Start Date] [End Date] [Query]          |
|  [Version Dropdown ▼] [Load Version]                     |
|                                                           |
|  Current Version: v20260115120000          [Save] [Cancel]|
+----------------------------------------------------------+
|  Product | Name    | Cat | Spec | Wh | Sales | Inv | ... |
+----------------------------------------------------------+
|  PROD001 | Prod 1  | A   | S1   | WH | 100.00| 250 | ... |
|  PROD002 | Prod 2  | B   | S2   | WH | 50.00 | 100 | ... | ← highlighted
+----------------------------------------------------------+
```

### Color Scheme
- Modified rows: Yellow/Amber background (#FFF9C4)
- Edit mode: Light blue border (#2196F3)
- Error state: Red text (#F44336)
- Loading: Skeleton animation or spinner

### Interactions
- Hover on editable cell: Show pencil icon
- Click cell: Replace with TextField
- Invalid input: Red border + error text below
- Save success: Green snackbar "Data saved successfully"
- Save error: Red snackbar with error message

## Non-Functional Criteria
- [ ] Page load time < 2 seconds
- [ ] Table renders 1000 rows without lag
- [ ] Sorting 1000 rows completes in < 500ms
- [ ] API calls include loading indicators
- [ ] No console errors or warnings
- [ ] Proper error boundaries for crash prevention
- [ ] Accessible: keyboard navigation works
- [ ] Accessible: screen reader friendly labels

## How to Verify

### 1. Test Page Access
```
1. Logout
2. Navigate to /inventory-integration
3. Verify redirect to login
4. Login as user with inventory.view
5. Navigate to /inventory-integration
6. Verify page loads
```

### 2. Test Real-Time Query
```
1. Select month: 2026-01
2. Click "Query"
3. Verify API call: GET /api/inventory-integration?month=2026-01
4. Verify table populated with data
5. Verify version dropdown updated
6. Check localStorage for version saved
```

### 3. Test Date Range Query
```
1. Select month: 2026-01
2. Set start date: 2026-01-10
3. Set end date: 2026-01-20
4. Click "Query"
5. Verify API call includes start_date and end_date parameters
6. Verify data displayed
```

### 4. Test Version Query
```
1. Complete real-time query (step 2)
2. Open version dropdown
3. Verify previous version appears
4. Select version
5. Click "Load Version"
6. Verify API call: GET /api/inventory-integration?month=2026-01&version=v...
7. Verify data matches snapshot
```

### 5. Test Edit with Permission
```
1. Login as user with inventory.edit
2. Load data
3. Hover over modified_subtotal cell
4. Verify edit icon appears
5. Click cell
6. Enter new value: 200.50
7. Press Enter
8. Verify "Save" button appears
9. Click "Save"
10. Verify PUT API call for modified row
11. Verify page refreshes with new version
```

### 6. Test Edit without Permission
```
1. Login as user without inventory.edit (view-only)
2. Load data
3. Hover over modified_subtotal cell
4. Verify no edit icon
5. Click cell
6. Verify nothing happens
7. Verify no Save button visible
```

### 7. Test Validation
```
1. Enter edit mode on modified_subtotal
2. Type "abc"
3. Verify error message shows
4. Type "123.456" (too many decimals)
5. Verify error message shows
6. Type "12345678901.99" (too many digits)
7. Verify error message shows
8. Type "200.50" (valid)
9. Verify no error
```

### 8. Test Unsaved Changes Warning
```
1. Edit a cell
2. Attempt to navigate away
3. Verify browser confirmation dialog appears
4. Cancel navigation
5. Click "Save" or "Cancel"
6. Navigate away
7. Verify no warning appears
```

### 9. Test Error Handling
```
1. Disconnect network
2. Click "Query"
3. Verify error message displayed
4. Verify table shows empty state
5. Reconnect network
6. Retry query
7. Verify success
```

### 10. Test Responsive Layout
```
1. Open page on desktop
2. Resize to tablet width (768px)
3. Verify query controls adjust
4. Resize to mobile width (375px)
5. Verify controls stack vertically
6. Verify table scrolls horizontally
```

### 11. Test Row Highlighting
```
1. Load data with modified_subtotal set
2. Verify rows with modified_subtotal highlighted
3. Edit a row to match production_subtotal exactly
4. Verify highlight persists until save
5. Save changes
6. Verify highlighting updates based on new data
```

### 12. Test Version History Limit
```
1. Perform 12 real-time queries (different months or repeated)
2. Open version dropdown
3. Verify only 10 most recent versions shown
4. Verify localStorage has max 10 entries
```
