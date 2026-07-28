# ADR-0016 : Basic Technical Dashboard (RED + USE)

## Status
Accepted

## Date
2026-07-28

## Context
Roadmap step 6 ("Observability foundation"), fourth and last sub-step
before moving to the security foundation. ADR-0013 made Prometheus scrape
`/actuator/prometheus`; ADR-0015 connected Grafana to Prometheus as a data
source but explicitly deferred the actual dashboard, to be built by hand
through the UI as a first hands-on exposure to Grafana. This ADR covers
that dashboard: what it shows, why those panels, and one supporting
config change (Micrometer percentile histograms) needed to make one of
them possible.

## Options Under Consideration
- **Panel selection: RED only vs. RED + USE (chosen)** — RED (Rate,
  Errors, Duration) covers the backend as a service answering HTTP
  requests, but says nothing about the resources behind it (JVM, DB
  connection pool). USE (Utilization, Saturation, Errors) fills that gap.
  A basic technical dashboard needs both: RED to see if the service is
  serving traffic correctly, USE to see if it's about to run out of
  resources — the two are complementary, not redundant, and a
  request-latency spike is only actionable if you can immediately check
  whether it correlates with a resource constraint.
- **`http_server_requests_seconds` percentile latency: client-side
  percentiles vs. `histogram_quantile` over `_bucket` series (chosen)** —
  Micrometer can export pre-computed client-side percentiles
  (`management.metrics.distribution.percentiles`), but those can't be
  aggregated correctly across multiple instances (percentiles of
  percentiles are mathematically invalid). `histogram_quantile` over
  `_bucket` series computed by Prometheus is the standard approach and
  aggregates correctly, at the cost of enabling
  `management.metrics.distribution.percentiles-histogram.http.server.requests=true`
  in `application.properties` — this creates one additional Prometheus
  series per `le` bucket per existing `http_server_requests_seconds`
  label combination (`method`/`status`/`uri`/`outcome`), which is why
  Micrometer doesn't enable it by default. Accepted deliberately: this
  app has a small, bounded set of endpoints, so the added cardinality is
  small; this would need revisiting (narrower per-metric enabling, or
  dropping some label dimensions) if the endpoint surface grows a lot.
- **CPU + Heap on one combined panel vs. two separate panels (chosen:
  combined)** — both normalized to a 0-100% scale, plotted together on
  purpose: the point of this panel is to answer "is a latency spike
  caused by CPU pressure, memory pressure, or neither?" in one glance,
  which is harder to do by eye across two separately-scrolled panels.
- **Dashboard as JSON provisioning-as-code vs. built through the Grafana
  UI (chosen: UI, consistent with ADR-0015)** — same reasoning as the
  data source in ADR-0015: built by hand on purpose to actually learn
  Grafana's panel/query editor rather than drop in a working config.
  The JSON model is exported to `infra/grafana/basic-technical.json`
  (new top-level sibling of `infra/docker/`, `infra/k8s/`, `infra/helm/`)
  purely as a backup against losing the `grafana_data` volume, not as a
  provisioning mechanism — it is not auto-loaded on startup.

## Decision
Five panels, all querying the existing Prometheus data source:
- **Request Rate**:
  `sum(rate(http_server_requests_seconds_count[$__rate_interval]))`
- **Error Rate** (% of requests with a 5xx status):
  `100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[$__rate_interval])) / sum(rate(http_server_requests_seconds_count[$__rate_interval]))`
- **Latency P95**:
  `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[$__rate_interval])) by (le))`
  — requires `management.metrics.distribution.percentiles-histogram.http.server.requests=true`,
  added to `apps/backend/src/main/resources/application.properties`.
- **CPU + Heap** (combined panel, both as %):
  `process_cpu_usage * 100` and
  `100 * jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}`
- **Database Pool (Hikari)**: `hikaricp_connections_active`,
  `hikaricp_connections_idle`, `hikaricp_connections_max`

## Consequences
- One config change ships with this ADR: `application.properties` now
  enables percentile histograms for `http.server.requests`, adding
  `_bucket` series to `/actuator/prometheus` and to what Prometheus
  stores — a small, deliberate memory/storage cost for this app's size.
- The dashboard itself is **not** reproducible by cloning the repo and
  running `make dev` — same limitation ADR-0015 already accepted for the
  data source, now extended to the dashboard. `infra/grafana/basic-technical.json`
  is a manually exported backup (Dashboard settings → JSON Model), not
  wired into Grafana's provisioning — a lost/reset `grafana_data` volume
  still means re-importing it by hand through the UI, but at least the
  panel definitions aren't lost.
- Explicitly **not** done in this pass: provisioning-as-code for the
  dashboard, and any Grafana alerting on these panels (e.g. paging on
  sustained error rate or heap saturation) — both natural next steps if
  this dashboard proves useful, but out of scope for "basic".
- Closes roadmap step 6 (observability foundation). Next roadmap step is
  the security foundation.
