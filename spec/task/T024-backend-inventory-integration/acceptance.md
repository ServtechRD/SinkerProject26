# T024: Acceptance Criteria

## Functional Acceptance Criteria

### Real-Time Query Mode
- [ ] GET /api/inventory-integration?month=2026-01 returns aggregated data
- [ ] Forecast quantities sum all 12 channels per product from sales_forecast
- [ ] Inventory balance is fetched from ERP stub for each product
- [ ] Sales quantity is fetched from ERP stub for month date range
- [ ] production_subtotal = forecast_quantity - inventory_balance - sales_quantity
- [ ] All results saved to database with generated version
- [ ] Response includes all fields: id, month, product details, quantities, version, dates, timestamps
- [ ] Multiple calls with same month create different versions
- [ ] start_date and end_date parameters override default month boundaries for sales query

### Version Query Mode
- [ ] GET /api/inventory-integration?month=2026-01&version=v123 returns saved data for that version
- [ ] No ERP calls made in version mode
- [ ] No new records created in version mode
- [ ] Returns empty array if version not found
- [ ] Returns data matching exact version string

### Permission Control
- [ ] Request without authentication returns 401
- [ ] Request without inventory.view permission returns 403
- [ ] Request with inventory.view permission returns 200

### Data Aggregation
- [ ] Products with same code/name/category/spec/warehouse are aggregated
- [ ] All 12 channel values are summed correctly
- [ ] Products with 0 forecast still appear if in sales_forecast table
- [ ] Results sorted by product_code ascending

## API Contract

### Request
```
GET /api/inventory-integration?month=2026-01&start_date=2026-01-01&end_date=2026-01-31&version=v123
Authorization: Bearer {jwt_token}
```

### Response (200 OK)
```json
[
  {
    "id": 1,
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
    "modifiedSubtotal": null,
    "version": "v20260115120000",
    "queryStartDate": "2026-01-01",
    "queryEndDate": "2026-01-31",
    "createdAt": "2026-01-15T12:00:00",
    "updatedAt": "2026-01-15T12:00:00"
  }
]
```

### Error Responses
```json
// 400 Bad Request - Missing month parameter
{
  "error": "Bad Request",
  "message": "month parameter is required",
  "status": 400
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
  "message": "Insufficient permissions: inventory.view required",
  "status": 403
}

// 500 Internal Server Error - ERP failure
{
  "error": "Internal Server Error",
  "message": "Failed to fetch data from ERP system",
  "status": 500
}
```

## Non-Functional Criteria
- [ ] Response time < 2 seconds for real-time query (with stub)
- [ ] Response time < 500ms for version query
- [ ] Handles up to 1000 products in single query
- [ ] Transaction rollback on any save failure
- [ ] Proper error logging for ERP failures
- [ ] Input validation for date formats
- [ ] SQL injection prevention via parameterized queries

## How to Verify

### 1. Setup Test Data
```sql
-- Insert forecast data (assuming T020 structure)
INSERT INTO sales_forecast (month, product_code, product_name, category, spec, warehouse_location, ch1, ch2, ch3, ch4, ch5, ch6, ch7, ch8, ch9, ch10, ch11, ch12)
VALUES ('2026-01', 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A', 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120);
```

### 2. Test Real-Time Query
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/inventory-integration?month=2026-01"
```
Expected: 200 OK with aggregated data, new version created

### 3. Test Version Query
```bash
# First, capture version from step 2 response
VERSION="v20260115120000"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/inventory-integration?month=2026-01&version=$VERSION"
```
Expected: 200 OK with same data, no new version created

### 4. Test Date Range Parameters
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/inventory-integration?month=2026-01&start_date=2026-01-10&end_date=2026-01-20"
```
Expected: 200 OK with sales data for Jan 10-20

### 5. Test Permission Denial
```bash
curl -H "Authorization: Bearer $TOKEN_NO_PERMISSION" \
  "http://localhost:8080/api/inventory-integration?month=2026-01"
```
Expected: 403 Forbidden

### 6. Verify Calculation
```sql
SELECT product_code, forecast_quantity, inventory_balance, sales_quantity, production_subtotal
FROM inventory_sales_forecast
WHERE month = '2026-01';
-- Verify: production_subtotal = forecast_quantity - inventory_balance - sales_quantity
```

### 7. Test Multiple Versions
```bash
# Call twice with same month
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/inventory-integration?month=2026-01"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/inventory-integration?month=2026-01"
```
Verify database has 2 different versions for same month
