# skill.md — Project Skills / Commands / Conventions

This file defines the technical stack, commands, architecture decisions,
testing strategy, and development conventions for Coding Agents.

The Coding Agent MUST follow this file together with `agent.md`.
Do not modify this file unless explicitly instructed by a human.

---

# 0. Execution Environment (IMPORTANT)

This project is Docker-first.

Coding Agents MUST assume:

- Java is NOT installed on host
- Gradle is NOT installed on host
- Node/NPM are NOT installed on host

All build, test, and runtime commands MUST run via:

1. Makefile targets (preferred)
2. docker compose exec (fallback)

Never execute gradle/npm directly on host.

Before coding:
1. Check containers:
   `docker compose ps`
2. If services are not running:
   `make dev-up`
Do NOT ask user about environment setup unless blocked.
---

# 1. Tech Stack

## Backend
- Java 17
- Spring Boot 3.x
- Gradle
- REST API
- JWT Authentication
- springdoc OpenAPI
- MariaDB
- Flyway or Liquibase for DB migrations

## Frontend
- React
- Vite
- React Router
- Axios for API calls
- Material UI v5
- CSS/Tailwind (keep consistent)

## Database
- MariaDB 10+
- UTF8MB4 charset
- Use migrations, never manual schema drift

## Testing Tools
- Backend Unit/Integration: JUnit5, Spring Boot Test
- Frontend Unit: Vitest + React Testing Library
- E2E: Playwright

---

# 2. Git Commands
## Branching
Create branch:
git checkout -b <branch>
Push:
git push -u origin <branch>
Delete local:
git branch -d <branch>
---

# 3. Makefile (Canonical Interface)

Always prefer Makefile.

## Dev
- Start:
  `make dev-up`
- Stop:
  `make dev-down`

## Testing
- Run compose tests:
  `make test-compose`
- Cleanup test env:
  `make test-down`

---

# 4. Docker / Compose
## Dev Stack
Start:
make dev-up

Logs:
docker compose logs -f

Stop:
make dev-down
List services:
docker compose ps


## Service Names
Main services:
- db
- backend_server
- backend_unit
- e2e
Agent MUST use these names.

---

# 5. Backend Commands (Spring Boot)
Backend runs inside container.

## Run (dev)
Handled by docker compose:
backend_server

## Build/Test (inside container)
Run tests:
docker compose exec backend_server ./gradlew test

Build:
docker compose exec backend_server ./gradlew build -x test

Never run `./gradlew` on host.

---

# 6. Frontend Commands (Vite + React)
Frontend commands MUST run inside container.
Install deps:
docker compose exec frontend npm ci

Dev server:
docker compose exec frontend npm run dev -- --host

Build:
docker compose exec frontend npm run build

Tests:
docker compose exec frontend npm test
Never run npm on host.

---

# 7. Database (MariaDB)
Host (inside containers):
db
Port:
3306
JDBC:
jdbc:mariadb://db:3306/app
Migration location:
backend/resources/db

---

# 8. Architecture Decisions
## Backend Package Structure
controller/
service/
repository/
entity/
dto/
config/


## Frontend Layout

- Left sidebar fixed after login
- Main content outlet
- Separate login page
- Dashboard landing page after login

---

# 9. API Conventions
Base path:
/api
Auth:
/api/auth/*
Health:
/api/health
OpenAPI:
/v3/api-docs
Swagger:
/swagger-ui


## Error Format

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/..."
}

10. Frontend Conventions
Always use relative /api/...
Never hardcode localhost backend URLs
Use environment variables if needed
Routes:
/login
/
/users
/forecast-config
/forecast-upload

11. Security Rules
JWT expiration: 24h
Lock account after 5 failed logins
Never disable auth middleware
Apply role checks
Token storage: localStorage (initial phase)

12. Testing Strategy
Backend
Unit tests for services
Integration tests for APIs
Use Testcontainers if DB required
Frontend
Component render tests
Form submit tests
E2E
Login flow mandatory
Protected route tests

13. Quality Gate (Before PR)
Must pass:
Backend tests
Frontend build
No lint errors
E2E login test
Backend coverage >= 70%

14. Branch & Commit Conventions
Branch Naming
Feature:
claude/feat/F001-login
Fix:
claude/fix/X002-lock
Commit Prefix
feat
fix
refactor
test
chore

15. PR Rules
One task = One PR
Target branch:
claude/integration
Agent MUST NOT merge PR
Human handles merge + branch deletion

16. Task Workflow (Agent)
Read description.md
Implement backend
Implement frontend
Write unit tests
Write E2E tests if required
Run tests
Commit
Push branch
Open PR

17. Agent Fast Rules (IMPORTANT)
Use Docker environment only.
Never ask about installing Java/Node.
If command fails:
check service name via docker compose ps
Prefer Makefile commands.
Do not change architecture unless task requires.

18. What NOT To Do
Do not commit to main
Do not disable authentication
Do not skip tests
Do not modify skill.md automatically
Do not implement outside defined task
Do not run gradle on host
Do not run npm on host
End of skill.md