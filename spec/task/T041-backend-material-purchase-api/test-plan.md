# T041: Test Plan

## Unit Tests

### BomServiceTest (Stub)
- **test_get_kg_per_box_for_known_product**
  - Call getKgPerBox("P001")
  - Verify returns hardcoded value (e.g., 5.50)

- **test_get_kg_per_box_for_unknown_product**
  - Call getKgPerBox("UNKNOWN")
  - Verify returns default value (e.g., 1.00)

- **test_get_boxes_per_barrel_for_known_product**
  - Call getBoxesPerBarrel("P001")
  - Verify returns hardcoded value (e.g., 20.00)

- **test_get_boxes_per_barrel_for_unknown_product**
  - Call getBoxesPerBarrel("UNKNOWN")
  - Verify returns default value (e.g., 10.00)

### MaterialPurchaseServiceTest
- **test_query_material_purchase_by_week_and_factory**
  - Mock repository
  - Call service with week_start and factory
  - Verify repository.findByWeekStartAndFactory called
  - Verify DTOs returned

- **test_query_returns_empty_list_when_no_data**
  - Mock repository to return empty list
  - Call service
  - Verify empty list returned (not null)

- **test_results_ordered_by_product_code**
  - Mock repository with unordered data
  - Call service
  - Verify results sorted by product_code

- **test_dto_mapping_includes_all_fields**
  - Mock repository with entity
  - Call service
  - Verify DTO contains all entity fields
  - Verify decimal precision preserved
  - Verify boolean is_erp_triggered mapped correctly

- **test_calculations_correct**
  - Create entity with quantity=1000, kg_per_box=5.50, boxes_per_barrel=20.00
  - Call service
  - Verify basketQuantity = 5500.00
  - Verify requiredBarrels = 275.00

### MaterialPurchaseControllerTest (MockMvc)
- **test_endpoint_requires_week_start_parameter**
  - GET /api/material-purchase?factory=F1
  - Verify 400 response

- **test_endpoint_requires_factory_parameter**
  - GET /api/material-purchase?week_start=2026-02-17
  - Verify 400 response

- **test_endpoint_validates_date_format**
  - GET /api/material-purchase?week_start=invalid&factory=F1
  - Verify 400 response

- **test_endpoint_returns_data**
  - Mock service
  - GET /api/material-purchase?week_start=2026-02-17&factory=F1
  - Verify 200 response with JSON array

- **test_permission_check_applied**
  - Mock user without material_purchase.view
  - GET /api/material-purchase?week_start=2026-02-17&factory=F1
  - Verify 403 response

## Integration Tests

### MaterialPurchaseControllerIntegrationTest
- **test_query_material_purchase_with_valid_parameters**
  - Use @SpringBootTest with Testcontainers
  - Insert test data for week 2026-02-17, factory F1
  - GET /api/material-purchase?week_start=2026-02-17&factory=F1
  - Verify 200 response
  - Verify correct data returned

- **test_query_filters_by_week_and_factory**
  - Insert data for multiple weeks and factories
  - Query specific week_start and factory
  - Verify only matching records returned

- **test_query_returns_empty_array_for_no_data**
  - Query non-existent week/factory
  - Verify 200 response with empty array

- **test_results_ordered_by_product_code**
  - Insert data with product codes: P003, P001, P002
  - Query data
  - Verify returned in order: P001, P002, P003

- **test_decimal_precision_preserved**
  - Insert data with decimal values: 5.50, 1500.75
  - Query data
  - Verify decimal values returned correctly

- **test_boolean_is_erp_triggered_returned**
  - Insert data with is_erp_triggered = TRUE
  - Query data
  - Verify isErpTriggered: true in JSON

- **test_erp_order_no_included**
  - Insert data with erp_order_no = "ERP-123"
  - Query data
  - Verify erpOrderNo: "ERP-123" in JSON

- **test_permission_enforcement**
  - Test with user having material_purchase.view
  - Verify 200 response
  - Test with user lacking permission
  - Verify 403 response

### MaterialPurchaseRepositoryTest
- **test_find_by_week_start_and_factory**
  - Save test entities
  - Call repository.findByWeekStartAndFactory
  - Verify correct entities returned

- **test_composite_index_used**
  - Insert large dataset
  - Query with week_start and factory
  - Verify query performance (under 100ms)

## E2E Tests
N/A - API testing covered by integration tests. Frontend E2E in T043.

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11
- Sample test data:
  ```java
  MaterialPurchase(
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
- Create multiple records for different weeks, factories, products
- Include records with is_erp_triggered = TRUE and erp_order_no set
- Use @Sql scripts to populate test data
- Clean database between tests

## Mocking Strategy
- **Unit tests**: Mock MaterialPurchaseRepository, mock BomService
- **Integration tests**: Real database via Testcontainers, real BomService stub
- **Security**: Use @WithMockUser for permission testing
- Mock authentication context for permission checks
