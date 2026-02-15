# Acceptance Criteria - X002

## Functional
1) Run frontend dev server and verify API requests go through Vite proxy:
   - Browser is on http://localhost:5173
   - API requests are sent to http://localhost:5173/api/... (same origin)
   - Vite proxies to backend http://localhost:8080

2) Swagger UI "Try it out" no longer shows Network Error (in dev).
   - It should successfully call backend endpoints via `/api`.

## Technical
3) `cd frontend && npm ci && npm run dev -- --host` starts successfully.
4) `cd frontend && npm run build` passes (no breaking change).

## Manual Test Steps
1) Start backend on 8080.
2) Start frontend on 5173.
3) Open swagger page (if in frontend) and click "Try it out".
4) In browser DevTools Network:
   - requests should be to /api/* on 5173
   - response should be 200/expected status
