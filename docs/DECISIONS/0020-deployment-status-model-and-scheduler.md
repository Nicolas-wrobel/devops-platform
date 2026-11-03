# ADR-0020 : Deployment Status Model and Completion Scheduler

## Status
Accepted

## Date
2026-11-02

## Context
Roadmap step 9 ("Deployment domain"), the first business module whose
record changes state over time rather than being fully decided at
creation — unlike `Environment`/`Application`, a deployment's outcome
is not known in advance, and in practice an organization can have
several deployments queued for the same application/environment while
only one actually runs at a time. The module also doubled as a first
hands-on exposure to Spring's `@Scheduled` background jobs, a mechanism
not used anywhere else in this codebase yet.

## Options Under Consideration
- **State model: binary succeeded/failed decided at creation vs. a
  4-state machine `PENDING → RUNNING → SUCCESS|FAILED` (chosen)** — a
  real deployment's result isn't known until it actually runs, and
  `PENDING` specifically models a deployment queued behind another one
  already `RUNNING` for the same application/environment pair, which a
  binary model can't express at all.
- **`PENDING → RUNNING` transition: automatic at creation vs. an
  explicit second API call (chosen)** — an explicit `POST
  /api/deployments/{id}/start` is what lets the service enforce "only
  one `RUNNING` deployment per application/environment link" (rejecting
  with 409 otherwise) and models a deployment actually waiting its turn
  instead of starting the instant it's requested.
- **`RUNNING → SUCCESS|FAILED` transition: computed lazily on read vs.
  a periodic `@Scheduled` background job (chosen)** — a job is closer
  to how a real deployment finishes (on its own, regardless of whether
  anyone is currently looking at it) and was the deliberate way to learn
  `@Scheduled`/`@EnableScheduling`, which this project hadn't used
  before. A dedicated `DeploymentCompletionScheduler` (`@Component`,
  separate from `DeploymentController`) owns the `@Scheduled(fixedDelay
  = 10000)` method, keeping the "triggered periodically" concern out of
  both the REST layer and the business logic.
- **Testability of time- and chance-driven logic: calling
  `Instant.now()`/`new Random()` directly vs. injecting `Clock`/`Random`
  as beans (chosen)** — `common/ClockConfig.java` exposes both as
  beans, so `DeploymentServiceTest` can substitute a fixed `Clock` and a
  mocked `Random` to make `start()`/`completeDueDeployments()`
  deterministic.
- **History: a separate audit/history entity vs. the `Deployment` rows
  themselves (chosen)** — there's no `PUT`/`DELETE` on `Deployment`, so
  each row is already an immutable-once-terminal record; filtering/
  ordering those rows (`GET /api/deployments?applicationId=&environmentId=`)
  is enough to serve as history without a second table duplicating the
  same data.

## Decision
`DeploymentStatus` is `PENDING, RUNNING, SUCCESS, FAILED`. A `Deployment`
is created `PENDING`. `DeploymentService.start(id)` transitions it to
`RUNNING`, rejecting with `DeploymentConflictException` (409) if another
`RUNNING` deployment already exists for the same
`ApplicationEnvironment` link, and sets `plannedFinishAt` using a
random simulated duration between
`deployment.simulation.min-duration-seconds` and
`...max-duration-seconds`. `DeploymentCompletionScheduler` calls
`DeploymentService.completeDueDeployments()` every 10 seconds; it finds
`RUNNING` deployments whose `plannedFinishAt` is in the past and
resolves each to `SUCCESS` or `FAILED` using a random draw against
`deployment.simulation.success-probability`. `Clock` and `Random` are
constructor-injected into `DeploymentService` rather than used
statically.

## Consequences
- Makes it possible to exercise real Spring scheduling and a realistic
  queued-deployment workflow without a real deployment backend behind
  it.
- The single-`RUNNING`-per-link rule is enforced at the service layer
  (`existsByApplicationEnvironmentIdAndStatus` read, then write), not
  by a database constraint — under concurrent `start()` calls there is a
  narrow race window where both could pass the check before either
  writes. Accepted at this project's scale; a partial unique index (or
  optimistic locking) would be needed if this were a real concern.
- The scheduler's own `fixedDelay` (10s) means a deployment's actual
  completion always lags its `plannedFinishAt` by up to one tick —
  simplicity over precision, acceptable for a simulated duration.
- No separate history table: `Deployment` rows are the history, which
  keeps the data model smaller at the cost of relying on "no
  `PUT`/`DELETE` on this entity" staying true going forward.
- Closes the state-model/scheduling half of roadmap step 9. The
  Micrometer instrumentation half is covered separately in ADR-0021.

## Amendment (2026-11-03)
The race window on the single-`RUNNING`-per-link rule described above is
closed: a partial unique index now enforces it in the database, with the
service-layer check kept as a fast path — see
[ADR-0026](0026-database-enforced-single-running-deployment.md).
