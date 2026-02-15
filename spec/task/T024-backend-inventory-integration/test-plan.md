# T024: Test Plan

## Unit Tests

### InventoryIntegrationServiceTest.java
- **testAggregateforecastData**: Verify 12-channel sum calculation for multiple products
- **testCalculateProductionSubtotal**: Verify formula: forecast - inventory - sales
- **testGenerateVersion**: Verify version format and uniqueness
- **testRealTimeQueryFlow**: Mock all dependencies, verify service orchestration
- **testVersionQueryFlow**: Verify repository query with version parameter
- **testHandleErpFailure**: Verify graceful handling when ERP stub returns error
- **testDateRangeDefaults**: Verify start/end dates default to month boundaries
- **testEmptyForecastData**: Verify behavior when no forecast data exists
- **testDuplicateVersionHandling**: Verify version collision handling

### ErpInventoryServiceTest.java
- **testGetInventoryBalanceStub**: Verify stub returns consistent mock data
- **testGetSalesQuantityStub**: Verify stub returns mock data for date range
- **testProductCodePatterns**: Verify different mock responses for different product patterns
- **testNullProductCode**: Verify error handling for invalid input

### InventoryIntegrationControllerTest.java (WebMvcTest)
- **testGetIntegrationRealTime**: Verify endpoint with month parameter only
- **testGetIntegrationWithDates**: Verify endpoint with start_date and end_date
- **testGetIntegrationWithVersion**: Verify endpoint with version parameter
- **testMissingMonthParameter**: Verify 400 error when month missing
- **testInvalidDateFormat**: Verify 400 error for malformed dates
- **testUnauthorized**: Verify 401 when no JWT token
- **testForbidden**: Verify 403 when missing inventory.view permission
- **testPermissionGranted**: Verify 200 when permission present

## Integration Tests

### InventoryIntegrationIT.java
- **testRealTimeQueryCreatesVersion**: Full flow from API call to database save
- **testVersionQueryRetrievesData**: Verify version query loads correct data
- **testMultipleVersionsForSameMonth**: Create 2 versions, verify both exist
- **testForecastAggregation**: Insert multi-channel forecast, verify sum correct
- **testErpIntegration**: Verify ERP stub calls with correct parameters
- **testTransactionRollback**: Simulate failure mid-process, verify no partial saves
- **testConcurrentQueries**: Multiple simultaneous requests create unique versions
- **test1000ProductsPerformance**: Load test with 1000 products, verify < 2s response

### Setup
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class InventoryIntegrationIT {

    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventorySalesForecastRepository repository;

    @Autowired
    private SalesForecastRepository forecastRepository;

    @MockBean
    private ErpInventoryService erpService;

    private String jwtToken;

    @BeforeEach
    void setup() {
        // Create JWT token with inventory.view permission
        jwtToken = createJwtToken("testuser", "inventory.view");

        // Setup ERP mock responses
        when(erpService.getInventoryBalance(anyString(), anyString()))
            .thenReturn(new BigDecimal("250.00"));
        when(erpService.getSalesQuantity(anyString(), any(), any()))
            .thenReturn(new BigDecimal("100.00"));
    }
}
```

## E2E Tests

### inventory-integration.spec.js (Playwright)
- **testRealTimeQueryFlow**: Login, navigate to inventory page, trigger real-time query, verify data displayed
- **testVersionSelection**: Select previous version from dropdown, verify data loads
- **testPermissionDenied**: Login as user without permission, verify error message
- **testDataAccuracy**: Verify calculated production_subtotal matches forecast - inventory - sales

## Test Data Setup Notes

### Forecast Data (from T020)
```sql
INSERT INTO sales_forecast
(month, product_code, product_name, category, spec, warehouse_location,
 ch1, ch2, ch3, ch4, ch5, ch6, ch7, ch8, ch9, ch10, ch11, ch12)
VALUES
('2026-01', 'PROD001', 'Product 1', 'Cat A', 'Spec A', 'WH-A',
 10.5, 20.5, 30.5, 40.5, 50.5, 60.5, 70.5, 80.5, 90.5, 100.5, 110.5, 120.5),
('2026-01', 'PROD002', 'Product 2', 'Cat B', 'Spec B', 'WH-B',
 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60);
-- PROD001 sum: 786.0
-- PROD002 sum: 390.0
```

### ERP Stub Responses
```java
// In test setup
when(erpService.getInventoryBalance("PROD001", "2026-01"))
    .thenReturn(new BigDecimal("250.00"));
when(erpService.getInventoryBalance("PROD002", "2026-01"))
    .thenReturn(new BigDecimal("100.00"));

when(erpService.getSalesQuantity(eq("PROD001"), any(), any()))
    .thenReturn(new BigDecimal("100.00"));
when(erpService.getSalesQuantity(eq("PROD002"), any(), any()))
    .thenReturn(new BigDecimal("50.00"));
```

### Expected Results
```
PROD001:
  forecast: 786.0
  inventory: 250.0
  sales: 100.0
  production_subtotal: 786.0 - 250.0 - 100.0 = 436.0

PROD002:
  forecast: 390.0
  inventory: 100.0
  sales: 50.0
  production_subtotal: 390.0 - 100.0 - 50.0 = 240.0
```

### User Permissions
```sql
-- User with permission
INSERT INTO users (username, password, email) VALUES ('inventory_user', '$2a$...', 'inv@test.com');
INSERT INTO user_roles (user_id, role_id) VALUES (1, (SELECT id FROM roles WHERE name = 'INVENTORY_VIEWER'));

-- User without permission
INSERT INTO users (username, password, email) VALUES ('basic_user', '$2a$...', 'basic@test.com');
INSERT INTO user_roles (user_id, role_id) VALUES (2, (SELECT id FROM roles WHERE name = 'BASIC_USER'));
```

## Mocking Strategy

### Unit Tests
- Mock InventorySalesForecastRepository for database operations
- Mock SalesForecastRepository for forecast data
- Mock ErpInventoryService for ERP calls
- Use @MockBean for Spring components

### Integration Tests
- Real database via Testcontainers (MariaDB 10.11)
- Mock ErpInventoryService only (stub behavior)
- Real Spring Security and JWT validation
- Real transaction management

### E2E Tests
- Real backend services
- Real database
- ERP stub (not real ERP)
- Real authentication flow

## Performance Benchmarks
- Real-time query (100 products): < 2 seconds
- Real-time query (1000 products): < 5 seconds
- Version query (any size): < 500ms
- 12-channel aggregation (1000 rows): < 1 second
- Concurrent requests (10 simultaneous): all complete within 10 seconds
