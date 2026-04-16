# Bug & Issue Report

Reviewed: all Java source, `app.js`, `style.css`, tests. Sorted critical → minor.

---

## CRITICAL

### BUG-01 — Algo beans are singletons, not prototype-scoped
**File:** `AlgoRegistry.java:41`, `VacuumAlgo.java`  
`getAlgo()` returns the Spring singleton bean. Multiple concurrent sessions using the same algorithm share one instance, including all mutable fields (e.g., `RandomAlgo.rng`). For any user-written stateful algo, two sessions would corrupt each other's state. `CLAUDE.md` and `RobotAlgo.java` both promise "a fresh instance is created per game session" — the promise is false.  
**Fix:** Add `@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)` to `@VacuumAlgo`, or use `ApplicationContext.getBean()` instead of `getBeansWithAnnotation()` so Spring creates a new instance each time.

---

### BUG-02 — RESUME creates a second concurrent game loop
**File:** `app.js:141-148`, `SseGameService.java:30-37`  
Clicking RESUME calls `openStream()`, which creates a new `EventSource` and a second virtual-thread game loop via `sseService.stream()`. The original loop is still running. Both loops call `engine.step()` concurrently on the same session, doubling the iteration rate and corrupting position/state. `SseGameService` has no guard against duplicate streams for the same session.  
**Fix:** Track active emitters per session in `SseGameService`; call `complete()` on any existing emitter before creating a new one. On the UI side, RESUME should only send the RESUME control action — the existing SSE connection already delivers events when the game loop continues.

---

### BUG-03 — `GameSession` has data races across threads
**File:** `GameSession.java`  
Fields `robotX`, `robotY`, `iterationsUsed`, `status`, `trace` are written by the virtual-thread game loop and read concurrently by HTTP request threads (e.g., `/state`, `/control`, score calculation). There is no synchronization. While JVM guarantees atomicity for 32-bit int reads/writes (no word-tearing), `status` and the `trace` list can be observed in stale or partially-updated state.  
**Fix:** Make `status` `volatile`, use `AtomicInteger` for counters, or synchronize `step()` and `getScore()` at the `GameEngine` level.

---

### BUG-04 — Sessions are never evicted from `GameEngine` — memory leak
**File:** `GameEngine.java:25`  
`sessions` (a `ConcurrentHashMap`) grows without bound. Every completed, failed, or aborted session stays in memory forever. A long-running server will eventually OOM.  
**Fix:** Remove sessions on completion/failure/abort, or use a time-based eviction cache (e.g., Caffeine) with a TTL of ~30 minutes.

---

### BUG-05 — Replay crashes silently when trace is empty
**File:** `LeaderboardController.java:52`, `app.js` (replay loop)  
`e.getTrace().split(",")` on an empty string returns `[""]` (one-element array with an empty string). A session that failed before making any move has `trace = ""`. In the JS replay loop, `DIRECTIONS[""]` is `undefined`, so `nx = rx + undefined` evaluates to `NaN`. All subsequent position checks fail silently and the replay loop runs to completion without drawing anything.  
**Fix:** Guard against empty traces: `trace.isEmpty() ? List.of() : Arrays.asList(trace.split(","))`.

---

## HIGH

### BUG-06 — `countFloorTiles()` recomputed on every step — O(w×h) per iteration
**File:** `GameMap.java:56-60`, `GameEngine.java:102`  
`countFloorTiles()` iterates the entire grid on every call. It is called twice per step (in `step()` and implicitly via `snapshot()` → `getScore()` → `countCleanedTiles()`), plus once for the session info response. The floor tile count never changes after map generation.  
**Fix:** Cache `floorTileCount` as a `final int` computed once in the constructor.

---

### BUG-07 — `LeaderboardDataSourceConfig` mutates a shared Spring-managed bean
**File:** `LeaderboardDataSourceConfig.java:31`  
`properties.setUrl(...)` modifies the injected `DataSourceProperties` bean in-place. This bean is a shared Spring component; other auto-configuration classes (e.g., `DataSourceInitializer`) that read the same bean after this mutation may use the modified URL unexpectedly, or properties may be partially overwritten depending on initialization order.  
**Fix:** Build a `DataSourceBuilder` directly: `DataSourceBuilder.create().url(...).username(...).password(...).build()` without touching the shared properties bean.

---

### BUG-08 — No HTTP error checking in UI fetch calls
**File:** `app.js` — all `fetch(...).then(r => r.json())` calls  
If the server returns a 4xx/5xx response, `.then(r => r.json())` will either parse the error body as JSON (which may succeed or throw `SyntaxError`) with no indication that something went wrong, or silently succeed with a JSON error object. Most error paths are invisible to the user.  
**Fix:** Add `.then(r => { if (!r.ok) throw new Error(r.status + ' ' + r.statusText); return r.json(); })` before every `.then(r => r.json())`.

---

### BUG-09 — Engine exceptions return HTTP 500 instead of 400/404
**File:** `GameEngine.java:127`, `AlgoRegistry.java:43`, `GameController.java`  
`getSession()` throws `IllegalArgumentException` for unknown sessions, and `getAlgo()` does the same for unknown algo IDs. Spring maps this to a 500 Internal Server Error. Unknown resource requests should return 404; invalid input should return 400.  
**Fix:** Add a `@ControllerAdvice` that maps `IllegalArgumentException` → 404, and add `@ResponseStatus` or use `ResponseEntity` in the controllers.

---

### BUG-10 — Leaderboard allows saving a still-running session
**File:** `LeaderboardController.java:41-44`  
`POST /api/leaderboard` looks up the session by ID and saves it unconditionally, regardless of `status`. A session in `RUNNING` or `PAUSED` state gets a snapshot of its current (incomplete) score and trace saved permanently.  
**Fix:** Check `session.isActive()` and return 400 if the session has not yet finished/failed/aborted.

---

## MEDIUM

### BUG-11 — `animateRobotTo()` redraws incorrect tile combinations
**File:** `app.js:322-324`  
`[fromX, tx].forEach(x => [fromY, ty].forEach(y => ...))` generates all four combinations: `(fromX,fromY)`, `(fromX,ty)`, `(tx,fromY)`, `(tx,ty)`. For a horizontal move (same row), this redraws `(fromX, ty)` and `(tx, fromY)` — tiles that are neither the source nor the destination. Only `(fromX, fromY)` (old position) and `(tx, ty)` (new position) should be redrawn.  
**Fix:** Redraw only the two tiles: `drawTile(fromX, fromY, ...)` and `drawTile(tx, ty, ...)`.

---

### BUG-12 — `@VacuumAlgo` bean name is the display name (spaces allowed)
**File:** `VacuumAlgo.java`, `RandomAlgo.java:8`, `AlwaysLeftAlgo.java:8`  
Bean names `"Random"` and `"Always Left"` are used as API identifiers sent over HTTP. Spaces in `"Always Left"` are technically valid as Spring bean names but are unusual and could break URL-based lookups or future serialization. If a display name is ever changed, the API contract breaks. The algo `id` returned to the UI should be stable and independent of the display name.  
**Fix:** Add a separate `id()` attribute to `@VacuumAlgo` (e.g., class simple name lowercased) for the stable API key, keeping `value()` purely as the display label.

---

### BUG-13 — No validation on `StartRequest` fields
**File:** `GameController.java:58-59`  
`maxIterations` can be 0 or negative (game ends immediately); `algoId` can be `null` (NPE in `AlgoRegistry.getAlgo(null)`); `avatar` can be `null`. No `@Valid` annotations or explicit guard clauses are present.  
**Fix:** Add `@NotNull`, `@Min(1)` via Bean Validation (`jakarta.validation`) or manual guard clauses at the start of `startSession()`.

---

### BUG-14 — JPA entities returned directly from REST API
**File:** `LeaderboardController.java:36`, `LeaderboardEntry.java`  
`GET /api/leaderboard` returns the raw `@Entity` object, including any Hibernate proxy wrapping and all database fields. This couples the API contract to the persistence model. Fields like `trace` (potentially 100k chars) are always serialized even for the list view.  
**Fix:** Introduce a `LeaderboardEntryDto` without the `trace` field for the list endpoint; keep the full entity only for the `/replay` endpoint.

---

### BUG-15 — `RoomMapGenerator.hashToSeed()` has poor distribution
**File:** `RoomMapGenerator.java:57-63`  
The polynomial rolling hash `seed = seed * 31 + c` over a long starting value produces very similar seeds for strings that differ only in the last characters (e.g., `"map-1"` and `"map-2"` differ by 1 in the final character, so their seeds differ by exactly 1). For low iteration counts of `Random`, adjacent seeds can produce similar-looking maps.  
**Fix:** Use `Objects.hash(hash)` mixed with a large prime, or apply a finalizer step like `seed ^= (seed >>> 33); seed *= 0xff51afd7ed558ccdL; seed ^= (seed >>> 33);` (Murmur3-style).

---

### BUG-16 — `AlgoContractTest` does not test algos × maps in a combined parameterized test
**File:** `AlgoContractTest.java`  
Each map gets its own `@ParameterizedTest` method (`noExceptionOnSquare`, `noExceptionOnCorridor`, etc.), meaning that adding a new map requires adding a new method. A single `@MethodSource` combining both `algos()` and `maps()` with `@CsvSource` or `Arguments.of(...)` would be more maintainable and ensure every algo is tested against every map automatically.

---

## MINOR

### BUG-17 — Dead fields in `robot` object in `app.js`
**File:** `app.js:15`  
`robot = { x, y, px, py, progress }` — fields `px`, `py`, and `progress` are initialized but never read. They appear to be leftovers from a planned but unimplemented sub-pixel animation system.  
**Fix:** Remove unused fields.

---

### BUG-18 — No logging anywhere in the application
**File:** All service/engine classes  
There is no `@Slf4j` / `Logger` usage anywhere. Errors are silently swallowed (e.g., the algo exception in `GameEngine.step()` sets `FAILED` with no log), and there is no visibility into game session lifecycle, map generation, or errors.  
**Fix:** Add SLF4J logging at `WARN`/`ERROR` for exception cases and `DEBUG` for lifecycle events.

---

### BUG-19 — `GameSession` exposes `RobotAlgo` instance via public getter
**File:** `GameSession.java:19` (`@Getter` on class)  
Lombok's `@Getter` generates a public `getAlgo()` method that returns the internal algo reference. External code could call `algo.next()` directly, bypassing the exception-handling and iteration-tracking logic in `GameEngine.step()`. The algo should be package-private or accessed only via the engine.

---

### BUG-20 — `LeaderboardService.save()` is not `@Transactional`
**File:** `LeaderboardService.java:23`  
`save()` delegates directly to `repo.save()`, which is transactional by default. But if the method is ever extended to do multiple operations (e.g., updating stats), no transaction boundary is guaranteed.  
**Fix:** Add `@Transactional` to `save()` as a forward-safe precaution.

---

### BUG-21 — `GameMap` constructor stores the walls array without a defensive copy
**File:** `GameMap.java:26-34`  
The `walls` parameter is stored directly. In practice, `RoomMapGenerator` does not retain the reference after passing it, so this is safe today. But any future code that constructs a `GameMap` and then modifies the source array would silently corrupt map state.  
**Fix:** Deep-copy in the constructor: `this.walls = Arrays.stream(walls).map(boolean[]::clone).toArray(boolean[][]::new);`

---

### BUG-22 — `.db` extension stripping in `leaderboardPath` is too eager
**File:** `LeaderboardDataSourceConfig.java:30`  
`leaderboardPath.replaceAll("\\.db$", "")` strips the final `.db` suffix. H2 appends `.mv.db` itself. If the user provides `/data/scores.db`, H2 creates `/data/scores.mv.db`. If the user provides `/data/scores`, H2 creates `/data/scores.mv.db`. This is consistent. However if the user provides `/data/scores.h2.db`, the result is `/data/scores.h2` and H2 creates `/data/scores.h2.mv.db`, which is confusing. The stripping should be documented, or the user should be told to provide a path without extension.

---

### BUG-23 — `leaderboard-pane` status note is inserted before `lb-content` on every app load, but `refreshLeaderboard()` is also called on every tab switch
**File:** `app.js:462-478`, `app.js` tab switching  
`checkLeaderboardStatus()` is called once and inserts a status note before `$('lb-content')`. `refreshLeaderboard()` replaces the `innerHTML` of `$('lb-content')` only — the status note is unaffected. This is fine. However if the user navigates away and back to the leaderboard tab, `refreshLeaderboard()` is called again, which correctly re-fetches entries. No duplication. This is not a bug but worth documenting that the note won't duplicate.

---

## Summary Table

| ID | Severity | Area | One-liner |
|----|----------|------|-----------|
| BUG-01 | Critical | Algo | Singleton algo beans shared across sessions |
| BUG-02 | Critical | SSE/UI | RESUME creates a second concurrent game loop |
| BUG-03 | Critical | Concurrency | `GameSession` fields have data races |
| BUG-04 | Critical | Memory | Sessions never evicted → memory leak |
| BUG-05 | Critical | Leaderboard | Empty trace crashes replay |
| BUG-06 | High | Performance | `countFloorTiles()` is O(w×h) every step |
| BUG-07 | High | Config | `LeaderboardDataSourceConfig` mutates shared bean |
| BUG-08 | High | UI | No HTTP error checking in fetch calls |
| BUG-09 | High | API | Business exceptions return 500 instead of 400/404 |
| BUG-10 | High | Leaderboard | Mid-game session can be saved to leaderboard |
| BUG-11 | Medium | UI | Animation redraws wrong tile set |
| BUG-12 | Medium | Design | Algo bean name == display name (spaces, fragile) |
| BUG-13 | Medium | Validation | No input validation on `StartRequest` |
| BUG-14 | Medium | Design | JPA entity returned directly from REST |
| BUG-15 | Medium | Map gen | Weak hash seed distribution |
| BUG-16 | Medium | Tests | Algo contract test not fully parameterized |
| BUG-17 | Minor | UI | Dead `px/py/progress` fields in robot object |
| BUG-18 | Minor | Ops | No logging anywhere |
| BUG-19 | Minor | Design | `getAlgo()` exposes internal algo reference |
| BUG-20 | Minor | DB | `save()` missing `@Transactional` |
| BUG-21 | Minor | Design | `GameMap` constructor takes walls without defensive copy |
| BUG-22 | Minor | Config | `.db` extension stripping behavior undocumented |
| BUG-23 | Minor | UI | (Non-bug) Leaderboard status note behavior note |
