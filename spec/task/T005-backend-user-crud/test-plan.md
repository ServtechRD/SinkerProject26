# T005: Test Plan

## Unit Tests

### UserServiceTest
- **Test: createUser with valid data**
  - Given valid CreateUserRequest
  - When createUser called
  - Then user saved to database
  - Then password is bcrypt hashed
  - Then created_by set to current user
  - Then UserDTO returned (no hashed_password)

- **Test: createUser with duplicate username**
  - Given username that already exists
  - When createUser called
  - Then DuplicateUsernameException thrown
  - Then user not saved

- **Test: createUser with duplicate email**
  - Given email that already exists
  - When createUser called
  - Then DuplicateEmailException thrown

- **Test: createUser with invalid email format**
  - Given invalid email
  - When createUser called
  - Then ValidationException thrown

- **Test: createUser with sales role without channels**
  - Given roleId for sales role
  - Given channels array is null or empty
  - When createUser called
  - Then ValidationException thrown with message about channels

- **Test: createUser with sales role with valid channels**
  - Given roleId for sales role
  - Given channels array with valid channels
  - When createUser called
  - Then user created successfully
  - (If T007 done) Then sales_channels_users records created

- **Test: updateUser with valid data**
  - Given existing user
  - Given valid UpdateUserRequest
  - When updateUser called
  - Then user updated in database
  - Then updated_at timestamp updated
  - Then UserDTO returned

- **Test: updateUser with password**
  - Given UpdateUserRequest with new password
  - When updateUser called
  - Then password bcrypt hashed
  - Then hashed_password updated in database

- **Test: updateUser without password**
  - Given UpdateUserRequest without password
  - When updateUser called
  - Then hashed_password not changed

- **Test: updateUser with duplicate username**
  - Given username already taken by another user
  - When updateUser called
  - Then DuplicateUsernameException thrown

- **Test: updateUser user not found**
  - Given non-existent user ID
  - When updateUser called
  - Then UserNotFoundException thrown

- **Test: deleteUser**
  - Given existing user ID
  - When deleteUser called
  - Then user deleted from database

- **Test: deleteUser not found**
  - Given non-existent user ID
  - When deleteUser called
  - Then UserNotFoundException thrown

- **Test: toggleUserStatus**
  - Given user with is_active=TRUE
  - When toggleUserStatus called
  - Then is_active set to FALSE
  - Then updated_at updated

- **Test: getUserById**
  - Given existing user ID
  - When getUserById called
  - Then UserDTO returned
  - Then DTO does not contain hashed_password

- **Test: listUsers with pagination**
  - Given 50 users in database
  - When listUsers called with page=0, size=20
  - Then 20 users returned
  - Then totalElements=50
  - Then totalPages=3

- **Test: listUsers with keyword search**
  - Given users with various usernames
  - When listUsers called with keyword="john"
  - Then only users matching "john" in username/fullName/email returned

- **Test: listUsers with role filter**
  - Given users with different roles
  - When listUsers called with roleId=2
  - Then only users with roleId=2 returned

- **Test: listUsers with isActive filter**
  - Given mix of active and inactive users
  - When listUsers called with isActive=true
  - Then only active users returned

- **Test: listUsers with sorting**
  - Given users with different usernames
  - When listUsers called with sortBy=username, sortOrder=asc
  - Then users sorted alphabetically by username

## Integration Tests

### UserControllerIntegrationTest (with @SpringBootTest and Testcontainers)
- **Test: GET /api/users - successful list**
  - Given authenticated user with user.view permission
  - When GET /api/users
  - Then status 200
  - Then response contains users array
  - Then response contains pagination metadata

- **Test: GET /api/users - unauthorized**
  - Given no authentication token
  - When GET /api/users
  - Then status 401

- **Test: GET /api/users - forbidden**
  - Given authenticated user without user.view permission
  - When GET /api/users
  - Then status 403

- **Test: GET /api/users - with search**
  - Given users in database
  - When GET /api/users?keyword=admin
  - Then status 200
  - Then response contains only matching users

- **Test: GET /api/users/:id - successful**
  - Given existing user ID
  - When GET /api/users/{id}
  - Then status 200
  - Then response contains user details
  - Then hashed_password not in response

- **Test: GET /api/users/:id - not found**
  - Given non-existent user ID
  - When GET /api/users/{id}
  - Then status 404

- **Test: POST /api/users - successful creation**
  - Given authenticated user with user.create permission
  - Given valid CreateUserRequest body
  - When POST /api/users
  - Then status 201
  - Then response contains created user
  - Then user exists in database
  - Then password is bcrypt hashed in database

- **Test: POST /api/users - duplicate username**
  - Given username that already exists
  - When POST /api/users
  - Then status 409
  - Then error message mentions username

- **Test: POST /api/users - duplicate email**
  - Given email that already exists
  - When POST /api/users
  - Then status 409
  - Then error message mentions email

- **Test: POST /api/users - validation errors**
  - Given missing required fields
  - When POST /api/users
  - Then status 400
  - Then error messages list validation failures

- **Test: POST /api/users - sales role without channels**
  - Given roleId for sales role
  - Given no channels in request
  - When POST /api/users
  - Then status 400
  - Then error message about channels requirement

- **Test: PUT /api/users/:id - successful update**
  - Given existing user
  - Given valid UpdateUserRequest
  - When PUT /api/users/{id}
  - Then status 200
  - Then user updated in database
  - Then response contains updated user

- **Test: PUT /api/users/:id - not found**
  - Given non-existent user ID
  - When PUT /api/users/{id}
  - Then status 404

- **Test: PUT /api/users/:id - duplicate username**
  - Given username already taken by another user
  - When PUT /api/users/{id}
  - Then status 409

- **Test: DELETE /api/users/:id - successful deletion**
  - Given existing user
  - When DELETE /api/users/{id}
  - Then status 204
  - Then user not in database

- **Test: DELETE /api/users/:id - not found**
  - Given non-existent user ID
  - When DELETE /api/users/{id}
  - Then status 404

- **Test: PATCH /api/users/:id/toggle - successful toggle**
  - Given user with is_active=TRUE
  - When PATCH /api/users/{id}/toggle
  - Then status 200
  - Then is_active=FALSE in response
  - Then is_active=FALSE in database

- **Test: PATCH /api/users/:id/toggle - not found**
  - Given non-existent user ID
  - When PATCH /api/users/{id}/toggle
  - Then status 404

### Permission Tests
- **Test: user.view permission required for list**
  - Given user without user.view permission
  - When GET /api/users
  - Then status 403

- **Test: user.create permission required for create**
  - Given user without user.create permission
  - When POST /api/users
  - Then status 403

- **Test: user.edit permission required for update**
  - Given user without user.edit permission
  - When PUT /api/users/{id}
  - Then status 403

- **Test: user.delete permission required for delete**
  - Given user without user.delete permission
  - When DELETE /api/users/{id}
  - Then status 403

## E2E Tests
N/A - E2E testing for user management covered in T008 (frontend integration).

## Test Data Setup

### Database Seeding for Tests
Use Testcontainers with Flyway migrations plus additional test data:

```sql
-- Test users (in addition to seeded admin)
INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked)
VALUES
  ('testuser1', 'test1@example.com', '$2a$10$hash1', 'Test User 1', 1, TRUE, FALSE),
  ('testuser2', 'test2@example.com', '$2a$10$hash2', 'Test User 2', 2, TRUE, FALSE),
  ('inactive_user', 'inactive@example.com', '$2a$10$hash3', 'Inactive User', 1, FALSE, FALSE);
```

### Test Roles
- Ensure all 4 roles from seed data exist (admin, sales, production_planner, procurement)

### Test Permissions
- Create test user with admin role (has all permissions)
- Create test user with limited permissions (only user.view)
- Create test user with no user permissions (for 403 tests)

## Mocking Strategy

### Unit Tests
- Mock UserRepository
- Mock PasswordEncoder
- Mock AuthenticationFacade or SecurityContext for current user
- Mock SalesChannelUserRepository (if T007 implemented)
- Do NOT mock UserService in controller tests - use real service with mocked repos

### Integration Tests
- Do NOT mock repositories - use Testcontainers
- Do NOT mock Spring Security - configure test users with roles/permissions
- Use @WithMockUser or custom security test annotations
- Use real BCryptPasswordEncoder

### Test Isolation
- Use @Transactional on test methods for auto-rollback
- Or manually clean up created test data in @AfterEach
- Reset database state between test classes if needed

## Additional Test Scenarios

### Validation Tests
- Test maximum length constraints (username 50, email 100, etc.)
- Test minimum password length
- Test special characters in username/email
- Test null vs empty string handling
- Test whitespace trimming

### Business Logic Tests
- Test created_by is set correctly
- Test timestamps are set automatically
- Test default values (is_active, is_locked, failed_login_count)
- Test role assignment
- Test sales channels validation (all 12 valid channels)

### Edge Cases
- Update user to same username (should succeed)
- Update user to same email (should succeed)
- Create user with minimal fields
- Create user with all fields
- Delete non-existent user (idempotent)
- Pagination with page out of bounds
- Search with special characters in keyword
- Empty result sets

### Concurrency Tests
- Test concurrent user creation with same username
- Test concurrent updates to same user
- Verify database constraints prevent race conditions

### Performance Tests
- Test list with 10,000 users (should still be fast)
- Test search with complex keyword
- Test sorting large datasets
