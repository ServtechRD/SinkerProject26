# T005: Acceptance Criteria

## Functional Acceptance Criteria

### List Users (GET /api/users)
- [ ] Endpoint requires authentication (401 if no token)
- [ ] Endpoint requires user.view permission (403 if missing)
- [ ] Returns paginated list of users
- [ ] Default page size is 20
- [ ] Supports page parameter (0-indexed)
- [ ] Supports size parameter
- [ ] Supports keyword search (searches username, full_name, email)
- [ ] Supports roleId filter
- [ ] Supports isActive filter (true/false/null for all)
- [ ] Supports sortBy parameter (username, created_at, etc.)
- [ ] Supports sortOrder parameter (asc/desc)
- [ ] Response includes: totalElements, totalPages, currentPage, users array
- [ ] Users in response do NOT include hashed_password

### Get User (GET /api/users/:id)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires user.view permission
- [ ] Returns user details for valid ID
- [ ] Returns 404 if user not found
- [ ] Response does NOT include hashed_password
- [ ] Response includes role information

### Create User (POST /api/users)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires user.create permission
- [ ] Validates username is required
- [ ] Validates username is unique (409 if duplicate)
- [ ] Validates username max 50 chars
- [ ] Validates email is required
- [ ] Validates email format
- [ ] Validates email is unique (409 if duplicate)
- [ ] Validates password is required
- [ ] Validates password min 6 characters
- [ ] Validates role_id is required and exists
- [ ] Password is bcrypt hashed before storage
- [ ] Returns 201 Created with user details
- [ ] created_by field set to current authenticated user ID
- [ ] created_at timestamp set automatically

### Create User - Sales Role Business Logic
- [ ] If role_id corresponds to "sales" role, channels array is required
- [ ] Validates channels against allowed list (12 channels)
- [ ] Returns 400 if sales role without channels
- [ ] (If T007 complete) Inserts records into sales_channels_users table

### Update User (PUT /api/users/:id)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires user.edit permission
- [ ] Returns 404 if user not found
- [ ] Validates username unique (excluding current user)
- [ ] Validates email unique (excluding current user)
- [ ] Password is optional (only hash if provided)
- [ ] Can update: username, email, password, full_name, role_id, department, phone
- [ ] Cannot update: id, created_by, created_at
- [ ] updated_at timestamp set automatically
- [ ] Returns 200 with updated user details

### Delete User (DELETE /api/users/:id)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires user.delete permission
- [ ] Returns 404 if user not found
- [ ] Deletes user record
- [ ] Returns 204 No Content on success
- [ ] (Optional) Prevents deletion if user has created other users
- [ ] (Optional) Soft delete by setting is_active=FALSE instead of hard delete

### Toggle User Status (PATCH /api/users/:id/toggle)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires user.edit permission
- [ ] Returns 404 if user not found
- [ ] Flips is_active from TRUE to FALSE or FALSE to TRUE
- [ ] Returns 200 with updated user details
- [ ] updated_at timestamp updated

## API Contracts

### GET /api/users

**Request:**
```
GET /api/users?page=0&size=20&keyword=john&roleId=2&isActive=true&sortBy=username&sortOrder=asc
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "users": [
    {
      "id": 2,
      "username": "john.doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "role": {
        "id": 2,
        "code": "sales",
        "name": "Sales"
      },
      "department": "Sales",
      "phone": "123-456-7890",
      "isActive": true,
      "isLocked": false,
      "lastLoginAt": "2026-02-14T10:30:00",
      "createdAt": "2026-02-01T09:00:00",
      "updatedAt": "2026-02-14T10:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0
}
```

### GET /api/users/:id

**Response 200:**
```json
{
  "id": 2,
  "username": "john.doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": {
    "id": 2,
    "code": "sales",
    "name": "Sales"
  },
  "department": "Sales",
  "phone": "123-456-7890",
  "isActive": true,
  "isLocked": false,
  "lastLoginAt": "2026-02-14T10:30:00",
  "createdAt": "2026-02-01T09:00:00",
  "updatedAt": "2026-02-14T10:30:00"
}
```

### POST /api/users

**Request:**
```json
{
  "username": "jane.smith",
  "email": "jane@example.com",
  "password": "password123",
  "fullName": "Jane Smith",
  "roleId": 2,
  "department": "Sales",
  "phone": "098-765-4321",
  "channels": ["PX/大全聯", "家樂福"]
}
```

**Response 201:**
```json
{
  "id": 3,
  "username": "jane.smith",
  "email": "jane@example.com",
  "fullName": "Jane Smith",
  "role": {
    "id": 2,
    "code": "sales",
    "name": "Sales"
  },
  "department": "Sales",
  "phone": "098-765-4321",
  "isActive": true,
  "isLocked": false,
  "createdAt": "2026-02-15T14:00:00",
  "updatedAt": "2026-02-15T14:00:00"
}
```

**Response 409 (Duplicate Username):**
```json
{
  "timestamp": "2026-02-15T14:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists",
  "path": "/api/users"
}
```

### PUT /api/users/:id

**Request:**
```json
{
  "email": "jane.new@example.com",
  "fullName": "Jane Smith Updated",
  "department": "Marketing"
}
```

**Response 200:** (same format as GET)

### DELETE /api/users/:id

**Response 204:** (No Content)

### PATCH /api/users/:id/toggle

**Response 200:** (same format as GET, with isActive flipped)

## UI Acceptance Criteria
N/A - This is backend only. Frontend integration covered in T008.

## Non-Functional Criteria

### Security
- [ ] hashed_password never returned in any response
- [ ] Password hashed with bcrypt cost 10-12
- [ ] All endpoints protected by authentication
- [ ] Permission checks enforced
- [ ] SQL injection prevented (use parameterized queries)

### Validation
- [ ] All validation errors return 400 with clear messages
- [ ] Email regex validation applied
- [ ] Username trimmed before uniqueness check
- [ ] Role ID validated against roles table
- [ ] Null checks for all required fields

### Performance
- [ ] List query with pagination uses indexed columns
- [ ] Keyword search uses indexed columns (username, email)
- [ ] List endpoint responds in under 500ms for 1000 users
- [ ] Create/Update endpoints respond in under 300ms

### Error Handling
- [ ] 400 for validation errors
- [ ] 401 for missing/invalid token
- [ ] 403 for missing permissions
- [ ] 404 for user not found
- [ ] 409 for uniqueness violations
- [ ] 500 for unexpected errors (with logging)

## How to Verify

### Manual Testing with cURL

**List users:**
```bash
TOKEN="<your-jwt-token>"
curl -X GET "http://localhost:8080/api/users?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Search users:**
```bash
curl -X GET "http://localhost:8080/api/users?keyword=admin" \
  -H "Authorization: Bearer $TOKEN"
```

**Get user by ID:**
```bash
curl -X GET "http://localhost:8080/api/users/1" \
  -H "Authorization: Bearer $TOKEN"
```

**Create user:**
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User",
    "roleId": 1
  }'
```

**Update user:**
```bash
curl -X PUT "http://localhost:8080/api/users/2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Updated Name"}'
```

**Toggle user status:**
```bash
curl -X PATCH "http://localhost:8080/api/users/2/toggle" \
  -H "Authorization: Bearer $TOKEN"
```

**Delete user:**
```bash
curl -X DELETE "http://localhost:8080/api/users/2" \
  -H "Authorization: Bearer $TOKEN"
```

### Integration Test Execution
```bash
cd backend
./mvnw test -Dtest=UserControllerTest
./mvnw test -Dtest=UserServiceTest
```

### Database Verification
```sql
-- Verify user created
SELECT * FROM users WHERE username='testuser';

-- Verify password hashed
SELECT username, hashed_password FROM users WHERE username='testuser';
-- Should start with $2a$ or $2b$

-- Verify created_by set
SELECT username, created_by FROM users WHERE username='testuser';
```
