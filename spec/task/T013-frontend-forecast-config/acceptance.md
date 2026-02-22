# T013: Acceptance Criteria

## Functional Acceptance Criteria

### Page Access and Display
- [ ] Route /sales-forecast/config is accessible after authentication
- [ ] User without sales_forecast_config.view permission is redirected or sees access denied
- [ ] Page displays all month configurations in a table
- [ ] Table shows columns: Month, Auto Close Day, Status, Closed At, Actions
- [ ] Months are sorted by month descending (newest first)
- [ ] Open months show green status badge with "Open" text
- [ ] Closed months show red/gray status badge with "Closed" text
- [ ] Closed_at timestamp formatted as "YYYY/MM/DD HH:MM:SS" for closed months
- [ ] Loading spinner displays during initial data fetch

### Batch Month Creation
- [ ] "Create Months" button visible to users with sales_forecast_config.edit permission
- [ ] Click "Create Months" opens dialog
- [ ] Dialog contains Start Month and End Month pickers (YYYYMM format)
- [ ] Create button disabled if start_month > end_month
- [ ] Create button disabled if either field is empty
- [ ] Successful creation shows success toast: "Created N months successfully"
- [ ] Table refreshes to show newly created months
- [ ] Dialog closes automatically on success
- [ ] Error response shows error toast with message
- [ ] Cancel button closes dialog without changes

### Edit Configuration
- [ ] Edit button visible in Actions column for users with sales_forecast_config.edit permission
- [ ] Click Edit opens dialog for specific month
- [ ] Dialog displays month name (read-only)
- [ ] Auto Close Day input accepts numbers 1-31
- [ ] Auto Close Day input shows validation error for values outside 1-31
- [ ] Is Closed toggle switch reflects current status
- [ ] Save button disabled if no changes made
- [ ] Save button disabled if validation errors exist
- [ ] Successful update shows success toast: "Configuration updated"
- [ ] Table refreshes to show updated data
- [ ] Changing is_closed from OFF to ON updates status immediately
- [ ] Changing is_closed from ON to OFF clears closed_at
- [ ] Error response shows error toast with message
- [ ] Cancel button closes dialog without changes

### Error Handling
- [ ] Network errors show error toast: "Failed to load configurations"
- [ ] 403 Forbidden hides create/edit buttons or shows access denied
- [ ] 409 Conflict on duplicate creation shows: "Some months already exist"
- [ ] 400 Bad Request shows validation error message
- [ ] All error messages are user-friendly and actionable

## UI Acceptance Criteria

### Layout and Design
- [ ] Page uses Material UI components consistently
- [ ] Table is responsive and scrollable on small screens
- [ ] Dialogs are centered and modal (dim background)
- [ ] Buttons follow MUI variant patterns (contained/outlined)
- [ ] Color scheme: Primary for actions, success/error for status
- [ ] Spacing follows Material Design guidelines (8px grid)

### Month Picker
- [ ] Month picker shows YYYYMM format (e.g., "202601")
- [ ] Picker allows keyboard input or dropdown selection
- [ ] Invalid formats show validation error
- [ ] Placeholder text: "YYYYMM (e.g., 202601)"

### Table Display
- [ ] Table headers are bold and have background color
- [ ] Rows have hover effect
- [ ] Status badges use MUI Chip component
- [ ] Actions column aligned right
- [ ] Empty state message if no configurations exist: "No configurations found. Create your first month."

### Responsive Behavior
- [ ] Table scrolls horizontally on mobile
- [ ] Dialogs are full-width on mobile, fixed-width on desktop
- [ ] Buttons stack vertically on very small screens

## Non-Functional Criteria
- [ ] Page loads within 2 seconds on normal network
- [ ] No console errors or warnings
- [ ] Component follows React best practices (hooks, functional components)
- [ ] Code is readable with proper comments
- [ ] Reusable components extracted where appropriate
- [ ] Accessibility: keyboard navigation works, ARIA labels present

## How to Verify

### Manual Testing
1. **Access Control:**
   - Login as user without sales_forecast_config.view
   - Navigate to /sales-forecast/config
   - Verify access denied or redirect

2. **View Configurations:**
   - Login as user with sales_forecast_config.view
   - Navigate to /sales-forecast/config
   - Verify table displays all months from backend
   - Verify sorting and status colors

3. **Create Months:**
   - Login as user with sales_forecast_config.edit
   - Click "Create Months"
   - Enter start_month=202601, end_month=202603
   - Click Create
   - Verify success toast and table refresh with 3 new months

4. **Edit Configuration:**
   - Click Edit on any month
   - Change auto_close_day to 20
   - Toggle is_closed
   - Click Save
   - Verify success toast and table updates

5. **Validation:**
   - In create dialog, set start > end, verify Create disabled
   - In edit dialog, enter auto_close_day=50, verify error message
   - Test with invalid month format

6. **Error Scenarios:**
   - Disconnect network, attempt to load page
   - Verify error toast
   - Create duplicate months
   - Verify appropriate error message

### Automated Testing (Vitest + React Testing Library)
- Render component tests for all dialogs
- Mock API responses for success/error scenarios
- Test button enable/disable logic
- Test form validation
- Snapshot tests for layout consistency
