# ADR-0025 : Fail-Fast Secrets and a Property-Gated Demo Seeder

## Status
Accepted

## Date
2026-11-03

## Context
A pre-Kubernetes audit found that `JWT_SECRET` was listed in
`.env.example` but never passed to the `backend` container by
`compose.yml`. The backend therefore always fell back to the default
value hardcoded in `JwtConfig`'s `@Value` — a secret published on
GitHub. Anyone could forge a valid `ADMIN` token for any deployment of
this app, and nothing at startup hinted that anything was wrong.
`SecurityConfig` had the same pattern for the admin and Prometheus
credentials (`adminpass`, `prometheuspass`).

The root cause is not the signing algorithm (HS256 stays the right fit,
see ADR-0018 — switching to RS256 with a hardcoded default private key
would reproduce the exact same bug) but **silent fallbacks**: a missing
piece of configuration produced a running, healthy-looking, insecure
app.

Removing those defaults broke the test suite, which had relied on them,
and exposed a second coupling: the `test` Spring profile was used both
to mean "we are in a test" and to switch off `DataSeeder`
(`@Profile("!test")`), so `DataSeederTest` had to run under a separate
profile and couldn't receive test-only configuration.

## Options Under Consideration
- **Keep defaults, document "always set these in prod" (rejected)** —
  relies on every operator remembering; this bug is the proof it fails.
- **No defaults, fail fast at every layer (chosen)** — compose refuses
  to start (`${VAR:?message}`, which catches unset *and* empty values),
  Spring refuses to start on an unresolvable placeholder, and `JwtConfig`
  rejects a secret shorter than HS256's 256-bit minimum in its
  constructor. Without that last check, Nimbus only rejects a short key
  when signing — at the first login, after the app passed its
  healthchecks. Defense in depth matters here because compose is only
  one way to start the backend (`./mvnw spring-boot:run`, tests, and
  Kubernetes next don't go through it).
- **Seeder: keep `@Profile("!test")`, add an
  `application-data-seeder-test.properties` (rejected)** — duplicates
  the test-only values in a second file that will drift.
- **Seeder: `@ConditionalOnProperty` on `app.demo-data.enabled`
  (chosen)** — whether to seed is a per-deployment setting, not an
  environment identity. Off when the property is absent (`havingValue =
  "true"`, no `matchIfMissing`), so a deployment that says nothing never
  gets fake data.

## Decision
- No default value for any secret or credential (`JWT_SECRET`,
  `PASSWORD_ENCODER_ID`, `APP_ADMIN_USER/_PASSWORD`,
  `PROMETHEUS_SCRAPE_USER/_PASSWORD`), in `@Value` or anywhere else.
- `compose.yml` marks all six as required with an actionable message.
- `JwtConfig` validates the secret length at construction, covered by
  `JwtConfigTest`.
- Test-only values live in `src/test/resources/application-test.properties`
  (never packaged in the jar), loaded through the `test` profile that
  surefire already activates. It only holds test-specific overrides —
  everything else is inherited from `application.properties`.
- `DataSeeder` is gated by `app.demo-data.enabled`: `true` in
  `application.properties` (the portfolio demo still seeds dev and prod,
  as ADR-0024 intended), `false` in `application-test.properties`,
  re-enabled inline by `DataSeederTest`. Any deployment can turn it off
  with `APP_DEMODATA_ENABLED=false`.

## Consequences
- A misconfigured deployment now fails immediately, with a message
  naming the missing or invalid setting, instead of running insecurely.
- `./mvnw spring-boot:run` outside Docker now needs those six variables
  exported, not just the datasource ones.
- Existing `.env.dev`/`.env.prod` files must define `JWT_SECRET` (≥ 32
  characters) — previously it was silently ignored.
- Every token issued before this change was signed with the leaked
  default secret; they expire within an hour, and the secret itself must
  be considered compromised forever (never reuse it).
- The Kubernetes step will need these values as `Secret`s — the
  fail-fast behavior carries over unchanged.
