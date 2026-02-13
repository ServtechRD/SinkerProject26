# Claude Agent Rules

## Branching
- NEVER push or merge to `main`.
- Base branch: `claude/integration`.
- Feature branches: `claude/feat/<ticket>-<desc>`.

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
- Summary
- Test commands + results
- Risks / rollback notes

