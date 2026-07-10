# Development

How to run, verify, and work on this repository day to day. For the
architecture behind these commands, see [ARCHITECTURE.md](ARCHITECTURE.md).

------------------------------------------------------------------------

## Commands

Everything runs through `make`, which wraps `scripts/dc-dev.sh` /
`scripts/dc-prod.sh`, which wrap `docker compose` with the right `-f` files
and `--env-file` (see [ADR-0002](DECISIONS/0002-docker-compose-env-split.md)).
Avoid calling `docker compose` directly.

**Dev stack** (hot-reload, source bind-mounted):
```bash
make dev            # build + start, detached
make dev-down / dev-logs / dev-ps / dev-build / dev-restart
```

**Prod-like stack locally** (multi-stage builds):
```bash
make prod
make prod-down / prod-logs / prod-ps / prod-build / prod-restart
```

Local URLs: frontend `:5173` (dev) / `:80` (prod), backend `:8080`,
Postgres `:5432`.

Env vars live in `infra/docker/.env.dev` / `.env.prod` — copy
`infra/docker/.env.example` to get started (see
[ADR-0003](DECISIONS/0003-env-files-strategy.md)). These files are
gitignored — never commit them.

**Backend** (from `apps/backend/`, outside Docker):
```bash
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=BackendApplicationTests   # single test class
./mvnw package
```
Needs `SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD` and a reachable Postgres
(defaults target `localhost:5432`).

**Frontend** (from `apps/frontend/`, outside Docker):
```bash
npm run dev / build / lint / preview
```

------------------------------------------------------------------------

## Remaining technical foundation

Before starting the first business module:

-   [x] Frontend ↔ backend communication (Vite dev-server proxy + nginx in prod, `/api` → backend)
-   [x] Health endpoint (Spring Boot Actuator, see [ADR-0004](DECISIONS/0004-actuator-vs-custom-health.md))
-   [x] Backend Docker healthcheck
-   [ ] Proper CORS configuration — currently side-stepped via the `/api`
    proxy (same-origin from the browser's point of view); revisit if a
    direct cross-origin call ever becomes necessary
-   [ ] Verify Docker startup end-to-end (see Verification below)
-   [x] Verify documentation (this reorganization)

When the unchecked items are done, the technical foundation is ready for the
first business module.

------------------------------------------------------------------------

## Verification after changes

Type checks and unit tests don't confirm the stack actually boots — verify
with:

1. `make dev` then `make dev-ps` — everything up and healthy.
2. Backend: `./mvnw test` (once tests exist for the change).
3. Frontend: `npm run lint` / `npm run build`.
4. Confirm `curl http://localhost:8080/actuator/health` returns `200` with
   `"status":"UP"`.
