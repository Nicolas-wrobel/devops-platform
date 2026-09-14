# ADR-0018 : JWT Implementation — OAuth2 Resource Server, HS256, Dual Filter Chains

## Status
Accepted

## Date
2026-09-14

## Context
ADR-0017 decided JWT over sessions. This ADR covers the concrete *how*:
which library validates the token, which signing algorithm, and how to
reconcile JWT with Prometheus, which scrapes `/actuator/prometheus` via
Basic Auth and can't speak JWT at all.

## Options Under Consideration
- **Hand-rolled `OncePerRequestFilter` + a JWT library (e.g. jjwt) vs.
  Spring Security's `oauth2-resource-server` support** — the built-in
  resource server support validates signature/expiration and builds the
  `Authentication` object without custom filter code; a hand-rolled
  filter would duplicate logic Spring already maintains.
- **Symmetric (HS256, one shared secret) vs. asymmetric (RS256, key
  pair)** — this backend is both the sole issuer and sole verifier of
  its tokens; asymmetric keys earn their complexity when a *different*
  service needs to verify tokens without ever holding the signing
  secret, which isn't the case here.
- **One `SecurityFilterChain` with both `httpBasic` and
  `oauth2ResourceServer` vs. two chains scoped by route** — a single
  chain works (Spring dispatches by the `Authorization` header's scheme
  automatically) but leaves Basic Auth valid on every route, not just
  the one that needs it (Prometheus).

## Decision
Spring Security's `oauth2-resource-server` (Nimbus-backed
`JwtEncoder`/`JwtDecoder`), symmetric HS256 with a shared secret
(`JWT_SECRET`), and **two** `@Order`-ed `SecurityFilterChain` beans:
`basicSecurityFilterChain` (`securityMatcher` scoped to
`/actuator/health` and `/actuator/prometheus`, httpBasic, a dedicated
`prometheus`/`METRICS` account) and `jwtSecurityFilterChain` (catch-all,
JWT). A `JwtAuthenticationConverter` reads a custom `roles` claim and
re-applies the `ROLE_` prefix so `hasRole(...)` checks work unchanged
whether the caller authenticated via JWT or (on the Basic chain) via
`UserDetails.roles(...)`.

## Consequences
- `POST /api/auth/login` must put the caller's roles in the token
  (`roles` claim) — there's no session or user store to fall back on
  once the request is stateless.
- Prometheus's scrape credentials live as Docker Compose file-based
  secrets (`username_file`/`password_file`), not env vars, because
  `prometheus.yml` is version-controlled and can't hold a plaintext
  secret the way gitignored `.env.*` files can (ADR-0003).
- Revoking a single token before expiry is still not possible (accepted
  in ADR-0017); this ADR doesn't change that trade-off.
- If a second service ever needs to verify these tokens independently,
  HS256 requires sharing the same secret with it — that's the point at
  which switching to RS256 (verify with a public key only) would pay off.
