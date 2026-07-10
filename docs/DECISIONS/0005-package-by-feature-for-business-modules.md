# ADR-0005: Package-by-feature for business modules

## Status
Accepted

## Date
2026-07-10

## Context
The backend was a blank skeleton (only `BackendApplication.java`) before this first
business module (`Environment`). `CLAUDE.md` already mandates the shape of a business
module (entity + repository + service + controller + DTO + validation + Flyway
migration) but not how to lay out packages. `PROJECT_GUIDE.md` lists several more
modules to come (`Applications`, `Deployments`, `Deployment history`, ...), so
whatever layout is picked now needs to stay sane once there are 4-5 modules, not
just one.

## Options Under Consideration
- Package-by-layer (`controller/`, `service/`, `repository/`, `entity/`, `dto/` at
  the top level, each containing one class per module) — familiar from older Spring
  tutorials, but every package becomes a flat bag of unrelated classes as modules
  are added (`controller/EnvironmentController`, `controller/ApplicationController`,
  `controller/DeploymentController`, ...), with no structural signal of which classes
  belong together.
- Package-by-feature (`environment/`, `application/`, `deployment/`, each self-contained
  with its own entity/repository/service/controller/dto) — mirrors the "business
  module" concept already used in the docs; a module's code lives in one place.

## Decision
Package-by-feature. The `environment/` package now contains the entity, repository,
service, controller and exceptions for that module, with a `dto/` sub-package for its
request/response records and MapStruct mapper. Cross-cutting pieces that must be
reused by every future module (the base `NotFoundException`/`ConflictException` and
the global `@RestControllerAdvice`) live in a separate `common/` package.

## Consequences
- Adding `Application` or `Deployment` next means creating a sibling package, not
  touching existing ones — low risk of merge conflicts between modules developed
  independently.
- `common/` is intentionally small and only holds things every module needs; it must
  not become a dumping ground, or the benefit of package-by-feature erodes.
- Once modules start referencing each other (e.g. a `Deployment` pointing at an
  `Environment` and an `Application`), package boundaries will need a convention for
  cross-module references (e.g. only depend on the other module's public
  entity/repository, not its internals) — to revisit when that module is built.
