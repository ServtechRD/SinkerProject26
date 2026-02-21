# T008: Frontend User Management Pages

## Context
This task implements the complete user management interface in React, integrating with the backend user CRUD API (T005). It provides administrators with the ability to list, search, filter, create, edit, delete, and toggle user status through an intuitive UI.

## Goal
Create comprehensive user management pages including a list view with search/filter/pagination, create form with validation, edit form, delete confirmation dialog, and toggle active/inactive functionality. Implement sales role-specific channel assignment with multi-select.

## Scope

### In Scope
- User list page with data table
- Search by keyword (username, name, email)
- Filter by role and active status
- Sort by columns (username, created_at, etc.)
- Pagination controls (20 records per page)
- Create user page with form
- Edit user page with pre-populated form
- Delete confirmation dialog
- Toggle active/inactive button
- Sales role detection with channel multi-select
- Form validation (client-side)
- Error handling and user feedback
- Loading states
- Success/error notifications

### Out of Scope
- Bulk user operations (bulk delete, bulk activate)
- User import from CSV
- User profile picture upload
- Password strength indicator (can be added later)
- User activity history view
- Advanced filtering (date ranges, multiple roles)
- Export user list to Excel/CSV

## Requirements
- List page displays users in table with columns: username, full name, email, role, status, actions
- Search input filters users as-you-type or on submit
- Role filter dropdown with all roles
- Status filter dropdown (All, Active, Inactive)
- Sort headers clickable
- Pagination with page numbers and prev/next buttons
- Create form fields: username, email, password, full name, role, department, phone, channels (if sales)
- Edit form same as create but password optional
- Validation: required fields, email format, password min length
- Channel multi-select appears when sales role selected
- Channel list: 12 predefined channels
- Delete shows confirmation dialog with user name
- Toggle button switches between active/inactive with immediate feedback
- All operations show loading spinner
- Success shows toast/snackbar notification
- Errors show toast/snackbar with error message
- Navigate back to list after create/edit/delete

## Implementation Notes
- Use React Router for navigation
- Use Axios for API calls (configured in T003)
- Use controlled components for forms
- Use useState for form state, loading, errors
- Use useEffect to fetch data on mount and when filters change
- Debounce search input (300ms)
- Show loading skeleton or spinner while fetching
- Use confirmation dialog component (create if doesn't exist)
- Show toast notifications (use library like react-toastify or build custom)
- Channel multi-select can use native <select multiple> or library like react-select
- Validate required fields on submit
- Disable submit button while submitting
- Clear form after successful create
- Redirect to list after successful edit/delete

## Files to Change
- src/pages/users/UserListPage.jsx (new)
- src/pages/users/UserCreatePage.jsx (new)
- src/pages/users/UserEditPage.jsx (new)
- src/components/ConfirmDialog.jsx (new)
- src/components/Toast.jsx or use react-toastify (new)
- src/api/users.js (new)
- src/router.jsx (update to add routes)
- src/components/Sidebar.jsx (update to highlight Users link)

## Dependencies
- T005: Requires backend user CRUD API
- T003: Requires base layout and routing
