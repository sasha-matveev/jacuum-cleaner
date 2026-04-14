# Jacuum Cleaner Progress Log

## 2026-04-14

### Completed

- Read original product task from `docs/Task.md`.
- Created approved product specification in `docs/spec.md`.
- Documented selected technology stack in `docs/stack.md`.
- Created repository agent instructions in `AGENTS.md`.
- Created focused future-agent briefs:
  - `.codex/agents/engine-worker.md`
  - `.codex/agents/ui-worker.md`
- Created phased implementation plan in `docs/plan.md`.

### Decisions

- Treat `docs/spec.md` as the approved product behavior.
- Use Java 21, Spring Boot 4.0.5, Maven Wrapper, Spring MVC, plain browser JavaScript, JUnit 5, AssertJ, Datafaker 2.5.4, and optional JSON leaderboard persistence.
- Do not create a project-specific custom skill yet. Current project conventions fit better in `AGENTS.md`; a reusable skill should be added only after a repeated workflow emerges and can be tested.

### Current Safepoint

Planning is complete. The next step is to choose an execution approach before implementation begins.

### Phase 1, Task 1.1

- Scaffolded the Maven Spring Boot project with Java 21 and Spring Boot 4.0.5.
- Added the application entry point at `src/main/java/dev/ytype/jacuum/VacuumCleanerApplication.java`.
- Added empty `src/main/resources/application.properties`.
- Generated a real Maven Wrapper setup with `maven-wrapper.jar` and canonical launcher scripts.
- Kept the wrapper distribution cache and Maven local repository inside the worktree so `.\mvnw test` works without a global Maven install.
- Test command: `.\mvnw test`
- Result: `BUILD SUCCESS`
- Summary: no tests were present, so Maven reported `No tests to run.`

### Phase 1, Quality Fix

- Added `.m2/` and `.mvn/wrapper/dists/` to `.gitignore` so wrapper caches stay ignored after local test runs.
- Verified `.\mvnw test` still passes with the wrapper cache and local Maven repository in the worktree.
- Verified `git status --short --ignored` shows `.m2/`, `.mvn/wrapper/dists/`, and `target/` as ignored entries (`!!`).

### Phase 1, Task 1.2

- Added immutable domain types: `Direction`, `Coordinate`, `SizePreset`, `RunStatus`, `TraceStep`, `TileView`, and `RoomMap`.
- Added focused test coverage in `DomainModelTest` for coordinate movement and size preset defaults.
- Verified `.\mvnw -Dtest=DomainModelTest test` after implementing the domain types.
- Verified `.\mvnw test` passes for the full suite.
- Notes: `RoomMap` is immutable, requires the start tile to be a floor tile, and exposes directional wall checks for future engine and algorithm work.

### Phase 1, Task 1.2 Review Fix

- Corrected `docs/plan.md` so only Task 1.1 and Task 1.2 stay checked; later phase checkboxes are back to unchecked.
- Tightened `RoomMap` validation to reject null inputs, null floor entries, out-of-bounds floor tiles, and invalid dimensions while keeping the start-tile check.
- Added focused coverage for floor and wall lookup, directional wall detection, reachable floor count, defensive copying, invalid inputs, and explicit null behavior on `Coordinate.move` and `RoomMap.hasWall`.
- Verified `.\mvnw -Dtest=DomainModelTest test`.
- Verified `.\mvnw test`.

### Phase 2, Task 2.1

- Added deterministic map generation in `mapgen` with SHA-256 seeding from `hash + ":" + preset.name()`.
- Generated rooms stay inside the outer boundary, keep the start tile on floor, and are validated for full reachability before returning.
- Added `MapValidator` reachability and boundary checks plus a small `GeneratedMap` value object for returning the generated hash, preset, and `RoomMap`.
- Added focused coverage for equal maps on repeated generation, different hashes producing different layouts, reachability, boundary walls, and the `TINY` preset producing floor tiles.
- Verified `.\mvnw -Dtest=MapGeneratorTest test`.
- Verified `.\mvnw test`.
