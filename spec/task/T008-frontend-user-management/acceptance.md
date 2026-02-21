# T008: Acceptance Criteria

## Functional Acceptance Criteria

### User List Page
- [ ] Page accessible at /users route
- [ ] Table displays users with columns: Username, Full Name, Email, Role, Status, Actions
- [ ] Status shows "Active" or "Inactive" badge
- [ ] Actions column includes Edit, Delete, Toggle buttons/icons
- [ ] Loading state shown while fetching data
- [ ] Empty state shown when no users found
- [ ] Error message shown if API fails

### Search Functionality
- [ ] Search input field present at top of page
- [ ] Searching filters users by keyword
- [ ] Search queries username, full name, and email
- [ ] Search is debounced (doesn't fire on every keystroke)
- [ ] Clear search button clears filter and shows all users

### Filter Functionality
- [ ] Role filter dropdown present
- [ ] Role dropdown includes "All Roles" plus all role options
- [ ] Selecting role filters list to that role
- [ ] Status filter dropdown present (All, Active, Inactive)
- [ ] Selecting status filters list accordingly
- [ ] Filters can be combined (role + status + search)

### Sorting
- [ ] Table headers for sortable columns are clickable
- [ ] Clicking header sorts by that column (ascending first)
- [ ] Clicking again toggles to descending
- [ ] Sort indicator (arrow icon) shows current sort direction
- [ ] Sorting works with filters

### Pagination
- [ ] Pagination controls visible at bottom of table
- [ ] Shows current page and total pages
- [ ] Previous button disabled on first page
- [ ] Next button disabled on last page
- [ ] Clicking page number navigates to that page
- [ ] Page size is 20 records per page
- [ ] Pagination resets to page 1 when filters change

### Create User Page
- [ ] Page accessible at /users/create
- [ ] "Create User" button on list page navigates to create page
- [ ] Form includes all required fields: username, email, password, full name, role
- [ ] Form includes optional fields: department, phone
- [ ] Role dropdown populated with all roles
- [ ] When "Sales" role selected, channels multi-select appears
- [ ] Channels multi-select includes all 12 channels
- [ ] Submit button creates user via API
- [ ] Loading state shown while submitting
- [ ] Success notification shown on successful create
- [ ] Redirects to user list after success
- [ ] Error notification shown on failure
- [ ] Cancel button returns to list without saving

### Edit User Page
- [ ] Page accessible at /users/:id/edit
- [ ] Clicking Edit button on list navigates to edit page
- [ ] Form pre-populated with existing user data
- [ ] Password field is optional (placeholder: "Leave blank to keep current password")
- [ ] Role change shows/hides channels multi-select
- [ ] If user has sales role, channels pre-selected
- [ ] Submit button updates user via API
- [ ] Success notification shown on successful update
- [ ] Redirects to user list after success
- [ ] Error notification shown on failure

### Delete User
- [ ] Clicking Delete button shows confirmation dialog
- [ ] Dialog shows user's name/username
- [ ] Dialog has Cancel and Delete buttons
- [ ] Cancel closes dialog without action
- [ ] Delete button calls API to delete user
- [ ] Success notification shown on successful delete
- [ ] User removed from list immediately
- [ ] Error notification shown on failure

### Toggle Active Status
- [ ] Toggle button present in Actions column
- [ ] Button shows current status (Active/Inactive)
- [ ] Clicking toggle calls API to toggle status
- [ ] Loading indicator shown during API call
- [ ] Status updates immediately on success
- [ ] Success notification shown
- [ ] Error notification shown on failure

## UI Acceptance Criteria

### Layout
- [ ] List page has consistent header with title "User Management"
- [ ] Search and filters aligned horizontally at top
- [ ] Create User button prominently placed (top right)
- [ ] Table is responsive and scrollable if needed
- [ ] Form layouts are clean with proper spacing
- [ ] Labels clearly associated with inputs

### Form Validation
- [ ] Required fields marked with asterisk or label
- [ ] Validation errors shown below/next to fields
- [ ] Errors shown on blur or on submit
- [ ] Submit button disabled if form invalid (optional)
- [ ] Username validation: required, max 50 chars
- [ ] Email validation: required, valid format, max 100 chars
- [ ] Password validation: required on create, min 6 chars
- [ ] Full name validation: required
- [ ] Role validation: required
- [ ] Channels validation: required if sales role selected

### User Feedback
- [ ] Toast/snackbar notifications for success/error
- [ ] Loading spinners on buttons during API calls
- [ ] Loading skeleton or spinner on list page load
- [ ] Confirmation dialog modal overlay
- [ ] Disabled state on buttons during submission

### Accessibility
- [ ] Form inputs have labels or aria-labels
- [ ] Buttons have descriptive text or aria-labels
- [ ] Table headers use <th> tags
- [ ] Dialogs use proper modal semantics
- [ ] Keyboard navigation works

## Non-Functional Criteria

### Performance
- [ ] List page loads in under 2 seconds
- [ ] Search debounce prevents excessive API calls
- [ ] Pagination prevents loading all users at once
- [ ] Form submission responds in under 1 second (network permitting)

### Error Handling
- [ ] Network errors show user-friendly message
- [ ] API validation errors displayed on form
- [ ] Duplicate username/email errors shown clearly
- [ ] 403 errors redirect to login
- [ ] Unexpected errors don't crash the app

### Data Validation
- [ ] Client-side validation mirrors backend requirements
- [ ] No XSS vulnerabilities (input sanitization)
- [ ] Prevent double submission during API call

## How to Verify

### Manual Testing Steps

**List Page:**
1. Navigate to /users
2. Verify table shows users
3. Verify pagination works
4. Try search with "admin"
5. Verify only matching users shown
6. Filter by role
7. Sort by username
8. Clear all filters

**Create User:**
1. Click "Create User" button
2. Fill form with valid data
3. Submit
4. Verify success notification
5. Verify redirected to list
6. Verify new user appears in list

**Create Sales User:**
1. Click "Create User"
2. Select "Sales" role
3. Verify channels multi-select appears
4. Select 2-3 channels
5. Submit
6. Verify user created

**Edit User:**
1. Click Edit on a user
2. Verify form pre-populated
3. Change full name
4. Submit
5. Verify success notification
6. Verify list shows updated name

**Delete User:**
1. Click Delete on a user
2. Verify confirmation dialog
3. Click Cancel
4. Verify dialog closes, user not deleted
5. Click Delete again
6. Click Delete in dialog
7. Verify user removed from list

**Toggle Status:**
1. Find active user
2. Click toggle button
3. Verify status changes to Inactive
4. Click toggle again
5. Verify status changes to Active

**Validation Errors:**
1. Create user with empty username
2. Submit
3. Verify error message
4. Enter invalid email format
5. Verify error message
6. Select Sales role without channels
7. Verify error message

### Automated Testing
```bash
cd frontend
npm test -- UserListPage
npm test -- UserCreatePage
npm test -- UserEditPage
```

### API Integration Testing
- Verify API calls include Authorization header
- Verify correct endpoints called
- Verify request payloads match API contracts
- Check Network tab in browser dev tools

### Visual Testing
- Test in Chrome, Firefox, Safari
- Verify responsive behavior (optional)
- Check UI consistency with design
