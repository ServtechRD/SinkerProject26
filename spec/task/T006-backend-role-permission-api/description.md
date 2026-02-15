# T006: Backend Role and Permission Management API

## Context
This task implements the REST API for managing roles and permissions, allowing administrators to view roles, see assigned permissions, and update role-permission mappings. This builds on the RBAC foundation from T001 and user management from T005.

## Goal
Create endpoints to list roles, view role details with assigned permissions, and update role permissions. Implement permission-based access control for role management operations.

## Scope

### In Scope
- GET /api/roles - List all roles
- GET /api/roles/:id - Get role details including assigned permissions
- PUT /api/roles/:id - Update role (name, description, permissions)
- Permission checks: role.view, role.edit
- Business logic: prevent editing system roles (is_system=TRUE) name/code
- Return roles with permission arrays grouped by module
- Validate permission IDs exist before assignment
- Update role_permissions junction table on role update
- Return DTOs with structured permission data

### Out of Scope
- Create new roles (can be added later)
- Delete roles (can be added later)
- Role activation/deactivation toggle
- Role assignment to users (handled in user CRUD T005)
- Permission CRUD (permissions are seeded, not managed via API)
- Role hierarchy or inheritance
- Audit logging for permission changes

## Requirements
- All endpoints require authentication (JWT token)
- GET /api/roles requires role.view permission
- PUT /api/roles/:id requires role.edit permission
- List all roles with basic info (id, code, name, description, is_system, is_active)
- Role detail includes array of assigned permissions
- Permissions grouped by module in response for easier UI rendering
- Update allows changing: name, description, permissions
- Update does NOT allow changing: code, is_system (system roles are protected)
- Validate all permission IDs exist before updating role_permissions
- Delete existing role_permissions and insert new ones atomically
- Return 404 if role not found
- Return 400 if trying to change system role code
- Return 400 if invalid permission IDs provided

## Implementation Notes
- Use Spring Data JPA for database access
- Use @Transactional for role update (delete + insert role_permissions)
- Create RoleDTO with nested PermissionDTO array
- Group permissions by module in service layer before returning
- Create UpdateRoleRequest DTO with validation
- Use @PreAuthorize for permission checks
- Return 200 with updated role details on successful update
- Consider using batch insert for role_permissions for performance
- Use Hibernate cascade or manual delete/insert for role_permissions

## Files to Change
- controller/RoleController.java (new)
- service/RoleService.java (new)
- repository/RoleRepository.java (new or update if exists)
- repository/PermissionRepository.java (new)
- repository/RolePermissionRepository.java (new)
- entity/Role.java (new or update)
- entity/Permission.java (new)
- entity/RolePermission.java (new)
- dto/role/RoleDTO.java (new)
- dto/role/RoleDetailDTO.java (new)
- dto/role/PermissionDTO.java (new)
- dto/role/UpdateRoleRequest.java (new)
- exception/SystemRoleException.java (new)
- exception/RoleNotFoundException.java (new)

## Dependencies
- T005: Builds on user management and permission checks
- T001: Requires roles and permissions tables
