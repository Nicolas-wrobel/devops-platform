# ADR-0011 : Continuous Integration with GitHub Actions

## Status
Accepted

## Date
2026-07-13

## Context
The repository had no automated verification on push or pull request —
`./mvnw test`, `npm run lint`, and `npm run build` were only ever run
manually (per `docs/DEVELOPMENT.md`). This is roadmap step 4
(`docs/PROJECT_GUIDE.md`).

Two prior ADRs already anticipated this step:
- ADR-0009 (Testcontainers for persistence tests) explicitly flagged
  that "the upcoming CI step will need Docker-in-Docker (or an
  equivalent) available in the pipeline" for the backend test suite,
  which spins up a real `postgres:16` container via Testcontainers.
- ADR-0010 (shared Testcontainers base class) was done specifically
  "before CI is set up," removing `BackendApplicationTests`'s hidden
  dependency on a locally-running Postgres so `./mvnw test` runs
  cleanly on a fresh runner.

The documented git workflow (`CLAUDE.md`) is trunk-based: `main` is
always stable, work happens on short-lived branches integrated via PR
(squash-merge by default). `CLAUDE.md` explicitly defers GitHub branch
protection ("require PR + require CI checks") until CI exists — so
branch protection is a natural, but separate, immediate follow-up to
this decision, not part of it.

## Options Under Consideration
- **GitHub Actions vs. GitLab CI / CircleCI / Jenkins** — GitHub
  Actions is native to where the repo is already hosted, needs no new
  account or service integration, and has unlimited free minutes on a
  public repository. The alternatives would add an external service as
  extra learning surface for a first CI setup, with no corresponding
  benefit here.
- **Single `ci.yml` with two always-run jobs vs. two path-filtered
  workflow files** (`backend-ci.yml` / `frontend-ci.yml`, each scoped
  to `apps/<app>/**`) — the per-app split is the more
  "monorepo-optimized" pattern, but it interacts badly with GitHub's
  *required status checks* (used once branch protection is enabled): a
  required check tied to a workflow that a given PR's changed paths
  never trigger leaves that PR permanently stuck "waiting for status."
  A single workflow avoids that trap and keeps one CI status per PR to
  reason about, at the cost of both jobs always running even when only
  one app changed — negligible for a solo portfolio repo (free minutes,
  jobs run in parallel).
- **Built-in `cache: maven` / `cache: npm` (via `setup-java` /
  `setup-node`) vs. an explicit `actions/cache` step** — the built-in
  option is one fewer concept to learn and is the officially
  recommended way to cache dependencies for these actions.
- **Default `ubuntu-latest` runner vs. explicit Docker-in-Docker
  setup** — GitHub-hosted Linux runners already have a working,
  non-nested Docker daemon preinstalled, which is exactly what
  Testcontainers needs. This resolves the risk flagged in ADR-0009
  without any extra configuration.

## Decision
Add `.github/workflows/ci.yml` with two parallel jobs, triggered on
push to `main` and on pull requests targeting `main`, with a
`concurrency` group to cancel superseded runs:
- `backend`: `actions/checkout@v5`, `actions/setup-java@v5` (Temurin
  21, `cache: maven`), then `./mvnw test` from `apps/backend`.
- `frontend`: `actions/checkout@v5`, `actions/setup-node@v5` (Node 20,
  `cache: npm`), then `npm ci`, `npm run lint`, `npm run build` from
  `apps/frontend`.

No test-reporting action, no matrix builds, no Docker image build/push
— kept to the minimum needed to verify the codebase on every change.

## Consequences
- `main` now has a repeatable, automated check on every push/PR —
  enables the next step (GitHub branch-protection rules requiring
  these checks before merge), which is a separate GitHub repo-settings
  action, not part of this change.
- Both jobs run on every push/PR regardless of which app changed — a
  deliberate simplicity-over-optimization trade-off; revisit with
  `paths:` filters if CI minutes or run time ever become a real
  constraint.
- Frontend has no automated test step yet (lint + build/typecheck
  only) since no test framework is configured — a `frontend` test step
  will need to be added once one is introduced.
- Docker image build validation, deployment, and matrix builds across
  OS/Node/Java versions are explicitly out of scope for this pass —
  single target stack, no registry push yet, and validating the prod
  Dockerfiles is a distinct, larger step naturally paired with future
  CD work.
