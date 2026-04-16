# Jacuum Cleaner Agent Instructions

## Product Direction

Use `docs/Task.md` as the original request and `docs/spec.md` as the approved product spec. Use `docs/stack.md` for technology decisions. If the documents conflict, prefer the approved spec, then the stack document, then the original task.

## Architecture Boundaries

Keep server-side logic authoritative.

- `domain`: immutable model types and value objects.
- `algo`: public robot algorithm API, registration, and sample algorithms.
- `engine`: simulation lifecycle, scoring, trace generation, and failure handling.
- `mapgen`: deterministic room generation and reachability validation.
- `leaderboard`: optional local JSON persistence.
- `web`: Spring MVC controllers, DTOs, and static browser assets.

The simulation engine must not depend on web controllers or persistence. Map generation must be deterministic and testable without Spring.

## Implementation Rules

- Use Java 21 and Spring Boot 4.0.5.
- Keep the UI thin: browser code displays state, animates movement, and calls server APIs.
- Prefer small focused Java records/classes over large mixed-responsibility files.
- Add tests before implementation for engine, map generation, leaderboard, and API behavior.
- Treat algorithm exceptions as failed runs with zero score.
- Keep leaderboard persistence optional; the app must run without a configured file.
- Document decisions in `docs/progress.md` as phases are completed.

## Commands

After the Maven wrapper exists:

```powershell
.\mvnw test
.\mvnw spring-boot:run
```

## Commit Discipline

The implementation plan requires a meaningful commit after every phase. Do not rewrite unrelated user changes. Include docs/progress.md updates in each phase commit.

## Current Planning Files

- `docs/spec.md`: approved product behavior.
- `docs/stack.md`: selected stack and exclusions.
- `docs/plan.md`: implementation phases and safepoints.
- `docs/progress.md`: status log.
