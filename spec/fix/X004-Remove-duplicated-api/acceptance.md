# Acceptance Criteria - X004

## Functional Verification
1. Backend endpoint check:
   - `curl http://localhost:8080/api/health` returns HTTP 200.

2. OpenAPI JSON verification:
   - `curl http://localhost:8080/api/v3/api-docs | grep "/api/api"` returns no results.

3. Swagger UI Try-It-Out:
   - Requests are sent to `/api/...` only once.
   - No `/api/api/...` appears in network logs.

## Technical Verification
4. Backend builds successfully:
   - `cd backend && ./gradlew build` passes.

5. No changes to controller routes:
   - Existing endpoints remain functional.

6. No frontend changes:
   - Vite configuration remains untouched.

## Manual Browser Test
1. Start backend on port 8080.
2. Open Swagger UI.
3. Click "Try it out" on `/health`.
4. Network tab should show:
   - `http://localhost:5173/api/health` (via proxy) OR
   - `http://localhost:8080/api/health` (direct)
5. No duplicated `/api/api/...` should appear.

## Definition of Done
- Swagger requests are correct.
- No duplicated prefixes.
- Backend builds successfully.
- PR is created and merged into `claude/integration`.
