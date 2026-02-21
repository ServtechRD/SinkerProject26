# Acceptance Criteria - X009

## 1) Login is public (no JWT required)
`POST /api/auth/login` must be accessible without Authorization header.
It must not be blocked by JwtAuthenticationFilter.

## 2) Login success returns token
Given a valid user exists (test seed or created for test):

```bash
curl -i -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

Expected:

HTTP 200

JSON includes:

token (non-empty string)

tokenType equals Bearer

3) Login failure returns 401

Wrong password:

Expected:

HTTP 401

JSON error body present and consistent.

4) Protected endpoints remain protected

Without token: 401

With returned token: 200 (or expected success)

Example:

TOKEN=<token-from-login>

curl -i "http://localhost:8080/api/health"
# health may be public depending on config

curl -i "http://localhost:8080/api/some-protected" 
# expect 401

curl -i "http://localhost:8080/api/some-protected" \
  -H "Authorization: Bearer $TOKEN"
# expect 200

5) Global Request Logging

For every request, logs must include:

Method, path, status, ip, user-agent, duration

Logs must NOT include:

Authorization header

Password

Full JWT tokens

6) Tests

cd backend && ./gradlew test passes

Includes at least:

login success integration test

login failure integration test


---

## `spec/task/X009-fix-login-always-401/test-plan.md`

```md
# Test Plan - X009

## Integration Tests (Spring Boot Test + MockMvc)
1) POST `/api/auth/login` success
   - Arrange: insert a user with BCrypt password in test DB (via repository).
   - Act: POST with correct credentials.
   - Assert: 200, token returned.

2) POST `/api/auth/login` failure
   - Act: POST with wrong password.
   - Assert: 401.

3) Protected endpoint smoke test
   - Without token -> 401
   - With token from login -> 200 (choose any existing protected endpoint).

## Unit Tests
- If AuthService exists, add unit tests for:
  - user not found
  - wrong password
  - correct password -> token generation

## Request Logging Validation
- Prefer a lightweight unit/integration test:
  - Ensure filter runs and does not throw.
- Manual check: verify log output includes method/path/status and excludes Authorization/password.

## Manual Verification
- Run backend and test with curl.
- Ensure login works from frontend after backend fix (payload shape aligned).