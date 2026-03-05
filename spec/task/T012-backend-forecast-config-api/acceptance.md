# T012: Acceptance Criteria

## Functional Acceptance Criteria

### Batch Month Creation
- [ ] POST /api/sales-forecast/config with start_month=202501, end_month=202503 creates 3 records
- [ ] Created months have default values: auto_close_day=10, is_closed=FALSE
- [ ] Duplicate month creation returns 409 Conflict or skips silently
- [ ] Invalid month format (e.g., "20251", "2025-01") returns 400 Bad Request
- [ ] start_month > end_month returns 400 Bad Request
- [ ] Requires sales_forecast_config.edit permission

### List Configurations
- [ ] GET /api/sales-forecast/config returns all months sorted by month DESC
- [ ] Response includes: id, month, auto_close_day, is_closed, closed_at, created_at, updated_at
- [ ] Requires sales_forecast_config.view permission
- [ ] Returns 200 with empty array if no configs exist

### Update Configuration
- [ ] PUT /api/sales-forecast/config/:id can update auto_close_day
- [ ] PUT /api/sales-forecast/config/:id can update is_closed
- [ ] auto_close_day must be between 1-31, else 400 Bad Request
- [ ] When is_closed changes FALSE→TRUE, closed_at is set to current timestamp
- [ ] When is_closed changes TRUE→FALSE, closed_at is set to NULL
- [ ] When is_closed unchanged, closed_at remains unchanged
- [ ] Requires sales_forecast_config.edit permission
- [ ] Returns 404 if config ID not found

### Auto-Close Scheduler
- [ ] Scheduler runs daily at 00:00
- [ ] Finds all configs where is_closed=FALSE and auto_close_day=current_day
- [ ] Sets is_closed=TRUE and closed_at=current_timestamp for matched records
- [ ] Logs number of months auto-closed
- [ ] Does not affect already closed months

## API Contracts

### POST /api/sales-forecast/config
**Request:**
```json
{
  "start_month": "202501",
  "end_month": "202503"
}
```
**Response (201 Created):**
```json
{
  "created_count": 3,
  "months": ["202501", "202502", "202503"]
}
```

### GET /api/sales-forecast/config
**Response (200 OK):**
```json
[
  {
    "id": 1,
    "month": "202503",
    "auto_close_day": 10,
    "is_closed": false,
    "closed_at": null,
    "created_at": "2025-01-15T10:30:00",
    "updated_at": "2025-01-15T10:30:00"
  },
  {
    "id": 2,
    "month": "202502",
    "auto_close_day": 15,
    "is_closed": true,
    "closed_at": "2025-02-15T00:00:00",
    "created_at": "2025-01-15T10:30:00",
    "updated_at": "2025-02-15T00:00:00"
  }
]
```

### PUT /api/sales-forecast/config/:id
**Request:**
```json
{
  "auto_close_day": 20,
  "is_closed": true
}
```
**Response (200 OK):**
```json
{
  "id": 1,
  "month": "202501",
  "auto_close_day": 20,
  "is_closed": true,
  "closed_at": "2025-01-20T14:25:30",
  "created_at": "2025-01-15T10:30:00",
  "updated_at": "2025-01-20T14:25:30"
}
```

## Non-Functional Criteria
- [ ] All endpoints respond within 500ms under normal load
- [ ] Scheduler execution completes within 5 seconds
- [ ] Transaction rollback on any error during batch creation
- [ ] Proper HTTP status codes (200, 201, 400, 401, 403, 404, 409)
- [ ] API follows REST conventions
- [ ] All operations are logged at INFO level
- [ ] Input validation errors return detailed error messages

## How to Verify

### Manual Testing
1. **Batch Creation:**
   - Login with user having sales_forecast_config.edit permission
   - POST to /api/sales-forecast/config with start_month=202601, end_month=202603
   - Verify 3 records created in database
   - Retry same request, verify no duplicates or appropriate error

2. **List Configs:**
   - Login with user having sales_forecast_config.view permission
   - GET /api/sales-forecast/config
   - Verify all months returned, sorted DESC

3. **Update Config:**
   - PUT /api/sales-forecast/config/1 with is_closed=true
   - Verify closed_at is populated
   - PUT same config with is_closed=false
   - Verify closed_at is NULL

4. **Scheduler:**
   - Create config with auto_close_day matching tomorrow's day
   - Wait for scheduler or trigger manually
   - Verify month is auto-closed

5. **Permission Tests:**
   - Attempt API calls without required permissions
   - Verify 403 Forbidden responses

### Automated Testing
- Run integration tests with Testcontainers
- Run unit tests for service layer
- Verify all test cases pass
