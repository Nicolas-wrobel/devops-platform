# ADR-0013 : Prometheus for Metrics Scraping

## Status
Accepted

## Date
2026-07-20

## Context
Roadmap step 6 ("Observability foundation"), second sub-step. The backend
already exposes `/actuator/prometheus` (Micrometer + Actuator, see ADR-0004
and the `micrometer-registry-prometheus` dependency added alongside it) —
but an exposed endpoint is just a snapshot at a given instant, with no
history. Something needs to pull that snapshot on a schedule and store it
over time before any dashboard (Grafana, next sub-step) or alerting can be
built on top of it.

Prometheus is the natural fit: it's the de-facto standard pull-based
metrics collector for exactly this Actuator/Micrometer exposition format,
and it's what the roadmap already names for this step.

## Options Under Consideration
- **New Docker Compose service (chosen) vs. running Prometheus outside
  Compose (host-installed, or a separate ad-hoc container)** — the project's
  entire local stack (`db`, `backend`, `frontend`) is already wired through
  `infra/docker/compose.yml` + `compose.dev.yml`/`compose.prod.yml`, launched
  only via `scripts/dc-dev.sh`/`dc-prod.sh` (ADR-0002). Adding `prometheus` as
  a fourth service keeps a single, reproducible entry point (`make dev`)
  instead of a manual side-process the rest of the team/future-me would have
  to remember to start separately.
- **Reaching the backend via the Compose service name (chosen) vs. via the
  host's exposed port** — no service in this project uses an explicit
  `networks:` block; every service already resolves the others by Compose
  service name over Docker's built-in DNS (e.g. `backend`'s
  `SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/...`). `prometheus`
  joins the same implicit default network automatically, so its
  `scrape_configs` target the backend as `backend:8080` — the same
  mechanism `db`/`backend` already use, no new networking concept
  introduced.
- **Providing `prometheus.yml` via a bind-mounted file (chosen) vs. baking a
  custom image with a `Dockerfile`** — `compose.yml` declares `prometheus`
  with `image: prom/prometheus:...` directly (no `build:`), exactly like
  `db` (`image: postgres:16.14`, no Dockerfile anywhere for it either). A
  bind mount (`./prometheus.yml:/etc/prometheus/prometheus.yml`) gets the
  scrape config into the container with zero build step, consistent with
  how this project already treats third-party, off-the-shelf images.
- **Image variant: default `prom/prometheus:v3.13.1` (chosen) vs.
  `v3.13.1-distroless`** — distroless was tried first for the smaller
  attack surface (same motivation as ADR-0012's alpine backend runtime), but
  it strips out `wget`/`sh` entirely. Confirmed by testing: the Docker
  Compose `healthcheck:` (`CMD wget --spider ...`) failed with `exec: "wget":
  executable file not found in $PATH` on every attempt. Unlike ADR-0012's
  backend case (where alpine was a smaller step that kept a usable shell),
  there's no intermediate "smaller but still checkable" option readily
  available here — either accept no healthcheck on `prometheus`, or drop
  distroless. The default (BusyBox-based) image was chosen to keep a
  healthcheck consistent with every other service in `compose.yml`
  (`db`, `backend`); the attack-surface reduction distroless would have
  given is deferred, not ruled out permanently.
- **Version pinning: precise tag `v3.13.1` (chosen) vs. floating `:latest`**
  — same rationale as ADR-0012 (`postgres:16.14`, not `postgres:16`):
  reproducible pulls, deliberate upgrades instead of silent drift.
- **`depends_on` on `backend` — not added.** Prometheus scraping a
  momentarily-down target just logs that target as `down` and retries on
  the next scrape interval; it doesn't need the backend to be healthy
  before *it* can start, unlike `backend`'s hard dependency on `db` being
  ready to accept connections.

## Decision
- Added a `prometheus` service to `infra/docker/compose.yml`: image
  `prom/prometheus:v3.13.1`, config bind-mounted from
  `infra/docker/prometheus.yml`, healthcheck via `wget --spider` against
  `/-/healthy` (same `CMD`/interval style as `db`/`backend`).
- `compose.dev.yml`/`compose.prod.yml` both add the same `command` flags
  (`--config.file`, `--storage.tsdb.path`, `--web.enable-lifecycle`) and
  expose the UI on a new `${PROMETHEUS_PORT}` host port (added to
  `.env.example`, following ADR-0003's convention) — identical between dev
  and prod, since Prometheus itself has no dev/prod behavioral split
  (unlike `backend`/`frontend`, which differ in build/run mode).
- `infra/docker/prometheus.yml`: a single `scrape_configs` job (`job_name:
  'backend-service'`), `metrics_path: '/actuator/prometheus'`, `targets:
  ['backend:8080']`, `scrape_interval: 15s`.

## Consequences
- `make dev` / `make prod` now bring up Prometheus alongside the rest of
  the stack with no extra manual step; `make dev-ps` reports its health
  like every other service.
- Metrics now have history (subject to Prometheus's local TSDB retention)
  instead of only the live snapshot Actuator exposes — the prerequisite for
  Grafana dashboards and any future alerting.
- Explicitly **not** done in this pass (deferred, not forgotten): Grafana
  and a technical dashboard (next roadmap sub-steps), `postgres_exporter`
  (Postgres has no native Prometheus format; discovered during this step,
  intentionally scoped out to keep this increment small), the distroless
  image variant (revisit if/when the healthcheck gap is closed some other
  way), and any alerting rules.
- If the healthcheck ever becomes a blocker again (e.g. reconsidering
  distroless), the same tension ADR-0012 already flagged for the backend
  applies here too — worth reading together if this is revisited.
