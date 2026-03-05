# T041: Acceptance Criteria

## Functional Acceptance Criteria
1. GET endpoint returns material purchase data filtered by week_start and factory
2. Query parameters week_start and factory are required
3. Results are ordered by product_code ascending
4. Empty result set returns empty array (not error)
5. Invalid date format returns 400 error
6. Missing required parameters returns 400 error
7. Permission check enforced (material_purchase.view)
8. Response includes all material purchase fields including calculations
9. Decimal values returned with 2 decimal precision
10. Boolean is_erp_triggered returned correctly
11. BomService stub provides sample BOM data

## API Contracts

### GET /api/material-purchase
**Request:**
- Query Parameters:
  - week_start (required): ISO date string (YYYY-MM-DD)
  - factory (required): string
- Permission: material_purchase.view

**Response 200:**
```json
[
  {
    "id": 1,
    "weekStart": "2026-02-17",
    "factory": "F1",
    "productCode": "P001",
    "productName": "產品A",
    "quantity": 1000.00,
    "semiProductName": "半成品A",
    "semiProductCode": "SP001",
    "kgPerBox": 5.50,
    "basketQuantity": 5500.00,
    "boxesPerBarrel": 20.00,
    "requiredBarrels": 275.00,
    "isErpTriggered": false,
    "erpOrderNo": null,
    "createdAt": "2026-02-15T10:00:00Z",
    "updatedAt": "2026-02-15T10:00:00Z"
  },
  {
    "id": 2,
    "weekStart": "2026-02-17",
    "factory": "F1",
    "productCode": "P002",
    "productName": "產品B",
    "quantity": 500.00,
    "semiProductName": "半成品B",
    "semiProductCode": "SP002",
    "kgPerBox": 3.00,
    "basketQuantity": 1500.00,
    "boxesPerBarrel": 15.00,
    "requiredBarrels": 100.00,
    "isErpTriggered": true,
    "erpOrderNo": "ERP-2026-001",
    "createdAt": "2026-02-15T10:00:00Z",
    "updatedAt": "2026-02-15T12:30:00Z"
  }
]
```

**Response 400 (Missing Parameter):**
```json
{
  "error": "Bad Request",
  "message": "Required parameter 'week_start' is missing"
}
```

**Response 400 (Invalid Date):**
```json
{
  "error": "Bad Request",
  "message": "Invalid date format for 'week_start'. Expected YYYY-MM-DD"
}
```

**Response 403:**
```json
{
  "error": "Forbidden",
  "message": "Insufficient permissions"
}
```

**Response 200 (No Data):**
```json
[]
```

## BOM Service Stub Behavior
- BomService.getKgPerBox(productCode): Returns hardcoded values (e.g., "P001" → 5.50, default → 1.00)
- BomService.getBoxesPerBarrel(productCode): Returns hardcoded values (e.g., "P001" → 20.00, default → 10.00)
- Stub clearly documented as temporary solution pending BOM integration

## UI Acceptance Criteria
N/A - Backend only (UI in T043)

## Non-Functional Criteria
- Query response time under 500ms for typical dataset (1000 rows per week/factory)
- Query response time under 2 seconds for large dataset (10,000 rows)
- Proper use of database indexes (week_start, factory composite index)
- Decimal precision maintained in calculations (no rounding errors)
- Thread-safe for concurrent requests
- Proper transaction management (read-only)

## How to Verify
1. Start Spring Boot application
2. Populate test data in material_purchase table
3. Authenticate with user having material_purchase.view permission
4. GET /api/material-purchase?week_start=2026-02-17&factory=F1
5. Verify 200 response with array of material purchase objects
6. Verify results filtered by week_start and factory
7. Verify results ordered by product_code
8. Verify decimal values have 2 decimal places
9. Verify is_erp_triggered is boolean
10. Verify erp_order_no is null for non-triggered items
11. Verify calculations: basketQuantity = quantity × kgPerBox
12. Verify calculations: requiredBarrels = basketQuantity / boxesPerBarrel
13. GET /api/material-purchase?week_start=2026-02-17 (missing factory)
14. Verify 400 error response
15. GET /api/material-purchase?week_start=invalid&factory=F1
16. Verify 400 error response
17. GET /api/material-purchase?week_start=2026-12-31&factory=F999
18. Verify 200 response with empty array
19. Test with user lacking material_purchase.view permission
20. Verify 403 response
21. Verify BomService stub returns expected values for known product codes
