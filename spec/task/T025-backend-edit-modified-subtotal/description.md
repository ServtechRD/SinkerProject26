# T025: Backend - Edit Modified Subtotal

## Context
Users need the ability to manually adjust the production_subtotal calculated by the system (T024). When a user modifies the subtotal, the change is stored in the modified_subtotal field, and a new version is created to maintain audit trail and history of changes.

## Goal
Implement a REST API endpoint to update the modified_subtotal field of an inventory integration record and generate a new version with the modified data.

## Scope

### In Scope
- PUT endpoint to update modified_subtotal by record ID
- Generate new version when modification occurs
- Create new record with updated modified_subtotal (preserving original)
- Permission-based access control (inventory.edit)
- Return updated record in response
- Validation of modified_subtotal value

### Out of Scope
- Editing other fields (product info, quantities, etc.)
- Bulk edit operations
- Delete operations
- Recalculation of production_subtotal
- Version comparison or diff functionality

## Requirements
- **Endpoint**: PUT /api/inventory-integration/:id
- **Request Body**:
  ```json
  {
    "modifiedSubtotal": 200.50
  }
  ```
- **Business Logic**:
  - Load existing record by ID
  - Validate modified_subtotal is valid decimal
  - Copy all fields from original record
  - Update modified_subtotal to new value
  - Generate new version identifier
  - Save as new record (do NOT update existing record)
  - Return newly created record
- **Permission**: Requires inventory.edit permission
- **Validation**:
  - ID must exist
  - modified_subtotal must be valid DECIMAL(10,2)
  - modified_subtotal can be NULL (to clear modification)
- **Response**: Updated InventoryIntegrationDTO with new ID and version

## Implementation Notes
- Do not modify the original record - create a new record with new version
- This preserves audit trail of all modifications
- New version format: "v{timestamp}-modified" or continue existing version scheme
- The modified_subtotal field being NULL vs 0 is significant:
  - NULL = no manual modification
  - 0 = explicitly set to zero
- Update both updated_at and created_at for new record
- Original production_subtotal remains unchanged
- Return 404 if ID not found
- Return 400 if validation fails

## Files to Change
- `backend/src/main/java/com/servtech/sinker/controller/InventoryIntegrationController.java` (update)
- `backend/src/main/java/com/servtech/sinker/service/InventoryIntegrationService.java` (update)
- `backend/src/main/java/com/servtech/sinker/dto/UpdateModifiedSubtotalRequest.java` (new)

## Dependencies
- T024: Inventory integration API must exist
- Spring Security for permission checking
- Existing InventorySalesForecast entity and repository
