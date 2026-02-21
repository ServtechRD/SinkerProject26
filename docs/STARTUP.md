# SinkerProject26 Startup Guide

This document provides step-by-step instructions for starting and managing the SinkerProject26 application stack using Docker.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Make (installed by default on most Linux/macOS systems)

## Architecture Overview

The application consists of three main services:

| Service | Description | Port | Health Check |
|---------|-------------|------|--------------|
| `db` | MariaDB 10 database | 3306 | mariadb-admin ping |
| `backend` | Spring Boot API server | 8080 | http://localhost:8080/actuator/health |
| `frontend` | React + Vite web application | 5173 | http://localhost:5173 |

## First-Time Setup

### 1. Clone and Navigate to Repository

```bash
cd /path/to/SinkerProject26
```

### 2. Start All Services

Use the Makefile target to build and start all services:

```bash
make dev-up
```

This command will:
- Build Docker images for backend and frontend
- Start the database service and wait for it to be healthy
- Run Flyway migrations automatically (see [DB_MIGRATION.md](./DB_MIGRATION.md))
- Start backend and frontend services

**Expected output:**
```
docker compose up -d --build
[+] Building ...
[+] Running 3/3
 ✔ Container sinkerproject26-db-1        Started
 ✔ Container sinkerproject26-backend-1   Started
 ✔ Container sinkerproject26-frontend-1  Started
```

### 3. Verify Services Are Running

```bash
docker compose ps
```

**Expected output:**
```
NAME                          STATUS              PORTS
sinkerproject26-backend-1     Up                  0.0.0.0:8080->8080/tcp
sinkerproject26-db-1          Up (healthy)        0.0.0.0:3306->3306/tcp
sinkerproject26-frontend-1    Up                  0.0.0.0:5173->80/tcp
```

All services should show status `Up` (database should show `Up (healthy)`).

## Daily Operations

### Start Services

If services are already built, start them with:

```bash
make dev-up
```

### Stop Services

To stop all services while preserving data:

```bash
docker compose stop
```

To stop and restart later:

```bash
docker compose stop
# Later...
docker compose start
```

### Stop and Remove Services (Clean Shutdown)

To completely tear down the stack and remove volumes (⚠️ **this deletes all database data**):

```bash
make dev-down
```

**Warning:** This command removes all containers, networks, and volumes. Database data will be lost.

## Health Checks and Verification

### Check Service Health

**Database:**
```bash
docker compose exec db mariadb-admin ping -h localhost -uapp -papp --silent
echo $?  # Should output: 0
```

**Backend API:**
```bash
curl http://localhost:8080/api/health
# Expected: {"status":"UP"}
```

**Frontend:**
```bash
curl -I http://localhost:5173
# Expected: HTTP/1.1 200 OK
```

### Access Application URLs

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080/api
- **API Documentation (Swagger):** http://localhost:8080/swagger-ui.html
- **Health Endpoint:** http://localhost:8080/actuator/health

## Viewing Logs

### All Services

```bash
docker compose logs -f
```

### Specific Service

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

### Last N Lines

```bash
docker compose logs -f --tail=100 backend
```

### Follow Logs During Startup

```bash
docker compose logs -f --tail=200
```

## Common Issues and Troubleshooting

### Issue: Port Already in Use

**Symptoms:**
```
Error response from daemon: driver failed programming external connectivity on endpoint
bind: address already in use
```

**Solution:**
1. Check what's using the port:
   ```bash
   sudo lsof -i :8080  # or :3306, :5173
   ```
2. Stop the conflicting process or change the port in `docker-compose.yml`

### Issue: Database Not Healthy

**Symptoms:**
- Backend fails to start
- "Waiting for db to be healthy" message persists

**Solution:**
1. Check database logs:
   ```bash
   docker compose logs db
   ```
2. Verify database health:
   ```bash
   docker compose exec db mariadb-admin ping -h localhost -uapp -papp
   ```
3. Restart database service:
   ```bash
   docker compose restart db
   ```

### Issue: Flyway Migration Failures

**Symptoms:**
- Backend fails to start with Flyway errors in logs

**Solution:**
See [DB_MIGRATION.md](./DB_MIGRATION.md) for detailed migration troubleshooting.

### Issue: Backend Build Failures

**Symptoms:**
- Backend container exits immediately after start
- Build errors in logs

**Solution:**
1. Check backend logs:
   ```bash
   docker compose logs backend
   ```
2. Rebuild with no cache:
   ```bash
   docker compose build --no-cache backend
   make dev-up
   ```

### Issue: Frontend Build Failures

**Symptoms:**
- Frontend shows "502 Bad Gateway" or doesn't load

**Solution:**
1. Check frontend logs:
   ```bash
   docker compose logs frontend
   ```
2. Rebuild frontend:
   ```bash
   docker compose build --no-cache frontend
   docker compose up -d frontend
   ```

### Issue: "Cannot connect to Docker daemon"

**Symptoms:**
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solution:**
1. Ensure Docker daemon is running:
   ```bash
   sudo systemctl start docker
   ```
2. Add your user to the docker group:
   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   ```

## Rebuilding Services

### Rebuild All Services

```bash
make dev-down
make dev-up
```

### Rebuild Specific Service

```bash
docker compose build --no-cache backend
docker compose up -d backend
```

## Executing Commands Inside Containers

### Backend (Spring Boot)

```bash
# Open shell
docker compose exec backend bash

# Run Gradle commands (inside container)
docker compose exec backend ./gradlew tasks
```

### Database (MariaDB)

```bash
# Open MariaDB CLI
docker compose exec db mariadb -uapp -papp app

# Run SQL file
docker compose exec -T db mariadb -uapp -papp app < backup.sql
```

### Frontend (Node/React)

```bash
# Open shell
docker compose exec frontend sh

# Note: Frontend uses nginx in production, not npm
```

## Environment Variables

The following environment variables are configured in `docker-compose.yml`:

**Database (db):**
- `MARIADB_DATABASE=app`
- `MARIADB_USER=app`
- `MARIADB_PASSWORD=app`
- `MARIADB_ROOT_PASSWORD=root`

**Backend (backend):**
- `SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/app`
- `SPRING_DATASOURCE_USERNAME=app`
- `SPRING_DATASOURCE_PASSWORD=app`
- `SPRING_PROFILES_ACTIVE=docker`

To override these, create a `.env` file in the project root or modify `docker-compose.yml`.

## Network Configuration

All services run on the `appnet` bridge network, allowing inter-service communication using service names:
- Backend connects to database using hostname `db`
- Frontend proxies API requests to backend using hostname `backend`

## Data Persistence

- **Database data:** Stored in a Docker volume (removed by `make dev-down`)
- **To preserve data:** Use `docker compose stop` instead of `make dev-down`

## Next Steps

- **Database Operations:** See [DB_MIGRATION.md](./DB_MIGRATION.md)
- **Test Coverage:** See [COVERAGE.md](./COVERAGE.md)
- **API Documentation:** Visit http://localhost:8080/swagger-ui.html after starting services
