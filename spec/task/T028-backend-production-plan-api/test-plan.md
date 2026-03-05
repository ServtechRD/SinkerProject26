# T028: Test Plan

## Unit Tests

### ProductionPlanServiceTest.java
- **testGetPlansByYear**: Verify service fetches all plans for given year
- **testCalculateTotalQuantity**: Verify sum of monthly values + buffer
- **testCalculateTotalWithMissingMonths**: Verify missing months treated as 0
- **testCalculateTotalWithEmptyAllocation**: Verify empty JSON returns only buffer
- **testCalculateDifference**: Verify total_quantity - original_forecast
- **testUpdateMonthlyAllocation**: Verify update logic and recalculation
- **testUpdateBufferQuantity**: Verify buffer update triggers total recalc
- **testUpdateRemarks**: Verify remarks update (including null/empty)
- **testPlanNotFound**: Verify exception thrown for invalid ID
- **testEmptyYear**: Verify empty list for year with no data

### MonthlyAllocationValidatorTest.java
- **testValidMonths**: Verify months 2-12 accepted
- **testInvalidMonth1**: Verify month "1" rejected
- **testInvalidMonth13**: Verify month "13" rejected
- **testInvalidMonthZero**: Verify month "0" rejected
- **testValidDecimalValues**: Verify proper decimal values accepted
- **testInvalidDecimalTooManyDigits**: Verify >10 digits rejected
- **testInvalidDecimalTooManyPlaces**: Verify >2 decimal places rejected
- **testNegativeValue**: Verify negative values rejected
- **testZeroValue**: Verify zero values accepted
- **testEmptyAllocation**: Verify empty map accepted
- **testNullAllocation**: Verify null map handling

### ProductionPlanControllerTest.java (WebMvcTest)
- **testGetPlans**: Verify GET endpoint with year parameter
- **testGetPlansMissingYear**: Verify 400 when year missing
- **testGetPlansInvalidYear**: Verify 400 for invalid year format
- **testPutPlan**: Verify PUT endpoint updates plan
- **testPutPlanInvalidId**: Verify 404 for non-existent ID
- **testPutPlanInvalidData**: Verify 400 for validation failures
- **testUnauthorized**: Verify 401 without JWT
- **testForbiddenGet**: Verify 403 without production_plan.view
- **testForbiddenPut**: Verify 403 without production_plan.edit
- **testPermissionGranted**: Verify 200 with proper permissions

### MonthlyAllocationConverterTest.java
- **testConvertToDatabaseColumn**: Verify Map<String, BigDecimal> → JSON string
- **testConvertToEntityAttribute**: Verify JSON string → Map<String, BigDecimal>
- **testEmptyMap**: Verify empty map → "{}"
- **testNullValue**: Verify null handling
- **testInvalidJson**: Verify exception for malformed JSON
- **testPreserveDecimalPrecision**: Verify 2 decimal places preserved

## Integration Tests

### ProductionPlanIT.java
- **testGetPlansByYear**: Full flow from API to database
- **testMultipleChannelsSameProduct**: Verify all channels returned
- **testOrderByProductAndChannel**: Verify correct ordering
- **testUpdatePlanRecalculates**: Update via API, verify DB calculations correct
- **testJsonPersistence**: Insert via API, query DB, verify JSON intact
- **testConcurrentUpdates**: Multiple simultaneous updates to different records
- **testTransactionRollback**: Simulate failure during update, verify no partial changes
- **testLargeDataset**: Query 1000 records, verify performance
- **testDecimalPrecision**: Verify calculations maintain 2 decimal precision
- **testNegativeDifference**: Verify negative difference (over-forecast) handled correctly

### Setup
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class ProductionPlanIT {

    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductionPlanRepository repository;

    private String jwtToken;

    @BeforeEach
    void setup() {
        jwtToken = createJwtToken("testuser", "production_plan.view", "production_plan.edit");

        // Create test data
        ProductionPlan plan = new ProductionPlan();
        plan.setYear(2026);
        plan.setProductCode("PROD001");
        plan.setProductName("Product 1");
        plan.setCategory("Cat A");
        plan.setSpec("Spec A");
        plan.setWarehouseLocation("WH-A");
        plan.setChannel("CH-DIRECT");

        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("2", new BigDecimal("100.00"));
        allocation.put("3", new BigDecimal("150.00"));
        allocation.put("4", new BigDecimal("200.00"));
        plan.setMonthlyAllocation(allocation);

        plan.setBufferQuantity(new BigDecimal("50.00"));
        plan.setTotalQuantity(new BigDecimal("500.00"));
        plan.setOriginalForecast(new BigDecimal("480.00"));
        plan.setDifference(new BigDecimal("20.00"));

        repository.save(plan);
    }

    @Test
    void testUpdatePlanRecalculates() throws Exception {
        ProductionPlan original = repository.findAll().get(0);

        String requestBody = """
            {
                "monthlyAllocation": {
                    "2": 120.00,
                    "3": 180.00,
                    "4": 220.00
                },
                "bufferQuantity": 60.00,
                "remarks": "Updated"
            }
            """;

        mockMvc.perform(put("/api/production-plan/" + original.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(580.00))
                .andExpect(jsonPath("$.difference").value(100.00)); // 580 - 480

        ProductionPlan updated = repository.findById(original.getId()).orElseThrow();
        assertEquals(new BigDecimal("580.00"), updated.getTotalQuantity());
        assertEquals(new BigDecimal("100.00"), updated.getDifference());
    }
}
```

## E2E Tests

### production-plan.spec.js (Playwright)
- **testViewPlansByYear**: Login, navigate to production plan page, select year, verify data displayed
- **testEditMonthlyValues**: Click cell, edit value, verify total recalculated
- **testEditBuffer**: Edit buffer quantity, verify total updated
- **testEditRemarks**: Add remarks, save, verify persisted
- **testMultipleChannels**: Verify all channels for product shown in grid
- **testPermissionDenied**: Login without edit permission, verify read-only mode
- **testCalculationAccuracy**: Edit multiple months, verify totals and differences correct

## Test Data Setup Notes

### Basic Test Plan
```java
ProductionPlan plan = ProductionPlan.builder()
    .year(2026)
    .productCode("PROD001")
    .productName("Product 1")
    .category("Category A")
    .spec("Spec A")
    .warehouseLocation("WH-A")
    .channel("CH-DIRECT")
    .monthlyAllocation(Map.of(
        "2", new BigDecimal("100.00"),
        "3", new BigDecimal("150.00"),
        "4", new BigDecimal("200.00")
    ))
    .bufferQuantity(new BigDecimal("50.00"))
    .totalQuantity(new BigDecimal("500.00"))
    .originalForecast(new BigDecimal("480.00"))
    .difference(new BigDecimal("20.00"))
    .build();
```

### Full Year Allocation
```java
Map<String, BigDecimal> fullYear = new HashMap<>();
fullYear.put("2", new BigDecimal("100.00"));
fullYear.put("3", new BigDecimal("110.00"));
fullYear.put("4", new BigDecimal("120.00"));
fullYear.put("5", new BigDecimal("130.00"));
fullYear.put("6", new BigDecimal("140.00"));
fullYear.put("7", new BigDecimal("150.00"));
fullYear.put("8", new BigDecimal("160.00"));
fullYear.put("9", new BigDecimal("170.00"));
fullYear.put("10", new BigDecimal("180.00"));
fullYear.put("11", new BigDecimal("190.00"));
fullYear.put("12", new BigDecimal("200.00"));
// Total: 1650.00
```

### Multiple Channels Test Data
```java
// Same product, different channels
createPlan(2026, "PROD001", "CH-DIRECT", allocation1, 50.00, 2000.00);
createPlan(2026, "PROD001", "CH-RETAIL", allocation2, 30.00, 1500.00);
createPlan(2026, "PROD001", "CH-ONLINE", allocation3, 20.00, 1000.00);
```

### Validation Test Cases
```json
// Valid request
{
  "monthlyAllocation": {"2": 100, "3": 150, "12": 200},
  "bufferQuantity": 50.00
}

// Invalid: month 1
{
  "monthlyAllocation": {"1": 100, "2": 150},
  "bufferQuantity": 50.00
}

// Invalid: month 13
{
  "monthlyAllocation": {"12": 100, "13": 150},
  "bufferQuantity": 50.00
}

// Invalid: negative value
{
  "monthlyAllocation": {"2": -100},
  "bufferQuantity": 50.00
}

// Invalid: too many decimals
{
  "monthlyAllocation": {"2": 100.555},
  "bufferQuantity": 50.00
}

// Invalid: negative buffer
{
  "monthlyAllocation": {"2": 100},
  "bufferQuantity": -10.00
}

// Valid: empty allocation
{
  "monthlyAllocation": {},
  "bufferQuantity": 100.00
}
```

### Calculation Test Cases
```
Case 1: Normal allocation
  monthly: {2: 100, 3: 150, 4: 200} = 450
  buffer: 50
  total: 500
  forecast: 480
  difference: 20

Case 2: Over-forecast (negative difference)
  monthly: {2: 50, 3: 75} = 125
  buffer: 25
  total: 150
  forecast: 200
  difference: -50

Case 3: Empty allocation
  monthly: {} = 0
  buffer: 100
  total: 100
  forecast: 150
  difference: -50

Case 4: Full year
  monthly: {2-12 with various values} = 1650
  buffer: 100
  total: 1750
  forecast: 1700
  difference: 50
```

### User Permissions
```sql
-- User with view and edit permissions
INSERT INTO users (username, password, email) VALUES ('planner_user', '$2a$...', 'planner@test.com');
INSERT INTO user_roles (user_id, role_id) VALUES
  (1, (SELECT id FROM roles WHERE name = 'PRODUCTION_PLANNER'));

-- User with view-only permission
INSERT INTO users (username, password, email) VALUES ('viewer_user', '$2a$...', 'viewer@test.com');
INSERT INTO user_roles (user_id, role_id) VALUES
  (2, (SELECT id FROM roles WHERE name = 'PRODUCTION_VIEWER'));
```

## Mocking Strategy

### Unit Tests
- Mock ProductionPlanRepository for all database operations
- Mock MonthlyAllocationConverter if used
- Use @MockBean for Spring components
- ArgumentCaptor to verify calculation correctness

### Integration Tests
- Real database via Testcontainers (MariaDB 10.11)
- Real Spring Security with test JWT tokens
- Real JSON persistence and retrieval
- No external service mocks (self-contained)

### E2E Tests
- Real backend services
- Real database with test data
- Real authentication flow
- Test user accounts with different permissions

## Performance Benchmarks
- GET query (100 products × 3 channels): < 500ms
- GET query (1000 products × 3 channels): < 1.5 seconds
- PUT update single record: < 300ms
- Calculation time for full year allocation: < 50ms
- JSON serialization/deserialization: < 10ms per record

## Edge Cases to Test
1. Empty monthly allocation (only buffer quantity)
2. Zero buffer quantity (only monthly allocations)
3. Both empty (total = 0)
4. Very large allocations (999999999.99 per month)
5. All months populated vs sparse allocation
6. Negative difference (over-forecast scenario)
7. Zero difference (exact match)
8. Original forecast = 0
9. Update that results in total = 0
10. Concurrent updates to same record
11. Update with remarks exceeding TEXT limit
12. Remarks with special characters and newlines
13. Product with 10+ channels
14. Year boundaries (2025, 2026, 2027, etc.)
15. JSON with invalid month keys mixed with valid ones
