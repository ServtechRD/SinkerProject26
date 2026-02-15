# T042: Backend API - ERP Purchase Order Trigger

## Context
This task implements the ERP integration endpoint for triggering purchase order creation in the external ERP system. Users need to send material purchase data to ERP and track which items have been processed. The endpoint prevents duplicate triggers and stores the ERP order number for reference. The actual ERP API integration is stubbed pending ERP system details.

## Goal
Create a REST API endpoint to trigger ERP purchase order creation for material purchase items, with duplicate prevention, ERP API stub integration, and status tracking.

## Scope

### In Scope
- POST /api/material-purchase/:id/trigger-erp - Trigger ERP order creation
- Check is_erp_triggered=FALSE before processing (prevent duplicates)
- ErpPurchaseService stub for ERP API calls
- Update is_erp_triggered=TRUE on success
- Store erp_order_no from ERP response
- Permission check: material_purchase.trigger_erp
- Transaction management (rollback on ERP failure)
- Error handling for duplicate triggers and ERP failures

### Out of Scope
- Real ERP API integration (stubbed for now)
- Batch trigger (trigger multiple items at once)
- ERP order status checking/polling
- Trigger cancellation or reversal
- Frontend implementation (T043)
- Audit logging beyond updated_at timestamp

## Requirements
- **Endpoint**: POST /api/material-purchase/{id}/trigger-erp
- **Permission**: material_purchase.trigger_erp
- **Request Body**: None (ID in path parameter)
- **Response**: Updated MaterialPurchaseDTO with is_erp_triggered=true and erp_order_no set
- **Validation**:
  - Material purchase ID must exist
  - is_erp_triggered must be FALSE (prevent duplicate)
- **ERP Service Stub**:
  - Accept parameters: {Itm, PrdNo, Qty, DemandDate}
  - Return mock order number: ERP-YYYY-NNNN
  - Simulate success (or occasional failure for testing)
- **Business Logic**:
  1. Load MaterialPurchase by ID
  2. Check is_erp_triggered == FALSE, throw error if TRUE
  3. Call ErpPurchaseService.createOrder(...)
  4. On success: set is_erp_triggered=TRUE, store erp_order_no
  5. Save entity
  6. Return DTO
- **Error Handling**:
  - 404 if ID not found
  - 409 if already triggered (is_erp_triggered=TRUE)
  - 500 if ERP service fails
  - 403 if permission denied

## Implementation Notes
- Use @Transactional to ensure atomicity
- ErpPurchaseService stub returns hardcoded order numbers with timestamp
- Map MaterialPurchase fields to ERP request format
- Update MaterialPurchaseController to add trigger endpoint
- Update MaterialPurchaseService with triggerErp method
- Use proper HTTP status codes (200 for success, 409 for conflict, 500 for ERP error)
- Log ERP API calls for debugging
- Consider adding @Async for ERP call if real integration is slow (not in stub)

## Files to Change
- Create: `src/main/java/com/servtech/sinker/service/ErpPurchaseService.java` (stub)
- Update: `src/main/java/com/servtech/sinker/controller/MaterialPurchaseController.java`
- Update: `src/main/java/com/servtech/sinker/service/MaterialPurchaseService.java`
- Create: `src/main/java/com/servtech/sinker/dto/ErpOrderRequest.java`
- Create: `src/main/java/com/servtech/sinker/dto/ErpOrderResponse.java`
- Create: `src/main/java/com/servtech/sinker/exception/AlreadyTriggeredErpException.java`

## Dependencies
- T041: Material purchase query API must be implemented
