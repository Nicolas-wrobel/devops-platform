# ADR-0010: Shared Testcontainers base class for integration tests

## Status
Accepted

## Date
2026-07-13

## Context
`EnvironmentRepositoryTest` and `EnvironmentApiTest` (see
[ADR-0009](0009-testcontainers-for-persistence-tests.md)) each independently
declared their own `@Container @ServiceConnection static final
PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")`. Fixing
`BackendApplicationTests` — which had no Testcontainers wiring at all and
implicitly depended on a locally reachable Postgres at `localhost:5432`, breaking
on any clean environment without one running — meant either copy-pasting that same
block a third time, or extracting it once, before a fourth business module
(`Application`, `Deployment`, ...) needed it a fourth time.

## Options Under Consideration
- Keep duplicating the `@Container`/`@ServiceConnection` block in every test class
  that needs a real Postgres — no new abstraction to learn, but the exact same
  boilerplate now exists in three places, and will keep growing with every future
  module's persistence/integration tests.
- Extract a shared abstract base class that every integration test extends —
  the container is declared once; adding a new test class that needs a real
  database becomes a one-line `extends AbstractIntegrationTest`.

## Decision
Extract `AbstractIntegrationTest` under
`src/test/java/com/devops_platform/backend/common/` (mirroring the main-code
`common/` package from [ADR-0005](0005-package-by-feature-for-business-modules.md)).
It carries `@Testcontainers` and the single `postgres:16` container declaration.
`EnvironmentRepositoryTest`, `EnvironmentApiTest`, and `BackendApplicationTests` all
now `extend` it instead of declaring their own container — `@Testcontainers` is a
JUnit 5 extension registered via `@ExtendWith`, which subclasses inherit
automatically, so no annotation needs repeating on the subclasses either.

## Consequences
- Every future module's persistence/integration tests reuse the same container
  declaration for free — just `extends AbstractIntegrationTest`, consistent with
  how `common/NotFoundException`/`ConflictException` are already reused on the
  main-code side (ADR-0008).
- `BackendApplicationTests` no longer has a hidden dependency on a locally running
  Postgres — this was a real gap that would have broken on a clean CI runner
  (the concrete risk ADR-0009 flagged), now closed before CI is set up.
- If a future test genuinely needs a *different* database version or a
  differently-configured container, it can still declare its own instead of
  extending this base — the shared base is a convenience for the common case, not
  a hard requirement.
