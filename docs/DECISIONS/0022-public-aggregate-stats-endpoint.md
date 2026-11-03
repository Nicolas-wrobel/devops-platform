# ADR-0022 : Public Aggregate Stats Endpoint

## Status
Accepted

## Date
2026-11-02

## Context
A visual/UX redesign of the frontend adds a public landing page (no login required),
meant to showcase this portfolio project to a stranger landing on the URL. A static
page alone (text + screenshots) works, but a genuinely live signal — a handful of
numbers that actually come from the running system — makes the demo feel real rather
than staged. Today, every route except `/api/auth/login` and `/actuator/health`
requires authentication (see `SecurityConfig.java`), and `/actuator/prometheus`
additionally requires `ROLE_METRICS` Basic auth — so some new, deliberately scoped
public surface was needed.

## Options Under Consideration
- **Expose `/actuator/prometheus` publicly (rejected)** — would mean dropping the
  `ROLE_METRICS` requirement on that endpoint entirely, or adding a second unauthenticated
  route onto the same meters. Either way it exposes far more than a landing page needs:
  JVM/HTTP internals, Hikari pool state, deployment duration histograms — operational
  detail that has no business being public, and that this project already deliberately
  gates behind a dedicated role (see ADR-0013/0018).
- **`permitAll` on the existing GET `/api/environments`, `/api/applications`,
  `/api/deployments` endpoints (rejected)** — these return full resource lists
  (names, descriptions, who triggered a deployment, timestamps). Loosening them for
  the sake of a landing page would conflate "public marketing numbers" with "the real
  CRUD API," and leak data that has no reason to be public.
- **A new, narrow, read-only public endpoint returning only aggregate counts (chosen)**
  — `GET /api/public/stats` returns four fields: `applicationCount`,
  `environmentCount`, `deploymentCount`, `successRatePercent`. Nothing else: no names,
  no IDs, no timestamps, no `triggeredBy`, no raw Prometheus series. Cheap to compute
  (three `COUNT` queries, reusing `JpaRepository.count()` and the existing
  `DeploymentRepository.countByStatus` already used by `DeploymentMetricsRegistry`).

## Decision
New `stats` package (`com.devops_platform.backend.stats`) — it legitimately spans the
`application`/`environment`/`deployment` modules, so it doesn't belong inside any one
of them. `StatsController` exposes a single `GET /api/public/stats`, backed by
`StatsService`, returning `StatsResponse(applicationCount, environmentCount,
deploymentCount, successRatePercent)`. `successRatePercent` is `null` when there are
zero completed (`SUCCESS`/`FAILED`) deployments, rather than a misleading `0.0`.
`SecurityConfig`'s JWT filter chain gets one new `permitAll` matcher for `GET
/api/public/stats`, added above the existing `/api/environments/**` etc. rule — no
other matcher is touched.

## Consequences
- One new always-public, unauthenticated `GET` route now exists. It is read-only and
  cheap, but it is still a new piece of attack surface (however small) — future
  contributors must resist the temptation to add "just one more field" to
  `StatsResponse`; anything beyond an aggregate count or rate should go through the
  existing authenticated endpoints instead, not this one.
- No `ROLE_METRICS`/Basic-auth reuse: this endpoint intentionally does not share any
  mechanism with `/actuator/prometheus` — they answer different questions (internal
  operational metrics vs. a public marketing summary) and should stay decoupled so a
  future change to one doesn't silently change the other's exposure.
- The frontend landing page can now show a genuinely live signal without requiring a
  backend change every time new "public-safe" data is wanted — any future addition
  still has to go through the same narrow-scope discipline this ADR establishes.

## Addendum (2026-11-02)
`StatsResponse` gained four fields — `pendingCount`, `runningCount`, `successCount`,
`failedCount` — so the public landing page can render the same per-status bar chart
the authenticated dashboard already shows (see ADR-0023), without requiring a login.
This doesn't change the boundary established above: still aggregate counts only, no
names/IDs/timestamps, computed via the same `DeploymentRepository.countByStatus` calls
already used both here and by `DeploymentMetricsRegistry` (ADR-0021).
