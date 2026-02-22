# T025: Acceptance Criteria

## Functional Acceptance Criteria

### Successful Edit
- [ ] PUT /api/inventory-integration/1 with valid modified_subtotal updates the field
- [ ] New record created with new ID and version
- [ ] Original record remains unchanged in database
- [ ] All other fields (product info, quantities, etc.) copied from original
- [ ] created_at and updated_at set to current timestamp on new record
- [ ] Response includes new ID and new version
- [ ] modified_subtotal accepts positive, negative, and zero values
- [ ] modified_subtotal can be set to NULL to clear modification

### Validation
- [ ] Invalid ID returns 404 Not Found
- [ ] Invalid decimal format returns 400 Bad Request
- [ ] Decimal with >10 digits before decimal returns 400
- [ ] Decimal with >2 digits after decimal returns 400
- [ ] Missing request body returns 400

### Permission Control
- [ ] Request without authentication returns 401
- [ ] Request without inventory.edit permission returns 403
- [ ] Request with inventory.edit permission returns 200

### Version Management
- [ ] Each edit generates unique version
- [ ] Version query for original version still returns original data
- [ ] Version query for new version returns modified data
- [ ] Multiple edits of same record create multiple versions

## API Contract

### Request
```
PUT /api/inventory-integration/123
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "modifiedSubtotal": 200.50
}
```

### Response (200 OK)
```json
{
  "id": 456,
  "month": "2026-01",
  "productCode": "PROD001",
  "productName": "Product 1",
  "category": "Category A",
  "spec": "Spec A",
  "warehouseLocation": "WH-A",
  "salesQuantity": 100.00,
  "inventoryBalance": 250.00,
  "forecastQuantity": 500.00,
  "productionSubtotal": 150.00,
  "modifiedSubtotal": 200.50,
  "version": "v20260115130000",
  "queryStartDate": "2026-01-01",
  "queryEndDate": "2026-01-31",
  "createdAt": "2026-01-15T13:00:00",
  "updatedAt": "2026-01-15T13:00:00"
}
```

### Clear Modification (Set to NULL)
```
PUT /api/inventory-integration/123
Content-Type: application/json

{
  "modifiedSubtotal": null
}
```

### Error Responses
```json
// 400 Bad Request - Invalid format
{
  "error": "Bad Request",
  "message": "modifiedSubtotal must be a valid decimal with max 10 digits and 2 decimal places",
  "status": 400
}

// 404 Not Found
{
  "error": "Not Found",
  "message": "Inventory integration record with ID 123 not found",
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
  "message": "Insufficient permissions: inventory.edit required",
  "status": 403
}
```

## Non-Functional Criteria
- [ ] Response time < 500ms
- [ ] Transaction ensures atomicity (new record created or none)
- [ ] Concurrent edits create separate versions without conflict
- [ ] Audit trail preserved (original records never deleted)
- [ ] Proper error logging for failures

## How to Verify

### 1. Setup Test Data
```sql
-- Create initial record via T024 API or insert directly
INSERT INTO inventory_sales_forecast
(month, product_code, product_name, category, spec, warehouse_location,
 sales_quantity, inventory_balance, forecast_quantity, production_subtotal,
 version, query_start_date, query_end_date)
VALUES
('2026-01', 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A',
 100.00, 250.00, 500.00, 150.00,
 'v20260115120000', '2026-01-01', '2026-01-31');
```

### 2. Test Successful Edit
```bash
RECORD_ID=1  # Use actual ID from step 1

curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modifiedSubtotal": 200.50}' \
  http://localhost:8080/api/inventory-integration/$RECORD_ID
```
Expected: 200 OK with new ID and version

### 3. Verify Original Unchanged
```sql
SELECT id, modified_subtotal, version FROM inventory_sales_forecast WHERE id = 1;
-- Expect: modified_subtotal = NULL (or original value), version = v20260115120000
```

### 4. Verify New Record Created
```sql
SELECT id, modified_subtotal, production_subtotal, version
FROM inventory_sales_forecast
ORDER BY id DESC LIMIT 1;
-- Expect: new ID, modified_subtotal = 200.50, production_subtotal = 150.00 (unchanged), new version
```

### 5. Test Set to Zero
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modifiedSubtotal": 0}' \
  http://localhost:8080/api/inventory-integration/$RECORD_ID
```
Verify: modified_subtotal is 0.00, not NULL

### 6. Test Clear Modification
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modifiedSubtotal": null}' \
  http://localhost:8080/api/inventory-integration/$RECORD_ID
```
Verify: modified_subtotal is NULL

### 7. Test Invalid ID
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modifiedSubtotal": 200.50}' \
  http://localhost:8080/api/inventory-integration/99999
```
Expected: 404 Not Found

### 8. Test Permission Denial
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN_NO_EDIT" \
  -H "Content-Type: application/json" \
  -d '{"modifiedSubtotal": 200.50}' \
  http://localhost:8080/api/inventory-integration/$RECORD_ID
```
Expected: 403 Forbidden

### 9. Test Decimal Validation
```bash
# Too many decimal places
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modifiedSubtotal": 200.5555}' \
  http://localhost:8080/api/inventory-integration/$RECORD_ID
```
Expected: 400 Bad Request

### 10. Verify Multiple Edits
```bash
# Edit same original record twice
curl -X PUT ... -d '{"modifiedSubtotal": 100}'
curl -X PUT ... -d '{"modifiedSubtotal": 200}'

# Query database
SELECT id, modified_subtotal, version FROM inventory_sales_forecast
WHERE product_code = 'PROD001' AND month = '2026-01'
ORDER BY created_at;
```
Expected: 3 records (1 original + 2 edits) with different versions
