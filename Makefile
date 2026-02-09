dev-up:
	docker compose up -d --build

dev-down:
	docker compose down -v

test-compose:
	docker compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from e2e

test-down:
	docker compose -f docker-compose.test.yml down -v
