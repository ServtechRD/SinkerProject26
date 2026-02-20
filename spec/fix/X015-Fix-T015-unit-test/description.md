# X010 - Fix T015 Unit Test (Docker Container Hang / Failing Test)

## Context
We are using Claude Code to implement tasks for a web system built with Java Spring Boot (backend) and React (frontend).
Work has been completed and merged up to **T015** via PRs, with manual merges.

During T015, one unit test was merged even though the Docker container/test run was getting stuck (blocked/hanging), so it was merged without being fully validated at that time.

After running tests manually, we confirmed there is a failing or incorrect unit test behavior introduced by T015.

## Problem Statement
- At least one unit test related to T015 is failing or behaving incorrectly when executed manually.
- In the Docker-based test environment, the test execution may hang (e.g., container stuck, tests never finish, or waits indefinitely).
- We need to fix the T015 unit test so that:
  1) the test is correct and deterministic
  2) it does not hang in Docker/CI
  3) it passes reliably locally and in Docker/CI

## Scope
- Identify the failing/hanging unit test(s) introduced or impacted by T015.
- Fix the unit test implementation (and test-only utilities/mocks) to remove nondeterminism/hangs.
- If the failure is caused by incorrect assumptions introduced in T015, adjust assertions/mocking accordingly.
- Keep production code changes minimal; prefer test fixes unless a small production fix is clearly required for correctness.

## Non-Goals
- No refactor unrelated to T015 test scope.
- No broad CI pipeline redesign.
- Do not change system behavior unless necessary to correct a bug introduced by T015.

## Notes / Hints (Common causes)
- Waiting on async operations without timeouts
- Integration-like behavior creeping into a unit test (network, DB, external services)
- Missing mocks for time, scheduler, thread pools, reactive streams, or message queues
- Test order dependency / shared static state between tests
- Docker resource limits causing long waits (needs timeouts or deterministic scheduling)
