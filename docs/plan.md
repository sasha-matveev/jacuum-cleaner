# Jacuum Cleaner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local Spring Boot coding game where users run registered smart vacuum algorithms on deterministic generated maps, watch animated cleanup, score attempts, and optionally save/replay leaderboard entries.

**Architecture:** The server owns domain rules, map generation, algorithm execution, scoring, traces, and persistence. The browser UI is static HTML/CSS/JavaScript served by Spring Boot and calls JSON endpoints for all state changes. Core modules stay independent: domain, algo, engine, mapgen, leaderboard, and web.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Maven Wrapper, Spring MVC, Jackson, Datafaker 2.5.4, JUnit 5, AssertJ, plain browser JavaScript.

---

## File Structure

Create this structure during implementation:

```text
pom.xml
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
src/main/java/dev/ytype/jacuum/JacuumCleanerApplication.java
src/main/java/dev/ytype/jacuum/domain/*.java
src/main/java/dev/ytype/jacuum/algo/*.java
src/main/java/dev/ytype/jacuum/engine/*.java
src/main/java/dev/ytype/jacuum/mapgen/*.java
src/main/java/dev/ytype/jacuum/leaderboard/*.java
src/main/java/dev/ytype/jacuum/web/*.java
src/main/resources/application.properties
src/main/resources/static/index.html
src/main/resources/static/styles.css
src/main/resources/static/app.js
src/test/java/dev/ytype/jacuum/**/*.java
docs/progress.md
```

Package responsibilities:

- `domain`: Java records/enums for `Coordinate`, `Direction`, `TileView`, `RoomMap`, `SizePreset`, `TraceStep`, `RunStatus`, `RunResult`, and scoring value objects.
- `algo`: `RobotAlgorithm`, metadata annotations, registry, and sample algorithms.
- `engine`: `RunEngine`, run sessions, score calculation, trace generation, and algorithm failure handling.
- `mapgen`: deterministic map creation and reachability checks.
- `leaderboard`: optional JSON file storage.
- `web`: Spring controllers and DTOs.

---

## Phase 1: Spring Skeleton And Domain Foundation

Value: the project builds, tests run, and core value objects exist.

### Task 1.1: Scaffold Maven Spring Boot Project

**Files:**

- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `src/main/java/dev/ytype/jacuum/JacuumCleanerApplication.java`
- Create: `src/main/resources/application.properties`
- Modify: `docs/progress.md`

- [x] **Step 1: Create Maven project files**

Use Spring Boot `4.0.5`, Java `21`, and dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
  </dependency>
  <dependency>
    <groupId>net.datafaker</groupId>
    <artifactId>datafaker</artifactId>
    <version>2.5.4</version>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

- [x] **Step 2: Add application entry point**

```java
package dev.ytype.jacuum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VacuumCleanerApplication {
    public static void main(String[] args) {
        SpringApplication.run(VacuumCleanerApplication.class, args);
    }
}
```

- [x] **Step 3: Run tests**

Run: `.\mvnw test`

Expected: build succeeds with no tests or one generated context test if added.

- [x] **Step 4: Update progress log**

Append a Phase 1 entry to `docs/progress.md` with scaffold status and command output summary.

- [x] **Step 5: Commit**

```powershell
git add pom.xml mvnw mvnw.cmd .mvn src/main docs/progress.md
git commit -m "chore: scaffold spring boot project"
```

### Task 1.2: Add Domain Records And Unit Tests

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/domain/Direction.java`
- Create: `src/main/java/dev/ytype/jacuum/domain/Coordinate.java`
- Create: `src/main/java/dev/ytype/jacuum/domain/SizePreset.java`
- Create: `src/main/java/dev/ytype/jacuum/domain/RunStatus.java`
- Create: `src/main/java/dev/ytype/jacuum/domain/TraceStep.java`
- Create: `src/main/java/dev/ytype/jacuum/domain/TileView.java`
- Create: `src/main/java/dev/ytype/jacuum/domain/RoomMap.java`
- Create: `src/test/java/dev/ytype/jacuum/domain/DomainModelTest.java`
- Modify: `docs/progress.md`

- [x] **Step 1: Write failing domain tests**

Test exact behavior:

```java
@Test
void coordinateMovesByDirection() {
    Coordinate origin = new Coordinate(3, 4);
    assertThat(origin.move(Direction.UP)).isEqualTo(new Coordinate(3, 3));
    assertThat(origin.move(Direction.RIGHT)).isEqualTo(new Coordinate(4, 4));
    assertThat(origin.move(Direction.DOWN)).isEqualTo(new Coordinate(3, 5));
    assertThat(origin.move(Direction.LEFT)).isEqualTo(new Coordinate(2, 4));
}

@Test
void sizePresetExposesStableDefaults() {
    assertThat(SizePreset.TINY.iterationDefault()).isLessThan(SizePreset.LARGE.iterationDefault());
    assertThat(SizePreset.MEDIUM.width()).isGreaterThan(SizePreset.SMALL.width());
}
```

- [x] **Step 2: Run test to verify failure**

Run: `.\mvnw -Dtest=DomainModelTest test`

Expected: compilation fails because domain classes do not exist.

- [x] **Step 3: Implement minimal domain types**

Use immutable records and enums. `RoomMap` must expose `isFloor(Coordinate)`, `isWall(Coordinate)`, `hasWall(Coordinate, Direction)`, and `reachableFloorCount()`.

- [x] **Step 4: Run test to verify pass**

Run: `.\mvnw -Dtest=DomainModelTest test`

Expected: PASS.

- [x] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/domain src/test/java/dev/ytype/jacuum/domain docs/progress.md
git commit -m "feat: add core domain model"
```

---

## Phase 2: Deterministic Map Generation

Value: users and tests can generate repeatable reachable rooms from hash and size preset.

### Task 2.1: Implement Map Generator

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/mapgen/GeneratedMap.java`
- Create: `src/main/java/dev/ytype/jacuum/mapgen/MapGenerator.java`
- Create: `src/main/java/dev/ytype/jacuum/mapgen/MapValidator.java`
- Create: `src/test/java/dev/ytype/jacuum/mapgen/MapGeneratorTest.java`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing tests**

Cover:

- same hash and preset produce equal `RoomMap`
- different hashes usually produce different layouts
- all floor tiles are reachable from start
- every outside boundary behaves as a wall
- single `TINY` map has at least one floor tile

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=MapGeneratorTest test`

Expected: compilation failure.

- [ ] **Step 3: Implement generator**

Use a deterministic `java.util.Random` seeded from SHA-256 of `hash + ":" + preset.name()`. Generate a rectangular room per preset, carve an irregular but connected floor area, add bounded internal obstacles only if reachability remains valid, and choose a deterministic start floor tile.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=MapGeneratorTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/mapgen src/test/java/dev/ytype/jacuum/mapgen docs/progress.md
git commit -m "feat: generate deterministic reachable maps"
```

### Task 2.2: Add Map API

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/web/MapController.java`
- Create: `src/main/java/dev/ytype/jacuum/web/CreateMapRequest.java`
- Create: `src/main/java/dev/ytype/jacuum/web/MapResponse.java`
- Create: `src/test/java/dev/ytype/jacuum/web/MapControllerTest.java`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing controller tests**

Test `POST /api/maps` with explicit hash and `small` preset returns the same JSON on repeated calls. Test request without hash returns a non-blank generated hash.

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=MapControllerTest test`

Expected: failure because controller does not exist.

- [ ] **Step 3: Implement controller and DTOs**

Expose:

```http
POST /api/maps
Content-Type: application/json

{"hash":"demo","size":"small"}
```

Return hash, size, width, height, start coordinate, and tile matrix.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=MapControllerTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/web src/test/java/dev/ytype/jacuum/web docs/progress.md
git commit -m "feat: expose deterministic map api"
```

---

## Phase 3: Algorithms And Simulation Engine

Value: registered algorithms can run against maps with validated movement, score, status, and trace.

### Task 3.1: Add Algorithm API And Registry

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/algo/RobotAlgorithm.java`
- Create: `src/main/java/dev/ytype/jacuum/algo/RobotAlgo.java`
- Create: `src/main/java/dev/ytype/jacuum/algo/AlgorithmDescriptor.java`
- Create: `src/main/java/dev/ytype/jacuum/algo/AlgorithmRegistry.java`
- Create: `src/main/java/dev/ytype/jacuum/algo/RandomWalkAlgorithm.java`
- Create: `src/main/java/dev/ytype/jacuum/algo/AlwaysLeftAlgorithm.java`
- Create: `src/main/java/dev/ytype/jacuum/algo/WallFollowerAlgorithm.java`
- Create: `src/test/java/dev/ytype/jacuum/algo/AlgorithmRegistryTest.java`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing registry tests**

Verify registered sample algorithm ids include `random-walk`, `always-left`, and `wall-follower`. Verify each algorithm can be created as a fresh instance.

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=AlgorithmRegistryTest test`

Expected: failure.

- [ ] **Step 3: Implement API**

Document the interface:

```java
/**
 * Produces the next virtual move for a robot.
 * Implementations may keep per-run state. The engine creates a fresh instance per run.
 */
public interface RobotAlgorithm {
    Direction next(TileView tile);
}
```

Use `@RobotAlgo(id = "...", name = "...", description = "...")` on Spring components and have `AlgorithmRegistry` expose descriptors plus factory access.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=AlgorithmRegistryTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/algo src/test/java/dev/ytype/jacuum/algo docs/progress.md
git commit -m "feat: register robot algorithms"
```

### Task 3.2: Implement Run Engine

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/engine/RunEngine.java`
- Create: `src/main/java/dev/ytype/jacuum/engine/RunRequest.java`
- Create: `src/main/java/dev/ytype/jacuum/engine/RunSession.java`
- Create: `src/main/java/dev/ytype/jacuum/engine/ScoreCalculator.java`
- Create: `src/test/java/dev/ytype/jacuum/engine/RunEngineTest.java`
- Create: `src/test/java/dev/ytype/jacuum/engine/SampleAlgorithmSmokeTest.java`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing engine tests**

Cover:

- start tile is cleaned immediately
- blocked movement consumes one iteration
- successful movement cleans dirty destination
- score equals `cleanedTiles * 1000 - iterationsUsed`
- all-clean map completes before limit
- algorithm exception ends with `FAILED` and zero score
- trace captures requested direction and resulting coordinate

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=RunEngineTest,SampleAlgorithmSmokeTest test`

Expected: failure.

- [ ] **Step 3: Implement engine**

Keep engine independent from controllers and leaderboard. `RunSession.step(int count)` advances virtual iterations and returns a `RunResult`. If an algorithm throws, catch it, set status `FAILED`, clear score to zero, and stop.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=RunEngineTest,SampleAlgorithmSmokeTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/engine src/test/java/dev/ytype/jacuum/engine docs/progress.md
git commit -m "feat: simulate robot cleanup runs"
```

---

## Phase 4: Run And Algorithm Web APIs

Value: the UI can list algorithms, start runs, step runs, and stop runs through HTTP.

### Task 4.1: Add Algorithm And Run Controllers

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/web/AlgorithmController.java`
- Create: `src/main/java/dev/ytype/jacuum/web/RunController.java`
- Create: `src/main/java/dev/ytype/jacuum/web/RunStore.java`
- Create: `src/main/java/dev/ytype/jacuum/web/RunDtos.java`
- Create: `src/test/java/dev/ytype/jacuum/web/RunControllerTest.java`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing web tests**

Verify:

- `GET /api/algorithms` returns sample descriptors
- `POST /api/runs` creates a run with map hash, size, algorithm id, username, avatar, and iteration limit
- `POST /api/runs/{id}/steps` advances the run
- `POST /api/runs/{id}/stop` marks an active run as interrupted
- unknown algorithm returns HTTP 400

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=RunControllerTest test`

Expected: failure.

- [ ] **Step 3: Implement controllers**

Use an in-memory `RunStore` keyed by UUID for local sessions. This is acceptable because saved leaderboard traces provide persistence; active runs do not need restart recovery.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=RunControllerTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/web src/test/java/dev/ytype/jacuum/web docs/progress.md
git commit -m "feat: expose run simulation api"
```

---

## Phase 5: Thin Browser UI

Value: a user can configure a scene, run an algorithm, watch smooth movement, pause/resume, stop, and see score.

### Task 5.1: Build Static UI

**Files:**

- Create: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/styles.css`
- Create: `src/main/resources/static/app.js`
- Create: `src/test/java/dev/ytype/jacuum/web/StaticUiTest.java`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing static asset test**

Verify `GET /` returns HTML and static resources are served.

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=StaticUiTest test`

Expected: failure until assets exist.

- [ ] **Step 3: Implement UI**

UI controls:

- username with generated default action
- avatar selector
- hash input and random hash button
- size preset selector
- snapped iteration range
- algorithm selector
- run, pause, resume, stop, faster, slower
- map grid
- run stats

Use `localStorage` keys prefixed with `jacuum.`.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=StaticUiTest test`

Expected: PASS.

- [ ] **Step 5: Run app manually**

Run: `.\mvnw spring-boot:run`

Expected: `http://localhost:8080/` opens the playable UI.

- [ ] **Step 6: Commit**

```powershell
git add src/main/resources/static src/test/java/dev/ytype/jacuum/web docs/progress.md
git commit -m "feat: add playable browser ui"
```

---

## Phase 6: Optional Leaderboard And Replay

Value: completed runs can be saved locally, replayed from trace, and retried with the same setup.

### Task 6.1: Add Leaderboard Persistence

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/leaderboard/LeaderboardEntry.java`
- Create: `src/main/java/dev/ytype/jacuum/leaderboard/LeaderboardProperties.java`
- Create: `src/main/java/dev/ytype/jacuum/leaderboard/LeaderboardRepository.java`
- Create: `src/main/java/dev/ytype/jacuum/leaderboard/JsonLeaderboardRepository.java`
- Create: `src/main/java/dev/ytype/jacuum/leaderboard/NoopLeaderboardRepository.java`
- Create: `src/test/java/dev/ytype/jacuum/leaderboard/LeaderboardRepositoryTest.java`
- Modify: `src/main/resources/application.properties`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing persistence tests**

Verify configured temp file saves and loads entries. Verify absent path uses no-op repository and does not fail. Verify default `jacuum-leaderboard.json` is used when present in the working directory.

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=LeaderboardRepositoryTest test`

Expected: failure.

- [ ] **Step 3: Implement repository**

Use Jackson to store an array of entries in JSON. Write atomically by serializing to a temporary file in the same directory and moving it into place.

- [ ] **Step 4: Run focused test**

Run: `.\mvnw -Dtest=LeaderboardRepositoryTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/leaderboard src/test/java/dev/ytype/jacuum/leaderboard src/main/resources/application.properties docs/progress.md
git commit -m "feat: persist optional local leaderboard"
```

### Task 6.2: Add Leaderboard API And UI Integration

**Files:**

- Create: `src/main/java/dev/ytype/jacuum/web/LeaderboardController.java`
- Create: `src/test/java/dev/ytype/jacuum/web/LeaderboardControllerTest.java`
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/styles.css`
- Modify: `src/main/resources/static/app.js`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write failing API tests**

Verify:

- `GET /api/leaderboard` returns rows or empty list
- `POST /api/leaderboard` saves a completed non-failed run
- failed runs are rejected with HTTP 400
- `GET /api/leaderboard/{id}/replay` returns trace data

- [ ] **Step 2: Run focused test**

Run: `.\mvnw -Dtest=LeaderboardControllerTest test`

Expected: failure.

- [ ] **Step 3: Implement controller and UI**

Add save score, leaderboard list, replay, and retry actions. Replay must animate from saved trace and must not call algorithm stepping endpoints.

- [ ] **Step 4: Run focused tests**

Run: `.\mvnw -Dtest=LeaderboardControllerTest,StaticUiTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/ytype/jacuum/web src/test/java/dev/ytype/jacuum/web src/main/resources/static docs/progress.md
git commit -m "feat: add leaderboard replay and retry"
```

---

## Phase 7: Final Verification And Documentation

Value: the app is documented, fully tested, and ready to run locally.

### Task 7.1: Add User Documentation

**Files:**

- Create: `README.md`
- Modify: `docs/progress.md`

- [ ] **Step 1: Write README**

Include:

- purpose
- requirements
- run command
- test command
- leaderboard file configuration
- algorithm extension example
- scoring formula

- [ ] **Step 2: Verify commands**

Run: `.\mvnw test`

Expected: PASS.

Run: `.\mvnw spring-boot:run`

Expected: app starts at `http://localhost:8080/`.

- [ ] **Step 3: Commit**

```powershell
git add README.md docs/progress.md
git commit -m "docs: explain local game usage"
```

### Task 7.2: Final Product Pass

**Files:**

- Modify: files required by verification fixes
- Modify: `docs/progress.md`

- [ ] **Step 1: Run full test suite**

Run: `.\mvnw test`

Expected: PASS.

- [ ] **Step 2: Run app and manually verify main scenario**

Run: `.\mvnw spring-boot:run`

Verify:

- generate map
- enter or generate username
- select avatar
- select algorithm
- run, pause, resume, speed up, slow down, stop
- completed run displays score
- optional leaderboard save works when configured
- replay does not call algorithm stepping
- retry uses saved map hash, size, and iteration limit

- [ ] **Step 3: Update progress log**

Record final test command, manual verification summary, and any known limitations.

- [ ] **Step 4: Commit**

```powershell
git add docs/progress.md
git add <verification-fix-files-if-any>
git commit -m "chore: verify jacuum cleaner mvp"
```

---

## Safepoint Rules

After every phase:

- update `docs/progress.md`
- run the focused tests for the phase
- run `.\mvnw test` if the phase changes shared behavior
- commit with the planned message or a more accurate equivalent

If interrupted, resume by checking:

```powershell
git status --short
Get-Content .\docs\progress.md
Select-String -Path .\docs\plan.md -Pattern "- \[ \]|- \[x\]"
```

## Plan Self-Review

Spec coverage:

- deterministic maps: Phase 2
- algorithm interface and registration: Phase 3
- simulation, scoring, trace, exception failure: Phase 3
- JSON APIs: Phases 2, 4, and 6
- thin browser UI and animation controls: Phase 5
- local preferences: Phase 5
- optional leaderboard persistence, replay, retry: Phase 6
- AI project environment and progress documentation: existing files plus all phases
- tests for infrastructure and core behavior: all phases
- commits after valuable phases: every task includes a commit step

Placeholder scan:

- No unresolved placeholder markers or unanswered decisions.

Type consistency:

- Shared names use the approved spec terms: `Direction`, `TileView`, `RoomMap`, `SizePreset`, `TraceStep`, `RunStatus`, `RunResult`, `RobotAlgorithm`, and `AlgorithmRegistry`.
