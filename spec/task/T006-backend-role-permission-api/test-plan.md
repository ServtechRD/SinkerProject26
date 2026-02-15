# T006: Test Plan

## Unit Tests

### RoleServiceTest
- **Test: getAllRoles**
  - When getAllRoles called
  - Then returns all roles from database
  - Then roles include id, code, name, description, is_system, is_active

- **Test: getRoleById - found**
  - Given existing role ID
  - When getRoleById called
  - Then returns RoleDetailDTO
  - Then includes assigned permissions

- **Test: getRoleById - not found**
  - Given non-existent role ID
  - When getRoleById called
  - Then throws RoleNotFoundException

- **Test: getRoleById - permissions grouped by module**
  - Given role with multiple permissions across modules
  - When getRoleById called
  - Then permissions grouped by module in response

- **Test: updateRole - valid update**
  - Given existing role
  - Given valid UpdateRoleRequest
  - When updateRole called
  - Then role name and description updated
  - Then role_permissions deleted and recreated
  - Then updated_at timestamp updated
  - Then returns updated RoleDetailDTO

- **Test: updateRole - invalid permission IDs**
  - Given permission IDs that don't exist
  - When updateRole called
  - Then throws ValidationException or InvalidPermissionException

- **Test: updateRole - empty permissions**
  - Given empty permissionIds array
  - When updateRole called
  - Then all role_permissions removed
  - Then role updated successfully

- **Test: updateRole - not found**
  - Given non-existent role ID
  - When updateRole called
  - Then throws RoleNotFoundException

- **Test: updateRole - transaction rollback on error**
  - Given update that will fail (e.g., DB constraint)
  - When updateRole called
  - Then exception thrown
  - Then role_permissions not modified (transaction rolled back)

### RolePermissionUpdateTest (specific transaction test)
- **Test: role permissions update is atomic**
  - Given role with existing permissions
  - When update fails midway (mock exception)
  - Then original permissions remain unchanged
  - Then no partial update occurs

## Integration Tests

### RoleControllerIntegrationTest (with @SpringBootTest and Testcontainers)
- **Test: GET /api/roles - successful list**
  - Given authenticated user with role.view permission
  - When GET /api/roles
  - Then status 200
  - Then response contains all roles
  - Then includes admin, sales, production_planner, procurement

- **Test: GET /api/roles - unauthorized**
  - Given no authentication token
  - When GET /api/roles
  - Then status 401

- **Test: GET /api/roles - forbidden**
  - Given authenticated user without role.view permission
  - When GET /api/roles
  - Then status 403

- **Test: GET /api/roles/:id - successful**
  - Given existing role ID (e.g., admin role)
  - When GET /api/roles/{id}
  - Then status 200
  - Then response contains role details
  - Then response contains permissions array
  - Then permissions include module grouping

- **Test: GET /api/roles/:id - not found**
  - Given non-existent role ID
  - When GET /api/roles/{id}
  - Then status 404

- **Test: PUT /api/roles/:id - successful update**
  - Given authenticated user with role.edit permission
  - Given existing role ID
  - Given valid UpdateRoleRequest with new name, description, permissions
  - When PUT /api/roles/{id}
  - Then status 200
  - Then role updated in database
  - Then role_permissions updated in database
  - Then response contains updated role

- **Test: PUT /api/roles/:id - update permissions only**
  - Given role with existing permissions
  - Given UpdateRoleRequest with only permissionIds
  - When PUT /api/roles/{id}
  - Then permissions updated
  - Then name and description unchanged

- **Test: PUT /api/roles/:id - invalid permission IDs**
  - Given permissionIds including non-existent IDs
  - When PUT /api/roles/{id}
  - Then status 400
  - Then error message lists invalid IDs

- **Test: PUT /api/roles/:id - not found**
  - Given non-existent role ID
  - When PUT /api/roles/{id}
  - Then status 404

- **Test: PUT /api/roles/:id - remove all permissions**
  - Given role with permissions
  - Given empty permissionIds array
  - When PUT /api/roles/{id}
  - Then status 200
  - Then all role_permissions removed
  - Then role still exists with no permissions

- **Test: PUT /api/roles/:id - unauthorized**
  - Given no authentication token
  - When PUT /api/roles/{id}
  - Then status 401

- **Test: PUT /api/roles/:id - forbidden**
  - Given user without role.edit permission
  - When PUT /api/roles/{id}
  - Then status 403

### Permission Grouping Test
- **Test: permissions grouped by module correctly**
  - Given admin role with all 29 permissions
  - When GET /api/roles/1
  - Then permissionsByModule contains 10 modules
  - Then user module has 4 permissions
  - Then role module has 4 permissions
  - Then sales_forecast module has 6 permissions

## E2E Tests
N/A - E2E testing for role management covered in T009 (frontend integration).

## Test Data Setup

### Database Seeding for Tests
Use Flyway migrations (V1, V2) which already seed:
- 4 roles: admin, sales, production_planner, procurement
- 29 permissions across 10 modules
- admin role with all permissions

Additional test data:
```sql
-- Create test role without permissions for testing
INSERT INTO roles (code, name, description, is_system, is_active)
VALUES ('test_role', 'Test Role', 'For testing', FALSE, TRUE);
```

### Test Permissions
- Admin user with role.view and role.edit permissions
- Limited user with only role.view permission
- User with no role permissions (for 403 tests)

## Mocking Strategy

### Unit Tests
- Mock RoleRepository
- Mock PermissionRepository
- Mock RolePermissionRepository
- Do NOT mock transactional behavior - test actual transactions

### Integration Tests
- Do NOT mock repositories - use Testcontainers
- Do NOT mock Spring Security - use test users with real permissions
- Use @Transactional for test isolation
- Use real database with Flyway migrations

### Test Isolation
- Use @Transactional with rollback on tests
- Or clean up test data in @AfterEach
- Ensure test role updates don't affect other tests

## Additional Test Scenarios

### Data Integrity Tests
- **Test: concurrent role updates**
  - Update same role from two threads
  - Verify no lost updates
  - Verify database constraints prevent corruption

- **Test: orphaned role_permissions cleanup**
  - Create role_permissions
  - Update role with new permissions
  - Verify old role_permissions deleted

- **Test: UNIQUE constraint on role_permissions**
  - Attempt to insert duplicate (role_id, permission_id)
  - Verify constraint prevents duplicate

### Edge Cases
- **Test: update role with same permissions**
  - Given role with permissions [1,2,3]
  - Update with same permissions [1,2,3]
  - Verify no errors
  - Verify update_at changed

- **Test: update role with reordered permissions**
  - Given permissions [1,2,3]
  - Update with [3,2,1]
  - Verify same permissions assigned (order doesn't matter)

- **Test: update with duplicate permission IDs in request**
  - Given permissionIds [1,2,2,3]
  - When update called
  - Verify only unique permissions assigned

- **Test: update with null permission IDs in array**
  - Given permissionIds [1, null, 3]
  - Verify handled gracefully (filtered or validation error)

### Validation Tests
- **Test: name too long**
  - Given name exceeding VARCHAR(100)
  - When update called
  - Then validation error

- **Test: description too long**
  - Given description exceeding TEXT limit
  - When update called
  - Then validation error

- **Test: empty name**
  - Given empty or null name
  - When update called
  - Then validation error

### Performance Tests
- **Test: update role with all 29 permissions**
  - Measure time to delete and insert 29 role_permissions
  - Should complete in under 500ms

- **Test: batch insert performance**
  - If using batch insert for role_permissions
  - Verify faster than individual inserts

### System Role Protection Tests
- **Test: cannot change system role code**
  - Given system role (is_system=TRUE)
  - Attempt to change code in update
  - Verify code unchanged or 400 error

- **Test: can update system role permissions**
  - Given system role
  - Update permissions
  - Verify allowed and successful

- **Test: cannot change is_system flag**
  - Attempt to set is_system=FALSE for admin role
  - Verify rejected or ignored
