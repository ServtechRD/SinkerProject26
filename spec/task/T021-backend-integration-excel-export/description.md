# T021: Backend API - Integration Excel Export

## Context
Spring Boot 3.2.12 REST API with JWT authentication and Apache POI for Excel generation. This task implements Excel export functionality for the 12-channel integration data with professional formatting.

## Goal
Implement backend API endpoint to export the 12-channel integration data as a formatted Excel file (.xlsx) with styled headers, proper column widths, and number formatting.

## Scope

### In Scope
- GET /api/sales-forecast/integration/export - Export integration data as Excel
- Accept query params: month (required), version (optional, defaults to latest)
- Use integration data from T020 (same query logic)
- Excel formatting:
  - Sheet name: "銷售預估量整合 - YYYYMM"
  - Frozen header row
  - Bold, centered, colored headers
  - Right-aligned numeric columns
  - Auto-width columns based on content
  - Borders around all cells
- Column headers in Chinese and English
- File download with proper filename
- Permission check: sales_forecast.view

### Out of Scope
- Multiple sheet support
- Charts or graphs
- Conditional formatting (colored cells based on values)
- Export to other formats (CSV, PDF)
- Scheduled export or email delivery

## Requirements
- **GET /api/sales-forecast/integration/export**: Accept month (required), version (optional)
- Query integration data using ForecastIntegrationService from T020
- Generate Excel file using Apache POI:
  - Workbook: XSSFWorkbook (.xlsx format)
  - Sheet name: "銷售預估量整合 - {month}" (e.g., "銷售預估量整合 - 202601")
  - Header row (row 0):
    - Columns: 庫位, 中類名稱, 貨品規格, 品名, 品號, PX/大全聯, 家樂福, 愛買, 711, 全家, OK/萊爾富, 好市多, 楓康, 美聯社, 康是美, 電商, 市面經銷, 原始小計, 差異, 備註
    - Bold font, centered alignment, background color (light blue or gray)
  - Data rows: populate from integration query results
  - Freeze panes: freeze first row (header)
  - Auto-size columns to fit content
  - Number format for quantity columns: "#,##0.00" (thousand separator, 2 decimals)
  - Right-align all numeric columns
  - Borders: thin borders around all cells
- Response:
  - Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
  - Content-Disposition: attachment; filename="sales_forecast_integration_{month}_{version}.xlsx"
- Permission: sales_forecast.view required
- Return 400 if month missing

## Implementation Notes
- Use Spring Boot @RestController
- Reuse ForecastIntegrationService from T020 for data query
- Create ExcelExportService for Excel generation logic
- Apache POI classes:
  - XSSFWorkbook: Excel workbook
  - XSSFSheet: worksheet
  - XSSFRow, XSSFCell: rows and cells
  - XSSFCellStyle: cell styling
  - XSSFFont: font styling
  - CreationHelper: for formatting
- Styling best practices:
  - Create reusable cell styles (header style, number style, text style)
  - Use IndexedColors for background colors
  - Set column widths using sheet.autoSizeColumn() after populating data
- Write workbook to ByteArrayOutputStream, return as ResponseEntity
- Set proper HTTP headers for file download
- Log export activity: user, month, version, row count
- Handle large datasets: stream if necessary (consider SXSSF for very large files)

## Files to Change
- `src/main/java/com/servtech/sinker/controller/ForecastIntegrationController.java` (update - add export endpoint)
- `src/main/java/com/servtech/sinker/service/ExcelExportService.java` (new)
- `pom.xml` (verify Apache POI dependency exists from T015)

## Dependencies
- T020: Integration query service for data retrieval
- T002: Authentication and permission system
- Apache POI library (poi-ooxml version 5.x)
- Spring Web for file download
