# T042: Acceptance Criteria

## Functional Acceptance Criteria
1. POST endpoint accepts material purchase ID and triggers ERP order creation
2. Endpoint checks is_erp_triggered=FALSE before processing
3. Duplicate trigger attempts return 409 error
4. Successful trigger sets is_erp_triggered=TRUE
5. Successful trigger stores erp_order_no from ERP response
6. ERP service stub is called with correct parameters
7. Transaction rolls back if ERP call fails
8. Permission check enforced (material_purchase.trigger_erp)
9. Non-existent ID returns 404 error
10. Updated entity returned in response

## API Contracts

### POST /api/material-purchase/{id}/trigger-erp
**Request:**
- Path Parameter: id (integer)
- Permission: material_purchase.trigger_erp
- Body: None

**Response 200 (Success):**
```json
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
  "isErpTriggered": true,
  "erpOrderNo": "ERP-2026-0001",
  "createdAt": "2026-02-15T10:00:00Z",
  "updatedAt": "2026-02-15T15:30:00Z"
}
```

**Response 404 (Not Found):**
```json
{
  "error": "Not Found",
  "message": "Material purchase with ID 999 not found"
}
```

**Response 409 (Already Triggered):**
```json
{
  "error": "Conflict",
  "message": "ERP order already triggered for this material purchase. Order number: ERP-2026-0001"
}
```

**Response 500 (ERP Service Failure):**
```json
{
  "error": "Internal Server Error",
  "message": "Failed to create ERP order: Connection timeout"
}
```

**Response 403 (Permission Denied):**
```json
{
  "error": "Forbidden",
  "message": "Insufficient permissions"
}
```

## ERP Service Stub Behavior
### ErpPurchaseService.createOrder(request)
**Input (ErpOrderRequest):**
```json
{
  "itm": "P001",
  "prdNo": "SP001",
  "qty": 1000.00,
  "demandDate": "2026-02-27"
}
```

**Output (ErpOrderResponse):**
```json
{
  "orderNo": "ERP-2026-0001",
  "status": "SUCCESS"
}
```

**Stub Behavior:**
- Generate order number: "ERP-YYYY-" + 4-digit sequence
- Always return SUCCESS (for testing, can simulate failure with specific inputs)
- Log request parameters
- Simulate 200-500ms delay

## UI Acceptance Criteria
N/A - Backend only (UI in T043)

## Non-Functional Criteria
- Transaction atomicity: If ERP call fails, is_erp_triggered remains FALSE
- Thread-safe: Concurrent trigger attempts handled correctly
- Idempotent: Repeated calls for same ID return 409 (already triggered)
- Response time under 2 seconds (including ERP stub delay)
- Proper error logging for ERP failures
- ERP stub clearly documented as temporary

## How to Verify
1. Start Spring Boot application
2. Insert test data in material_purchase table with is_erp_triggered=FALSE
3. Authenticate with user having material_purchase.trigger_erp permission
4. POST /api/material-purchase/1/trigger-erp
5. Verify 200 response with isErpTriggered: true
6. Verify erpOrderNo set (e.g., "ERP-2026-0001")
7. Verify updated_at timestamp changed
8. Query database: SELECT * FROM material_purchase WHERE id = 1
9. Verify is_erp_triggered = TRUE (1) in database
10. Verify erp_order_no stored in database
11. POST /api/material-purchase/1/trigger-erp again (duplicate)
12. Verify 409 response with conflict message
13. Verify is_erp_triggered still TRUE (no change)
14. POST /api/material-purchase/999/trigger-erp (non-existent)
15. Verify 404 response
16. Mock ErpPurchaseService to throw exception
17. POST /api/material-purchase/2/trigger-erp
18. Verify 500 response
19. Verify is_erp_triggered still FALSE (transaction rolled back)
20. Test with user lacking material_purchase.trigger_erp permission
21. Verify 403 response
22. Verify ErpPurchaseService.createOrder called with correct parameters (check logs)
