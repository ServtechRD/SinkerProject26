# T002: Acceptance Criteria

## Functional Acceptance Criteria

### Login Endpoint
- [ ] POST /api/auth/login endpoint exists
- [ ] Accepts Content-Type: application/json
- [ ] Request body validates username and password are required
- [ ] Returns 400 if username or password missing
- [ ] Returns 401 if username not found
- [ ] Returns 401 if password incorrect
- [ ] Returns 403 if user is_active=FALSE
- [ ] Returns 403 if user is_locked=TRUE
- [ ] Returns 200 with token and user info on success
- [ ] Updates last_login_at timestamp on successful login

### JWT Token
- [ ] Token is valid JWT format (header.payload.signature)
- [ ] Token contains claim: sub (username)
- [ ] Token contains claim: userId
- [ ] Token contains claim: roleCode
- [ ] Token contains iat (issued at)
- [ ] Token contains exp (expiration = iat + 24 hours)
- [ ] Token is signed with HS256 algorithm
- [ ] Token can be decoded and verified

### Protected Endpoints
- [ ] Requests without Authorization header to protected routes return 401
- [ ] Requests with invalid token return 401
- [ ] Requests with expired token return 401
- [ ] Requests with valid token proceed to controller
- [ ] Spring Security context contains authenticated user
- [ ] User principal includes user ID, username, role

### Public Endpoints
- [ ] /api/auth/login accessible without token
- [ ] /swagger-ui/** accessible without token
- [ ] /v3/api-docs/** accessible without token
- [ ] /actuator/health accessible without token (if enabled)

## API Contracts

### POST /api/auth/login

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response 200 (Success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "fullName": "System Administrator",
    "roleCode": "admin"
  }
}
```

**Response 401 (Invalid Credentials):**
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/auth/login"
}
```

**Response 403 (Account Locked):**
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Account is locked",
  "path": "/api/auth/login"
}
```

**Response 403 (Account Inactive):**
```json
{
  "timestamp": "2026-02-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Account is inactive",
  "path": "/api/auth/login"
}
```

### Protected Endpoint Request Format
```
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## UI Acceptance Criteria
N/A - This is backend only. UI integration covered in T003.

## Non-Functional Criteria

### Security
- [ ] Passwords never logged or returned in responses
- [ ] JWT secret stored in configuration (not hardcoded)
- [ ] Bcrypt cost factor appropriate (10-12)
- [ ] CORS configured to allow only trusted origins
- [ ] Token signature verified on every request
- [ ] No sensitive data in JWT payload

### Validation
- [ ] Username trimmed and validated (not empty, max 50 chars)
- [ ] Password validated (not empty)
- [ ] Null checks for database lookups
- [ ] Exception handling for all error cases

### Performance
- [ ] Login completes in under 500ms (excluding network)
- [ ] JWT verification completes in under 50ms
- [ ] Database query uses indexed column (username)

### Error Handling
- [ ] All exceptions caught and mapped to appropriate HTTP status
- [ ] Stack traces not exposed in production
- [ ] Consistent error response format
- [ ] Logged errors include request context

## How to Verify

### Manual Testing with cURL

**Successful login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Invalid password:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'
```

**Access protected endpoint without token:**
```bash
curl -X GET http://localhost:8080/api/users
# Should return 401
```

**Access protected endpoint with token:**
```bash
TOKEN="<token from login response>"
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN"
```

### Verify JWT Token
```bash
# Decode token at https://jwt.io
# Or use jq:
echo "<token>" | cut -d. -f2 | base64 -d | jq
```

### Database Verification
```sql
-- Check last_login_at updated
SELECT username, last_login_at FROM users WHERE username='admin';
```

### Integration Test Execution
```bash
cd backend
./mvnw test -Dtest=AuthControllerTest
./mvnw test -Dtest=JwtTokenProviderTest
```
