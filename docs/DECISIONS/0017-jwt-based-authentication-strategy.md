# ADR-0017 : JWT-Based Authentication Strategy (Token over Session)

## Status
Accepted

## Date
2026-07-31

## Context
Roadmap step 7, "Security foundation". Nothing authenticates or
authorizes requests today: every `/api/environments/**` endpoint,
including mutating `POST`/`PUT`/`DELETE` routes, and `/actuator/*` with
`show-details=always`, is fully open. Before adding `spring-boot-starter-
security`, roles, and route protection, a foundational choice has to be
made and documented: how does the backend recognize a request as coming
from an authenticated caller — session (server-side state, cookie) or
token (JWT, stateless)?

This choice shapes everything downstream: whether the backend needs to
store session state anywhere, how CORS/CSRF need to be configured, and
how the future Kubernetes step (roadmap step 10, multiple backend
replicas behind a load balancer) will behave without extra work.

## Options Under Consideration
- **Session-based (server-side session + cookie)** — simplest to wire up
  with Spring Security defaults, immediate revocation (delete the
  session server-side and the user is logged out instantly). Downsides:
  requires shared session storage (e.g. Redis, or sticky sessions) as
  soon as more than one backend instance runs behind a load balancer —
  which is exactly the shape roadmap step 10 (Kubernetes) is expected to
  take. Cookies are also sent automatically by the browser, which opens
  CSRF as a concern that needs explicit mitigation.
- **Token-based (JWT, stateless)** — the backend verifies a signed
  token on every request without storing anything; any instance can
  validate any token, so it scales horizontally with zero extra
  infrastructure. Fits a separate React SPA calling a JSON API well: the
  token travels in an `Authorization` header rather than a cookie, which
  sidesteps CSRF (the browser doesn't attach it automatically) at the
  cost of making XSS the primary risk to manage if the token is ever
  stored somewhere a script can read it. The real trade-off is
  revocation: a signed JWT stays valid until it expires — there's no
  "delete it server-side" — so invalidating a compromised or logged-out
  token before expiry needs a deliberate mechanism (short expiry, refresh
  tokens, or a blacklist, which reintroduces some state).

## Decision
Token-based authentication using JWT.

The deciding factor is roadmap step 10: this project is explicitly headed
towards Kubernetes, where running more than one backend replica is the
expected end state, not a hypothetical. Stateless JWT verification means
that step arrives "for free" — no shared session store to introduce
later, no sticky-session configuration to reason about. It also matches
the actual shape of the system today: a React SPA as a separate client
calling a JSON API, not a server-rendered app where cookies+sessions are
the natural fit.

The harder revocation story (no server-side "log this token out"
button) is accepted as a known, deliberate trade-off — mitigated with a
short token expiry to bound the exposure window. A refresh-token flow or
any blacklist mechanism is not in scope for this ADR; it will be
revisited if/when the project needs it (e.g. a real "log out everywhere"
requirement).

## Consequences
- No session storage needs to be introduced anywhere in the stack —
  `spring-boot-starter-security` will be configured stateless
  (`SessionCreationPolicy.STATELESS`).
- The React frontend must send the JWT via the `Authorization: Bearer
  <token>` header on every authenticated request; exact storage location
  on the client (memory vs `localStorage` vs elsewhere) is an
  implementation detail to settle when the frontend auth flow is built,
  not this ADR.
- CSRF protection is not needed for the API itself (no cookie-based auth
  to protect), simplifying the Spring Security configuration; XSS
  prevention on the frontend becomes the corresponding concern to keep
  in mind instead.
- Logout is client-side only (discard the token) unless a future ADR
  introduces refresh tokens or a blacklist — a real limitation to
  revisit if the project ever needs immediate, server-enforced logout.
- This decision directly unblocks the next roadmap-step-7 items: adding
  `spring-boot-starter-security`, a roles/authorization model, and
  protecting `/actuator/*` and mutating routes, all built on top of
  stateless JWT verification.
