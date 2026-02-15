# Claude Agent Rules

## Branching
- NEVER push or merge to `main`.
- Base branch: `claude/integration`.
- Claude MAY merge into `claude/integration` only if tests pass.
- Always create a new branch for each task.
- Always open PR to claude/integration.
- Never commit directly to integration or main.

## Task Source
- Tasks are defined under `spec/feat/*` and `spec/fix/*`.
- Each task folder MUST contain:
  - `description.md`
  - `acceptance.md`
  - `status.todo | status.doing | status.done`

## Task Processing Rules
1. Only process tasks explicitly mentioned by ID (e.g., F001, X002),
   or the next folder containing `status.todo`.
2. Before starting:
   - Rename `status.todo` → `status.doing`.
3. Create branch:
   - feat → `claude/feat/<TASK-ID>-<slug>`
   - fix  → `claude/fix/<TASK-ID>-<slug>`
   - base → `claude/integration`
4. Read `description.md` and `acceptance.md`.
5. Implement ONLY the defined scope.
6. Add or update tests to satisfy acceptance.
7. Run project tests/build (see skill.md).
8. Commit using Conventional Commits:
   - feat(F001): ...
   - fix(X002): ...
9. Push branch and open PR to `claude/integration`.
10. If CI/tests pass:
   - Merge PR into `claude/integration`.
   - Delete feature branch.
   - Rename `status.doing` → `status.done`.

## Task Execution Rule
- Only work on one task at a time.
- Never jump to another task.
- Always open PR after finishing.
- Never merge PR.

## Safety Rules
- NEVER delete `main`, `develop`, or `claude/integration`.
- NEVER force push to shared branches.
- Do not change DB schema outside task scope.
- If ambiguous, stop and ask for clarification.

## Workflow
1) Create feature branch from `claude/integration`
2) Implement only what the spec requests
3) Add/update tests
4) Run:
   - Backend: `cd backend && ./gradlew test`
   - Frontend: `cd frontend && npm ci && npm run build`
5) Commit and push
6) Open PR to `claude/integration` only
7) You MAY merge into `claude/integration` if checks pass
8) NEVER merge into `main` (human only)

## PR Requirements
- Summary of changes
- Test commands and results
- Risk/rollback notes
- Scope (modules/files touched)


## Default Decisions
If not specified, assume:

- Backend language: Java 17
- Frontend framework: React + Vite
- Testing: JUnit + Playwright
- API base path: /api
- Authentication: JWT 24h
- Database: MariaDB
- Styling: Tailwind or CSS Modules
- Error format: standard JSON error
- Port:
  - Frontend: 5173
  - Backend: 8080

Do not ask clarification if these match the requirement.

## Allowed Autonomy
The agent MAY decide:

- File names
- Folder structure inside module
- Minor UI styling
- Variable names
- Test data
- Mock services
- Commit messages

## Do Not Ask For
- Naming preferences
- Minor UI colors
- Test framework choice
- Folder naming
- Lint rules