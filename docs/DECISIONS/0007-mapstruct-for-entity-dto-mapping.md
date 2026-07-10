# ADR-0007: MapStruct for Entity <-> DTO mapping

## Status
Accepted

## Date
2026-07-10

## Context
`CLAUDE.md`'s module shape separates the JPA entity (`Environment`) from its API
representation (`EnvironmentRequest`/`EnvironmentResponse`), which means something
has to convert between them on every create/read/update. With more modules coming,
this conversion code will be written repeatedly.

## Options Under Consideration
- Manual mapping — plain Java methods/constructors converting Entity <-> DTO by
  hand. Zero new dependencies, every line is visible and easy to step through, but
  becomes repetitive boilerplate as more entities and fields are added across
  modules.
- MapStruct — an annotation processor that generates the mapping implementation at
  compile time from a `@Mapper` interface. Less boilerplate to maintain long-term
  and compile-time-checked (a mismatched field is a build error, not a runtime bug),
  at the cost of an extra dependency, an annotation processor step in the Maven
  build, and one more piece of generated code to understand when debugging.

## Decision
MapStruct. Added `org.mapstruct:mapstruct` (compile) and
`org.mapstruct:mapstruct-processor` (annotation processor path in
`maven-compiler-plugin`), version `1.6.3` (no Spring Boot-managed version, pinned
explicitly). `EnvironmentMapper` is a `@Mapper(componentModel = "spring")` interface
with `toEntity`, `toResponse`, and `updateEntityFromRequest` methods; the generated
`EnvironmentMapperImpl` is a Spring bean injected into `EnvironmentService`.

## Consequences
- Adding a field to `Environment` and its DTOs requires no manual mapping code as
  long as names line up — MapStruct matches `EnvironmentRequest`'s record components
  to `Environment`'s constructor parameters/setters by name.
- A mismatched or renamed field becomes a compile error (unmapped target property)
  instead of a silent runtime bug, which is a meaningful safety net for a project
  meant to build good habits.
- The mapper's generated implementation lives under `target/generated-sources` and
  must be regenerated on every build — nothing to commit, but IDEs occasionally need
  a manual "reload Maven project" for `EnvironmentMapperImpl` to resolve during
  editing.
