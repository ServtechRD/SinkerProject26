# T020: Test Plan

## Unit Tests

### ForecastIntegrationServiceTest

1. **testIntegrationQuery_SingleProduct**
   - Mock repository to return product P001 in 3 channels
   - Call queryIntegration(month, version)
   - Assert returns 1 row with 3 channels populated, others = 0

2. **testIntegrationQuery_MultipleProducts**
   - Mock repository to return 5 products across various channels
   - Assert returns 5 rows, each with correct channel quantities

3. **testSubtotalCalculation**
   - Mock product with quantities: 大全聯=100, 家樂福=80, 711=120
   - Assert original_subtotal = 300

4. **testDifferenceCalculation_Increase**
   - Mock current version subtotal = 350
   - Mock previous version subtotal = 300
   - Assert difference = 50

5. **testDifferenceCalculation_Decrease**
   - Mock current = 200, previous = 300
   - Assert difference = -100

6. **testDifferenceCalculation_NewProduct**
   - Mock current = 100, no previous
   - Assert difference = 100

7. **testDifferenceCalculation_NoPrevious**
   - Query first version (no previous)
   - Assert all differences = 0

8. **testRemarksGeneration_NewProduct**
   - Mock new product (difference = subtotal)
   - Assert remarks = "新增產品"

9. **testRemarksGeneration_Increase**
   - Mock difference > 0
   - Assert remarks = "數量增加"

10. **testRemarksGeneration_Decrease**
    - Mock difference < 0
    - Assert remarks = "數量減少"

11. **testRemarksGeneration_NoChange**
    - Mock difference = 0
    - Assert remarks = "無變化"

12. **testSorting_CategoryCode**
    - Mock products with categories: "03日用品", "01飲料類", "02零食類"
    - Assert sorted: 01, 02, 03

13. **testSorting_FlavorCode**
    - Mock products in same category with flavor codes: 0102, 0101, 0103
    - Assert sorted: 0101, 0102, 0103

14. **testSorting_FallbackAlphabetic**
    - Mock products without numeric codes
    - Assert sorted alphabetically

15. **testAll12Channels**
    - Mock product in all 12 channels
    - Assert all qty fields populated correctly

16. **testMissingChannels**
    - Mock product in only 2 channels
    - Assert other 10 channels = 0 (not null)

17. **testEmptyResult**
    - Mock repository to return no data
    - Assert returns empty list

## Integration Tests

### ForecastIntegrationControllerIntegrationTest (Spring Boot Test + Testcontainers)

1. **testIntegrationQuery_Success**
   - Insert data for 3 products across 5 channels
   - GET /api/sales-forecast/integration?month=202601
   - Assert 200 OK
   - Assert 3 rows returned
   - Verify each row structure

2. **testIntegrationQuery_SpecificVersion**
   - Insert 2 versions of data
   - GET with version parameter
   - Assert returns data for specified version

3. **testIntegrationQuery_LatestVersion**
   - Insert 3 versions
   - GET without version parameter
   - Assert returns latest version data

4. **testIntegrationQuery_Unauthorized**
   - GET without JWT
   - Assert 401 Unauthorized

5. **testIntegrationQuery_NoPermission**
   - GET with JWT missing sales_forecast.view
   - Assert 403 Forbidden

6. **testIntegrationQuery_MissingMonth**
   - GET /api/sales-forecast/integration (no params)
   - Assert 400 Bad Request

7. **testIntegrationQuery_EmptyResult**
   - GET for non-existent month
   - Assert 200 OK with empty array

8. **testSubtotalCalculation**
   - Insert product P001:
     - 大全聯: 100
     - 家樂福: 80
     - 711: 120
     - Other channels: none
   - Query integration
   - Assert P001 original_subtotal = 300

9. **testDifferenceCalculation**
   - Version 1: Insert P001 total = 300
   - Version 2: Insert P001 total = 350
   - Query version 2
   - Assert difference = 50

10. **testNewProductDifference**
    - Version 1: only P001
    - Version 2: add P002 (total = 100)
    - Query version 2
    - Assert P002 difference = 100
    - Assert P002 remarks = "新增產品"

11. **testRemarksGeneration**
    - Setup various scenarios (increase, decrease, no change)
    - Query integration
    - Assert correct remarks for each product

12. **testSorting**
    - Insert products with categories: 03, 01, 02
    - Query integration
    - Assert returned in order: 01, 02, 03

13. **testAll12Channels**
    - Insert product P001 in all 12 channels with different quantities
    - Query integration
    - Assert all 12 qty fields populated
    - Assert subtotal = sum of all

14. **testProductMetadata**
    - Insert same product in multiple channels
    - Query integration
    - Assert one row with correct metadata

15. **testPerformance_LargeDataset**
    - Insert 500 products across 12 channels (6000 records)
    - Measure query execution time
    - Assert < 3 seconds

16. **testMultipleProductsOverlap**
    - Insert:
      - P001 in channels: 大全聯, 家樂福, 711
      - P002 in channels: 家樂福, 愛買, 好市多
      - P003 in channels: 711, 全家, OK
    - Query integration
    - Assert 3 rows, each with correct channel distribution

## E2E Tests
N/A - Backend integration query is thoroughly tested via integration tests. E2E tests will cover frontend display in T022.

## Test Data Setup
- Use Testcontainers MariaDB 10.11
- Create test user with sales_forecast.view permission
- Create multiple versions of forecast data:
  - Version 1: 10 products across various channels
  - Version 2: 12 products (2 new, 10 updated)
  - Version 3: 11 products (1 removed from version 2)
- Sample data structure:
  ```sql
  -- Product P001 in multiple channels
  INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version, is_modified)
  VALUES
    ('202601', '大全聯', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 100, 'v1', false),
    ('202601', '家樂福', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 80, 'v1', false),
    ('202601', '711', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 120, 'v1', false);
  ```

## Mocking Strategy
- **Unit Tests:**
  - Mock SalesForecastRepository query methods
  - Mock version comparison logic
  - Use Mockito to return sample data
  - Test aggregation and calculation logic in isolation

- **Integration Tests:**
  - No mocking of Spring components
  - Use real database via Testcontainers
  - Insert real test data
  - Execute actual SQL queries
  - Measure real query performance
  - Test complex JOINs and aggregations

- **Performance Testing:**
  - Use @Sql scripts to load large datasets
  - Profile SQL execution with EXPLAIN
  - Optimize indexes based on test results
