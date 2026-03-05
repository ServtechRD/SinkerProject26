# T031: Backend - Weekly Schedule API

## Context
The weekly production schedule module enables users to upload Excel files containing production schedules, query schedules by week and factory, and manually edit schedule entries. Excel uploads replace existing data for the specified week and factory to ensure data consistency.

## Goal
Implement REST API endpoints for Excel upload, querying weekly schedules, and editing individual schedule entries with Monday validation and permission control.

## Scope

### In Scope
- POST endpoint for Excel file upload (multipart/form-data)
- GET endpoint to query schedules by week_start and factory
- PUT endpoint to edit individual schedule entries
- Excel parsing with specific column mapping (需求日期, 品號, 品名, 庫位, 箱數小計)
- Delete existing data for week+factory before inserting upload data
- Monday validation for week_start parameter
- Permission-based access control (weekly_schedule.upload, weekly_schedule.view, weekly_schedule.edit)
- Error handling for invalid Excel format

### Out of Scope
- Excel export functionality
- Batch edit operations
- Schedule approval workflow
- Email notifications
- Integration with PDCA API (covered in T032)
- Schedule templates or generation

## Requirements

### POST /api/weekly-schedule/upload
- **Request**: multipart/form-data with file, week_start, factory
- **Parameters**:
  - file: Excel file (.xlsx, .xls)
  - week_start: DATE string (YYYY-MM-DD, must be Monday)
  - factory: STRING
- **Permission**: weekly_schedule.upload
- **Business Logic**:
  1. Validate week_start is Monday
  2. Validate Excel file format
  3. Delete existing records for week_start + factory
  4. Parse Excel with columns: 需求日期, 品號, 品名, 庫位, 箱數小計
  5. Map to: demand_date, product_code, product_name, warehouse_location, quantity
  6. Insert all rows in single transaction
  7. Return count of inserted records
- **Validation**:
  - week_start must be Monday (DAYOFWEEK = 2)
  - Excel file must have required columns
  - demand_date must be valid date
  - quantity must be valid decimal >= 0

### GET /api/weekly-schedule
- **Query Parameters**:
  - week_start (required): DATE string
  - factory (required): STRING
- **Permission**: weekly_schedule.view
- **Response**: List of WeeklyScheduleDTO
- **Business Logic**:
  - Fetch all records matching week_start and factory
  - Order by demand_date, product_code

### PUT /api/weekly-schedule/:id
- **Request Body**:
  ```json
  {
    "demandDate": "2026-02-03",
    "quantity": 150.50
  }
  ```
- **Permission**: weekly_schedule.edit
- **Business Logic**:
  - Load existing record by ID
  - Update demand_date and/or quantity
  - Validate demand_date is valid date
  - Validate quantity >= 0
  - Return updated record

## Implementation Notes
- Use Apache POI for Excel parsing
- Excel column mapping (Chinese headers):
  - 需求日期 → demand_date
  - 品號 → product_code
  - 品名 → product_name
  - 庫位 → warehouse_location
  - 箱數小計 → quantity
- Monday validation: use LocalDate.getDayOfWeek() == DayOfWeek.MONDAY
- Delete + Insert wrapped in @Transactional for atomicity
- Support both .xlsx and .xls formats
- Skip empty rows in Excel
- Log upload statistics (rows processed, rows inserted)
- Return 400 if week_start not Monday
- Return 400 if Excel missing required columns
- Return 404 if ID not found (for PUT)

## Files to Change
- `backend/src/main/java/com/servtech/sinker/controller/WeeklyScheduleController.java` (new)
- `backend/src/main/java/com/servtech/sinker/service/WeeklyScheduleService.java` (new)
- `backend/src/main/java/com/servtech/sinker/service/WeeklyScheduleExcelParser.java` (new)
- `backend/src/main/java/com/servtech/sinker/entity/WeeklySchedule.java` (new)
- `backend/src/main/java/com/servtech/sinker/repository/WeeklyScheduleRepository.java` (new)
- `backend/src/main/java/com/servtech/sinker/dto/WeeklyScheduleDTO.java` (new)
- `backend/src/main/java/com/servtech/sinker/dto/UploadScheduleRequest.java` (new)
- `backend/src/main/java/com/servtech/sinker/dto/UpdateScheduleRequest.java` (new)
- `backend/pom.xml` (add Apache POI dependency)

## Dependencies
- T030: Database table must exist
- Apache POI library for Excel parsing
- Spring Security for permission checking
- Spring Web for multipart file upload
