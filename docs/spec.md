# Jacuum Cleaner — Product Specification

## Overview

A locally-run, single-page coding game where users implement a vacuum robot algorithm that navigates and cleans a randomly generated room map. The game is scored by cleaned tiles and iterations used. The server hosts both the UI and the API.

---

## Core Concepts

### Map

- A **map** is a 2D grid of **tiles**. Each tile is either **floor** (cleanable) or **wall** (impassable).
- Maps are **room-like**: bounded by walls, may have inner obstacles, but every floor tile must be reachable from every other floor tile (guaranteed connectivity).
- Map generation is seeded by a **hash string** — identical hash → identical map.
- If no hash is provided, a random hash is generated.
- The **starting position** of the robot is a property of the map (derived from the hash/seed).
- Users may specify a **size** preset: `tiny`, `small`, `medium`, `large` (or a numeric hint). The preset controls approximate tile count.

### Robot

- The robot occupies one tile at a time.
- Moving to a floor tile **cleans** it. Already-clean tiles count as valid moves but score no points.
- The robot cannot move into wall tiles.
- In each **iteration**, the algorithm returns one `Direction` (UP, DOWN, LEFT, RIGHT).
- Attempting to move into a wall is a no-op (robot stays in place, iteration is consumed).

### Algorithm Interface

```java
/**
 * One method: given the current tile, return the next direction to move.
 * Called once per iteration. If this method throws, the run ends immediately
 * with zero points regardless of progress.
 */
public interface RobotAlgo {
    Direction next(Tile currentTile);
}
```

- `Tile` provides: `isClean()`, `hasWall(Direction)`, `getX()`, `getY()`.
- Algorithms are annotated with `@VacuumAlgo("Display Name")` and auto-registered via Spring component scan.
- Sample implementations: `RandomAlgo`, `AlwaysLeftAlgo`.

### Game Session

- A session has a fixed **max iterations** budget (user-configurable, snapped presets).
- The session ends when: all floor tiles are clean, iterations are exhausted, or an exception is thrown by the algo.
- **Score** = cleaned tiles × weight − iterations_used × penalty (exact formula TBD, displayed as integer).

### Scoring

```
score = (cleaned_tiles * 100) - (iterations_used * 1)
```

Minimum score is 0. Higher is better.

---

## Features

### Map Setup Screen

- Generate random map (auto-hash) or enter a hash manually.
- Select size preset: tiny / small / medium / large.
- Enter username (optional; defaults to a generated movie-hero fake name via Faker).
- Select robot avatar from a curated list (or random).
- Select iteration budget via a snapping slider (`100 | 250 | 500 | 1000 | 2500 | 5000`).
- Select algorithm from all registered `@VacuumAlgo` implementations.
- Settings auto-saved in browser `localStorage` between visits.

### Game View

- Renders the map as a grid of squares.
- Robot position animated smoothly — no instant jumps.
- Cleaned vs dirty tiles visually distinct.
- Displays: current iteration, cleaned count, total floor tiles, live score.
- Controls: **Run**, **Pause**, **Resume**, **Speed Up**, **Speed Down**, **Abort**.
- Speed levels: 1 (slowest) → 5 (fastest), controls delay between SSE-streamed steps.

### End Screen

- Shows final score, cleaned %, iterations used.
- Option to **Save to Leaderboard** (if leaderboard file is configured).
- Option to **Retry** (same map, choose new algo/username/avatar).
- Option to **New Game**.

### Leaderboard

- Optional — app works without it.
- User specifies a path to a leaderboard file at startup (CLI argument or config file property `leaderboard.path`).
- Each entry stores:
  - Username
  - Robot avatar
  - Map hash + size
  - Max iterations / iterations used
  - Score
  - Execution trace (list of directions taken, for playback)
  - Timestamp
- Leaderboard screen: sortable table by score / map / date.
- Per-row actions:
  - **Watch Replay**: play back the stored trace (no algo involved).
  - **Retry**: load same map + iterations, pick new username/avatar/algo.

---

## Algo Unit Tests

Every `RobotAlgo` implementation must pass a shared parameterized test suite using predefined maps:
- `square_5x5` — fully open 5×5 room.
- `corridor_1x10` — single-tile-wide corridor.
- `L_shape` — L-shaped room.
- `room_with_pillar` — open room with one inner wall tile.

Tests verify:
- No exception thrown during N iterations on these maps.
- Robot cleans at least 1 tile.
- Algorithm does not violate tile API contract.

Tests do NOT require 100% map coverage.

---

## API Endpoints (summary)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Serve UI (index.html) |
| POST | `/api/map/generate` | Generate map by hash+size |
| GET | `/api/algos` | List registered algorithms |
| POST | `/api/game/start` | Start a game session |
| GET | `/api/game/{id}/stream` | SSE stream of iteration events |
| POST | `/api/game/{id}/control` | Pause / Resume / Abort |
| GET | `/api/leaderboard` | List leaderboard entries |
| POST | `/api/leaderboard` | Save entry |
| GET | `/api/leaderboard/{id}/replay` | Get replay trace |

---

## Non-Functional Requirements

- Runs with a single `./mvnw spring-boot:run` (or IDE Run).
- No external database or network required.
- Leaderboard file is optional; missing/unreadable file → leaderboard disabled, no crash.
- All infra code (map generation, game engine, leaderboard I/O) has unit/integration tests.
- Algo exception → graceful game-end with `status=FAILED, score=0`.
