# X004 - Remove Duplicated /api Prefix in OpenAPI and Springdoc Configuration

## Background
The backend API already exposes endpoints with the `/api` prefix  
(e.g. `/api/health` returns HTTP 200).

However, Swagger / OpenAPI "Try it out" requests are being sent to  
`/api/api/...`, resulting in 404 errors.

This happens because the OpenAPI `servers` configuration or Springdoc
path settings are adding an extra `/api` prefix on top of the existing one.

## Goal
Ensure Swagger UI and OpenAPI use the correct base path so that:
- Requests are sent to `/api/...` (single prefix)
- No duplicated `/api/api/...` occurs

## Scope
Backend configuration only:
- OpenAPI configuration
- Springdoc configuration
- No frontend changes
- No controller route changes
- No database changes

## Requirements (Must Follow)
1. Remove `/api` from OpenAPI `servers` configuration.
   - Preferred: do not define `servers` at all.
   - Alternative: use `/` as server URL.
2. If `springdoc.api-docs.path` or `swagger-ui.path` contains `/api`,
   remove the duplicated `/api` prefix.
3. Do NOT modify existing controller mappings or REST endpoints.
4. Do NOT change Vite proxy or frontend code.

## Technical Notes
- The backend already handles `/api` routing correctly.
- Swagger UI must rely on the current origin and existing endpoint paths.
- Avoid hardcoding hostnames or ports.

## Deliverables
- Updated OpenAPI configuration class or bean.
- Updated `application.yml` or `application.properties` if needed.
- Ensure OpenAPI JSON does not contain duplicated `/api`.
- Commit and open PR.
