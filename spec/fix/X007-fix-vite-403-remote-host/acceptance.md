# Acceptance Criteria - X007

## Remote Host Access
From an external machine (not the VM itself):

1) Frontend is accessible:
- `http://<VM-IP>:5173/` loads successfully

2) Login API call via Vite proxy returns a backend response (not Vite 403):
- In browser DevTools -> Network:
  - POST `http://<VM-IP>:5173/api/auth/login`
  - must NOT return `403 Forbidden` from Vite
  - must return a backend response (e.g. 200/401/400 depending on credentials)

## Proxy Verification
On the VM, verify Vite proxy is active:
- The login request should reach backend logs.
- Alternatively, verify:
  - `curl -i http://localhost:5173/api/health` returns backend response (200)

## No Hardcoded Backend URL
- Search the frontend codebase to ensure no direct `http://localhost:8080` is used in API calls.

## Build / Run
- `cd frontend && npm ci`
- `cd frontend && npm run dev -- --host 0.0.0.0 --port 5173` starts without errors.

## Definition of Done
- Remote access to Vite (via VM public IP) works.
- `/api/*` calls through port 5173 succeed and are proxied to backend.
- Login no longer fails due to Vite 403.
