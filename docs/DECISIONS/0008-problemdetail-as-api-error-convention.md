# ADR-0008: RFC 7807 ProblemDetail as the API error convention

## Status
Accepted

## Date
2026-07-10

## Context
The `Environment` CRUD API needs a consistent error response shape for validation
failures (400), not-found (404), and name conflicts (409) — and every business
module built after it (`Application`, `Deployment`, ...) will need the same thing.
This is exactly the kind of architectural convention `PROJECT_GUIDE.md` asks to be
decided deliberately and documented, not improvised per controller.

## Options Under Consideration
- Custom error DTO — a hand-rolled shape such as
  `{timestamp, status, message, path}`. Full control over the exact fields, but it
  reinvents something the framework already provides, and that shape has to be
  documented and kept consistent by hand across every module and every developer
  touching the API.
- `ProblemDetail` (RFC 7807) — built into Spring Framework 6+/Boot 3+ with zero extra
  dependencies. Standard fields (`type`, `title`, `status`, `detail`, `instance`)
  plus arbitrary extension properties via `setProperty(...)`, which covers the
  field-level validation detail this API needs.

## Decision
`ProblemDetail`. `spring.mvc.problemdetails.enabled=true` was set in
`application.properties`, and `common/GlobalExceptionHandler` (a
`@RestControllerAdvice`) maps `NotFoundException` → 404, `ConflictException` → 409,
`DataIntegrityViolationException` → 409 (guards against a concurrent unique-constraint
race), and `MethodArgumentNotValidException` → 400 with a field-level `errors` map
attached as a `ProblemDetail` extension property. The handler is annotated
`@Order(Ordered.HIGHEST_PRECEDENCE)`: Spring Boot's own built-in ProblemDetail
handling for `MethodArgumentNotValidException` would otherwise win first (same exact
exception-type match, default ordering), producing a generic message instead of the
field-level detail this project wants.

## Consequences
- Every future module reuses the same `NotFoundException`/`ConflictException` base
  classes from `common/` and gets consistent 404/409 handling for free — no new
  `@ExceptionHandler` code needed per module for those cases.
- Error responses are self-descriptive (`Content-Type: application/problem+json`)
  and match a published standard, which is worth more on a portfolio project than a
  bespoke shape.
- The precedence subtlety with Spring's own problemdetails support is not obvious
  from the code alone — worth remembering if a future `@ExceptionHandler` for a
  framework-level exception silently doesn't seem to fire.
