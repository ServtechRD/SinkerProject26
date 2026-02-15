# skill.md — Project Skills / Commands / Conventions

This file defines the **technical stack, commands, architecture decisions, testing strategy, and development conventions** for Coding Agents.

The Coding Agent MUST follow this file together with `agent.md`.
Do not modify this file unless explicitly instructed by a human.

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
- CSS or Tailwind (keep consistent)

## Database
- MariaDB 10+
- UTF8MB4 charset
- Use migrations, never manual schema drift

## Testing Tools
- Backend Unit/Integration: JUnit 5, Spring Boot Test
- Frontend Unit: Vitest + React Testing Library
- E2E: Playwright

---

# 2. Git Commands

## Branching
- Create branch:
  `git checkout -b <branch>`

- Push branch:
  `git push -u origin <branch>`

- Delete local branch:
  `git branch -d <branch>`

---

# 3. Backend Commands (Spring Boot)

- Build:
  `cd backend && ./gradlew build -x test`

- Test:
  `cd backend && ./gradlew test`

- Run:
  `cd backend && ./gradlew bootRun`

---

# 4. Frontend Commands (Vite + React)

- Install dependencies:
  `cd frontend && npm ci`

- Dev server:
  `cd frontend && npm run dev -- --host`

- Build:
  `cd frontend && npm run build`

- Lint & Test:
  `cd frontend && npm run lint && npm test`

---

# 5. Docker / Compose

- Build images:
  `docker compose build`

- Start services:
  `docker compose up -d`

- View logs:
  `docker compose logs -f`

- Stop services:
  `docker compose down`

---

# 6. Database (MariaDB)

- Host (from containers): `db`
- Port: `3306`
- JDBC:
  `jdbc:mariadb://db:3306/app`

- Migration location:
  `backend/resources/db`

---

# 7. Architecture Decisions

## Backend Package Structure
# skill.md — Project Skills / Commands / Conventions

This file defines the **technical stack, commands, architecture decisions, testing strategy, and development conventions** for Coding Agents.

The Coding Agent MUST follow this file together with `agent.md`.
Do not modify this file unless explicitly instructed by a human.

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
- CSS or Tailwind (keep consistent)

## Database
- MariaDB 10+
- UTF8MB4 charset
- Use migrations, never manual schema drift

## Testing Tools
- Backend Unit/Integration: JUnit 5, Spring Boot Test
- Frontend Unit: Vitest + React Testing Library
- E2E: Playwright

---

# 2. Git Commands

## Branching
- Create branch:
  `git checkout -b <branch>`

- Push branch:
  `git push -u origin <branch>`

- Delete local branch:
  `git branch -d <branch>`

---

# 3. Backend Commands (Spring Boot)

- Build:
  `cd backend && ./gradlew build -x test`

- Test:
  `cd backend && ./gradlew test`

- Run:
  `cd backend && ./gradlew bootRun`

---

# 4. Frontend Commands (Vite + React)

- Install dependencies:
  `cd frontend && npm ci`

- Dev server:
  `cd frontend && npm run dev -- --host`

- Build:
  `cd frontend && npm run build`

- Lint & Test:
  `cd frontend && npm run lint && npm test`

---

# 5. Docker / Compose

- Build images:
  `docker compose build`

- Start services:
  `docker compose up -d`

- View logs:
  `docker compose logs -f`

- Stop services:
  `docker compose down`

---

# 6. Database (MariaDB)

- Host (from containers): `db`
- Port: `3306`
- JDBC:
  `jdbc:mariadb://db:3306/app`

- Migration location:
  `backend/resources/db`

---

# 7. Architecture Decisions

## Backend Package Structure
controller/
service/
repository/
entity/
dto/
config/


## Frontend Layout
- Left sidebar (fixed after login)
- Main content outlet
- Separate login page
- Dashboard landing page after login

---

# 8. API Conventions

- Base Path: `/api`
- Auth APIs: `/api/auth/*`
- Health: `/api/health`
- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui`

## Error Format
{
"timestamp": "...",
"status": 400,
"error": "Bad Request",
"path": "/api/..."
}


---

# 9. Frontend Conventions

- Always use relative `/api/...`
- Never hardcode `http://localhost:8080`
- Use environment variables if needed
- Routes:
  - `/login`
  - `/`
  - `/users`
  - `/forecast-config`
  - `/forecast-upload`

---

# 10. Security Rules

- JWT expiration: 24 hours
- Lock account after 5 failed login attempts
- Never disable auth middleware
- Apply role/permission checks on protected APIs
- Store tokens in localStorage (acceptable initial approach)

---

# 11. Testing Strategy

## Backend
- Unit tests for services
- Integration tests for APIs
- Use Testcontainers when DB needed

## Frontend
- Component render tests
- Submit/form tests

## E2E
- Login flow mandatory
- Protected route access tests

---

# 12. Quality Gate (Before PR)

- Backend tests must pass
- Frontend build must succeed
- No lint errors
- E2E login test must pass
- Code coverage target:
  - Backend >= 70%

---

# 13. Branch & Commit Conventions

## Branch Naming
- Feature:
  `claude/feat/F001-login`
- Fix:
  `claude/fix/X002-lock`

## Commit Prefixes
- `feat`
- `fix`
- `refactor`
- `test`
- `chore`

---

# 14. PR Rules

- One task = One PR
- Target branch: `claude/integration`
- Do NOT merge PRs (human only)
- Human deletes branch after merge

---

# 15. Task Workflow

1. Read `description.md`
2. Implement backend
3. Implement frontend
4. Write unit tests
5. Write E2E tests if needed
6. Run all tests
7. Commit with prefix
8. Push branch
9. Open PR

---

# 16. What NOT To Do

- Do not commit directly to `main`
- Do not disable authentication
- Do not skip tests
- Do not modify `skill.md` automatically
- Do not implement outside defined tasks

---

# End of skill.md
