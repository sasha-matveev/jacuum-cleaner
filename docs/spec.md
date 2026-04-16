# Jacuum Cleaner Product Spec

## Purpose

Jacuum Cleaner is a local coding game for testing smart vacuum cleaner algorithms. A player chooses a generated room, a robot avatar, an iteration limit, and one registered algorithm. The application runs the algorithm on the server, animates the robot in a browser UI, scores the attempt, and optionally stores the result in a local leaderboard file.

The main product goal is to make robot algorithms easy to write, run, compare, replay, and improve.

## Core Concepts

### Map

A map is a deterministic room layout made of square tiles. Each tile is either reachable floor or wall/obstacle. The robot starts on a floor tile that belongs to the map.

Map generation must satisfy these rules:

- A map can be generated from a user-provided hash.
- The same hash and size preset always produce the same layout and start position.
- A map can also be generated without a provided hash; in that case the server creates a new hash and exposes it in the UI.
- Every floor tile is reachable from the start tile.
- The outer boundary is closed by walls.
- Obstacles may exist inside the room, but they must not make floor tiles unreachable.
- Generated shapes should look like plausible rooms to a human observer rather than arbitrary noise.

The first version will support size presets instead of a raw tile count: `tiny`, `small`, `medium`, and `large`. Internally, each preset maps to target dimensions and an approximate floor tile count. Exact dimensions are an implementation detail as long as the preset is deterministic.

### Robot Algorithm

Robot algorithms are Java classes registered in the Spring application. The UI lists all registered algorithms and lets the user choose one before running an attempt.

The public algorithm contract is intentionally small:

```java
Direction next(Tile tile);
```

`Direction` has four values: `UP`, `RIGHT`, `DOWN`, and `LEFT`.

`Tile` is a read-only view of the robot's current tile. It exposes only the information an algorithm needs for the first version:

- whether the current tile is already clean
- whether a wall blocks movement in a given direction
- the current coordinate
- the iteration number

Algorithms may keep their own internal state. The game engine creates a fresh algorithm instance for each run so attempts do not leak state into each other.

If an algorithm throws an exception during `next`, the run ends immediately as failed and receives zero score.

Initial sample algorithms:

- `RandomWalkAlgorithm`: chooses a random legal direction.
- `AlwaysLeftAlgorithm`: always tries `LEFT`, and stays blocked if a wall exists.
- `WallFollowerAlgorithm`: a simple deterministic baseline that prefers turning left, then forward, then right, then back.

### Run

A run is one execution of one algorithm on one map.

At the start of a run:

- the robot is placed at the map start tile
- the start tile becomes clean
- cleaned tile count starts at 1 for the cleaned start tile
- score starts at 1000 for the cleaned start tile
- the remaining iteration count is the configured limit

Each iteration:

1. The engine calls `algorithm.next(currentTile)`.
2. If the algorithm throws, the run fails with zero score.
3. If the chosen direction is blocked by a wall, the robot stays in place and the iteration is consumed.
4. If the direction is open, the robot moves one tile.
5. If the destination tile was dirty, it becomes clean and adds one point.
6. The step is appended to the trace.

A run ends when:

- all reachable tiles are clean
- the iteration limit is exhausted
- the user interrupts the run
- the algorithm fails with an exception

### Score

The first scoring formula is:

```text
score = cleanedTiles * 1000 - iterationsUsed
```

Higher score is better. This preserves the product intent that cleaned tiles matter most while fewer iterations break ties.

The UI also displays:

- cleaned tile count
- total reachable tile count
- iterations used
- iterations available
- completion percentage
- run status

### Trace

A trace is the complete server-side record needed to replay a run without invoking the algorithm again.

Each trace step contains:

- iteration number
- previous coordinate
- requested direction
- resulting coordinate
- whether movement was blocked
- whether the resulting tile was newly cleaned
- score after the step

Replays use only the saved trace, map hash, map size, start position, avatar, and run metadata.

## User Experience

### Main Screen

The first screen is the playable experience, not a landing page.

The UI contains:

- map setup controls
- username control with generated default and "generate another" action
- robot avatar selector with random fallback
- map hash input and random hash action
- map size preset selector
- iteration limit selector with snapped values
- algorithm selector
- run controls: run, pause, resume, stop, speed up, slow down
- animated map view with square tiles and smooth robot movement
- current run stats and final score
- optional leaderboard area when a leaderboard file is configured or discovered

The UI is thin. It asks the server to create maps, list algorithms, start runs, control playback, and persist scores. Browser code owns animation timing and display state only; simulation truth stays on the server.

### Preferences

The browser stores non-sensitive UI preferences in `localStorage`:

- last username
- last selected avatar
- last map size preset
- last iteration limit
- last animation speed
- last selected algorithm id

The leaderboard file path is not stored in browser storage. It is provided through application configuration or request-time setup because it is a local filesystem concern.

### Leaderboard

Leaderboard storage is optional. The application must run without a leaderboard file.

The server discovers leaderboard storage in this order:

1. an explicit application property or command-line argument
2. a default file named `jacuum-leaderboard.json` in the working directory
3. no persistence

When persistence is available, users can save completed runs. Failed runs can be displayed locally but are not saved by default.

Each saved leaderboard row contains:

- id
- created timestamp
- robot avatar
- username
- algorithm id and display name
- map hash
- map size preset
- iterations used
- iterations available
- cleaned tile count
- reachable tile count
- score
- final status
- trace

Leaderboard actions:

- replay a saved attempt from trace
- retry the same map hash, size, and iteration limit with a selected algorithm, username, and avatar

## Server API

The server exposes the browser UI and JSON endpoints from the same local Spring Boot application.

Initial API shape:

- `GET /` serves the UI.
- `GET /api/algorithms` lists registered algorithms.
- `POST /api/maps` creates or loads a deterministic map from hash and size preset.
- `POST /api/runs` starts a server-side run request and returns the initial state.
- `POST /api/runs/{id}/steps` advances a run by one or more iterations.
- `POST /api/runs/{id}/stop` stops a run.
- `GET /api/leaderboard` returns saved leaderboard rows when storage is available.
- `POST /api/leaderboard` saves an eligible run.
- `GET /api/leaderboard/{id}/replay` returns replay data for a saved run.

The first implementation can use request/response polling for run steps. WebSockets or server-sent events are not required for the initial product because virtual time and animation speed are controlled by the browser.

## Architecture

The application is organized around clear server-side boundaries:

- `domain`: immutable model types such as map, tile, direction, coordinate, trace, score, and run result.
- `algo`: public algorithm interface, algorithm registry, and sample implementations.
- `engine`: deterministic simulation rules and run lifecycle.
- `mapgen`: deterministic room generation from hash and size preset.
- `leaderboard`: optional local JSON persistence and replay loading.
- `web`: Spring MVC controllers, request/response DTOs, and static UI resources.

The simulation engine depends on algorithm interfaces and domain models. It does not depend on web controllers or persistence.

Map generation is deterministic and testable without Spring. Leaderboard persistence is optional and must degrade cleanly when no file is configured.

## Testing Requirements

Tests are required for infrastructure and core behavior before UI polish.

Required test coverage:

- deterministic map generation for same hash and size
- map reachability validation
- wall boundaries and blocked movement
- score calculation
- trace generation
- algorithm exception handling as failed zero-score result
- algorithm registry discovery
- leaderboard read/write behavior when a file is configured
- graceful behavior when leaderboard storage is absent
- sample algorithm smoke tests on predefined maps

Predefined algorithm test maps:

- square room
- corridor
- loop/circle-like route
- room with an internal obstacle
- single-tile room

Algorithm tests verify that sample algorithms do not throw and produce valid directions. They do not require every algorithm to clean the full map.

## AI Project Environment

The repository should include AI-facing project instructions so future work can resume safely from documented decisions.

Planned environment artifacts:

- `AGENTS.md`: repository instructions, architecture boundaries, testing expectations, and commit discipline.
- `docs/stack.md`: selected technologies and rationale.
- `docs/plan.md`: phased implementation plan with safepoints.
- `docs/progress.md`: completion log for phases and decisions.
- `.codex/skills/`: project-specific skills if repeated workflows emerge during implementation.
- `.codex/agents/`: project-specific agent briefs if implementation work becomes large enough to delegate later.

Project-specific AI artifacts must be small, practical, and derived from this spec. They should not replace normal code documentation.

## Phasing Expectations

Implementation must be split into short valuable phases. Each phase should leave the product in a runnable or verifiable state and end with a commit that has a meaningful message.

Expected phase sequence:

1. Spring Boot skeleton, domain model, and test setup.
2. Deterministic map generation and map API.
3. Algorithm interface, registry, sample algorithms, and engine tests.
4. Run API with trace and scoring.
5. Thin browser UI for setup, map display, and run animation.
6. Optional local leaderboard persistence and replay.
7. Polish, documentation, and final verification.

The implementation plan may refine this sequence, but each phase must produce user-visible value or tested core behavior.

## Open Decisions Resolved For First Version

- Use size presets instead of a free-form numeric map size.
- Use higher-is-better scoring with cleaned tiles weighted above iteration count.
- Use `localStorage` for browser preferences.
- Use optional JSON file persistence for leaderboards.
- Use HTTP polling/step requests instead of WebSockets for run animation.
- Treat blocked movement as a consumed iteration with no score change.
- Clean the starting tile immediately.
- Save only completed non-failed runs by default.
