# T003: Test Plan

## Unit Tests (Vitest + React Testing Library)

### LoginPage.test.jsx
- **Test: renders login form**
  - Render LoginPage
  - Assert username input exists
  - Assert password input exists
  - Assert submit button exists

- **Test: validates empty fields**
  - Render LoginPage
  - Click submit without entering data
  - Assert validation error messages shown or submit prevented

- **Test: handles successful login**
  - Mock AuthContext login function
  - Mock useNavigate
  - Render LoginPage
  - Enter username and password
  - Click submit
  - Assert login function called with credentials
  - Assert navigate called to '/'

- **Test: displays error on failed login**
  - Mock login function to reject with 401 error
  - Render LoginPage
  - Submit form
  - Assert error message displayed: "Invalid username or password"

- **Test: displays error on locked account**
  - Mock login function to reject with 403 error (locked)
  - Render LoginPage
  - Submit form
  - Assert error message displayed: "Account is locked"

- **Test: shows loading state during login**
  - Mock login function with delayed promise
  - Render LoginPage
  - Submit form
  - Assert loading indicator visible or button disabled

### AuthContext.test.jsx
- **Test: provides initial auth state**
  - Render component with AuthProvider
  - Assert isAuthenticated is false
  - Assert user is null
  - Assert token is null

- **Test: login updates state**
  - Mock API call
  - Call login function
  - Assert token set in state
  - Assert user set in state
  - Assert isAuthenticated is true
  - Assert token saved to localStorage

- **Test: logout clears state**
  - Set initial authenticated state
  - Call logout function
  - Assert token cleared
  - Assert user cleared
  - Assert isAuthenticated is false
  - Assert localStorage cleared

- **Test: loads auth from localStorage on mount**
  - Set token in localStorage before mount
  - Render AuthProvider
  - Assert isAuthenticated is true
  - Assert token loaded from localStorage

- **Test: handles invalid token in localStorage**
  - Set malformed token in localStorage
  - Render AuthProvider
  - Assert isAuthenticated is false
  - Assert token cleared

### ProtectedRoute.test.jsx
- **Test: redirects to login when not authenticated**
  - Mock AuthContext with isAuthenticated=false
  - Render ProtectedRoute with child component
  - Assert redirect to /login

- **Test: renders children when authenticated**
  - Mock AuthContext with isAuthenticated=true
  - Render ProtectedRoute with child component
  - Assert child component rendered

### Sidebar.test.jsx
- **Test: renders all navigation links**
  - Render Sidebar
  - Assert Dashboard link exists
  - Assert Users link exists
  - Assert Forecast Config link exists
  - Assert Forecast Upload link exists
  - Assert Logout button exists

- **Test: highlights active link**
  - Mock useLocation to return current path
  - Render Sidebar with path='/'
  - Assert Dashboard link has active class
  - Change path to '/users'
  - Assert Users link has active class

- **Test: logout button calls logout**
  - Mock AuthContext logout function
  - Render Sidebar
  - Click Logout button
  - Assert logout function called

### MainLayout.test.jsx
- **Test: renders sidebar and outlet**
  - Render MainLayout
  - Assert Sidebar component rendered
  - Assert Outlet (content area) rendered

## Integration Tests (Vitest + React Testing Library)

### Router Integration Test
- **Test: routing from login to dashboard**
  - Render App with Router
  - Start at /login
  - Mock successful login
  - Fill form and submit
  - Assert navigated to /
  - Assert DashboardPage rendered

- **Test: protected route redirects unauthenticated user**
  - Clear localStorage
  - Render App with Router
  - Navigate to /users
  - Assert redirected to /login

- **Test: authenticated user accesses protected route**
  - Set token in localStorage
  - Render App with Router
  - Navigate to /users
  - Assert UserListPage rendered (not login page)

### API Integration Test (with MSW - Mock Service Worker)
- **Test: login API call successful**
  - Mock POST /api/auth/login to return token and user
  - Render LoginPage
  - Submit form with credentials
  - Assert API called with correct payload
  - Assert token stored in localStorage

- **Test: login API call failed**
  - Mock POST /api/auth/login to return 401
  - Render LoginPage
  - Submit form
  - Assert error message displayed

- **Test: axios interceptor adds Authorization header**
  - Set token in AuthContext
  - Make API call to any endpoint
  - Assert Authorization header includes Bearer token

- **Test: axios interceptor handles 401 globally**
  - Mock API call to return 401
  - Make request from authenticated state
  - Assert logout called
  - Assert redirected to /login

## E2E Tests
N/A - E2E testing with Playwright covered in T004.

## Test Data Setup

### Mock API Responses (MSW)
Set up Mock Service Worker handlers:
```javascript
// Login success
rest.post('/api/auth/login', (req, res, ctx) => {
  return res(ctx.json({
    token: 'mock.jwt.token',
    user: {
      id: 1,
      username: 'admin',
      email: 'admin@example.com',
      fullName: 'Admin User',
      roleCode: 'admin'
    }
  }))
})

// Login failure
rest.post('/api/auth/login', (req, res, ctx) => {
  return res(ctx.status(401), ctx.json({ message: 'Invalid credentials' }))
})
```

### Test Users
- Valid user: admin / admin123
- Invalid user: invalid / wrong
- Locked user: locked / password

### localStorage Mocking
Mock localStorage in tests:
```javascript
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
global.localStorage = localStorageMock
```

## Mocking Strategy

### Unit Tests
- Mock AuthContext using custom provider wrapper
- Mock useNavigate and useLocation from react-router-dom
- Mock axios calls using vi.mock('axios')
- Do NOT render actual API calls - use mocked functions

### Integration Tests
- Use MSW for API mocking
- Use MemoryRouter for routing tests
- Render full component trees (not mocked components)
- Mock only external dependencies (API, localStorage)

### Context Mocking
Create test utility:
```javascript
const renderWithAuth = (component, authValue = {}) => {
  return render(
    <AuthContext.Provider value={authValue}>
      {component}
    </AuthContext.Provider>
  )
}
```

### Router Mocking
Create test utility:
```javascript
const renderWithRouter = (component, initialRoute = '/') => {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      {component}
    </MemoryRouter>
  )
}
```

## Test Environment Setup

### Vitest Configuration
- Configure test environment: jsdom
- Setup file for global mocks (MSW, localStorage)
- Configure coverage thresholds (80%+)

### React Testing Library
- Use screen queries (getByRole, getByLabelText, etc.)
- Use userEvent for interactions
- Use waitFor for async assertions

### MSW Setup
```javascript
// setupTests.js
import { setupServer } from 'msw/node'
import { rest } from 'msw'

export const server = setupServer(
  // handlers
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

## Additional Test Scenarios

### Edge Cases
- Token expired (decode and check expiration)
- Malformed token in localStorage
- Network timeout on login
- Rapid navigation between routes
- Double-click submit button
- Browser back/forward navigation

### Accessibility Tests
- Use jest-axe or @axe-core/react
- Test form labels
- Test keyboard navigation
- Test screen reader announcements

### Responsive Tests (Optional)
- Test sidebar on mobile viewport
- Test layout breakpoints
