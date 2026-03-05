# T021: Acceptance Criteria

## Functional Acceptance Criteria

### Export Endpoint
- [ ] GET /api/sales-forecast/integration/export?month=202601 downloads Excel file
- [ ] GET with version parameter exports specific version
- [ ] Without version parameter, exports latest version
- [ ] Requires sales_forecast.view permission
- [ ] Missing month parameter returns 400 Bad Request
- [ ] No data returns empty Excel with headers only

### Excel File Structure
- [ ] File format is .xlsx (Excel 2007+)
- [ ] Sheet name: "銷售預估量整合 - YYYYMM" (e.g., "銷售預估量整合 - 202601")
- [ ] One sheet per file
- [ ] Header row at row 0
- [ ] Data starts at row 1

### Excel Headers
- [ ] Columns in order: 庫位, 中類名稱, 貨品規格, 品名, 品號, PX/大全聯, 家樂福, 愛買, 711, 全家, OK/萊爾富, 好市多, 楓康, 美聯社, 康是美, 電商, 市面經銷, 原始小計, 差異, 備註
- [ ] 20 columns total
- [ ] All 12 channels represented

### Excel Data
- [ ] Each row represents one product
- [ ] Data matches integration query from T020
- [ ] All fields populated correctly
- [ ] Numeric values display as numbers (not text)
- [ ] Zero values displayed as "0" or "0.00"

### Excel Formatting
- [ ] Header row frozen (scrolling down keeps headers visible)
- [ ] Header cells: bold font, centered alignment, background color (light blue/gray)
- [ ] Quantity columns (12 channels + subtotal + difference): right-aligned
- [ ] Quantity columns: number format "#,##0.00" (e.g., 1,234.50)
- [ ] Text columns (庫位, 中類名稱, etc.): left-aligned
- [ ] All cells have thin borders
- [ ] Column widths auto-sized to content (readable without manual adjustment)

### File Download
- [ ] Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- [ ] Content-Disposition: attachment; filename="sales_forecast_integration_202601_{timestamp}.xlsx"
- [ ] Filename includes month
- [ ] Browser triggers download (not opens inline)

### Permission and Error Handling
- [ ] User without sales_forecast.view gets 403 Forbidden
- [ ] Invalid month format returns 400 Bad Request
- [ ] Server errors return 500 with error message
- [ ] Export logged: user, month, version, row count, timestamp

## File Format Validation

### Open Excel File
- [ ] File opens in Microsoft Excel without errors
- [ ] File opens in Google Sheets without errors
- [ ] File opens in LibreOffice Calc without errors

### Visual Inspection
- [ ] Headers clearly visible and styled
- [ ] Numbers formatted with thousand separators and 2 decimals
- [ ] Columns wide enough to show full content
- [ ] No overlapping or truncated text
- [ ] Frozen header works when scrolling

## Non-Functional Criteria
- [ ] Export completes within 5 seconds for 500 products
- [ ] File size reasonable (not unnecessarily large)
- [ ] Handles large datasets (1000+ products)
- [ ] Memory efficient (use streaming for very large exports if needed)
- [ ] Proper cleanup of resources (workbook closed, streams closed)

## How to Verify

### Manual Testing
1. **Basic Export:**
   - Upload forecast data for month 202601
   - GET /api/sales-forecast/integration/export?month=202601
   - Verify file downloads
   - Open file in Excel
   - Verify sheet name: "銷售預估量整合 - 202601"
   - Verify headers present and styled

2. **Data Accuracy:**
   - Compare exported data with integration query API response
   - Verify all products present
   - Verify all channel quantities match
   - Verify subtotals and differences match

3. **Formatting:**
   - Open exported file
   - Verify header row frozen (scroll down, headers stay)
   - Verify headers bold and centered with background color
   - Verify quantity columns right-aligned with format "1,234.50"
   - Verify column widths appropriate

4. **All 12 Channels:**
   - Upload data to all 12 channels
   - Export
   - Verify all 12 channel columns present with data

5. **Version Export:**
   - Upload multiple versions
   - Export specific version: GET ...?month=202601&version=...
   - Verify exported data matches that version

6. **Empty Export:**
   - Query month with no data
   - Export
   - Verify file contains headers but no data rows

7. **Permission Test:**
   - Login without sales_forecast.view
   - Attempt export
   - Verify 403 Forbidden

8. **Large Dataset:**
   - Create 1000 products
   - Export
   - Verify file generates successfully
   - Verify opens without issues in Excel

9. **File Download:**
   - Trigger export
   - Verify browser downloads file automatically
   - Verify filename format correct

10. **Cross-Platform:**
    - Open exported file in:
      - Microsoft Excel (Windows/Mac)
      - Google Sheets
      - LibreOffice Calc
    - Verify displays correctly in all

### Automated Testing
- Integration test: generate Excel and verify structure
- Parse generated Excel and verify data matches input
- Verify cell styles programmatically
- Performance test with large datasets
