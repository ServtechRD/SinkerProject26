# T008: Test Plan

## Unit Tests (Vitest + React Testing Library)

### UserListPage.test.jsx
- **Test: renders user list**
  - Mock API to return users
  - Render UserListPage
  - Assert table rendered
  - Assert user rows displayed

- **Test: shows loading state**
  - Mock API with delayed response
  - Render UserListPage
  - Assert loading indicator visible
  - Wait for data
  - Assert loading indicator hidden

- **Test: shows empty state**
  - Mock API to return empty array
  - Render UserListPage
  - Assert empty state message shown

- **Test: shows error state**
  - Mock API to reject
  - Render UserListPage
  - Assert error message shown

- **Test: search filters users**
  - Mock API
  - Render UserListPage
  - Type in search input
  - Assert API called with keyword parameter

- **Test: role filter works**
  - Mock API
  - Render UserListPage
  - Select role from dropdown
  - Assert API called with roleId parameter

- **Test: status filter works**
  - Mock API
  - Render UserListPage
  - Select "Active" from status filter
  - Assert API called with isActive=true

- **Test: sorting works**
  - Mock API
  - Render UserListPage
  - Click username header
  - Assert API called with sortBy=username, sortOrder=asc
  - Click again
  - Assert API called with sortOrder=desc

- **Test: pagination works**
  - Mock API with multiple pages
  - Render UserListPage
  - Click next page
  - Assert API called with page=1

- **Test: delete button shows confirmation**
  - Mock API
  - Render UserListPage
  - Click delete button
  - Assert confirmation dialog shown

- **Test: toggle button calls API**
  - Mock API
  - Render UserListPage
  - Click toggle button
  - Assert API called with PATCH /users/:id/toggle

### UserCreatePage.test.jsx
- **Test: renders create form**
  - Render UserCreatePage
  - Assert all form fields present
  - Assert submit button present

- **Test: submits valid form**
  - Mock API
  - Render UserCreatePage
  - Fill all required fields
  - Click submit
  - Assert API called with correct data
  - Assert navigate called to /users

- **Test: shows validation errors**
  - Render UserCreatePage
  - Click submit without filling fields
  - Assert error messages shown

- **Test: shows channels for sales role**
  - Render UserCreatePage
  - Select "Sales" from role dropdown
  - Assert channels multi-select visible

- **Test: hides channels for non-sales role**
  - Render UserCreatePage
  - Select "Sales" role (channels appear)
  - Select "Admin" role
  - Assert channels multi-select hidden

- **Test: validates channels required for sales**
  - Render UserCreatePage
  - Select "Sales" role
  - Submit without selecting channels
  - Assert validation error

- **Test: shows error on API failure**
  - Mock API to reject
  - Render UserCreatePage
  - Fill and submit form
  - Assert error notification shown

- **Test: cancel navigates back**
  - Mock useNavigate
  - Render UserCreatePage
  - Click cancel button
  - Assert navigate called to /users

### UserEditPage.test.jsx
- **Test: loads and displays user data**
  - Mock API to return user
  - Render UserEditPage with userId
  - Assert form pre-populated with user data

- **Test: password field is optional**
  - Mock API
  - Render UserEditPage
  - Leave password blank
  - Submit
  - Assert API called without password field

- **Test: updates user successfully**
  - Mock API
  - Render UserEditPage
  - Change full name
  - Submit
  - Assert API called with updated data
  - Assert navigate called

- **Test: shows validation errors**
  - Render UserEditPage
  - Clear required field
  - Submit
  - Assert error shown

- **Test: shows channels if user has sales role**
  - Mock API to return sales user with channels
  - Render UserEditPage
  - Assert channels multi-select visible
  - Assert user's channels pre-selected

### ConfirmDialog.test.jsx
- **Test: renders with message**
  - Render ConfirmDialog with message
  - Assert message displayed

- **Test: cancel button closes dialog**
  - Mock onCancel
  - Render ConfirmDialog
  - Click cancel
  - Assert onCancel called

- **Test: confirm button calls callback**
  - Mock onConfirm
  - Render ConfirmDialog
  - Click confirm button
  - Assert onConfirm called

## Integration Tests (Vitest + React Testing Library + MSW)

### User Management Flow Test
- **Test: full create flow**
  - Mock POST /api/users
  - Render App with Router
  - Navigate to /users/create
  - Fill form
  - Submit
  - Assert API called
  - Assert redirected to /users
  - Assert success notification shown

- **Test: full edit flow**
  - Mock GET /api/users/:id and PUT /api/users/:id
  - Render App
  - Navigate to /users/:id/edit
  - Assert form loaded
  - Update field
  - Submit
  - Assert API called
  - Assert redirected

- **Test: full delete flow**
  - Mock DELETE /api/users/:id
  - Render UserListPage
  - Click delete
  - Confirm in dialog
  - Assert API called
  - Assert user removed from list

### API Integration Tests (with MSW)
- **Test: list users API call**
  - Set up MSW handler for GET /api/users
  - Render UserListPage
  - Assert API called with correct headers
  - Assert users displayed

- **Test: create user API call**
  - Set up MSW handler for POST /api/users
  - Render UserCreatePage
  - Submit form
  - Assert request body matches CreateUserRequest format
  - Assert Authorization header included

- **Test: duplicate username error**
  - Mock POST /api/users to return 409
  - Render UserCreatePage
  - Submit form
  - Assert error message shown: "Username already exists"

- **Test: validation error from API**
  - Mock POST /api/users to return 400 with validation errors
  - Render UserCreatePage
  - Submit form
  - Assert validation errors displayed on form

## E2E Tests
Covered in separate E2E test suite (Playwright). Focus on critical user journeys:
- Create user → verify in list
- Edit user → verify changes
- Delete user → verify removed
- Search and filter combinations

## Test Data Setup

### Mock API Responses (MSW)
```javascript
// List users
rest.get('/api/users', (req, res, ctx) => {
  return res(ctx.json({
    users: [
      { id: 1, username: 'admin', fullName: 'Admin', email: 'admin@test.com', role: { code: 'admin', name: 'Admin' }, isActive: true },
      { id: 2, username: 'sales1', fullName: 'Sales User', email: 'sales@test.com', role: { code: 'sales', name: 'Sales' }, isActive: true }
    ],
    totalElements: 2,
    totalPages: 1,
    currentPage: 0
  }))
})

// Get user
rest.get('/api/users/:id', (req, res, ctx) => {
  return res(ctx.json({
    id: 1,
    username: 'admin',
    email: 'admin@test.com',
    fullName: 'Admin User',
    role: { id: 1, code: 'admin', name: 'Admin' },
    department: 'IT',
    phone: '123-456-7890',
    isActive: true
  }))
})

// Create user success
rest.post('/api/users', (req, res, ctx) => {
  return res(ctx.status(201), ctx.json({ id: 3, ...req.body }))
})

// Create user duplicate username
rest.post('/api/users', (req, res, ctx) => {
  return res(ctx.status(409), ctx.json({ message: 'Username already exists' }))
})
```

### Test Data Constants
```javascript
const MOCK_CHANNELS = [
  'PX/大全聯', '家樂福', '愛買', '711', '全家',
  'OK/萊爾富', '好市多', '楓康', '美聯社', '康是美',
  '電商', '市面經銷'
]

const MOCK_ROLES = [
  { id: 1, code: 'admin', name: 'Administrator' },
  { id: 2, code: 'sales', name: 'Sales' },
  { id: 3, code: 'production_planner', name: 'Production Planner' },
  { id: 4, code: 'procurement', name: 'Procurement' }
]
```

## Mocking Strategy

### Unit Tests
- Mock API calls using vi.mock or MSW
- Mock useNavigate from react-router-dom
- Mock toast notification library
- Render components in isolation

### Integration Tests
- Use MSW for all API mocking
- Use MemoryRouter for routing
- Render full page components (not isolated)
- Test actual user interactions

### Common Test Utilities
```javascript
// renderWithRouter
const renderWithRouter = (component, initialRoute = '/') => {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <AuthProvider>
        {component}
      </AuthProvider>
    </MemoryRouter>
  )
}

// fillForm helper
const fillUserForm = async (data) => {
  await userEvent.type(screen.getByLabelText(/username/i), data.username)
  await userEvent.type(screen.getByLabelText(/email/i), data.email)
  // ...
}
```

## Test Coverage Goals
- Line coverage: 80%+
- Branch coverage: 75%+
- Critical paths: 100% (create, edit, delete)

## Additional Test Scenarios

### Edge Cases
- Pagination on last page (next button disabled)
- Search with no results
- Filter combination with no results
- Rapid filter changes (debouncing)
- Submit form twice (double-click prevention)
- Network timeout
- Very long username/email (truncation/wrapping)

### Accessibility Tests
- Keyboard navigation through form
- Screen reader labels on inputs
- Focus management (form errors)
- Dialog focus trap

### Responsive Tests (Optional)
- Table responsive on small screens
- Form layout on mobile
- Filter controls on mobile
