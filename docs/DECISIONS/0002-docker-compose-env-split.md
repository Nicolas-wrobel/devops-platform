# ADR-0002 : Launch contract — Makefile, wrapper scripts, and Compose base/overlay split

## Status
Accepted

## Date
2026-07-10

## Context
The stack needs a single, identical way to start regardless of who runs it
or where — the same parameters and versions everywhere, so that "works on my
machine" problems don't happen. On top of that, dev and prod run structurally
differently (hot-reload with bind-mounted source vs. an immutable built
artifact) but should share as much configuration as possible to avoid the two
setups drifting apart.

This covers two related decisions: what the "launch contract" command looks
like, and how the Compose files are organized to serve both dev and prod
without duplicating config.

## Options Under Consideration

**Launch contract:**
- A single shell script (e.g. `./scripts/dev.sh`) — minimal, but no discovery
  of available commands and no shared plumbing between dev/prod invocations.
- A `Makefile` only, with compose logic inlined in each target — standardizes
  the command surface, but mixes "what to run" with "how to resolve paths/
  files", making targets harder to read.
- **A `Makefile` wrapping dedicated scripts (chosen)** — `make dev` / `make
  prod` as the uniform, discoverable command surface; the scripts
  (`scripts/dc-dev.sh`, `scripts/dc-prod.sh`) own path resolution and which
  Compose files/env file to pass. Also a chance to practice shell scripting.

**Compose file organization:**
- A single `compose.yml` using Compose profiles to toggle dev/prod behavior —
  keeps everything in one file, but conditionals inside one file make the
  actual differences between environments harder to see at a glance.
- **Base + overlay files (chosen)**: `compose.yml` (shared service
  definitions) + `compose.dev.yml` / `compose.prod.yml` (only what differs) —
  more files, but each one is small and the diff between dev and prod is
  explicit.

## Decision
`make dev` / `make prod` is the single command surface. Each target calls
`scripts/dc-dev.sh` or `scripts/dc-prod.sh`, which resolve the repo root and
invoke `docker compose -f infra/docker/compose.yml -f
infra/docker/compose.{dev,prod}.yml --env-file infra/docker/.env.{dev,prod}
"$@"` — so `docker compose` is never called directly, and the right files are
always picked automatically.

`compose.yml` (base) holds what's shared: the `db` service, and the
`healthcheck` + `depends_on: condition: service_healthy` chain
(`db` → `backend` → `frontend`) that guarantees Postgres is ready before the
backend starts, and the backend is healthy before the frontend starts —
avoiding startup crashes from racing dependencies.

`compose.dev.yml` and `compose.prod.yml` hold only what's structurally
different: dev bind-mounts `apps/backend` and `apps/frontend` into the
containers and builds from `Dockerfile.dev`, running `mvn spring-boot:run` /
`npm run dev` directly for hot reload; prod builds from the multi-stage
`Dockerfile` (jar on `eclipse-temurin`, static build served by `nginx`) with
no bind mounts. `Dockerfile.dev` deliberately has no `COPY` step for
application source — the image only provides the runtime/toolchain, since
Compose bind-mounts the actual code in dev.

## Consequences
- No config duplication between dev and prod — the base file is the single
  source of truth for what both environments share, overlays make the
  differences explicit and auditable in a diff.
- Everyone (including future me) runs the exact same `make dev` / `make
  prod`, regardless of working directory, removing a class of "works on my
  machine" issues.
- Adds a few layers of indirection (`make` → script → `docker compose` →
  `--env-file`) that a new contributor has to learn before they can debug a
  startup issue directly with `docker compose`.
- If dev and prod ever need a third variant (e.g. a staging overlay), the
  pattern already supports it — add `compose.staging.yml` and a matching
  script/Makefile target.
