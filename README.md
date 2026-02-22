# SinkerProject26

Project for Sinker 2026

## Overview

Full-stack web application with:
- **Backend:** Spring Boot 3.x + MariaDB (Java 17)
- **Frontend:** React + Vite
- **Infrastructure:** Docker + Docker Compose

## Quick Start

```bash
# Start all services
make dev-up

# Stop all services
make dev-down

# Run tests
make test-compose
```

Access the application:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- API Documentation: http://localhost:8080/swagger-ui.html

## Documentation

Comprehensive operational guides:

- **[📘 Startup Guide](docs/STARTUP.md)** - First-time setup, starting/stopping services, troubleshooting
- **[💾 Database Migration Guide](docs/DB_MIGRATION.md)** - Flyway migrations, database operations, schema management
- **[📊 Test Coverage Guide](docs/COVERAGE.md)** - Generating backend/frontend coverage reports (JaCoCo, Vitest)

## Available Commands

Run `make help` to see all available commands:

```bash
make help
```

## Development Notes

- **Docker-first:** All commands run through Docker (no host Java/Gradle/npm required)
- **Flyway Migrations:** Automatically applied on backend startup
- **Test Stack:** Isolated test environment with `docker-compose.test.yml`

---

check auto-merge in claude/intergration branch#4

add script to run all task

fix T019 merge
fix T020 merge