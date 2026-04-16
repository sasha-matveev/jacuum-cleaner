# Jacuum Cleaner — Design Spec
*Date: 2026-04-16*

---

## 1. Overview

Jacuum Cleaner is a browser-based coding game served by a local Java Spring Boot application.
Players implement a robot vacuum algorithm in Java. The server simulates the robot on a procedurally
generated map and streams results to the browser in real-time. Each cleaned tile scores a point;
the game ends when the robot exhausts its iteration budget or cleans every tile.

---

## 2. Core Concepts

| Term | Definition |
|---|---|
| **Map** | A 2-D grid of tiles enclosed by walls, with optional interior obstacles. Every floor tile is reachable from the start. |
| **Tile** | A single grid cell. Carries: coordinates, clean/dirty state, and wall flags for all four directions. |
| **Direction** | `NORTH \| SOUTH \| EAST \| WEST` |
| **Robot** | An agent that occupies exactly one tile per iteration and moves by one step at a time. |
| **Algo** | A Java class annotated `@RobotAlgorithm` that implements `RobotAlgo`. Called once per iteration. |
| **Session** | One run of the simulation: a fixed map, algo, iteration budget, user/avatar, and recorded trace. |
| **Trace** | The ordered sequence of `(iteration, direction, x, y, score)` events recorded during a session. |
| **Score** | `+1` per newly cleaned tile. Final score also shows iterations used vs. available. |
| **Leaderboard** | Optional JSON file persisting completed sessions (trace included for replay). |

---

## 3. Map Generation

### 3.1 Input parameters

| Parameter | Type | Notes |
|---|---|---|
| `hash` | `String` | Deterministic seed. Empty → random UUID used as hash. |
| `size` | `SizePreset` | `TINY(~60)`, `SMALL(~120)`, `MEDIUM(~250)`, `LARGE(~500)` tiles. |

`hash` is converted to a `long` seed via `hash.hashCode()` extended with a 64-bit mix. Same hash → identical map every time.

### 3.2 Generation algorithm

1. Derive `Random rng` from seed.
2. Allocate a grid of `W × H` cells (W, H chosen from preset, slightly random ratio).
3. **Cellular automata (cave-style):**
   - Randomly fill ~45 % of interior cells as "wall".
   - Apply 4–5 smoothing passes (a cell becomes wall if ≥ 5 of 8 neighbours are wall).
4. **Flood-fill reachability check:** Find largest connected floor region; mark everything else as wall.
5. **Starting position:** Centroid of the largest open region, snapped to nearest floor tile.
6. **Border walls:** All perimeter cells are forced to wall.
7. **Wall flags:** Each tile's `hasWall(Direction)` is derived from whether the neighbour in that direction is a wall cell.

This produces organic, room-like shapes with islands removed. Every floor tile is guaranteed reachable.

---

## 4. Robot Algo Interface

```java
/**
 * Implement this interface and annotate the class with {@code @RobotAlgorithm}
 * to register it as a selectable algorithm.
 *
 * <p>The engine calls {@link #next(Tile)} once per iteration.
 * Return the direction the robot should move.
 * Throwing any exception is treated as an immediate unsuccessful finish (score = 0).
 *
 * <p>Stateful implementations are supported — the engine creates one instance per session.
 */
public interface RobotAlgo {
    Direction next(Tile tile) throws Exception;
}
```

```java
/** Read-only view of the robot's current tile. */
public interface Tile {
    int x();
    int y();
    boolean isClean();
    boolean hasWall(Direction direction);
}
```

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component          // picked up by Spring component scan
public @interface RobotAlgorithm {
    /** Display name shown in the UI. Defaults to simple class name. */
    String value() default "";
}
```

**Algo lifecycle:** The engine instantiates a fresh copy per session (via Spring prototype scope or direct reflection). Instance state is private to one session.

**Bundled sample algos:**
| Class | Behaviour |
|---|---|
| `RandomAlgo` | Moves in a uniformly random direction each iteration, avoiding walls. |
| `AlwaysLeftAlgo` | Prefers WEST; falls back clockwise until a passable direction is found. |

---

## 5. Game Engine

### 5.1 Session lifecycle

```
SETUP → RUNNING → PAUSED → RUNNING → FINISHED
                                   ↑ interrupt
```

### 5.2 Iteration loop

Each iteration (server-side, no real time):
1. Call `algo.next(currentTile)`. Catch all `Throwable` → `ALGO_CRASH` finish.
2. Validate direction — if the move hits a wall, the robot stays put (no penalty, counts as iteration).
3. Move robot to target tile.
4. If tile was dirty, mark clean, increment score.
5. Emit `IterationEvent {iter, direction, x, y, score, totalCleaned, mapDirtyCount}` via WebSocket.
6. If `iter == maxIterations` or all tiles clean → `FINISHED`.

### 5.3 Timing

- **Virtual time only** — no wall-clock timing per iteration.
- Client controls playback speed (delay between receiving events and rendering next frame).
- Server streams events as fast as possible; the client throttles display.

### 5.4 Scoring

```
finalScore = totalCleanedTiles × 100
           - iterationsUsed          (lower iterations = better efficiency)
```

Minimum score is 0.

---

## 6. WebSocket / API Design

All HTTP is served by Spring Boot on a single port (default 8080).

| Path | Type | Purpose |
|---|---|---|
| `GET /` | HTTP | Serves `index.html` |
| `GET /api/algos` | REST | List registered algo names |
| `GET /api/avatars` | REST | List available avatar options |
| `POST /api/session` | REST | Create session (returns `sessionId`, map snapshot) |
| `POST /api/session/{id}/start` | REST | Begin simulation |
| `POST /api/session/{id}/pause` | REST | Pause |
| `POST /api/session/{id}/resume` | REST | Resume |
| `POST /api/session/{id}/stop` | REST | Interrupt |
| `GET /api/leaderboard` | REST | Fetch leaderboard entries |
| `POST /api/leaderboard` | REST | Save completed session to leaderboard |
| `WS /ws` | STOMP | Simulation event stream |

**STOMP topics:**
- `/topic/session/{id}/events` — `IterationEvent` stream
- `/topic/session/{id}/status` — status changes (`RUNNING`, `PAUSED`, `FINISHED`)

---

## 7. Web UI

**Technology:** Vanilla JS + HTML Canvas. Served as static resources from `src/main/resources/static/`.
No npm, no build step. SockJS + STOMP.js loaded from CDN (bundled as local copies for offline use).

### 7.1 Screens

**Setup screen:**
- Map hash input (or "Generate Random") + size preset selector
- Username input (placeholder: random movie-hero name via Faker)
- Avatar picker (grid of emoji/icon choices)
- Iteration budget: snapped range slider (`250 | 500 | 1000 | 2000 | 5000`)
- Algo selector dropdown (populated from `/api/algos`)
- Preferences auto-saved to `localStorage`

**Game screen:**
- Canvas grid rendering: dirty tiles light-grey, clean tiles white, walls dark, robot as avatar icon
- Score, cleaned/total, iterations remaining — live updating
- Controls: RUN / PAUSE / RESUME / STOP, speed slider (×0.5 → ×8)
- On finish: score display, "Save to Leaderboard" button

**Leaderboard screen:**
- Table of entries: rank, avatar, username, map hash, size, score, iterations used/available
- Per-row actions: **Replay** (watch saved trace) and **Retry** (launch new session with same map + iterations, own username/algo/avatar)

### 7.2 Animation

- Client maintains an event queue fed by the WebSocket stream.
- A `requestAnimationFrame` loop dequeues events at the configured speed.
- Robot moves are interpolated (slide animation between tiles, ~150 ms per step at 1× speed).

---

## 8. Leaderboard

**Storage:** A single JSON file. Path configurable via `--leaderboard.file=<path>` CLI argument. If no path is given, the app looks for `leaderboard.json` in the working directory. If absent, leaderboard features are silently disabled.

**Entry schema:**
```json
{
  "id": "uuid",
  "username": "Luke Skywalker",
  "avatar": "🤖",
  "mapHash": "abc123",
  "mapSize": "SMALL",
  "algoName": "RandomAlgo",
  "iterationsUsed": 312,
  "iterationsAvailable": 500,
  "score": 8800,
  "completedAt": "2026-04-16T14:23:00Z",
  "trace": [
    {"iter": 1, "dir": "EAST", "x": 5, "y": 3, "score": 1},
    ...
  ]
}
```

**Replay:** The leaderboard screen streams the saved trace through the same WebSocket rendering path as a live game (no algo calls). The "Retry" action starts a fresh session with the same `mapHash`, `mapSize`, and `iterationsAvailable`.

---

## 9. Testing

| Layer | What is tested | Tool |
|---|---|---|
| Map generator | Reachability, size bounds, determinism (same hash → same map) | JUnit 5 |
| Game engine | Scoring, wall collision, ALGO_CRASH handling, finish conditions | JUnit 5 |
| Algo interface | All sample algos run on predefined maps (square, corridor, L-shape) without exception for N iterations | JUnit 5 |
| Leaderboard | Read/write/round-trip JSON, missing file graceful disable | JUnit 5 |
| REST endpoints | Status codes, response shapes | Spring MockMvc |
| WebSocket | Event emission on iteration, status transitions | Spring WebSocket test support |

Algo tests use small prebuilt maps (hardcoded cell arrays). They do not assert complete coverage — only absence of exceptions and non-negative score.

---

## 10. Configuration

`application.properties` defaults:

```properties
server.port=8080
leaderboard.file=             # empty = auto-detect leaderboard.json in cwd
game.default-iterations=500
game.max-sessions=20          # old sessions GC'd after this limit
```

---

## 11. Out of Scope (v1)

- User authentication / remote leaderboard sharing
- Multiple robots on the same map
- Custom map upload / map editor
- Sound effects
- Mobile-specific layout
