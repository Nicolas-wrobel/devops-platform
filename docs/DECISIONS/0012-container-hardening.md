# ADR-0012 : Container Image Hardening (Non-root, Alpine, Version Pinning, Frontend Healthcheck)

## Status
Accepted

## Date
2026-07-13

## Context
Roadmap step 5, right after CI (ADR-0011). CI now catches functional
regressions on every push/PR, but the prod Dockerfiles and compose
wiring themselves hadn't been revisited since they were first written:
the backend ran as root, used a full (non-alpine) JRE base, every base
image sat on a floating tag (`postgres:16`, `node:20`,
`eclipse-temurin:21-jre`, `maven:3.9-eclipse-temurin-21`), and there
was a healthcheck gap — `db` and `backend` both have one in
`compose.yml`, `frontend` doesn't.

None of these are correctness bugs — the stack works today — but they
are well-established, low-effort container hygiene items that matter
before the stack is deployed anywhere beyond a local machine
(Kubernetes is roadmap step 7). This step tightens exactly those,
deliberately without pulling in heavier changes (build cache mounts,
distroless/jlink images, image labels, digest pinning, or structural
changes to the dev-only Dockerfiles) that would add complexity out of
proportion to a portfolio project at this stage.

## Options Under Consideration
- **Backend runtime base: `eclipse-temurin:21-jre` (current) vs.
  `-jre-alpine` vs. Debian `-slim` vs. distroless** — alpine keeps the
  same Temurin JRE with a much smaller, lower-surface OS layer
  (musl+BusyBox) than the default Debian-based image, with no native
  dependency conflicts for this pure-Java Spring Boot app. `-slim`
  would be a smaller step (still Debian, still apt-based) with less
  size benefit. Distroless would be the most minimal but drops shell
  access entirely, making the existing curl-based healthcheck
  impossible without extra tooling — too big a jump for now.
- **Non-root: full custom UID/GID vs. Alpine's `adduser -S` system
  account** — a system account via `adduser -S` is the minimal,
  idiomatic Alpine pattern (no password, no login shell) and is
  sufficient; a fixed numeric UID/GID (useful for k8s
  `securityContext.runAsUser` alignment) is deferred until the
  Kubernetes step actually needs it.
- **Version pinning: precise tags (chosen) vs. SHA256 digest pinning**
  — digests are maximally reproducible (byte-identical pulls forever)
  but unreadable/unmaintainable by hand and awkward to bump
  deliberately; precise tags (e.g. `postgres:16.14`) give the
  reproducibility win that matters here (no silent minor-version
  drift) while staying human-readable and easy to bump on purpose.
  Digest pinning deferred.
- **Frontend healthcheck: BusyBox `wget --spider` (chosen) vs.
  installing `curl` into `nginx:alpine`** — `wget` ships in the base
  image already (Alpine's BusyBox), so it needs zero extra packages;
  installing `curl` would mirror the backend's healthcheck style but
  adds an unnecessary package to an image whose whole point is being
  small and static.
- **Nginx running as non-root vs. keeping default (root master /
  `nginx`-user workers)** — the official `nginx:alpine` image already
  drops worker processes (the ones that actually handle traffic) to
  the unprivileged `nginx` user by default; only the master process
  (which just binds port 80 and forks workers) runs as root. This
  repo's `nginx.conf` only overrides `conf.d/default.conf` and never
  touches the base image's `user nginx;` directive, so that default is
  already in effect, unmodified. Full non-root nginx (remapping to an
  unprivileged port, running the master as non-root too) is a real
  hardening technique but adds real complexity for a security property
  already substantially covered by the default worker-process
  behavior — deferred.
- **Whether to touch `Dockerfile.dev` (backend/frontend)** — dev
  images bind-mount host source; running backend dev as non-root would
  create host-side file-ownership friction for `target/` output with
  no real security benefit (these containers are local-only,
  ephemeral, never exposed). Structural changes (`USER`) to dev images
  are out of scope; base-image version pins were still applied for
  reproducibility, since that carries no such downside.

## Decision
- Backend prod runtime: `eclipse-temurin:21-jre` →
  `eclipse-temurin:21.0.11_10-jre-alpine`, with a dedicated non-root
  `appuser`/`appgroup` system account, `COPY --chown` for the jar, and
  `USER appuser` before `ENTRYPOINT`.
- All four identified base images pinned to precise patch-version tags
  (verified against Docker Hub on 2026-07-13): `maven:3.9-eclipse-
  temurin-21` → `maven:3.9.16-eclipse-temurin-21`, `eclipse-
  temurin:21-jre` → `eclipse-temurin:21.0.11_10-jre-alpine`,
  `node:20` → `node:20.20.2`, `postgres:16` → `postgres:16.14` —
  applied across `Dockerfile`, `Dockerfile.dev`, and `compose.yml`.
- `frontend` service gets a `healthcheck` in `infra/docker/compose.prod.yml`
  (not the shared base `compose.yml`) using BusyBox `wget --spider`,
  closing the inconsistency with `db`/`backend`. It's prod-only
  because the dev and prod frontend containers serve on different
  internal ports (Vite dev server on 5173 vs. nginx on 80 in prod) —
  a shared healthcheck targeting port 80 would incorrectly mark the
  dev container unhealthy (confirmed by testing: it does).
  `frontend`'s existing `depends_on: backend: condition:
  service_healthy` in the base `compose.yml` is unchanged.
- `Dockerfile.dev` (backend and frontend): version tags pinned, no
  other change — no non-root user, no structural change.
- Nginx worker non-root behavior: confirmed as already the default, no
  change made or needed.

## Consequences
- Backend prod image is smaller (alpine base) and runs as an
  unprivileged user — meaningfully reduces blast radius if the
  application is ever compromised, with no functional trade-off (port
  8080 is already unprivileged, no capability grants needed).
- All three services now have consistent healthchecks
  (`db`/`backend`/`frontend`), so `docker compose ps` / `make prod-ps`
  gives an accurate readiness picture for the whole stack.
- Version bumps for base images are now a deliberate, visible action
  (editing a pinned tag) instead of happening silently on next pull —
  slightly more manual maintenance, in exchange for reproducibility.
- Explicitly **not** done in this pass (deferred, not forgotten):
  BuildKit cache mounts, OCI image labels
  (`org.opencontainers.image.*`), a jlink custom minimal JRE /
  distroless backend runtime, full nginx non-root/unprivileged-port
  hardening, SHA256 digest pinning (any image, including `nginx:alpine`
  itself, deliberately left unpinned), and any structural change to
  the dev Dockerfiles beyond tag pinning. Registry push, image
  signing, and SBOM generation are CD-territory, later roadmap steps.
- Exact pinned patch versions were verified against Docker Hub at
  write time and should be treated as a starting point, not a
  permanent fact — worth a periodic manual bump rather than assuming
  they stay current indefinitely.
