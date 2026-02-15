# T032 — Backend: PDCA API Integration Service

## 1. Context
Module 6 (Weekly Production Schedule) requires that every time a weekly schedule is uploaded or edited, the system calls the PDCA API to calculate material requirements. The PDCA system takes production schedule data, cross-references BOM and ERP inventory, and returns the raw material quantities needed.

## 2. Goal
Implement a PDCA API integration service that:
- Sends weekly schedule data to the PDCA API
- Receives calculated material requirements
- Writes results into the `material_demand` table (Module 8)
- Is triggered automatically after weekly schedule upload/edit (T031)

The PDCA API will be **stubbed** initially since external API docs are pending.

## 3. Scope

### In Scope
- PDCA API client service (stub implementation)
- Request/response DTOs matching PDCA API contract
- Writing PDCA results to `material_demand` table
- Triggering PDCA calculation from WeeklyScheduleService after upload/edit
- Stub returning realistic mock data

### Out of Scope
- Real PDCA API integration (pending external docs)
- Frontend changes
- Material demand query API (T038)

## 4. Requirements
- Call PDCA API with schedule data: `{"schedule":[{"product_code","quantity","demand_date"}]}`
- Parse response: `{"materials":[{"material_code","material_name","unit","demand_date","expected_delivery","demand_quantity","estimated_inventory"}]}`
- Delete existing material_demand records for same week_start+factory before inserting new ones
- Stub must return plausible mock data for testing
- Handle API failures gracefully (log error, do not fail the upload)

## 5. Implementation Notes
- Create `PdcaApiClient` interface + `PdcaApiClientStub` implementation (profile-switchable)
- Create `PdcaRequest` / `PdcaResponse` DTOs
- Create `PdcaIntegrationService` that orchestrates: build request from schedule → call API → map response → save to material_demand
- Modify `WeeklyScheduleService` to call `PdcaIntegrationService` after successful upload/edit
- Use `@Async` or fire-and-forget pattern so PDCA failures don't block the upload response

## 6. Files to Change
- `backend/src/main/java/.../service/PdcaApiClient.java` (interface)
- `backend/src/main/java/.../service/PdcaApiClientStub.java` (stub)
- `backend/src/main/java/.../service/PdcaIntegrationService.java`
- `backend/src/main/java/.../dto/PdcaRequest.java`
- `backend/src/main/java/.../dto/PdcaResponse.java`
- `backend/src/main/java/.../service/WeeklyScheduleService.java` (modify to trigger PDCA)
- `backend/src/main/java/.../entity/MaterialDemand.java`
- `backend/src/main/java/.../repository/MaterialDemandRepository.java`

## 7. Dependencies
- T031 (Weekly schedule API — provides the trigger point)
- T037 (material_demand table migration — provides the target table)
