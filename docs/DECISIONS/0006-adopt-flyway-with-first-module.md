# ADR-0006: Adopt Flyway with the first business module (not deferred)

## Status
Accepted

## Date
2026-07-10

## Context
`PROJECT_GUIDE.md`'s long-term roadmap lists "Database migrations" as step 3, after
"First business module" (step 2) — read literally, Flyway could have been deferred
to a later, dedicated iteration. But `CLAUDE.md`'s Conventions section already
declares the shape of a business module as "entity + repository + service +
controller + DTO + validation + Flyway migration", and explicitly notes that
adopting Flyway means moving `spring.jpa.hibernate.ddl-auto` from `update` to
`validate`. Building the first module was the moment to resolve this ambiguity
between the two docs.

## Options Under Consideration
- Defer Flyway to a later step, keep `ddl-auto=update` for the `Environment` module —
  matches the literal roadmap order in `PROJECT_GUIDE.md`, less to learn in one
  iteration, but contradicts the module shape already written down in `CLAUDE.md`
  and means redoing the module later to retrofit a migration.
- Adopt Flyway now, with this module — follows `CLAUDE.md`'s convention exactly, and
  since the `environment` table doesn't exist yet in any database, the very first
  migration script is a real `V1` (no baseline needed to reconcile against an
  existing Hibernate-managed schema).

## Decision
Adopt Flyway now. `flyway-core` + `flyway-database-postgresql` were added,
`ddl-auto` was switched to `validate`, and
`src/main/resources/db/migration/V1__create_environment_table.sql` is the first
versioned migration. Flyway runs automatically on application startup against the
same datasource already configured for the app.

## Consequences
- Schema changes for every future module (`Application`, `Deployment`, ...) must ship
  as a new `V{n}__description.sql` file — Hibernate can no longer alter the schema
  implicitly, which removes a class of "works on my machine, breaks in prod" drift.
- Local development requires re-running migrations (`db/migration`) whenever the
  schema changes; there is no more silent auto-update — this is deliberate, matching
  `CLAUDE.md`'s intent, but is a mental model shift from the bootstrap phase.
- `PROJECT_GUIDE.md`'s roadmap step 3 ("Database migrations") is effectively
  satisfied by this decision rather than being a separate future step — worth
  updating that document to avoid the same ambiguity for the next reader.
