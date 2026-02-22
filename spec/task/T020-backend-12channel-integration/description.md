# T020: Backend API - 12-Channel Integration Query

## Context
Spring Boot 3.2.12 REST API with JWT authentication. This task implements a complex query endpoint that aggregates sales forecast data across all 12 channels for a given month, providing a consolidated view with comparison capabilities.

## Goal
Implement backend API endpoint to query and integrate sales forecast data from all 12 channels simultaneously, pivoting by product code and calculating totals and differences from previous versions.

## Scope

### In Scope
- GET /api/sales-forecast/integration - Query all 12 channels for given month
- Accept query params: month (required), version (optional, defaults to latest)
- Pivot data by product_code: one row per product showing quantities for all 12 channels
- Include metadata columns: warehouse_location, category, spec, product_name, product_code
- Calculate original_subtotal: sum of quantities across all 12 channels
- Calculate difference: comparison with previous version's subtotal
- Calculate remarks: auto-generate notes based on changes
- Sort by category code rules: 2-digit category + 2-digit flavor grouping
- Permission check: sales_forecast.view

### Out of Scope
- Real-time updates or caching
- Filtering by specific products
- Aggregation by category (show all products)
- Export functionality (covered in T021)
- Pagination

## Requirements
- **GET /api/sales-forecast/integration**: Accept month (required), version (optional)
- Query all 12 channels: PX/大全聯, 家樂福, 愛買, 711, 全家, OK/萊爾富, 好市多, 楓康, 美聯社, 康是美, 電商, 市面經銷
- For each product_code appearing in any channel:
  - Aggregate quantities from all 12 channels (0 if product not in channel)
  - Return: product metadata + 12 channel quantities + original_subtotal + difference + remarks
- original_subtotal: SUM(all 12 channel quantities)
- difference: current version subtotal - previous version subtotal for same product
- If no previous version, difference = 0
- remarks: auto-generated based on changes (e.g., "新增產品", "數量增加", "數量減少", "無變化")
- Sort by category code: extract first 2 digits as category, next 2 as flavor, sort numerically
- Permission: sales_forecast.view required
- Return 400 if month missing
- Return empty array if no data

## Implementation Notes
- Use Spring Boot @RestController
- Service layer handles complex aggregation logic
- Query strategy:
  1. Find all distinct product_codes for month (all channels, specific version)
  2. For each product, query quantity for each of 12 channels
  3. Calculate subtotal
  4. Query previous version data for same products
  5. Calculate differences
  6. Generate remarks based on difference values
- Consider using native SQL query for performance (JOIN across channels)
- Or use repository method with Specifications/Criteria API
- DTOs: IntegrationQueryParams, IntegrationRowDTO
- IntegrationRowDTO fields:
  - warehouse_location, category, spec, product_name, product_code
  - qty_px, qty_carrefour, qty_aimall, qty_711, qty_familymart, qty_ok, qty_costco, qty_fkmart, qty_wellsociety, qty_cosmed, qty_ecommerce, qty_distributor
  - original_subtotal, difference, remarks
- Category sorting: parse category string, extract numeric codes, sort
- Log query execution time for performance monitoring

## Files to Change
- `src/main/java/com/servtech/sinker/controller/ForecastIntegrationController.java` (new)
- `src/main/java/com/servtech/sinker/service/ForecastIntegrationService.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/IntegrationRowDTO.java` (new)
- `src/main/java/com/servtech/sinker/dto/forecast/IntegrationQueryParams.java` (new)
- `src/main/java/com/servtech/sinker/repository/SalesForecastRepository.java` (update - add integration queries)

## Dependencies
- T017: Query API for fetching forecast data and versions
- T002: Authentication and permission system
- Spring Data JPA or native SQL for complex queries
