# Test Plan — T032

## Unit Tests

### PdcaApiClientStub
- Returns non-empty material list for valid schedule input
- Returns empty list for empty schedule input
- Response DTOs map correctly

### PdcaIntegrationService
- Builds correct PdcaRequest from weekly schedule data
- Maps PdcaResponse materials to MaterialDemand entities correctly
- Deletes old material_demand records before inserting new ones
- Handles PdcaApiClient exception gracefully (logs, does not throw)
- Handles empty PDCA response (no materials)

### WeeklyScheduleService (PDCA trigger)
- Triggers PDCA integration after successful upload
- Triggers PDCA integration after successful edit
- Does not trigger PDCA if upload validation fails
- Schedule upload still succeeds even if PDCA integration fails

## Integration Tests

### PDCA → material_demand write
- Upload schedule → verify material_demand table populated
- Re-upload schedule → verify old records replaced with new ones
- Edit schedule item → verify material_demand updated
- Verify material_demand fields match PDCA response values

### End-to-end flow
- POST /api/weekly-schedule/upload → GET /api/material-demand?week_start=&factory= returns data
- PUT /api/weekly-schedule/:id → material_demand re-calculated

## E2E Tests
- N/A for this task (backend-only integration service)
- Will be covered in T033 (frontend) and T039 (material demand page)

## Test Data Setup
- Seed weekly_schedule records for test week_start+factory
- PdcaApiClientStub configured in test profile
- material_demand table cleared before each test

## Mocking Strategy
- **PDCA API:** Use `PdcaApiClientStub` (always active until real API docs provided)
- **Database:** Testcontainers with MariaDB for integration tests
- **WeeklyScheduleService:** Mock PdcaIntegrationService in unit tests to verify trigger calls
