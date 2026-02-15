# X005 - Force OpenAPI Server URL to Relative "/" (Fix Swagger Try-It-Out Port Issue)

## Background
Swagger UI is served via frontend (Vite on port 5173) and proxied to backend (port 8080).
Swagger "Try it out" currently sends requests directly to backend port 8080 because the OpenAPI JSON includes:

"servers":[{"url":"http://localhost:8080","description":"Generated server url"}]

As a result, API calls do NOT go through the Vite proxy (5173), causing network/CORS issues.

## Goal
Make Swagger "Try it out" call APIs using the current page origin (port 5173) by forcing OpenAPI `servers` to be relative.

Target behavior:
- OpenAPI JSON should use `servers: [{ "url": "/" }]` (relative).
- Swagger Try-It-Out should call:
  - `http://<current-host>:5173/api/...` (same-origin)
  - then Vite proxy forwards to `http://localhost:8080/api/...`

## Scope
Backend only:
- Add or update OpenAPI configuration so `servers` is relative.
- Do not modify controller mappings or API paths.
- Do not modify frontend/vite configuration in this task.

## Requirements (Must Follow)
1) OpenAPI JSON at `/v3/api-docs` MUST NOT contain hardcoded host/port (e.g. `http://localhost:8080`).
2) OpenAPI `servers` MUST be set to a relative URL:
   - Preferred: `"/"`
3) Keep existing paths unchanged (e.g. `/api/health` must remain as-is).
4) Keep Swagger UI enabled (do not disable for prod).

## Implementation Hints
- Create/update a Spring configuration class, e.g.:
  `backend/src/main/java/.../config/OpenApiConfig.java`
- Define an `@Bean OpenAPI customOpenAPI()` that sets:
  - title/description/version (keep current values)
  - servers = `List.of(new Server().url("/"))`

## Deliverables
- Code change to OpenAPI configuration.
- If OpenAPI snapshot export exists (spec/openapi/openapi.json), update it accordingly.
- Commit, push branch, open PR to `claude/integration`.
