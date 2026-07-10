# ADR-0001 : Monorepo structure

## Status
Accepted

## Date
2026-07-10

## Context
Before writing any application code, the repository topology for the
backend, frontend, and infrastructure had to be picked. This is a solo
portfolio project: one contributor, one machine, one release cadence, built
step by step to practice production-like engineering habits.

## Options Under Consideration
- **Monorepo** (`apps/backend`, `apps/frontend`, `infra/`, `docs/`, `scripts/`
  all in one repository) — one place to clone, browse, and version
  everything; no cross-repo coordination needed.
- **Polyrepo** (one repository per app: `backend`, `frontend`, plus a
  separate infra repo) — mirrors how independent teams typically split
  ownership, but adds repo-juggling overhead with no team to justify it.
- **Polyrepo + meta-repo** (submodules or a tooling layer tying separate
  repos together) — closer to a multi-service org setup, but its
  coordination overhead has no payoff for a single contributor.

This wasn't a deliberate side-by-side comparison at the time — the project
started as a monorepo from the first commit.

## Decision
Monorepo. For a solo, portfolio-scale project, having backend, frontend, and
infra in a single repository is simpler: one clone, one place to see and
version everything, and no repo-juggling for a single contributor.

## Consequences
- Coordinated changes across backend/frontend/infra can land in one commit
  or PR, which fits the trunk-based workflow already in place.
- Onboarding (even just "future me") only requires one clone and one
  `CLAUDE.md`/`docs/` set, instead of keeping multiple repos in sync.
- If the project ever grows into independently released services with
  separate ownership, this will need revisiting — a future CI setup will
  need path-based triggers so a frontend-only change doesn't rebuild the
  backend, and vice versa.
