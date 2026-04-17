# Jacuum Cleaner — Bug Report

Generated after full implementation review. Sorted critical → minor.

---

## CRITICAL

### C1 — XSS: leaderboard.js injects untrusted data into innerHTML
**File:** `src/main/resources/static/js/leaderboard.js:13–27`

Leaderboard entries (`e.username`, `e.avatar`, `e.algoName`, `e.mapHash`, `e.mapSize`) are interpolated directly into an `innerHTML` template literal. Any of these fields can contain `<script>` tags or event-handler attributes, enabling stored XSS. An attacker submits a crafted entry via `POST /api/leaderboard`; every user who opens the leaderboard screen executes arbitrary JS.

Fix: Use `textContent` on created elements instead of template-literal HTML, or escape all values before interpolation.

---

### C2 — Thread safety: pause() and resume() mutate session state without synchronization
**File:** `src/main/java/com/jacuum/engine/MemorySessions.java:108–109`

```java
@Override public void pause(String id)  throws Exception { require(id).status = RunStatus.PAUSED; }
@Override public void resume(String id) throws Exception { require(id).status = RunStatus.RUNNING; }
```

`stop()` was fixed with a `synchronized (s)` guard; `pause()` and `resume()` were not. The game loop reads `s.status` on every iteration; a concurrent `pause()` call performs an unsynchronized write to a `volatile` field with no atomicity guarantee for check-then-act sequences. If the loop checks `s.status == RUNNING`, a concurrent `pause()` and then `stop()` call can result in the loop executing one extra iteration after the session is declared FINISHED.

Fix: Wrap both methods in `synchronized (s)` blocks with state validation, matching the pattern already used in `start()` and `stop()`.

---

### C3 — Thread safety: non-atomic compound operations on volatile fields in runLoop()
**File:** `src/main/java/com/jacuum/engine/MemorySessions.java:58–80`

`robotX`, `robotY`, `score`, and `iterationsUsed` are individually `volatile` but are written in sequence without atomicity. A reader on the HTTP thread calling `view()` can observe `robotX` updated while `robotY` still holds the previous value — a torn position read. Additionally `s.score += 100` is a non-atomic read-modify-write on a `volatile int`, which is safe here only because the game loop is the sole writer; the comment in the code should state this explicitly to prevent future regression.

Fix (display): Accept the torn-read for display purposes (document it). Fix (score): no action needed given single-writer, but document the assumption.

---

### C4 — Null values from request can crash instantiate()
**File:** `src/main/java/com/jacuum/web/SessionEndpoint.java:45`

`req.algoName()` is passed directly to `sessions.open()` which passes it to `algorithms.instantiate(s.algoName)`. Jackson deserializes missing JSON fields as `null` for `String` record components. A request with no `algoName` field causes `SpringAlgorithms.instantiate(null)` to call `displayName(...).equals(null)` — this silently fails to find any match and throws `Exception("Unknown algorithm: null")`. No validation at the boundary.

Fix: Validate `req.algoName()`, `req.username()`, `req.avatar()` are non-null and non-blank before proceeding; return HTTP 400 on invalid input.

---

## IMPORTANT

### I1 — game.max-sessions property declared but never enforced
**File:** `src/main/resources/application.properties:4` / `src/main/java/com/jacuum/engine/MemorySessions.java`

`game.max-sessions=50` is declared in properties but nothing reads it. Without a session cap, each `POST /api/session` call opens a new `ConcurrentHashMap` entry and spawns a virtual thread. Under load this is an unbounded memory/thread exhaustion vector.

Fix: Inject the value in `MemorySessions` and throw an exception in `open()` when the cap is reached.

---

### I2 — Static fields in JsonLeaderboard violate immutability rules
**File:** `src/main/java/com/jacuum/leaderboard/JsonLeaderboard.java:13–16`

```java
private static final ObjectMapper MAPPER = ...
private static final TypeReference<List<LeaderboardEntry>> TYPE = ...
```

Static fields are banned by the project's OOP rules. `ObjectMapper` is also mutable (`.enable(...)` modifies internal state and is called in the field initializer, which is constructor-equivalent logic). The `TypeReference` anonymous subclass is safe but the `static` field placement is a rule violation.

Fix: Construct `ObjectMapper` and `TypeReference` in the `JsonLeaderboard` constructor and store as `final` instance fields. `ObjectMapper` is thread-safe after configuration so sharing between method calls on the same instance is fine.

---

### I3 — CellularMaps has seven static methods and two static constants
**File:** `src/main/java/com/jacuum/map/CellularMaps.java:8–9, 27, 34, 42, 56, 99`

`SMOOTHING_PASSES`, `FILL_RATIO`, `seedFrom()`, `initialFloor()`, `smooth()`, `keepLargestRegion()`, `centroidOfFloor()` are all `private static`. Static methods are prohibited by the project's OOP rules.

Fix: Remove `static` from all private helpers. Move constants to constructor parameters or final instance fields, allowing different `CellularMaps` instances to use different parameters (e.g. for testing determinism with fixed seeds).

---

### I4 — Controllers and config classes don't implement interfaces
**Files:**
- `src/main/java/com/jacuum/web/SessionEndpoint.java`
- `src/main/java/com/jacuum/web/AlgosEndpoint.java`
- `src/main/java/com/jacuum/web/LeaderboardEndpoint.java`
- `src/main/java/com/jacuum/web/AppConfig.java`
- `src/main/java/com/jacuum/algo/SpringAlgorithms.java`
- `src/main/java/com/jacuum/leaderboard/JsonLeaderboard.java`
- `src/main/java/com/jacuum/leaderboard/SilentLeaderboard.java`

The OOP rules require every class to implement an interface. `JsonLeaderboard` and `SilentLeaderboard` implement `Leaderboard` ✓. `SpringAlgorithms` implements `Algorithms` ✓. The three controllers and `AppConfig` are concrete classes with no interface.

Fix: Extract interfaces for the controllers (`SessionApi`, `AlgosApi`, `LeaderboardApi`) and for `AppConfig`. Spring MVC works through the concrete class; adding an interface is purely a design discipline step.

---

### I5 — JacuumApplication is not final and implements no interface
**File:** `src/main/java/com/jacuum/JacuumApplication.java`

`public class JacuumApplication` is neither `final` nor `abstract` and implements no interface. This is a `@SpringBootApplication`-annotated entry point; Spring needs to be able to instantiate it. Making it `final` is correct and harmless; no interface is needed for entry-point classes, but the non-final declaration is technically a rule violation.

Fix: Add `final` to the class declaration.

---

### I6 — MemorySessions null-guards violate no-null-check rule
**File:** `src/main/java/com/jacuum/engine/MemorySessions.java:98, 104, 116`

```java
if (messaging != null)
    messaging.convertAndSend(...)
if (s.future != null) s.future.interrupt();
```

The project rules ban `if (x == null)` checks. These null paths exist because unit tests pass `null` for `messaging` and `future` may be unset before `stop()` is called.

Fix for `messaging`: Use a Null Object — a `NullMessagingTemplate` that implements the same interface but does nothing, injected in tests instead of `null`. Fix for `future`: Initialize `future` to a no-op `Thread` stub or check `status != RUNNING` before attempting interruption (the thread is only present when `start()` has been called).

---

### I7 — require() null check pattern
**File:** `src/main/java/com/jacuum/engine/MemorySessions.java:122`

```java
if (s == null) throw new Exception("Unknown session: " + id);
```

Despite being the intended behavior, this is a null check (`ConcurrentHashMap.get()` returns `null` for missing keys). The OOP rules prohibit this pattern; the Null Object approach or a typed `Optional`-like wrapper should be used instead.

Fix: Use `computeIfAbsent` or wrap the map value in a non-null sentinel class that throws on access.

---

### I8 — InterruptedException swallowed in runLoop
**File:** `src/main/java/com/jacuum/engine/MemorySessions.java:51`

```java
try { Thread.sleep(50); } catch (InterruptedException e) { break; }
```

The interrupt flag is cleared by catching `InterruptedException` without re-interrupting the thread (`Thread.currentThread().interrupt()`). The loop breaks, which leads to `OUT_OF_ITERATIONS` being set instead of `INTERRUPTED`. If `stop()` interrupts the thread during a pause sleep, the finish reason will be wrong.

Fix: Re-interrupt the thread (`Thread.currentThread().interrupt()`) before breaking so the loop's finally-equivalent path can distinguish interrupted-from-pause from normal termination.

---

### I9 — Missing test: LeaderboardEndpoint has no controller test
**Files:** `src/main/java/com/jacuum/web/LeaderboardEndpoint.java` (no corresponding test)

`SessionEndpoint` and `AlgosEndpoint` have MockMvc tests. `LeaderboardEndpoint` was added in Task 14 without tests. The GET and POST paths are untested at the HTTP layer.

Fix: Create `src/test/java/com/jacuum/web/LeaderboardEndpointTest.java` with at least: GET returns empty list by default; POST saves and GET retrieves.

---

### I10 — @Autowired in integration tests
**Files:**
- `src/test/java/com/jacuum/web/SessionEndpointTest.java:18–19`
- `src/test/java/com/jacuum/web/AlgosEndpointTest.java:15`

`@Autowired` is used to inject `MockMvc` and `ObjectMapper`. The global OOP rules ban `@Autowired`. Note: Spring Boot's `@SpringBootTest` requires field injection for test infrastructure since there is no constructor through which Spring can inject these.

Fix: Use `@Autowired` on a constructor in the test class (constructor injection), or accept this as a framework test-infrastructure exception and document it.

---

### I11 — SessionEndpoint null check violates no-null rule
**File:** `src/main/java/com/jacuum/web/SessionEndpoint.java:40`

```java
(req.hash() != null && !req.hash().isBlank()) ? req.hash() : UUID.randomUUID().toString()
```

Null check on a request field. Jackson deserializes absent fields as `null` for record components.

Fix: Make `hash()` return `Optional<String>` or use a custom deserializer that converts null/blank to an empty string sentinel. Or validate the DTO at the boundary and reject nulls early.

---

## MINOR

### M1 — window._lbEntries global state in leaderboard.js
**File:** `src/main/resources/static/js/leaderboard.js:28`

`window._lbEntries = sorted` is used so that inline `onclick` attribute handlers (`Leaderboard.replay(${i})`) can look up entries by index. If `load()` is called again before a replay/retry action completes, the index silently refers to different data. The `onclick` attributes are generated using interpolated indices, making the coupling invisible.

Fix: Store entries in a module-scoped closure variable (not `window`), and generate onclick handlers via `addEventListener` with the entry object captured in a closure rather than an index.

---

### M2 — Silent exception swallow in setup.js loadPrefs()
**File:** `src/main/resources/static/js/setup.js:23`

```javascript
try {
  const p = JSON.parse(localStorage.getItem(PREFS_KEY) || '{}');
  ...
} catch (_) {}
```

Parsing errors in stored preferences are silently discarded. If localStorage is corrupted the user gets no feedback and the form fields stay blank with no indication of why.

Fix: Log the error to the console at minimum (`console.warn('Failed to load preferences:', _)`).

---

### M3 — alert() used for leaderboard save confirmation
**File:** `src/main/resources/static/js/game.js:169`

`alert('Saved!')` blocks the browser's JS thread and is poor UX. Browsers on some platforms suppress repeated `alert()` calls.

Fix: Display a transient status message in the DOM (e.g. update a status `<span>` element).

---

### M4 — pendingAlgo variable declared after the function that writes it
**File:** `src/main/resources/static/js/setup.js:25–33`

`loadPrefs()` (line 21) assigns `pendingAlgo = p.algo` but `pendingAlgo` is declared with `let` at line 33, below `loadPrefs()`. JavaScript hoisting makes this work (`let` is hoisted but not initialized — however `loadPrefs` is only called after `pendingAlgo` is initialized). This ordering is fragile and confusing.

Fix: Declare `pendingAlgo` before the functions that use it.

---

### M5 — GameLoopTest uses busy-wait polling
**File:** `src/test/java/com/jacuum/engine/GameLoopTest.java:31–35`

```java
while (sessions.view(id).status() != RunStatus.FINISHED
       && System.currentTimeMillis() < deadline)
    Thread.sleep(50);
```

Busy-wait polling loops can produce intermittent failures on slow CI machines. The 2-second timeout may be insufficient under heavy load.

Fix: Use `Awaitility.await().atMost(...)` or join the virtual thread directly via the stored `Thread` reference.

---

### M6 — AlwaysLeftAlgo allocates a new Direction array on every call
**File:** `src/main/java/com/jacuum/algo/impl/AlwaysLeftAlgo.java:17`

```java
final Direction[] preference =
    {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};
```

A new 4-element array is allocated per iteration call. With 5 000 iterations per session this is 20 000 small allocations. The array content never changes.

Fix: The array is pure constant data; since static fields are prohibited by the OOP rules, the cleanest solution is to reference `Direction.values()` in the preferred order or use an immutable `List.of(...)` constant stored as a `private final` field initialized in the constructor.

---

### M7 — SpringAlgorithms.names() and instantiate() iterate the bean map twice for the same request
**File:** `src/main/java/com/jacuum/algo/SpringAlgorithms.java:21–37`

`instantiate()` calls `getBeansWithAnnotation()` which scans all beans. For each session start, this rescans the application context. With many concurrent sessions this creates redundant context queries.

Fix: Cache the `Map<String, Object>` result in a constructor-initialized `final` field since the set of available algorithms is fixed after startup.

---

### M8 — SessionEndpoint.toResponse() mixes coordinate iteration and DTO construction in one method
**File:** `src/main/java/com/jacuum/web/SessionEndpoint.java:78–105`

The method builds a full map tile snapshot with nested loops. This logic belongs in a dedicated class (e.g. `MapSnapshotFactory`) that takes a `GameMap` and returns a `MapSnapshot`. As-is, `SessionEndpoint` has two responsibilities: routing and map serialization.

Fix: Extract tile-snapshot building to a separate class implementing a `Snapshots` interface, injected into `SessionEndpoint` via constructor.

---

### M9 — game.js init() re-registers onclick handlers on every call
**File:** `src/main/resources/static/js/game.js:61–65`

Every call to `init()` (new game) re-assigns `onclick` properties on buttons. Using property assignment (`.onclick =`) replaces the previous handler, so this is technically safe — but mixing `addEventListener` for some events (speed-range `oninput`) with direct property assignment for others is inconsistent and makes event management hard to follow.

Fix: Use `addEventListener` consistently, removing old listeners before adding new ones, or extract button wiring to a dedicated `bindControls()` function.

---

### M10 — JsonLeaderboard.save() reads then writes without a lock
**File:** `src/main/java/com/jacuum/leaderboard/JsonLeaderboard.java:32–35`

```java
final List<LeaderboardEntry> current = new ArrayList<>(entries());
current.add(entry);
MAPPER.writeValue(file.toFile(), current);
```

Two concurrent `save()` calls will both read the same file, both build a list with their respective entry, and the second write will overwrite the first. This is a file-level TOCTOU race.

Fix: `synchronized` on the `JsonLeaderboard` instance, or use a file lock via `FileChannel.tryLock()`.

---

### M11 — SizePreset fallback in sizeFrom() is SMALL, not documented
**File:** `src/main/java/com/jacuum/web/SessionEndpoint.java:98–103`

Invalid or unrecognized size strings silently fall back to `SMALL`. The API gives no indication to the client that the requested size was invalid.

Fix: Return HTTP 400 for unrecognized size values, or document the fallback behavior in the API contract.
