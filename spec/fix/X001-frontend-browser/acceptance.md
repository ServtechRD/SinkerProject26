# Acceptance Criteria

## Functional
- Browser loads frontend without blank screen
- No critical console error
- Network requests return valid responses
- Login page renders UI correctly

## Technical
- `npm ci && npm run build` passes
- `docker compose up frontend` works
- Port accessible from host browser
- Vite dev server reachable

## Test Steps
1. docker compose build frontend
2. docker compose up -d frontend
3. Open browser http://localhost:<port>
4. Console has no red errors
5. UI visible

## Non-Functional
- Build time < 2 min
- No dependency conflicts
