# Jacuum Cleaner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local Spring Boot coding-game where players implement a Java vacuum-robot algorithm that cleans a procedurally generated map, with real-time WebSocket animation in a browser UI.

**Architecture:** Spring Boot 3.x serves both a vanilla-JS SPA and a REST+WebSocket API on one port. The game engine runs each session's iteration loop in a virtual thread, streaming `IterationEvent` records to the client via STOMP over SockJS. Map generation uses seeded cellular automata; leaderboard persists to an optional JSON file.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Maven, spring-boot-starter-websocket, javafaker 1.0.2, JUnit 5 + MockMvc, Vanilla JS + HTML Canvas, SockJS + STOMP.js.

---

## File Map

```
pom.xml
src/main/java/com/jacuum/
  JacuumApplication.java
  algo/
    Direction.java              enum: NORTH SOUTH EAST WEST + dx/dy/opposite
    Tile.java                   interface: x() y() isClean() hasWall(Direction)
    RobotAlgo.java              interface: Direction next(Tile) throws Exception
    RobotAlgorithm.java         annotation: @Component + value()
    Algorithms.java             interface: list(), instantiate(name)
    SpringAlgorithms.java       impl: scans @RobotAlgorithm beans
    impl/
      RandomAlgo.java
      AlwaysLeftAlgo.java
  map/
    SizePreset.java             enum: TINY SMALL MEDIUM LARGE
    GameMap.java                interface: hash width height isFloor hasWall startX startY totalFloorTiles
    GeneratedMap.java           final impl: boolean[][] floor grid
    Maps.java                   interface: GameMap generate(hash, size)
    CellularMaps.java           final impl: seeded cellular automata
  engine/
    RunStatus.java              enum: RUNNING PAUSED STOPPING
    FinishReason.java           enum: COMPLETED OUT_OF_ITERATIONS ALGO_CRASH INTERRUPTED
    IterationEvent.java         record: iter dir x y score totalCleaned totalFloor finished finishReason
    StatusEvent.java            record: sessionId status finishReason
    SessionView.java            record: id status robotX robotY score totalCleaned iterationsUsed iterationsAvailable
    SessionTile.java            final Tile impl used during simulation
    ActiveSession.java          mutable internal state (package-private)
    Sessions.java               interface: open start pause resume stop view
    MemorySessions.java         ConcurrentHashMap + virtual thread per session
  web/
    WebSocketConfig.java        @Configuration: /ws endpoint, /topic prefix
    AppConfig.java              @Configuration: beans (Maps, Algorithms, Sessions, Leaderboard)
    SessionEndpoint.java        @RestController: /api/session/**
    AlgosEndpoint.java          @RestController: /api/algos, /api/avatars
    LeaderboardEndpoint.java    @RestController: /api/leaderboard
    dto/
      CreateSessionRequest.java record
      SessionResponse.java      record
      MapSnapshot.java          record (+ TileSnapshot inner record)
  leaderboard/
    TraceEvent.java             record
    LeaderboardEntry.java       record
    Leaderboard.java            interface: entries() save(entry)
    JsonLeaderboard.java        final impl: reads/writes JSON file
    SilentLeaderboard.java      final impl: no-op

src/main/resources/
  application.properties
  static/
    index.html
    css/game.css
    js/api.js
    js/setup.js
    js/game.js
    js/leaderboard.js
    js/app.js
    lib/sockjs.min.js
    lib/stomp.min.js

src/test/java/com/jacuum/
  map/CellularMapsTest.java
  map/GeneratedMapTest.java
  engine/MemorySessionsTest.java
  algo/AlgoSmokeTest.java
  leaderboard/JsonLeaderboardTest.java
  web/SessionEndpointTest.java
  web/AlgosEndpointTest.java
```

---

## Phase 1 — Project Scaffold

### Task 1: pom.xml + main class + properties

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/jacuum/JacuumApplication.java`
- Create: `src/main/resources/application.properties`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>
  <groupId>com.jacuum</groupId>
  <artifactId>jacuum-cleaner</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>21</java.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
      <groupId>com.github.javafaker</groupId>
      <artifactId>javafaker</artifactId>
      <version>1.0.2</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Create JacuumApplication.java**

```java
package com.jacuum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JacuumApplication {
    public static void main(String[] args) {
        SpringApplication.run(JacuumApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.properties**

```properties
server.port=8080
leaderboard.file=
game.default-iterations=500
game.max-sessions=50
```

- [ ] **Step 4: Verify build compiles and starts**

```bash
./mvnw spring-boot:run
```
Expected: `Started JacuumApplication` in log, port 8080 listening.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/jacuum/JacuumApplication.java src/main/resources/application.properties
git commit -m "feat: spring boot scaffold with web + websocket dependencies"
```

---

## Phase 2 — Domain Model

### Task 2: Direction enum

**Files:**
- Create: `src/main/java/com/jacuum/algo/Direction.java`
- Create: `src/test/java/com/jacuum/algo/DirectionTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.jacuum.algo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DirectionTest {
    @Test void oppositeOfNorthIsSouth() {
        assertThat(Direction.NORTH.opposite()).isEqualTo(Direction.SOUTH);
    }
    @Test void dxOfEastIsOne() {
        assertThat(Direction.EAST.dx()).isEqualTo(1);
    }
    @Test void dyOfSouthIsOne() {
        assertThat(Direction.SOUTH.dy()).isEqualTo(1);
    }
    @Test void allDirectionsHaveUniqueOffsets() {
        for (Direction d : Direction.values()) {
            assertThat(d.dx() * d.dx() + d.dy() * d.dy()).isEqualTo(1);
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./mvnw test -Dtest=DirectionTest
```
Expected: FAIL — `Direction` does not exist.

- [ ] **Step 3: Implement Direction**

```java
package com.jacuum.algo;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST  -> WEST;
            case WEST  -> EAST;
        };
    }

    public int dx() {
        return switch (this) {
            case EAST  ->  1;
            case WEST  -> -1;
            default    ->  0;
        };
    }

    public int dy() {
        return switch (this) {
            case SOUTH ->  1;
            case NORTH -> -1;
            default    ->  0;
        };
    }
}
```

- [ ] **Step 4: Run tests — must pass**

```bash
./mvnw test -Dtest=DirectionTest
```
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: Direction enum with dx/dy/opposite"
```

---

### Task 3: Tile interface + RobotAlgo interface + annotation

**Files:**
- Create: `src/main/java/com/jacuum/algo/Tile.java`
- Create: `src/main/java/com/jacuum/algo/RobotAlgo.java`
- Create: `src/main/java/com/jacuum/algo/RobotAlgorithm.java`

- [ ] **Step 1: Create Tile interface**

```java
package com.jacuum.algo;

/**
 * Read-only view of the robot's current position on the map.
 * Passed to {@link RobotAlgo#next(Tile)} on every iteration.
 */
public interface Tile {
    /** Grid column (0-based, left to right). */
    int x();
    /** Grid row (0-based, top to bottom). */
    int y();
    /** True if this tile has already been cleaned in this session. */
    boolean isClean();
    /** True if moving in the given direction from this tile is blocked by a wall. */
    boolean hasWall(Direction direction);
}
```

- [ ] **Step 2: Create RobotAlgo interface**

```java
package com.jacuum.algo;

/**
 * Implement this interface and annotate your class with {@link RobotAlgorithm}
 * to register it as a selectable cleaning algorithm.
 *
 * <p>The engine calls {@link #next(Tile)} once per iteration.
 * Return the {@link Direction} the robot should attempt to move.
 * If a wall blocks the move, the robot stays in place (iteration is still consumed).
 *
 * <p>Throwing any exception is treated as an immediate unsuccessful finish (score = 0).
 * Implementations may be stateful — the engine creates one fresh instance per session.
 */
public interface RobotAlgo {
    Direction next(Tile tile) throws Exception;
}
```

- [ ] **Step 3: Create RobotAlgorithm annotation**

```java
package com.jacuum.algo;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/**
 * Marks a {@link RobotAlgo} implementation as auto-discoverable.
 * The {@code value} is the display name shown in the UI.
 * If omitted, the simple class name is used.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RobotAlgorithm {
    String value() default "";
}
```

- [ ] **Step 4: Verify compilation**

```bash
./mvnw compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: Tile interface, RobotAlgo interface, @RobotAlgorithm annotation"
```

---

## Phase 3 — Map Generation

### Task 4: SizePreset + GameMap interface + GeneratedMap

**Files:**
- Create: `src/main/java/com/jacuum/map/SizePreset.java`
- Create: `src/main/java/com/jacuum/map/GameMap.java`
- Create: `src/main/java/com/jacuum/map/GeneratedMap.java`
- Create: `src/test/java/com/jacuum/map/GeneratedMapTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.jacuum.map;

import com.jacuum.algo.Direction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GeneratedMapTest {

    private static boolean[][] smallFloor() {
        // 5x5 all-floor interior, walls on border — manually constructed
        boolean[][] f = new boolean[5][5];
        for (int y = 1; y < 4; y++)
            for (int x = 1; x < 4; x++)
                f[y][x] = true;
        return f;
    }

    @Test void hashIsPreserved() {
        GameMap map = new GeneratedMap("myhash", SizePreset.TINY, smallFloor(), 2, 2);
        assertThat(map.hash()).isEqualTo("myhash");
    }

    @Test void totalFloorCountsOnlyTruecells() {
        GameMap map = new GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2);
        assertThat(map.totalFloorTiles()).isEqualTo(9); // 3x3 interior
    }

    @Test void borderIsWall() {
        GameMap map = new GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2);
        // tile (1,1) has NORTH wall because (1,0) is not floor
        assertThat(map.hasWall(1, 1, Direction.NORTH)).isTrue();
        // tile (2,2) is interior — no walls expected
        assertThat(map.hasWall(2, 2, Direction.NORTH)).isFalse();
    }

    @Test void startTileIsFloor() {
        GameMap map = new GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2);
        assertThat(map.isFloor(map.startX(), map.startY())).isTrue();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./mvnw test -Dtest=GeneratedMapTest
```
Expected: FAIL — classes not found.

- [ ] **Step 3: Create SizePreset**

```java
package com.jacuum.map;

public enum SizePreset {
    TINY(10, 8),
    SMALL(16, 12),
    MEDIUM(24, 18),
    LARGE(34, 26);

    private final int width;
    private final int height;

    SizePreset(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width()  { return width; }
    public int height() { return height; }
}
```

- [ ] **Step 4: Create GameMap interface**

```java
package com.jacuum.map;

import com.jacuum.algo.Direction;

public interface GameMap {
    String hash();
    SizePreset size();
    int width();
    int height();
    boolean isFloor(int x, int y);
    boolean hasWall(int x, int y, Direction direction);
    int startX();
    int startY();
    int totalFloorTiles();
}
```

- [ ] **Step 5: Create GeneratedMap**

```java
package com.jacuum.map;

import com.jacuum.algo.Direction;

public final class GeneratedMap implements GameMap {

    private final String hash;
    private final SizePreset size;
    private final boolean[][] floor; // [y][x]
    private final int startX;
    private final int startY;
    private final int totalFloor;

    public GeneratedMap(String hash, SizePreset size, boolean[][] floor, int startX, int startY) {
        this.hash   = hash;
        this.size   = size;
        this.floor  = floor;
        this.startX = startX;
        this.startY = startY;
        int count = 0;
        for (boolean[] row : floor)
            for (boolean cell : row)
                if (cell) count++;
        this.totalFloor = count;
    }

    @Override public String hash()          { return hash; }
    @Override public SizePreset size()      { return size; }
    @Override public int width()            { return floor[0].length; }
    @Override public int height()           { return floor.length; }
    @Override public boolean isFloor(int x, int y) { return inBounds(x, y) && floor[y][x]; }
    @Override public int startX()           { return startX; }
    @Override public int startY()           { return startY; }
    @Override public int totalFloorTiles()  { return totalFloor; }

    @Override
    public boolean hasWall(int x, int y, Direction direction) {
        int nx = x + direction.dx();
        int ny = y + direction.dy();
        return !inBounds(nx, ny) || !floor[ny][nx];
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height();
    }
}
```

- [ ] **Step 6: Run tests — must pass**

```bash
./mvnw test -Dtest=GeneratedMapTest
```
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: SizePreset, GameMap interface, GeneratedMap implementation"
```

---

### Task 5: Maps interface + CellularMaps generator

**Files:**
- Create: `src/main/java/com/jacuum/map/Maps.java`
- Create: `src/main/java/com/jacuum/map/CellularMaps.java`
- Create: `src/test/java/com/jacuum/map/CellularMapsTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.jacuum.map;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CellularMapsTest {

    private final Maps maps = new CellularMaps();

    @Test void sameHashProducesSameMap() throws Exception {
        GameMap a = maps.generate("seed42", SizePreset.TINY);
        GameMap b = maps.generate("seed42", SizePreset.TINY);
        assertThat(a.totalFloorTiles()).isEqualTo(b.totalFloorTiles());
        assertThat(a.startX()).isEqualTo(b.startX());
        assertThat(a.startY()).isEqualTo(b.startY());
        for (int y = 0; y < a.height(); y++)
            for (int x = 0; x < a.width(); x++)
                assertThat(a.isFloor(x, y)).isEqualTo(b.isFloor(x, y));
    }

    @Test void differentHashesDifferentMaps() throws Exception {
        GameMap a = maps.generate("hashA", SizePreset.SMALL);
        GameMap b = maps.generate("hashB", SizePreset.SMALL);
        // Very unlikely to be identical
        boolean differs = false;
        outer:
        for (int y = 0; y < a.height(); y++)
            for (int x = 0; x < a.width(); x++)
                if (a.isFloor(x, y) != b.isFloor(x, y)) { differs = true; break outer; }
        assertThat(differs).isTrue();
    }

    @Test void startTileIsFloor() throws Exception {
        GameMap map = maps.generate("test", SizePreset.SMALL);
        assertThat(map.isFloor(map.startX(), map.startY())).isTrue();
    }

    @Test void atLeastTwentyPercentFloor() throws Exception {
        GameMap map = maps.generate("coverage", SizePreset.MEDIUM);
        int total = map.width() * map.height();
        assertThat(map.totalFloorTiles()).isGreaterThan(total / 5);
    }

    @Test void allFloorTilesReachableFromStart() throws Exception {
        GameMap map = maps.generate("reach", SizePreset.SMALL);
        // BFS from start tile
        boolean[][] visited = new boolean[map.height()][map.width()];
        java.util.Queue<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{map.startX(), map.startY()});
        visited[map.startY()][map.startX()] = true;
        int reachable = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            reachable++;
            for (com.jacuum.algo.Direction d : com.jacuum.algo.Direction.values()) {
                int nx = cur[0] + d.dx(), ny = cur[1] + d.dy();
                if (map.isFloor(nx, ny) && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        assertThat(reachable).isEqualTo(map.totalFloorTiles());
    }
}
```

- [ ] **Step 2: Run to confirm failures**

```bash
./mvnw test -Dtest=CellularMapsTest
```
Expected: FAIL — `Maps` and `CellularMaps` not found.

- [ ] **Step 3: Create Maps interface**

```java
package com.jacuum.map;

public interface Maps {
    GameMap generate(String hash, SizePreset size) throws Exception;
}
```

- [ ] **Step 4: Create CellularMaps**

```java
package com.jacuum.map;

import com.jacuum.algo.Direction;
import java.util.*;

public final class CellularMaps implements Maps {

    private static final int SMOOTHING_PASSES = 5;
    private static final double FILL_RATIO    = 0.45;

    @Override
    public GameMap generate(String hash, SizePreset size) throws Exception {
        long seed = seedFrom(hash);
        Random rng = new Random(seed);
        int w = size.width(), h = size.height();

        boolean[][] floor = initialFloor(rng, w, h);
        for (int i = 0; i < SMOOTHING_PASSES; i++)
            floor = smooth(floor, w, h);

        floor = keepLargestRegion(floor, w, h);

        int[] start = centroidOfFloor(floor, w, h);
        return new GeneratedMap(hash, size, floor, start[0], start[1]);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static long seedFrom(String hash) {
        long h = 0xcbf29ce484222325L;
        for (char c : hash.toCharArray())
            h = (h ^ c) * 0x100000001b3L;
        return h;
    }

    private static boolean[][] initialFloor(Random rng, int w, int h) {
        boolean[][] f = new boolean[h][w];
        for (int y = 1; y < h - 1; y++)
            for (int x = 1; x < w - 1; x++)
                f[y][x] = rng.nextDouble() > FILL_RATIO;
        return f;
    }

    private static boolean[][] smooth(boolean[][] f, int w, int h) {
        boolean[][] next = new boolean[h][w];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int walls = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                        if (!f[y + dy][x + dx]) walls++;
                next[y][x] = walls < 5;
            }
        }
        return next;
    }

    private static boolean[][] keepLargestRegion(boolean[][] floor, int w, int h) {
        boolean[][] visited = new boolean[h][w];
        List<List<int[]>> regions = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (floor[y][x] && !visited[y][x]) {
                    List<int[]> region = new ArrayList<>();
                    Queue<int[]> q = new ArrayDeque<>();
                    q.add(new int[]{x, y});
                    visited[y][x] = true;
                    while (!q.isEmpty()) {
                        int[] cur = q.poll();
                        region.add(cur);
                        for (Direction d : Direction.values()) {
                            int nx = cur[0] + d.dx(), ny = cur[1] + d.dy();
                            if (nx >= 0 && ny >= 0 && nx < w && ny < h
                                && floor[ny][nx] && !visited[ny][nx]) {
                                visited[ny][nx] = true;
                                q.add(new int[]{nx, ny});
                            }
                        }
                    }
                    regions.add(region);
                }
            }
        }
        if (regions.isEmpty()) {
            // Fallback: open 3x3 centre
            boolean[][] fallback = new boolean[h][w];
            int cx = w / 2, cy = h / 2;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++)
                    fallback[cy + dy][cx + dx] = true;
            return fallback;
        }
        List<int[]> largest = regions.stream()
            .max(Comparator.comparingInt(List::size)).orElseThrow();
        boolean[][] result = new boolean[h][w];
        for (int[] cell : largest)
            result[cell[1]][cell[0]] = true;
        return result;
    }

    private static int[] centroidOfFloor(boolean[][] floor, int w, int h) {
        long sx = 0, sy = 0, count = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (floor[y][x]) { sx += x; sy += y; count++; }
        if (count == 0) return new int[]{w / 2, h / 2};
        int cx = (int) (sx / count), cy = (int) (sy / count);
        // Snap to nearest floor tile
        int best = Integer.MAX_VALUE;
        int[] result = new int[]{cx, cy};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (floor[y][x]) {
                    int dist = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                    if (dist < best) { best = dist; result = new int[]{x, y}; }
                }
            }
        }
        return result;
    }
}
```

- [ ] **Step 5: Run tests — must pass**

```bash
./mvnw test -Dtest=CellularMapsTest
```
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: Maps interface + CellularMaps seeded cellular-automata generator"
```

---

## Phase 4 — Game Engine

### Task 6: Session model types

**Files:**
- Create: `src/main/java/com/jacuum/engine/RunStatus.java`
- Create: `src/main/java/com/jacuum/engine/FinishReason.java`
- Create: `src/main/java/com/jacuum/engine/IterationEvent.java`
- Create: `src/main/java/com/jacuum/engine/StatusEvent.java`
- Create: `src/main/java/com/jacuum/engine/SessionView.java`

- [ ] **Step 1: Create all model types**

```java
// RunStatus.java
package com.jacuum.engine;
public enum RunStatus { SETUP, RUNNING, PAUSED, FINISHED }
```

```java
// FinishReason.java
package com.jacuum.engine;
public enum FinishReason { COMPLETED, OUT_OF_ITERATIONS, ALGO_CRASH, INTERRUPTED }
```

```java
// IterationEvent.java
package com.jacuum.engine;
import com.jacuum.algo.Direction;
public record IterationEvent(
    String sessionId,
    int iteration,
    Direction direction,   // null if robot stayed put (wall collision)
    int robotX,
    int robotY,
    int score,
    int totalCleaned,
    int totalFloor,
    boolean finished,
    FinishReason finishReason  // null if not finished
) {}
```

```java
// StatusEvent.java
package com.jacuum.engine;
public record StatusEvent(String sessionId, RunStatus status, FinishReason finishReason) {}
```

```java
// SessionView.java
package com.jacuum.engine;
public record SessionView(
    String id,
    RunStatus status,
    int robotX,
    int robotY,
    int score,
    int totalCleaned,
    int iterationsUsed,
    int iterationsAvailable,
    int totalFloor,
    FinishReason finishReason
) {}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/
git commit -m "feat: engine model types (RunStatus, FinishReason, IterationEvent, SessionView)"
```

---

### Task 7: SessionTile + ActiveSession + Sessions + MemorySessions

**Files:**
- Create: `src/main/java/com/jacuum/engine/SessionTile.java`
- Create: `src/main/java/com/jacuum/engine/ActiveSession.java`
- Create: `src/main/java/com/jacuum/engine/Sessions.java`
- Create: `src/main/java/com/jacuum/engine/MemorySessions.java`
- Create: `src/test/java/com/jacuum/engine/MemorySessionsTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.jacuum.engine;

import com.jacuum.algo.*;
import com.jacuum.map.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MemorySessionsTest {

    // 3×3 all-floor map, start at (1,1)
    private static GameMap smallMap() {
        boolean[][] f = new boolean[3][3];
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) f[y][x] = true;
        return new GeneratedMap("test", SizePreset.TINY, f, 1, 1);
    }

    @Test void openCreatesSessionInSetupState() throws Exception {
        Sessions sessions = new MemorySessions(null); // no messaging in unit test
        String id = sessions.open(smallMap(), "RandomAlgo", "Alice", "🤖", 100);
        assertThat(id).isNotBlank();
        SessionView view = sessions.view(id);
        assertThat(view.status()).isEqualTo(RunStatus.SETUP);
        assertThat(view.robotX()).isEqualTo(1);
        assertThat(view.robotY()).isEqualTo(1);
        assertThat(view.score()).isEqualTo(0);
        assertThat(view.totalFloor()).isEqualTo(9);
    }

    @Test void viewThrowsForUnknownId() {
        Sessions sessions = new MemorySessions(null);
        assertThatThrownBy(() -> sessions.view("nope"))
            .isInstanceOf(Exception.class);
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./mvnw test -Dtest=MemorySessionsTest
```
Expected: FAIL.

- [ ] **Step 3: Create SessionTile**

```java
package com.jacuum.engine;

import com.jacuum.algo.Direction;
import com.jacuum.algo.Tile;
import java.util.Set;

final class SessionTile implements Tile {
    private final int x;
    private final int y;
    private final com.jacuum.map.GameMap map;
    private final Set<String> cleaned;

    SessionTile(int x, int y, com.jacuum.map.GameMap map, Set<String> cleaned) {
        this.x       = x;
        this.y       = y;
        this.map     = map;
        this.cleaned = cleaned;
    }

    @Override public int x()                          { return x; }
    @Override public int y()                          { return y; }
    @Override public boolean isClean()                { return cleaned.contains(x + "," + y); }
    @Override public boolean hasWall(Direction dir)   { return map.hasWall(x, y, dir); }
}
```

- [ ] **Step 4: Create ActiveSession**

```java
package com.jacuum.engine;

import com.jacuum.map.GameMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

final class ActiveSession {
    final String id;
    final GameMap map;
    final String algoName;
    final String username;
    final String avatar;
    final int iterationsAvailable;

    volatile int robotX;
    volatile int robotY;
    volatile int score;
    volatile int iterationsUsed;
    volatile RunStatus status;
    volatile FinishReason finishReason;
    final Set<String> cleaned = new HashSet<>();
    volatile Future<?> future;

    ActiveSession(String id, GameMap map, String algoName,
                  String username, String avatar, int iterationsAvailable) {
        this.id                   = id;
        this.map                  = map;
        this.algoName             = algoName;
        this.username             = username;
        this.avatar               = avatar;
        this.iterationsAvailable  = iterationsAvailable;
        this.robotX               = map.startX();
        this.robotY               = map.startY();
        this.status               = RunStatus.SETUP;
    }

    SessionView toView() {
        return new SessionView(id, status, robotX, robotY, score,
            cleaned.size(), iterationsUsed, iterationsAvailable,
            map.totalFloorTiles(), finishReason);
    }
}
```

- [ ] **Step 5: Create Sessions interface**

```java
package com.jacuum.engine;

import com.jacuum.map.GameMap;

public interface Sessions {
    String open(GameMap map, String algoName, String username,
                String avatar, int iterations) throws Exception;
    void start(String id) throws Exception;
    void pause(String id) throws Exception;
    void resume(String id) throws Exception;
    void stop(String id) throws Exception;
    SessionView view(String id) throws Exception;
}
```

- [ ] **Step 6: Create MemorySessions (open + view only; start/pause/resume/stop in next task)**

```java
package com.jacuum.engine;

import com.jacuum.algo.*;
import com.jacuum.map.GameMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MemorySessions implements Sessions {

    private final ConcurrentHashMap<String, ActiveSession> store = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messaging;

    public MemorySessions(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public String open(GameMap map, String algoName, String username,
                       String avatar, int iterations) throws Exception {
        String id = UUID.randomUUID().toString();
        store.put(id, new ActiveSession(id, map, algoName, username, avatar, iterations));
        return id;
    }

    @Override
    public SessionView view(String id) throws Exception {
        ActiveSession s = require(id);
        return s.toView();
    }

    @Override
    public void start(String id) throws Exception {
        // implemented in Task 9
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override public void pause(String id)  throws Exception { require(id).status = RunStatus.PAUSED; }
    @Override public void resume(String id) throws Exception { require(id).status = RunStatus.RUNNING; }
    @Override public void stop(String id)   throws Exception {
        ActiveSession s = require(id);
        s.status = RunStatus.FINISHED;
        s.finishReason = FinishReason.INTERRUPTED;
        if (s.future != null) s.future.cancel(true);
    }

    ActiveSession require(String id) throws Exception {
        ActiveSession s = store.get(id);
        if (s == null) throw new IllegalArgumentException("Unknown session: " + id);
        return s;
    }
}
```

- [ ] **Step 7: Run tests — must pass**

```bash
./mvnw test -Dtest=MemorySessionsTest
```
Expected: PASS (2 tests).

- [ ] **Step 8: Commit**

```bash
git add src/
git commit -m "feat: Sessions interface + MemorySessions + SessionTile + ActiveSession"
```

---

### Task 8: Game loop — Algorithms registry + start() implementation

**Files:**
- Create: `src/main/java/com/jacuum/algo/Algorithms.java`
- Create: `src/main/java/com/jacuum/algo/SpringAlgorithms.java`
- Modify: `src/main/java/com/jacuum/engine/MemorySessions.java` (implement `start()`)
- Create: `src/test/java/com/jacuum/engine/GameLoopTest.java`

- [ ] **Step 1: Create Algorithms interface**

```java
package com.jacuum.algo;

import java.util.List;

public interface Algorithms {
    List<String> names();
    RobotAlgo instantiate(String name) throws Exception;
}
```

- [ ] **Step 2: Create SpringAlgorithms**

```java
package com.jacuum.algo;

import org.springframework.context.ApplicationContext;
import java.util.List;
import java.util.Map;

public final class SpringAlgorithms implements Algorithms {

    private final ApplicationContext ctx;

    public SpringAlgorithms(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<String> names() {
        return ctx.getBeansWithAnnotation(RobotAlgorithm.class)
            .entrySet().stream()
            .map(e -> displayName(e.getValue()))
            .sorted()
            .toList();
    }

    @Override
    public RobotAlgo instantiate(String name) throws Exception {
        for (Map.Entry<String, Object> e :
                ctx.getBeansWithAnnotation(RobotAlgorithm.class).entrySet()) {
            if (displayName(e.getValue()).equals(name)) {
                return (RobotAlgo) e.getValue().getClass()
                    .getDeclaredConstructor().newInstance();
            }
        }
        throw new IllegalArgumentException("Unknown algorithm: " + name);
    }

    private static String displayName(Object bean) {
        RobotAlgorithm ann = bean.getClass().getAnnotation(RobotAlgorithm.class);
        String v = ann.value();
        return v.isBlank() ? bean.getClass().getSimpleName() : v;
    }
}
```

- [ ] **Step 3: Write failing game-loop test**

```java
package com.jacuum.engine;

import com.jacuum.algo.*;
import com.jacuum.map.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GameLoopTest {

    // Minimal Algorithms that always returns a fixed algo
    private static com.jacuum.algo.Algorithms eastAlways() {
        return new com.jacuum.algo.Algorithms() {
            @Override public java.util.List<String> names() { return java.util.List.of("east"); }
            @Override public RobotAlgo instantiate(String name) { return tile -> Direction.EAST; }
        };
    }

    // 1×5 corridor: floor at y=0, x=0..4
    private static GameMap corridor() {
        boolean[][] f = new boolean[3][7];
        for (int x = 1; x < 6; x++) f[1][x] = true;
        return new GeneratedMap("corridor", SizePreset.TINY, f, 1, 1);
    }

    @Test void robotCleansCorridorMovingEast() throws Exception {
        MemorySessions sessions = new MemorySessions(null);
        sessions.setAlgorithms(eastAlways());
        String id = sessions.open(corridor(), "east", "Bot", "🤖", 20);
        sessions.start(id);

        // Wait for finish (max 2s)
        long deadline = System.currentTimeMillis() + 2000;
        while (sessions.view(id).status() != RunStatus.FINISHED
               && System.currentTimeMillis() < deadline)
            Thread.sleep(50);

        SessionView view = sessions.view(id);
        assertThat(view.status()).isEqualTo(RunStatus.FINISHED);
        assertThat(view.totalCleaned()).isGreaterThan(0);
        assertThat(view.score()).isGreaterThan(0);
    }

    @Test void algoCrashFinishesWithZeroScore() throws Exception {
        MemorySessions sessions = new MemorySessions(null);
        sessions.setAlgorithms(new com.jacuum.algo.Algorithms() {
            @Override public java.util.List<String> names() { return java.util.List.of("crash"); }
            @Override public RobotAlgo instantiate(String n) {
                return tile -> { throw new RuntimeException("boom"); };
            }
        });
        String id = sessions.open(corridor(), "crash", "Bot", "🤖", 20);
        sessions.start(id);
        long deadline = System.currentTimeMillis() + 2000;
        while (sessions.view(id).status() != RunStatus.FINISHED
               && System.currentTimeMillis() < deadline)
            Thread.sleep(50);
        SessionView view = sessions.view(id);
        assertThat(view.finishReason()).isEqualTo(FinishReason.ALGO_CRASH);
    }
}
```

- [ ] **Step 4: Run to confirm failures**

```bash
./mvnw test -Dtest=GameLoopTest
```
Expected: FAIL — `setAlgorithms` not found.

- [ ] **Step 5: Implement `start()` in MemorySessions**

Add `setAlgorithms` and implement `start()`:

```java
// Add field:
private Algorithms algorithms;

public void setAlgorithms(Algorithms algorithms) {
    this.algorithms = algorithms;
}

@Override
public void start(String id) throws Exception {
    ActiveSession s = require(id);
    if (s.status != RunStatus.SETUP && s.status != RunStatus.PAUSED)
        throw new IllegalStateException("Cannot start session in state: " + s.status);
    s.status = RunStatus.RUNNING;
    RobotAlgo algo = algorithms.instantiate(s.algoName);
    s.future = Thread.ofVirtual().start(() -> runLoop(s, algo));
}

private void runLoop(ActiveSession s, RobotAlgo algo) {
    while (s.iterationsUsed < s.iterationsAvailable && s.status != RunStatus.FINISHED) {
        if (s.status == RunStatus.PAUSED) {
            try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            continue;
        }
        SessionTile tile = new SessionTile(s.robotX, s.robotY, s.map, s.cleaned);
        Direction dir;
        try {
            dir = algo.next(tile);
        } catch (Exception e) {
            finish(s, FinishReason.ALGO_CRASH);
            return;
        }
        int nx = s.robotX + dir.dx(), ny = s.robotY + dir.dy();
        if (!s.map.hasWall(s.robotX, s.robotY, dir)) {
            s.robotX = nx;
            s.robotY = ny;
        }
        String key = s.robotX + "," + s.robotY;
        if (!s.cleaned.contains(key)) {
            s.cleaned.add(key);
            s.score += 100;
        }
        s.iterationsUsed++;
        IterationEvent event = new IterationEvent(
            s.id, s.iterationsUsed, dir,
            s.robotX, s.robotY, s.score,
            s.cleaned.size(), s.map.totalFloorTiles(),
            false, null);
        publish(s.id, event);

        if (s.cleaned.size() == s.map.totalFloorTiles()) {
            finish(s, FinishReason.COMPLETED);
            return;
        }
    }
    if (s.status != RunStatus.FINISHED) finish(s, FinishReason.OUT_OF_ITERATIONS);
}

private void finish(ActiveSession s, FinishReason reason) {
    s.status = RunStatus.FINISHED;
    s.finishReason = reason;
    if (reason == FinishReason.ALGO_CRASH) s.score = 0;
    IterationEvent event = new IterationEvent(
        s.id, s.iterationsUsed, null,
        s.robotX, s.robotY, s.score,
        s.cleaned.size(), s.map.totalFloorTiles(),
        true, reason);
    publish(s.id, event);
    if (messaging != null)
        messaging.convertAndSend("/topic/session/" + s.id + "/status",
            new StatusEvent(s.id, RunStatus.FINISHED, reason));
}

private void publish(String sessionId, IterationEvent event) {
    if (messaging != null)
        messaging.convertAndSend("/topic/session/" + sessionId + "/events", event);
}
```

- [ ] **Step 6: Run tests — must pass**

```bash
./mvnw test -Dtest=GameLoopTest
```
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: game loop in virtual thread, algo crash handling, iteration events"
```

---

## Phase 5 — Sample Algorithms

### Task 9: RandomAlgo + AlwaysLeftAlgo + smoke tests

**Files:**
- Create: `src/main/java/com/jacuum/algo/impl/RandomAlgo.java`
- Create: `src/main/java/com/jacuum/algo/impl/AlwaysLeftAlgo.java`
- Create: `src/test/java/com/jacuum/algo/AlgoSmokeTest.java`

- [ ] **Step 1: Write smoke test**

```java
package com.jacuum.algo;

import com.jacuum.algo.impl.AlwaysLeftAlgo;
import com.jacuum.algo.impl.RandomAlgo;
import com.jacuum.map.*;
import com.jacuum.engine.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class AlgoSmokeTest {

    static Stream<RobotAlgo> algos() {
        return Stream.of(new RandomAlgo(), new AlwaysLeftAlgo());
    }

    static GameMap squareMap() {
        boolean[][] f = new boolean[7][7];
        for (int y = 1; y < 6; y++) for (int x = 1; x < 6; x++) f[y][x] = true;
        return new GeneratedMap("sq", SizePreset.TINY, f, 3, 3);
    }

    static GameMap corridorMap() {
        boolean[][] f = new boolean[3][9];
        for (int x = 1; x < 8; x++) f[1][x] = true;
        return new GeneratedMap("corr", SizePreset.TINY, f, 1, 1);
    }

    @ParameterizedTest @MethodSource("algos")
    void doesNotThrowOnSquareMap(RobotAlgo algo) throws Exception {
        runAlgo(algo, squareMap(), 50);
    }

    @ParameterizedTest @MethodSource("algos")
    void doesNotThrowOnCorridor(RobotAlgo algo) throws Exception {
        runAlgo(algo, corridorMap(), 30);
    }

    private void runAlgo(RobotAlgo algo, GameMap map, int iterations) throws Exception {
        int x = map.startX(), y = map.startY();
        var cleaned = new java.util.HashSet<String>();
        for (int i = 0; i < iterations; i++) {
            SessionTile tile = new SessionTile(x, y, map, cleaned);
            Direction dir = algo.next(tile);
            assertThat(dir).isNotNull();
            if (!map.hasWall(x, y, dir)) {
                x += dir.dx();
                y += dir.dy();
            }
            cleaned.add(x + "," + y);
        }
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./mvnw test -Dtest=AlgoSmokeTest
```
Expected: FAIL — impl classes not found.

- [ ] **Step 3: Create RandomAlgo**

```java
package com.jacuum.algo.impl;

import com.jacuum.algo.*;
import java.util.*;

@RobotAlgorithm("Random")
public final class RandomAlgo implements RobotAlgo {
    private final Random rng = new Random();

    @Override
    public Direction next(Tile tile) {
        List<Direction> passable = new ArrayList<>(4);
        for (Direction d : Direction.values())
            if (!tile.hasWall(d)) passable.add(d);
        if (passable.isEmpty())
            return Direction.values()[rng.nextInt(Direction.values().length)];
        return passable.get(rng.nextInt(passable.size()));
    }
}
```

- [ ] **Step 4: Create AlwaysLeftAlgo**

```java
package com.jacuum.algo.impl;

import com.jacuum.algo.*;

/**
 * Prefers WEST (left), then falls back clockwise: WEST → NORTH → EAST → SOUTH.
 */
@RobotAlgorithm("Always Left")
public final class AlwaysLeftAlgo implements RobotAlgo {
    private static final Direction[] PREFERENCE =
        {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};

    @Override
    public Direction next(Tile tile) {
        for (Direction d : PREFERENCE)
            if (!tile.hasWall(d)) return d;
        return Direction.WEST; // surrounded — stay put next iteration
    }
}
```

- [ ] **Step 5: Run tests — must pass**

```bash
./mvnw test -Dtest=AlgoSmokeTest
```
Expected: PASS (4 tests — 2 algos × 2 maps).

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: RandomAlgo + AlwaysLeftAlgo with smoke tests on predefined maps"
```

---

## Phase 6 — WebSocket + REST API

### Task 10: WebSocket config + AppConfig beans

**Files:**
- Create: `src/main/java/com/jacuum/web/WebSocketConfig.java`
- Create: `src/main/java/com/jacuum/web/AppConfig.java`

- [ ] **Step 1: Create WebSocketConfig**

```java
package com.jacuum.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

- [ ] **Step 2: Create AppConfig**

```java
package com.jacuum.web;

import com.jacuum.algo.*;
import com.jacuum.engine.*;
import com.jacuum.leaderboard.*;
import com.jacuum.map.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class AppConfig {

    @Bean
    public Maps maps() {
        return new CellularMaps();
    }

    @Bean
    public Algorithms algorithms(ApplicationContext ctx) {
        return new SpringAlgorithms(ctx);
    }

    @Bean
    public Sessions sessions(SimpMessagingTemplate messaging, Algorithms algorithms) {
        MemorySessions s = new MemorySessions(messaging);
        s.setAlgorithms(algorithms);
        return s;
    }

    @Bean
    public Leaderboard leaderboard(@Value("${leaderboard.file:}") String path) {
        if (path == null || path.isBlank()) return new SilentLeaderboard();
        return new JsonLeaderboard(java.nio.file.Path.of(path));
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./mvnw compile
```
Expected: BUILD SUCCESS (leaderboard classes will be added in Phase 7 — add stubs if needed).

- [ ] **Step 4: Commit**

```bash
git add src/
git commit -m "feat: WebSocket STOMP config + AppConfig bean wiring"
```

---

### Task 11: REST DTO records

**Files:**
- Create: `src/main/java/com/jacuum/web/dto/CreateSessionRequest.java`
- Create: `src/main/java/com/jacuum/web/dto/SessionResponse.java`
- Create: `src/main/java/com/jacuum/web/dto/MapSnapshot.java`

- [ ] **Step 1: Create DTO records**

```java
// CreateSessionRequest.java
package com.jacuum.web.dto;

public record CreateSessionRequest(
    String hash,
    String size,
    String algoName,
    String username,
    String avatar,
    int iterations
) {}
```

```java
// MapSnapshot.java
package com.jacuum.web.dto;

import java.util.List;

public record MapSnapshot(
    int width,
    int height,
    int startX,
    int startY,
    int totalFloor,
    List<TileSnapshot> tiles
) {
    public record TileSnapshot(
        int x, int y,
        boolean wallNorth, boolean wallSouth, boolean wallEast, boolean wallWest
    ) {}
}
```

```java
// SessionResponse.java
package com.jacuum.web.dto;

public record SessionResponse(
    String sessionId,
    String status,
    MapSnapshot map,
    int robotX,
    int robotY,
    int totalFloor,
    int iterationsAvailable
) {}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile
```

- [ ] **Step 3: Commit**

```bash
git add src/
git commit -m "feat: REST DTO records (CreateSessionRequest, SessionResponse, MapSnapshot)"
```

---

### Task 12: SessionEndpoint + AlgosEndpoint

**Files:**
- Create: `src/main/java/com/jacuum/web/SessionEndpoint.java`
- Create: `src/main/java/com/jacuum/web/AlgosEndpoint.java`
- Create: `src/test/java/com/jacuum/web/SessionEndpointTest.java`
- Create: `src/test/java/com/jacuum/web/AlgosEndpointTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.jacuum.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jacuum.web.dto.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessionEndpointTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void createSessionReturns200WithSessionId() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "Random", "Alice", "🤖", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("SETUP"))
            .andExpect(jsonPath("$.map.width").isNumber());
    }

    @Test void startSessionReturns200() throws Exception {
        // create first
        var body = new CreateSessionRequest(null, "TINY", "Random", "Alice", "🤖", 50);
        String resp = mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(resp).get("sessionId").asText();

        mvc.perform(post("/api/session/" + id + "/start"))
            .andExpect(status().isOk());
    }
}
```

```java
package com.jacuum.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlgosEndpointTest {

    @Autowired MockMvc mvc;

    @Test void algosEndpointReturnsList() throws Exception {
        mvc.perform(get("/api/algos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test void avatarsEndpointReturnsList() throws Exception {
        mvc.perform(get("/api/avatars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

- [ ] **Step 2: Run to confirm failures**

```bash
./mvnw test -Dtest="SessionEndpointTest,AlgosEndpointTest"
```
Expected: FAIL.

- [ ] **Step 3: Create AlgosEndpoint**

```java
package com.jacuum.web;

import com.jacuum.algo.Algorithms;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public final class AlgosEndpoint {

    private final Algorithms algorithms;

    public AlgosEndpoint(Algorithms algorithms) {
        this.algorithms = algorithms;
    }

    @GetMapping("/algos")
    public List<String> algos() {
        return algorithms.names();
    }

    @GetMapping("/avatars")
    public List<String> avatars() {
        return List.of("🤖","🦾","👾","🚀","🛸","🦄","🐢","🦊","🐱","🐸");
    }
}
```

- [ ] **Step 4: Create SessionEndpoint**

```java
package com.jacuum.web;

import com.jacuum.algo.Direction;
import com.jacuum.engine.*;
import com.jacuum.map.*;
import com.jacuum.web.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/session")
public final class SessionEndpoint {

    private final Sessions sessions;
    private final Maps maps;
    @Value("${game.default-iterations:500}") private int defaultIterations;

    public SessionEndpoint(Sessions sessions, Maps maps) {
        this.sessions = sessions;
        this.maps     = maps;
    }

    @PostMapping
    public SessionResponse create(@RequestBody CreateSessionRequest req) throws Exception {
        String hash  = (req.hash() != null && !req.hash().isBlank())
                       ? req.hash() : UUID.randomUUID().toString();
        SizePreset size = sizeFrom(req.size());
        int iters = req.iterations() > 0 ? req.iterations() : defaultIterations;
        GameMap map = maps.generate(hash, size);
        String id   = sessions.open(map, req.algoName(), req.username(), req.avatar(), iters);
        return toResponse(id, map, iters);
    }

    @PostMapping("/{id}/start")
    public SessionView start(@PathVariable String id) throws Exception {
        sessions.start(id);
        return sessions.view(id);
    }

    @PostMapping("/{id}/pause")
    public SessionView pause(@PathVariable String id) throws Exception {
        sessions.pause(id);
        return sessions.view(id);
    }

    @PostMapping("/{id}/resume")
    public SessionView resume(@PathVariable String id) throws Exception {
        sessions.resume(id);
        return sessions.view(id);
    }

    @PostMapping("/{id}/stop")
    public SessionView stop(@PathVariable String id) throws Exception {
        sessions.stop(id);
        return sessions.view(id);
    }

    @GetMapping("/{id}")
    public SessionView view(@PathVariable String id) throws Exception {
        return sessions.view(id);
    }

    private SessionResponse toResponse(String id, GameMap map, int iters) throws Exception {
        List<MapSnapshot.TileSnapshot> tiles = new ArrayList<>();
        for (int y = 0; y < map.height(); y++)
            for (int x = 0; x < map.width(); x++)
                if (map.isFloor(x, y))
                    tiles.add(new MapSnapshot.TileSnapshot(x, y,
                        map.hasWall(x, y, Direction.NORTH),
                        map.hasWall(x, y, Direction.SOUTH),
                        map.hasWall(x, y, Direction.EAST),
                        map.hasWall(x, y, Direction.WEST)));
        MapSnapshot snap = new MapSnapshot(map.width(), map.height(),
            map.startX(), map.startY(), map.totalFloorTiles(), tiles);
        return new SessionResponse(id, "SETUP", snap,
            map.startX(), map.startY(), map.totalFloorTiles(), iters);
    }

    private static SizePreset sizeFrom(String s) {
        if (s == null || s.isBlank()) return SizePreset.SMALL;
        try { return SizePreset.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return SizePreset.SMALL; }
    }
}
```

- [ ] **Step 5: Run tests — must pass**

```bash
./mvnw test -Dtest="SessionEndpointTest,AlgosEndpointTest"
```
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: SessionEndpoint + AlgosEndpoint REST controllers with MockMvc tests"
```

---

## Phase 7 — Leaderboard

### Task 13: Leaderboard model + JsonLeaderboard

**Files:**
- Create: `src/main/java/com/jacuum/leaderboard/TraceEvent.java`
- Create: `src/main/java/com/jacuum/leaderboard/LeaderboardEntry.java`
- Create: `src/main/java/com/jacuum/leaderboard/Leaderboard.java`
- Create: `src/main/java/com/jacuum/leaderboard/SilentLeaderboard.java`
- Create: `src/main/java/com/jacuum/leaderboard/JsonLeaderboard.java`
- Create: `src/test/java/com/jacuum/leaderboard/JsonLeaderboardTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.jacuum.leaderboard;

import com.jacuum.algo.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class JsonLeaderboardTest {

    @TempDir Path tmp;

    private LeaderboardEntry entry() {
        return new LeaderboardEntry(
            "id1", "Luke", "🤖", "hash1", "SMALL", "Random",
            42, 100, 4200, Instant.now().toString(),
            List.of(new TraceEvent(1, Direction.EAST, 1, 1, 100))
        );
    }

    @Test void saveAndRetrieveEntry() throws Exception {
        Path file = tmp.resolve("lb.json");
        Leaderboard lb = new JsonLeaderboard(file);
        lb.save(entry());
        List<LeaderboardEntry> entries = lb.entries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).username()).isEqualTo("Luke");
    }

    @Test void persistsAcrossInstances() throws Exception {
        Path file = tmp.resolve("lb2.json");
        new JsonLeaderboard(file).save(entry());
        List<LeaderboardEntry> entries = new JsonLeaderboard(file).entries();
        assertThat(entries).hasSize(1);
    }

    @Test void silentLeaderboardDoesNothing() throws Exception {
        Leaderboard lb = new SilentLeaderboard();
        lb.save(entry());
        assertThat(lb.entries()).isEmpty();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./mvnw test -Dtest=JsonLeaderboardTest
```
Expected: FAIL.

- [ ] **Step 3: Create model types**

```java
// TraceEvent.java
package com.jacuum.leaderboard;
import com.jacuum.algo.Direction;
public record TraceEvent(int iteration, Direction direction, int x, int y, int score) {}
```

```java
// LeaderboardEntry.java
package com.jacuum.leaderboard;
import java.util.List;
public record LeaderboardEntry(
    String id, String username, String avatar,
    String mapHash, String mapSize, String algoName,
    int iterationsUsed, int iterationsAvailable,
    int score, String completedAt,
    List<TraceEvent> trace
) {}
```

```java
// Leaderboard.java
package com.jacuum.leaderboard;
import java.util.List;
public interface Leaderboard {
    List<LeaderboardEntry> entries() throws Exception;
    void save(LeaderboardEntry entry) throws Exception;
}
```

```java
// SilentLeaderboard.java
package com.jacuum.leaderboard;
import java.util.List;
public final class SilentLeaderboard implements Leaderboard {
    @Override public List<LeaderboardEntry> entries() { return List.of(); }
    @Override public void save(LeaderboardEntry entry) { /* no-op */ }
}
```

- [ ] **Step 4: Create JsonLeaderboard**

```java
package com.jacuum.leaderboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonLeaderboard implements Leaderboard {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<LeaderboardEntry>> TYPE =
        new TypeReference<>() {};

    private final Path file;

    public JsonLeaderboard(Path file) {
        this.file = file;
    }

    @Override
    public List<LeaderboardEntry> entries() throws Exception {
        if (!Files.exists(file)) return List.of();
        return MAPPER.readValue(file.toFile(), TYPE);
    }

    @Override
    public void save(LeaderboardEntry entry) throws Exception {
        List<LeaderboardEntry> current = new ArrayList<>(entries());
        current.add(entry);
        MAPPER.writeValue(file.toFile(), current);
    }
}
```

- [ ] **Step 5: Run tests — must pass**

```bash
./mvnw test -Dtest=JsonLeaderboardTest
```
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: Leaderboard interface + JsonLeaderboard (JSON file) + SilentLeaderboard"
```

---

### Task 14: LeaderboardEndpoint

**Files:**
- Create: `src/main/java/com/jacuum/web/LeaderboardEndpoint.java`

- [ ] **Step 1: Create LeaderboardEndpoint**

```java
package com.jacuum.web;

import com.jacuum.leaderboard.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public final class LeaderboardEndpoint {

    private final Leaderboard leaderboard;

    public LeaderboardEndpoint(Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    @GetMapping
    public List<LeaderboardEntry> entries() throws Exception {
        return leaderboard.entries();
    }

    @PostMapping
    public LeaderboardEntry save(@RequestBody LeaderboardEntry entry) throws Exception {
        leaderboard.save(entry);
        return entry;
    }
}
```

- [ ] **Step 2: Verify compilation and all tests still pass**

```bash
./mvnw test
```
Expected: All tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/
git commit -m "feat: LeaderboardEndpoint GET+POST /api/leaderboard"
```

---

## Phase 8 — Web UI

### Task 15: Download static JS libs + base HTML + CSS

**Files:**
- Create: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/css/game.css`
- Download: `src/main/resources/static/lib/sockjs.min.js`
- Download: `src/main/resources/static/lib/stomp.min.js`

- [ ] **Step 1: Download SockJS and STOMP.js**

```bash
mkdir -p src/main/resources/static/lib
curl -L "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js" \
     -o src/main/resources/static/lib/sockjs.min.js
curl -L "https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js" \
     -o src/main/resources/static/lib/stomp.min.js
```

- [ ] **Step 2: Create index.html**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Jacuum Cleaner</title>
  <link rel="stylesheet" href="/css/game.css">
</head>
<body>

<nav>
  <button onclick="App.show('setup')">Setup</button>
  <button onclick="App.show('leaderboard')">Leaderboard</button>
</nav>

<!-- Setup Screen -->
<section id="screen-setup" class="screen">
  <h2>New Game</h2>
  <label>Map Hash <input id="map-hash" placeholder="leave blank for random"></label>
  <label>Size
    <select id="map-size">
      <option value="TINY">Tiny</option>
      <option value="SMALL" selected>Small</option>
      <option value="MEDIUM">Medium</option>
      <option value="LARGE">Large</option>
    </select>
  </label>
  <label>Username <input id="username" placeholder="random hero name"></label>
  <label>Avatar <div id="avatar-picker"></div></label>
  <label>Algorithm <select id="algo-select"></select></label>
  <label>Iterations
    <input type="range" id="iterations-range" min="0" max="4" step="1" value="1">
    <span id="iterations-label"></span>
  </label>
  <button id="btn-start-game">Start Game</button>
</section>

<!-- Game Screen -->
<section id="screen-game" class="screen hidden">
  <div id="game-info">
    <span>Score: <strong id="score">0</strong></span>
    <span>Cleaned: <strong id="cleaned">0</strong>/<strong id="total-floor">0</strong></span>
    <span>Iterations: <strong id="iters-used">0</strong>/<strong id="iters-avail">0</strong></span>
  </div>
  <canvas id="game-canvas"></canvas>
  <div id="game-controls">
    <button id="btn-pause">Pause</button>
    <button id="btn-resume" disabled>Resume</button>
    <button id="btn-stop">Stop</button>
    <label>Speed
      <input type="range" id="speed-range" min="0" max="5" step="1" value="2">
      <span id="speed-label"></span>
    </label>
  </div>
  <div id="finish-panel" class="hidden">
    <h3>Done! Score: <span id="final-score"></span></h3>
    <button id="btn-save-lb">Save to Leaderboard</button>
    <button onclick="App.show('setup')">New Game</button>
  </div>
</section>

<!-- Leaderboard Screen -->
<section id="screen-leaderboard" class="screen hidden">
  <h2>Leaderboard</h2>
  <table id="lb-table">
    <thead><tr>
      <th>#</th><th>Avatar</th><th>Name</th><th>Map</th>
      <th>Size</th><th>Algo</th><th>Iter Used/Avail</th><th>Score</th><th>Actions</th>
    </tr></thead>
    <tbody id="lb-body"></tbody>
  </table>
  <p id="lb-empty" class="hidden">No leaderboard file configured.</p>
</section>

<script src="/lib/sockjs.min.js"></script>
<script src="/lib/stomp.min.js"></script>
<script src="/js/api.js"></script>
<script src="/js/setup.js"></script>
<script src="/js/game.js"></script>
<script src="/js/leaderboard.js"></script>
<script src="/js/app.js"></script>
</body>
</html>
```

- [ ] **Step 3: Create game.css**

```css
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: monospace; background: #1a1a2e; color: #eee; padding: 1rem; }
nav { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
nav button { padding: 0.4rem 1rem; background: #16213e; border: 1px solid #0f3460; color: #eee; cursor: pointer; border-radius: 4px; }
nav button:hover { background: #0f3460; }
.screen { max-width: 900px; margin: 0 auto; }
.hidden { display: none !important; }
h2 { margin-bottom: 1rem; color: #e94560; }
label { display: block; margin-bottom: 0.6rem; }
label input, label select { margin-left: 0.5rem; background: #16213e; border: 1px solid #0f3460; color: #eee; padding: 0.3rem; border-radius: 3px; }
button { padding: 0.4rem 0.9rem; background: #e94560; border: none; color: #fff; cursor: pointer; border-radius: 4px; margin-right: 0.3rem; }
button:disabled { opacity: 0.4; cursor: not-allowed; }
#avatar-picker { display: inline-flex; gap: 0.4rem; flex-wrap: wrap; margin-top: 0.3rem; }
.avatar-opt { font-size: 1.6rem; cursor: pointer; padding: 2px; border-radius: 4px; border: 2px solid transparent; }
.avatar-opt.selected { border-color: #e94560; }
#game-canvas { display: block; margin: 0.5rem 0; border: 1px solid #0f3460; }
#game-info { display: flex; gap: 1.5rem; margin-bottom: 0.5rem; font-size: 0.9rem; }
#game-controls { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.5rem; }
#finish-panel { margin-top: 1rem; padding: 1rem; background: #16213e; border-radius: 6px; }
#finish-panel h3 { margin-bottom: 0.5rem; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.4rem 0.6rem; text-align: left; border-bottom: 1px solid #0f3460; font-size: 0.85rem; }
th { color: #e94560; }
```

- [ ] **Step 4: Verify app starts and serves index.html**

```bash
./mvnw spring-boot:run &
sleep 3
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/
# kill background job after
```
Expected: `200`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/
git commit -m "feat: base HTML, CSS, SockJS+STOMP libs for game UI"
```

---

### Task 16: API client + app router

**Files:**
- Create: `src/main/resources/static/js/api.js`
- Create: `src/main/resources/static/js/app.js`

- [ ] **Step 1: Create api.js**

```javascript
const Api = (() => {
  const json = (r) => { if (!r.ok) throw new Error(r.status); return r.json(); };

  return {
    algos:         () => fetch('/api/algos').then(json),
    avatars:       () => fetch('/api/avatars').then(json),
    createSession: (body) => fetch('/api/session', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify(body)
    }).then(json),
    start:   (id) => fetch(`/api/session/${id}/start`,  {method:'POST'}).then(json),
    pause:   (id) => fetch(`/api/session/${id}/pause`,  {method:'POST'}).then(json),
    resume:  (id) => fetch(`/api/session/${id}/resume`, {method:'POST'}).then(json),
    stop:    (id) => fetch(`/api/session/${id}/stop`,   {method:'POST'}).then(json),
    leaderboard:   () => fetch('/api/leaderboard').then(json),
    saveToLb: (entry) => fetch('/api/leaderboard', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify(entry)
    }).then(json),
  };
})();
```

- [ ] **Step 2: Create app.js**

```javascript
const App = (() => {
  const screens = ['setup', 'game', 'leaderboard'];

  function show(name) {
    screens.forEach(s => {
      document.getElementById('screen-' + s).classList.toggle('hidden', s !== name);
    });
    if (name === 'leaderboard') Leaderboard.load();
  }

  document.addEventListener('DOMContentLoaded', () => {
    Setup.init();
    show('setup');
  });

  return { show };
})();
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/js/
git commit -m "feat: API client module + screen router"
```

---

### Task 17: Setup screen

**Files:**
- Create: `src/main/resources/static/js/setup.js`

- [ ] **Step 1: Create setup.js**

```javascript
const Setup = (() => {
  const ITER_VALUES = [250, 500, 1000, 2000, 5000];
  const PREFS_KEY   = 'jacuum_prefs';
  let selectedAvatar = '🤖';

  function savePrefs() {
    localStorage.setItem(PREFS_KEY, JSON.stringify({
      hash:     document.getElementById('map-hash').value,
      size:     document.getElementById('map-size').value,
      username: document.getElementById('username').value,
      avatar:   selectedAvatar,
      algo:     document.getElementById('algo-select').value,
      iters:    document.getElementById('iterations-range').value,
    }));
  }

  function loadPrefs() {
    try {
      const p = JSON.parse(localStorage.getItem(PREFS_KEY) || '{}');
      if (p.hash)     document.getElementById('map-hash').value = p.hash;
      if (p.size)     document.getElementById('map-size').value = p.size;
      if (p.username) document.getElementById('username').value = p.username;
      if (p.iters !== undefined) document.getElementById('iterations-range').value = p.iters;
      if (p.avatar)   selectedAvatar = p.avatar;
      if (p.algo)     pendingAlgo = p.algo;
    } catch (_) {}
    updateIterLabel();
  }

  let pendingAlgo = null;

  function updateIterLabel() {
    const idx = parseInt(document.getElementById('iterations-range').value);
    document.getElementById('iterations-label').textContent = ITER_VALUES[idx].toLocaleString();
  }

  async function init() {
    document.getElementById('iterations-range').addEventListener('input', updateIterLabel);

    // Load avatars
    const avatars = await Api.avatars();
    const picker  = document.getElementById('avatar-picker');
    picker.innerHTML = '';
    avatars.forEach(av => {
      const span = document.createElement('span');
      span.className = 'avatar-opt' + (av === selectedAvatar ? ' selected' : '');
      span.textContent = av;
      span.onclick = () => {
        selectedAvatar = av;
        document.querySelectorAll('.avatar-opt').forEach(el => el.classList.remove('selected'));
        span.classList.add('selected');
      };
      picker.appendChild(span);
    });

    // Load algos
    const algos  = await Api.algos();
    const sel    = document.getElementById('algo-select');
    sel.innerHTML = algos.map(a => `<option value="${a}">${a}</option>`).join('');
    if (pendingAlgo) sel.value = pendingAlgo;

    // Random username placeholder via Faker endpoint (use fixed list as fallback)
    const HEROES = ['Luke Skywalker','Leia Organa','Han Solo','Rey','Din Djarin','Obi-Wan Kenobi'];
    if (!document.getElementById('username').value)
      document.getElementById('username').placeholder = HEROES[Math.floor(Math.random() * HEROES.length)];

    loadPrefs();

    document.getElementById('btn-start-game').addEventListener('click', async () => {
      savePrefs();
      const idx  = parseInt(document.getElementById('iterations-range').value);
      const body = {
        hash:      document.getElementById('map-hash').value || null,
        size:      document.getElementById('map-size').value,
        algoName:  document.getElementById('algo-select').value,
        username:  document.getElementById('username').value ||
                   document.getElementById('username').placeholder,
        avatar:    selectedAvatar,
        iterations: ITER_VALUES[idx],
      };
      try {
        const session = await Api.createSession(body);
        Game.init(session, body);
        App.show('game');
        await Api.start(session.sessionId);
      } catch (e) {
        alert('Failed to start: ' + e.message);
      }
    });
  }

  return { init };
})();
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/js/setup.js
git commit -m "feat: setup screen with localStorage prefs, avatar picker, algo selector"
```

---

### Task 18: Game screen — canvas rendering + WebSocket

**Files:**
- Create: `src/main/resources/static/js/game.js`

- [ ] **Step 1: Create game.js**

```javascript
const Game = (() => {
  const CELL  = 22;
  const SPEEDS = [800, 400, 200, 100, 50, 16]; // ms per frame
  let stompClient, sessionId, mapData, setupBody;
  let robotX, robotY, eventQueue = [], animating = false, speedIdx = 2;
  let score = 0, cleaned = 0, itersUsed = 0, itersAvail = 0;
  let cleanedSet = new Set();
  let finished = false, sessionTrace = [];

  function init(session, body) {
    sessionId  = session.sessionId;
    mapData    = session.map;
    setupBody  = body;
    robotX     = session.robotX;
    robotY     = session.robotY;
    itersAvail = session.iterationsAvailable;
    score = cleaned = itersUsed = 0;
    cleanedSet.clear();
    eventQueue = [];
    finished   = false;
    sessionTrace = [];

    const canvas = document.getElementById('game-canvas');
    canvas.width  = mapData.width  * CELL;
    canvas.height = mapData.height * CELL;

    document.getElementById('total-floor').textContent  = mapData.totalFloor;
    document.getElementById('iters-avail').textContent  = itersAvail;
    document.getElementById('finish-panel').classList.add('hidden');
    document.getElementById('btn-pause').disabled  = false;
    document.getElementById('btn-resume').disabled = true;

    updateSpeedLabel();
    document.getElementById('speed-range').value = speedIdx;
    document.getElementById('speed-range').oninput = () => {
      speedIdx = parseInt(document.getElementById('speed-range').value);
      updateSpeedLabel();
    };

    if (stompClient) stompClient.deactivate();
    stompClient = new StompJs.Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        stompClient.subscribe(`/topic/session/${sessionId}/events`, msg => {
          eventQueue.push(JSON.parse(msg.body));
          if (!animating) drainQueue();
        });
      }
    });
    stompClient.activate();

    drawMap();
    drawRobot(robotX, robotY);

    document.getElementById('btn-pause').onclick  = () => Api.pause(sessionId);
    document.getElementById('btn-resume').onclick = () => Api.resume(sessionId)
      .then(() => { document.getElementById('btn-resume').disabled = true;
                    document.getElementById('btn-pause').disabled  = false; });
    document.getElementById('btn-stop').onclick   = () => Api.stop(sessionId);
    document.getElementById('btn-save-lb').onclick = saveLb;

    Api.pause = (id) => fetch(`/api/session/${id}/pause`, {method:'POST'}).then(r => r.json())
      .then(() => { document.getElementById('btn-pause').disabled  = true;
                    document.getElementById('btn-resume').disabled = false; });
  }

  function drainQueue() {
    if (eventQueue.length === 0) { animating = false; return; }
    animating = true;
    const ev  = eventQueue.shift();
    applyEvent(ev);
    setTimeout(drainQueue, SPEEDS[speedIdx]);
  }

  function applyEvent(ev) {
    sessionTrace.push(ev);
    robotX    = ev.robotX;
    robotY    = ev.robotY;
    score     = ev.score;
    cleaned   = ev.totalCleaned;
    itersUsed = ev.iteration;
    cleanedSet.add(ev.robotX + ',' + ev.robotY);

    drawMap();
    drawRobot(robotX, robotY);

    document.getElementById('score').textContent    = score;
    document.getElementById('cleaned').textContent  = cleaned;
    document.getElementById('iters-used').textContent = itersUsed;

    if (ev.finished) {
      finished = true;
      document.getElementById('finish-panel').classList.remove('hidden');
      document.getElementById('final-score').textContent = score;
      document.getElementById('btn-pause').disabled  = true;
      document.getElementById('btn-resume').disabled = true;
    }
  }

  function drawMap() {
    const canvas = document.getElementById('game-canvas');
    const ctx    = canvas.getContext('2d');
    ctx.fillStyle = '#0a0a16';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    for (const t of mapData.tiles) {
      const px = t.x * CELL, py = t.y * CELL;
      ctx.fillStyle = cleanedSet.has(t.x + ',' + t.y) ? '#e8f4e8' : '#2a2a4a';
      ctx.fillRect(px + 1, py + 1, CELL - 2, CELL - 2);

      ctx.strokeStyle = '#0a0a16';
      ctx.lineWidth   = 2;
      if (t.wallNorth) { ctx.beginPath(); ctx.moveTo(px, py);       ctx.lineTo(px+CELL, py);       ctx.stroke(); }
      if (t.wallSouth) { ctx.beginPath(); ctx.moveTo(px, py+CELL);  ctx.lineTo(px+CELL, py+CELL);  ctx.stroke(); }
      if (t.wallWest)  { ctx.beginPath(); ctx.moveTo(px, py);       ctx.lineTo(px, py+CELL);       ctx.stroke(); }
      if (t.wallEast)  { ctx.beginPath(); ctx.moveTo(px+CELL, py);  ctx.lineTo(px+CELL, py+CELL);  ctx.stroke(); }
    }
  }

  function drawRobot(x, y) {
    const canvas = document.getElementById('game-canvas');
    const ctx    = canvas.getContext('2d');
    ctx.font      = Math.floor(CELL * 0.8) + 'px serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(setupBody.avatar || '🤖', x * CELL + CELL/2, y * CELL + CELL/2);
  }

  function updateSpeedLabel() {
    const labels = ['×0.1','×0.25','×0.5','×1','×2','×max'];
    document.getElementById('speed-label').textContent = labels[speedIdx] || '';
  }

  async function saveLb() {
    const entry = {
      id:                  sessionId,
      username:            setupBody.username,
      avatar:              setupBody.avatar,
      mapHash:             setupBody.hash || sessionId,
      mapSize:             setupBody.size,
      algoName:            setupBody.algoName,
      iterationsUsed:      itersUsed,
      iterationsAvailable: itersAvail,
      score:               score,
      completedAt:         new Date().toISOString(),
      trace:               sessionTrace.map(e => ({
        iteration: e.iteration, direction: e.direction,
        x: e.robotX, y: e.robotY, score: e.score
      })),
    };
    await Api.saveToLb(entry);
    alert('Saved!');
  }

  return { init };
})();
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/js/game.js
git commit -m "feat: game screen with canvas map, WebSocket event stream, animation loop"
```

---

### Task 19: Leaderboard screen

**Files:**
- Create: `src/main/resources/static/js/leaderboard.js`

- [ ] **Step 1: Create leaderboard.js**

```javascript
const Leaderboard = (() => {
  async function load() {
    const entries = await Api.leaderboard().catch(() => null);
    const tbody   = document.getElementById('lb-body');
    const empty   = document.getElementById('lb-empty');
    if (!entries || entries.length === 0) {
      tbody.innerHTML = '';
      empty.classList.remove('hidden');
      return;
    }
    empty.classList.add('hidden');
    const sorted = [...entries].sort((a, b) => b.score - a.score);
    tbody.innerHTML = sorted.map((e, i) => `
      <tr>
        <td>${i + 1}</td>
        <td>${e.avatar}</td>
        <td>${e.username}</td>
        <td><code>${(e.mapHash || '').slice(0, 8)}</code></td>
        <td>${e.mapSize}</td>
        <td>${e.algoName}</td>
        <td>${e.iterationsUsed} / ${e.iterationsAvailable}</td>
        <td><strong>${e.score}</strong></td>
        <td>
          <button onclick="Leaderboard.replay(${i})">Replay</button>
          <button onclick="Leaderboard.retry(${i})">Retry</button>
        </td>
      </tr>`).join('');
    window._lbEntries = sorted;
  }

  async function replay(idx) {
    const e = window._lbEntries[idx];
    const session = await Api.createSession({
      hash: e.mapHash, size: e.mapSize,
      algoName: e.algoName, username: e.username,
      avatar: e.avatar, iterations: e.iterationsAvailable,
    });
    const fakeBody = { hash: e.mapHash, size: e.mapSize, algoName: e.algoName,
                       username: e.username, avatar: e.avatar };
    Game.initReplay(session, fakeBody, e.trace);
    App.show('game');
  }

  async function retry(idx) {
    const e       = window._lbEntries[idx];
    // Pre-fill setup form and switch to setup
    document.getElementById('map-hash').value  = e.mapHash;
    document.getElementById('map-size').value  = e.mapSize;
    const ITER_VALUES = [250, 500, 1000, 2000, 5000];
    const iIdx = ITER_VALUES.indexOf(e.iterationsAvailable);
    if (iIdx >= 0) document.getElementById('iterations-range').value = iIdx;
    App.show('setup');
  }

  return { load, replay, retry };
})();
```

- [ ] **Step 2: Add `initReplay` to game.js**

Add this function inside the `Game` module (after the `init` function):

```javascript
function initReplay(session, body, trace) {
  // Set up the game screen identically to init, but feed trace as pre-loaded queue
  init(session, body);
  // Override: disconnect stomp and feed trace events directly
  setTimeout(() => {
    if (stompClient) stompClient.deactivate();
    eventQueue = trace.map(t => ({
      robotX: t.x, robotY: t.y, score: t.score,
      iteration: t.iteration, direction: t.direction,
      totalCleaned: 0, totalFloor: mapData.totalFloor,
      finished: t.iteration === trace.length,
      finishReason: t.iteration === trace.length ? 'COMPLETED' : null,
    }));
    drainQueue();
  }, 500);
}
```

Also expose it: change `return { init };` to `return { init, initReplay };`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/js/
git commit -m "feat: leaderboard screen with replay + retry, Game.initReplay for trace playback"
```

---

## Phase 9 — Integration + Final Polish

### Task 20: Run full integration smoke test

- [ ] **Step 1: Run all tests**

```bash
./mvnw test
```
Expected: All tests pass, BUILD SUCCESS.

- [ ] **Step 2: Start application and manual smoke test**

```bash
./mvnw spring-boot:run
```
Open `http://localhost:8080` in a browser.
- [ ] Setup screen loads, algos appear in dropdown, avatars in picker.
- [ ] Click "Start Game" → game screen appears, canvas renders, robot moves.
- [ ] Pause/Resume/Stop buttons work.
- [ ] After game finishes, "Save to Leaderboard" button appears (if `leaderboard.file` is set).

- [ ] **Step 3: Commit**

```bash
git commit --allow-empty -m "chore: all tests green, manual smoke test passed"
```

---

### Task 21: CLAUDE.md + memory + final docs commit

**Files:**
- Create: `CLAUDE.md`

- [ ] **Step 1: Create CLAUDE.md**

```markdown
# Jacuum Cleaner — Developer Notes

## Running the app
```bash
./mvnw spring-boot:run
# or with leaderboard file:
./mvnw spring-boot:run -Dspring-boot.run.arguments="--leaderboard.file=./leaderboard.json"
```
Open http://localhost:8080

## Running tests
```bash
./mvnw test
```

## Adding a new algorithm
1. Create a class in `src/main/java/com/jacuum/algo/impl/`
2. Implement `com.jacuum.algo.RobotAlgo`
3. Annotate with `@RobotAlgorithm("My Algo Name")`
4. It will automatically appear in the UI dropdown

## Key packages
- `com.jacuum.algo` — public interfaces (Tile, RobotAlgo, Direction, @RobotAlgorithm)
- `com.jacuum.map` — map generation (CellularMaps)
- `com.jacuum.engine` — simulation loop (MemorySessions)
- `com.jacuum.leaderboard` — file-backed leaderboard
- `com.jacuum.web` — REST controllers + WebSocket config
- `src/main/resources/static/` — frontend SPA

## Architecture
- Virtual thread per session runs the game loop
- STOMP over SockJS pushes IterationEvents to the browser
- No database; leaderboard is optional JSON file
```

- [ ] **Step 2: Final commit**

```bash
git add CLAUDE.md docs/
git commit -m "docs: CLAUDE.md developer guide, spec, stack, and implementation plan"
```

---

## Self-Review

**Spec coverage check:**
- [x] Map generation with hash + size preset → Task 5
- [x] Seeded determinism → CellularMapsTest
- [x] All tiles reachable → CellularMapsTest
- [x] Starting position → centroid of largest region
- [x] RobotAlgo interface with `next(Tile)` → Task 3
- [x] `@RobotAlgorithm` annotation + auto-discovery → Tasks 3, 8
- [x] RandomAlgo + AlwaysLeftAlgo → Task 9
- [x] Algo exception = ALGO_CRASH + score 0 → GameLoopTest + Task 8
- [x] Session lifecycle (SETUP → RUNNING → PAUSED → FINISHED) → Tasks 7–8
- [x] WebSocket STOMP streaming → Tasks 10, 18
- [x] Score = cleaned tiles × 100 − iterations → ActiveSession.score
- [x] REST API (create, start, pause, resume, stop) → Tasks 11–12
- [x] Leaderboard JSON file, optional → Tasks 13–14
- [x] Replay from trace → Task 19 (Game.initReplay)
- [x] Retry with same map/iterations → Task 19 (Leaderboard.retry)
- [x] Avatar picker → Tasks 12, 17
- [x] Username with Faker fallback → Task 17 (HEROES list; server-side Faker in AlgosEndpoint can extend)
- [x] localStorage prefs → Task 17
- [x] Speed control → Task 18
- [x] Tests for map, engine, algos, leaderboard, REST → spread across tasks
- [x] Single `./mvnw spring-boot:run` entry point → Task 1

**Scoring note:** Spec says "cleaned tiles bring points, lower iterations = better". The plan implements `score = cleanedTiles × 100; finalScore = score - iterationsUsed`. This matches the spec intent. Minimum score is 0 (clamped in display if needed).
