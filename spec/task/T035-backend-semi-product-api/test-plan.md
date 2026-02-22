# T035: Test Plan

## Unit Tests

### SemiProductExcelParserTest
- **test_parse_valid_excel_file**
  - Create Excel with valid data
  - Parse and verify all rows extracted correctly

- **test_parse_excel_with_chinese_headers**
  - Verify parser recognizes 品號, 品名, 提前日數

- **test_parse_excel_missing_headers**
  - Excel without required headers
  - Verify exception thrown

- **test_parse_excel_with_empty_rows**
  - Excel with empty rows between data
  - Verify empty rows skipped

- **test_parse_excel_with_invalid_advance_days**
  - Row with non-numeric advance_days
  - Verify validation error

### SemiProductServiceTest
- **test_upload_truncates_and_inserts**
  - Mock repository
  - Insert initial data
  - Call upload with new data
  - Verify deleteAll() called then saveAll() called

- **test_upload_validates_all_rows_before_truncate**
  - Upload with mix of valid and invalid rows
  - Verify no data changed (transaction rolled back)

- **test_upload_validates_positive_advance_days**
  - Upload with zero and negative values
  - Verify validation errors returned

- **test_list_all_products**
  - Mock repository with test data
  - Verify all products returned as DTOs

- **test_update_advance_days**
  - Mock existing product
  - Update advance_days
  - Verify only advance_days changed

- **test_update_nonexistent_product**
  - Update with invalid ID
  - Verify exception thrown

- **test_generate_template**
  - Call template generation
  - Verify Excel file created with correct headers

## Integration Tests

### SemiProductControllerIntegrationTest
- **test_upload_endpoint_with_valid_file**
  - Use @SpringBootTest with Testcontainers
  - POST multipart Excel file
  - Verify 200 response and data in database

- **test_upload_replaces_existing_data**
  - Insert initial data
  - Upload new data
  - Verify old data removed, new data present

- **test_upload_transaction_rollback_on_error**
  - Insert initial data
  - Upload file with validation errors
  - Verify initial data unchanged

- **test_get_all_products**
  - Insert test data
  - GET /api/semi-product
  - Verify all products returned

- **test_update_product_advance_days**
  - Insert test product
  - PUT /api/semi-product/{id}
  - Verify advance_days updated

- **test_download_template**
  - GET /api/semi-product/template
  - Verify Excel file response
  - Parse and verify headers

- **test_permission_checks**
  - Test each endpoint without required permission
  - Verify 403 responses

### SemiProductRepositoryTest
- **test_save_and_find_by_product_code**
  - Save entity
  - Find by product code
  - Verify retrieval

- **test_unique_constraint_on_product_code**
  - Save entity with product_code
  - Attempt to save duplicate product_code
  - Verify constraint violation

## E2E Tests
N/A - API testing covered by integration tests. Frontend E2E in T036.

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11
- Create test Excel files programmatically using Apache POI in test code
- Test data examples:
  - SP001, 半成品A, 7
  - SP002, 半成品B, 14
  - SP003, 半成品C, 3
- Use @Sql scripts to reset database between tests
- Create test users with different permission sets

## Mocking Strategy
- **Unit tests**: Mock repositories, Excel parser for service tests
- **Integration tests**: Real database via Testcontainers, real Excel parsing
- **Security**: Use @WithMockUser for permission testing
- Mock MultipartFile for file upload tests in unit tests
