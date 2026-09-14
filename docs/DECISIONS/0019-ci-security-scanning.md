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
- **OWASP dependency-check-maven** was the main cost/benefit call: it
  scans Java dependencies against the NVD feed, but without an
  `NVD_API_KEY` secret it's slow and rate-limited enough to make CI
  unreliable as a hard merge gate.

## Decision
Full bundle: `npm audit` (frontend), Dependabot alerts (enabled at the
repo level), `gitleaks` (secret scanning across git history), `Trivy`
(scans the actual built prod images, not just source), and OWASP
`dependency-check-maven` (backend deps) — the last one wired with
`continue-on-error: true` and a cached NVD data directory until an
`NVD_API_KEY` is configured and the job's been observed to run reliably.

Two supply-chain findings surfaced *while wiring this up*, fixed before
landing: `aquasecurity/trivy-action` had 76 of 77 version tags hijacked
in a March 2026 incident (credential-stealing `entrypoint.sh`,
GHSA-69fq-xp46-6x23) — pinned to the commit SHA behind the one
unaffected tag (`0.35.0`) rather than a mutable tag. Separately,
`gitleaks-action@v2` and `actions/cache@v4` are Node 20-based, and
GitHub was days from dropping Node 20 runner support — bumped to
`gitleaks-action@v3` (also SHA-pinned) and `actions/cache@v5`.

## Consequences
- The OWASP job can go green/red somewhat inconsistently until an NVD
  API key is added — `continue-on-error` means it won't block merges in
  the meantime, but also means it's not yet a real gate. Revisit once a
  key is configured.
- Third-party (non-`actions/*`) GitHub Actions in this repo are now
  pinned to a commit SHA rather than a tag, on principle — a mutable tag
  is not a security boundary, as the Trivy incident demonstrated.
- New base images or dependencies will periodically trip these scans;
  that's the intended behavior, not a bug to work around.
