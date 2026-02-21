# Database Migration Guide

This document explains the database schema management using Flyway migrations in SinkerProject26.

## Overview

The project uses **Flyway** for database version control and migrations. All migrations are applied automatically when the backend service starts.

- **Database:** MariaDB 10
- **Migration Tool:** Flyway 9.x (included in Spring Boot)
- **Location:** `backend/src/main/resources/db/migration/`
- **Naming Convention:** `V<VERSION>__<description>.sql`

## Migration Files

Current migrations in the repository:

| File | Version | Description |
|------|---------|-------------|
| `V1__create_auth_tables.sql` | 1 | Creates authentication tables (users, roles) |
| `V2__seed_auth_data.sql` | 2 | Seeds initial auth data (admin user, roles) |
| `V3__create_channels_loginlogs.sql` | 3 | Creates channels and login logs tables |
| `V4__create_sales_forecast_config.sql` | 4 | Creates sales forecast configuration tables |
| `V5__create_sales_forecast.sql` | 5 | Creates sales forecast tables |
| `V6__create_inventory_sales_forecast.sql` | 6 | Creates inventory sales forecast tables |
| `V7__create_production_plan.sql` | 7 | Creates production planning tables |
| `V8__create_weekly_schedule.sql` | 8 | Creates weekly schedule tables |
| `V9__create_semi_product.sql` | 9 | Creates semi-product tables |
| `V10__create_material_demand.sql` | 10 | Creates material demand tables |
| `V11__create_material_purchase.sql` | 11 | Creates material purchase tables |

## Flyway Naming Convention

Migration files **must** follow this pattern:

```
V<VERSION>__<description>.sql

Examples:
V1__create_auth_tables.sql        ✅ Valid
V12__add_new_feature.sql          ✅ Valid
V2.1__hotfix_users.sql            ✅ Valid (with dot notation)
v1__create_tables.sql             ❌ Invalid (lowercase v)
create_tables.sql                 ❌ Invalid (no version)
V1_create_tables.sql              ❌ Invalid (single underscore)
```

**Rules:**
- Version prefix: `V` (uppercase)
- Version number: Integer or dotted (e.g., `1`, `2.1`, `3.0.1`)
- Separator: Double underscore `__`
- Description: Snake_case or dash-separated
- Extension: `.sql`

## How Migrations Work

### Automatic Execution

Migrations run automatically when:
1. You start the backend service with `make dev-up`
2. Backend connects to the database
3. Flyway checks the `flyway_schema_history` table
4. New migrations (not yet applied) are executed in order

### Flyway Configuration

Configuration in `backend/src/main/resources/application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Database Initialization

### First-Time Setup

When you first start the application, Flyway will:

1. Create the `flyway_schema_history` table
2. Execute all migrations in order (V1 → V11)
3. Record each migration with checksum and timestamp

**Command:**
```bash
make dev-up
```

**Expected output in backend logs:**
```
Flyway Community Edition 9.x by Redgate
Database: jdbc:mariadb://db:3306/app (MariaDB 10)
Successfully validated 11 migrations (execution time 00:00.023s)
Creating Schema History table `app`.`flyway_schema_history` ...
Current version of schema `app`: << Empty Schema >>
Migrating schema `app` to version "1 - create auth tables"
Migrating schema `app` to version "2 - seed auth data"
...
Migrating schema `app` to version "11 - create material purchase"
Successfully applied 11 migrations to schema `app` (execution time 00:01.234s)
```

## Viewing Migration Status

### Check Migration History

Connect to the database and query the history table:

```bash
docker compose exec db mariadb -uapp -papp app -e "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

**Expected output:**
```
+----------------+---------+---------------------------+----------+---------------------+---------------+--------------+
| installed_rank | version | description               | type     | script              | checksum      | installed_on |
+----------------+---------+---------------------------+----------+---------------------+---------------+--------------+
|              1 | 1       | create auth tables        | SQL      | V1__create_auth...  |  -1234567890  | 2026-02-21   |
|              2 | 2       | seed auth data            | SQL      | V2__seed_auth_...  |  -987654321   | 2026-02-21   |
...
+----------------+---------+---------------------------+----------+---------------------+---------------+--------------+
```

### Check Current Schema Version

```bash
docker compose exec db mariadb -uapp -papp app -e "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;"
```

## Database Operations

### Connect to Database

**Using MariaDB CLI:**
```bash
docker compose exec db mariadb -uapp -papp app
```

**Using root credentials:**
```bash
docker compose exec db mariadb -uroot -proot app
```

### List All Tables

```bash
docker compose exec db mariadb -uapp -papp app -e "SHOW TABLES;"
```

**Expected output:**
```
+------------------------+
| Tables_in_app          |
+------------------------+
| channels               |
| flyway_schema_history  |
| inventory_sales_...    |
| login_logs             |
| material_demand        |
| material_purchase      |
| production_plan        |
| roles                  |
| sales_forecast         |
| sales_forecast_config  |
| semi_product           |
| users                  |
| weekly_schedule        |
+------------------------+
```

### Describe Table Structure

```bash
docker compose exec db mariadb -uapp -papp app -e "DESCRIBE users;"
```

### Export Database

**Full database dump:**
```bash
docker compose exec db mariadb-dump -uapp -papp app > backup.sql
```

**Schema only (no data):**
```bash
docker compose exec db mariadb-dump -uapp -papp --no-data app > schema.sql
```

### Import SQL File

**Import into running database:**
```bash
docker compose exec -T db mariadb -uapp -papp app < backup.sql
```

## Adding New Migrations

### Step 1: Create Migration File

1. Determine the next version number:
   ```bash
   # Check latest version
   ls -1 backend/src/main/resources/db/migration/ | sort -V | tail -1
   # Output: V11__create_material_purchase.sql
   # Next version: V12
   ```

2. Create new migration file:
   ```bash
   touch backend/src/main/resources/db/migration/V12__add_your_feature.sql
   ```

### Step 2: Write SQL

Example migration file:

```sql
-- V12: Add inventory tracking table

CREATE TABLE inventory_items (
    id          INT           NOT NULL AUTO_INCREMENT,
    item_code   VARCHAR(50)   NOT NULL,
    item_name   VARCHAR(100)  NOT NULL,
    quantity    INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_item_code (item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Step 3: Apply Migration

Restart the backend service to apply the new migration:

```bash
docker compose restart backend
```

Or rebuild and restart all services:

```bash
make dev-down
make dev-up
```

### Step 4: Verify Migration

Check the logs:

```bash
docker compose logs backend | grep -i flyway
```

**Expected output:**
```
Migrating schema `app` to version "12 - add your feature"
Successfully applied 1 migration to schema `app`
```

Verify in database:

```bash
docker compose exec db mariadb -uapp -papp app -e "SELECT * FROM flyway_schema_history WHERE version = '12';"
```

## Troubleshooting

### Issue: Migration Checksum Mismatch

**Symptoms:**
```
Migration checksum mismatch for migration version 5
Expected: -123456789
Actual:   -987654321
```

**Cause:** A previously applied migration file was modified.

**Solution:**
⚠️ **Never modify applied migrations in production!**

For development only:

1. Reset the database:
   ```bash
   make dev-down
   make dev-up
   ```

### Issue: Migration Failed

**Symptoms:**
```
Migration V12__add_feature.sql failed
SQL State: 42S01
Error Code: 1050
Message: Table 'inventory_items' already exists
```

**Solution:**

1. Check if the table already exists:
   ```bash
   docker compose exec db mariadb -uapp -papp app -e "SHOW TABLES LIKE 'inventory_items';"
   ```

2. If it exists, either:
   - Drop the table (development only):
     ```bash
     docker compose exec db mariadb -uapp -papp app -e "DROP TABLE inventory_items;"
     ```
   - Or modify the migration to use `CREATE TABLE IF NOT EXISTS`

3. Mark the failed migration as repaired (if applicable):
   ```bash
   # Connect to database
   docker compose exec db mariadb -uapp -papp app

   # Delete the failed migration record
   DELETE FROM flyway_schema_history WHERE version = '12' AND success = 0;
   ```

4. Restart backend to retry:
   ```bash
   docker compose restart backend
   ```

### Issue: Out-of-Order Migrations

**Symptoms:**
```
Detected applied migration not resolved locally: 12
```

**Cause:** A migration with a lower version number was added after higher versions were applied.

**Solution:**
- In development: Reset database with `make dev-down && make dev-up`
- In production: Use Flyway's out-of-order mode or create a new migration

### Issue: Cannot Connect to Database

**Symptoms:**
- Backend logs show connection errors
- Flyway cannot access database

**Solution:**

1. Verify database is healthy:
   ```bash
   docker compose ps db
   # Should show: Up (healthy)
   ```

2. Check database logs:
   ```bash
   docker compose logs db
   ```

3. Test connection manually:
   ```bash
   docker compose exec db mariadb -uapp -papp app -e "SELECT 1;"
   ```

4. Restart services in order:
   ```bash
   docker compose restart db
   sleep 10
   docker compose restart backend
   ```

## Makefile Targets for Database

These docker-first targets can be added to the Makefile for convenience:

### db-info: Show database information

```bash
make db-info
```

Shows:
- Migration history
- Current schema version
- Table list

### db-migrate: Force migration run

```bash
make db-migrate
```

Restarts backend to rerun migrations.

### db-shell: Open database shell

```bash
make db-shell
```

Opens MariaDB CLI for the `app` database.

## Best Practices

### DO:
✅ Create sequential version numbers (V1, V2, V3...)
✅ Use descriptive migration names
✅ Test migrations in development first
✅ Keep migrations idempotent when possible (`IF NOT EXISTS`)
✅ Include rollback plan in migration comments
✅ Review migration before committing

### DON'T:
❌ Modify migrations after they're applied
❌ Delete migration files
❌ Reuse version numbers
❌ Run migrations directly on production database
❌ Skip version numbers without good reason
❌ Use DML (INSERT/UPDATE/DELETE) in schema migrations (use separate seed migrations like V2)

## Related Documentation

- [STARTUP.md](./STARTUP.md) - Starting and stopping services
- [COVERAGE.md](./COVERAGE.md) - Running tests with coverage
- [Flyway Documentation](https://flywaydb.org/documentation/)
