# T010: Test Plan

## Unit Tests

### LoginLogServiceTest
- **Test: logSuccessfulLogin creates entry**
  - Given user, IP address, user agent
  - When logSuccessfulLogin called
  - Then login_logs entry created with login_type='success'
  - Then user_id, username, ip_address, user_agent set
  - Then failed_reason is NULL

- **Test: logFailedLogin creates entry**
  - Given username, IP address, user agent, failed reason
  - When logFailedLogin called
  - Then login_logs entry created with login_type='failed'
  - Then username, ip_address, user_agent, failed_reason set
  - Then user_id is NULL (or set if user exists)

- **Test: logSuccessfulLogin with missing IP**
  - Given NULL IP address
  - When logSuccessfulLogin called
  - Then entry created with NULL ip_address
  - Then no exception thrown

- **Test: logFailedLogin with missing user agent**
  - Given NULL user agent
  - When logFailedLogin called
  - Then entry created with NULL user_agent
  - Then no exception thrown

### AuthServiceTest (updated for T010)
- **Test: successful login logs and resets count**
  - Given user with failed_login_count=3
  - Given valid credentials
  - When login called
  - Then successful login_logs entry created
  - Then user.failed_login_count reset to 0
  - Then login succeeds

- **Test: failed login increments count**
  - Given user with failed_login_count=2
  - Given invalid password
  - When login called
  - Then failed login_logs entry created
  - Then user.failed_login_count incremented to 3
  - Then login fails with 401

- **Test: 5th failed login locks account**
  - Given user with failed_login_count=4
  - Given invalid password
  - When login called
  - Then user.is_locked set to TRUE
  - Then user.failed_login_count set to 5
  - Then login fails with 403

- **Test: locked account cannot login**
  - Given user with is_locked=TRUE
  - Given valid password
  - When login called
  - Then login fails with 403
  - Then failed_reason='Account is locked'
  - Then failed_login_count NOT incremented

- **Test: inactive account logs failure**
  - Given user with is_active=FALSE
  - When login called
  - Then failed login_logs entry created
  - Then failed_reason='Account is inactive'

- **Test: non-existent username logs failure**
  - Given non-existent username
  - When login called
  - Then failed login_logs entry created with username
  - Then user_id is NULL
  - Then failed_reason='Invalid username or password'

### IPAddressUtilTest
- **Test: extract IP from X-Forwarded-For**
  - Given request with X-Forwarded-For header
  - When extractIP called
  - Then returns first IP from header

- **Test: extract IP from X-Real-IP**
  - Given request with X-Real-IP header (no X-Forwarded-For)
  - When extractIP called
  - Then returns X-Real-IP value

- **Test: extract IP from remote address**
  - Given request with no proxy headers
  - When extractIP called
  - Then returns request.getRemoteAddr()

- **Test: handle missing headers**
  - Given request with no IP headers
  - When extractIP called
  - Then returns fallback or NULL gracefully

- **Test: handle IPv6 addresses**
  - Given request with IPv6 address
  - When extractIP called
  - Then returns full IPv6 address

## Integration Tests

### AuthControllerIntegrationTest (updated for T010)
- **Test: successful login creates log entry**
  - Given valid credentials
  - When POST /api/auth/login
  - Then status 200
  - Then login_logs has new entry with login_type='success'
  - Then ip_address and user_agent populated

- **Test: failed login creates log entry**
  - Given invalid credentials
  - When POST /api/auth/login
  - Then status 401
  - Then login_logs has new entry with login_type='failed'
  - Then failed_reason='Invalid username or password'

- **Test: lockout after 5 failed attempts**
  - Given user with failed_login_count=0
  - When POST /api/auth/login with wrong password 5 times
  - Then 5 failed login_logs entries created
  - Then user.is_locked=TRUE
  - Then user.failed_login_count=5

- **Test: locked account returns 403**
  - Given user with is_locked=TRUE
  - When POST /api/auth/login with correct password
  - Then status 403
  - Then message='Account is locked'
  - Then login_logs entry created with failed_reason='Account is locked'

- **Test: successful login resets failed count**
  - Given user with failed_login_count=3, is_locked=FALSE
  - When POST /api/auth/login with correct password
  - Then status 200
  - Then user.failed_login_count=0

- **Test: IP address logged correctly**
  - Given request with X-Forwarded-For header
  - When POST /api/auth/login
  - Then login_logs.ip_address matches X-Forwarded-For value

- **Test: user agent logged correctly**
  - Given request with User-Agent header
  - When POST /api/auth/login
  - Then login_logs.user_agent matches User-Agent value

- **Test: non-existent user logs with NULL user_id**
  - Given username that doesn't exist
  - When POST /api/auth/login
  - Then status 401
  - Then login_logs entry has username but user_id=NULL

### LoginLogRepositoryTest
- **Test: save and retrieve login log**
  - Create LoginLog entity
  - Save to repository
  - Query by username
  - Assert retrieved correctly

- **Test: query by user_id**
  - Save multiple login logs for user
  - Query by user_id
  - Assert all logs for user returned

- **Test: query by date range**
  - Save logs with different timestamps
  - Query with date range
  - Assert only logs in range returned

## E2E Tests
N/A - Login logging is backend functionality. E2E tests from T004 will indirectly test this (can verify logs created).

## Test Data Setup

### Test Users
Create users with different failed_login_count values:
```sql
-- User with 0 failed attempts
INSERT INTO users (..., failed_login_count, is_locked) VALUES (..., 0, FALSE);

-- User with 3 failed attempts
INSERT INTO users (..., failed_login_count, is_locked) VALUES (..., 3, FALSE);

-- User with 4 failed attempts (one away from lockout)
INSERT INTO users (..., failed_login_count, is_locked) VALUES (..., 4, FALSE);

-- Locked user
INSERT INTO users (..., failed_login_count, is_locked) VALUES (..., 5, TRUE);
```

### Test Scenarios
- Fresh user (0 failures)
- User with some failures (1-4)
- User at lockout threshold (4 failures)
- Locked user (5+ failures)
- Inactive user
- Non-existent user

## Mocking Strategy

### Unit Tests
- Mock LoginLogRepository
- Mock UserRepository
- Mock HttpServletRequest for IP/user agent extraction
- Do NOT mock LoginLogService in AuthServiceTest - use real service with mocked repo

### Integration Tests
- Do NOT mock repositories - use Testcontainers
- Use MockMvc to set request headers (X-Forwarded-For, User-Agent)
- Use real database to verify state changes
- Verify login_logs entries created

### Test Isolation
- Use @Transactional with rollback
- Clean up login_logs between tests (high volume table)
- Reset user failed_login_count between tests

## Additional Test Scenarios

### Concurrency Tests
- **Test: concurrent failed logins increment count correctly**
  - Simulate 5 concurrent login attempts
  - Verify failed_login_count is exactly 5 (no lost updates)
  - Verify is_locked set correctly

- **Test: concurrent login (one success, one failure)**
  - User with 3 failed attempts
  - Thread 1: successful login (should reset count)
  - Thread 2: failed login (should increment count)
  - Verify final state is consistent

### Edge Cases
- **Test: lockout exactly at 5 failures**
  - 4 failures: not locked
  - 5th failure: locked
  - Verify boundary condition

- **Test: multiple failures after lockout**
  - Locked user attempts login 3 more times
  - Verify failed_login_count doesn't keep incrementing
  - Verify all attempts logged

- **Test: unlock and re-lock**
  - Locked user
  - Admin unlocks (set is_locked=FALSE, failed_login_count=0)
  - User fails login 5 times again
  - Verify locked again

### Transaction Tests
- **Test: login log insert fails**
  - Mock repository to throw exception on save
  - Attempt login
  - Verify login still completes (log failure doesn't block auth)

- **Test: failed_login_count update is atomic**
  - Simulate failure during update
  - Verify no partial state (count incremented but is_locked not set)

### IP Address Tests
- **Test: IPv4 address formats**
  - Test 127.0.0.1
  - Test 192.168.1.1
  - Test public IP

- **Test: IPv6 address formats**
  - Test full IPv6: 2001:0db8:85a3:0000:0000:8a2e:0370:7334
  - Test compressed IPv6: 2001:db8::1
  - Verify stored correctly (VARCHAR 45)

- **Test: X-Forwarded-For with multiple IPs**
  - Header: "203.0.113.1, 198.51.100.1, 192.0.2.1"
  - Verify first IP extracted (203.0.113.1)

### User Agent Tests
- **Test: various user agents**
  - Chrome user agent
  - Firefox user agent
  - Mobile user agent
  - Custom user agent
  - Very long user agent (TEXT field)

### Audit Log Tests
- **Test: login log retention**
  - Create logs over time
  - Query old logs
  - Verify accessible for audit

- **Test: denormalized username preserved**
  - Create user
  - Log login
  - Delete user
  - Query login_logs
  - Verify username still present (user_id NULL)
