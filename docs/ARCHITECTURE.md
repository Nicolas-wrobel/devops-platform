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
| Later          | GitHub Actions, Flyway, Monitoring, Kubernetes, Helm       |

Backend dependencies currently wired: `spring-boot-starter-webmvc`,
`spring-boot-starter-data-jpa`, `spring-boot-starter-validation`,
`spring-boot-starter-actuator` (see
[ADR-0004](DECISIONS/0004-actuator-vs-custom-health.md)), the PostgreSQL
driver, and `spring-boot-devtools` for local development. No controllers or
entities yet — see the roadmap in PROJECT_GUIDE.md.

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

- **`compose.dev.yml`** — hot-reload setup:
  - `backend` builds from `Dockerfile.dev` (no `COPY`; source is bind-mounted
    from `apps/backend`), runs `mvn -DskipTests spring-boot:run`.
  - `frontend` builds from `Dockerfile.dev`, bind-mounts `apps/frontend`,
    runs `npm run dev -- --host 0.0.0.0 --port 5173`.
  - Ports exposed on the host via `${SPRING_BOOT_PORT}` / `${REACT_PORT}` /
    `${POSTGRES_PORT}`.

- **`compose.prod.yml`** — immutable artifact setup:
  - `backend` builds from the multi-stage `Dockerfile` (Maven build stage →
    `eclipse-temurin:21-jre` runtime stage, packaged jar).
  - `frontend` builds from the multi-stage `Dockerfile` (Node build stage →
    static files served by `nginx:alpine`).
  - No bind mounts — containers run what was actually built into the image.

Local URLs: frontend `:5173` (dev) / `:80` (prod), backend `:8080`,
Postgres `:5432`.

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

See [DEVELOPMENT.md](DEVELOPMENT.md) for what's still open before the first
business module.
