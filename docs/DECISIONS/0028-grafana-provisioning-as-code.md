# ADR-0028 : Grafana Provisioning as Code

## Status
Accepted

## Date
2026-11-04

## Context
Grafana's Prometheus data source (ADR-0015) and the basic technical
dashboard (ADR-0016) were created by hand in the UI and only lived in the
`grafana_data` volume; the dashboard JSON in the repo was a backup, not
something Grafana loaded. A fresh clone, a deleted volume, or — next
roadmap step — a Kubernetes pod rescheduled without persistent storage
all started from an empty Grafana, to be rebuilt manually.

The backup also wasn't portable: every panel referenced the data source
by the random UID Grafana generated at manual creation
(`dft30dachgum8a`), which doesn't exist on any other installation.

## Options Under Consideration
- **Keep the manual setup, document the clicks (rejected)** — not
  reproducible, and incompatible with disposable Kubernetes pods.
- **Grafana's file provisioning (chosen)** — YAML for the data source and
  a dashboard provider, plus dashboard JSON files, all mounted read-only
  into the container and loaded at startup. Built into Grafana, no extra
  tooling, maps directly onto Kubernetes ConfigMaps later.
- **Grafana's newer repository/Git Sync provisioning, Terraform, or
  Grafonnet/Grafana Foundation SDK** — more powerful (dashboards as
  generated code, multi-instance management), but extra tooling for one
  dashboard and one data source.

## Decision
- `infra/grafana/provisioning/datasources/prometheus.yml` declares the
  Prometheus data source with a **fixed UID (`prometheus`)** — the stable
  contract dashboards reference — `access: proxy` to
  `http://prometheus:9090` (the Grafana server queries Prometheus over the
  compose network), default, not editable in the UI.
- `infra/grafana/provisioning/dashboards/devops-platform.yml` is a file
  provider reading `/etc/grafana/dashboards` into a "DevOps Platform"
  folder, with `allowUiUpdates: false` and `disableDeletion: true`: git
  is the source of truth, the UI refuses to save provisioned dashboards
  (verified: `POST /api/dashboards/db` → 400). Workflow to change one:
  edit in the UI, export the JSON, commit it in a PR.
- `infra/grafana/dashboards/basic-technical.json` (moved from
  `infra/grafana/`) references the data source by `uid: prometheus` and
  has a fixed dashboard UID (`basic-technical`) for a stable URL.
- Both directories are bind-mounted read-only in `compose.yml`.

**Format pitfall found while implementing:** the backup had been
exported in Grafana's v2 dashboard schema (`elements`/`layout`), and
Grafana 13's file provisioning rejects it ("dashboard appears to be in v2
format"). Rather than rewriting the 8 panels by hand, the dashboard was
imported into a throwaway Grafana through the v2 API and read back
through the v1 API, letting Grafana itself convert it to the classic
`panels` schema (conversion reported no failure).

Verified on a throwaway Grafana with no volume (the fresh-clone case):
data source health check OK, dashboard loaded in its folder, every
panel's query answered by Prometheus.

## Consequences
- `make dev`/`make prod` on a fresh clone now gets a working Grafana with
  its dashboard, no manual step — and the same files become ConfigMaps
  on Kubernetes.
- Dashboards must be exported in the **classic** JSON format to be
  provisioned; a v2 export will be rejected at startup (visible in the
  Grafana logs as a provisioning error).
- An existing `grafana_data` volume keeps its hand-made data source and
  dashboard next to the provisioned ones (same names, different UIDs).
  Provisioning adds what is declared; it doesn't remove what it never
  owned. One-time cleanup: delete the old data source and the old
  dashboard in the UI, or delete the volume (nothing provisioned is
  lost).
- Grafana logs harmless errors for the optional `plugins/` and
  `alerting/` provisioning directories, which don't exist yet; alerting
  provisioning (roadmap step 12) will live there.
- Two of the panels (deployment completions and durations) show "No
  data" right after a backend restart until the first completion: those
  Micrometer meters are only registered on first use. Unrelated to
  provisioning; worth pre-registering them later.
