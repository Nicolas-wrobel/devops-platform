# DevOps Platform (Monorepo)

[![CI](https://github.com/Nicolas-wrobel/devops-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Nicolas-wrobel/devops-platform/actions/workflows/ci.yml)

A portfolio-grade full-stack + DevOps playground built with:
- **Backend:** Spring Boot (Java 21, Maven)
- **Frontend:** React + TypeScript (Vite)
- **Database:** PostgreSQL (Docker)
- **Infra (later):** CI/CD, Docker images, monitoring, Kubernetes/Helm

The goal is to grow this repository step by step with production-like practices: clean architecture, automation, reproducible environments, and clear documentation.

---

## Documentation

| Doc | What's in it |
|---|---|
| [docs/PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md) | Goals, roadmap, engineering mindset |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Stack, repo layout, container architecture, current progress |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Full command reference, verification steps |
| [docs/DECISIONS/](docs/DECISIONS/) | ADRs — what was decided, and why |

---

## Quickstart

**Prerequisites:** Git, Docker Engine, Docker Compose v2, (optional) Make.

```bash
git clone <REPO_URL>
cd devops-platform
cp infra/docker/.env.example infra/docker/.env.dev   # fill in real values
make dev
```

**Local URLs:** frontend `http://localhost:5173` · backend `http://localhost:8080` · database `localhost:5432`

For every other command (stop, logs, rebuild, prod mode, backend/frontend
standalone, verification), see [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

---

## License

TBD
