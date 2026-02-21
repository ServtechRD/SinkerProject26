# T004: Acceptance Criteria

## Functional Acceptance Criteria

### Test Coverage
- [ ] Successful login test exists and passes
- [ ] Invalid username test exists and passes
- [ ] Invalid password test exists and passes
- [ ] Locked account test exists and passes
- [ ] Inactive account test exists and passes
- [ ] Protected route redirect test exists and passes
- [ ] Logout flow test exists and passes
- [ ] Navigation test exists and passes
- [ ] Token persistence test exists and passes

### Successful Login Flow Test
- [ ] Test navigates to /login
- [ ] Test enters valid username
- [ ] Test enters valid password
- [ ] Test clicks submit button
- [ ] Test verifies redirect to / (dashboard)
- [ ] Test verifies sidebar is visible
- [ ] Test verifies user is authenticated (token in localStorage)

### Invalid Credentials Tests
- [ ] Test enters invalid username
- [ ] Test enters valid password
- [ ] Test clicks submit
- [ ] Test verifies error message: "Invalid username or password"
- [ ] Test verifies no redirect occurs
- [ ] Test verifies token NOT in localStorage

### Locked Account Test
- [ ] Test enters locked user credentials
- [ ] Test clicks submit
- [ ] Test verifies error message: "Account is locked"
- [ ] Test verifies no redirect occurs

### Inactive Account Test
- [ ] Test enters inactive user credentials
- [ ] Test clicks submit
- [ ] Test verifies error message: "Account is inactive"
- [ ] Test verifies no redirect occurs

### Protected Route Redirect Test
- [ ] Test clears any existing auth token
- [ ] Test navigates to /users (protected route)
- [ ] Test verifies redirect to /login
- [ ] Test verifies URL is /login

### Logout Flow Test
- [ ] Test logs in successfully
- [ ] Test clicks logout button
- [ ] Test verifies redirect to /login
- [ ] Test verifies token removed from localStorage
- [ ] Test attempts to navigate to protected route
- [ ] Test verifies redirect back to /login

### Navigation Test
- [ ] Test logs in successfully
- [ ] Test clicks "Users" link in sidebar
- [ ] Test verifies navigation to /users
- [ ] Test verifies Users link is highlighted/active
- [ ] Test clicks "Dashboard" link
- [ ] Test verifies navigation to /
- [ ] Test verifies Dashboard link is highlighted/active

### Token Persistence Test
- [ ] Test logs in successfully
- [ ] Test refreshes page (page.reload())
- [ ] Test verifies still authenticated
- [ ] Test verifies sidebar still visible
- [ ] Test verifies on dashboard route

## Non-Functional Criteria

### Test Reliability
- [ ] Tests pass consistently (no flaky tests)
- [ ] Tests are independent (can run in any order)
- [ ] Tests clean up after themselves
- [ ] Tests use proper waits (no arbitrary sleeps)

### Test Performance
- [ ] Each test completes in under 10 seconds
- [ ] Full test suite completes in under 2 minutes
- [ ] Tests run in parallel where possible

### Test Maintainability
- [ ] Page Object Model used for reusability
- [ ] Selectors use data-testid or stable attributes
- [ ] Common actions extracted to helper functions
- [ ] Test data managed separately from test logic

### Configuration
- [ ] Playwright config includes baseURL
- [ ] Config includes reasonable timeouts
- [ ] Config includes retry logic for CI
- [ ] Screenshots on failure enabled
- [ ] Video recording enabled for failed tests (optional)

### Database Setup
- [ ] Test database seeded before tests run
- [ ] Test users created: admin, testuser, locked_user, inactive_user
- [ ] Database reset between test runs
- [ ] No test pollution between runs

## How to Verify

### Run All E2E Tests
```bash
cd frontend
npm run test:e2e
# or
npx playwright test
```

### Run Specific Test File
```bash
npx playwright test login.spec.ts
npx playwright test navigation.spec.ts
```

### Run in Headed Mode (for debugging)
```bash
npx playwright test --headed
npx playwright test --debug
```

### View Test Report
```bash
npx playwright show-report
```

### Check Test Coverage
- All test scenarios listed above should have passing tests
- Playwright HTML report should show 100% pass rate
- No skipped or flaky tests

### Manual Verification of Test Environment
1. Start backend: `cd backend && ./mvnw spring-boot:run`
2. Start frontend: `cd frontend && npm run dev`
3. Verify http://localhost:8080/api/auth/login accessible
4. Verify http://localhost:3000 accessible
5. Run Playwright tests

### Verify Test Data Setup
```sql
-- Connect to test database
SELECT username, is_active, is_locked FROM users
WHERE username IN ('admin', 'testuser', 'locked_user', 'inactive_user');

-- Should return 4 rows with appropriate flags
```

### Verify Screenshots on Failure
1. Force a test to fail
2. Check test-results/ directory
3. Verify screenshot captured

### CI Pipeline Verification (if applicable)
- [ ] Tests run in CI environment
- [ ] CI starts backend and frontend before tests
- [ ] CI fails build if tests fail
- [ ] CI uploads test artifacts (screenshots, videos, reports)

## Test Artifacts

### Expected Output Files
- playwright-report/ (HTML report)
- test-results/ (screenshots, videos on failure)
- .auth/ (storage state files if used)

### Expected Console Output
```
Running 9 tests using 3 workers

  ✓ login.spec.ts:5:1 › successful login redirects to dashboard (2s)
  ✓ login.spec.ts:15:1 › invalid credentials show error (1.5s)
  ✓ login.spec.ts:25:1 › locked account shows error (1.5s)
  ✓ login.spec.ts:35:1 › inactive account shows error (1.5s)
  ✓ login.spec.ts:45:1 › protected route redirects to login (1s)
  ✓ navigation.spec.ts:5:1 › sidebar navigation works (2.5s)
  ✓ navigation.spec.ts:20:1 › logout clears auth (2s)
  ✓ navigation.spec.ts:30:1 › token persists on refresh (2s)

  9 passed (15s)
```
