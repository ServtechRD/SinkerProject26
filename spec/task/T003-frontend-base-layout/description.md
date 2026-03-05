# T003: Frontend Base Layout and Authentication

## Context
This task establishes the foundational React 18 application structure with authentication integration, routing, and base layout. It connects to the JWT authentication backend implemented in T002 and provides the navigation framework for all future features.

## Goal
Create a complete React frontend with login flow, JWT token management, protected routing, and a base layout featuring a fixed left sidebar and main content area. Implement authentication context and redirect logic for protected routes.

## Scope

### In Scope
- Login page with username/password form
- Authentication context for global auth state management
- JWT token storage in localStorage
- API service layer for authentication endpoints
- Protected route wrapper component
- Post-login redirect to dashboard
- Base layout with fixed left sidebar and main content area
- React Router configuration with public and protected routes
- Sidebar navigation component with menu items
- Dashboard placeholder page
- User management placeholder page
- Sales forecast config placeholder page
- Sales forecast upload placeholder page
- Logout functionality
- Auto-redirect to login if no token present

### Out of Scope
- User registration
- Password reset
- Token refresh mechanism
- Actual implementation of user management (T008)
- Actual implementation of forecast pages (future tasks)
- Responsive mobile layout (can be added later)
- User profile dropdown
- Role-based menu visibility (can be added later)

## Requirements
- Login page at /login route (public)
- Form fields: username (required), password (required)
- Display validation errors
- Display API error messages (401, 403)
- Show loading state during login
- Store JWT token in localStorage on success
- Redirect to / (dashboard) after successful login
- Create AuthContext with: user, token, login(), logout(), isAuthenticated
- Implement ProtectedRoute component that redirects to /login if not authenticated
- Create MainLayout with fixed left sidebar (250px width) and flex main content
- Sidebar contains navigation links: Dashboard, Users, Forecast Config, Forecast Upload
- Active route highlighted in sidebar
- Logout button in sidebar
- React Router routes: /login, /, /users, /sales-forecast/config, /sales-forecast/upload
- All routes except /login are protected
- Axios instance with Authorization header interceptor
- Handle 401 responses by clearing token and redirecting to login

## Implementation Notes
- Use React Router v6
- Use Axios for HTTP requests
- Store token in localStorage key: 'authToken'
- Store user info in localStorage key: 'user' (or fetch from token on load)
- Create axios instance with baseURL from environment variable (default: http://localhost:8080)
- Add request interceptor to inject Authorization: Bearer <token>
- Add response interceptor to handle 401 globally
- Use React Context API for auth state (or Zustand if preferred)
- Use controlled components for login form
- Use useState for form state and loading/error states
- Sidebar should use NavLink from react-router-dom for active styling
- Main content area should have padding and overflow-y scroll
- Use CSS modules or styled-components for styling
- Show password visibility toggle (optional but recommended)

## Files to Change
- src/App.jsx (new)
- src/main.jsx (update to add Router)
- src/router.jsx (new)
- src/layouts/MainLayout.jsx (new)
- src/components/Sidebar.jsx (new)
- src/components/ProtectedRoute.jsx (new)
- src/pages/LoginPage.jsx (new)
- src/pages/DashboardPage.jsx (new)
- src/pages/users/UserListPage.jsx (new - placeholder)
- src/pages/sales-forecast/ForecastConfigPage.jsx (new - placeholder)
- src/pages/sales-forecast/ForecastUploadPage.jsx (new - placeholder)
- src/contexts/AuthContext.jsx (new)
- src/api/axios.js (new)
- src/api/auth.js (new)
- src/App.css or src/styles/ (new)
- .env (add VITE_API_BASE_URL)

## Dependencies
- T002: Requires backend JWT authentication endpoint to be functional
