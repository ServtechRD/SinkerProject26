# T028: Acceptance Criteria

## Functional Acceptance Criteria

### GET Production Plan
- [ ] GET /api/production-plan?year=2026 returns all plans for 2026
- [ ] Response includes all channels for each product
- [ ] Results ordered by product_code, then channel
- [ ] Empty array returned if no data for year
- [ ] monthly_allocation JSON properly deserialized to object/map
- [ ] Calculated fields (total_quantity, difference) included in response
- [ ] Returns 400 if year parameter missing
- [ ] Returns 400 if year parameter invalid

### PUT Production Plan
- [ ] PUT /api/production-plan/123 updates record successfully
- [ ] monthly_allocation updated with new values
- [ ] buffer_quantity updated
- [ ] remarks updated (can be set to null/empty)
- [ ] total_quantity recalculated automatically
- [ ] difference recalculated automatically
- [ ] updated_at timestamp updated
- [ ] Returns updated record in response
- [ ] Returns 404 if ID not found
- [ ] Returns 400 if validation fails

### Calculation Logic
- [ ] total_quantity = sum(all monthly values) + buffer_quantity
- [ ] Missing months in JSON treated as 0
- [ ] difference = total_quantity - original_forecast
- [ ] Calculations preserve 2 decimal precision
- [ ] Negative differences allowed (over-forecast scenario)

### Validation
- [ ] monthlyAllocation keys must be "2" through "12" only
- [ ] monthlyAllocation keys "1" or "13+" rejected
- [ ] monthlyAllocation values must be valid decimals
- [ ] monthlyAllocation values > 9999999999.99 rejected
- [ ] monthlyAllocation values with >2 decimal places rejected
- [ ] Negative monthly values rejected
- [ ] bufferQuantity must be >= 0
- [ ] remarks longer than 65535 chars rejected

### Permission Control
- [ ] Request without authentication returns 401
- [ ] GET without production_plan.view permission returns 403
- [ ] PUT without production_plan.edit permission returns 403
- [ ] Requests with appropriate permissions return 200

## API Contract

### GET Request
```
GET /api/production-plan?year=2026
Authorization: Bearer {jwt_token}
```

### GET Response (200 OK)
```json
[
  {
    "id": 1,
    "year": 2026,
    "productCode": "PROD001",
    "productName": "Product 1",
    "category": "Category A",
    "spec": "Spec A",
    "warehouseLocation": "WH-A",
    "channel": "CH-DIRECT",
    "monthlyAllocation": {
      "2": 100.00,
      "3": 150.00,
      "4": 200.00,
      "5": 180.00,
      "6": 220.00,
      "7": 250.00,
      "8": 230.00,
      "9": 210.00,
      "10": 240.00,
      "11": 260.00,
      "12": 280.00
    },
    "bufferQuantity": 50.00,
    "totalQuantity": 2370.00,
    "originalForecast": 2300.00,
    "difference": 70.00,
    "remarks": "Adjusted based on Q1 performance",
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": "2026-01-20T14:30:00"
  },
  {
    "id": 2,
    "year": 2026,
    "productCode": "PROD001",
    "productName": "Product 1",
    "category": "Category A",
    "spec": "Spec A",
    "warehouseLocation": "WH-A",
    "channel": "CH-RETAIL",
    "monthlyAllocation": {
      "2": 50.00,
      "3": 75.00,
      "12": 100.00
    },
    "bufferQuantity": 25.00,
    "totalQuantity": 250.00,
    "originalForecast": 300.00,
    "difference": -50.00,
    "remarks": null,
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": "2026-01-15T10:00:00"
  }
]
```

### PUT Request
```
PUT /api/production-plan/1
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "monthlyAllocation": {
    "2": 110.00,
    "3": 160.00,
    "4": 210.00,
    "5": 190.00,
    "6": 230.00,
    "7": 260.00,
    "8": 240.00,
    "9": 220.00,
    "10": 250.00,
    "11": 270.00,
    "12": 290.00
  },
  "bufferQuantity": 60.00,
  "remarks": "Updated for revised demand forecast"
}
```

### PUT Response (200 OK)
```json
{
  "id": 1,
  "year": 2026,
  "productCode": "PROD001",
  "productName": "Product 1",
  "category": "Category A",
  "spec": "Spec A",
  "warehouseLocation": "WH-A",
  "channel": "CH-DIRECT",
  "monthlyAllocation": {
    "2": 110.00,
    "3": 160.00,
    "4": 210.00,
    "5": 190.00,
    "6": 230.00,
    "7": 260.00,
    "8": 240.00,
    "9": 220.00,
    "10": 250.00,
    "11": 270.00,
    "12": 290.00
  },
  "bufferQuantity": 60.00,
  "totalQuantity": 2490.00,
  "originalForecast": 2300.00,
  "difference": 190.00,
  "remarks": "Updated for revised demand forecast",
  "createdAt": "2026-01-15T10:00:00",
  "updatedAt": "2026-01-22T16:45:00"
}
```

### Error Responses
```json
// 400 Bad Request - Missing year
{
  "error": "Bad Request",
  "message": "year parameter is required",
  "status": 400
}

// 400 Bad Request - Invalid monthly allocation
{
  "error": "Bad Request",
  "message": "monthlyAllocation contains invalid month key: 13. Valid keys are 2-12",
  "status": 400
}

// 404 Not Found
{
  "error": "Not Found",
  "message": "Production plan with ID 123 not found",
  "status": 404
}

// 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "Authentication required",
  "status": 401
}

// 403 Forbidden
{
  "error": "Forbidden",
  "message": "Insufficient permissions: production_plan.edit required",
  "status": 403
}
```

## Non-Functional Criteria
- [ ] GET response time < 1 second for 1000 records
- [ ] PUT response time < 500ms
- [ ] Calculations accurate to 2 decimal places
- [ ] Concurrent updates handled safely (optimistic locking optional)
- [ ] Proper error logging for all failures
- [ ] Input sanitization prevents SQL injection
- [ ] JSON parsing errors handled gracefully

## How to Verify

### 1. Setup Test Data
```sql
INSERT INTO production_plan
(year, product_code, product_name, category, spec, warehouse_location, channel,
 monthly_allocation, buffer_quantity, total_quantity, original_forecast, difference)
VALUES
(2026, 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A', 'CH-DIRECT',
 '{"2": 100, "3": 150, "4": 200}', 50.00, 500.00, 480.00, 20.00),
(2026, 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A', 'CH-RETAIL',
 '{"2": 50, "3": 75}', 25.00, 150.00, 200.00, -50.00);
```

### 2. Test GET Endpoint
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/production-plan?year=2026"
```
Expected: 200 OK with array of 2 records

### 3. Test Missing Year Parameter
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/production-plan"
```
Expected: 400 Bad Request

### 4. Test PUT Endpoint
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "monthlyAllocation": {"2": 120, "3": 180, "4": 220},
    "bufferQuantity": 60.00,
    "remarks": "Updated plan"
  }' \
  http://localhost:8080/api/production-plan/1
```
Expected: 200 OK with updated record

### 5. Verify Calculation
```sql
SELECT total_quantity, buffer_quantity, difference, original_forecast
FROM production_plan WHERE id = 1;
-- total_quantity should be 120 + 180 + 220 + 60 = 580.00
-- difference should be 580.00 - 480.00 = 100.00
```

### 6. Test Invalid Month Key
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "monthlyAllocation": {"1": 100, "2": 150},
    "bufferQuantity": 50.00
  }' \
  http://localhost:8080/api/production-plan/1
```
Expected: 400 Bad Request (month "1" not allowed)

### 7. Test Negative Buffer
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "monthlyAllocation": {"2": 100},
    "bufferQuantity": -10.00
  }' \
  http://localhost:8080/api/production-plan/1
```
Expected: 400 Bad Request

### 8. Test Permission Denial
```bash
curl -H "Authorization: Bearer $TOKEN_NO_EDIT" \
  -X PUT \
  -H "Content-Type: application/json" \
  -d '{"bufferQuantity": 100}' \
  http://localhost:8080/api/production-plan/1
```
Expected: 403 Forbidden

### 9. Test Partial Monthly Allocation
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "monthlyAllocation": {"2": 100, "12": 200},
    "bufferQuantity": 50.00
  }' \
  http://localhost:8080/api/production-plan/1
```
Expected: 200 OK, total_quantity = 100 + 200 + 50 = 350.00

### 10. Test Empty Monthly Allocation
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "monthlyAllocation": {},
    "bufferQuantity": 100.00
  }' \
  http://localhost:8080/api/production-plan/1
```
Expected: 200 OK, total_quantity = 100.00
