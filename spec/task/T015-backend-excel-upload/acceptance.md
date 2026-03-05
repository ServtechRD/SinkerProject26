# T015: Acceptance Criteria

## Functional Acceptance Criteria

### Excel Upload
- [ ] POST /api/sales-forecast/upload accepts multipart form with file, month, channel
- [ ] Only .xlsx files accepted (reject .xls, .csv, etc.)
- [ ] File size limit enforced (e.g., 10MB max)
- [ ] Month must exist in sales_forecast_config and be open (is_closed=FALSE)
- [ ] Month format validated (YYYYMM)
- [ ] Channel must be one of 12 valid channels
- [ ] User must have sales_forecast.upload permission
- [ ] User must own the specified channel
- [ ] Excel columns parsed in correct order: 中類名稱, 貨品規格, 品號, 品名, 庫位, 箱數小計
- [ ] Header row skipped during parsing
- [ ] Each product_code validated via ErpProductService (stub returns true)
- [ ] Invalid product_code returns 400 Bad Request with details
- [ ] All existing data for same month+channel deleted before insert
- [ ] All new rows inserted with is_modified=FALSE
- [ ] Version string generated: "YYYY/MM/DD HH:MM:SS(通路名稱)" (e.g., "2026/01/15 14:30:00(大全聯)")
- [ ] Upload is atomic: all rows succeed or all fail (transaction rollback)
- [ ] Response includes: rows_processed, version, upload_timestamp
- [ ] Empty Excel file returns 400 Bad Request
- [ ] Excel with missing required columns returns 400 Bad Request
- [ ] Quantity must be positive decimal, else 400 Bad Request

### Template Download
- [ ] GET /api/sales-forecast/template/:channel returns .xlsx file
- [ ] Template has correct headers: 中類名稱, 貨品規格, 品號, 品名, 庫位, 箱數小計
- [ ] Template includes one sample row with example data
- [ ] Headers are bold and styled
- [ ] First row is frozen
- [ ] File downloads with proper filename: "sales_forecast_template_{channel}.xlsx"
- [ ] No authentication required for template download (or same as upload)

### Validation and Error Handling
- [ ] Closed month returns 403 Forbidden: "Month 202601 is closed"
- [ ] Month not found returns 404 Not Found: "Month 202601 not configured"
- [ ] Invalid channel returns 400 Bad Request: "Invalid channel"
- [ ] User doesn't own channel returns 403 Forbidden: "No permission for channel"
- [ ] Invalid Excel format returns 400 Bad Request: "Invalid file format"
- [ ] Missing required field in row returns 400 Bad Request with row number
- [ ] Negative quantity returns 400 Bad Request with row number
- [ ] ERP validation failure returns 400 Bad Request: "Product P001 not found in ERP"

## API Contracts

### POST /api/sales-forecast/upload
**Request (multipart/form-data):**
```
file: (binary .xlsx file)
month: "202601"
channel: "大全聯"
```

**Response (200 OK):**
```json
{
  "rows_processed": 150,
  "version": "2026/01/15 14:30:00(大全聯)",
  "upload_timestamp": "2026-01-15T14:30:00",
  "month": "202601",
  "channel": "大全聯"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Validation failed",
  "details": [
    "Row 5: Product code P999 not found in ERP",
    "Row 12: Quantity must be positive"
  ]
}
```

### GET /api/sales-forecast/template/:channel
**Request:**
```
GET /api/sales-forecast/template/大全聯
```

**Response (200 OK):**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="sales_forecast_template_大全聯.xlsx"
(binary Excel file)
```

## Excel File Format

### Upload File Expected Format
| 中類名稱 | 貨品規格 | 品號 | 品名 | 庫位 | 箱數小計 |
|---------|---------|------|------|------|---------|
| 飲料類  | 600ml*24入 | P001 | 可口可樂 | A01 | 100.50 |
| 零食類  | 150g*12包 | P002 | 樂事洋芋片 | B02 | 50.00 |

### Template File Format
Same as above with sample row included.

## Non-Functional Criteria
- [ ] Upload processes 1000 rows within 5 seconds
- [ ] Transaction rollback on any error (no partial data)
- [ ] Concurrent uploads to different channels succeed
- [ ] Concurrent uploads to same month+channel handled with locks or queue
- [ ] File upload size limited to prevent memory issues
- [ ] Detailed logging of upload activity (user, channel, month, row count, duration)
- [ ] Template generation takes less than 1 second

## How to Verify

### Manual Testing
1. **Valid Upload:**
   - Login as user with sales_forecast.upload permission for channel 大全聯
   - Create month 202601 and ensure it's open
   - Prepare valid Excel file with 10 rows
   - POST to /api/sales-forecast/upload with file, month=202601, channel=大全聯
   - Verify 200 OK response with rows_processed=10
   - Query database: `SELECT * FROM sales_forecast WHERE month='202601' AND channel='大全聯'`
   - Verify 10 rows exist with correct version and is_modified=FALSE

2. **Replace Existing Data:**
   - Upload 10 rows for 202601 + 大全聯
   - Upload 5 different rows for same month+channel
   - Verify only 5 rows remain (old 10 deleted)

3. **Month Closed:**
   - Close month 202601 (set is_closed=TRUE)
   - Attempt upload
   - Verify 403 Forbidden error

4. **Invalid Channel:**
   - Upload with channel="InvalidChannel"
   - Verify 400 Bad Request

5. **Invalid Product Code:**
   - Update ErpProductService stub to return false for specific product
   - Upload Excel with that product
   - Verify 400 Bad Request with product code in error message

6. **Template Download:**
   - GET /api/sales-forecast/template/家樂福
   - Verify Excel file downloads
   - Open file and verify headers and sample row

7. **Permission Tests:**
   - Login as user without sales_forecast.upload permission
   - Attempt upload
   - Verify 403 Forbidden
   - Login as user who doesn't own the channel
   - Attempt upload
   - Verify 403 Forbidden

### Automated Testing
- Run integration tests with Testcontainers
- Run unit tests for ExcelParserService
- Verify all test cases pass
