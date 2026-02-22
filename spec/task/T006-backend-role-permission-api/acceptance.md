# T006: Acceptance Criteria

## Functional Acceptance Criteria

### List Roles (GET /api/roles)
- [ ] Endpoint requires authentication (401 if no token)
- [ ] Endpoint requires role.view permission (403 if missing)
- [ ] Returns all roles (no pagination needed, small dataset)
- [ ] Each role includes: id, code, name, description, is_system, is_active
- [ ] Roles ordered by id or code
- [ ] Returns 200 with array of roles

### Get Role Detail (GET /api/roles/:id)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires role.view permission
- [ ] Returns role with full details
- [ ] Includes array of assigned permissions
- [ ] Permissions include: id, code, name, module
- [ ] Permissions optionally grouped by module
- [ ] Returns 404 if role not found
- [ ] Returns 200 with role details

### Update Role (PUT /api/roles/:id)
- [ ] Endpoint requires authentication
- [ ] Endpoint requires role.edit permission
- [ ] Returns 404 if role not found
- [ ] Allows updating name
- [ ] Allows updating description
- [ ] Allows updating permissions (array of permission IDs)
- [ ] Validates all permission IDs exist (400 if invalid)
- [ ] Updates role_permissions table (delete old, insert new)
- [ ] Update is atomic (transaction)
- [ ] Returns 200 with updated role details
- [ ] updated_at timestamp updated

### System Role Protection
- [ ] Cannot change code of system role (is_system=TRUE)
- [ ] Cannot change is_system flag
- [ ] Can update name, description, permissions of system role
- [ ] Returns 400 if attempting to change protected fields

## API Contracts

### GET /api/roles

**Request:**
```
GET /api/roles
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "roles": [
    {
      "id": 1,
      "code": "admin",
      "name": "Administrator",
      "description": "Full system access",
      "isSystem": true,
      "isActive": true
    },
    {
      "id": 2,
      "code": "sales",
      "name": "Sales",
      "description": "Sales team member",
      "isSystem": true,
      "isActive": true
    }
  ]
}
```

### GET /api/roles/:id

**Request:**
```
GET /api/roles/1
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "id": 1,
  "code": "admin",
  "name": "Administrator",
  "description": "Full system access",
  "isSystem": true,
  "isActive": true,
  "permissions": [
    {
      "id": 1,
      "code": "user.view",
      "name": "View Users",
      "module": "user"
    },
    {
      "id": 2,
      "code": "user.create",
      "name": "Create User",
      "module": "user"
    }
  ],
  "permissionsByModule": {
    "user": [
      {"id": 1, "code": "user.view", "name": "View Users"},
      {"id": 2, "code": "user.create", "name": "Create User"}
    ],
    "role": [
      {"id": 5, "code": "role.view", "name": "View Roles"},
      {"id": 6, "code": "role.edit", "name": "Edit Roles"}
    ]
  }
}
```

### PUT /api/roles/:id

**Request:**
```json
{
  "name": "Administrator Updated",
  "description": "Updated description",
  "permissionIds": [1, 2, 3, 5, 6]
}
```

**Response 200:**
```json
{
  "id": 1,
  "code": "admin",
  "name": "Administrator Updated",
  "description": "Updated description",
  "isSystem": true,
  "isActive": true,
  "permissions": [
    {"id": 1, "code": "user.view", "name": "View Users", "module": "user"},
    {"id": 2, "code": "user.create", "name": "Create User", "module": "user"}
  ]
}
```

**Response 400 (Invalid Permission ID):**
```json
{
  "timestamp": "2026-02-15T14:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid permission IDs: [999]",
  "path": "/api/roles/1"
}
```

**Response 404 (Role Not Found):**
```json
{
  "timestamp": "2026-02-15T14:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Role not found with id: 999",
  "path": "/api/roles/999"
}
```

## UI Acceptance Criteria
N/A - This is backend only. Frontend integration covered in T009.

## Non-Functional Criteria

### Security
- [ ] All endpoints protected by authentication
- [ ] Permission checks enforced
- [ ] System role code cannot be changed
- [ ] Prevent permission escalation attacks

### Validation
- [ ] Permission IDs validated against database
- [ ] Name and description length validated
- [ ] Empty permission array allowed (role with no permissions)
- [ ] Null checks for required fields

### Performance
- [ ] List roles responds in under 200ms
- [ ] Get role detail responds in under 300ms (includes join)
- [ ] Update role responds in under 500ms
- [ ] Batch insert for role_permissions if updating large permission set

### Data Integrity
- [ ] Role update is transactional (all or nothing)
- [ ] Orphaned role_permissions cleaned up
- [ ] UNIQUE constraint on role_permissions prevents duplicates
- [ ] Foreign key constraints maintained

### Error Handling
- [ ] 400 for validation errors
- [ ] 401 for missing/invalid token
- [ ] 403 for missing permissions
- [ ] 404 for role not found
- [ ] 500 for unexpected errors (with logging)

## How to Verify

### Manual Testing with cURL

**List roles:**
```bash
TOKEN="<your-jwt-token>"
curl -X GET "http://localhost:8080/api/roles" \
  -H "Authorization: Bearer $TOKEN"
```

**Get role detail:**
```bash
curl -X GET "http://localhost:8080/api/roles/1" \
  -H "Authorization: Bearer $TOKEN"
```

**Update role:**
```bash
curl -X PUT "http://localhost:8080/api/roles/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Administrator Updated",
    "description": "Updated description",
    "permissionIds": [1, 2, 3, 4, 5, 6, 7, 8]
  }'
```

**Update with invalid permission ID:**
```bash
curl -X PUT "http://localhost:8080/api/roles/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "permissionIds": [1, 2, 999]
  }'
# Should return 400
```

### Database Verification

**Check role_permissions updated:**
```sql
-- Before update
SELECT COUNT(*) FROM role_permissions WHERE role_id=1;

-- After update (via API)
SELECT COUNT(*) FROM role_permissions WHERE role_id=1;

-- Verify specific permissions
SELECT p.code
FROM permissions p
JOIN role_permissions rp ON p.id = rp.permission_id
WHERE rp.role_id = 1
ORDER BY p.module, p.code;
```

**Verify updated_at changed:**
```sql
SELECT name, updated_at FROM roles WHERE id=1;
```

### Integration Test Execution
```bash
cd backend
./mvnw test -Dtest=RoleControllerTest
./mvnw test -Dtest=RoleServiceTest
```

### Permission Tests
```bash
# Test with user lacking role.view permission
# Should return 403 for GET /api/roles

# Test with user lacking role.edit permission
# Should return 403 for PUT /api/roles/:id
```
