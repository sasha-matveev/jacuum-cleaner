# Implementation Plan

Each phase ends with a git commit. Phases are short and independently valuable.

---

## Phase 1: Project Skeleton
**Goal:** Runnable Spring Boot app, serves a placeholder HTML page.

- [ ] Generate Maven project with Spring Boot 3.4.x initializr structure
- [ ] Add dependencies: web, jpa, h2, lombok, javafaker, test
- [ ] `JacuumApplication.java` entry point
- [ ] `application.properties` with server port 8080, H2 file mode optional
- [ ] Static `index.html` placeholder
- [ ] Verify `./mvnw spring-boot:run` starts and returns 200 on `/`

**Commit:** `feat: bootstrap Spring Boot project skeleton`

---

## Phase 2: Domain Model
**Goal:** Core types defined, compilable, no logic yet.

- [ ] `Direction` enum: UP, DOWN, LEFT, RIGHT with dx/dy helpers
- [ ] `Tile` interface: `isClean()`, `hasWall(Direction)`, `getX()`, `getY()`
- [ ] `TileImpl` record/class implementing `Tile`
- [ ] `GameMap` class: 2D grid of tiles, width/height, start position
- [ ] `RobotAlgo` interface with Javadoc
- [ ] `@VacuumAlgo` annotation
- [ ] Unit tests for `Direction` helpers

**Commit:** `feat: add core domain model (Direction, Tile, GameMap, RobotAlgo)`

---

## Phase 3: Map Generation
**Goal:** Reproducible random room-like maps, all floor tiles reachable.

- [ ] `MapGenerator` interface: `GameMap generate(String hash, SizePreset size)`
- [ ] `SizePreset` enum: TINY(~64 tiles), SMALL(~150), MEDIUM(~300), LARGE(~600)
- [ ] `RoomMapGenerator` implementation:
  - Seeds RNG from `hash.hashCode()`
  - Builds base rectangular room from size preset
  - Carves irregular walls (random walk border variation)
  - Places inner obstacle clusters
  - Flood-fill from start → remove unreachable floor tiles (make them walls)
  - Sets start position (first reachable tile from center or near-center)
- [ ] Unit tests: same hash → same map; different hash → likely different; all floor tiles reachable; start pos is floor tile

**Commit:** `feat: implement reproducible room map generator with reachability guarantee`

---

## Phase 4: Algo Engine
**Goal:** Algo registration, two sample algos, contract tests.

- [ ] `AlgoRegistry` Spring component: scans for `@VacuumAlgo` beans, exposes `Map<String, RobotAlgo>`
- [ ] `RandomAlgo`: random valid direction each step
- [ ] `AlwaysLeftAlgo`: always try LEFT; if wall, try DOWN; if wall, try RIGHT; if wall, try UP
- [ ] `AlgoContractTest` parameterized test:
  - Predefined maps: `square_5x5`, `corridor_1x10`, `L_shape`, `room_with_pillar`
  - Run each algo for 500 iterations on each map
  - Assert: no exception, at least 1 tile cleaned
- [ ] Test helper: `MapFixtures.java` — static factory methods for test maps

**Commit:** `feat: algo registry, RandomAlgo, AlwaysLeftAlgo, contract test suite`

---

## Phase 5: Game Engine
**Goal:** Core game loop, scoring, session state.

- [ ] `GameSession` record: sessionId, map, algoName, username, avatar, maxIterations, iteration counter, score, status (RUNNING / PAUSED / FINISHED / FAILED / ABORTED)
- [ ] `GameEngine` service:
  - `startSession(...)` → creates session, returns sessionId
  - `step(sessionId)` → calls `algo.next(tile)`, moves robot, updates cleaned state, increments iteration
  - Exception in `next()` → set status=FAILED, score=0
  - Wall collision → robot stays, iteration consumed
  - Returns `StepResult` (new position, cleaned flag, score, status)
- [ ] `ScoreCalculator`: `score = (cleanedTiles * 100) - iterationsUsed`
- [ ] Unit tests for game engine with fixed maps and algos

**Commit:** `feat: game engine with step execution, scoring, and session lifecycle`

---

## Phase 6: REST API
**Goal:** All endpoints wired, testable via curl/Postman.

- [ ] `MapController`: `POST /api/map/generate` → returns map metadata + tile grid
- [ ] `AlgoController`: `GET /api/algos` → list of `{id, name}` for registered algos
- [ ] `GameController`:
  - `POST /api/game/start` → starts session, returns sessionId
  - `GET /api/game/{id}/stream` → SSE stream; server advances one step per tick (tick delay controlled by speed)
  - `POST /api/game/{id}/control` body: `{action: "PAUSE"|"RESUME"|"ABORT"}`
  - `GET /api/game/{id}/state` → current session snapshot
- [ ] `SseGameService`: manages SSE emitters, runs game loop in virtual thread, respects pause/resume/abort
- [ ] Integration tests for map and algo endpoints

**Commit:** `feat: REST API for map generation, algo listing, game session control, and SSE stream`

---

## Phase 7: Basic UI
**Goal:** Playable game — map renders, robot moves, score updates.

- [ ] `index.html`: setup panel + game panel layout
- [ ] `style.css`: clean minimal look, dark/light tiles, robot indicator
- [ ] `app.js` (ES module):
  - Setup form: hash input, size select, username, avatar picker, iterations slider, algo select
  - `localStorage` save/restore for all setup fields
  - Fetch `/api/map/generate` → draw map on `<canvas>`
  - Start game → open `EventSource` on `/api/game/{id}/stream`
  - Handle SSE events: animate robot movement (smooth translate), update counters
  - Control buttons: Pause/Resume (POST control), Abort
  - End screen overlay with score + options

**Commit:** `feat: UI — map canvas, robot animation, game controls, setup persistence`

---

## Phase 8: Speed Control & Polish
**Goal:** Speed up/down works, animation is smooth.

- [ ] Speed levels 1–5, stored in session; `POST /api/game/{id}/control {action:"SPEED", level:3}`
- [ ] SSE tick delay: `[800, 400, 200, 80, 20]` ms per level
- [ ] Canvas animation: tween robot position between tiles (requestAnimationFrame)
- [ ] Visual indicators: dirty=beige, clean=white, wall=dark gray, robot=accent color
- [ ] Iterations progress bar
- [ ] Retry and New Game buttons on end screen

**Commit:** `feat: speed control, smooth canvas animation, end-game UX`

---

## Phase 9: Leaderboard
**Goal:** Optional file-based leaderboard, save, view, replay, retry.

- [ ] `LeaderboardEntry` JPA entity: all fields from spec
- [ ] `LeaderboardRepository` (Spring Data JPA)
- [ ] H2 file datasource wired only when `leaderboard.path` property is set; otherwise no-op service
- [ ] `LeaderboardService`: save entry with trace, list entries, load trace for replay
- [ ] `LeaderboardController`:
  - `GET /api/leaderboard`
  - `POST /api/leaderboard`
  - `GET /api/leaderboard/{id}/replay` → returns stored direction trace
- [ ] UI: leaderboard tab (only shown when leaderboard is available)
- [ ] Replay mode: `EventSource` replays stored trace without calling algo
- [ ] Retry: pre-fills setup form with map hash + iterations from entry

**Commit:** `feat: optional file-based leaderboard with save, view, replay, and retry`

---

## Phase 10: Final Tests & Documentation
**Goal:** All infra tested, CLAUDE.md written, README ready.

- [ ] Fill any missing unit tests (map gen edge cases, game engine edge cases)
- [ ] Integration test: full game run via HTTP (start → stream → finish)
- [ ] `CLAUDE.md`: project-specific guidance for future AI sessions
- [ ] Verify `./mvnw test` passes
- [ ] Verify `./mvnw spring-boot:run` + manual smoke test

**Commit:** `chore: complete test coverage, CLAUDE.md, final polish`

---

## Progress Tracker

| Phase | Status |
|-------|--------|
| 1 - Skeleton | done |
| 2 - Domain Model | done |
| 3 - Map Generation | done |
| 4 - Algo Engine | done |
| 5 - Game Engine | done |
| 6 - REST API | done |
| 7 - Basic UI | done |
| 8 - Speed & Polish | done |
| 9 - Leaderboard | done |
| 10 - Tests & Docs | done |
