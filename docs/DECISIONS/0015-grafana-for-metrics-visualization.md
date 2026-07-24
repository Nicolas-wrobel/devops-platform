# ADR-0015 : Grafana for Metrics Visualization

## Status
Accepted

## Date
2026-07-24

## Context
Roadmap step 6 ("Observability foundation"), third sub-step. Prometheus
(ADR-0013) now scrapes `/actuator/prometheus` and stores metrics over time,
but its own UI is built for ad-hoc PromQL queries and target/alert
debugging — not for building persistent, shareable dashboards. Grafana is
the natural next layer: it queries Prometheus as a data source and lets
panels/dashboards be built and saved on top of it. The "basic technical
dashboard" sub-step that follows this one needs a running, connected
Grafana instance to exist first.

## Options Under Consideration
- **Image: `grafana/grafana` (chosen) vs. `grafana/grafana-oss`** — both
  repositories currently ship the same OSS-licensed image (Enterprise
  features stay locked without a license key), but Grafana Labs announced
  that `grafana/grafana-oss` stops receiving updates starting from release
  `12.4.0`, with `grafana/grafana` as the maintained successor. Using the
  `-oss` tag today would mean starting this service on a repository that's
  already scheduled to go stale.
- **Version pinning: precise tag `13.1.1` (chosen) vs. floating `:latest`**
  — same rationale as every other service in this stack (`postgres:16.14`,
  `prom/prometheus:v3.13.1`): reproducible pulls, deliberate upgrades
  instead of silent drift.
- **Healthcheck: `wget --spider` against `/api/health` (chosen)** — verified
  first, same lesson ADR-0013 already learned the hard way with
  Prometheus's distroless variant: the `grafana/grafana` image is a minimal
  base with `wget` available but no `curl`. `wget --quiet --tries=1
  --spider http://localhost:3000/api/health` was confirmed to work inside
  the container before being committed to `compose.yml`, keeping the same
  healthcheck style as every other service (`db`, `backend`, `prometheus`).
- **Persistence: named volume `grafana_data:/var/lib/grafana` (chosen) vs.
  provisioning as code (datasource/dashboard YAML committed to the repo)**
  — Grafana keeps its internal state (dashboards, data sources, users) in
  a local sqlite database under `/var/lib/grafana` by default; a named
  volume (same convention as `postgres_data`/`prometheus_data`) makes that
  state survive `docker compose down`/restarts. Provisioning-as-code
  (YAML-defined data sources/dashboards auto-loaded on startup) was
  deliberately **not** done in this pass: the data source and the eventual
  dashboard are being built by hand through the Grafana UI on purpose, to
  actually learn the tool (first hands-on exposure to Grafana) rather than
  drop in a pre-baked config. This is a scope choice, not an oversight —
  worth revisiting once the concepts are familiar and reproducibility
  across environments matters more than the learning value of clicking
  through the UI.
- **Admin credentials via `GF_SECURITY_ADMIN_USER` /
  `GF_SECURITY_ADMIN_PASSWORD` env vars (chosen) vs. the image's default
  `admin`/`admin`** — same pattern already used for Postgres
  (`POSTGRES_USER`/`POSTGRES_PASSWORD`): real values live in
  `.env.dev`/`.env.prod` (gitignored), dummy placeholders in
  `.env.example` (ADR-0003's convention). Shipping the default admin/admin
  combination was ruled out as an easily avoidable bad practice.
- **`depends_on: prometheus: condition: service_healthy` (chosen) vs. no
  `depends_on`** — ADR-0013 deliberately skipped `depends_on` from
  `prometheus` to `backend`, because a momentarily-down scrape target just
  gets retried on Prometheus's own scrape loop. Grafana's relationship to
  Prometheus is different: it isn't polling on a fixed interval, it
  queries on demand (UI load, "Save & Test" on the data source, dashboard
  render) — so there's no automatic retry loop underneath it the way there
  is for a Prometheus scrape target. Tested in practice: `condition:
  service_healthy` makes Compose wait for Prometheus's existing
  `wget --spider http://localhost:9090/-/healthy` healthcheck to pass
  before starting Grafana — this reuses the healthcheck already in place
  rather than needing a separate wait-for-it script, and means the
  Prometheus data source has a real chance of connecting successfully the
  first time someone opens Grafana right after `make dev`.

## Decision
- Added a `grafana` service to `infra/docker/compose.yml`: image
  `grafana/grafana:13.1.1`, admin credentials from
  `GF_SECURITY_ADMIN_USER`/`GF_SECURITY_ADMIN_PASSWORD`, data persisted in
  a named `grafana_data` volume, healthcheck via `wget --spider` against
  `/api/health` (same `CMD`/interval style as `db`/`backend`/`prometheus`),
  and `depends_on: prometheus: condition: service_healthy`.
- `compose.dev.yml`/`compose.prod.yml` both expose the UI on a new
  `${GRAFANA_PORT}` host port, identical between dev and prod (no dev/prod
  behavioral split for this service, same as `prometheus`).
- New keys added to `infra/docker/.env.example`: `GRAFANA_PORT`,
  `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` (dummy values, real values
  kept locally in the gitignored `.env.dev`/`.env.prod`).
- The Prometheus data source was added manually through the Grafana UI
  (Connections → Data sources → Prometheus, URL `http://prometheus:9090`,
  no auth/TLS — nothing in `prometheus.yml`/`compose.yml` puts any auth
  layer in front of Prometheus), confirmed working via "Save & Test" and by
  browsing Micrometer metrics (`application_ready_time_seconds`, etc.) in
  Grafana's Explore view.

## Consequences
- `make dev` / `make prod` now bring up Grafana alongside the rest of the
  stack with no extra manual step; `make dev-ps` reports its health like
  every other service, and its startup order is guaranteed relative to
  Prometheus.
- Grafana dashboards/data sources/users now persist across restarts, but
  the current setup is **not** reproducible by just cloning the repo and
  running `make dev` — the Prometheus data source has to be re-added by
  hand on a fresh volume. Acceptable for now (explicit scope choice above);
  revisit with provisioning-as-code if/when a teammate or a fresh
  environment needs this to "just work" without manual clicking.
- Explicitly **not** done in this pass (deferred, not forgotten): the
  actual "basic technical dashboard" (next roadmap sub-step — building
  PromQL-backed panels is left as hands-on exploration), any
  provisioning-as-code for data sources/dashboards, and any Grafana
  alerting.
