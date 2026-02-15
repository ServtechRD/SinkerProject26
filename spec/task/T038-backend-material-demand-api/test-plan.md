# T038: Test Plan

## Unit Tests

### MaterialDemandServiceTest
- **test_query_material_demand_by_week_and_factory**
  - Mock repository
  - Call service with week_start and factory
  - Verify repository.findByWeekStartAndFactory called
  - Verify DTOs returned

- **test_query_returns_empty_list_when_no_data**
  - Mock repository to return empty list
  - Call service
  - Verify empty list returned (not null)

- **test_results_ordered_by_material_code**
  - Mock repository with unordered data
  - Call service
  - Verify results sorted by material_code

- **test_dto_mapping_includes_all_fields**
  - Mock repository with entity
  - Call service
  - Verify DTO contains all entity fields
  - Verify decimal precision preserved

- **test_handles_null_last_purchase_date**
  - Mock entity with null lastPurchaseDate
  - Call service
  - Verify DTO lastPurchaseDate is null

### MaterialDemandControllerTest (MockMvc)
- **test_endpoint_requires_week_start_parameter**
  - GET /api/material-demand?factory=F1
  - Verify 400 response

- **test_endpoint_requires_factory_parameter**
  - GET /api/material-demand?week_start=2026-02-17
  - Verify 400 response

- **test_endpoint_validates_date_format**
  - GET /api/material-demand?week_start=invalid&factory=F1
  - Verify 400 response

- **test_endpoint_returns_data**
  - Mock service
  - GET /api/material-demand?week_start=2026-02-17&factory=F1
  - Verify 200 response with JSON array

- **test_permission_check_applied**
  - Mock user without material_demand.view
  - GET /api/material-demand?week_start=2026-02-17&factory=F1
  - Verify 403 response

## Integration Tests

### MaterialDemandControllerIntegrationTest
- **test_query_material_demand_with_valid_parameters**
  - Use @SpringBootTest with Testcontainers
  - Insert test data for week 2026-02-17, factory F1
  - GET /api/material-demand?week_start=2026-02-17&factory=F1
  - Verify 200 response
  - Verify correct data returned

- **test_query_filters_by_week_and_factory**
  - Insert data for multiple weeks and factories
  - Query specific week_start and factory
  - Verify only matching records returned

- **test_query_returns_empty_array_for_no_data**
  - Query non-existent week/factory
  - Verify 200 response with empty array

- **test_results_ordered_by_material_code**
  - Insert data with material codes: M003, M001, M002
  - Query data
  - Verify returned in order: M001, M002, M003

- **test_decimal_precision_preserved**
  - Insert data with decimal values: 123.45, 0.50
  - Query data
  - Verify decimal values returned correctly

- **test_date_format_in_response**
  - Insert data
  - Query data
  - Verify dates in format YYYY-MM-DD

- **test_permission_enforcement**
  - Test with user having material_demand.view
  - Verify 200 response
  - Test with user lacking permission
  - Verify 403 response

### MaterialDemandRepositoryTest
- **test_find_by_week_start_and_factory**
  - Save test entities
  - Call repository.findByWeekStartAndFactory
  - Verify correct entities returned

- **test_composite_index_used**
  - Insert large dataset
  - Query with week_start and factory
  - Verify query performance (under 100ms)

## E2E Tests
N/A - API testing covered by integration tests. Frontend E2E in T039.

## Test Data Setup Notes
- Use Testcontainers with MariaDB 10.11
- Sample test data:
  ```java
  MaterialDemand(
    weekStart: LocalDate.of(2026, 2, 17),
    factory: "F1",
    materialCode: "M001",
    materialName: "原料A",
    unit: "kg",
    lastPurchaseDate: LocalDate.of(2026, 2, 10),
    demandDate: LocalDate.of(2026, 2, 20),
    expectedDelivery: 100.50,
    demandQuantity: 500.00,
    estimatedInventory: 50.25
  )
  ```
- Create multiple records for different weeks, factories, materials
- Include records with null lastPurchaseDate
- Use @Sql scripts to populate test data
- Clean database between tests

## Mocking Strategy
- **Unit tests**: Mock MaterialDemandRepository
- **Integration tests**: Real database via Testcontainers
- **Security**: Use @WithMockUser for permission testing
- Mock authentication context for permission checks
