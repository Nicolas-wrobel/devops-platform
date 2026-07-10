# ADR-0003 : Environment files strategy

## Status
Accepted

## Date
2026-07-10

## Context
The stack needs per-environment configuration — database credentials, ports,
service URLs — that differs between dev and prod, without ever committing
secrets to git, and without making onboarding (cloning the repo and getting
it running) harder than it needs to be.

## Options Under Consideration
- **Hardcode values directly in the Compose files** — simplest to read, but
  makes secrets unavoidable in git and gives no way to vary config per
  environment.
- **A single shared `.env`** — one file to maintain, but can't represent
  dev and prod having different values (ports, credentials) at the same time,
  and still risks being committed by accident.
- **Per-environment `.env.dev` / `.env.prod`, gitignored, plus a committed
  `.env.example` (chosen)** — one file per environment holding real values,
  never committed; `.env.example` committed with dummy values as the
  documented contract of which keys are required.

## Decision
`infra/docker/.env.dev` and `infra/docker/.env.prod` hold the real values
(`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_PORT`,
`SPRING_BOOT_PORT`, `REACT_PORT`) and are gitignored. `infra/docker/.env.example`
is committed with dummy values for the same keys, kept in sync manually, and
is what a new clone copies to get started. The wrapper scripts
(`scripts/dc-dev.sh` / `dc-prod.sh`, see [ADR-0002](0002-docker-compose-env-split.md))
pass the matching file via `docker compose --env-file`, so the Compose files
themselves never hardcode environment-specific values.

## Consequences
- The same Compose files work unmodified across dev and prod — only the env
  file passed in changes.
- Secrets never enter git history; `.env.example` gives onboarding a clear,
  versioned list of required keys with placeholder values.
- `.env.example` can drift from the real required keys over time since
  nothing enforces the two staying in sync — needs manual discipline when
  adding/removing a variable until an automated check (e.g. a CI lint step)
  exists.
