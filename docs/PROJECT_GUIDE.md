# DevOps Platform -- Project Guide

## Engineering mindset first

The goal of this project is **not only to build software**, but to build
the habits of a software engineer.

Before implementing a feature, always ask yourself:

-   What problem am I solving?
-   What are the possible solutions?
-   Why did I choose this one?
-   What are its advantages?
-   What are its limitations?
-   Would I make the same decision in one year?

Try to document important technical decisions in `docs/DECISIONS/000X-short-title.md`.
Even a few paragraphs explaining your reasoning are more valuable than undocumented code.

------------------------------------------------------------------------

# Project objective

Build a portfolio-grade Full Stack + DevOps application while becoming
autonomous again.

Goals:

-   Improve software engineering skills
-   Improve DevOps knowledge
-   Learn modern tooling
-   Understand every technical decision
-   Avoid blind copy/paste
-   Build a project that reflects professional practices

For the current stack, repository layout, and container architecture, see
[ARCHITECTURE.md](ARCHITECTURE.md). For day-to-day commands and how to verify
the stack works, see [DEVELOPMENT.md](DEVELOPMENT.md).

------------------------------------------------------------------------

# Business application

This project simulates an internal platform for managing a company's
applications, their environments, and their deployment history. It is not
a deployment tool — no real deployment ever happens. Every deployment is
simulated (including simulated failures), so the project doubles as a
hands-on way to learn DevOps vocabulary, concepts, and tooling (CI, Docker,
observability, Kubernetes, etc.).

**Analogy:** think of a parcel-tracking system. It doesn't transport any
parcels — it just stores a tracking number, a status, a recipient, and a
history. This application works the same way: it doesn't run Kubernetes or
perform real deployments. It stores applications, environments,
deployments, and their history.

Concretely, for a company with several applications, the platform tracks
(non-exhaustive — this is the first-version scope; more fields/variables
are expected to be added later as the project grows more realistic):
- which applications exist, and which environments they run in
  (dev/staging/prod, ...)
- for each deployment: its status, the deployed version, who triggered it,
  and its outcome
- the full deployment history per application/environment
- whether things currently look "healthy" for a given
  application/environment

The observability work (roadmap step 6) monitors this platform itself —
the application that manages the simulated deployments — not any real
infrastructure being deployed to.

Possible modules:

-   Applications
-   Environments
-   Deployments
-   Deployment history
-   Dashboard
-   Monitoring integration
-   Alerts

The infrastructure (Docker, CI, Kubernetes, ...) supports and demonstrates
this application; it is not the application itself.

------------------------------------------------------------------------

# Long-term roadmap

This is the first detailed version of the roadmap — steps and sub-steps
below are expected to evolve as the project grows (new sub-steps, reordering,
or entirely new steps for modules/variables not yet anticipated).

1.  Technical foundation — done
2.  Environment module — done (`Environment` CRUD, see [ADRs 0005-0009](DECISIONS/))
3.  Database migrations — done (Flyway, adopted as part of step 2, see [ADR-0006](DECISIONS/0006-adopt-flyway-with-first-module.md))
4.  CI — done (GitHub Actions, see [ADR-0011](DECISIONS/0011-ci-with-github-actions.md))
5.  Container hardening — done (non-root prod backend, alpine JRE runtime, pinned base image versions, frontend healthcheck, see [ADR-0012](DECISIONS/0012-container-hardening.md))

6.  Observability foundation
    -   Actuator and Micrometer
    -   Prometheus
    -   Grafana
    -   Basic technical dashboard

7.  Product foundation
    -   `Application` module
    -   `Application` ↔ `Environment` relationship
    -   First usable React interface

8.  Deployment domain
    -   Deployment model
    -   Status and history
    -   Business validations
    -   Business metrics

9.  Kubernetes
    -   Local deployment
    -   ConfigMaps and Secrets
    -   Health probes
    -   Persistent storage

10. Helm
    -   Parameterized deployment
    -   Environment-specific values

11. Production-readiness
    -   Security
    -   Logs and traces
    -   Alerting
    -   Backup and recovery
    -   Performance and resilience

------------------------------------------------------------------------

# Success criteria before business development

The following questions should all have the answer "Yes":

-   Can someone clone and start the project easily?
-   Does Docker start correctly?
-   Does the frontend communicate with the backend?
-   Does the backend communicate with PostgreSQL?
-   Are environment variables managed properly?
-   Is the project documented?

If yes, the project is ready for business development.

------------------------------------------------------------------------

# Personal reminder

Always prioritize:

-   Understanding over speed
-   Simplicity over unnecessary complexity
-   Reproducibility over "works on my machine"
-   Documentation over memory
-   Small iterative improvements instead of large rewrites
