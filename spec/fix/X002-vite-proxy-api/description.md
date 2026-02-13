# X002 Fix Swagger Try-It-Out Network Error via Vite Proxy

## Background
Frontend runs on port 5173 (Vite) and backend runs on port 8080.
Swagger UI "Try it out" triggers browser requests to backend and results in Network Error
due to cross-origin/CORS issues.

## Goal
Solve the problem by using Vite dev-server proxy so that browser requests are same-origin.

## Scope
Frontend only:
- Update Vite dev server proxy to forward `/api/*` to backend `http://localhost:8080`.
- Ensure frontend (and any swagger UI embedded in frontend) uses `/api` as API base path.
- Update any documentation/README if present.

## Out of Scope
- Backend CORS changes
- Backend security changes
- Database changes

## Requirements (Must Follow)
1) Add Vite proxy:
   - Proxy path: `/api`
   - Target: `http://localhost:8080`
   - `changeOrigin: true`
2) Ensure any frontend API calls use `/api/...` (NOT `http://localhost:8080/...`).
3) If swagger UI is rendered in frontend, ensure OpenAPI URL is `/api/v3/api-docs`
   (or whatever backend exposes under `/api`).

## Notes
- This fix is intended for local dev (Vite server). Docker/Nginx deployment is not required in this task.
