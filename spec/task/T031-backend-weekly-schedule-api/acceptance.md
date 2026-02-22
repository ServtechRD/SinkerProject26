# T031: Acceptance Criteria

## Functional Acceptance Criteria

### Excel Upload
- [ ] POST /api/weekly-schedule/upload accepts multipart/form-data with Excel file
- [ ] Upload with valid Monday week_start succeeds
- [ ] Upload with Tuesday week_start returns 400
- [ ] Excel with all required columns parses successfully
- [ ] Excel missing required columns returns 400 with clear error message
- [ ] Chinese column headers (需求日期, 品號, 品名, 庫位, 箱數小計) recognized
- [ ] Existing data for week+factory deleted before insert
- [ ] All rows inserted in single transaction
- [ ] Transaction rolls back on any parse error
- [ ] Returns count of inserted records
- [ ] Both .xlsx and .xls formats supported
- [ ] Empty rows in Excel skipped

### Query Schedule
- [ ] GET /api/weekly-schedule?week_start=2026-02-02&factory=FACTORY-A returns matching records
- [ ] Results ordered by demand_date, then product_code
- [ ] Empty array returned if no data exists
- [ ] Returns 400 if week_start parameter missing
- [ ] Returns 400 if factory parameter missing

### Edit Schedule
- [ ] PUT /api/weekly-schedule/123 updates demand_date and quantity
- [ ] Can update demand_date only
- [ ] Can update quantity only
- [ ] Can update both fields
- [ ] Returns 404 if ID not found
- [ ] Returns 400 if quantity negative
- [ ] Returns 400 if demand_date invalid format
- [ ] updated_at timestamp updated

### Permission Control
- [ ] Upload without weekly_schedule.upload permission returns 403
- [ ] GET without weekly_schedule.view permission returns 403
- [ ] PUT without weekly_schedule.edit permission returns 403
- [ ] All endpoints require authentication (401 without JWT)

## API Contract

### Upload Request
```
POST /api/weekly-schedule/upload
Authorization: Bearer {jwt_token}
Content-Type: multipart/form-data

file: [Excel binary]
week_start: 2026-02-02
factory: FACTORY-A
```

### Upload Response (200 OK)
```json
{
  "message": "Upload successful",
  "recordsInserted": 25,
  "weekStart": "2026-02-02",
  "factory": "FACTORY-A"
}
```

### GET Request
```
GET /api/weekly-schedule?week_start=2026-02-02&factory=FACTORY-A
Authorization: Bearer {jwt_token}
```

### GET Response (200 OK)
```json
[
  {
    "id": 1,
    "weekStart": "2026-02-02",
    "factory": "FACTORY-A",
    "demandDate": "2026-02-03",
    "productCode": "PROD001",
    "productName": "Product 1",
    "warehouseLocation": "WH-A",
    "quantity": 100.50,
    "createdAt": "2026-02-02T10:00:00",
    "updatedAt": "2026-02-02T10:00:00"
  }
]
```

### PUT Request
```
PUT /api/weekly-schedule/1
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "demandDate": "2026-02-04",
  "quantity": 150.75
}
```

### PUT Response (200 OK)
```json
{
  "id": 1,
  "weekStart": "2026-02-02",
  "factory": "FACTORY-A",
  "demandDate": "2026-02-04",
  "productCode": "PROD001",
  "productName": "Product 1",
  "warehouseLocation": "WH-A",
  "quantity": 150.75,
  "createdAt": "2026-02-02T10:00:00",
  "updatedAt": "2026-02-02T15:30:00"
}
```

### Error Responses
```json
// 400 - Not Monday
{
  "error": "Bad Request",
  "message": "week_start must be a Monday. Provided date: 2026-02-03 (Tuesday)",
  "status": 400
}

// 400 - Missing Excel columns
{
  "error": "Bad Request",
  "message": "Excel file missing required columns: 品號, 箱數小計",
  "status": 400
}

// 400 - Invalid Excel format
{
  "error": "Bad Request",
  "message": "Invalid Excel file format or corrupted file",
  "status": 400
}

// 403 - Permission denied
{
  "error": "Forbidden",
  "message": "Insufficient permissions: weekly_schedule.upload required",
  "status": 403
}
```

## Non-Functional Criteria
- [ ] Upload 1000 rows completes in < 10 seconds
- [ ] Query 1000 rows returns in < 500ms
- [ ] Excel file size limit: 10MB
- [ ] Transaction isolation prevents concurrent upload conflicts
- [ ] Proper error logging for all failures
- [ ] Excel parsing errors provide row number context

## How to Verify

### 1. Create Test Excel File
Create Excel with columns: 需求日期, 品號, 品名, 庫位, 箱數小計
```
需求日期      | 品號     | 品名      | 庫位  | 箱數小計
2026-02-03   | PROD001  | Product 1 | WH-A  | 100.50
2026-02-04   | PROD002  | Product 2 | WH-B  | 150.75
```

### 2. Test Upload
```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@schedule.xlsx" \
  -F "week_start=2026-02-02" \
  -F "factory=FACTORY-A" \
  http://localhost:8080/api/weekly-schedule/upload
```
Expected: 200 OK with recordsInserted: 2

### 3. Verify Data in Database
```sql
SELECT * FROM production_weekly_schedule
WHERE week_start = '2026-02-02' AND factory = 'FACTORY-A';
-- Expect 2 rows
```

### 4. Test Upload Replaces Existing
```bash
# Upload again with different data
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@schedule2.xlsx" \
  -F "week_start=2026-02-02" \
  -F "factory=FACTORY-A" \
  http://localhost:8080/api/weekly-schedule/upload
```

```sql
-- Verify old data deleted, new data inserted
SELECT COUNT(*) FROM production_weekly_schedule
WHERE week_start = '2026-02-02' AND factory = 'FACTORY-A';
-- Should match new file row count, not old + new
```

### 5. Test Monday Validation
```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@schedule.xlsx" \
  -F "week_start=2026-02-03" \
  -F "factory=FACTORY-A" \
  http://localhost:8080/api/weekly-schedule/upload
```
Expected: 400 Bad Request (Tuesday)

### 6. Test GET Endpoint
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/weekly-schedule?week_start=2026-02-02&factory=FACTORY-A"
```
Expected: 200 OK with array of schedules

### 7. Test PUT Endpoint
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "demandDate": "2026-02-05",
    "quantity": 200.00
  }' \
  http://localhost:8080/api/weekly-schedule/1
```
Expected: 200 OK with updated record

### 8. Test Permission Denial
```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN_NO_UPLOAD" \
  -F "file=@schedule.xlsx" \
  -F "week_start=2026-02-02" \
  -F "factory=FACTORY-A" \
  http://localhost:8080/api/weekly-schedule/upload
```
Expected: 403 Forbidden
