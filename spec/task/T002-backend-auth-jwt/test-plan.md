# T002: Test Plan

## Unit Tests

### JwtTokenProviderTest
- **Test: generate token with valid user**
  - Given user with id, username, roleCode
  - When generateToken called
  - Then token is non-null, non-empty
  - Then token can be parsed
  - Then token contains correct claims (sub, userId, roleCode)
  - Then exp = iat + 24 hours

- **Test: validate valid token**
  - Given freshly generated token
  - When validateToken called
  - Then returns true

- **Test: validate expired token**
  - Given token with exp in past (mock clock or use reflection)
  - When validateToken called
  - Then returns false

- **Test: validate tampered token**
  - Given valid token with modified payload
  - When validateToken called
  - Then throws exception or returns false

- **Test: extract username from token**
  - Given valid token
  - When getUsernameFromToken called
  - Then returns correct username

- **Test: extract claims from token**
  - Given valid token
  - When getUserIdFromToken, getRoleCodeFromToken called
  - Then return correct values

### AuthServiceTest
- **Test: login with valid credentials**
  - Given existing active user with correct password
  - When login called
  - Then returns LoginResponse with token
  - Then user info populated correctly
  - Then last_login_at updated in database

- **Test: login with invalid username**
  - Given non-existent username
  - When login called
  - Then throws UsernameNotFoundException or returns 401

- **Test: login with invalid password**
  - Given existing user with wrong password
  - When login called
  - Then throws BadCredentialsException or returns 401

- **Test: login with inactive account**
  - Given user with is_active=FALSE
  - When login called
  - Then throws exception with message "Account is inactive"

- **Test: login with locked account**
  - Given user with is_locked=TRUE
  - When login called
  - Then throws AccountLockedException

- **Test: password verification**
  - Given user with bcrypt hashed password
  - When verifyPassword called with correct password
  - Then returns true
  - When verifyPassword called with wrong password
  - Then returns false

## Integration Tests

### AuthControllerIntegrationTest (with @SpringBootTest and Testcontainers)
- **Test: POST /api/auth/login - successful login**
  - Given admin user exists in database
  - When POST /api/auth/login with correct credentials
  - Then status 200
  - Then response contains token (non-empty string)
  - Then response contains user object with id, username, email, fullName, roleCode
  - Then database shows last_login_at updated

- **Test: POST /api/auth/login - invalid username**
  - When POST with non-existent username
  - Then status 401
  - Then error message "Invalid username or password"

- **Test: POST /api/auth/login - invalid password**
  - Given admin user exists
  - When POST with wrong password
  - Then status 401
  - Then error message "Invalid username or password"

- **Test: POST /api/auth/login - inactive account**
  - Given user with is_active=FALSE
  - When POST with correct credentials
  - Then status 403
  - Then error message "Account is inactive"

- **Test: POST /api/auth/login - locked account**
  - Given user with is_locked=TRUE
  - When POST with correct credentials
  - Then status 403
  - Then error message "Account is locked"

- **Test: POST /api/auth/login - missing username**
  - When POST with only password
  - Then status 400
  - Then validation error message

- **Test: POST /api/auth/login - missing password**
  - When POST with only username
  - Then status 400
  - Then validation error message

### JwtAuthenticationFilterIntegrationTest
- **Test: access protected endpoint without token**
  - When GET /api/users (or any protected endpoint)
  - Then status 401

- **Test: access protected endpoint with valid token**
  - Given valid JWT token
  - When GET protected endpoint with Authorization: Bearer <token>
  - Then status not 401 (may be 200 or 403 depending on permissions)
  - Then request proceeds to controller

- **Test: access protected endpoint with expired token**
  - Given expired token (use mock time or wait)
  - When GET protected endpoint
  - Then status 401

- **Test: access protected endpoint with invalid signature**
  - Given token signed with different secret
  - When GET protected endpoint
  - Then status 401

- **Test: access protected endpoint with malformed token**
  - Given invalid JWT format (not 3 parts)
  - When GET protected endpoint
  - Then status 401

### SecurityConfigIntegrationTest
- **Test: public endpoints accessible without auth**
  - When GET /api/auth/login (OPTIONS for CORS preflight)
  - Then status 200 or 405 (not 401)
  - When GET /swagger-ui/index.html
  - Then status not 401
  - When GET /v3/api-docs
  - Then status not 401

- **Test: CORS configuration**
  - When OPTIONS /api/auth/login with Origin: http://localhost:3000
  - Then Access-Control-Allow-Origin header present
  - Then Access-Control-Allow-Methods includes POST
  - When OPTIONS protected endpoint
  - Then CORS headers present

## E2E Tests
N/A - E2E testing covered in T004 which includes full login flow with frontend.

## Test Data Setup

### Database Seeding for Tests
Use @Sql scripts or Testcontainers with Flyway migrations:
- Run V1 and V2 migrations to create tables and seed admin user
- Create additional test users:
  - active_user: is_active=TRUE, is_locked=FALSE
  - inactive_user: is_active=FALSE
  - locked_user: is_locked=TRUE

### Test Users
```sql
-- In test data script
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked)
VALUES
  ('testuser', 'test@example.com', '<bcrypt_hash>', 'Test User', 1, TRUE, FALSE),
  ('inactive', 'inactive@example.com', '<bcrypt_hash>', 'Inactive User', 1, FALSE, FALSE),
  ('locked', 'locked@example.com', '<bcrypt_hash>', 'Locked User', 1, TRUE, TRUE);
```

## Mocking Strategy

### Unit Tests
- Mock UserRepository in AuthServiceTest
- Mock BCryptPasswordEncoder for password verification
- Mock Clock or use fixed time for token expiration tests
- Do NOT mock JwtTokenProvider in AuthServiceTest - use real implementation

### Integration Tests
- Do NOT mock repositories - use Testcontainers MariaDB
- Do NOT mock Spring Security components
- Use real JWT token generation and validation
- Use TestRestTemplate or MockMvc for HTTP requests

### Test Isolation
- Use @Transactional on test methods for automatic rollback
- Or use @DirtiesContext if needed
- Clean up test data between tests
- Reset database state for each test class

## Additional Test Scenarios

### Concurrency Tests
- Test multiple simultaneous login requests
- Verify no race conditions on last_login_at update

### Token Lifecycle Tests
- Generate token, wait 24 hours (mock), verify expired
- Generate token, use multiple times, verify all succeed

### Edge Cases
- Empty string username/password
- Very long username/password (test validation)
- SQL injection attempts in username
- Special characters in password
- Unicode characters in user data
