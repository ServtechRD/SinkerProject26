# T010: Acceptance Criteria

## Functional Acceptance Criteria

### Login Logging - Successful Login
- [ ] Successful login creates login_logs entry
- [ ] Entry has login_type='success'
- [ ] Entry has user_id set to logged-in user
- [ ] Entry has username
- [ ] Entry has ip_address from request
- [ ] Entry has user_agent from request headers
- [ ] Entry has created_at timestamp
- [ ] Entry has NULL failed_reason
- [ ] failed_login_count reset to 0 for user

### Login Logging - Failed Login (Invalid Credentials)
- [ ] Failed login creates login_logs entry
- [ ] Entry has login_type='failed'
- [ ] Entry has user_id NULL or set if username found
- [ ] Entry has username (even if user doesn't exist)
- [ ] Entry has ip_address
- [ ] Entry has user_agent
- [ ] Entry has failed_reason='Invalid username or password'
- [ ] If username exists, failed_login_count incremented

### Login Logging - Failed Login (Locked Account)
- [ ] Locked account login attempt creates login_logs entry
- [ ] Entry has login_type='failed'
- [ ] Entry has failed_reason='Account is locked'
- [ ] failed_login_count NOT incremented (already locked)

### Login Logging - Failed Login (Inactive Account)
- [ ] Inactive account login attempt creates login_logs entry
- [ ] Entry has login_type='failed'
- [ ] Entry has failed_reason='Account is inactive'
- [ ] failed_login_count NOT incremented

### Account Lockout Logic
- [ ] User with 0 failed attempts can log in
- [ ] After 1 failed attempt, failed_login_count=1
- [ ] After 4 failed attempts, failed_login_count=4, account not locked
- [ ] After 5 failed attempts, failed_login_count=5, is_locked=TRUE
- [ ] Subsequent login attempts for locked account return 403
- [ ] Successful login resets failed_login_count to 0
- [ ] 4 failed attempts then 1 successful resets count, user not locked

### IP Address Extraction
- [ ] IP extracted from X-Forwarded-For if present
- [ ] IP extracted from X-Real-IP if present and X-Forwarded-For not present
- [ ] IP extracted from request.getRemoteAddr() if no proxy headers
- [ ] IPv4 addresses stored correctly
- [ ] IPv6 addresses stored correctly
- [ ] NULL or unknown IP handled gracefully

### User Agent Extraction
- [ ] User-Agent header extracted from request
- [ ] User agent stored in login_logs
- [ ] Missing User-Agent handled gracefully (NULL or empty string)

## API Contracts

### POST /api/auth/login - Successful

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response 200:** (same as T002)

**Database State After:**
```sql
-- login_logs entry
SELECT login_type, username, ip_address, user_agent, failed_reason
FROM login_logs
WHERE username='admin'
ORDER BY created_at DESC LIMIT 1;
-- Result: login_type='success', user_agent='...', failed_reason=NULL

-- users table
SELECT failed_login_count, is_locked FROM users WHERE username='admin';
-- Result: failed_login_count=0, is_locked=FALSE
```

### POST /api/auth/login - Failed (Invalid Password)

**Request:**
```json
{
  "username": "admin",
  "password": "wrongpassword"
}
```

**Response 401:**
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/auth/login"
}
```

**Database State After:**
```sql
-- login_logs entry
SELECT login_type, failed_reason FROM login_logs
WHERE username='admin' ORDER BY created_at DESC LIMIT 1;
-- Result: login_type='failed', failed_reason='Invalid username or password'

-- users table
SELECT failed_login_count FROM users WHERE username='admin';
-- Result: failed_login_count incremented by 1
```

### POST /api/auth/login - Account Locked After 5 Failures

**Scenario:** User has 4 failed attempts, this is the 5th

**Response 403:**
```json
{
  "timestamp": "2026-02-15T10:35:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Account is locked",
  "path": "/api/auth/login"
}
```

**Database State After:**
```sql
SELECT failed_login_count, is_locked FROM users WHERE username='testuser';
-- Result: failed_login_count=5, is_locked=TRUE
```

## UI Acceptance Criteria
N/A - This is backend only. Frontend displays error messages from T002/T003.

## Non-Functional Criteria

### Security
- [ ] Logging does not expose sensitive data (passwords never logged)
- [ ] Failed username logged even if user doesn't exist (for audit)
- [ ] IP address logged for security analysis
- [ ] User agent logged for device tracking
- [ ] Lockout prevents brute-force attacks

### Performance
- [ ] Logging does not significantly delay login response (< 50ms overhead)
- [ ] Async logging preferred (if implemented)
- [ ] Database insert optimized (indexed columns)

### Data Integrity
- [ ] Log insert failure does not prevent successful login
- [ ] failed_login_count incremented atomically
- [ ] is_locked flag set atomically with count
- [ ] No race conditions on concurrent login attempts

### Error Handling
- [ ] Missing IP address doesn't fail login
- [ ] Missing user agent doesn't fail login
- [ ] Database error during logging doesn't fail login
- [ ] All exceptions logged for debugging

### Audit Trail
- [ ] All login attempts logged (100% coverage)
- [ ] Timestamps accurate for audit
- [ ] Failed reasons clear and unambiguous
- [ ] Username preserved even if user deleted (denormalized)

## How to Verify

### Manual Testing

**Successful Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "User-Agent: TestClient/1.0" \
  -d '{"username":"admin","password":"admin123"}'

# Check database
SELECT * FROM login_logs WHERE username='admin' ORDER BY created_at DESC LIMIT 1;
SELECT failed_login_count, is_locked FROM users WHERE username='admin';
```

**Failed Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'

# Check database
SELECT login_type, failed_reason FROM login_logs WHERE username='admin' ORDER BY created_at DESC LIMIT 1;
SELECT failed_login_count FROM users WHERE username='admin';
```

**Lockout After 5 Failures:**
```bash
# Fail 5 times
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"wrong"}'
  echo "\nAttempt $i"
done

# Check locked
SELECT is_locked, failed_login_count FROM users WHERE username='testuser';

# Try to login with correct password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"correct"}'
# Should return 403 Account is locked
```

**Reset Count on Success:**
```bash
# User with 3 failed attempts
# Login successfully
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"correct"}'

# Check count reset
SELECT failed_login_count FROM users WHERE username='testuser';
# Should be 0
```

### Database Verification

**Check login logs:**
```sql
SELECT
  login_type,
  username,
  ip_address,
  user_agent,
  failed_reason,
  created_at
FROM login_logs
ORDER BY created_at DESC
LIMIT 10;
```

**Check lockout state:**
```sql
SELECT
  username,
  failed_login_count,
  is_locked,
  last_login_at
FROM users
WHERE username='testuser';
```

**Verify IP and User Agent logged:**
```sql
SELECT DISTINCT ip_address, user_agent
FROM login_logs
WHERE username='admin'
LIMIT 5;
```

### Integration Test Execution
```bash
cd backend
./mvnw test -Dtest=LoginLogServiceTest
./mvnw test -Dtest=AuthServiceTest
./mvnw test -Dtest=AuthControllerIntegrationTest
```

### Lockout Scenario Test
1. Create test user with failed_login_count=0
2. Attempt login with wrong password 5 times
3. Verify is_locked=TRUE after 5th attempt
4. Verify 5 failed log entries created
5. Attempt login with correct password
6. Verify 403 response
7. Verify 6th log entry with "Account is locked"
