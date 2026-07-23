# Architecture

Current, factual state of the stack — what exists today, not the roadmap
(see [PROJECT_GUIDE.md](PROJECT_GUIDE.md) for goals/roadmap, and
[DEVELOPMENT.md](DEVELOPMENT.md) for how to run it).

------------------------------------------------------------------------

## Current stack

| Layer          | Technology                                              |
|----------------|----------------------------------------------------------|
| Backend        | Spring Boot (parent `spring-boot-starter-parent:4.0.2`), Java 21, Maven |
| Frontend       | React 19, TypeScript, Vite                                |
| Database       | PostgreSQL 16                                             |
| Infrastructure | Docker, Docker Compose, Git, Makefile, Bash scripts        |
| Migrations     | Flyway (see [ADR-0006](DECISIONS/0006-adopt-flyway-with-first-module.md)) |
| Observability  | Micrometer + Prometheus (see [ADR-0013](DECISIONS/0013-prometheus-metrics-scraping.md)); Grafana next |
| Later          | Kubernetes, Helm                                          |

Backend dependencies currently wired: `spring-boot-starter-webmvc`,
`spring-boot-starter-data-jpa`, `spring-boot-starter-validation`,
`spring-boot-starter-actuator` (see
[ADR-0004](DECISIONS/0004-actuator-vs-custom-health.md)) with
`micrometer-registry-prometheus` for the `/actuator/prometheus` metrics
endpoint (see [ADR-0013](DECISIONS/0013-prometheus-metrics-scraping.md)),
`spring-boot-starter-flyway`
+ `flyway-database-postgresql` (see
[ADR-0006](DECISIONS/0006-adopt-flyway-with-first-module.md)), MapStruct (see
[ADR-0007](DECISIONS/0007-mapstruct-for-entity-dto-mapping.md)), the PostgreSQL
driver, and `spring-boot-devtools` for local development.

The first business module, `Environment` (full CRUD under `/api/environments`),
is built and merged — package-by-feature under
`com.devops_platform.backend.environment`, with cross-cutting exception handling
in `com.devops_platform.backend.common`. See
[ADR-0005](DECISIONS/0005-package-by-feature-for-business-modules.md) for the
package layout convention and
[ADR-0008](DECISIONS/0008-problemdetail-as-api-error-convention.md) for the API
error convention — both are the pattern to follow for the next modules
(`Application`, `Deployment`, ...).

------------------------------------------------------------------------

## Repository structure

```
devops-platform/
├── apps/
│   ├── backend/    # Spring Boot API — Dockerfile (prod) + Dockerfile.dev
│   └── frontend/   # React + Vite app — Dockerfile (prod) + Dockerfile.dev
├── infra/
│   ├── docker/     # compose.yml (base) + compose.dev.yml / compose.prod.yml + .env.{dev,prod,example}
│   ├── k8s/        # empty, planned
│   └── helm/       # empty, planned
├── docs/
│   ├── PROJECT_GUIDE.md
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT.md
│   └── DECISIONS/
├── scripts/        # dc-dev.sh / dc-prod.sh — docker compose wrappers
├── Makefile
└── README.md
```

`apps/*` are independent, self-contained projects — each owns its own
Dockerfile and dependency manifest. `infra/docker/` is the single source of
truth for how services are wired together (ports, env, healthchecks,
`depends_on`).

------------------------------------------------------------------------

## Container architecture

Three Compose files, layered (rationale in
[ADR-0002](DECISIONS/0002-docker-compose-env-split.md)):

- **`compose.yml`** (base, shared by dev and prod)
  - `db` (Postgres 16) with a `pg_isready` healthcheck.
  - `backend` — reads DB connection info from env vars, has a healthcheck
    hitting `/actuator/health` (curl), and `depends_on: db: condition:
    service_healthy`.
  - `frontend` — `depends_on: backend: condition: service_healthy`.
  - This chain guarantees Postgres is ready before the backend starts, and
    the backend is healthy before the frontend starts.
  - `prometheus` (see [ADR-0013](DECISIONS/0013-prometheus-metrics-scraping.md))
    — image pinned to `prom/prometheus:v3.13.1`, config bind-mounted from
    `infra/docker/prometheus.yml`, data persisted in a named
    `prometheus_data` volume, healthcheck via `wget --spider` against
    `/-/healthy`. Scrapes `backend:8080/actuator/prometheus` by Compose
    service-name DNS — no explicit `networks:` needed, same mechanism `db`
    and `backend` already rely on. No `depends_on` on `backend`: a
    momentarily-down scrape target just retries on the next interval.

- **`compose.dev.yml`** — hot-reload setup:
  - `backend` builds from `Dockerfile.dev` (no `COPY`; source is bind-mounted
    from `apps/backend`), runs `mvn -DskipTests spring-boot:run`.
  - `frontend` builds from `Dockerfile.dev`, bind-mounts `apps/frontend`,
    runs `npm run dev -- --host 0.0.0.0 --port 5173`.
  - Ports exposed on the host via `${SPRING_BOOT_PORT}` / `${REACT_PORT}` /
    `${POSTGRES_PORT}` / `${PROMETHEUS_PORT}`.

- **`compose.prod.yml`** — immutable artifact setup:
  - `backend` builds from the multi-stage `Dockerfile` (Maven build stage →
    `eclipse-temurin:21-jre` runtime stage, packaged jar).
  - `frontend` builds from the multi-stage `Dockerfile` (Node build stage →
    static files served by `nginx:alpine`).
  - No bind mounts — containers run what was actually built into the image.
  - `prometheus` exposes the same `${PROMETHEUS_PORT}` as dev — no dev/prod
    behavioral split for this service.

Local URLs: frontend `:5173` (dev) / `:80` (prod), backend `:8080`,
Postgres `:5432`, Prometheus UI `:9090`.

------------------------------------------------------------------------

## Current progress

Completed:

-   Monorepo created
-   Backend initialized
-   Frontend initialized
-   PostgreSQL with Docker
-   Compose base/dev/prod
-   Dockerfiles
-   Environment files strategy
-   Makefile
-   Startup scripts
-   README
-   Spring Boot Actuator health/info endpoints
-   Docker healthcheck for the backend
-   Frontend ↔ backend proxy and health display
-   First clean Git commits, trunk-based workflow defined
-   First business module: `Environment` CRUD, with Flyway migrations,
    MapStruct mapping, RFC 7807 error responses, and a three-tier test suite
    (unit, Testcontainers persistence, Testcontainers API) — see
    [ADRs 0005-0010](DECISIONS/)
-   CI via GitHub Actions — backend tests + frontend lint/build on every
    push/PR to `main` — see [ADR-0011](DECISIONS/0011-ci-with-github-actions.md)
-   Container hardening — non-root prod backend, alpine JRE runtime, pinned
    base-image versions, consistent healthchecks — see
    [ADR-0012](DECISIONS/0012-container-hardening.md)
-   Metrics endpoint (`/actuator/prometheus` via Micrometer) and a
    Prometheus service scraping it, both wired into the Compose stack — see
    [ADR-0013](DECISIONS/0013-prometheus-metrics-scraping.md)

See [DEVELOPMENT.md](DEVELOPMENT.md) for day-to-day commands. Next up:
Grafana + a basic technical dashboard, to close out the observability
foundation (see the roadmap in [PROJECT_GUIDE.md](PROJECT_GUIDE.md)).
