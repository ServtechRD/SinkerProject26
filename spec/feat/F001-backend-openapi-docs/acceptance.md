# Acceptance Criteria - F001

## Functional
1) When backend is running:
   - GET /v3/api-docs returns JSON (HTTP 200)
   - Swagger UI is accessible at /swagger-ui/index.html (HTTP 200)
2) OpenAPI metadata is present (title + version at minimum).

## Export / Update Mechanism
3) `scripts/export-openapi.sh` generates `spec/openapi/openapi.json` from a running backend.
4) `scripts/check-openapi.sh` exits non-zero if the generated OpenAPI differs from committed snapshot.

## Build / Test
5) `cd backend && ./gradlew test` passes.
6) `cd backend && ./gradlew bootRun` works (dev profile).

## Manual Test Steps
1) Start backend (local or docker).
2) Open browser:
   - http://localhost:8080/swagger-ui/index.html
3) Verify OpenAPI JSON:
   - curl http://localhost:8080/v3/api-docs
4) Export snapshot:
   - ./scripts/export-openapi.sh http://localhost:8080
5) Check snapshot:
   - ./scripts/check-openapi.sh http://localhost:8080
