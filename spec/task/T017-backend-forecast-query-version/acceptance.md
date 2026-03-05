# T017: Acceptance Criteria

## Functional Acceptance Criteria

### Query Forecast Items
- [ ] GET /api/sales-forecast?month=202601&channel=大全聯 returns all items for latest version
- [ ] GET /api/sales-forecast?month=202601&channel=大全聯&version=... returns items for specific version
- [ ] Results sorted by category ASC, spec ASC, product_code ASC
- [ ] Returns all fields: id, month, channel, category, spec, product_code, product_name, warehouse_location, quantity, version, is_modified, created_at, updated_at
- [ ] User with sales_forecast.view can query any channel
- [ ] User with sales_forecast.view_own can only query own channels
- [ ] Missing month parameter returns 400 Bad Request
- [ ] Missing channel parameter returns 400 Bad Request
- [ ] No data found returns 200 OK with empty array
- [ ] Invalid month format returns 400 Bad Request
- [ ] Invalid channel returns 400 Bad Request

### Query Versions
- [ ] GET /api/sales-forecast/versions?month=202601&channel=大全聯 returns all distinct versions
- [ ] Versions sorted DESC (newest first)
- [ ] Each version includes: version string, item count, timestamp (extracted from version or latest updated_at)
- [ ] User with sales_forecast.view can query any channel
- [ ] User with sales_forecast.view_own can only query own channels
- [ ] Missing month parameter returns 400 Bad Request
- [ ] Missing channel parameter returns 400 Bad Request
- [ ] No versions found returns 200 OK with empty array

### Permission-Based Filtering
- [ ] User without sales_forecast.view or sales_forecast.view_own gets 403 Forbidden
- [ ] User with view_own querying non-owned channel gets 403 Forbidden
- [ ] User with view querying any channel succeeds

### Latest Version Logic
- [ ] When version param omitted, returns items from latest version
- [ ] Latest determined by MAX(version) or most recent timestamp
- [ ] If multiple versions exist, only latest returned

## API Contracts

### GET /api/sales-forecast
**Request:**
```
GET /api/sales-forecast?month=202601&channel=大全聯
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "month": "202601",
    "channel": "大全聯",
    "category": "飲料類",
    "spec": "600ml*24入",
    "product_code": "P001",
    "product_name": "可口可樂",
    "warehouse_location": "A01",
    "quantity": 100.50,
    "version": "2026/01/15 14:30:00(大全聯)",
    "is_modified": false,
    "created_at": "2026-01-15T14:30:00",
    "updated_at": "2026-01-15T14:30:00"
  },
  {
    "id": 2,
    "month": "202601",
    "channel": "大全聯",
    "category": "零食類",
    "spec": "150g*12包",
    "product_code": "P002",
    "product_name": "樂事洋芋片",
    "warehouse_location": "B02",
    "quantity": 50.00,
    "version": "2026/01/15 14:30:00(大全聯)",
    "is_modified": true,
    "created_at": "2026-01-15T14:30:00",
    "updated_at": "2026-01-15T15:00:00"
  }
]
```

**Request with Version:**
```
GET /api/sales-forecast?month=202601&channel=大全聯&version=2026/01/15 14:30:00(大全聯)
```

### GET /api/sales-forecast/versions
**Request:**
```
GET /api/sales-forecast/versions?month=202601&channel=大全聯
```

**Response (200 OK):**
```json
[
  {
    "version": "2026/01/15 15:00:00(大全聯)",
    "item_count": 150,
    "timestamp": "2026-01-15T15:00:00"
  },
  {
    "version": "2026/01/15 14:30:00(大全聯)",
    "item_count": 148,
    "timestamp": "2026-01-15T14:30:00"
  },
  {
    "version": "2026/01/10 09:00:00(大全聯)",
    "item_count": 145,
    "timestamp": "2026-01-10T09:00:00"
  }
]
```

## Non-Functional Criteria
- [ ] Query responds within 1 second for up to 1000 items
- [ ] Uses database indexes for efficient querying
- [ ] Large result sets handled gracefully
- [ ] Proper HTTP status codes used
- [ ] Clear error messages for validation failures

## How to Verify

### Manual Testing
1. **Query Latest Version:**
   - Upload forecast data for 202601 + 大全聯
   - GET /api/sales-forecast?month=202601&channel=大全聯
   - Verify returns items from latest version
   - Verify sorted by category, spec, product_code

2. **Query Specific Version:**
   - Upload data (version 1)
   - Edit item (version 2)
   - GET with version=version1
   - Verify returns version 1 data
   - GET with version=version2
   - Verify returns version 2 data

3. **List Versions:**
   - Upload data 3 times with different timestamps
   - GET /api/sales-forecast/versions?month=202601&channel=大全聯
   - Verify returns 3 versions sorted DESC

4. **Permission - View All:**
   - Login as user with sales_forecast.view
   - Query any channel
   - Verify succeeds

5. **Permission - View Own:**
   - Login as user with sales_forecast.view_own for channel 家樂福
   - Query channel 家樂福 - succeeds
   - Query channel 大全聯 - returns 403 Forbidden

6. **No Permission:**
   - Login without view permissions
   - Query any channel
   - Verify 403 Forbidden

7. **Empty Results:**
   - Query non-existent month+channel
   - Verify 200 OK with empty array

8. **Validation:**
   - GET without month parameter - 400 Bad Request
   - GET without channel parameter - 400 Bad Request
   - GET with invalid month format - 400 Bad Request

9. **Sorting:**
   - Create items with various categories: "零食類", "飲料類", "日用品"
   - Query and verify sorted alphabetically by category
   - Within same category, verify sorted by spec

10. **Is Modified Highlight:**
    - Upload data (is_modified=FALSE)
    - Edit one item (is_modified=TRUE)
    - Query data
    - Verify is_modified flag correctly reflects edit status

### Automated Testing
- Run integration tests with Testcontainers
- Run unit tests for query service
- Verify all test cases pass
