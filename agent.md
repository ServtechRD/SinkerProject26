# agent.md
# Coding Agent Rules (Docker-first)

This file defines the operational rules for Coding Agents (Claude Code).
The agent MUST follow this file together with `skill.md`.

DO NOT modify this file unless explicitly instructed by a human.

---

## 0) Absolute Rules (Highest Priority)

### Docker-first / Makefile-first
- The host machine MUST be treated as having **no Java, no Gradle, no Node/NPM**.
- NEVER run `java`, `gradle`, `./gradlew`, `npm`, `node` directly on the host.
- All build/test/run commands MUST be executed via:
  1) **Makefile targets** (preferred)
  2) `docker compose exec ...` (fallback)

### Single Task Only
- Only work on **one task at a time**.
- Only process tasks explicitly mentioned by ID (e.g., `F001`, `X010`,`T016`),
  OR the next folder containing `status.todo` under the configured task root.

### Branch + PR + Human Merge
- One task = one branch = one PR.
- Target branch for PR: `claude/intergration`.
- The agent MUST NOT merge PRs (no direct merge, no auto-merge command).
  - GitHub may auto-merge based on repo settings and CI checks.

### Safety
- NEVER force-push to any shared branch.
- NEVER delete `main`, `develop`, or `claude/intergration`.
- Do not change DB schema outside task scope.
- Do not disable authentication or role checks.
- If the task is ambiguous or blocked, stop and ask a human.

---

## 1) Task Source / Structure

Tasks are defined under:
- `spec/feat/*`
- `spec/fix/*`

Each task folder MUST contain:
- `description.md`
- `acceptance.md`
- One status file:
  - `status.todo` (not started)
  - `status.doing` (in progress)
  - `status.done` (completed)

---

## 2) Execution Environment Checklist (Before Coding)

1. Check containers:
   - `docker compose ps`

2. If services are not running:
   - `make dev-up`

3. Do NOT ask the user about installing Java/Node/Gradle unless truly blocked.

---

## 3) Task Processing Workflow (Canonical)

### Step A — Identify Task
- Locate the task folder:
  - `spec/feat/<TASK-ID>-.../` or `spec/fix/<TASK-ID>-.../`
  - or exact folder name used by the repo
- Read:
  - `description.md`
  - `acceptance.md`

### Step B — Mark In Progress
- Rename:
  - `status.todo` → `status.doing`

### Step C — Create Branch (from `claude/integration`)
Branch naming:
- Feature: `claude/feat/<TASK-ID>-<slug>`
- Fix: `claude/fix/<TASK-ID>-<slug>`

Commands:
- `git checkout claude/integration`
- `git pull`
- `git checkout -b <branch>`

### Step D — Implement Only In-Scope Changes
- Implement ONLY what the spec requires.
- Minimize diffs. Avoid refactors unless required.
- Prefer test fixes over production changes unless production is incorrect.

### Step E — Tests (MUST follow skill.md)
**All tests must run in Docker via Makefile.**

Canonical test command:
- `make test-compose`

Cleanup after tests:
- `make test-down`

If debugging is needed:
- Use `docker compose ps`
- Use `docker compose logs -f --tail=200`

### Step F — Commit
Use Conventional Commits:
- `feat(<TASK-ID>): ...`
- `fix(<TASK-ID>): ...`
- `test(<TASK-ID>): ...`
- `chore(<TASK-ID>): ...`
- `refactor(<TASK-ID>): ...`

### Step G — Push + PR
- `git push -u origin <branch>`
- Open PR to `claude/intergration`
- PR must include:
  - Summary of changes
  - Files/modules touched
  - Test commands + results
  - Risk / rollback notes
  - Scope confirmation (what is intentionally NOT changed)

### Step H — Mark Done
After PR is opened and checks are green:
- Rename:
  - `status.doing` → `status.done`

Do NOT merge PR. Human or GitHub auto-merge handles it.

---

## 4) Test Execution Rules (IMPORTANT)

### Always use Makefile
- Start dev stack (if needed): `make dev-up`
- Run test stack: `make test-compose`
- Tear down test stack: `make test-down`

### Fallback: docker compose exec
Only if Makefile is blocked, use service names defined in `skill.md`:
- `db`
- `backend_server`
- `backend_unit`
- `e2e`

Examples (fallback only):
- `docker compose exec backend_server ./gradlew test`
- `docker compose exec frontend npm run build`

But the preferred path is still `make test-compose`.

---

## 5) File Reading / Cost Control Rules

To reduce unnecessary exploration and cost:
- Read ONLY:
  - the task spec files
  - files directly referenced by the spec
  - files needed to fix failing tests (test + immediate dependencies)
- Do NOT scan the entire repo unless required.
- Do NOT modify unrelated files.

---

## 6) UI Text / Glossary Rule

All UI text must follow:
- `spec/task/X008-ui-text-zh-tw/glossary.md`

Do NOT invent UI terminology outside glossary.

---

## 7) Default Decisions (When Not Specified)

If not specified, assume:
- Backend: Java 17 + Spring Boot 3.x
- Frontend: React + Vite + MUI v5
- API base path: `/api`
- Auth: JWT 24h with role checks
- DB: MariaDB + migrations
- Tests: JUnit5 + Playwright (E2E)
- Styling: keep consistent with existing project

Do not ask for clarification if these match the requirement.

---

## 8) What NOT To Do

- Do not run gradle/npm on host.
- Do not commit directly to `main` or `claude/intergration`.
- Do not merge PRs.
- Do not skip required tests.
- Do not disable authentication/authorization.
- Do not change architecture unless required by the spec.
- Do not implement outside the defined task scope.

--- 

End of agent.md
