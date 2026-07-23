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
`./mvnw spring-boot:run` needs `SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD` and a
reachable Postgres (defaults target `localhost:5432`). `./mvnw test` does **not**
need any of that — every test that touches a database spins up its own disposable
`postgres:16` container via Testcontainers (see
[ADR-0009](DECISIONS/0009-testcontainers-for-persistence-tests.md) and
[ADR-0010](DECISIONS/0010-shared-testcontainers-base-for-integration-tests.md)),
so `./mvnw test` only needs a local Docker daemon, not a running dev stack.

Schema changes go through Flyway, not `ddl-auto` (see
[ADR-0006](DECISIONS/0006-adopt-flyway-with-first-module.md)): add a new
`V{n}__description.sql` file under
`apps/backend/src/main/resources/db/migration/`, never edit an already-applied
one. Migrations run automatically on backend startup, in Docker or via
`./mvnw spring-boot:run`.

**Frontend** (from `apps/frontend/`, outside Docker):
```bash
npm run dev / build / lint / preview
```

------------------------------------------------------------------------

## Known follow-ups

-   [ ] Proper CORS configuration — currently side-stepped via the `/api`
    proxy (same-origin from the browser's point of view); revisit if a
    direct cross-origin call ever becomes necessary
-   [x] Verify Docker startup end-to-end — done as part of the `Environment`
    module work (`make dev` + `make dev-ps` all healthy, full CRUD exercised
    with `curl` against the running stack, Flyway migration applied
    automatically)

Next up on the roadmap: Grafana + a basic technical dashboard, to close out
the observability foundation (see [PROJECT_GUIDE.md](PROJECT_GUIDE.md)).

------------------------------------------------------------------------

## Verification after changes

Type checks and unit tests don't confirm the stack actually boots — verify
with:

1. `make dev` then `make dev-ps` — everything up and healthy (`db`,
   `backend`, `frontend`, `prometheus`).
2. Backend: `./mvnw test` (once tests exist for the change).
3. Frontend: `npm run lint` / `npm run build`.
4. Confirm `curl http://localhost:8080/actuator/health` returns `200` with
   `"status":"UP"`.
5. Confirm Prometheus is scraping the backend: open the UI on
   `${PROMETHEUS_PORT}` → Status → Targets, and check `backend` is `UP`
   (see [ADR-0013](DECISIONS/0013-prometheus-metrics-scraping.md)).

Steps 2 and 3 also run automatically on every push/PR to `main` via
[`.github/workflows/ci.yml`](../.github/workflows/ci.yml) (see
[ADR-0011](DECISIONS/0011-ci-with-github-actions.md)) — steps 1, 4, and 5
stay manual for now.
