# T001: Acceptance Criteria

## Functional Acceptance Criteria

### Database Schema
- [ ] users table created with all 15 columns as specified
- [ ] roles table created with 7 columns as specified
- [ ] permissions table created with 7 columns as specified
- [ ] role_permissions table created with 4 columns as specified
- [ ] All PRIMARY KEY constraints applied
- [ ] All FOREIGN KEY constraints applied with correct cascade behavior
- [ ] All UNIQUE constraints applied (username, email, role code, permission code, role_permission pair)
- [ ] All DEFAULT values configured correctly

### Seed Data - Roles
- [ ] 4 roles created: admin, sales, production_planner, procurement
- [ ] All seeded roles have is_system=TRUE
- [ ] All seeded roles have is_active=TRUE
- [ ] Each role has appropriate name and description

### Seed Data - Permissions
- [ ] 4 user module permissions seeded
- [ ] 4 role module permissions seeded
- [ ] 6 sales_forecast module permissions seeded
- [ ] 2 sales_forecast_config module permissions seeded
- [ ] 2 production_plan module permissions seeded
- [ ] 2 inventory module permissions seeded
- [ ] 3 weekly_schedule module permissions seeded
- [ ] 3 semi_product module permissions seeded
- [ ] 1 material_demand module permission seeded
- [ ] 2 material_purchase module permissions seeded
- [ ] All permissions have correct module assignment
- [ ] All permissions are active (is_active=TRUE)

### Seed Data - Admin User
- [ ] Admin user created with username 'admin'
- [ ] Admin user email set appropriately
- [ ] Password is bcrypt hashed (starts with $2a$ or $2b$)
- [ ] Admin user is_active=TRUE
- [ ] Admin user is_locked=FALSE
- [ ] Admin user assigned to admin role

### Seed Data - Role Permissions
- [ ] Admin role mapped to ALL permissions in role_permissions table
- [ ] No duplicate role_permission entries

## Non-Functional Criteria

### Data Integrity
- [ ] Cannot insert duplicate username
- [ ] Cannot insert duplicate email
- [ ] Cannot insert duplicate role code
- [ ] Cannot insert duplicate permission code
- [ ] Cannot insert duplicate role_permission pair
- [ ] Foreign key constraints prevent orphaned records
- [ ] Deleting a role cascades to role_permissions
- [ ] Deleting a permission cascades to role_permissions

### Performance
- [ ] Indexes created on username, email for fast lookups
- [ ] Indexes created on role.code, permission.code
- [ ] Index on permission.module for grouped queries

### Migration
- [ ] V1 migration runs successfully on clean database
- [ ] V2 migration runs successfully after V1
- [ ] Migrations are idempotent (can verify with flyway:validate)
- [ ] No SQL syntax errors
- [ ] Migrations complete in under 5 seconds

## How to Verify

### Run Migrations
```bash
cd backend
./mvnw flyway:migrate
```

### Verify Tables Created
```sql
SHOW TABLES;
-- Should show: users, roles, permissions, role_permissions

DESCRIBE users;
DESCRIBE roles;
DESCRIBE permissions;
DESCRIBE role_permissions;
```

### Verify Constraints
```sql
SHOW CREATE TABLE users;
SHOW CREATE TABLE role_permissions;
-- Check for UNIQUE, FOREIGN KEY constraints
```

### Verify Seed Data
```sql
SELECT COUNT(*) FROM roles; -- Should be 4
SELECT COUNT(*) FROM permissions; -- Should be 29
SELECT COUNT(*) FROM users WHERE username='admin'; -- Should be 1
SELECT COUNT(*) FROM role_permissions WHERE role_id=(SELECT id FROM roles WHERE code='admin');
-- Should be 29 (all permissions)
```

### Verify Password Hash
```sql
SELECT username, hashed_password FROM users WHERE username='admin';
-- hashed_password should start with $2a$ or $2b$ and be 60 characters
```

### Test Constraints
```sql
-- Should fail: duplicate username
INSERT INTO users (username, email, hashed_password, full_name)
VALUES ('admin', 'another@email.com', 'hash', 'Test');

-- Should fail: duplicate role code
INSERT INTO roles (code, name) VALUES ('admin', 'Another Admin');
```
