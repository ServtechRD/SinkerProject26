# Acceptance Criteria - X005

## OpenAPI JSON Checks
1) `/v3/api-docs` must not include hardcoded server url:
   - `curl -s http://localhost:8080/v3/api-docs | grep -n "http://localhost:8080"` returns nothing

2) `/v3/api-docs` must include a relative server url:
   - `curl -s http://localhost:8080/v3/api-docs | grep -n "\"servers\""` shows `url":"\/"` (or servers omitted entirely, but preferred is "/")

Example expected snippet:
"servers":[{"url":"/", ... }]

## Swagger Try-It-Out Behavior (Manual)
3) Open Swagger UI from the frontend host (5173).
4) Use "Try it out" for `/api/health`.
5) In browser Network tab, requests should be made to:
   - `http://<host>:5173/api/health` (same-origin)
   - NOT directly to `http://<host>:8080/...`

## Build/Test
6) Backend build passes:
   - `cd backend && ./gradlew build`

## Definition of Done
- OpenAPI server url is relative ("/") and no longer hardcoded to 8080.
- Swagger Try-It-Out uses the current origin (5173) and works through Vite proxy.
- PR is created and merged into `claude/integration`.
