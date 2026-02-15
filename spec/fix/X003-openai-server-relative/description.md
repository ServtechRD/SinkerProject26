# X003 Fix Swagger Try-It-Out Uses Wrong Port (Use Relative Server URL)

## Background
Frontend is served on host port 5173, but Swagger "Try it out" requests are sent to port 80 (or another fixed port).
This happens because the OpenAPI `servers` (or generated host) is fixed to `:80`, so Swagger uses that base URL.

## Goal
Fix Swagger Try-It-Out so it calls APIs using the current page origin and Vite proxy:
- Swagger should call `/api/...` (same-origin), not `http://<host>:80/...`.

## Scope
Backend only:
- Update OpenAPI generation to NOT hardcode host/port.
- Set OpenAPI `servers` to a relative base URL: `/api` (preferred).
- Do NOT modify frontend code for this task.
- Do NOT change backend routes, only documentation behavior.

## Requirements (Must Follow)
1) OpenAPI servers must be relative:
   - Use `Server().url("/api")`, or remove explicit servers if present.
2) OpenAPI JSON must NOT include a fixed host/port like `http://...:80`.
3) Swagger UI Try-It-Out must issue requests to:
   - `http://<current-host>/api/...` (same-origin), so Vite proxy can forward to backend 8080.

## Notes
- This change is to support dev flow where frontend uses Vite proxy.
- Keep Swagger enabled in prod as before (do not disable).
- If there is an existing OpenAPI config bean, modify it. Otherwise add one.

## Deliverables
- Code change in backend OpenAPI configuration.
- Update/confirm OpenAPI snapshot (if the project uses spec/openapi/openapi.json).
- Tests if applicable (at least a minimal test that `/api/v3/api-docs` exists and does not contain `:80`).
