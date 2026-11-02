# ADR-0021 : Deployment Business Metrics (Micrometer Gauge/Counter/Timer)

## Status
Accepted

## Date
2026-11-02

## Context
ADR-0013/0015/0016 cover generic RED/USE metrics (HTTP, JVM, Hikari),
auto-instrumented by Spring Boot/Micrometer. Roadmap step 9 wanted
business-level visibility into the `Deployment` module itself — how
many deployments are in each state right now, how many have completed,
how long they take — and doubled as a first hands-on use of
Micrometer's custom instrumentation API (`Gauge`/`Counter`/`Timer`),
not used anywhere else in this codebase yet.

## Options Under Consideration
- **Current count per status: a `Gauge` backed by a live repository
  query vs. in-memory counters incremented/decremented on every
  transition (chosen: live query)** — in-memory counters would
  duplicate state already in the database, drift on application
  restart (reset to zero while the DB still holds the real counts), and
  need careful +1/-1 bookkeeping on every transition. A
  `Gauge.builder(name, repository, r -> r.countByStatus(status))`
  registers once and re-runs the query on every Prometheus scrape —
  always correct, nothing to keep in sync, at the cost of one `COUNT`
  query per status per scrape.
- **Status representation: one metric name per status (`deployments.PENDING`,
  `deployments.RUNNING`, ...) vs. one metric name with a `status` tag
  (chosen: separate names)** — the same tag-based approach used for the
  completion `Counter`/`Timer` below was considered here too, but for a
  fixed, small, known-in-advance set of states, four independent
  single-value gauges are simpler to query directly by name, with no
  aggregation needed on the Grafana side.
- **Total completions: a `Counter` tagged by final status vs. deriving
  a total from the `SUCCESS`/`FAILED` gauges (chosen: `Counter`)** — a
  `Counter` is the metric type Prometheus expects for `rate()`/
  `increase()` queries over time (a proper event stream), whereas
  summing two live gauges only gives a snapshot total tied to whatever
  rows currently exist — correct today because `Deployment` has no
  `DELETE`, but a `Counter` keeps accumulating independently of what
  happens to the underlying rows later.
- **Duration: a `Timer` vs. manually tracked sum/count fields (chosen:
  `Timer`)** — `Timer.record(duration)` maintains count, sum, and max
  in one call (and would support percentile buckets later), instead of
  reimplementing that bookkeeping by hand.
- **Where to trigger the `Counter`/`Timer` recording: inside
  `Deployment.complete()` vs. from `DeploymentService.completeDueDeployments()`
  right after calling it (chosen: the service)** — `Deployment` is a
  plain JPA entity, instantiated with `new Deployment(...)` and never
  managed by Spring, so it has no way to receive an injected
  `MeterRegistry`. The recording has to happen in the Spring-managed
  caller, at the exact point the "deployment completed" event is known.
- **`DeploymentMetricsRegistry` dependencies: inject `DeploymentService`
  vs. inject `DeploymentRepository` directly (chosen: the repository)**
  — `DeploymentService` needs to call this component for the `Counter`/
  `Timer`, so having the component depend back on `DeploymentService`
  would create a circular bean dependency. It depends on
  `DeploymentRepository` directly instead, the same repository
  `DeploymentService` itself already uses for the same kind of reads.

## Decision
`DeploymentMetricsRegistry` (`@Component`) constructor-injects
`MeterRegistry` and `DeploymentRepository`. Its constructor registers
four `Gauge`s (`deployments.PENDING/RUNNING/SUCCESS/FAILED`), each a
live `countByStatus` query. It exposes
`recordCompletion(DeploymentStatus status, Duration duration)`, which
increments a `deployments.completed` `Counter` tagged `status=...` and
records into a `deployments.duration` `Timer` tagged the same way.
`DeploymentService.completeDueDeployments()` calls it right after
`deployment.complete(...)`, passing the actual elapsed duration
(`Duration.between(startedAt, finishedAt)`).

## Consequences
- `/actuator/prometheus` now exposes deployment business metrics
  alongside the generic RED/USE ones, with no extra application state
  to keep in sync — the gauges recompute on every read.
- Three panels were added to the existing Grafana dashboard (ADR-0016):
  current counts per status (`Stat`), cumulative completions over time
  (`Time series`), and average completion duration in seconds
  (`Time series`, `deployments_duration_seconds_sum /
  deployments_duration_seconds_count`) — re-exported to
  `infra/grafana/basic-technical.json`.
- Known limitation: four separate metric names for status counts
  (rather than one tagged metric) works while the state set stays
  fixed and small; it would need to become tag-based if the status
  model grows or needs a generic per-status alerting rule.
- Closes the metrics-instrumentation half of roadmap step 9 (the
  state-machine/scheduler half is ADR-0020).
