# T042: Test Plan

## Unit Tests

### ErpPurchaseServiceTest (Stub)
- **test_create_order_returns_order_number**
  - Create ErpOrderRequest
  - Call createOrder(request)
  - Verify ErpOrderResponse with orderNo returned

- **test_create_order_generates_unique_order_numbers**
  - Call createOrder multiple times
  - Verify each returns different orderNo

- **test_create_order_formats_order_number_correctly**
  - Call createOrder
  - Verify orderNo matches pattern "ERP-YYYY-NNNN"

### MaterialPurchaseServiceTest
- **test_trigger_erp_success**
  - Mock repository with entity (is_erp_triggered=FALSE)
  - Mock ErpPurchaseService to return order number
  - Call triggerErp(id)
  - Verify is_erp_triggered set to TRUE
  - Verify erp_order_no stored
  - Verify entity saved

- **test_trigger_erp_throws_error_if_already_triggered**
  - Mock repository with entity (is_erp_triggered=TRUE, erp_order_no="ERP-123")
  - Call triggerErp(id)
  - Verify AlreadyTriggeredErpException thrown with order number in message

- **test_trigger_erp_throws_error_if_not_found**
  - Mock repository to return empty
  - Call triggerErp(999)
  - Verify EntityNotFoundException thrown

- **test_trigger_erp_rollback_on_erp_failure**
  - Mock repository with entity
  - Mock ErpPurchaseService to throw exception
  - Call triggerErp(id)
  - Verify exception propagated
  - Verify entity not saved (is_erp_triggered still FALSE)

- **test_trigger_erp_maps_fields_correctly_to_erp_request**
  - Mock repository with entity
  - Mock ErpPurchaseService
  - Call triggerErp(id)
  - Verify ErpOrderRequest contains correct Itm, PrdNo, Qty, DemandDate

### MaterialPurchaseControllerTest (MockMvc)
- **test_trigger_erp_endpoint_returns_200**
  - Mock service
  - POST /api/material-purchase/1/trigger-erp
  - Verify 200 response with updated entity

- **test_trigger_erp_endpoint_returns_404_for_not_found**
  - Mock service to throw EntityNotFoundException
  - POST /api/material-purchase/999/trigger-erp
  - Verify 404 response

- **test_trigger_erp_endpoint_returns_409_for_duplicate**
  - Mock service to throw AlreadyTriggeredErpException
  - POST /api/material-purchase/1/trigger-erp
  - Verify 409 response

- **test_trigger_erp_endpoint_returns_500_for_erp_failure**
  - Mock service to throw RuntimeException
  - POST /api/material-purchase/1/trigger-erp
  - Verify 500 response

- **test_trigger_erp_permission_check_applied**
  - Mock user without material_purchase.trigger_erp
  - POST /api/material-purchase/1/trigger-erp
  - Verify 403 response

## Integration Tests

### MaterialPurchaseControllerIntegrationTest
- **test_trigger_erp_complete_workflow**
  - Use @SpringBootTest with Testcontainers
  - Insert test data with is_erp_triggered=FALSE
  - POST /api/material-purchase/1/trigger-erp
  - Verify 200 response
  - Verify isErpTriggered: true in response
  - Verify erpOrderNo present in response
  - Query database to verify is_erp_triggered=TRUE and erp_order_no stored

- **test_trigger_erp_duplicate_prevention**
  - Insert test data with is_erp_triggered=TRUE, erp_order_no="ERP-123"
  - POST /api/material-purchase/1/trigger-erp
  - Verify 409 response
  - Verify response message includes existing order number

- **test_trigger_erp_not_found**
  - POST /api/material-purchase/999/trigger-erp
  - Verify 404 response

- **test_trigger_erp_transaction_rollback**
  - Mock ErpPurchaseService to throw exception
  - Insert test data with is_erp_triggered=FALSE
  - POST /api/material-purchase/1/trigger-erp
  - Verify 500 response
  - Query database to verify is_erp_triggered still FALSE
  - Verify erp_order_no still NULL

- **test_trigger_erp_permission_enforcement**
  - Test with user having material_purchase.trigger_erp
  - Verify 200 response
  - Test with user lacking permission
  - Verify 403 response

- **test_trigger_erp_updates_timestamp**
  - Insert test data
  - Note initial updated_at
  - Wait 1 second
  - POST /api/material-purchase/1/trigger-erp
  - Verify updated_at changed in response and database

### ErpPurchaseServiceIntegrationTest
- **test_create_order_stub_integration**
  - Create real ErpOrderRequest
  - Call ErpPurchaseService.createOrder
  - Verify ErpOrderResponse returned
  - Verify orderNo format correct
  - Verify logs contain request parameters

## E2E Tests
N/A - API testing covered by integration tests. Frontend E2E in T043.

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11
- Sample test data:
  ```java
  MaterialPurchase(
    id: 1,
    weekStart: LocalDate.of(2026, 2, 17),
    factory: "F1",
    productCode: "P001",
    productName: "產品A",
    quantity: 1000.00,
    semiProductName: "半成品A",
    semiProductCode: "SP001",
    kgPerBox: 5.50,
    basketQuantity: 5500.00,
    boxesPerBarrel: 20.00,
    requiredBarrels: 275.00,
    isErpTriggered: false,
    erpOrderNo: null
  )
  ```
- Create records with is_erp_triggered=TRUE for duplicate testing
- Use @Sql scripts to populate test data
- Clean database between tests

## Mocking Strategy
- **Unit tests**: Mock MaterialPurchaseRepository, mock ErpPurchaseService
- **Integration tests**: Real database via Testcontainers, real ErpPurchaseService stub
- **Security**: Use @WithMockUser for permission testing
- Mock ErpPurchaseService in transaction rollback tests to simulate failures
