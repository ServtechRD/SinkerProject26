# T015: Test Plan

## Unit Tests

### ExcelParserServiceTest
1. **testParseValidExcel**
   - Create in-memory Excel with valid data (3 rows)
   - Call parseExcel()
   - Assert returns list of 3 SalesForecastRow objects
   - Assert each row has correct field values

2. **testParseExcel_SkipsHeaderRow**
   - Create Excel with header + 2 data rows
   - Assert returns 2 items (header skipped)

3. **testParseExcel_EmptyFile**
   - Create Excel with only header row
   - Assert returns empty list or throws appropriate exception

4. **testParseExcel_MissingRequiredColumn**
   - Create Excel missing "品號" column
   - Assert throws ExcelParseException with clear message

5. **testParseExcel_InvalidQuantityFormat**
   - Create Excel with quantity="abc" (non-numeric)
   - Assert throws ExcelParseException with row number

6. **testParseExcel_NegativeQuantity**
   - Create Excel with quantity=-10.5
   - Assert throws ExcelParseException

7. **testParseExcel_ChineseCharacters**
   - Create Excel with Chinese text in all fields
   - Assert parses correctly without encoding issues

8. **testParseExcel_BlankCells**
   - Create Excel with blank cells in optional fields (category, spec, warehouse_location)
   - Assert parses successfully with null/empty values
   - Create Excel with blank product_code
   - Assert throws exception

9. **testParseExcel_DecimalPrecision**
   - Create Excel with quantity=100.99
   - Assert parsed value is exactly 100.99

### ErpProductServiceTest
1. **testValidateProduct_Stub**
   - Call validateProduct("P001")
   - Assert returns true (stub implementation)

2. **testValidateProduct_Future**
   - Placeholder for future real ERP integration tests
   - Mock HTTP client for ERP API call

### SalesForecastUploadServiceTest
1. **testUpload_Success**
   - Mock ExcelParserService to return 5 rows
   - Mock ErpProductService to return true
   - Mock SalesForecastConfigRepository to return open month
   - Mock user owns channel
   - Call upload()
   - Assert SalesForecastRepository.deleteByMonthAndChannel() called
   - Assert SalesForecastRepository.saveAll() called with 5 items
   - Assert all items have is_modified=FALSE
   - Assert all items have correct version string format

2. **testUpload_MonthClosed**
   - Mock config with is_closed=TRUE
   - Assert throws ForbiddenException

3. **testUpload_MonthNotFound**
   - Mock config repository to return empty
   - Assert throws NotFoundException

4. **testUpload_InvalidChannel**
   - Call with channel="InvalidChannel"
   - Assert throws BadRequestException

5. **testUpload_UserDoesNotOwnChannel**
   - Mock user channels list without target channel
   - Assert throws ForbiddenException

6. **testUpload_ProductValidationFails**
   - Mock ErpProductService to return false for product "P999"
   - Assert throws BadRequestException with product code

7. **testUpload_GeneratesCorrectVersion**
   - Mock current timestamp: 2026-01-15 14:30:00
   - Call upload with channel="大全聯"
   - Assert version = "2026/01/15 14:30:00(大全聯)"

8. **testUpload_DeletesOldData**
   - Create existing data for month+channel
   - Call upload
   - Assert old data deleted before new data inserted

9. **testUpload_TransactionRollback**
   - Mock repository saveAll() to throw exception
   - Assert transaction rolled back (no data inserted)

### ExcelTemplateServiceTest
1. **testGenerateTemplate**
   - Call generateTemplate("家樂福")
   - Assert returns ByteArrayInputStream
   - Parse returned Excel
   - Assert has 6 columns with correct headers
   - Assert has sample data row

2. **testGenerateTemplate_Styling**
   - Generate template
   - Parse and check:
     - Header row is bold
     - First row is frozen
     - Column widths are reasonable

## Integration Tests

### SalesForecastUploadControllerIntegrationTest (Spring Boot Test + Testcontainers)

1. **testUpload_Success**
   - Create open month in database
   - Create test Excel file with 10 rows
   - POST /api/sales-forecast/upload with valid JWT and channel ownership
   - Assert 200 OK
   - Assert response contains rows_processed=10
   - Query database and verify 10 rows inserted

2. **testUpload_ReplacesExistingData**
   - Insert 5 existing rows for month+channel
   - Upload 3 new rows
   - Assert only 3 rows remain in database

3. **testUpload_Unauthorized**
   - POST without JWT
   - Assert 401 Unauthorized

4. **testUpload_NoPermission**
   - POST with JWT missing sales_forecast.upload permission
   - Assert 403 Forbidden

5. **testUpload_DoesNotOwnChannel**
   - POST with user who owns different channel
   - Assert 403 Forbidden

6. **testUpload_MonthClosed**
   - Create month with is_closed=TRUE
   - Attempt upload
   - Assert 403 Forbidden

7. **testUpload_InvalidFileFormat**
   - POST with .csv file instead of .xlsx
   - Assert 400 Bad Request

8. **testUpload_EmptyFile**
   - POST with Excel containing only headers
   - Assert 400 Bad Request

9. **testUpload_InvalidData**
   - POST with Excel containing negative quantity
   - Assert 400 Bad Request with row number in error

10. **testUpload_VersionFormat**
    - Upload file
    - Query database
    - Assert version matches format "YYYY/MM/DD HH:MM:SS(通路名稱)"

11. **testTemplateDownload_Success**
    - GET /api/sales-forecast/template/大全聯
    - Assert 200 OK
    - Assert Content-Type is Excel MIME type
    - Assert Content-Disposition header contains filename
    - Parse file and verify headers

12. **testConcurrentUpload_SameMonthChannel**
    - Spawn 2 threads uploading to same month+channel
    - Assert both succeed or one fails gracefully
    - Assert final data is from one complete upload (not mixed)

13. **testConcurrentUpload_DifferentChannels**
    - Spawn 2 threads uploading to different channels
    - Assert both succeed
    - Assert data for both channels exists

## E2E Tests
N/A - Backend upload functionality is thoroughly tested via integration tests. E2E tests will cover the full upload workflow (frontend + backend) in T018.

## Test Data Setup
- Use Testcontainers MariaDB 10.11
- Create test user with sales_forecast.upload permission
- Create user-channel mappings for test user
- Create open month configurations
- Generate test Excel files using Apache POI in test setup
- Sample Excel data:
  ```
  中類名稱: 飲料類, 貨品規格: 600ml*24入, 品號: P001, 品名: 可口可樂, 庫位: A01, 箱數小計: 100.50
  中類名稱: 零食類, 貨品規格: 150g*12包, 品號: P002, 品名: 樂事洋芋片, 庫位: B02, 箱數小計: 50.00
  ```

## Mocking Strategy
- **Unit Tests:**
  - Mock ExcelParserService in upload service tests
  - Mock ErpProductService (always returns true for now)
  - Mock repositories (SalesForecastRepository, SalesForecastConfigRepository)
  - Mock authentication context for channel ownership checks
  - Mock Clock for consistent timestamp in version generation

- **Integration Tests:**
  - No mocking of Spring components
  - Use real database via Testcontainers
  - Use real Apache POI for Excel parsing
  - Use real transaction management
  - Mock ErpProductService as @MockBean (stub implementation)
  - Generate real multipart file uploads in tests

- **Excel File Generation in Tests:**
  - Use Apache POI XSSFWorkbook to create test Excel files in memory
  - Do not read from file system - generate programmatically
