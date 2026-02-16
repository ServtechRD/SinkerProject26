# X009 - Fix login API always returning 401 + Add global request logging

## Background
The login API endpoint is reachable but login attempts always return `401 Unauthorized`,
even when correct credentials are expected.

The project uses:
- Spring Boot (Java 17)
- JWT authentication (stateless)
- Custom JwtAuthenticationFilter

We also need better visibility for debugging (401/CORS/proxy path issues), so we want a global request log.

## Goals
1) Fix authentication flow for login:
   - Valid credentials -> 200 OK and return token
   - Invalid credentials -> 401 Unauthorized with consistent JSON error
2) Add a global HTTP request logging filter:
   - Log method/path/status/ip/ua/duration for every request
   - Mask sensitive information

## Scope
Backend only (unless frontend payload shape must be aligned):
- Fix login flow and credential verification.
- Ensure JwtAuthenticationFilter does NOT block login and other public endpoints.
- Add global request logging.
- Add tests.

Do NOT:
- Disable security broadly
- Permit all endpoints
- Change unrelated business logic

## Suspected Root Causes (investigate)
1) JwtAuthenticationFilter runs on `/api/auth/login` and rejects due to missing/invalid token.
2) Login path mismatch vs security permit list (trailing slash, different endpoint).
3) User lookup fails (email vs username field mismatch).
4) Password verification fails (BCrypt vs plaintext, double hashing, wrong encoder usage).
5) Request payload mismatch (frontend sends email but backend expects username).

## Requirements - Login
1) Login endpoint must be publicly accessible:
   - `POST /api/auth/login` must NOT require JWT.
2) JwtAuthenticationFilter must skip public endpoints:
   - `/api/auth/login`
   - `/api/health`
   - `/swagger-ui/**`, `/swagger-ui.html`
   - `/v3/api-docs/**`
   - `/actuator/health`
3) Valid credentials:
   - return `200`
   - response JSON includes at least:
     - `token` (JWT)
     - `tokenType` = "Bearer"
     - (optional) minimal user info object
4) Invalid credentials:
   - return `401` with JSON body consistent with project error response.
5) Keep protected APIs protected (no regression).

## Requirements - Global Request Logging
1) Log every incoming request:
   - Timestamp
   - HTTP method
   - URI path
   - Query string (if present)
   - Remote IP
   - User-Agent
2) Log response status code and duration (ms).
3) Do NOT log sensitive data:
   - Do not log Authorization header
   - Do not log request body for login
   - Do not log full JWT tokens
4) Use INFO level for dev.
5) Ensure logging filter runs BEFORE JwtAuthenticationFilter so rejections are visible.

### Example Log Format
`[REQ] POST /api/auth/login status=200 ip=84.xxx.xxx.xxx ua=Mozilla/5.0 12ms`

## Tests
Add/Update tests to verify:
- Login success returns 200 and token
- Login failure returns 401
- Protected endpoint:
  - without token -> 401
  - with token -> 200 (or expected success)
- If possible, add a lightweight test that OPTIONS/POST requests do not get blocked by JwtAuthenticationFilter for login path.

## Data Setup (dev/test)
Provide a deterministic approach to create a test user:
- Prefer integration tests inserting user via repository in test context.
- For dev usage, optionally add dev-only seed data (`data.sql`) OR a dev initializer.

## Expected Files to Change
- Auth controller/service (login logic)
- JwtAuthenticationFilter (exclude paths)
- SecurityConfig (if needed)
- User repository/entity (if needed)
- Global request logging filter (new)
- Tests under `backend/src/test/...`
- Optional: dev seed under `backend/src/main/resources/`

## Notes
- Do not hardcode secrets; use config/env.
- JWT expiration remains 24 hours.
- Keep changes minimal and focused.
