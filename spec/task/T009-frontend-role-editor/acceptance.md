# T009: Acceptance Criteria

## Functional Acceptance Criteria

### Role List Page
- [ ] Page accessible at /roles route (if added to sidebar) or other route
- [ ] Table displays all roles
- [ ] Columns: Code, Name, Description, System Role (Yes/No), Actions
- [ ] Actions column includes Edit button
- [ ] Clicking Edit navigates to role edit page
- [ ] Loading state shown while fetching roles
- [ ] Error message shown if API fails

### Role Edit Page
- [ ] Page accessible at /roles/:id/edit
- [ ] Role info section at top shows: Code (read-only), Name (editable), Description (editable)
- [ ] Name input is editable
- [ ] Description textarea is editable
- [ ] Code is displayed but not editable
- [ ] Permissions section below role info
- [ ] Permissions grouped by module
- [ ] All 10 modules displayed (user, role, sales_forecast, sales_forecast_config, production_plan, inventory, weekly_schedule, semi_product, material_demand, material_purchase)
- [ ] Each module has section header with module name
- [ ] Each module has "Select All" checkbox
- [ ] Each permission within module has checkbox with name and code

### Permission Selection
- [ ] Clicking individual permission checkbox toggles selection
- [ ] Clicking "Select All" for a module selects all permissions in that module
- [ ] Clicking "Select All" again deselects all permissions in that module
- [ ] "Select All" checkbox shows indeterminate state if some (not all) permissions selected
- [ ] "Select All" checkbox is checked if all permissions in module selected
- [ ] "Select All" checkbox is unchecked if no permissions in module selected
- [ ] Selection state persists while on page (doesn't reset)

### Save and Cancel
- [ ] Save button at bottom of form
- [ ] Cancel button at bottom of form
- [ ] Save button submits updated role to API (name, description, permissionIds)
- [ ] Loading state shown during save (disabled form, spinner on button)
- [ ] Success notification shown on successful save
- [ ] Error notification shown on failure
- [ ] Cancel button returns to role list without saving
- [ ] Unsaved changes warning (optional but recommended)

### Data Loading
- [ ] Role data and permissions loaded on page mount
- [ ] Loading skeleton or spinner shown while loading
- [ ] Error state if role not found (404)
- [ ] Error state if API fails
- [ ] Role's current permissions pre-selected on load

## UI Acceptance Criteria

### Layout
- [ ] Page has clear header "Edit Role: {role name}"
- [ ] Role info section visually distinct from permissions section
- [ ] Permissions organized in clear groups
- [ ] Module headers bold or otherwise emphasized
- [ ] Checkboxes aligned properly
- [ ] Save/Cancel buttons prominently placed

### Module Grouping
- [ ] Modules displayed in logical order
- [ ] Each module visually separated (border, spacing, or background color)
- [ ] Module names human-readable (e.g., "User Management" not just "user")
- [ ] Permission names human-readable
- [ ] Permission codes shown in smaller/gray text

### Form Behavior
- [ ] Name input max length enforced (100 chars)
- [ ] Description textarea allows multiple lines
- [ ] Checkboxes keyboard accessible
- [ ] "Select All" checkbox visually distinct (bold label or icon)

### User Feedback
- [ ] Toast/snackbar notifications for success/error
- [ ] Loading spinner on save button during submission
- [ ] Loading state disables all inputs
- [ ] Confirmation dialog on cancel if changes made (optional)

### Accessibility
- [ ] Form inputs have labels
- [ ] Checkboxes have proper labels
- [ ] Keyboard navigation works
- [ ] Screen reader support for indeterminate state

## Non-Functional Criteria

### Performance
- [ ] Page loads in under 2 seconds
- [ ] Checkbox interactions instantaneous (no lag)
- [ ] Save completes in under 1 second (network permitting)

### Error Handling
- [ ] Network errors show user-friendly message
- [ ] API validation errors displayed
- [ ] 403 errors redirect to login
- [ ] Unexpected errors don't crash the app

### Data Validation
- [ ] Name required (cannot be empty)
- [ ] At least show warning if role has no permissions (optional)
- [ ] Client-side validation matches backend

## How to Verify

### Manual Testing Steps

**List Page:**
1. Navigate to /roles (or configured route)
2. Verify all 4 roles displayed (admin, sales, production_planner, procurement)
3. Verify System Role column shows "Yes" for all seeded roles
4. Click Edit on admin role

**Edit Page Load:**
1. Verify role name, code, description displayed
2. Verify code is read-only
3. Verify all 10 modules displayed
4. Verify admin role has all permissions pre-selected (29 total)

**Individual Checkbox:**
1. Uncheck a single permission (e.g., user.view)
2. Verify checkbox unchecked
3. Verify "Select All" for user module becomes indeterminate
4. Check it again
5. Verify "Select All" becomes checked (if all others selected)

**Module Select All:**
1. Click "Select All" for user module
2. Verify all 4 user permissions selected
3. Click "Select All" again
4. Verify all 4 user permissions deselected

**Save Changes:**
1. Uncheck some permissions
2. Click Save
3. Verify loading state
4. Verify success notification
5. Navigate back to list
6. Edit same role again
7. Verify changes persisted

**Cancel:**
1. Make changes to permissions
2. Click Cancel
3. Verify returned to list
4. Edit role again
5. Verify changes were not saved

**Edit Non-Sales Role:**
1. Edit "Production Planner" role
2. Select only production_plan permissions
3. Save
4. Verify success

**Validation:**
1. Clear name field
2. Click Save
3. Verify validation error

### Automated Testing
```bash
cd frontend
npm test -- RoleListPage
npm test -- RoleEditPage
```

### API Integration Testing
- Verify GET /api/roles called on list page
- Verify GET /api/roles/:id called on edit page
- Verify PUT /api/roles/:id called on save
- Verify request includes permissionIds array
- Check Network tab in browser dev tools

### Visual Testing
- Verify layout in Chrome, Firefox, Safari
- Test with many permissions (admin role - 29 permissions)
- Test with few permissions (custom role)
- Verify module grouping clear and readable
