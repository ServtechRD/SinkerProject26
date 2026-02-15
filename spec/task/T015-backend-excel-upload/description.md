# T015: Backend API - Excel Upload for Sales Forecast

## Context
Spring Boot 3.2.12 REST API with JWT authentication. This task implements Excel file upload functionality for bulk sales forecast data import, with validation against ERP API and version tracking.

## Goal
Implement backend API to accept Excel file uploads containing sales forecast data, parse and validate the data, and store it in the database with proper version tracking. Also provide Excel template download functionality.

## Scope

### In Scope
- POST /api/sales-forecast/upload - Upload .xlsx file with forecast data
- GET /api/sales-forecast/template/:channel - Download Excel template
- Parse Excel with columns: 中類名稱, 貨品規格, 品號, 品名, 庫位, 箱數小計
- Validate product_code against ERP API (stub initially)
- Delete existing data for same month+channel before insert
- Generate version string format: "YYYY/MM/DD HH:MM:SS(通路名稱)"
- Permission checks: sales_forecast.upload + channel ownership
- Month open/closed validation

### Out of Scope
- Excel file storage or archive
- Partial upload (all-or-nothing transaction)
- Real-time ERP integration (stub for now)
- Excel validation UI preview before save
- Support for other file formats (CSV, XLS)

## Requirements
- Accept multipart/form-data with file, month (YYYYMM), channel parameters
- Validate file is .xlsx format (check extension and content type)
- Parse Excel using Apache POI
- Expected columns (order matters):
  1. 中類名稱 (category)
  2. 貨品規格 (spec)
  3. 品號 (product_code)
  4. 品名 (product_name)
  5. 庫位 (warehouse_location)
  6. 箱數小計 (quantity)
- Validate each product_code via ErpProductService.validateProduct() (stub returns true initially)
- Check month is open (is_closed=FALSE in sales_forecast_config)
- Check user owns the channel (from JWT claims or user-channel mapping)
- Delete all existing sales_forecast records for same month+channel
- Insert all rows from Excel with is_modified=FALSE
- Generate version: current timestamp formatted as "YYYY/MM/DD HH:MM:SS" + "(" + channel + ")"
- All operations in single transaction (rollback on any error)
- Return summary: rows_processed, version, upload_timestamp
- Template download: pre-formatted .xlsx with headers and sample row

## Implementation Notes
- Use Apache POI (poi-ooxml) for Excel parsing
- Use @Transactional for atomic upload
- Validate month format and existence in sales_forecast_config
- Validate channel is one of 12 valid channels
- Skip header row when parsing
- Validate quantity is positive decimal
- Handle missing/invalid cells gracefully with clear error messages
- ErpProductService stub always returns true (implement real API call in future)
- Log upload activity: user, channel, month, row count, timestamp
- Template service generates Excel with styled headers, frozen panes
- Permission annotation: @PreAuthorize("hasAuthority('sales_forecast.upload')")
- Additional channel ownership check in service layer

## Files to Change
- `src/main/java/com/servtech/sinker/controller/SalesForecastUploadController.java` (new)
- `src/main/java/com/servtech/sinker/service/ExcelParserService.java` (new)
- `src/main/java/com/servtech/sinker/service/ErpProductService.java` (new - stub)
- `src/main/java/com/servtech/sinker/service/SalesForecastUploadService.java` (new)
- `src/main/java/com/servtech/sinker/entity/SalesForecast.java` (new)
- `src/main/java/com/servtech/sinker/repository/SalesForecastRepository.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/UploadRequest.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/UploadResponse.java` (new)
- `pom.xml` (update - add Apache POI dependency)

## Dependencies
- T014: sales_forecast table must exist
- T012: sales_forecast_config table and API for month validation
- T002: Authentication and permission system
- Apache POI library (poi-ooxml version 5.x)
- Spring Web MultipartFile support
