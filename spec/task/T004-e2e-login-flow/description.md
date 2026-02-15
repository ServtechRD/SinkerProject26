# T004: E2E Login Flow Testing

## Context
This task establishes end-to-end testing for the authentication flow using Playwright. It verifies the complete user journey from login through navigation, ensuring the frontend (T003) correctly integrates with the backend authentication system (T002).

## Goal
Create comprehensive Playwright E2E tests that validate the complete login workflow, including successful authentication, invalid credentials handling, account lockout scenarios, post-login navigation, and sidebar functionality.

## Scope

### In Scope
- E2E test: Successful login flow (login page → enter credentials → dashboard)
- E2E test: Invalid credentials (verify error message displayed)
- E2E test: Locked account (verify error message and no redirect)
- E2E test: Inactive account (verify error message)
- E2E test: Post-login navigation (verify sidebar links work)
- E2E test: Protected route redirect (verify unauthenticated redirect to login)
- E2E test: Logout flow (verify logout clears auth and redirects)
- E2E test: Token persistence (verify refresh maintains auth state)
- Playwright configuration for frontend and backend
- Test fixtures for database seeding
- Page Object Model for login and layout pages
- Test data setup and teardown

### Out of Scope
- Backend unit tests (covered in T002)
- Frontend unit tests (covered in T003)
- Performance testing
- Visual regression testing
- Mobile responsive testing
- Browser compatibility testing beyond Chrome/Chromium

## Requirements
- Configure Playwright to run against local development servers
- Start backend on localhost:8080 and frontend on localhost:3000
- Create database test fixtures with known users (active, inactive, locked)
- Implement Page Object Model for LoginPage and MainLayout
- Test successful login redirects to dashboard
- Test invalid credentials show error message "Invalid username or password"
- Test locked account shows error message "Account is locked"
- Test inactive account shows error message "Account is inactive"
- Test sidebar navigation links work after login
- Test logout clears token and redirects to login
- Test accessing protected route without auth redirects to login
- Test page refresh maintains authentication state
- All tests should be independent and idempotent
- Use Playwright's auto-waiting and retry mechanisms
- Take screenshots on failure for debugging

## Implementation Notes
- Use Playwright Test framework (not Jest + Playwright)
- Configure baseURL for frontend (http://localhost:3000)
- Use beforeEach/afterEach hooks for test isolation
- Seed test database before test suite runs
- Consider using Playwright's storageState for auth persistence tests
- Use data-testid attributes in React components for stable selectors
- Create helper functions: login(), logout(), seedDatabase()
- Use Playwright's expect assertions
- Configure headed mode for debugging, headless for CI
- Set timeout appropriate for API calls (5-10 seconds)
- Use parallel execution where possible

## Files to Change
- e2e/login.spec.ts (new)
- e2e/navigation.spec.ts (new)
- e2e/fixtures/test-users.sql (new)
- e2e/page-objects/LoginPage.ts (new)
- e2e/page-objects/MainLayout.ts (new)
- e2e/helpers/db-setup.ts (new)
- playwright.config.ts (new or update)
- package.json (add Playwright scripts)

## Dependencies
- T002: Requires backend authentication API to be functional
- T003: Requires frontend login page and layout to be implemented
