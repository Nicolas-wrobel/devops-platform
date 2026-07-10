# ADR-0009: Testcontainers for persistence and integration tests

## Status
Accepted

## Date
2026-07-10

## Context
`Environment` is the first entity with real persistence logic (a unique-name
constraint enforced partly in the database, partly in the service layer) and the
first Flyway migration. Something has to run the repository and full-API tests
against a real database engine so a test suite failure means something is actually
wrong, not just an artifact of a stand-in database.

## Options Under Consideration
- H2 in-memory database — `@DataJpaTest` runs fast, no Docker dependency to execute
  `./mvnw test`. Risk: H2 does not fully match PostgreSQL's SQL dialect and type
  behavior, so a test could pass against H2 and still fail against the real engine
  used in dev/prod (e.g. how the unique constraint or enum-as-string mapping behaves).
- Testcontainers — spins up a real, disposable `postgres:16` container per test class
  (matching the exact image used in `infra/docker/compose.yml`) via
  `spring-boot-testcontainers` + `@ServiceConnection`. Full fidelity with prod, at
  the cost of requiring Docker to run `./mvnw test` locally, and a slower suite.

## Decision
Testcontainers. `EnvironmentRepositoryTest` (`@DataJpaTest` +
`@AutoConfigureTestDatabase(replace = NONE)` + `@ServiceConnection` Postgres
container) and `EnvironmentApiTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` +
the same container pattern) both run the Flyway migration and the real JPA/Hibernate
mapping against `postgres:16` — the same image the dev/prod Compose stack uses.

## Consequences
- Running `./mvnw test` now requires Docker to be available locally — already true
  for the rest of this project's workflow (`make dev`/`make prod`), so it's not a new
  constraint for this environment, but worth remembering if tests ever need to run
  somewhere without Docker.
- The upcoming CI step (roadmap step 4) will need Docker-in-Docker (or an equivalent)
  available in the pipeline for these tests to run — a concrete requirement to carry
  into that ADR when CI is set up.
- Test suite is slower than an in-memory alternative (container startup per test
  class), which is an accepted trade-off for the confidence gained; container reuse
  across test classes can be revisited later if suite time becomes a problem.
