# X006 Fix - Invalid CORS request when accessing backend via external VM IP

## Background
When accessing the app remotely:
- Frontend: `http://<VM-IP>:5173`
- Backend:  `http://<VM-IP>:8080`

Calling the login endpoint from the browser results in:
- `Invalid CORS request` (Spring/Security CORS rejection)

A curl preflight test using `Origin: http://localhost:5173` succeeds, but real browser Origin is
`http://<VM-IP>:5173`, which is not currently allowed.

## Goal
Allow browser requests from the frontend origin on port 5173 to call backend APIs on port 8080 without CORS errors.

## Scope
Backend only:
- Configure CORS correctly for dev/remote VM usage.
- Ensure Spring Security enables CORS processing.
- Do not change business logic.
- Do not change frontend code in this task.

## Requirements
1) CORS must allow origins for dev on port 5173, including remote VM IP/hostname:
   - Use `allowedOriginPatterns` and include `http://*:5173`
   - Also allow localhost variants (`http://localhost:5173`, `http://127.0.0.1:5173`)
2) Allow methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
3) Allow headers: Authorization, Content-Type, Accept
4) Allow credentials = true
5) Ensure Spring Security enables CORS:
   - `http.cors(Customizer.withDefaults())`
6) Ensure OPTIONS requests are not blocked (permit OPTIONS to all paths).
7) Keep production safety:
   - If there is an environment profile mechanism, limit wildcard patterns to dev profile only.
   - If no profiles exist yet, document clearly in code comments and keep it minimal.

## Expected Files to Change
- A CORS config class (e.g. `CorsConfig.java`)
- Security configuration (e.g. `SecurityConfig.java`)
- Potentially `application.yml` profiles if used

## Notes
- Do not hardcode the VM IP in code.
- Prefer `allowedOriginPatterns` for flexibility in remote dev.
