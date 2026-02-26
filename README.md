# DevOps Platform (Monorepo)

A portfolio-grade full-stack + DevOps playground built with:
- **Backend:** Spring Boot (Java 21, Maven)
- **Frontend:** React + TypeScript (Vite)
- **Database:** PostgreSQL (Docker)
- **Infra (later):** CI/CD, Docker images, monitoring, Kubernetes/Helm

The goal is to grow this repository step by step with production-like practices: clean architecture, automation, reproducible environments, and clear documentation.

---

## Repository structure

```
devops-platform/
    apps/
        backend/ # Spring Boot API
        frontend/ # React + TS app
    infra/
        docker/ # docker-compose and related config
        k8s/ # Kubernetes manifests (later)
        helm/ # Helm charts (later)
    docs/ # Architecture notes + ADRs
    scripts/ # Local developer scripts (run/lint/test)
```

---

## Prerequisites

- **Git**
- **Docker Engine**
- **Docker Compose v2**
- (Optional but recommended) **Make**

> The project is containerized and designed to run through Docker.  
> Make targets are provided as a convenience layer on top of Docker Compose.

---

## Getting started (local development)

### 1) Clone the repository

```bash
git clone <REPO_URL>
cd devops-platform
```

### 2) Environment configuration

Environment variables are managed through Docker Compose:

- `infra/docker/.env.dev` → development
- `infra/docker/.env.prod` → production

Make sure the appropriate `.env.*` file exists and is correctly configured before starting the stack.

### 3) Start the development stack

The development environment uses:

- `compose.yml`
- `compose.dev.yml`
- `.env.dev`

To build and start all services in detached mode:

```bash
make dev
```

Equivalent command (without Make):

```bash
./scripts/dc-dev.sh up --build -d
```

### 4) Useful development commands

Stop the stack:

```bash
make dev-down
```

View logs:

```bash
make dev-logs
```

List running containers:

```bash
make dev-ps
```

Rebuild containers (no cache):

```bash
make dev-build
```

Restart the stack:

```bash
make dev-restart
```

---

## Local URLs

- **Frontend:** `http://localhost:5173`

- **Backend:** `http://localhost:8080`

- **Database:** `localhost:5432` (PostgreSQL)

---

## Production mode (locally)

The production stack uses:

- `compose.yml`
- `compose.prod.yml`
- `.env.prod`

Start production mode locally:

```bash
make prod
```

Other available commands:

```bash
make prod-down
make prod-logs
make prod-ps
make prod-build
make prod-restart
```

---

## Notes

- All Docker Compose commands are wrapped through:
  - `scripts/dc-dev.sh` (development)
  - `scripts/dc-prod.sh` (production)
- These scripts automatically resolve the project root, select the correct Compose files, and load the appropriate environment file.
- No manual docker compose -f ... commands are required when using make.

---

## Current status

- ✅ Monorepo structure created

- ✅ Spring Boot project generated (runs locally)

- ✅ React + TS project generated (runs locally)

- ✅ PostgreSQL available via Docker Compose

- ⏳ Frontend ↔ Backend integration (next)

- ⏳ First business module (planned)

- ⏳ CI pipeline (planned)

- ⏳ Docker images + Kubernetes/Helm (planned)

---

## Roadmap (high level)

### 1. Bootstrap
- clean repo, README, scripts, basic infra
### 2. API contract + integration
- health/version endpoint
- frontend calls backend
### 3. First domain module                                  
- environment / service / deployment tracking (CRUD)
### 4. CI
- build + tests for backend & frontend
### 5. Containerization
- Dockerfiles + compose standardization
### 6. Observability
- metrics/logs stack in docker (later)
### 7. Kubernetes
- local cluster deployment + Helm charts

---

## Engineering principles
- Keep changes small and reviewable
- Prefer reproducible environments
- Document decisions (ADR) when trade-offs exist
- Avoid “magic”: understand each step before automating it

---

## License

TBD