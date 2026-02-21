# T038: Acceptance Criteria

## Functional Acceptance Criteria
1. GET endpoint returns material demand data filtered by week_start and factory
2. Query parameters week_start and factory are required
3. Results are ordered by material_code ascending
4. Empty result set returns empty array (not error)
5. Invalid date format returns 400 error
6. Missing required parameters returns 400 error
7. Permission check enforced (material_demand.view)
8. Response includes all material demand fields
9. Decimal values returned with 2 decimal precision
10. Dates returned in ISO-8601 format (YYYY-MM-DD)

## API Contracts

### GET /api/material-demand
**Request:**
- Query Parameters:
  - week_start (required): ISO date string (YYYY-MM-DD)
  - factory (required): string
- Permission: material_demand.view

**Response 200:**
```json
[
  {
    "id": 1,
    "weekStart": "2026-02-17",
    "factory": "F1",
    "materialCode": "M001",
    "materialName": "原料A",
    "unit": "kg",
    "lastPurchaseDate": "2026-02-10",
    "demandDate": "2026-02-20",
    "expectedDelivery": 100.50,
    "demandQuantity": 500.00,
    "estimatedInventory": 50.25,
    "createdAt": "2026-02-15T10:00:00Z",
    "updatedAt": "2026-02-15T10:00:00Z"
  },
  {
    "id": 2,
    "weekStart": "2026-02-17",
    "factory": "F1",
    "materialCode": "M002",
    "materialName": "原料B",
    "unit": "pcs",
    "lastPurchaseDate": null,
    "demandDate": "2026-02-22",
    "expectedDelivery": 0.00,
    "demandQuantity": 1000.00,
    "estimatedInventory": 0.00,
    "createdAt": "2026-02-15T10:00:00Z",
    "updatedAt": "2026-02-15T10:00:00Z"
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

## UI Acceptance Criteria
N/A - Backend only (UI in T039)

## Non-Functional Criteria
- Query response time under 500ms for typical dataset (1000 rows per week/factory)
- Query response time under 2 seconds for large dataset (10,000 rows)
- Proper use of database indexes (week_start, factory composite index)
- Decimal precision maintained (no rounding errors)
- Thread-safe for concurrent requests
- Proper transaction management (read-only)

## How to Verify
1. Start Spring Boot application
2. Populate test data in material_demand table (via T032 or manual insert)
3. Authenticate with user having material_demand.view permission
4. GET /api/material-demand?week_start=2026-02-17&factory=F1
5. Verify 200 response with array of material demand objects
6. Verify results filtered by week_start and factory
7. Verify results ordered by material_code
8. Verify decimal values have 2 decimal places
9. Verify dates in ISO-8601 format
10. GET /api/material-demand?week_start=2026-02-17 (missing factory)
11. Verify 400 error response
12. GET /api/material-demand?week_start=invalid&factory=F1
13. Verify 400 error response
14. GET /api/material-demand?week_start=2026-12-31&factory=F999
15. Verify 200 response with empty array
16. Test with user lacking material_demand.view permission
17. Verify 403 response
18. Verify database query uses composite index (EXPLAIN query)
