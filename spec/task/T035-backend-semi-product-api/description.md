# T035: Backend API - Semi Product Management

## Context
This task implements the backend REST API for managing semi-product advance purchase configuration data. Users need to upload Excel files containing semi-product configurations, view existing data, and edit advance days for individual products. The upload operation performs a TRUNCATE and re-insert to ensure data consistency.

## Goal
Create REST API endpoints for semi-product management including Excel upload with TRUNCATE-and-reload strategy, listing all products, editing advance days, and template download. Implement proper permission checks and validation.

## Scope

### In Scope
- POST /api/semi-product/upload - Excel file upload endpoint
- GET /api/semi-product - List all semi-products
- PUT /api/semi-product/:id - Edit advance_days for a product
- GET /api/semi-product/template - Download Excel template
- Excel parsing for columns: 品號, 品名, 提前日數
- TRUNCATE + bulk insert strategy for upload
- Permission checks: semi_product.upload, semi_product.view, semi_product.edit
- Validation: advance_days must be positive integer
- JPA entity and repository

### Out of Scope
- Frontend implementation (T036)
- Database migration (T034)
- Soft delete functionality
- Audit logging beyond created_at/updated_at

## Requirements
- **Entity**: `SemiProductAdvancePurchase` with fields: id, productCode, productName, advanceDays, createdAt, updatedAt
- **Repository**: `SemiProductAdvancePurchaseRepository` extending JpaRepository
- **Controller**: `SemiProductController` with endpoints:
  - POST /api/semi-product/upload (multipart/form-data)
  - GET /api/semi-product
  - PUT /api/semi-product/{id}
  - GET /api/semi-product/template
- **Service**: `SemiProductService` with business logic
- **Excel Parser**: `SemiProductExcelParser` using Apache POI
- **Validation**:
  - advance_days > 0
  - Required fields: product_code, product_name, advance_days
  - Excel format validation
- **Permissions**: Use Spring Security with custom permission annotations
- **Error Handling**: Return appropriate HTTP status codes and error messages

## Implementation Notes
- Use Apache POI library for Excel parsing (.xlsx format)
- Excel template should have headers: 品號, 品名, 提前日數
- Upload process: Parse Excel → Validate all rows → TRUNCATE table → Insert all rows in transaction
- If any validation fails, reject entire upload (no partial updates)
- PUT endpoint only allows editing advance_days, not product_code or product_name
- Use @PreAuthorize annotations for permission checks
- Return DTOs instead of entities to avoid JPA lazy-loading issues
- Template download generates Excel file with headers only

## Files to Change
- Create: `src/main/java/com/servtech/sinker/entity/SemiProductAdvancePurchase.java`
- Create: `src/main/java/com/servtech/sinker/repository/SemiProductAdvancePurchaseRepository.java`
- Create: `src/main/java/com/servtech/sinker/controller/SemiProductController.java`
- Create: `src/main/java/com/servtech/sinker/service/SemiProductService.java`
- Create: `src/main/java/com/servtech/sinker/util/SemiProductExcelParser.java`
- Create: `src/main/java/com/servtech/sinker/dto/SemiProductDTO.java`
- Create: `src/main/java/com/servtech/sinker/dto/SemiProductUpdateDTO.java`

## Dependencies
- T034: Database migration for semi_product_advance_purchase table
