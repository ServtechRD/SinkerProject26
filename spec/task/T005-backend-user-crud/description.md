# T005: Backend User CRUD API

## Context
This task implements the complete REST API for user management, enabling administrators to create, read, update, delete, and manage user accounts. It builds on the authentication foundation (T002) and adds permission-based access control for user management operations.

## Goal
Create a comprehensive user management API with list/search/filter/pagination, create, update, delete, and toggle active status endpoints. Implement business logic for validation, uniqueness checks, password hashing, and sales channel assignment for sales role users.

## Scope

### In Scope
- GET /api/users - List users with search, filter, sort, pagination
- GET /api/users/:id - Get user by ID
- POST /api/users - Create new user
- PUT /api/users/:id - Update existing user
- DELETE /api/users/:id - Delete user (soft or hard)
- PATCH /api/users/:id/toggle - Toggle is_active status
- Search by keyword (username, full_name, email)
- Filter by role_id, is_active status
- Sort by any column (username, created_at, etc.)
- Pagination (20 records per page)
- Permission checks: user.view, user.create, user.edit, user.delete
- Validation: username uniqueness, email uniqueness, email format
- Password hashing with bcrypt on create/update
- Business logic: sales role requires channels assignment
- Return DTOs (never expose hashed_password)

### Out of Scope
- User self-registration
- Password reset functionality
- User profile picture upload
- User activity history
- Bulk user import
- Sales channel management (separate endpoints, handled in data on create/update)
- Audit logging (can be added later)

## Requirements
- All endpoints require authentication (JWT token)
- GET /api/users requires user.view permission
- POST /api/users requires user.create permission
- PUT /api/users/:id requires user.edit permission
- DELETE /api/users/:id requires user.delete permission
- PATCH /api/users/:id/toggle requires user.edit permission
- List endpoint supports query params: page, size, keyword, roleId, isActive, sortBy, sortOrder
- Create validates: username unique, email unique and valid format, password min 6 chars, role_id exists
- Update validates: username unique (excluding current user), email unique (excluding current user)
- Create/Update: if roleCode is "sales", require channels array (validate against 12 allowed channels)
- Password is optional on update (only hash if provided)
- Delete should check if user has created other users (prevent deletion or cascade as appropriate)
- Toggle endpoint flips is_active boolean
- Return paginated response with totalElements, totalPages, currentPage
- DTOs should not include hashed_password, created_by details

## Implementation Notes
- Use Spring Data JPA for database access
- Use Page<User> for pagination
- Use Specification or QueryDSL for dynamic filtering
- Create UserDTO to exclude sensitive fields
- Create CreateUserRequest and UpdateUserRequest DTOs with validation annotations
- Use @PreAuthorize for permission checks
- Use BCryptPasswordEncoder to hash passwords
- For sales users, insert/update sales_channels_users table (requires T007, so handle gracefully if not available or add TODO comment)
- Map User entity to UserDTO in service layer
- Return 404 if user not found
- Return 400 for validation errors
- Return 409 for uniqueness constraint violations
- Use Pageable from Spring Data

## Files to Change
- controller/UserController.java (new)
- service/UserService.java (new)
- repository/UserRepository.java (update - may already exist from T002)
- dto/user/UserDTO.java (new)
- dto/user/CreateUserRequest.java (new)
- dto/user/UpdateUserRequest.java (new)
- dto/user/UserListResponse.java (new)
- exception/DuplicateUsernameException.java (new)
- exception/DuplicateEmailException.java (new)
- entity/User.java (update if needed)
- config/SecurityConfig.java (update if needed for permissions)

## Dependencies
- T002: Requires authentication and JWT implementation
- (Optional) T007: For sales_channels_users table (can stub or skip channel assignment for now)
