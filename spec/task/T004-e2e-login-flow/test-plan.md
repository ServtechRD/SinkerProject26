# T004: Test Plan

## Unit Tests
N/A - This task is exclusively E2E testing. Unit tests covered in T002 and T003.

## Integration Tests
N/A - This task is exclusively E2E testing.

## E2E Tests (Playwright)

### login.spec.ts

#### Test: Successful login redirects to dashboard
- Navigate to http://localhost:3000
- Verify redirect to /login (if not already there)
- Locate username input by data-testid="username-input"
- Fill username: "admin"
- Locate password input by data-testid="password-input"
- Fill password: "admin123"
- Click submit button
- Wait for navigation to "/"
- Assert URL is http://localhost:3000/ or http://localhost:3000
- Assert sidebar visible (locate by data-testid="sidebar")
- Assert localStorage contains "authToken"
- Assert localStorage token is non-empty string

#### Test: Invalid username shows error
- Navigate to /login
- Fill username: "nonexistent"
- Fill password: "password"
- Click submit
- Wait for error message to appear
- Assert error text contains "Invalid username or password" or "Invalid credentials"
- Assert still on /login route
- Assert localStorage does not contain authToken

#### Test: Invalid password shows error
- Navigate to /login
- Fill username: "admin"
- Fill password: "wrongpassword"
- Click submit
- Wait for error message
- Assert error text contains "Invalid username or password"
- Assert still on /login route

#### Test: Locked account shows error
- Navigate to /login
- Fill username: "locked_user"
- Fill password: "password"
- Click submit
- Wait for error message
- Assert error text contains "locked" (case insensitive)
- Assert still on /login route

#### Test: Inactive account shows error
- Navigate to /login
- Fill username: "inactive_user"
- Fill password: "password"
- Click submit
- Wait for error message
- Assert error text contains "inactive" (case insensitive)
- Assert still on /login route

#### Test: Protected route redirects unauthenticated user
- Clear localStorage
- Navigate to /users
- Wait for navigation
- Assert URL is /login
- Assert login form visible

#### Test: Empty form validation
- Navigate to /login
- Click submit without filling fields
- Assert form validation error or submit prevented
- Assert still on /login

### navigation.spec.ts

#### Test: Sidebar navigation - Users
- Log in as admin
- Verify on dashboard (/)
- Click sidebar link "Users" (data-testid="nav-users")
- Wait for navigation to /users
- Assert URL is /users
- Assert Users link has active class/styling
- Assert page heading contains "User" (placeholder page)

#### Test: Sidebar navigation - Forecast Config
- Log in as admin
- Click sidebar link "Forecast Config"
- Wait for navigation to /sales-forecast/config
- Assert URL is /sales-forecast/config
- Assert Forecast Config link has active class

#### Test: Sidebar navigation - Forecast Upload
- Log in as admin
- Click sidebar link "Forecast Upload"
- Wait for navigation to /sales-forecast/upload
- Assert URL is /sales-forecast/upload
- Assert Forecast Upload link has active class

#### Test: Sidebar navigation - Dashboard
- Log in as admin
- Navigate to /users
- Click sidebar link "Dashboard"
- Wait for navigation to /
- Assert URL is /
- Assert Dashboard link has active class

#### Test: Logout clears authentication
- Log in as admin
- Verify token in localStorage
- Click logout button (data-testid="logout-button")
- Wait for navigation to /login
- Assert URL is /login
- Assert localStorage does not contain authToken
- Attempt to navigate to /users
- Assert redirected back to /login

#### Test: Token persistence on page refresh
- Log in as admin
- Verify on dashboard
- Reload page
- Wait for page load
- Assert still on /
- Assert sidebar visible
- Assert localStorage still contains authToken
- Click Users link to verify navigation still works

### auth-state.spec.ts (optional additional file)

#### Test: Concurrent sessions (different tabs)
- Log in in first context
- Create second browser context
- Navigate to / in second context
- Assert redirected to /login (no shared state)

#### Test: Expired token handling (if time-based expiration implemented)
- Log in as admin
- Mock time to 25 hours later (or manipulate token in localStorage)
- Navigate to /users
- Assert 401 response handled
- Assert redirected to /login
- Assert token cleared

## Test Data Setup

### Database Seeding Script
Create SQL script or use Flyway test migration:
```sql
-- e2e/fixtures/test-users.sql

-- Admin user (from V2 migration, ensure exists)
-- username: admin, password: admin123

-- Active test user
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked)
VALUES ('testuser', 'testuser@example.com', '$2a$10$hashedpassword', 'Test User', 1, TRUE, FALSE);

-- Locked user
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked)
VALUES ('locked_user', 'locked@example.com', '$2a$10$hashedpassword', 'Locked User', 1, TRUE, TRUE);

-- Inactive user
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked)
VALUES ('inactive_user', 'inactive@example.com', '$2a$10$hashedpassword', 'Inactive User', 1, FALSE, FALSE);
```

### Seed Execution
- Run seed script in `globalSetup` of Playwright config
- Or use database reset before each test file
- Ensure bcrypt hashed passwords match plain text used in tests

### Test User Credentials
- admin / admin123 (active, not locked)
- testuser / password (active, not locked)
- locked_user / password (active, locked)
- inactive_user / password (inactive, not locked)

## Mocking Strategy

### No Frontend Mocking
- Do NOT mock API calls in E2E tests
- Use real backend endpoints
- Use real database with test data

### Backend Test Environment
- Consider using test profile for backend (application-test.yml)
- Use separate test database (not production or dev)
- Disable email sending or other external integrations
- Use in-memory database or Testcontainers if preferred

### localStorage
- Use Playwright's built-in localStorage access
- Clear before tests that require clean state
- Verify token presence/absence directly

## Test Environment Setup

### Prerequisites
- Backend running on localhost:8080
- Frontend running on localhost:3000
- Test database seeded with test users
- Node.js and npm installed
- Playwright installed (`npx playwright install`)

### Playwright Configuration (playwright.config.ts)
```typescript
export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  retries: 2, // retry flaky tests
  workers: 3, // parallel execution
  use: {
    baseURL: 'http://localhost:3000',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'cd ../backend && ./mvnw spring-boot:run -Dspring.profiles.active=test',
      port: 8080,
      timeout: 60000,
      reuseExistingServer: true,
    },
    {
      command: 'npm run dev',
      port: 3000,
      timeout: 30000,
      reuseExistingServer: true,
    },
  ],
});
```

### Page Object Model

#### LoginPage.ts
```typescript
class LoginPage {
  constructor(page: Page);

  async navigate();
  async fillUsername(username: string);
  async fillPassword(password: string);
  async clickSubmit();
  async login(username: string, password: string);
  async getErrorMessage(): Promise<string>;
  async isOnLoginPage(): Promise<boolean>;
}
```

#### MainLayout.ts
```typescript
class MainLayout {
  constructor(page: Page);

  async clickDashboard();
  async clickUsers();
  async clickForecastConfig();
  async clickForecastUpload();
  async clickLogout();
  async isSidebarVisible(): Promise<boolean>;
  async getActiveLink(): Promise<string>;
}
```

### Helper Functions

#### login.helper.ts
```typescript
async function loginAsAdmin(page: Page);
async function loginAsUser(page: Page, username: string, password: string);
async function clearAuth(page: Page);
async function getAuthToken(page: Page): Promise<string | null>;
```

#### db.helper.ts
```typescript
async function seedDatabase();
async function resetDatabase();
async function createTestUser(username, isActive, isLocked);
```

## Test Execution Strategy

### Local Development
- Run tests in headed mode for debugging
- Run single test file during development
- Use `--debug` flag to step through tests

### CI Pipeline
- Run tests in headless mode
- Use `--retries=2` for flaky test tolerance
- Fail fast on first error (optional)
- Upload artifacts on failure

### Test Order
1. login.spec.ts (foundational)
2. navigation.spec.ts (depends on login)
3. auth-state.spec.ts (advanced scenarios)

### Parallel Execution
- Safe to run login tests in parallel (independent)
- Navigation tests can run in parallel if using separate browser contexts
- Database should handle concurrent test users

## Additional Considerations

### CI/CD Integration
- GitHub Actions or GitLab CI
- Start services before tests (docker-compose)
- Run Playwright in CI mode
- Archive test reports as artifacts

### Debugging Failed Tests
- Check screenshots in test-results/
- Check Playwright trace (if enabled)
- Run specific test in headed mode
- Check backend logs for API errors
- Verify test database state

### Performance Optimization
- Use `storageState` to persist auth between tests (avoid repeated logins)
- Parallelize where safe
- Use `beforeAll` for expensive setup
- Minimize page navigation

### Accessibility Testing (Optional)
- Use `@axe-core/playwright` to check accessibility
- Verify keyboard navigation
- Verify screen reader labels
