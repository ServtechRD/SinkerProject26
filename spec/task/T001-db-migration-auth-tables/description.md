# T001: Database Migration - Auth Tables

## Context
This is the foundational database schema for the authentication and authorization system. The project uses MariaDB 10.11 with Flyway for migration management. This establishes the core tables needed for user management, role-based access control (RBAC), and permissions.

## Goal
Create Flyway migration scripts to establish the users, roles, permissions, and role_permissions tables with proper constraints, indexes, and seed data for initial system bootstrap.

## Scope

### In Scope
- DDL for users table with all specified fields
- DDL for roles table
- DDL for permissions table
- DDL for role_permissions junction table with cascade delete
- Seed data for 4 system roles: admin, sales, production_planner, procurement
- Seed data for all permissions across 9 modules (user, role, sales_forecast, sales_forecast_config, production_plan, inventory, weekly_schedule, semi_product, material_demand, material_purchase)
- Seed data for admin user (username: admin, password: admin123)
- Seed data for role_permissions mapping (admin gets all permissions)
- Proper foreign key constraints
- Indexes on frequently queried columns

### Out of Scope
- sales_channels_users table (covered in T007)
- login_logs table (covered in T007)
- Application-level code
- Data validation logic (handled by application)

## Requirements
- Create V1__create_auth_tables.sql for table DDL
- Create V2__seed_auth_data.sql for initial data
- users.username must be UNIQUE, VARCHAR(50)
- users.email must be UNIQUE, VARCHAR(100)
- users.hashed_password VARCHAR(255) to accommodate bcrypt
- users.is_active defaults to TRUE
- users.is_locked defaults to FALSE
- users.failed_login_count defaults to 0
- roles.code must be UNIQUE, VARCHAR(50)
- roles.is_system defaults to FALSE for system roles (set TRUE for seeded roles)
- permissions.code must be UNIQUE, VARCHAR(100)
- permissions.module VARCHAR(50) for grouping
- role_permissions must have UNIQUE constraint on (role_id, permission_id)
- Foreign keys must have appropriate CASCADE/SET NULL behavior
- created_at and updated_at timestamps on all tables
- Admin user password must be bcrypt hashed

## Implementation Notes
- Use InnoDB engine for all tables
- Set DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
- Create indexes on: users.username, users.email, roles.code, permissions.code, permissions.module
- Hash admin password using bcrypt cost factor 10
- Seed permissions in logical module order
- Map all permissions to admin role in role_permissions
- Use AUTO_INCREMENT for all id columns
- Self-referential FK on users.created_by should allow NULL for bootstrap

## Files to Change
- backend/src/main/resources/db/migration/V1__create_auth_tables.sql (new)
- backend/src/main/resources/db/migration/V2__seed_auth_data.sql (new)

## Dependencies
None - This is the first task establishing the foundation.
