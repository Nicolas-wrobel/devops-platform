# ADR-0026 : Database-Enforced "One RUNNING Deployment per Link"

## Status
Accepted

## Date
2026-11-03

## Context
ADR-0020 introduced the business rule "at most one `RUNNING` deployment
per application/environment link", enforced in `DeploymentService.start()`
as a check-then-act: `existsByApplicationEnvironmentIdAndStatus(...,
RUNNING)`, then transition to `RUNNING`.

A pre-Kubernetes audit flagged this as a race condition. Two concurrent
`start` requests on the same link run in separate transactions; under
PostgreSQL's default `READ_COMMITTED` isolation, neither sees the
other's uncommitted `RUNNING` row, so both checks pass and both commit.
Application code alone cannot close this gap — only the database sees
every concurrent transaction. This becomes more likely, not less, once
the backend runs as several replicas on Kubernetes.

## Options Under Consideration
- **Pessimistic lock on the link row (`SELECT ... FOR UPDATE`)** —
  serializes starts per link. Correct, but every future write path that
  can create a `RUNNING` row must remember to take the lock; the rule
  stays a convention.
- **Optimistic locking (`@Version` on `Deployment`)** — detects
  concurrent updates of the *same* row, not two *different* rows racing
  for the same link. Doesn't address this bug.
- **`SERIALIZABLE` isolation for `start`** — correct, but surfaces as
  generic serialization failures that need retry logic, and is easy to
  lose in a refactor.
- **A plain `UNIQUE (application_environment_id, status)`** — would also
  forbid two `SUCCESS`/`FAILED` rows per link, i.e. the whole history.
- **Partial unique index (chosen)** — `CREATE UNIQUE INDEX ... ON
  deployment (application_environment_id) WHERE status = 'RUNNING'`.
  Uniqueness only applies to `RUNNING` rows. The invariant holds no
  matter which code path, replica, or manual SQL writes to the table.

## Decision
- Flyway `V5__enforce_single_running_deployment_per_link.sql` adds the
  partial unique index `uk_deployment_one_running_per_link`.
- `DeploymentService.start()` keeps the existing check as a fast path
  (clear `409` without touching the index in the common, non-concurrent
  case), then calls `repository.flush()` so a violation surfaces inside
  the method and is translated to the same `DeploymentConflictException`
  — instead of a generic `DataIntegrityViolationException` at commit
  time, after the method already returned.
- `DeploymentConcurrencyTest` reproduces the race deterministically
  against real PostgreSQL: transaction 1 starts deployment A without
  committing, transaction 2 starts B on the same link and is observed
  blocked on the index (`pg_stat_activity`, no arbitrary sleep), then
  transaction 1 commits and transaction 2 must fail with a conflict.
  Verified to fail without the migration.

## Consequences
- The rule is now a database invariant, safe under any number of
  backend replicas.
- Applying V5 fails on a database that already holds two `RUNNING`
  deployments for one link — not possible with the seeded data, but any
  such row would need fixing first.
- The `catch (DataIntegrityViolationException)` in `start()` assumes the
  index is the only constraint the transition can violate (it only
  changes `status`/`startedAt`/`plannedFinishAt`). If that changes,
  inspect the constraint name instead.
- `DeploymentRepositoryTest` previously built two `RUNNING` deployments
  on the same link — a state the rule forbids; it now uses two links.
