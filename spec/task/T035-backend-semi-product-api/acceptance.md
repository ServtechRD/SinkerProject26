# T035: Acceptance Criteria

## Functional Acceptance Criteria
1. Excel upload endpoint accepts .xlsx files and processes correctly
2. Upload performs TRUNCATE + re-insert (all existing data replaced)
3. Excel parser correctly reads columns: 品號, 品名, 提前日數
4. List endpoint returns all semi-products in database
5. Edit endpoint updates only advance_days field
6. Template download returns valid Excel file with correct headers
7. All validation rules are enforced
8. Permission checks are applied to all endpoints
9. Transaction rollback occurs if upload validation fails

## API Contracts

### POST /api/semi-product/upload
**Request:**
- Content-Type: multipart/form-data
- Body: file (Excel .xlsx)
- Permission: semi_product.upload

**Response 200:**
```json
{
  "message": "Upload successful",
  "count": 150
}
```

**Response 400:**
```json
{
  "error": "Validation failed",
  "details": ["Row 5: advance_days must be positive", "Row 12: product_code is required"]
}
```

### GET /api/semi-product
**Request:**
- Permission: semi_product.view

**Response 200:**
```json
[
  {
    "id": 1,
    "productCode": "SP001",
    "productName": "半成品A",
    "advanceDays": 7,
    "createdAt": "2026-02-15T10:00:00Z",
    "updatedAt": "2026-02-15T10:00:00Z"
  }
]
```

### PUT /api/semi-product/{id}
**Request:**
- Content-Type: application/json
- Permission: semi_product.edit
- Body:
```json
{
  "advanceDays": 10
}
```

**Response 200:**
```json
{
  "id": 1,
  "productCode": "SP001",
  "productName": "半成品A",
  "advanceDays": 10,
  "createdAt": "2026-02-15T10:00:00Z",
  "updatedAt": "2026-02-15T15:30:00Z"
}
```

**Response 404:**
```json
{
  "error": "Semi-product not found",
  "id": 999
}
```

### GET /api/semi-product/template
**Request:**
- Permission: semi_product.view

**Response 200:**
- Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- Headers: Content-Disposition: attachment; filename="semi_product_template.xlsx"
- Body: Excel file with headers: 品號, 品名, 提前日數

## UI Acceptance Criteria
N/A - Backend only (UI in T036)

## Non-Functional Criteria
- Upload of 1000 rows completes in under 5 seconds
- API responses return in under 500ms for list operations
- Proper transaction management (all-or-nothing for uploads)
- Excel parsing handles up to 10,000 rows
- Concurrent upload requests are handled safely
- Proper error messages in English and/or Chinese as appropriate

## How to Verify
1. Start Spring Boot application
2. Authenticate with user having semi_product.upload permission
3. Create test Excel file with 品號, 品名, 提前日數 headers and 10 rows
4. POST to /api/semi-product/upload with Excel file
5. Verify 200 response with count: 10
6. GET /api/semi-product and verify 10 items returned
7. Upload again with 5 different rows
8. Verify only 5 items exist (previous 10 truncated)
9. PUT /api/semi-product/1 with advanceDays: 15
10. Verify updated_at changed and advanceDays updated
11. Create Excel with invalid data (negative advance_days)
12. Verify 400 error with validation details
13. GET /api/semi-product/template
14. Verify Excel file downloads with correct headers
15. Test with user lacking permissions, verify 403 responses
