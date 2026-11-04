# ADR-0027 : Multi-Replica-Safe Deployment Completion Job

## Status
Accepted

## Date
2026-11-04

## Context
`DeploymentCompletionScheduler` (ADR-0020) is a Spring `@Scheduled` job:
it runs once per JVM, not once per application. Once the backend runs as
several replicas on Kubernetes (roadmap step 10), every replica runs
`completeDueDeployments()` every 10 seconds. Under `READ_COMMITTED`, they
all see the same due `RUNNING` rows and each one:
- draws its own random outcome (`new Random()`, a different seed per
  JVM) — one replica can write `SUCCESS`, another `FAILED`, and the last
  commit wins;
- records the completion in its own Micrometer counters/timers, which
  Prometheus then sums across pods — the same deployment counted as both
  a success and a failure, inconsistent with the database.

## Options Under Consideration
- **Only one replica runs the job — distributed lock / leader election
  (ShedLock, Kubernetes leader election)** — valid and simple to reason
  about, but adds a dependency and a lock table, and needs a lock
  lifetime (`lockAtMostFor`) to recover when the lock holder dies. Work
  is never spread across replicas (not an issue for this tiny job).
- **All replicas run, each row is claimed by a column written in the row
  (`claimed_by`, `claimed_at`)** — a committed claim survives the death
  of the pod that wrote it, so it needs a lease expiry and a cleanup
  path.
- **All replicas run, each row is claimed by a PostgreSQL row lock —
  `SELECT ... FOR UPDATE SKIP LOCKED` (chosen)** — a row lock belongs to
  the transaction and is released at `COMMIT` or `ROLLBACK`, nothing
  else. If a pod dies mid-job, its connection drops, PostgreSQL rolls the
  transaction back, the locks are released and the rows are untouched —
  the next tick on any replica picks them up. `SKIP LOCKED` makes other
  replicas skip claimed rows instead of waiting for them. No new
  dependency, no new table, and the database stays the arbiter of
  concurrency, as in ADR-0026.

## Decision
`DeploymentRepository.findByStatusAndPlannedFinishAtBefore` is annotated
`@Lock(PESSIMISTIC_WRITE)` with the `jakarta.persistence.lock.timeout`
hint set to `-2` (Hibernate's `SKIP_LOCKED`). On PostgreSQL, Hibernate
renders it as `FOR NO KEY UPDATE ... SKIP LOCKED` — a slightly weaker
lock than `FOR UPDATE` that still excludes other writers of the row but
doesn't block inserts referencing it by foreign key; enough here since
the job never changes the primary key.

`completeDueDeployments()` keeps the `SELECT` and the `complete(...)`
updates in a single transaction (class-level `@Transactional` on
`DeploymentService`) — the locks last exactly as long as that
transaction.

`DeploymentConcurrencyTest` plays two replicas deterministically: one
claims the due row and rolls back (a pod dying mid-job), the other must
finish without waiting and without touching the row; the next tick then
completes it. Verified to fail both without any lock (the second replica
processes the row) and with a plain `FOR UPDATE` (it blocks).

## Consequences
- The completion job is safe with any number of replicas: each due
  deployment is completed and counted exactly once.
- Recovery after a crash is automatic, with no lease or cleanup job.
- If a whole node disappears (no TCP FIN/RST sent), PostgreSQL only
  notices through TCP keepalives, so the rows stay locked until then —
  delayed, not lost. `idle_in_transaction_session_timeout` or tighter
  keepalives would shorten that window if it ever matters.
- The lock semantics depend on the transaction boundary; the comment on
  `completeDueDeployments()` guards that invariant.
- Delivery is "at least once with no partial state": a rolled-back claim
  writes nothing, so re-processing it is safe. Metrics are recorded
  before commit, so a commit failure after `recordCompletion` would still
  over-count — acceptable for a simulated job, worth revisiting if the
  metrics ever feed alerting.
- The scheduler's interval stays per replica: with N replicas the table
  is polled N times every 10 seconds. Negligible today.
