# T016: Acceptance Criteria

## Functional Acceptance Criteria

### Create Forecast Item
- [ ] POST /api/sales-forecast creates new item with all fields
- [ ] Requires sales_forecast.create permission
- [ ] User must own the specified channel
- [ ] Month must be open (is_closed=FALSE)
- [ ] product_code validated via ErpProductService
- [ ] Duplicate check: same month+channel+product_code returns 409 Conflict
- [ ] Created item has is_modified=TRUE
- [ ] Version generated: "YYYY/MM/DD HH:MM:SS(通路名稱)"
- [ ] Quantity must be positive, else 400 Bad Request
- [ ] Returns 201 Created with item data including ID
- [ ] Invalid product_code returns 400 Bad Request

### Update Forecast Item
- [ ] PUT /api/sales-forecast/:id updates quantity only
- [ ] Requires sales_forecast.edit permission
- [ ] User must own the channel of the item
- [ ] Month must be open
- [ ] Updated item has is_modified=TRUE
- [ ] New version generated with current timestamp
- [ ] Returns 200 OK with updated item
- [ ] Returns 404 Not Found if item doesn't exist
- [ ] Quantity must be positive, else 400 Bad Request
- [ ] Attempting to change other fields ignored or rejected

### Delete Forecast Item
- [ ] DELETE /api/sales-forecast/:id removes item from database
- [ ] Requires sales_forecast.delete permission
- [ ] User must own the channel of the item
- [ ] Month must be open
- [ ] Returns 204 No Content on success
- [ ] Returns 404 Not Found if item doesn't exist
- [ ] Hard delete (record actually removed from database)

### Common Validations
- [ ] Closed month returns 403 Forbidden for all operations
- [ ] User without channel ownership returns 403 Forbidden
- [ ] Missing required fields return 400 Bad Request
- [ ] All operations logged with user, action, timestamp

## API Contracts

### POST /api/sales-forecast
**Request:**
```json
{
  "month": "202601",
  "channel": "大全聯",
  "category": "飲料類",
  "spec": "600ml*24入",
  "product_code": "P001",
  "product_name": "可口可樂",
  "warehouse_location": "A01",
  "quantity": 100.50
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "month": "202601",
  "channel": "大全聯",
  "category": "飲料類",
  "spec": "600ml*24入",
  "product_code": "P001",
  "product_name": "可口可樂",
  "warehouse_location": "A01",
  "quantity": 100.50,
  "version": "2026/01/15 14:30:00(大全聯)",
  "is_modified": true,
  "created_at": "2026-01-15T14:30:00",
  "updated_at": "2026-01-15T14:30:00"
}
```

**Error Response (409 Conflict):**
```json
{
  "error": "Duplicate entry",
  "message": "Product P001 already exists for month 202601 and channel 大全聯"
}
```

### PUT /api/sales-forecast/:id
**Request:**
```json
{
  "quantity": 150.75
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "month": "202601",
  "channel": "大全聯",
  "category": "飲料類",
  "spec": "600ml*24入",
  "product_code": "P001",
  "product_name": "可口可樂",
  "warehouse_location": "A01",
  "quantity": 150.75,
  "version": "2026/01/15 15:00:00(大全聯)",
  "is_modified": true,
  "created_at": "2026-01-15T14:30:00",
  "updated_at": "2026-01-15T15:00:00"
}
```

### DELETE /api/sales-forecast/:id
**Response (204 No Content)**

## Non-Functional Criteria
- [ ] Create operation completes within 500ms
- [ ] Update operation completes within 500ms
- [ ] Delete operation completes within 500ms
- [ ] All operations are transactional
- [ ] Detailed audit logging for compliance
- [ ] Proper HTTP status codes used
- [ ] Error messages are clear and actionable

## How to Verify

### Manual Testing
1. **Create Item:**
   - Login with sales_forecast.create permission for channel 大全聯
   - Ensure month 202601 is open
   - POST /api/sales-forecast with valid data
   - Verify 201 Created response
   - Query database: `SELECT * FROM sales_forecast WHERE product_code='P001'`
   - Verify is_modified=TRUE, version populated

2. **Duplicate Detection:**
   - Create item with product_code=P001, month=202601, channel=大全聯
   - Attempt to create same product again
   - Verify 409 Conflict response

3. **Update Item:**
   - Create item with quantity=100
   - PUT /api/sales-forecast/:id with quantity=150
   - Verify quantity updated, new version generated
   - Verify is_modified=TRUE

4. **Delete Item:**
   - Create item
   - DELETE /api/sales-forecast/:id
   - Verify 204 No Content
   - Query database and verify item deleted

5. **Month Closed:**
   - Close month 202601
   - Attempt create/update/delete
   - Verify 403 Forbidden for all

6. **Invalid Product:**
   - Mock ErpProductService to return false for P999
   - POST with product_code=P999
   - Verify 400 Bad Request

7. **Permission Tests:**
   - Attempt operations without required permissions
   - Verify 403 Forbidden
   - Attempt operations on channel user doesn't own
   - Verify 403 Forbidden

8. **Not Found:**
   - PUT /api/sales-forecast/99999
   - Verify 404 Not Found
   - DELETE /api/sales-forecast/99999
   - Verify 404 Not Found

### Automated Testing
- Run integration tests with Testcontainers
- Run unit tests for service layer
- Verify all test cases pass
