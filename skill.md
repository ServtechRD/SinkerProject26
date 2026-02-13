# Project Skills / Commands

## Backend (Spring Boot)
- Build: `cd backend && ./gradlew build -x test`
- Test:  `cd backend && ./gradlew test`
- Run:   `cd backend && ./gradlew bootRun`

## Frontend (Vite + React)
- Install: `cd frontend && npm ci`
- Dev:     `cd frontend && npm run dev -- --host`
- Build:   `cd frontend && npm run build`

## Docker
- Build: `docker compose build`
- Up:    `docker compose up -d`
- Logs:  `docker compose logs -f`

## DB (MariaDB)
- Host from containers: `db`
- Port: `3306`
- JDBC: `jdbc:mariadb://db:3306/app`

