# Acceptance Criteria - X003

## Functional
1) When frontend runs on 5173 and backend runs on 8080 (with Vite proxy):
   - Swagger Try-It-Out sends requests to same-origin `/api/...` (NOT `:80`).
2) OpenAPI JSON at `/api/v3/api-docs` MUST NOT include a fixed host/port (e.g. `:80`).

## Verification Steps
1) Start backend on 8080.
2) Confirm OpenAPI JSON:
   - curl -s http://localhost:8080/api/v3/api-docs | grep -n ':80'  -> should return nothing
3) Open Swagger UI and use Try-It-Out (via frontend swagger page or wherever it is embedded):
   - Network requests should be to `http://<host>:5173/api/...` (same origin)

## Build/Test
- `cd backend && ./gradlew test` passes
- If OpenAPI snapshot exists: regenerate and commit it
