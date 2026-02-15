# T007: Database Migration - Sales Channels and Login Logs

## Context
This task creates the database tables for sales channel assignments and login audit logging. The sales_channels_users table supports the business requirement that sales role users must be assigned specific sales channels. The login_logs table enables security auditing and supports the account lockout feature (T010).

## Goal
Create Flyway migration scripts to establish the sales_channels_users and login_logs tables with proper constraints, indexes, and foreign key relationships.

## Scope

### In Scope
- DDL for sales_channels_users table
- DDL for login_logs table
- Foreign key constraints with appropriate cascade behavior
- UNIQUE constraint on (user_id, channel) for sales_channels_users
- Indexes on frequently queried columns
- ENUM type for login_type in login_logs
- Support for IPv4 and IPv6 addresses (VARCHAR 45)

### Out of Scope
- Seed data for sales channels (channels are values, not reference table)
- Application-level code for managing channels
- Login logging logic (covered in T010)
- Login log retention/archival policies
- Login log analytics or reporting queries

## Requirements
- Create V3__create_channels_loginlogs.sql migration
- sales_channels_users.user_id FK to users.id with CASCADE delete
- sales_channels_users.channel VARCHAR(50) for channel name
- sales_channels_users UNIQUE constraint on (user_id, channel)
- sales_channels_users.created_at timestamp
- login_logs.id BIGINT AUTO_INCREMENT (high volume table)
- login_logs.user_id FK to users.id with SET NULL (preserve logs if user deleted)
- login_logs.username VARCHAR(50) for denormalized username
- login_logs.login_type ENUM('success', 'failed')
- login_logs.ip_address VARCHAR(45) for IPv6 support
- login_logs.user_agent TEXT for browser info
- login_logs.failed_reason VARCHAR(255) for failure details
- login_logs.created_at timestamp for log entry time
- Indexes on: login_logs.user_id, login_logs.username, login_logs.created_at, login_logs.login_type

## Implementation Notes
- Use InnoDB engine for both tables
- Set DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
- CASCADE delete on sales_channels_users when user deleted (channels are tied to user)
- SET NULL on login_logs.user_id when user deleted (preserve audit trail)
- login_logs.username denormalized to preserve history even if user deleted
- Consider partitioning login_logs by created_at for future scalability (optional)
- Index on (user_id, created_at) for lockout queries (last N failed attempts)
- Index on created_at for log archival queries

## Files to Change
- backend/src/main/resources/db/migration/V3__create_channels_loginlogs.sql (new)

## Dependencies
- T001: Requires users table to exist for foreign keys
