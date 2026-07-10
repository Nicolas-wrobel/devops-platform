# ADR-0004 : Spring Boot Actuator vs. a custom health endpoint

## Status
Accepted

## Date
2026-07-10

## Context
The backend needed a health signal to drive the Docker healthcheck and the
`depends_on: condition: service_healthy` chain described in
[ADR-0002](0002-docker-compose-env-split.md) — the frontend shouldn't come up
until the backend is actually ready.

## Options Under Consideration
- **Hand-rolled `/health` controller** — a minimal endpoint returning
  `200 OK`, full control over its shape, no extra dependency. Covers the
  Docker healthcheck need and nothing more.
- **Spring Boot Actuator (chosen)** — a standard Spring starter exposing
  `/actuator/health` (and other management endpoints) out of the box.
- **External sidecar/process check** — checking the JVM process or port from
  outside the app. Doesn't verify the application layer is actually
  responding (e.g. DB connectivity), only that a process/port exists.

## Decision
Spring Boot Actuator, because the need goes beyond a basic ping: `/actuator/info`
is wanted now, and a metrics endpoint (e.g. `/actuator/prometheus`) is on the
roadmap once monitoring is tackled. Actuator provides that without hand-building
infrastructure Spring already ships. Configured in
`apps/backend/src/main/resources/application.properties`:
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```
The Compose healthcheck (`infra/docker/compose.yml`) calls
`curl -f http://localhost:8080/actuator/health || exit 1`, which is why both
`apps/backend/Dockerfile` and `Dockerfile.dev` install `curl`.

## Consequences
- Health and info endpoints came for free, with detailed health information
  (`show-details=always`) useful for local debugging.
- Adding `/actuator/prometheus` later (see roadmap: Monitoring) will be a
  dependency + config change, not a rewrite of the health-check mechanism.
- Exposes more surface than a minimal hand-rolled endpoint would — `include=health,info`
  is deliberately narrow for now, but exposure and `show-details` need
  revisiting before anything prod-facing (showing full health details
  publicly is a diagnostic risk).
- Ties both Dockerfiles to installing `curl` solely to satisfy the
  container-level healthcheck against this endpoint.
