# T021: Test Plan

## Unit Tests

### ExcelExportServiceTest

1. **testGenerateIntegrationExcel_Success**
   - Pass sample integration data (5 products)
   - Call generateIntegrationExcel(data, month)
   - Assert returns ByteArrayOutputStream
   - Parse output as Excel workbook
   - Assert workbook has 1 sheet
   - Assert sheet name = "銷售預估量整合 - {month}"

2. **testExcelHeaders**
   - Generate Excel
   - Parse header row
   - Assert 20 columns with correct Chinese names in order

3. **testExcelDataRows**
   - Pass 3 products
   - Generate Excel
   - Parse data rows
   - Assert 3 data rows (excluding header)
   - Verify each row has correct product data

4. **testHeaderStyling**
   - Generate Excel
   - Parse header row cells
   - Assert bold font
   - Assert centered alignment
   - Assert background color set (not white)

5. **testNumberFormatting**
   - Pass product with quantity 1234.56
   - Generate Excel
   - Parse quantity cell
   - Assert cell type is NUMERIC
   - Assert formatted value displays "1,234.56"

6. **testRightAlignment_Numbers**
   - Generate Excel
   - Parse quantity column cells
   - Assert alignment is RIGHT

7. **testLeftAlignment_Text**
   - Generate Excel
   - Parse text column cells (category, product_name)
   - Assert alignment is LEFT or GENERAL

8. **testFrozenPane**
   - Generate Excel
   - Get sheet pane information
   - Assert row freeze at row 1 (header frozen)

9. **testBorders**
   - Generate Excel
   - Parse cells
   - Assert border style applied to all sides

10. **testAutoSizeColumns**
    - Generate Excel with varied content lengths
    - Assert column widths > 0
    - Assert columns wide enough for content

11. **testEmptyData**
    - Pass empty list
    - Generate Excel
    - Assert header row exists
    - Assert no data rows

12. **testZeroValues**
    - Pass product with qty_carrefour = 0
    - Generate Excel
    - Assert cell shows "0" or "0.00" (not empty)

13. **testAll12ChannelColumns**
    - Generate Excel
    - Assert columns for all 12 channels present
    - Verify column order matches specification

## Integration Tests

### ForecastIntegrationControllerIntegrationTest (Spring Boot Test + Testcontainers)

1. **testExport_Success**
   - Insert test data for 3 products
   - GET /api/sales-forecast/integration/export?month=202601
   - Assert 200 OK
   - Assert Content-Type is Excel MIME type
   - Assert Content-Disposition header contains filename
   - Parse response body as Excel
   - Assert 3 data rows

2. **testExport_SpecificVersion**
   - Insert 2 versions
   - GET export with version parameter
   - Parse Excel
   - Verify data matches specified version

3. **testExport_LatestVersion**
   - Insert 3 versions
   - GET export without version parameter
   - Verify exported data is from latest version

4. **testExport_Unauthorized**
   - GET without JWT
   - Assert 401 Unauthorized

5. **testExport_NoPermission**
   - GET with JWT missing sales_forecast.view
   - Assert 403 Forbidden

6. **testExport_MissingMonth**
   - GET /api/sales-forecast/integration/export (no params)
   - Assert 400 Bad Request

7. **testExport_EmptyData**
   - GET export for month with no data
   - Assert 200 OK
   - Parse Excel
   - Assert only header row exists

8. **testExport_DataAccuracy**
   - Insert known data: P001 in 3 channels (100, 80, 120)
   - Export
   - Parse Excel
   - Verify P001 row shows: qty_px=100, qty_carrefour=80, qty_711=120
   - Verify subtotal=300

9. **testExport_Filename**
   - Export with month=202601
   - Extract Content-Disposition header
   - Assert filename contains "202601"
   - Assert filename ends with ".xlsx"

10. **testExport_SheetName**
    - Export month=202601
    - Parse Excel
    - Assert sheet name = "銷售預估量整合 - 202601"

11. **testExport_AllChannels**
    - Insert product in all 12 channels
    - Export
    - Parse Excel
    - Verify all 12 channel columns have values

12. **testExport_HeadersCorrect**
    - Export
    - Parse header row
    - Assert columns: 庫位, 中類名稱, 貨品規格, 品名, 品號, [12 channels], 原始小計, 差異, 備註

13. **testExport_Performance_LargeDataset**
    - Insert 500 products
    - Measure export execution time
    - Assert < 5 seconds

14. **testExport_FrozenHeader**
    - Export
    - Parse sheet
    - Assert pane freeze at row 1

15. **testExport_NumberFormatting**
    - Insert product with quantity 1234.567
    - Export
    - Parse cell
    - Assert cell displays "1,234.57" (rounded to 2 decimals)

16. **testExport_Sorting**
    - Insert products with different categories: 03, 01, 02
    - Export
    - Parse Excel rows
    - Assert sorted: 01, 02, 03 (same as integration query)

## E2E Tests
N/A - Backend export is thoroughly tested via integration tests. E2E tests will verify frontend download button in T022.

## Test Data Setup
- Use Testcontainers MariaDB 10.11
- Reuse test data from T020 integration tests
- Sample products across all 12 channels
- Multiple versions for version-specific export tests
- Products with various quantity values: 0, small decimals, large numbers

## Mocking Strategy

### Unit Tests
- Mock ForecastIntegrationService to return sample integration data
- Do not mock Apache POI classes - use real XSSFWorkbook
- Test actual Excel generation
- Parse generated Excel to verify structure

### Integration Tests
- No mocking of Spring components
- Use real database via Testcontainers
- Use real ForecastIntegrationService
- Generate real Excel files
- Parse real Excel output for verification

### Excel Verification Helper
Create helper methods for test assertions:
```java
private void assertHeaderStyle(Cell cell) {
    CellStyle style = cell.getCellStyle();
    assertNotNull(style);
    assertTrue(style.getFont().getBold());
    assertEquals(HorizontalAlignment.CENTER, style.getAlignment());
    assertNotEquals(IndexedColors.WHITE.getIndex(), style.getFillForegroundColor());
}

private void assertNumberFormat(Cell cell) {
    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertTrue(cell.getCellStyle().getDataFormatString().contains("#,##0.00"));
}
```

## Performance Testing
- Load test with 1000 products
- Measure:
  - Query execution time
  - Excel generation time
  - Total response time
  - Memory usage
- Optimize if any operation exceeds thresholds
- Consider SXSSF (Streaming) for very large exports if needed
