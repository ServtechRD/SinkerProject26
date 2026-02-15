# T009: Test Plan

## Unit Tests (Vitest + React Testing Library)

### RoleListPage.test.jsx
- **Test: renders role list**
  - Mock API to return roles
  - Render RoleListPage
  - Assert table rendered with roles

- **Test: shows loading state**
  - Mock API with delayed response
  - Render RoleListPage
  - Assert loading indicator visible

- **Test: shows error state**
  - Mock API to reject
  - Render RoleListPage
  - Assert error message shown

- **Test: edit button navigates**
  - Mock API and useNavigate
  - Render RoleListPage
  - Click edit button
  - Assert navigate called with /roles/:id/edit

### RoleEditPage.test.jsx
- **Test: loads and displays role data**
  - Mock API to return role with permissions
  - Render RoleEditPage
  - Assert role name, code, description displayed
  - Assert permissions grouped by module

- **Test: code field is read-only**
  - Render RoleEditPage
  - Assert code input is disabled or readonly

- **Test: name field is editable**
  - Render RoleEditPage
  - Type in name input
  - Assert value updates

- **Test: permissions pre-selected**
  - Mock role with specific permissions
  - Render RoleEditPage
  - Assert those permission checkboxes are checked

- **Test: clicking permission checkbox toggles**
  - Render RoleEditPage
  - Click unchecked permission
  - Assert checkbox becomes checked
  - Click again
  - Assert becomes unchecked

- **Test: module select-all selects all in module**
  - Render RoleEditPage
  - Click "Select All" for user module
  - Assert all 4 user permissions checked

- **Test: module select-all deselects all**
  - Render with user permissions selected
  - Click "Select All" for user module
  - Assert all user permissions unchecked

- **Test: indeterminate state on module select-all**
  - Render RoleEditPage
  - Check some (not all) user permissions
  - Assert user module "Select All" is indeterminate

- **Test: save button submits**
  - Mock PUT API
  - Render RoleEditPage
  - Change permissions
  - Click Save
  - Assert API called with updated permissionIds

- **Test: save shows loading state**
  - Mock API with delay
  - Render RoleEditPage
  - Click Save
  - Assert loading indicator shown
  - Assert form disabled during save

- **Test: save shows success notification**
  - Mock API success
  - Render RoleEditPage
  - Click Save
  - Assert success toast shown

- **Test: save shows error notification**
  - Mock API to reject
  - Render RoleEditPage
  - Click Save
  - Assert error toast shown

- **Test: cancel navigates back**
  - Mock useNavigate
  - Render RoleEditPage
  - Click Cancel
  - Assert navigate called to /roles

### PermissionModuleGroup.test.jsx (if component extracted)
- **Test: renders module name**
  - Render with module name and permissions
  - Assert module name displayed

- **Test: renders all permissions**
  - Render with 4 permissions
  - Assert 4 checkboxes rendered

- **Test: select-all checked when all selected**
  - Render with all permissions selected
  - Assert select-all checkbox is checked

- **Test: select-all unchecked when none selected**
  - Render with no permissions selected
  - Assert select-all checkbox is unchecked

- **Test: select-all indeterminate when some selected**
  - Render with some permissions selected
  - Assert select-all checkbox has indeterminate property

## Integration Tests (Vitest + React Testing Library + MSW)

### Role Edit Flow Test
- **Test: full edit flow**
  - Mock GET /api/roles/:id and PUT /api/roles/:id
  - Render RoleEditPage
  - Assert role loaded
  - Change name
  - Toggle permission
  - Click Save
  - Assert API called with updates
  - Assert success notification

- **Test: permission grouping**
  - Mock API to return admin role (all 29 permissions)
  - Render RoleEditPage
  - Assert 10 modules rendered
  - Assert user module has 4 permissions
  - Assert sales_forecast module has 6 permissions

### API Integration Tests (with MSW)
- **Test: list roles API call**
  - Set up MSW handler for GET /api/roles
  - Render RoleListPage
  - Assert API called
  - Assert roles displayed

- **Test: get role detail API call**
  - Set up MSW handler for GET /api/roles/:id
  - Render RoleEditPage with roleId
  - Assert API called with correct ID
  - Assert role data displayed

- **Test: update role API call**
  - Mock PUT /api/roles/:id
  - Render RoleEditPage
  - Make changes
  - Save
  - Assert request body includes name, description, permissionIds
  - Assert Authorization header included

- **Test: validation error from API**
  - Mock PUT to return 400 with validation error
  - Render RoleEditPage
  - Save
  - Assert error message displayed

## E2E Tests
Covered in separate E2E test suite (Playwright). Focus on critical user journeys:
- List roles → Edit role → Change permissions → Save
- Select-all functionality
- Cancel without saving

## Test Data Setup

### Mock API Responses (MSW)
```javascript
// List roles
rest.get('/api/roles', (req, res, ctx) => {
  return res(ctx.json({
    roles: [
      { id: 1, code: 'admin', name: 'Administrator', description: 'Full access', isSystem: true, isActive: true },
      { id: 2, code: 'sales', name: 'Sales', description: 'Sales team', isSystem: true, isActive: true }
    ]
  }))
})

// Get role detail
rest.get('/api/roles/:id', (req, res, ctx) => {
  return res(ctx.json({
    id: 1,
    code: 'admin',
    name: 'Administrator',
    description: 'Full access',
    isSystem: true,
    permissions: [
      { id: 1, code: 'user.view', name: 'View Users', module: 'user' },
      { id: 2, code: 'user.create', name: 'Create User', module: 'user' },
      // ... all 29 permissions
    ],
    permissionsByModule: {
      user: [
        { id: 1, code: 'user.view', name: 'View Users' },
        { id: 2, code: 'user.create', name: 'Create User' },
        // ...
      ],
      role: [/* ... */],
      // ... other modules
    }
  }))
})

// Update role
rest.put('/api/roles/:id', (req, res, ctx) => {
  return res(ctx.json({ id: 1, ...req.body }))
})
```

### Test Data Constants
```javascript
const MOCK_PERMISSIONS_BY_MODULE = {
  user: [
    { id: 1, code: 'user.view', name: 'View Users' },
    { id: 2, code: 'user.create', name: 'Create User' },
    { id: 3, code: 'user.edit', name: 'Edit User' },
    { id: 4, code: 'user.delete', name: 'Delete User' }
  ],
  role: [
    { id: 5, code: 'role.view', name: 'View Roles' },
    { id: 6, code: 'role.create', name: 'Create Role' },
    { id: 7, code: 'role.edit', name: 'Edit Role' },
    { id: 8, code: 'role.delete', name: 'Delete Role' }
  ],
  // ... other modules
}
```

## Mocking Strategy

### Unit Tests
- Mock API calls using vi.mock or MSW
- Mock useNavigate and useParams from react-router-dom
- Mock toast notification library
- Render components in isolation

### Integration Tests
- Use MSW for all API mocking
- Use MemoryRouter for routing
- Render full page components
- Test actual user interactions

### Common Test Utilities
```javascript
// renderWithRouter
const renderRoleEditPage = (roleId = '1') => {
  return render(
    <MemoryRouter initialEntries={[`/roles/${roleId}/edit`]}>
      <AuthProvider>
        <Routes>
          <Route path="/roles/:id/edit" element={<RoleEditPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  )
}

// helper to get checkboxes
const getPermissionCheckbox = (code) => {
  return screen.getByRole('checkbox', { name: new RegExp(code, 'i') })
}
```

## Test Coverage Goals
- Line coverage: 80%+
- Branch coverage: 75%+
- Critical paths: 100% (save, cancel, select-all logic)

## Additional Test Scenarios

### Edge Cases
- Role with no permissions (empty state)
- Role with all permissions (admin)
- Role with permissions from single module
- Rapid checkbox clicking
- Network timeout during save
- Invalid role ID (404)

### Select-All Logic Edge Cases
- **Test: select-all when 0/4 permissions selected**
  - Assert unchecked, not indeterminate

- **Test: select-all when 4/4 permissions selected**
  - Assert checked, not indeterminate

- **Test: select-all when 2/4 permissions selected**
  - Assert indeterminate

- **Test: clicking select-all from indeterminate state**
  - Should select all (not toggle to unchecked)

### Permission Calculation Tests
- **Test: grouping permissions by module**
  - Given flat array of permissions
  - When grouped by module
  - Assert correct structure

- **Test: calculating selected permission IDs**
  - Given selected checkboxes
  - When building payload
  - Assert only selected IDs included

### Accessibility Tests
- Use jest-axe for accessibility violations
- Test keyboard navigation through checkboxes
- Test screen reader labels
- Test focus management

### State Management Tests
- **Test: state persists during edits**
  - Change permissions
  - Scroll page
  - Assert selections maintained

- **Test: state resets on cancel**
  - Change permissions
  - Cancel
  - Return to edit
  - Assert original permissions shown

### Visual Regression Tests (Optional)
- Screenshot of permissions grouped by module
- Indeterminate checkbox state
- Loading state
