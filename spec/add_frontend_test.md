Goal:
Raise frontend branch coverage to >= 70% (Vitest thresholds).

Rules:
- Prefer adding/updating tests only. Do not change production code unless necessary to make code testable.
- Focus on the lowest branch coverage folders first:
  - src/pages/production
  - src/pages/inventory
- Add tests for branches:
  - loading state
  - error state
  - empty data state
  - conditional render branches (if/else, ternaries, early returns)
- Mock API modules (vi.mock) instead of hitting real network.
- Use existing project testing patterns and utilities.

Loop:
1) Run: make coverage-frontend
2) If thresholds fail, open coverage HTML report under coverage-reports/frontend and identify files with low branch coverage.
3) Add targeted tests until thresholds pass.
4) Repeat until branch coverage >= 70%.
