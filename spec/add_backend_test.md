Goal:
Increase backend unit-test coverage to >= 80% for these packages/classes:
- com.sinker.app.service.*
- com.sinker.app.util.*
- com.sinker.app.converter.*
- com.sinker.app.dto.auth.*
(ONLY these targets must meet >=80%; don't try to raise global coverage.)

How to verify:
Run:
make coverage-backend

What to use:
- Use JUnit 5 + Mockito for unit tests.
- Do NOT rely on MariaDB / external services (unit tests only).
- Prefer testing business logic (services/utils), not getters/setters.
- Keep production behavior unchanged.
- If existing code is hard to unit test, refactor minimally (constructor injection, extract pure methods) but do not change behavior.

Process:
1) Generate the JaCoCo HTML report and identify which target classes are below 80%.
2) Add focused unit tests to cover missing branches and edge cases.
3) Re-run the verification command until the rule passes.
4) Summarize which tests were added and which classes reached >=80%.
