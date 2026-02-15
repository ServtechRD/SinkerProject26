# T020: Acceptance Criteria

## Functional Acceptance Criteria

### Basic Query
- [ ] GET /api/sales-forecast/integration?month=202601 returns integrated data for latest version
- [ ] GET /api/sales-forecast/integration?month=202601&version=... returns data for specific version
- [ ] Requires sales_forecast.view permission
- [ ] Missing month parameter returns 400 Bad Request
- [ ] No data for month returns 200 OK with empty array

### Data Aggregation
- [ ] Each row represents one unique product_code
- [ ] Row includes quantities for all 12 channels
- [ ] Channels: PX/大全聯, 家樂福, 愛買, 711, 全家, OK/萊爾富, 好市多, 楓康, 美聯社, 康是美, 電商, 市面經銷
- [ ] If product not in a channel, quantity = 0 (not null)
- [ ] Product appearing in multiple channels correctly aggregated
- [ ] Product metadata (category, spec, product_name, warehouse_location) from any channel record

### Calculations
- [ ] original_subtotal = sum of all 12 channel quantities
- [ ] original_subtotal calculated correctly for each product
- [ ] difference calculated as current_subtotal - previous_subtotal
- [ ] If no previous version, difference = 0
- [ ] If product new in current version, difference = current_subtotal
- [ ] If product removed in current version, difference = -previous_subtotal

### Remarks Generation
- [ ] New product: remarks = "新增產品"
- [ ] Quantity increased: remarks = "數量增加"
- [ ] Quantity decreased: remarks = "數量減少"
- [ ] No change: remarks = "無變化" or empty
- [ ] Removed product: remarks = "已移除" (if applicable)

### Sorting
- [ ] Data sorted by category code: 2-digit category + 2-digit flavor
- [ ] Category code extracted from category field (e.g., "01飲料類" → 01)
- [ ] Numeric sorting applied (not string sorting)
- [ ] Within same category, sorted by flavor code
- [ ] If no numeric code, sorted alphabetically as fallback

### Permission
- [ ] User without sales_forecast.view permission gets 403 Forbidden
- [ ] User with sales_forecast.view can query any month

## API Contract

### GET /api/sales-forecast/integration
**Request:**
```
GET /api/sales-forecast/integration?month=202601&version=2026/01/15 14:30:00
```

**Response (200 OK):**
```json
[
  {
    "warehouse_location": "A01",
    "category": "01飲料類",
    "spec": "600ml*24入",
    "product_name": "可口可樂",
    "product_code": "P001",
    "qty_px": 100.50,
    "qty_carrefour": 80.00,
    "qty_aimall": 60.00,
    "qty_711": 120.00,
    "qty_familymart": 110.00,
    "qty_ok": 50.00,
    "qty_costco": 200.00,
    "qty_fkmart": 40.00,
    "qty_wellsociety": 30.00,
    "qty_cosmed": 25.00,
    "qty_ecommerce": 90.00,
    "qty_distributor": 70.00,
    "original_subtotal": 975.50,
    "difference": 50.00,
    "remarks": "數量增加"
  },
  {
    "warehouse_location": "B02",
    "category": "02零食類",
    "spec": "150g*12包",
    "product_name": "樂事洋芋片",
    "product_code": "P002",
    "qty_px": 50.00,
    "qty_carrefour": 0,
    "qty_aimall": 30.00,
    "qty_711": 40.00,
    "qty_familymart": 0,
    "qty_ok": 0,
    "qty_costco": 100.00,
    "qty_fkmart": 0,
    "qty_wellsociety": 0,
    "qty_cosmed": 0,
    "qty_ecommerce": 60.00,
    "qty_distributor": 20.00,
    "original_subtotal": 300.00,
    "difference": 0,
    "remarks": "無變化"
  }
]
```

## Non-Functional Criteria
- [ ] Query responds within 3 seconds for 500 products
- [ ] Efficient SQL queries (avoid N+1 problem)
- [ ] Uses database indexes for performance
- [ ] Handles large datasets (1000+ products) gracefully
- [ ] Proper error messages for validation failures

## How to Verify

### Manual Testing
1. **Basic Integration Query:**
   - Upload data for 3 channels with overlapping products
   - GET /api/sales-forecast/integration?month=202601
   - Verify each product shows quantities for all 12 channels
   - Verify channels without product show 0

2. **Subtotal Calculation:**
   - Product P001 in channels: 大全聯(100), 家樂福(80), 711(120)
   - Verify original_subtotal = 300
   - Other channels show 0

3. **Difference Calculation:**
   - Upload version 1: P001 subtotal = 300
   - Edit to version 2: P001 subtotal = 350
   - Query version 2
   - Verify difference = 50

4. **New Product:**
   - Version 1: no P002
   - Version 2: P002 subtotal = 100
   - Query version 2
   - Verify P002 difference = 100
   - Verify remarks = "新增產品"

5. **Sorting:**
   - Create products with categories: 01飲料類, 03日用品, 02零食類
   - Query integration
   - Verify sorted: 01飲料類, 02零食類, 03日用品

6. **Permission Test:**
   - Login without sales_forecast.view
   - Query integration
   - Verify 403 Forbidden

7. **Empty Result:**
   - Query month with no data
   - Verify 200 OK with empty array

8. **Version Comparison:**
   - Upload 3 versions with changing quantities
   - Query version 1 - verify differences based on version 0 (or no previous)
   - Query version 2 - verify differences based on version 1
   - Query version 3 - verify differences based on version 2

9. **All 12 Channels:**
   - Upload data to all 12 channels for product P001
   - Query integration
   - Verify P001 row shows all 12 quantities
   - Verify subtotal = sum of all

10. **Product Metadata:**
    - Same product P001 in multiple channels with same metadata
    - Verify metadata (category, spec, name, location) displayed once

### Automated Testing
- Run integration tests with Testcontainers
- Run unit tests for aggregation logic
- Verify all test cases pass
- Performance test with 1000 products
