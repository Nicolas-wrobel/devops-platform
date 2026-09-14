# ADR-0019 : CI Security Scanning (Dependencies, Secrets, Containers)

## Status
Accepted

## Date
2026-09-14

## Context
CI (ADR-0011) only ran tests/lint/build — nothing checked for vulnerable
dependencies, leaked secrets, or vulnerable base images.

## Options Under Consideration
- **Minimal** (Dependabot alerts only, a repo setting, zero CI cost) vs.
  **full bundle** (also gitleaks, Trivy, OWASP dependency-check) — the
  minimal option catches known-vulnerable dependencies for free but
  misses secrets accidentally committed and vulnerabilities baked into
  the built container images.
- **OWASP dependency-check-maven**, tried first: scans Java dependencies
  against the NVD feed directly. Without an `NVD_API_KEY` (a manual,
  email-gated signup this project doesn't have yet), the public NVD API
  rate-limits hard enough that a first sync ran 25+ minutes before being
  cancelled by the next push. Options at that point: get the key, keep
  the tool with `continue-on-error`, or drop it since Dependabot alerts
  (already enabled) cover largely the same "vulnerable dependency"
  signal without ever touching NVD directly or costing CI time.

## Decision
`npm audit --omit=dev` (frontend, scoped to what actually ships —
devDependencies like Vite/Rollup generate a lot of build-tooling-only
noise), Dependabot alerts (enabled at the repo level), `gitleaks`
(secret scanning across git history), and `Trivy` (scans the actual
built prod images, not just source). **No OWASP dependency-check-maven**
— dropped after the NVD rate-limiting problem above, in favor of relying
on Dependabot for the dependency-vulnerability signal instead of running
a second, slower tool covering mostly the same ground.

Two supply-chain findings surfaced *while wiring this up*, fixed before
landing: `aquasecurity/trivy-action` had 76 of 77 version tags hijacked
in a March 2026 incident (credential-stealing `entrypoint.sh`,
GHSA-69fq-xp46-6x23) — pinned to the commit SHA behind the one
unaffected tag (`0.35.0`) rather than a mutable tag. Separately,
`gitleaks-action@v2` and `actions/cache@v4` are Node 20-based, and
GitHub was days from dropping Node 20 runner support — bumped to
`gitleaks-action@v3` (also SHA-pinned) and `actions/cache@v5`.

Trivy's very first real run also caught genuine CRITICAL/HIGH CVEs
already present in the backend image (Spring Boot, Spring Security,
Spring Framework, Jackson) — fixed by bumping the Spring Boot parent to
4.0.8, which pulled in patched versions of all of them transitively via
Boot's own dependency management.

## Consequences
- Dependency-vulnerability coverage for the backend now comes from
  Dependabot alone, not a dedicated CI step — no local HTML report, and
  it's a GitHub-side signal (visible in the Security tab) rather than a
  PR check. Revisit if that turns out to be insufficient, or once an
  `NVD_API_KEY` makes dependency-check-maven fast enough to be worth
  re-adding (https://nvd.nist.gov/developers/request-an-api-key);
  `grype` was also considered as a faster alternative with its own
  vulnerability DB sync, not requiring an NVD key at all.
- Third-party (non-`actions/*`) GitHub Actions in this repo are now
  pinned to a commit SHA rather than a tag, on principle — a mutable tag
  is not a security boundary, as the Trivy incident demonstrated.
- New base images or dependencies will periodically trip `npm audit`/
  Trivy; that's the intended behavior, not a bug to work around.
