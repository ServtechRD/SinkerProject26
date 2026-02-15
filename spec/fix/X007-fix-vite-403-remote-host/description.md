# X007 Fix - Vite 403 when accessing /api/* via remote VM IP (5173)

## Background
When accessing the frontend from an external machine using the VM public IP:
- Frontend: `http://<VM-IP>:5173`

A POST to:
- `http://<VM-IP>:5173/api/auth/login`
returns `403 Forbidden`.

This suggests the Vite dev server is rejecting requests from non-allowed hosts (DNS rebinding protection / host allowlist),
preventing the `/api` proxy from working when accessed via public IP.

## Goal
Allow remote access to the Vite dev server via VM public IP (or hostname) so that:
- `/api/*` requests sent to port 5173 are accepted by Vite
- `/api/*` is proxied to backend at `http://localhost:8080`
- Login works from outside without CORS errors

## Scope
Frontend only:
- Update Vite dev server configuration to allow the VM IP host.
- Ensure `/api` proxy is configured correctly.
- Ensure frontend API calls use relative `/api/...` paths (no hardcoded backend URL).
- No backend CORS/security changes in this task.

## Requirements
1) Vite dev server must accept requests from remote host:
   - Configure Vite `server.allowedHosts` appropriately:
     - Prefer `allowedHosts: true` if supported by current Vite version, OR
     - Use a whitelist array including:
       - the VM public IP (e.g. `84.247.145.63`)
       - `localhost`
2) Vite dev server must bind to all interfaces:
   - `server.host = "0.0.0.0"`
   - use port 5173
3) Ensure Vite proxy forwards `/api` to backend:
   - `/api` -> `http://localhost:8080`
4) Frontend login request must call relative path:
   - use `/api/auth/login` (not `http://localhost:8080/...`)
5) Document how to run dev server for remote access.

## Expected Files to Change
- `frontend/vite.config.ts` (or `vite.config.js`)
- Frontend API client (if it currently hardcodes backend URL)
- Optional: `frontend/README.md` (run instructions)

## Notes
- This is a dev environment fix. Production should use a proper reverse proxy (Nginx) and domain allowlist.
