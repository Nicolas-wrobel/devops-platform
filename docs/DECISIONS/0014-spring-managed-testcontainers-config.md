# ADR-0014 : Spring-managed Testcontainers config, replacing per-class container churn

## Status
Accepted

## Date
2026-07-23

## Context
ADR-0010 introduced `AbstractIntegrationTest` (`@Testcontainers` on the class,
a `static final @Container @ServiceConnection PostgreSQLContainer` field),
extended by every integration test class, with the intent of one Postgres
container shared across the whole test run.

In practice, `@Container` ties the container's lifecycle to JUnit 5's
per-test-class `beforeAll`/`afterAll` callbacks — regardless of the field
being `static`. Verified locally (`./mvnw test`, reading the Testcontainers
log lines one by one): the same "shared" container was being stopped after
each test class's `afterAll` and a **new** Docker container recreated for
the next class — four separate `Creating container for image: postgres:16`
events, four different host ports, in a single run that only has one
`static` container declaration. This container churn is the most plausible
cause of an intermittent CI failure observed on `EnvironmentApiTest`
(`org.postgresql.util.PSQLException: Connection to localhost:XXXXX refused`
from HikariCP, immediately following a fresh container having just been
(re)created for that class) — a race between the stop/restart cycle and the
next Spring context trying to connect, more likely to lose under GitHub
Actions' shared/constrained runners than on a local machine.

## Options Under Consideration
- **Keep `@Container static` and tune timeouts/retries** — doesn't address
  the root cause (JUnit still owns start/stop per class), just makes the
  race window narrower without closing it.
- **`@ImportTestcontainers`** (Spring Boot's dedicated interface-based
  mechanism for importing `@Container` fields as Spring beans) — the
  officially documented path for cross-class container sharing.
- **A plain `@TestConfiguration(proxyBeanMethods = false)` with a `@Bean`
  method returning the container, annotated `@ServiceConnection` (chosen)**
  — same underlying goal as `@ImportTestcontainers` (hand container
  ownership to Spring instead of JUnit), expressed as an ordinary Spring
  bean. Simpler to reason about than introducing a second Testcontainers
  integration mechanism alongside `@ServiceConnection`, and makes explicit
  that `@Container` (a JUnit 5 extension marker) has no place on a `@Bean`
  factory method — mixing the two was the mistake in the first draft of
  this fix.

## Decision
Replaced `AbstractIntegrationTest` (`common/`) with **`PostgresContainerConfig`**,
a `@TestConfiguration(proxyBeanMethods = false)` exposing a single `@Bean`
`PostgreSQLContainer`, annotated `@ServiceConnection`. Every integration test
class now uses `@Import(PostgresContainerConfig.class)` instead of `extends
AbstractIntegrationTest`: `EnvironmentRepositoryTest`, `EnvironmentApiTest`,
`PrometheusEndpointTest`, `BackendApplicationTests`.

The container's lifecycle is now owned by Spring's `ApplicationContext`, not
by JUnit 5's per-class extension callbacks. Spring's test context cache
reuses an already-started context (and its container bean) across test
classes that share an identical bootstrap signature — confirmed locally:
`EnvironmentApiTest` and `PrometheusEndpointTest` (both `@SpringBootTest` +
`@AutoConfigureMockMvc` + the same `@Import`) share one container with no
restart between them, while `EnvironmentRepositoryTest` (`@DataJpaTest`) and
`BackendApplicationTests` (`@SpringBootTest` alone) each get their own — one
container per distinct Spring context shape, none of them torn down and
recreated mid-run.

## Consequences
- Eliminates the container stop/restart cycle between test classes that
  previously shared the same (now-removed) static field — directly
  addresses the suspected cause of the intermittent `Connection refused`
  failures in `EnvironmentApiTest` on CI.
- Not literally "one container for the entire suite" — each distinct Spring
  context configuration (slice test vs. full `@SpringBootTest`, with or
  without `@AutoConfigureMockMvc`) still gets its own container. This is
  expected and fine: contexts that can't share a `DataSource` bean couldn't
  meaningfully share a container connection either; what mattered was
  removing the unnecessary churn between classes that *do* share a context
  shape.
- ADR-0010 is left unmodified as the historical record of the original
  decision; this ADR documents why and how that approach was replaced,
  rather than rewriting history.
- Every future integration test class should use
  `@Import(PostgresContainerConfig.class)`, not `extends` — there is no
  base class left to extend.
