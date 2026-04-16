# CLAUDE.md — Jacuum Cleaner Project

## Quick Context

Vacuum cleaner coding game. Java 21 + Spring Boot 3.4.x, single runnable app.
Spec: `docs/spec.md` | Stack: `docs/stack.md` | Plan: `docs/plan.md`

## Running the App

```bash
./mvnw spring-boot:run
# Then open http://localhost:8080
```

## Running Tests

```bash
./mvnw test
```

## Key Packages

| Package | Purpose |
|---------|---------|
| `com.jacuum.map` | Direction, Tile, GameMap, map generation |
| `com.jacuum.algo` | RobotAlgo interface, @VacuumAlgo annotation, registry, implementations |
| `com.jacuum.game` | GameEngine, GameSession, SseGameService, scoring |
| `com.jacuum.leaderboard` | Optional H2-backed persistence |
| `com.jacuum.api` | Spring MVC REST controllers |

## Adding a New Algorithm

1. Create a class in `com.jacuum.algo.impl`.
2. Annotate with `@VacuumAlgo("My Algo Name")`.
3. Implement `RobotAlgo` — one method: `Direction next(Tile currentTile)`.
4. It will be auto-detected and appear in the UI selector.
5. It will automatically be covered by `AlgoContractTest`.

## Algo Contract Rules

- `next()` is called once per iteration. Must return a `Direction`. Never null.
- Returning a direction toward a wall is allowed (robot stays put, iteration consumed).
- Throwing any exception ends the run immediately: status=FAILED, score=0.
- The algo may maintain internal state between calls (it's a stateful bean — be aware of session scope).

## Leaderboard Setup (Optional)

Add to `application.properties` or pass as CLI arg:

```
leaderboard.path=/path/to/leaderboard.db
```

If not set, leaderboard features are silently disabled.

## Design Decisions

- **SSE not WebSocket**: one-directional server→client stream is sufficient; no need for bidirectional.
- **H2 file mode**: zero-config file DB for leaderboard. Activated only when `leaderboard.path` is set.
- **Virtual threads**: `SseGameService` uses virtual threads (Java 21) for per-session game loops.
- **Score formula**: `(cleaned_tiles × 100) − iterations_used`. Min 0. Higher is better.
- **All floor tiles reachable**: guaranteed by flood-fill post-processing in `RoomMapGenerator`.

## Implementation Progress

See `docs/plan.md` for detailed phase tracking.
