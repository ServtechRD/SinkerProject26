# Project Skills / Commands

## Git
- Create branch: `git checkout -b <branch>`
- Push: `git push -u origin <branch>`
- Delete local branch: `git branch -d <branch>`

## Backend (Spring Boot)
- Build: `cd backend && ./gradlew build -x test`
- Test:  `cd backend && ./gradlew test`
- Run:   `cd backend && ./gradlew bootRun`

## Frontend (Vite + React)
- Install deps: `cd frontend && npm ci`
- Dev server:   `cd frontend && npm run dev -- --host`
- Build:        `cd frontend && npm run build`
- Lint/Test:    `cd frontend && npm run lint && npm test`

## Docker / Compose
- Build images: `docker compose build`
- Up (bg):      `docker compose up -d`
- Logs:         `docker compose logs -f`
- Down:         `docker compose down`

## Database (MariaDB)
- Host (from containers): `db`
- Port: `3306`
- JDBC: `jdbc:mariadb://db:3306/app`
- Migrations: use Flyway/Liquibase or SQL scripts in `backend/resources/db`

## Testing Strategy
- Backend: unit + integration for APIs touched by task.
- Frontend: render + submit flow for pages touched.
- Always satisfy `acceptance.md` before PR.

## Conventions
- Commit prefixes: `feat`, `fix`, `refactor`, `test`, `chore`
- Branch names:
  - `claude/feat/F001-login`
  - `claude/fix/X002-lock`