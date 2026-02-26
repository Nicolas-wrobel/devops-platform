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
- **Java 21**
- **Node.js (LTS recommended)**
- **Docker Engine + Docker Compose v2**
- (Optional) **Make**

> This project targets **Linux (Ubuntu 24.04)** as the primary dev environment, but it should remain cross-platform where possible.

---

## Getting started (local development)

### 1) Clone
```bash
git clone <REPO_URL>
cd devops-platform
```

### 2) Start the database (PostgreSQL)

```bash
docker compose -f infra/docker/docker-compose.dev.yml up -d
```

### 3) Start backend (API)

```bash
cd apps/backend
./mvnw spring-boot:run
```

### 4) Start frontend (web app)

```bash
cd apps/frontend
npm install
npm run dev
```

---

## Local URLs

- **Frontend:** `http://localhost:5173`

- **Backend:** `http://localhost:8080`

- **Database:** `localhost:5432` (PostgreSQL)

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