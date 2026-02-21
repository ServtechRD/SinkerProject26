# Development environment
dev-up:
	docker compose up -d --build

dev-down:
	docker compose down -v

# Testing
test-compose:
	docker compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from e2e

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
	@docker compose exec backend ./gradlew clean test jacocoTestReport
	@mkdir -p coverage-reports
	@docker compose cp backend:/app/build/reports/jacoco/test/html ./coverage-reports/backend
	@echo "✅ Backend coverage report: coverage-reports/backend/index.html"

coverage-frontend:
	@echo "Generating frontend coverage report..."
	@mkdir -p coverage-reports
	@docker compose run --rm frontend npm run test:coverage
	@cp -r frontend/coverage ./coverage-reports/frontend
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

.PHONY: dev-up dev-down test-compose test-down db-info db-shell db-migrate coverage-backend coverage-frontend coverage help
