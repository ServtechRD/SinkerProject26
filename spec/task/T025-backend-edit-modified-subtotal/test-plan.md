# T025: Test Plan

## Unit Tests

### InventoryIntegrationServiceTest.java
- **testUpdateModifiedSubtotal**: Verify service creates new record with updated value
- **testCopyAllFieldsExceptModified**: Verify all fields copied correctly from original
- **testGenerateNewVersion**: Verify new version generated and differs from original
- **testSetModifiedToZero**: Verify 0 is stored as 0.00, not NULL
- **testSetModifiedToNull**: Verify NULL is stored as NULL, not 0
- **testOriginalRecordUnchanged**: Verify original record not modified
- **testRecordNotFound**: Verify exception thrown for invalid ID
- **testDecimalPrecisionPreserved**: Verify 2 decimal places maintained
- **testNegativeValues**: Verify negative modified_subtotal allowed
- **testLargeValues**: Verify values up to 10 digits before decimal accepted

### InventoryIntegrationControllerTest.java (WebMvcTest)
- **testPutModifiedSubtotal**: Verify endpoint accepts PUT request
- **testValidRequestBody**: Verify JSON deserialization works
- **testInvalidDecimalFormat**: Verify 400 for malformed decimal
- **testMissingRequestBody**: Verify 400 for empty body
- **testInvalidId**: Verify 404 for non-existent ID
- **testUnauthorized**: Verify 401 without JWT
- **testForbidden**: Verify 403 without inventory.edit permission
- **testPermissionGranted**: Verify 200 with inventory.edit permission
- **testNullModifiedSubtotal**: Verify null value accepted in request

### UpdateModifiedSubtotalRequestTest.java
- **testValidation**: Verify @Valid annotations work correctly
- **testDecimalScale**: Verify max 2 decimal places constraint
- **testDecimalPrecision**: Verify max 10 digits before decimal constraint
- **testNullAllowed**: Verify null passes validation

## Integration Tests

### InventoryIntegrationEditIT.java
- **testEditCreatesNewRecord**: Full flow from PUT request to database verification
- **testOriginalPreserved**: Verify original record remains in database unchanged
- **testVersionIncremented**: Verify new version differs from old
- **testMultipleEdits**: Edit same record twice, verify 3 total records (1 original + 2 edits)
- **testVersionQuery**: Verify each version queryable independently via T024 API
- **testConcurrentEdits**: Simulate 2 users editing same record simultaneously
- **testTransactionRollback**: Simulate failure during save, verify no partial data
- **testDecimalEdgeCases**: Test max precision (9999999999.99), min value (0.01), negative
- **testClearModification**: Set to null after setting to value, verify null stored

### Setup
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class InventoryIntegrationEditIT {

    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventorySalesForecastRepository repository;

    private String jwtToken;
    private Long testRecordId;

    @BeforeEach
    void setup() {
        jwtToken = createJwtToken("testuser", "inventory.view", "inventory.edit");

        // Create test record
        InventorySalesForecast record = new InventorySalesForecast();
        record.setMonth("2026-01");
        record.setProductCode("PROD001");
        record.setProductName("Product 1");
        record.setCategory("Cat A");
        record.setSpec("Spec A");
        record.setWarehouseLocation("WH-A");
        record.setSalesQuantity(new BigDecimal("100.00"));
        record.setInventoryBalance(new BigDecimal("250.00"));
        record.setForecastQuantity(new BigDecimal("500.00"));
        record.setProductionSubtotal(new BigDecimal("150.00"));
        record.setVersion("v20260115120000");
        record.setQueryStartDate("2026-01-01");
        record.setQueryEndDate("2026-01-31");

        InventorySalesForecast saved = repository.save(record);
        testRecordId = saved.getId();
    }

    @Test
    void testEditCreatesNewRecord() throws Exception {
        long initialCount = repository.count();

        mockMvc.perform(put("/api/inventory-integration/" + testRecordId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modifiedSubtotal\": 200.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(not(testRecordId)))
                .andExpect(jsonPath("$.modifiedSubtotal").value(200.50))
                .andExpect(jsonPath("$.productionSubtotal").value(150.00))
                .andExpect(jsonPath("$.version").value(not("v20260115120000")));

        assertEquals(initialCount + 1, repository.count());

        // Verify original unchanged
        InventorySalesForecast original = repository.findById(testRecordId).orElseThrow();
        assertNull(original.getModifiedSubtotal());
        assertEquals("v20260115120000", original.getVersion());
    }
}
```

## E2E Tests

### inventory-edit.spec.js (Playwright)
- **testEditFromTable**: Click edit button in table, modify value, save, verify new version created
- **testEditMultipleTimes**: Edit same record twice, verify version history shows both
- **testEditPermissionDenied**: Login as view-only user, verify edit button disabled
- **testEditValidation**: Enter invalid decimal, verify error message displayed
- **testClearModification**: Set value, then clear it, verify reverts to calculated value

## Test Data Setup Notes

### Initial Record
```java
InventorySalesForecast original = InventorySalesForecast.builder()
    .month("2026-01")
    .productCode("PROD001")
    .productName("Product 1")
    .category("Cat A")
    .spec("Spec A")
    .warehouseLocation("WH-A")
    .salesQuantity(new BigDecimal("100.00"))
    .inventoryBalance(new BigDecimal("250.00"))
    .forecastQuantity(new BigDecimal("500.00"))
    .productionSubtotal(new BigDecimal("150.00"))
    .modifiedSubtotal(null)
    .version("v20260115120000")
    .queryStartDate("2026-01-01")
    .queryEndDate("2026-01-31")
    .build();
```

### Edit Request Examples
```json
// Set to positive value
{"modifiedSubtotal": 200.50}

// Set to zero
{"modifiedSubtotal": 0}

// Set to negative
{"modifiedSubtotal": -50.75}

// Clear modification
{"modifiedSubtotal": null}

// Max precision
{"modifiedSubtotal": 9999999999.99}

// Min value
{"modifiedSubtotal": 0.01}
```

### Expected Results After Edit
```
Original Record (ID=1):
  modified_subtotal: NULL
  version: v20260115120000

New Record (ID=2):
  modified_subtotal: 200.50
  version: v20260115130000 (or similar)
  production_subtotal: 150.00 (unchanged)
  All other fields: same as original
```

### User Permissions
```sql
-- User with edit permission
INSERT INTO users (username, password, email) VALUES ('editor_user', '$2a$...', 'editor@test.com');
INSERT INTO user_roles (user_id, role_id) VALUES
  (1, (SELECT id FROM roles WHERE name = 'INVENTORY_VIEWER')),
  (1, (SELECT id FROM roles WHERE name = 'INVENTORY_EDITOR'));

-- User with view-only permission
INSERT INTO users (username, password, email) VALUES ('viewer_user', '$2a$...', 'viewer@test.com');
INSERT INTO user_roles (user_id, role_id) VALUES
  (2, (SELECT id FROM roles WHERE name = 'INVENTORY_VIEWER'));
```

## Mocking Strategy

### Unit Tests
- Mock InventorySalesForecastRepository for all database operations
- Use ArgumentCaptor to verify correct data passed to save()
- Mock version generator if extracted to separate component

### Integration Tests
- Real database via Testcontainers (MariaDB 10.11)
- Real Spring Security with test JWT tokens
- No mocking except for external services (none in this task)

### E2E Tests
- Real backend services
- Real database
- Real authentication flow
- Test user accounts with different permissions

## Performance Benchmarks
- Single edit operation: < 500ms
- Edit with database containing 10,000 records: < 500ms
- Concurrent edits (10 users): all complete within 2 seconds
- Query performance not degraded with multiple versions per product

## Edge Cases to Test
1. Edit record that has already been edited (chain of versions)
2. Edit record with modified_subtotal already set
3. Set modified_subtotal to same value as production_subtotal
4. Very large negative values
5. Maximum decimal precision (9999999999.99)
6. Minimum decimal precision (0.01)
7. Concurrent edits of same record by different users
8. Edit during active database transaction from another operation
