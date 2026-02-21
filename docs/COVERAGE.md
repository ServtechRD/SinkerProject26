# Test Coverage Guide

This document explains how to generate and view test coverage reports for backend and frontend components.

## Overview

- **Backend:** JaCoCo (Java Code Coverage) - generates HTML/XML reports
- **Frontend:** Vitest coverage (c8/istanbul) - generates HTML/JSON reports
- **All commands:** Docker-first (via Makefile or `docker compose exec`)

## Backend Coverage (JaCoCo)

### Setup

JaCoCo needs to be configured in the backend's `build.gradle` file.

#### Step 1: Add JaCoCo Plugin

Add to `backend/build.gradle`:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.12'
    id 'io.spring.dependency-management' version '1.1.6'
    id 'jacoco'  // Add this line
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.11"
}

tasks.named('test') {
    useJUnitPlatform()
    finalizedBy jacocoTestReport  // Generate report after tests
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

// Optional: Enforce minimum coverage
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.70  // 70% coverage minimum
            }
        }
    }
}
```

#### Step 2: Rebuild Backend

```bash
docker compose build --no-cache backend
docker compose up -d backend
```

### Generating Coverage Reports

#### Using Makefile (recommended)

After adding the `coverage-backend` target to the Makefile:

```bash
make coverage-backend
```

#### Manual Command (fallback)

```bash
docker compose exec backend ./gradlew test jacocoTestReport
```

**Expected output:**
```
> Task :test
> Task :jacocoTestReport

BUILD SUCCESSFUL in 15s
5 actionable tasks: 5 executed
```

### Viewing Coverage Reports

#### 1. Copy Reports to Host

```bash
docker compose cp backend:/app/build/reports/jacoco/test/html ./coverage-reports/backend
```

#### 2. Open in Browser

```bash
# Linux
xdg-open coverage-reports/backend/index.html

# macOS
open coverage-reports/backend/index.html

# Windows
start coverage-reports/backend/index.html
```

#### 3. Check Coverage from Terminal

```bash
docker compose exec backend ./gradlew test jacocoTestReport
docker compose exec backend cat build/reports/jacoco/test/html/index.html | grep -A 2 "Total"
```

### Coverage Report Locations (Inside Container)

| Format | Path (inside backend container) |
|--------|----------------------------------|
| HTML | `/app/build/reports/jacoco/test/html/` |
| XML | `/app/build/reports/jacoco/test/jacocoTestReport.xml` |
| Exec Data | `/app/build/jacoco/test.exec` |

### Understanding JaCoCo Reports

The HTML report shows:

- **Instructions Coverage:** Individual JVM bytecode instructions
- **Branches Coverage:** If/else conditions and switch statements
- **Cyclomatic Complexity:** Code complexity metric
- **Lines Coverage:** Source code lines executed
- **Methods Coverage:** Methods invoked during tests
- **Classes Coverage:** Classes loaded during tests

**Color coding:**
- 🟢 Green: Covered
- 🔴 Red: Not covered
- 🟡 Yellow: Partially covered (branches)

### Example: Finding Uncovered Code

```bash
# Generate report
docker compose exec backend ./gradlew test jacocoTestReport

# Copy to host
docker compose cp backend:/app/build/reports/jacoco/test/html ./coverage-reports/backend

# Open and navigate to:
# coverage-reports/backend/index.html
# → Click package name
# → Click class name
# → See line-by-line coverage with green/red highlighting
```

## Frontend Coverage (Vitest)

### Setup

Vitest coverage requires additional configuration in `vite.config.js`.

#### Step 1: Add Coverage Configuration

Update `frontend/vite.config.js`:

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    exclude: ['e2e/**', 'node_modules/**'],
    coverage: {
      provider: 'v8',  // or 'istanbul'
      reporter: ['text', 'html', 'json', 'lcov'],
      exclude: [
        'node_modules/',
        'e2e/',
        'src/test/',
        '**/*.config.js',
        '**/mockData.js',
      ],
      reportsDirectory: './coverage',
      all: true,
      lines: 70,
      functions: 70,
      branches: 70,
      statements: 70,
    },
  },
})
```

#### Step 2: Install Coverage Dependencies

Update `frontend/package.json`:

```json
{
  "scripts": {
    "test": "vitest run",
    "test:watch": "vitest",
    "test:coverage": "vitest run --coverage",
    "test:ui": "vitest --ui"
  },
  "devDependencies": {
    "@vitest/coverage-v8": "^4.0.18",
    // ... other deps
  }
}
```

#### Step 3: Rebuild Frontend

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

### Generating Coverage Reports

#### Using Makefile (recommended)

After adding the `coverage-frontend` target to the Makefile:

```bash
make coverage-frontend
```

#### Manual Command (fallback)

Since the frontend container runs nginx in production, we need to run tests during build:

**Option A: Update Dockerfile to support test command**

Add to `frontend/Dockerfile`:

```dockerfile
# Build stage
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Test stage (optional)
FROM build as test
CMD ["npm", "run", "test:coverage"]

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Then run tests:

```bash
docker compose run --rm --build -e NODE_ENV=test frontend npm run test:coverage
```

**Option B: Run tests on host (if Node.js is installed)**

⚠️ This violates the Docker-first principle and should only be used if absolutely necessary:

```bash
cd frontend
npm run test:coverage
```

### Viewing Coverage Reports

#### 1. Copy Reports to Host

```bash
docker compose run --rm frontend npm run test:coverage
# Reports are written to frontend/coverage/

# Or copy from container
docker compose cp frontend:/app/coverage ./coverage-reports/frontend
```

#### 2. Open in Browser

```bash
# Linux
xdg-open frontend/coverage/index.html

# macOS
open frontend/coverage/index.html

# Windows
start frontend/coverage/index.html
```

### Coverage Report Locations (Frontend)

| Format | Path |
|--------|------|
| HTML | `frontend/coverage/index.html` |
| JSON | `frontend/coverage/coverage-final.json` |
| LCOV | `frontend/coverage/lcov.info` |

### Understanding Vitest Coverage

The HTML report shows:

- **Statements:** Individual statements executed
- **Branches:** If/else branches taken
- **Functions:** Functions called
- **Lines:** Source lines executed

**Color coding:**
- 🟢 Green (80-100%): Good coverage
- 🟡 Yellow (50-80%): Moderate coverage
- 🔴 Red (0-50%): Low coverage

## Combined Coverage Report

### Generate Both Reports

Using Makefile (after adding targets):

```bash
make coverage
```

This will:
1. Generate backend coverage
2. Generate frontend coverage
3. Copy both reports to `coverage-reports/`

### Manual Commands

```bash
# Backend
docker compose exec backend ./gradlew clean test jacocoTestReport
docker compose cp backend:/app/build/reports/jacoco/test/html ./coverage-reports/backend

# Frontend
docker compose run --rm frontend npm run test:coverage
cp -r frontend/coverage ./coverage-reports/frontend

# Summary
echo "✅ Backend coverage: coverage-reports/backend/index.html"
echo "✅ Frontend coverage: coverage-reports/frontend/index.html"
```

## Makefile Targets

The following docker-first targets can be added to the Makefile:

### coverage-backend

Generates backend test coverage report.

```makefile
coverage-backend:
	@echo "Generating backend coverage report..."
	docker compose exec backend ./gradlew clean test jacocoTestReport
	@mkdir -p coverage-reports
	docker compose cp backend:/app/build/reports/jacoco/test/html ./coverage-reports/backend
	@echo "✅ Backend coverage report: coverage-reports/backend/index.html"
```

**Usage:**
```bash
make coverage-backend
```

### coverage-frontend

Generates frontend test coverage report.

```makefile
coverage-frontend:
	@echo "Generating frontend coverage report..."
	@mkdir -p coverage-reports
	docker compose run --rm frontend npm run test:coverage
	@cp -r frontend/coverage ./coverage-reports/frontend
	@echo "✅ Frontend coverage report: coverage-reports/frontend/index.html"
```

**Usage:**
```bash
make coverage-frontend
```

### coverage

Generates both backend and frontend coverage reports.

```makefile
coverage: coverage-backend coverage-frontend
	@echo ""
	@echo "📊 Coverage Reports Generated:"
	@echo "   Backend:  coverage-reports/backend/index.html"
	@echo "   Frontend: coverage-reports/frontend/index.html"
```

**Usage:**
```bash
make coverage
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Coverage

on: [push, pull_request]

jobs:
  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start services
        run: make dev-up

      - name: Generate backend coverage
        run: make coverage-backend

      - name: Generate frontend coverage
        run: make coverage-frontend

      - name: Upload backend coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage-reports/backend/jacocoTestReport.xml
          flags: backend

      - name: Upload frontend coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./frontend/coverage/lcov.info
          flags: frontend
```

## Troubleshooting

### Issue: "JaCoCo not found"

**Symptoms:**
```
Task 'jacocoTestReport' not found in project ':backend'.
```

**Solution:**
1. Verify `jacoco` plugin is added to `build.gradle`
2. Rebuild backend container:
   ```bash
   docker compose build --no-cache backend
   ```

### Issue: "No tests found"

**Symptoms:**
```
No tests found
```

**Solution:**
1. Verify test files exist:
   ```bash
   docker compose exec backend find src/test -name "*Test.java"
   ```
2. Check test task configuration in `build.gradle`

### Issue: "Coverage report not generated"

**Symptoms:**
- Tests pass but no report generated

**Solution:**
1. Check if `finalizedBy jacocoTestReport` is in `build.gradle`
2. Run explicitly:
   ```bash
   docker compose exec backend ./gradlew test
   docker compose exec backend ./gradlew jacocoTestReport
   ```

### Issue: "Frontend coverage missing dependencies"

**Symptoms:**
```
Cannot find module '@vitest/coverage-v8'
```

**Solution:**
1. Add to `package.json` devDependencies:
   ```json
   "@vitest/coverage-v8": "^4.0.18"
   ```
2. Rebuild:
   ```bash
   docker compose build --no-cache frontend
   ```

### Issue: "Permission denied copying reports"

**Symptoms:**
```
Error: Permission denied
```

**Solution:**
```bash
# Fix permissions
docker compose exec backend chmod -R 755 /app/build/reports
# Then retry copy
docker compose cp backend:/app/build/reports/jacoco/test/html ./coverage-reports/backend
```

## Best Practices

### DO:
✅ Run coverage reports regularly
✅ Set minimum coverage thresholds
✅ Focus on covering critical business logic
✅ Review coverage reports during code review
✅ Exclude generated code from coverage
✅ Use coverage as a guide, not a goal

### DON'T:
❌ Don't aim for 100% coverage blindly
❌ Don't write tests just to increase coverage
❌ Don't ignore low-value coverage (getters/setters)
❌ Don't commit coverage reports to git (add to .gitignore)
❌ Don't rely solely on coverage metrics for quality

## Coverage Goals

Recommended coverage targets:

| Component | Target | Priority |
|-----------|--------|----------|
| Controllers | 80%+ | High |
| Services | 85%+ | High |
| Repositories | 70%+ | Medium |
| Models/Entities | 60%+ | Low |
| Utilities | 90%+ | High |
| Frontend Components | 75%+ | Medium |
| Frontend Hooks | 80%+ | High |
| Frontend Utils | 85%+ | High |

## Related Documentation

- [STARTUP.md](./STARTUP.md) - Starting and stopping services
- [DB_MIGRATION.md](./DB_MIGRATION.md) - Database operations
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Vitest Coverage](https://vitest.dev/guide/coverage.html)
