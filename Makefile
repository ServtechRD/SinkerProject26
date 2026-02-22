# Development environment
dev-up:
	docker compose up -d --build

# Rebuild only backend + frontend (db stays as-is, no volume wipe)
dev-build:
	docker compose up -d --build backend frontend

dev-down:
	docker compose down -v

# Testing
test-compose:
#   docker compose -f docker-compose.test.yml up --build  --exit-code-from backend_unit
	docker compose -f docker-compose.test.yml down -v --remove-orphans || true
	docker compose -f docker-compose.test.yml up -d --build db backend frontend
	docker compose -f docker-compose.test.yml build backend_unit
	docker compose -f docker-compose.test.yml run --rm backend_unit
	docker compose -f docker-compose.test.yml build e2e
	docker compose -f docker-compose.test.yml run --rm e2e
	docker compose -f docker-compose.test.yml down -v --remove-orphans

test-down:
	docker compose -f docker-compose.test.yml down -v

# Database operations
db-info:
	@echo "=== Database Migration History ==="
	@docker compose exec db mariadb -uapp -papp app -e "SELECT installed_rank, version, description, type, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null || echo "Database not running. Start with: make dev-up"
	@echo ""
	@echo "=== Current Schema Version ==="
	@docker compose exec db mariadb -uapp -papp app -e "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;" 2>/dev/null || true
	@echo ""
	@echo "=== Tables in Database ==="
	@docker compose exec db mariadb -uapp -papp app -e "SHOW TABLES;" 2>/dev/null || true

db-shell:
	@echo "Opening MariaDB shell (database: app, user: app)..."
	@echo "Exit with: quit or Ctrl+D"
	@docker compose exec db mariadb -uapp -papp app

db-migrate:
	@echo "Restarting backend to run Flyway migrations..."
	docker compose restart backend
	@echo "Waiting for migrations to complete..."
	@sleep 5
	@docker compose logs backend | grep -i flyway | tail -20

# Coverage reports
coverage-backend:
	@echo "Generating backend coverage report..."
	@docker compose -f docker-compose.coverage.yml up --build db backend backend_coverage --abort-on-container-exit --exit-code-from backend_coverage
	@mkdir -p coverage-reports/backend
	@cp -r backend/build/reports/jacoco/test/html/* coverage-reports/backend/
	@docker compose -f docker-compose.coverage.yml down -v
	@echo "✅ Backend coverage report: coverage-reports/backend/index.html"


coverage-frontend:
	@echo "Generating frontend coverage report..."
	@mkdir -p coverage-reports/frontend
	@docker compose -f docker-compose.coverage.yml run --rm frontend_coverage
	@echo "✅ Frontend coverage report: coverage-reports/frontend/index.html"

coverage: coverage-backend coverage-frontend
	@echo ""
	@echo "📊 Coverage Reports Generated:"
	@echo "   Backend:  coverage-reports/backend/index.html"
	@echo "   Frontend: coverage-reports/frontend/index.html"

# Help
help:
	@echo "SinkerProject26 - Available Make Targets"
	@echo ""
	@echo "Development:"
	@echo "  make dev-up           - Start all services (build + run)"
	@echo "  make dev-build        - Rebuild only backend + frontend (keeps DB data)"
	@echo "  make dev-down         - Stop and remove all services (⚠️  deletes data)"
	@echo ""
	@echo "Testing:"
	@echo "  make test-compose     - Run E2E tests in Docker"
	@echo "  make test-down        - Stop test containers"
	@echo ""
	@echo "Database:"
	@echo "  make db-info          - Show migration history and tables"
	@echo "  make db-shell         - Open MariaDB CLI"
	@echo "  make db-migrate       - Restart backend to run migrations"
	@echo ""
	@echo "Coverage:"
	@echo "  make coverage-backend - Generate backend coverage (JaCoCo)"
	@echo "  make coverage-frontend- Generate frontend coverage (Vitest)"
	@echo "  make coverage         - Generate both coverage reports"
	@echo ""
	@echo "Documentation: See docs/ folder"

.PHONY: dev-up dev-build dev-down test-compose test-down db-info db-shell db-migrate coverage-backend coverage-frontend coverage help
