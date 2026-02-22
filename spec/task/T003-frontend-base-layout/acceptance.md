# T003: Acceptance Criteria

## Functional Acceptance Criteria

### Login Page
- [ ] Login page renders at /login route
- [ ] Page displays username input field
- [ ] Page displays password input field
- [ ] Both fields are required
- [ ] Submit button labeled "Login" or similar
- [ ] Form prevents submission if fields empty
- [ ] Loading spinner or disabled button shown during API call
- [ ] Error message displayed for invalid credentials (401)
- [ ] Error message displayed for locked account (403)
- [ ] Error message displayed for inactive account (403)
- [ ] Successful login redirects to / (dashboard)
- [ ] Token stored in localStorage after successful login
- [ ] User info stored in localStorage or context after login

### Authentication Flow
- [ ] Unauthenticated user redirected to /login when accessing protected route
- [ ] Authenticated user can access protected routes
- [ ] Token included in Authorization header for API requests
- [ ] 401 response from API clears token and redirects to login
- [ ] Logout button clears token and redirects to login
- [ ] Page refresh preserves authentication state (reads from localStorage)

### Layout
- [ ] MainLayout renders with sidebar and main content area
- [ ] Sidebar fixed on left side, 250px width
- [ ] Main content area takes remaining width
- [ ] Sidebar visible on all protected routes
- [ ] Sidebar not visible on login page

### Sidebar Navigation
- [ ] Dashboard link renders and navigates to /
- [ ] Users link renders and navigates to /users
- [ ] Forecast Config link renders and navigates to /sales-forecast/config
- [ ] Forecast Upload link renders and navigates to /sales-forecast/upload
- [ ] Active route link highlighted (different background/color)
- [ ] Logout button/link renders
- [ ] Clicking logout clears auth and redirects to login

### Routing
- [ ] / route renders DashboardPage
- [ ] /users route renders UserListPage placeholder
- [ ] /sales-forecast/config route renders ForecastConfigPage placeholder
- [ ] /sales-forecast/upload route renders ForecastUploadPage placeholder
- [ ] /login route renders LoginPage
- [ ] Unknown routes redirect to / or show 404

### Protected Routes
- [ ] All routes except /login require authentication
- [ ] Accessing protected route without token redirects to /login
- [ ] After login, user redirected to originally requested route (or dashboard)

## UI Acceptance Criteria

### Login Page Layout
- [ ] Centered login form (vertically and horizontally)
- [ ] Form width reasonable (300-400px)
- [ ] Input fields have labels or placeholders
- [ ] Password field masks input
- [ ] Submit button full width or appropriately sized
- [ ] Error messages displayed in red above or below form
- [ ] Consistent spacing and padding

### Sidebar Layout
- [ ] Sidebar spans full height of viewport
- [ ] Background color distinct from main content
- [ ] Navigation items vertically stacked
- [ ] Adequate padding around items
- [ ] Active item visually distinct (background color, border, etc.)
- [ ] Logout button positioned at bottom or top (clear separation)

### Main Content Area
- [ ] Content area scrollable if content exceeds viewport height
- [ ] Appropriate padding (16-24px)
- [ ] Background color white or light gray

### Placeholder Pages
- [ ] Dashboard shows heading "Dashboard" or similar
- [ ] User List page shows "User Management" or similar
- [ ] Forecast Config shows heading
- [ ] Forecast Upload shows heading
- [ ] All placeholders clearly indicate they're placeholders (e.g., "Coming soon" or "Under construction")

## Non-Functional Criteria

### Security
- [ ] Password field type="password"
- [ ] Token not logged to console in production
- [ ] No sensitive data in localStorage beyond token
- [ ] HTTPS used in production (configuration ready)

### Performance
- [ ] Login form submits in under 1 second (network permitting)
- [ ] Route transitions instantaneous
- [ ] No unnecessary re-renders on route change
- [ ] Lazy loading for route components (optional but recommended)

### Validation
- [ ] Client-side validation for empty fields
- [ ] Clear validation error messages
- [ ] Prevent double submission
- [ ] Handle network errors gracefully

### Error Handling
- [ ] Network errors show user-friendly message
- [ ] 401/403 errors show appropriate message
- [ ] Axios interceptor catches all 401s globally
- [ ] Errors don't crash the app

### Accessibility
- [ ] Form inputs have labels or aria-labels
- [ ] Submit button keyboard accessible
- [ ] Navigation links keyboard accessible
- [ ] Semantic HTML elements used
- [ ] Adequate color contrast

## How to Verify

### Manual Testing Steps

**Login Flow:**
1. Navigate to http://localhost:3000
2. Verify redirect to /login
3. Enter invalid credentials
4. Verify error message displayed
5. Enter valid credentials (admin/admin123)
6. Verify redirect to dashboard
7. Verify sidebar visible
8. Verify token in localStorage (dev tools)

**Protected Routes:**
1. Log out (clear token)
2. Navigate to http://localhost:3000/users
3. Verify redirect to /login
4. Log in
5. Verify redirect to /users

**Navigation:**
1. Click Dashboard link
2. Verify route changes to /
3. Verify Dashboard link highlighted
4. Click Users link
5. Verify route changes to /users
6. Verify Users link highlighted

**Logout:**
1. Click Logout
2. Verify redirect to /login
3. Verify token removed from localStorage
4. Navigate to /users
5. Verify redirect to /login

### Automated Testing
```bash
cd frontend
npm test -- LoginPage
npm test -- AuthContext
npm test -- ProtectedRoute
```

### Visual Testing
- Inspect sidebar styling
- Verify responsive behavior (optional)
- Test in Chrome, Firefox, Safari

### API Integration Testing
```bash
# Start backend on :8080
# Start frontend on :3000
# Test login flow with actual API
# Verify Authorization header sent (browser dev tools Network tab)
```

### localStorage Verification
```javascript
// In browser console after login
localStorage.getItem('authToken')
localStorage.getItem('user')
```
