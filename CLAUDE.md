# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

**What:** Portfolio-project monorepo, grown step by step. Backend has its first business module (`Environment` CRUD, package-by-feature under `environment/`, cross-cutting concerns under `common/`) with Flyway-managed schema migrations. Frontend is still a generated skeleton beyond the backend health check display. `infra/k8s/`, `infra/helm/` are empty placeholders. `docs/` holds [PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md) (goals/roadmap), [ARCHITECTURE.md](docs/ARCHITECTURE.md) (current stack/layout), [DEVELOPMENT.md](docs/DEVELOPMENT.md) (commands/verification), and `DECISIONS/` (ADRs, currently 0001-0011). CI is in place via GitHub Actions (`.github/workflows/ci.yml`, see ADR-0011) — backend tests + frontend lint/build on every push/PR to `main`; branch protection not enabled yet.

**Why:** Repo is grown step by step with production-like practices. Roadmap: API contract + frontend/backend integration (done) → first domain module (done — `Environment` CRUD, ADRs 0005-0010) → CI (done — GitHub Actions, ADR-0011) → containerization → observability → Kubernetes.

**How:** `Environment` (see [`apps/backend/src/main/java/com/devops_platform/backend/environment/`](apps/backend/src/main/java/com/devops_platform/backend/environment/)) is the reference implementation of the module shape below — follow it, don't reinvent it, when adding the next domain module (`Application`, `Deployment`, ...).

## Repository structure

```
devops-platform/
  apps/
    backend/   # Spring Boot API (Java 21, Maven)
    frontend/  # React 19 + TypeScript app (Vite)
  infra/
    docker/    # compose.yml (base) + compose.dev.yml / compose.prod.yml overlays + .env.{dev,prod,example}
    k8s/       # base/ + overlays/{dev,prod} — empty, planned
    helm/      # empty, planned
  docs/        # PROJECT_GUIDE.md, ARCHITECTURE.md, DEVELOPMENT.md, DECISIONS/ (ADRs)
  scripts/     # dc-dev.sh / dc-prod.sh — docker compose wrappers
```

## Commands

**What/Why:** Everything runs through `make`, which wraps `scripts/dc-dev.sh` / `scripts/dc-prod.sh`, which wrap `docker compose` with the right `-f` files and `--env-file`. Avoid calling `docker compose` directly — the scripts resolve the project root and pick the correct files automatically.

**Dev stack** (hot-reload: `mvn spring-boot:run` / `vite` dev server, source bind-mounted):
```bash
make dev            # build + start, detached (compose.yml + compose.dev.yml + .env.dev)
make dev-down / dev-logs / dev-ps / dev-build / dev-restart
```

**Prod-like stack locally** (multi-stage builds: jar on eclipse-temurin, static build on nginx):
```bash
make prod
make prod-down / prod-logs / prod-ps / prod-build / prod-restart
```

Local URLs: frontend `:5173` (dev) / `:80` (prod), backend `:8080`, Postgres `:5432`.

Env vars live in `infra/docker/.env.dev` / `.env.prod` (see `.env.example` for keys: `POSTGRES_USER/PASSWORD/DB/PORT`, `SPRING_BOOT_PORT`, `REACT_PORT`). Gitignored except `.env.example` — see [Conventions](#conventions).

**Backend** (from `apps/backend/`, outside Docker):
```bash
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=BackendApplicationTests   # single test class
./mvnw package
```
`./mvnw spring-boot:run` needs `SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD` and a reachable Postgres (defaults target `localhost:5432`). `./mvnw test` does not — every database-touching test spins up its own Testcontainers `postgres:16` (see ADR-0009/0010). `spring.jpa.hibernate.ddl-auto=validate` — Flyway (`src/main/resources/db/migration/`) is the schema authority, see ADR-0006.

**Frontend** (from `apps/frontend/`, outside Docker):
```bash
npm run dev / build / lint / preview
```

## Architecture notes

- **Monorepo layout**: `apps/*` are independent, self-contained projects (own Dockerfile, own dependency manifest); `infra/docker` is the single source of truth for how services are wired (ports, env, healthchecks, depends_on). Base `compose.yml` holds shared config; `compose.dev.yml`/`compose.prod.yml` overlay build context/Dockerfile, ports, and dev-only bind mounts.
- **Backend**: standard Spring Boot layout, package `com.devops_platform.backend`. `webmvc`, `data-jpa`, `validation`, `actuator`, Flyway, MapStruct, Testcontainers (test scope), Postgres driver wired. Business modules live in their own package-by-feature directory (e.g. `environment/`, see ADR-0005); cross-cutting exception base classes and the global `ProblemDetail` handler live in `common/` (ADR-0008), reused by every module.
- **Dev vs prod containers differ structurally, not just by flag**: dev bind-mounts source and runs the build tool directly for hot reload; prod does a multi-stage build producing an immutable jar / static nginx bundle. A build/start change may need updating in both `Dockerfile` and `Dockerfile.dev`.

## Conventions

- **Commits**: descriptive, imperative mood, explain *why* not just what.
- **Secrets**: never commit `.env.dev`, `.env.prod`, or any secret — only `.env.example` (dummy values, kept in sync with required keys).
- **Domain modules**: a business module = entity + repository + service + controller + DTO + validation + Flyway migration, package-by-feature (ADR-0005). `Environment` is the first one built this way (ADRs 0005-0010) — keep later modules (`Application`, `Deployment`, ...) consistent with it: reuse `common/NotFoundException`/`ConflictException` and `GlobalExceptionHandler` rather than reinventing error handling per module.
- **Language**: all code, comments, commit messages, and documentation in this repo are written in English.

## Documentation of Decisions 
Any fundamental technical decision (choice of library, architectural pattern, significant trade-off) must be documented in docs/DECISIONS/ following the format specified in docs/DECISIONS/0000-template.md. Numbered format: 000X-short-title.md

## Git workflow

Trunk-based (GitHub Flow), even solo — the discipline matters for CI
and for good habits, not just team size.

- `main` is always stable and deployable. Never commit directly to it.
- Before any new feature/fix/chore: branch from an up-to-date `main`.
  Naming mirrors existing commit prefixes: `feature/<slug>`, `fix/<slug>`,
  `chore/<slug>`, `docs/<slug>`.
- Work on the branch with the usual atomic, descriptive commits (see
  Commits above).
- Integrate back via a pull request into `main`, even solo — not a direct
  local merge. Squash-merge by default to keep `main` linear; use a
  regular merge only when the branch's individual commits are worth
  preserving.
- Delete the branch after merge.
- `git push` still always requires explicit confirmation first (see
  Security and permissions).
- CI exists now (GitHub Actions, ADR-0011) — the immediate next step is
  adding GitHub branch protection on `main` (PR required + CI checks
  required before merge). Not enabled yet, do this once the `ci.yml`
  workflow has run green at least once on a real PR.

## Security and permissions

- Never read or display the contents of `.env.dev`, `.env.prod`, or any file containing secrets.
- `git commit`: freely, after each completed logical step.
- `git push`: always ask for explicit confirmation first, even in auto-accept mode.
- Never run commands that touch paths outside the project directory.

## Verification after changes

Type checks and unit tests don't confirm the stack actually boots — verify with:
1. `make dev-up` then `make dev-ps` — everything up and healthy.
2. Backend: `./mvnw test`.
3. Frontend: `npm run lint` / `npm run build`.
4. Confirm `/actuator/health` returns `200`.
