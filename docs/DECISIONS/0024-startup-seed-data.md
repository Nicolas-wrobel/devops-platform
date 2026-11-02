# ADR-0024 : Demo Data Seeded on Startup

## Status
Accepted

## Date
2026-11-02

## Context
A freshly deployed instance of this app — dev, prod, or a live portfolio
demo — starts with an empty database: no applications, no environments, no
deployments. Every page (landing page stats, dashboard, the three CRUD
pages) looks empty and unconvincing until someone manually creates data.
Since this is explicitly a demo/portfolio project, showing real-looking
data by default is the point, not an afterthought.

## Options Under Consideration
- **A Flyway migration inserting demo rows (rejected)** — Flyway was
  deliberately established as the schema-only authority (ADR-0006);
  mixing demo data into the versioned migration chain conflates two
  different concerns (schema evolution vs. application-level seed data)
  and would need its own `DELETE`/cleanup story if the data ever needed
  to change shape.
- **A Spring `ApplicationRunner` bean that seeds on an empty database
  (chosen)** — runs once per application startup, checks
  `environmentRepository.count() == 0` before doing anything (idempotent
  across restarts — a populated database is left untouched), and keeps
  Flyway's job unchanged. Seeds 3 environments, 2 applications, all six
  application/environment links, and 8 deployments (6 terminal —
  `SUCCESS`/`FAILED` — plus 1 left `PENDING` so the "Start" button has
  something to demo immediately).
- **Backdating seeded timestamps for a "lived-in" look (considered,
  dropped)** — `Environment`/`Application`/`Deployment.createdAt` are all
  `@CreationTimestamp`-managed with no setter; Hibernate stamps real wall-
  clock time at insert, not a value the seeder can override without
  reflection. Accepted as-is: seeded rows show today's date, which reads
  perfectly reasonably as "freshly deployed instance." `startedAt`/
  `finishedAt` on seeded deployments ARE still deliberately realistic
  (computed via the injected `Clock` and `Deployment`'s own `start()`/
  `complete()` methods, same ~20s duration the real scheduler simulates).
- **Keeping it enabled during the automated test suite (rejected)** —
  every Testcontainers-based `@SpringBootTest` starts from a genuinely
  empty database on purpose (ADR-0009/0010); several tests assert exact
  counts or empty-list responses, which the seeder would silently break.
  `DataSeeder` is annotated `@Profile("!test")`; `pom.xml`'s
  `maven-surefire-plugin` activates the `test` profile for every `mvn
  test` run (`systemPropertyVariables` → `spring.profiles.active=test`),
  so this is automatic — no per-test-class opt-out needed. The one test
  that actually exercises the seeder (`DataSeederTest`) opts back in via
  `@ActiveProfiles("data-seeder-test")` instead.

## Decision
`DataSeeder` (`com.devops_platform.backend`, top-level — it spans all
three business modules, same reasoning as `stats/` in ADR-0022)
implements `ApplicationRunner`, constructor-injects the four repositories
plus `Clock`, and is active everywhere except the `test` profile.

## Consequences
- A fresh `make dev`/`make prod`/first deploy now shows realistic data
  immediately — no manual setup needed to see the app "alive."
- The seeded data's `createdAt` timestamps are "whenever the app first
  started," not artificially backdated — a known, accepted limitation,
  not a bug.
- Discovered while building this: a stale Maven build output
  (`target/test-classes/application.properties`, left over from an
  earlier, reverted approach to gating the seeder) silently shadowed the
  real `src/main/resources/application.properties` for every test run
  until a `mvn clean` was done — a reminder that `mvn test` without
  `clean` can mask exactly this kind of resource-shadowing regression.
- Known limitation: if the seed data's shape ever needs to change
  (different sample apps, more variety), existing deployed databases
  that were seeded under the old shape won't be updated — this seeder
  only ever acts on a genuinely empty database, by design.
