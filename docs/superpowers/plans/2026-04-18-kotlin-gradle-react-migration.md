# Kotlin/Gradle/React Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate Jacuum Cleaner from Java/Maven/Vanilla JS to Kotlin/Gradle/React+TypeScript in three sequential phases, each independently buildable and runnable.

**Architecture:** Three feature branches off `claude-powered`: Phase 1 replaces the build system (Maven → Gradle + Node plugin), Phase 2 rewrites all backend Java to Kotlin with coroutines, Phase 3 replaces the vanilla JS frontend with a Vite/React/TypeScript SPA. Each phase ends with all tests passing.

**Tech Stack:** Kotlin 2.x, Spring Boot 3.3.x, Gradle 8.x (Kotlin DSL), kotlinx-coroutines, React 18, TypeScript 5, Vite 5, Vitest 2, React Testing Library, @stomp/stompjs

**Branch convention:** `feature/cld-pow-p1-gradle`, `feature/cld-pow-p2-kotlin`, `feature/cld-pow-p3-react` — all branch from and merge back to `claude-powered`.

---

## FILE MAP

### Phase 1 — created
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`

### Phase 1 — deleted
- `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`

### Phase 2 — created (Kotlin replacements, delete paired .java)
```
src/main/kotlin/com/jacuum/
  algo/  Direction.kt  Tile.kt  RobotAlgo.kt  RobotAlgorithm.kt
         Algorithms.kt  SpringAlgorithms.kt
         impl/  RandomAlgo.kt  AlwaysLeftAlgo.kt  FrontierAlgo.kt
  map/   SizePreset.kt  GameMap.kt  Maps.kt  GeneratedMap.kt  CellularMaps.kt
  leaderboard/  Leaderboard.kt  TraceEvent.kt  LeaderboardEntry.kt
                SilentLeaderboard.kt  JsonLeaderboard.kt
  engine/  RunStatus.kt  FinishReason.kt  Messaging.kt  SilentMessaging.kt
           ActiveMessaging.kt  StatusEvent.kt  IterationEvent.kt
           SessionView.kt  Sessions.kt  SessionTile.kt  ActiveSession.kt
           MemorySessions.kt
  web/  dto/CreateSessionRequest.kt  dto/MapSnapshot.kt  dto/SessionResponse.kt
        SessionApi.kt  AlgosApi.kt  LeaderboardApi.kt  Config.kt  Snapshots.kt
        WebSocketConfig.kt  AppConfig.kt  GameMapSnapshots.kt
        SessionEndpoint.kt  AlgosEndpoint.kt  LeaderboardEndpoint.kt
  JacuumApplication.kt

src/test/kotlin/com/jacuum/   (mirrors above — all .java test files converted)
```

### Phase 3 — created
```
src/main/frontend/
  index.html  package.json  vite.config.ts  tsconfig.json
  src/
    main.tsx  types.ts  api.ts  GameLogic.ts  App.tsx
    SetupScreen.tsx  GameScreen.tsx  LeaderboardScreen.tsx
    test-setup.ts
    __tests__/  api.test.ts  GameLogic.test.ts
```

### Phase 3 — deleted
- `src/main/resources/static/js/`
- `src/main/resources/static/lib/`
- `src/main/resources/static/index.html`
- `src/test/javascript/`
- root `package.json`, `jest.config.js`, `package-lock.json`

---

# PHASE 1 — Maven → Gradle

---

### Task 1.1: Create `settings.gradle.kts`

**Files:** Create `settings.gradle.kts`

- [x] Create `settings.gradle.kts`:
```kotlin
rootProject.name = "jacuum-cleaner"
```

---

### Task 1.2: Create `build.gradle.kts`

**Files:** Create `build.gradle.kts`

- [x] Create `build.gradle.kts`:
```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.jacuum"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

node {
    version = "20.17.0"
    download = true
    workDir = file("${project.projectDir}/.gradle/nodejs")
    nodeProjectDir = file("${project.projectDir}")
}

val npmRunTest by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    args = listOf("test")
    dependsOn(tasks.named("npmInstall"))
}

tasks.test {
    useJUnitPlatform()
    dependsOn(npmRunTest)
}
```

---

### Task 1.3: Generate Gradle wrapper

**Files:** `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [x] Ensure Gradle 8.x is installed locally, then run:
```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```
- [x] Verify these files now exist:
```
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradlew
gradlew.bat
```
- [x] Confirm `gradle/wrapper/gradle-wrapper.properties` contains:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
```

---

### Task 1.4: Delete Maven files

**Files:** Delete `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`

- [x] Delete the files:
```bash
rm pom.xml mvnw mvnw.cmd
rm -rf .mvn
```

---

### Task 1.5: Validate and commit Phase 1

- [x] Run tests:
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL — all Java JUnit tests pass, Jest tests pass.

- [x] Commit:
```bash
git add -A
git commit -m "build: replace Maven with Gradle + Node plugin (Phase 1)"
```

- [x] Push and open PR from `feature/cld-pow-p1-gradle` → `claude-powered`, merge when green.

---

# PHASE 2 — Java → Kotlin

Create branch `feature/cld-pow-p2-kotlin` from `claude-powered` after Phase 1 merges.

---

### Task 2.0: Add Kotlin to `build.gradle.kts`

**Files:** Modify `build.gradle.kts`

- [x] Replace the `plugins` block:
```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.node-gradle.node") version "7.1.0"
}
```

- [x] Replace the `java { toolchain {...} }` block:
```kotlin
kotlin {
    jvmToolchain(21)
}
```

- [x] Add to `dependencies`:
```kotlin
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
```

- [x] Run `./gradlew test` — must still pass before any `.java` files are touched.

---

### Task 2.1: Convert `com.jacuum.algo` package

**Files:**
- Delete: all 6 files in `src/main/java/com/jacuum/algo/` (not `impl/`)
- Create: 6 files in `src/main/kotlin/com/jacuum/algo/`
- Delete: `src/test/java/com/jacuum/algo/DirectionTest.java`, `AlgoSmokeTest.java`
- Create: `src/test/kotlin/com/jacuum/algo/DirectionTest.kt`, `AlgoSmokeTest.kt`

- [ ] Delete `src/main/java/com/jacuum/algo/Direction.java` and create `src/main/kotlin/com/jacuum/algo/Direction.kt`:
```kotlin
package com.jacuum.algo

enum class Direction {
    NORTH, SOUTH, EAST, WEST;

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH; SOUTH -> NORTH; EAST -> WEST; WEST -> EAST
    }
    fun dx(): Int = when (this) { EAST -> 1; WEST -> -1; else -> 0 }
    fun dy(): Int = when (this) { SOUTH -> 1; NORTH -> -1; else -> 0 }
}
```

- [ ] Delete `Tile.java`, create `src/main/kotlin/com/jacuum/algo/Tile.kt`:
```kotlin
package com.jacuum.algo

interface Tile {
    fun x(): Int
    fun y(): Int
    fun isClean(): Boolean
    fun hasWall(direction: Direction): Boolean
}
```

- [ ] Delete `RobotAlgo.java`, create `src/main/kotlin/com/jacuum/algo/RobotAlgo.kt`:
```kotlin
package com.jacuum.algo

interface RobotAlgo {
    @Throws(Exception::class)
    fun next(tile: Tile): Direction
}
```

- [ ] Delete `RobotAlgorithm.java`, create `src/main/kotlin/com/jacuum/algo/RobotAlgorithm.kt`:
```kotlin
package com.jacuum.algo

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

@Component
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RobotAlgorithm(
    @get:AliasFor(annotation = Component::class)
    val value: String = ""
)
```

- [ ] Delete `Algorithms.java`, create `src/main/kotlin/com/jacuum/algo/Algorithms.kt`:
```kotlin
package com.jacuum.algo

interface Algorithms {
    fun names(): List<String>
    @Throws(Exception::class)
    fun instantiate(name: String): RobotAlgo
}
```

- [ ] Delete `SpringAlgorithms.java`, create `src/main/kotlin/com/jacuum/algo/SpringAlgorithms.kt`:
```kotlin
package com.jacuum.algo

import org.springframework.context.ApplicationContext

class SpringAlgorithms(private val ctx: ApplicationContext) : Algorithms {

    private val beans: Map<String, Any> =
        ctx.getBeansWithAnnotation(RobotAlgorithm::class.java).toMap()

    override fun names(): List<String> = beans.values.map { displayName(it) }.sorted()

    @Throws(Exception::class)
    override fun instantiate(name: String): RobotAlgo {
        val entry = beans.entries.find { displayName(it.value) == name }
            ?: throw Exception("Unknown algorithm: $name")
        return ctx.getBean(entry.key) as RobotAlgo
    }

    private fun displayName(bean: Any): String {
        val ann = bean.javaClass.getAnnotation(RobotAlgorithm::class.java)
        return ann.value.ifBlank { bean.javaClass.simpleName }
    }
}
```

- [ ] Delete `DirectionTest.java`, create `src/test/kotlin/com/jacuum/algo/DirectionTest.kt`:
```kotlin
package com.jacuum.algo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DirectionTest {
    @Test fun oppositeOfNorthIsSouth() = assertThat(Direction.NORTH.opposite()).isEqualTo(Direction.SOUTH)
    @Test fun dxOfEastIsOne() = assertThat(Direction.EAST.dx()).isEqualTo(1)
    @Test fun dyOfSouthIsOne() = assertThat(Direction.SOUTH.dy()).isEqualTo(1)
    @Test fun allDirectionsHaveUniqueOffsets() {
        for (d in Direction.entries) assertThat(d.dx() * d.dx() + d.dy() * d.dy()).isEqualTo(1)
    }
    @Test fun oppositeOfSouthIsNorth() = assertThat(Direction.SOUTH.opposite()).isEqualTo(Direction.NORTH)
    @Test fun oppositeOfEastIsWest() = assertThat(Direction.EAST.opposite()).isEqualTo(Direction.WEST)
    @Test fun oppositeOfWestIsEast() = assertThat(Direction.WEST.opposite()).isEqualTo(Direction.EAST)
    @Test fun dxOfWestIsMinusOne() = assertThat(Direction.WEST.dx()).isEqualTo(-1)
    @Test fun dyOfNorthIsMinusOne() = assertThat(Direction.NORTH.dy()).isEqualTo(-1)
    @Test fun doubleOppositeIsIdentity() {
        for (d in Direction.entries) assertThat(d.opposite().opposite()).isEqualTo(d)
    }
}
```

- [x] Read `src/test/java/com/jacuum/algo/AlgoSmokeTest.java`, then delete it and create the Kotlin equivalent in `src/test/kotlin/com/jacuum/algo/AlgoSmokeTest.kt` with the same test logic using Kotlin property access on any data objects.

- [x] Run `./gradlew test` — must pass.

- [x] Commit:
```bash
git commit -m "refactor: convert com.jacuum.algo to Kotlin"
```

---

### Task 2.2: Convert `com.jacuum.map` package

**Files:**
- Delete: 5 files in `src/main/java/com/jacuum/map/`
- Create: 5 files in `src/main/kotlin/com/jacuum/map/`
- Delete: `GeneratedMapTest.java`, `CellularMapsTest.java`
- Create: Kotlin equivalents

- [ ] Delete `SizePreset.java`, create `src/main/kotlin/com/jacuum/map/SizePreset.kt`:
```kotlin
package com.jacuum.map

enum class SizePreset(val width: Int, val height: Int) {
    TINY(10, 8), SMALL(16, 12), MEDIUM(24, 18), LARGE(34, 26)
}
```

- [ ] Delete `GameMap.java`, create `src/main/kotlin/com/jacuum/map/GameMap.kt`:
```kotlin
package com.jacuum.map

import com.jacuum.algo.Direction

interface GameMap {
    fun hash(): String
    fun size(): SizePreset
    fun width(): Int
    fun height(): Int
    fun isFloor(x: Int, y: Int): Boolean
    fun hasWall(x: Int, y: Int, direction: Direction): Boolean
    fun startX(): Int
    fun startY(): Int
    fun totalFloorTiles(): Int
}
```

- [ ] Delete `Maps.java`, create `src/main/kotlin/com/jacuum/map/Maps.kt`:
```kotlin
package com.jacuum.map

interface Maps {
    @Throws(Exception::class)
    fun generate(hash: String, size: SizePreset): GameMap
}
```

- [ ] Delete `GeneratedMap.java`, create `src/main/kotlin/com/jacuum/map/GeneratedMap.kt`:
```kotlin
package com.jacuum.map

import com.jacuum.algo.Direction

class GeneratedMap(
    private val hash: String,
    private val size: SizePreset,
    private val floor: Array<BooleanArray>,
    private val startX: Int,
    private val startY: Int
) : GameMap {
    private val totalFloor: Int = floor.sumOf { row -> row.count { it } }

    override fun hash(): String = hash
    override fun size(): SizePreset = size
    override fun width(): Int = floor[0].size
    override fun height(): Int = floor.size
    override fun isFloor(x: Int, y: Int): Boolean = inBounds(x, y) && floor[y][x]
    override fun startX(): Int = startX
    override fun startY(): Int = startY
    override fun totalFloorTiles(): Int = totalFloor

    override fun hasWall(x: Int, y: Int, direction: Direction): Boolean {
        val nx = x + direction.dx()
        val ny = y + direction.dy()
        return !inBounds(nx, ny) || !floor[ny][nx]
    }

    private fun inBounds(x: Int, y: Int) = x >= 0 && y >= 0 && x < width() && y < height()
}
```

- [ ] Delete `CellularMaps.java`, create `src/main/kotlin/com/jacuum/map/CellularMaps.kt`:
```kotlin
package com.jacuum.map

import com.jacuum.algo.Direction
import java.util.ArrayDeque
import java.util.Random

class CellularMaps : Maps {

    private val smoothingPasses = 5
    private val fillRatio = 0.45

    override fun generate(hash: String, size: SizePreset): GameMap {
        val rng = Random(seedFrom(hash))
        val w = size.width; val h = size.height
        var floor = initialFloor(rng, w, h)
        repeat(smoothingPasses) { floor = smooth(floor, w, h) }
        floor = keepLargestRegion(floor, w, h)
        val (sx, sy) = centroidOfFloor(floor, w, h)
        return GeneratedMap(hash, size, floor, sx, sy)
    }

    private fun seedFrom(hash: String): Long {
        var h = -3750763034362895579L
        for (c in hash) h = (h xor c.code.toLong()) * 1099511628211L
        return h
    }

    private fun initialFloor(rng: Random, w: Int, h: Int): Array<BooleanArray> {
        val f = Array(h) { BooleanArray(w) }
        for (y in 1 until h - 1)
            for (x in 1 until w - 1)
                f[y][x] = rng.nextDouble() > fillRatio
        return f
    }

    private fun smooth(f: Array<BooleanArray>, w: Int, h: Int): Array<BooleanArray> {
        val next = Array(h) { BooleanArray(w) }
        for (y in 1 until h - 1)
            for (x in 1 until w - 1) {
                var walls = 0
                for (dy in -1..1) for (dx in -1..1) if (!f[y + dy][x + dx]) walls++
                next[y][x] = walls < 5
            }
        return next
    }

    private fun keepLargestRegion(floor: Array<BooleanArray>, w: Int, h: Int): Array<BooleanArray> {
        val visited = Array(h) { BooleanArray(w) }
        val regions = mutableListOf<List<Pair<Int, Int>>>()
        for (y in 0 until h) for (x in 0 until w) {
            if (floor[y][x] && !visited[y][x]) {
                val region = mutableListOf<Pair<Int, Int>>()
                val q = ArrayDeque<Pair<Int, Int>>()
                q.add(x to y); visited[y][x] = true
                while (q.isNotEmpty()) {
                    val (cx, cy) = q.poll(); region.add(cx to cy)
                    for (d in Direction.entries) {
                        val nx = cx + d.dx(); val ny = cy + d.dy()
                        if (nx in 0 until w && ny in 0 until h && floor[ny][nx] && !visited[ny][nx]) {
                            visited[ny][nx] = true; q.add(nx to ny)
                        }
                    }
                }
                regions.add(region)
            }
        }
        if (regions.isEmpty()) {
            val fallback = Array(h) { BooleanArray(w) }
            val cx = w / 2; val cy = h / 2
            for (dy in -1..1) for (dx in -1..1) fallback[cy + dy][cx + dx] = true
            return fallback
        }
        val largest = regions.maxByOrNull { it.size }!!
        val result = Array(h) { BooleanArray(w) }
        for ((rx, ry) in largest) result[ry][rx] = true
        return result
    }

    private fun centroidOfFloor(floor: Array<BooleanArray>, w: Int, h: Int): Pair<Int, Int> {
        var sx = 0L; var sy = 0L; var count = 0L
        for (y in 0 until h) for (x in 0 until w) if (floor[y][x]) { sx += x; sy += y; count++ }
        if (count == 0L) return w / 2 to h / 2
        val cx = (sx / count).toInt(); val cy = (sy / count).toInt()
        var best = Int.MAX_VALUE; var result = cx to cy
        for (y in 0 until h) for (x in 0 until w) if (floor[y][x]) {
            val dist = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            if (dist < best) { best = dist; result = x to y }
        }
        return result
    }
}
```

- [x] Read `src/test/java/com/jacuum/map/GeneratedMapTest.java` and `CellularMapsTest.java`, then delete them and create Kotlin equivalents in `src/test/kotlin/com/jacuum/map/` with the same logic using `map.startX()` method-call style (these are interface methods, not Kotlin properties, so no change needed).

- [x] Run `./gradlew test` — must pass.

- [x] Commit:
```bash
git commit -m "refactor: convert com.jacuum.map to Kotlin"
```

---

### Task 2.3: Convert `com.jacuum.leaderboard` package

**Files:**
- Delete: 5 files in `src/main/java/com/jacuum/leaderboard/`
- Create: 5 files in `src/main/kotlin/com/jacuum/leaderboard/`
- Delete/convert: `JsonLeaderboardTest.java`

- [ ] Delete `TraceEvent.java`, create `src/main/kotlin/com/jacuum/leaderboard/TraceEvent.kt`:
```kotlin
package com.jacuum.leaderboard

import com.jacuum.algo.Direction

data class TraceEvent(
    val iteration: Int,
    val direction: Direction?,
    val x: Int,
    val y: Int,
    val score: Int
)
```

- [ ] Delete `LeaderboardEntry.java`, create `src/main/kotlin/com/jacuum/leaderboard/LeaderboardEntry.kt`:
```kotlin
package com.jacuum.leaderboard

data class LeaderboardEntry(
    val id: String,
    val username: String,
    val avatar: String,
    val mapHash: String,
    val mapSize: String,
    val algoName: String,
    val iterationsUsed: Int,
    val iterationsAvailable: Int,
    val score: Int,
    val completedAt: String,
    val trace: List<TraceEvent>
)
```

- [ ] Delete `Leaderboard.java`, create `src/main/kotlin/com/jacuum/leaderboard/Leaderboard.kt`:
```kotlin
package com.jacuum.leaderboard

interface Leaderboard {
    @Throws(Exception::class)
    fun entries(): List<LeaderboardEntry>
    @Throws(Exception::class)
    fun save(entry: LeaderboardEntry)
}
```

- [ ] Delete `SilentLeaderboard.java`, create `src/main/kotlin/com/jacuum/leaderboard/SilentLeaderboard.kt`:
```kotlin
package com.jacuum.leaderboard

class SilentLeaderboard : Leaderboard {
    override fun entries(): List<LeaderboardEntry> = emptyList()
    override fun save(entry: LeaderboardEntry) {}
}
```

- [ ] Delete `JsonLeaderboard.java`, create `src/main/kotlin/com/jacuum/leaderboard/JsonLeaderboard.kt`:
```kotlin
package com.jacuum.leaderboard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

class JsonLeaderboard(private val file: Path) : Leaderboard {

    private val mapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)

    @Synchronized
    override fun entries(): List<LeaderboardEntry> {
        if (!Files.exists(file)) return emptyList()
        return mapper.readValue(file.toFile())
    }

    @Synchronized
    override fun save(entry: LeaderboardEntry) {
        val current = entries().toMutableList()
        current.add(entry)
        mapper.writeValue(file.toFile(), current)
    }
}
```

- [x] Read `src/test/java/com/jacuum/leaderboard/JsonLeaderboardTest.java`, delete it, create `src/test/kotlin/com/jacuum/leaderboard/JsonLeaderboardTest.kt` with the same test logic. Note: `LeaderboardEntry` is now a Kotlin `data class` — in the Kotlin test, access fields as properties (`entry.id`, `entry.score`) not method calls.

- [x] Run `./gradlew test` — must pass.

- [x] Commit:
```bash
git commit -m "refactor: convert com.jacuum.leaderboard to Kotlin"
```

---

### Task 2.4: Convert `com.jacuum.engine` package (+ coroutines)

This is the most complex conversion. The game loop virtual thread becomes a coroutine. Pause/resume uses a `Mutex` as a gate.

**Files:**
- Delete: all 12 files in `src/main/java/com/jacuum/engine/`
- Create: 12 files in `src/main/kotlin/com/jacuum/engine/`
- Delete/convert: `GameLoopTest.java`, `MemorySessionsTest.java`

- [ ] Create `src/main/kotlin/com/jacuum/engine/RunStatus.kt`:
```kotlin
package com.jacuum.engine
enum class RunStatus { SETUP, RUNNING, PAUSED, FINISHED }
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/FinishReason.kt`:
```kotlin
package com.jacuum.engine
enum class FinishReason { COMPLETED, OUT_OF_ITERATIONS, ALGO_CRASH, INTERRUPTED }
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/Messaging.kt`:
```kotlin
package com.jacuum.engine
interface Messaging {
    fun send(destination: String, payload: Any)
}
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/SilentMessaging.kt`:
```kotlin
package com.jacuum.engine
internal class SilentMessaging : Messaging {
    override fun send(destination: String, payload: Any) {}
}
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/ActiveMessaging.kt`:
```kotlin
package com.jacuum.engine
import org.springframework.messaging.simp.SimpMessagingTemplate

class ActiveMessaging(private val delegate: SimpMessagingTemplate) : Messaging {
    override fun send(destination: String, payload: Any) {
        delegate.convertAndSend(destination, payload)
    }
}
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/StatusEvent.kt`:
```kotlin
package com.jacuum.engine
data class StatusEvent(val sessionId: String, val status: RunStatus, val finishReason: FinishReason?)
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/IterationEvent.kt`:
```kotlin
package com.jacuum.engine
import com.jacuum.algo.Direction

data class IterationEvent(
    val sessionId: String,
    val iteration: Int,
    val direction: Direction?,
    val robotX: Int,
    val robotY: Int,
    val score: Int,
    val totalCleaned: Int,
    val totalFloor: Int,
    val finished: Boolean,
    val finishReason: FinishReason?
)
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/SessionView.kt`:
```kotlin
package com.jacuum.engine
data class SessionView(
    val id: String,
    val status: RunStatus,
    val robotX: Int,
    val robotY: Int,
    val score: Int,
    val totalCleaned: Int,
    val iterationsUsed: Int,
    val iterationsAvailable: Int,
    val totalFloor: Int,
    val finishReason: FinishReason?
)
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/Sessions.kt`:
```kotlin
package com.jacuum.engine
import com.jacuum.map.GameMap

interface Sessions {
    @Throws(Exception::class)
    fun open(map: GameMap, algoName: String, username: String, avatar: String, iterations: Int): String
    @Throws(Exception::class) fun start(id: String)
    @Throws(Exception::class) fun pause(id: String)
    @Throws(Exception::class) fun resume(id: String)
    @Throws(Exception::class) fun stop(id: String)
    @Throws(Exception::class) fun view(id: String): SessionView
}
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/SessionTile.kt`:
```kotlin
package com.jacuum.engine
import com.jacuum.algo.Direction
import com.jacuum.algo.Tile

internal class SessionTile(
    private val x: Int,
    private val y: Int,
    private val map: com.jacuum.map.GameMap,
    private val cleaned: Set<String>
) : Tile {
    override fun x(): Int = x
    override fun y(): Int = y
    override fun isClean(): Boolean = cleaned.contains("$x,$y")
    override fun hasWall(direction: Direction): Boolean = map.hasWall(x, y, direction)
}
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/ActiveSession.kt`:
```kotlin
package com.jacuum.engine
import com.jacuum.map.GameMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

internal class ActiveSession(
    val id: String,
    val map: GameMap,
    val algoName: String,
    val username: String,
    val avatar: String,
    val iterationsAvailable: Int
) {
    @Volatile var robotX: Int = map.startX()
    @Volatile var robotY: Int = map.startY()
    @Volatile var score: Int = 0
    @Volatile var iterationsUsed: Int = 0
    @Volatile var status: RunStatus = RunStatus.SETUP
    @Volatile var finishReason: FinishReason? = null
    val cleaned: MutableSet<String> = ConcurrentHashMap.newKeySet()
    var job: Job? = null
    // Unlocked = running; locked = paused. Game loop calls withLock{} to suspend when paused.
    val pauseGate: Mutex = Mutex()

    fun toView() = SessionView(
        id, status, robotX, robotY, score,
        cleaned.size, iterationsUsed, iterationsAvailable,
        map.totalFloorTiles(), finishReason
    )
}
```

- [ ] Create `src/main/kotlin/com/jacuum/engine/MemorySessions.kt`:
```kotlin
package com.jacuum.engine
import com.jacuum.algo.Algorithms
import com.jacuum.algo.Direction
import com.jacuum.algo.RobotAlgo
import com.jacuum.map.GameMap
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MemorySessions(
    private val messaging: Messaging,
    private val algorithms: Algorithms,
    private val maxSessions: Int
) : Sessions {

    private val store = ConcurrentHashMap<String, ActiveSession>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PreDestroy
    fun destroy() = scope.cancel()

    override fun open(map: GameMap, algoName: String, username: String,
                      avatar: String, iterations: Int): String {
        if (store.size >= maxSessions)
            throw Exception("Session cap reached ($maxSessions max)")
        val id = UUID.randomUUID().toString()
        store[id] = ActiveSession(id, map, algoName, username, avatar, iterations)
        return id
    }

    override fun view(id: String): SessionView = require(id).toView()

    override fun start(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status != RunStatus.SETUP && s.status != RunStatus.PAUSED)
                throw Exception("Cannot start session in state: ${s.status}")
            s.status = RunStatus.RUNNING
        }
        val algo = algorithms.instantiate(s.algoName)
        s.job = scope.launch { runLoop(s, algo) }
    }

    private suspend fun runLoop(s: ActiveSession, algo: RobotAlgo) {
        while (s.iterationsUsed < s.iterationsAvailable && s.status != RunStatus.FINISHED) {
            ensureActive()
            s.pauseGate.withLock {} // suspends here while paused

            val tile = SessionTile(s.robotX, s.robotY, s.map, s.cleaned)
            val dir: Direction = try { algo.next(tile) } catch (e: Exception) {
                finish(s, FinishReason.ALGO_CRASH); return
            }
            val moved = !s.map.hasWall(s.robotX, s.robotY, dir)
            if (moved) { s.robotX += dir.dx(); s.robotY += dir.dy() }
            if (s.cleaned.add("${s.robotX},${s.robotY}")) s.score += 100
            s.iterationsUsed++

            messaging.send("/topic/session/${s.id}/events", IterationEvent(
                s.id, s.iterationsUsed, if (moved) dir else null,
                s.robotX, s.robotY, s.score,
                s.cleaned.size, s.map.totalFloorTiles(), false, null))

            if (s.cleaned.size == s.map.totalFloorTiles()) {
                finish(s, FinishReason.COMPLETED); return
            }
        }
        if (s.status != RunStatus.FINISHED) finish(s, FinishReason.OUT_OF_ITERATIONS)
    }

    private fun finish(s: ActiveSession, reason: FinishReason) {
        s.status = RunStatus.FINISHED
        s.finishReason = reason
        if (reason == FinishReason.ALGO_CRASH) s.score = 0
        messaging.send("/topic/session/${s.id}/events", IterationEvent(
            s.id, s.iterationsUsed, null,
            s.robotX, s.robotY, s.score,
            s.cleaned.size, s.map.totalFloorTiles(), true, reason))
        messaging.send("/topic/session/${s.id}/status",
            StatusEvent(s.id, RunStatus.FINISHED, reason))
    }

    override fun pause(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status != RunStatus.RUNNING)
                throw Exception("Cannot pause session in state: ${s.status}")
            s.status = RunStatus.PAUSED
        }
        s.pauseGate.tryLock() // takes the gate; loop suspends on next withLock{}
    }

    override fun resume(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status != RunStatus.PAUSED)
                throw Exception("Cannot resume session in state: ${s.status}")
            s.status = RunStatus.RUNNING
        }
        if (s.pauseGate.isLocked) s.pauseGate.unlock()
    }

    override fun stop(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status == RunStatus.FINISHED) return
            finish(s, FinishReason.INTERRUPTED)
        }
        if (s.pauseGate.isLocked) s.pauseGate.unlock()
        s.job?.cancel()
    }

    internal fun require(id: String): ActiveSession =
        Optional.ofNullable(store[id])
            .orElseThrow { Exception("Unknown session: $id") }
}
```

- [ ] Delete `GameLoopTest.java`, create `src/test/kotlin/com/jacuum/engine/GameLoopTest.kt`:
```kotlin
package com.jacuum.engine

import com.jacuum.algo.*
import com.jacuum.map.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameLoopTest {

    private fun eastAlways() = object : Algorithms {
        override fun names() = listOf("east")
        override fun instantiate(name: String) = RobotAlgo { Direction.EAST }
    }

    private fun corridor(): GameMap {
        val f = Array(3) { BooleanArray(7) }
        for (x in 1..5) f[1][x] = true
        return GeneratedMap("corridor", SizePreset.TINY, f, 1, 1)
    }

    @Test fun robotCleansCorridorMovingEast() {
        val sessions = MemorySessions(SilentMessaging(), eastAlways(), 50)
        val id = sessions.open(corridor(), "east", "Bot", "🤖", 20)
        sessions.start(id)

        val deadline = System.currentTimeMillis() + 2000
        while (sessions.view(id).status != RunStatus.FINISHED
               && System.currentTimeMillis() < deadline)
            Thread.sleep(50)

        val view = sessions.view(id)
        assertThat(view.status).isEqualTo(RunStatus.FINISHED)
        assertThat(view.totalCleaned).isGreaterThan(0)
        assertThat(view.score).isGreaterThan(0)
    }

    @Test fun algoCrashFinishesWithZeroScore() {
        val crashAlgo = object : Algorithms {
            override fun names() = listOf("crash")
            override fun instantiate(name: String) = RobotAlgo { throw RuntimeException("boom") }
        }
        val sessions = MemorySessions(SilentMessaging(), crashAlgo, 50)
        val id = sessions.open(corridor(), "crash", "Bot", "🤖", 20)
        sessions.start(id)
        val deadline = System.currentTimeMillis() + 2000
        while (sessions.view(id).status != RunStatus.FINISHED
               && System.currentTimeMillis() < deadline)
            Thread.sleep(50)
        assertThat(sessions.view(id).finishReason).isEqualTo(FinishReason.ALGO_CRASH)
    }
}
```

- [ ] Delete `MemorySessionsTest.java`, create `src/test/kotlin/com/jacuum/engine/MemorySessionsTest.kt`:
```kotlin
package com.jacuum.engine

import com.jacuum.map.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class MemorySessionsTest {

    private fun smallMap(): GameMap {
        val f = Array(3) { BooleanArray(3) { true } }
        return GeneratedMap("test", SizePreset.TINY, f, 1, 1)
    }

    @Test fun openCreatesSessionInSetupState() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        val id = sessions.open(smallMap(), "RandomAlgo", "Alice", "🤖", 100)
        assertThat(id).isNotBlank()
        val view = sessions.view(id)
        assertThat(view.status).isEqualTo(RunStatus.SETUP)
        assertThat(view.robotX).isEqualTo(1)
        assertThat(view.robotY).isEqualTo(1)
        assertThat(view.score).isEqualTo(0)
        assertThat(view.totalFloor).isEqualTo(9)
    }

    @Test fun viewThrowsForUnknownId() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        assertThatThrownBy { sessions.view("nope") }.isInstanceOf(Exception::class.java)
    }

    @Test fun sessionCapRejected() {
        val sessions = MemorySessions(SilentMessaging(), null, 2)
        sessions.open(smallMap(), "a", "Alice", "🤖", 100)
        sessions.open(smallMap(), "a", "Bob", "🤖", 100)
        assertThatThrownBy { sessions.open(smallMap(), "a", "Charlie", "🤖", 100) }
            .isInstanceOf(Exception::class.java)
            .hasMessageContaining("Session cap reached")
    }

    @Test fun pauseRequiresRunningState() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        val id = sessions.open(smallMap(), "a", "Alice", "🤖", 100)
        assertThatThrownBy { sessions.pause(id) }.isInstanceOf(Exception::class.java)
    }

    @Test fun resumeRequiresPausedState() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        val id = sessions.open(smallMap(), "a", "Alice", "🤖", 100)
        assertThatThrownBy { sessions.resume(id) }.isInstanceOf(Exception::class.java)
    }
}
```

- [x] Run `./gradlew test` — must pass.

- [x] Commit:
```bash
git commit -m "refactor: convert com.jacuum.engine to Kotlin with coroutines"
```

---

### Task 2.5: Convert `com.jacuum.web` package

**Files:**
- Delete: all files in `src/main/java/com/jacuum/web/` (including `dto/`)
- Create: Kotlin equivalents
- Delete/convert: all 3 web endpoint test files

- [ ] Create `src/main/kotlin/com/jacuum/web/dto/CreateSessionRequest.kt`:
```kotlin
package com.jacuum.web.dto
data class CreateSessionRequest(
    val hash: String?, val size: String?, val algoName: String?,
    val username: String?, val avatar: String?, val iterations: Int
)
```

- [ ] Create `src/main/kotlin/com/jacuum/web/dto/MapSnapshot.kt`:
```kotlin
package com.jacuum.web.dto
data class MapSnapshot(
    val width: Int, val height: Int, val startX: Int, val startY: Int,
    val totalFloor: Int, val tiles: List<TileSnapshot>
) {
    data class TileSnapshot(
        val x: Int, val y: Int,
        val wallNorth: Boolean, val wallSouth: Boolean,
        val wallEast: Boolean, val wallWest: Boolean
    )
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/dto/SessionResponse.kt`:
```kotlin
package com.jacuum.web.dto
data class SessionResponse(
    val sessionId: String, val status: String, val map: MapSnapshot,
    val robotX: Int, val robotY: Int, val totalFloor: Int, val iterationsAvailable: Int
)
```

- [ ] Create `src/main/kotlin/com/jacuum/web/SessionApi.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.engine.SessionView
import com.jacuum.web.dto.CreateSessionRequest
import com.jacuum.web.dto.SessionResponse

internal interface SessionApi {
    @Throws(Exception::class) fun create(req: CreateSessionRequest): SessionResponse
    @Throws(Exception::class) fun start(id: String): SessionView
    @Throws(Exception::class) fun pause(id: String): SessionView
    @Throws(Exception::class) fun resume(id: String): SessionView
    @Throws(Exception::class) fun stop(id: String): SessionView
    @Throws(Exception::class) fun view(id: String): SessionView
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/AlgosApi.kt`:
```kotlin
package com.jacuum.web
internal interface AlgosApi {
    fun algos(): List<String>
    fun avatars(): List<String>
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/LeaderboardApi.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.leaderboard.LeaderboardEntry
internal interface LeaderboardApi {
    @Throws(Exception::class) fun entries(): List<LeaderboardEntry>
    @Throws(Exception::class) fun save(entry: LeaderboardEntry): LeaderboardEntry
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/Config.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.algo.Algorithms
import com.jacuum.engine.Sessions
import com.jacuum.leaderboard.Leaderboard
import com.jacuum.map.Maps
import org.springframework.context.ApplicationContext
import org.springframework.messaging.simp.SimpMessagingTemplate

internal interface Config {
    fun maps(): Maps
    fun algorithms(ctx: ApplicationContext): Algorithms
    fun sessions(messaging: SimpMessagingTemplate, algorithms: Algorithms, maxSessions: Int): Sessions
    fun leaderboard(path: String): Leaderboard
    fun snapshots(): Snapshots
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/Snapshots.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.map.GameMap
import com.jacuum.web.dto.MapSnapshot

internal interface Snapshots {
    fun of(map: GameMap): MapSnapshot
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/WebSocketConfig.kt`:
```kotlin
package com.jacuum.web
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.*

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").withSockJS()
    }
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/GameMapSnapshots.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.algo.Direction
import com.jacuum.map.GameMap
import com.jacuum.web.dto.MapSnapshot

internal class GameMapSnapshots : Snapshots {
    override fun of(map: GameMap): MapSnapshot {
        val tiles = mutableListOf<MapSnapshot.TileSnapshot>()
        for (y in 0 until map.height()) for (x in 0 until map.width()) {
            if (map.isFloor(x, y)) tiles.add(MapSnapshot.TileSnapshot(x, y,
                map.hasWall(x, y, Direction.NORTH), map.hasWall(x, y, Direction.SOUTH),
                map.hasWall(x, y, Direction.EAST),  map.hasWall(x, y, Direction.WEST)))
        }
        return MapSnapshot(map.width(), map.height(),
            map.startX(), map.startY(), map.totalFloorTiles(), tiles)
    }
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/AppConfig.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.algo.Algorithms
import com.jacuum.algo.SpringAlgorithms
import com.jacuum.engine.ActiveMessaging
import com.jacuum.engine.MemorySessions
import com.jacuum.engine.Sessions
import com.jacuum.leaderboard.JsonLeaderboard
import com.jacuum.leaderboard.Leaderboard
import com.jacuum.leaderboard.SilentLeaderboard
import com.jacuum.map.CellularMaps
import com.jacuum.map.Maps
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.nio.file.Path

@Configuration(proxyBeanMethods = false)
class AppConfig : Config {
    @Bean override fun maps(): Maps = CellularMaps()
    @Bean override fun algorithms(ctx: ApplicationContext): Algorithms = SpringAlgorithms(ctx)
    @Bean override fun sessions(
        messaging: SimpMessagingTemplate,
        algorithms: Algorithms,
        @Value("\${game.max-sessions:50}") maxSessions: Int
    ): Sessions = MemorySessions(ActiveMessaging(messaging), algorithms, maxSessions)
    @Bean override fun leaderboard(@Value("\${leaderboard.file:}") path: String): Leaderboard =
        if (path.isBlank()) SilentLeaderboard() else JsonLeaderboard(Path.of(path))
    @Bean override fun snapshots(): Snapshots = GameMapSnapshots()
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/AlgosEndpoint.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.algo.Algorithms
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AlgosEndpoint(private val algorithms: Algorithms) : AlgosApi {
    @GetMapping("/algos")
    override fun algos(): List<String> = algorithms.names()
    @GetMapping("/avatars")
    override fun avatars(): List<String> =
        listOf("🤖", "🦾", "👾", "🚀", "🛸", "🦄", "🐢", "🦊", "🐱", "🐸")
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/LeaderboardEndpoint.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.leaderboard.Leaderboard
import com.jacuum.leaderboard.LeaderboardEntry
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leaderboard")
class LeaderboardEndpoint(private val leaderboard: Leaderboard) : LeaderboardApi {
    @GetMapping
    override fun entries(): List<LeaderboardEntry> = leaderboard.entries()
    @PostMapping
    override fun save(@RequestBody entry: LeaderboardEntry): LeaderboardEntry {
        leaderboard.save(entry); return entry
    }
}
```

- [ ] Create `src/main/kotlin/com/jacuum/web/SessionEndpoint.kt`:
```kotlin
package com.jacuum.web
import com.jacuum.engine.Sessions
import com.jacuum.engine.SessionView
import com.jacuum.map.GameMap
import com.jacuum.map.Maps
import com.jacuum.map.SizePreset
import com.jacuum.web.dto.CreateSessionRequest
import com.jacuum.web.dto.SessionResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/session")
class SessionEndpoint(
    private val sessions: Sessions,
    private val maps: Maps,
    private val snapshots: Snapshots,
    @Value("\${game.default-iterations:500}") private val defaultIterations: Int
) : SessionApi {

    @PostMapping
    override fun create(@RequestBody req: CreateSessionRequest): SessionResponse {
        if (req.algoName.isNullOrBlank())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "algoName is required")
        if (req.username.isNullOrBlank())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required")
        if (req.avatar.isNullOrBlank())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar is required")
        val hash = req.hash?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val size = sizeFrom(req.size)
        val iters = if (req.iterations > 0) req.iterations else defaultIterations
        val map = maps.generate(hash, size)
        val id = sessions.open(map, req.algoName, req.username, req.avatar, iters)
        return toResponse(id, map, iters)
    }

    @PostMapping("/{id}/start")
    override fun start(@PathVariable id: String): SessionView { sessions.start(id); return sessions.view(id) }
    @PostMapping("/{id}/pause")
    override fun pause(@PathVariable id: String): SessionView { sessions.pause(id); return sessions.view(id) }
    @PostMapping("/{id}/resume")
    override fun resume(@PathVariable id: String): SessionView { sessions.resume(id); return sessions.view(id) }
    @PostMapping("/{id}/stop")
    override fun stop(@PathVariable id: String): SessionView { sessions.stop(id); return sessions.view(id) }
    @GetMapping("/{id}")
    override fun view(@PathVariable id: String): SessionView = sessions.view(id)

    private fun toResponse(id: String, map: GameMap, iters: Int) =
        SessionResponse(id, "SETUP", snapshots.of(map),
            map.startX(), map.startY(), map.totalFloorTiles(), iters)

    private fun sizeFrom(s: String?): SizePreset {
        if (s.isNullOrBlank()) return SizePreset.SMALL
        return try { SizePreset.valueOf(s.uppercase()) } catch (e: IllegalArgumentException) { SizePreset.SMALL }
    }
}
```

- [x] Read each Java test file in `src/test/java/com/jacuum/web/`, then delete them and create Kotlin equivalents in `src/test/kotlin/com/jacuum/web/`. The Spring MockMvc tests use `@SpringBootTest` + `@AutoConfigureMockMvc` — these work identically in Kotlin. Replace `var body = new CreateSessionRequest(...)` with `val body = CreateSessionRequest(...)`. Keep all assertions identical.

- [x] Run `./gradlew test` — must pass.

- [x] Commit:
```bash
git commit -m "refactor: convert com.jacuum.web to Kotlin"
```

---

### Task 2.6: Convert `com.jacuum.algo.impl` package

**Files:**
- Delete: 3 files in `src/main/java/com/jacuum/algo/impl/`
- Create: 3 files in `src/main/kotlin/com/jacuum/algo/impl/`
- Delete/convert: `FrontierAlgoTest.java`, `AlgoSmokeTest.java` (if not already converted in Task 2.1)

- [ ] Delete `RandomAlgo.java`, create `src/main/kotlin/com/jacuum/algo/impl/RandomAlgo.kt`:
```kotlin
package com.jacuum.algo.impl
import com.jacuum.algo.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import java.util.Random

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Random")
class RandomAlgo : RobotAlgo {
    private val rng = Random()
    override fun next(tile: Tile): Direction {
        val passable = Direction.entries.filter { !tile.hasWall(it) }
        return if (passable.isEmpty()) Direction.entries.random() else passable.random(java.util.Random().let {
            object : kotlin.random.Random() {
                override fun nextBits(bitCount: Int) = rng.nextInt().ushr(32 - bitCount)
            }
        })
    }
}
```
Note: for determinism with `java.util.Random`, write `next()` as:
```kotlin
override fun next(tile: Tile): Direction {
    val passable = Direction.entries.filter { !tile.hasWall(it) }
    val choices = passable.ifEmpty { Direction.entries }
    return choices[rng.nextInt(choices.size)]
}
```

- [ ] Delete `AlwaysLeftAlgo.java`, create `src/main/kotlin/com/jacuum/algo/impl/AlwaysLeftAlgo.kt`:
```kotlin
package com.jacuum.algo.impl
import com.jacuum.algo.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Always Left")
class AlwaysLeftAlgo : RobotAlgo {
    private val preference = listOf(Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH)
    override fun next(tile: Tile): Direction =
        preference.firstOrNull { !tile.hasWall(it) } ?: Direction.WEST
}
```

- [ ] Delete `FrontierAlgo.java`, create `src/main/kotlin/com/jacuum/algo/impl/FrontierAlgo.kt`:
```kotlin
package com.jacuum.algo.impl
import com.jacuum.algo.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import java.util.ArrayDeque
import java.util.EnumSet

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Frontier BFS")
class FrontierAlgo : RobotAlgo {
    private val walls = HashMap<String, EnumSet<Direction>>()
    private val clean = HashSet<String>()
    private val plan = ArrayDeque<Direction>()
    private var startKey: String? = null

    override fun next(tile: Tile): Direction {
        observe(tile)
        if (plan.isNotEmpty()) return plan.poll()

        val cur = key(tile.x(), tile.y())
        val toFrontier = bfsTo(tile.x(), tile.y(), null)
        if (toFrontier.isNotEmpty()) { plan.addAll(toFrontier); return plan.poll() }

        val sk = startKey
        if (sk != null && !clean.contains(sk) && cur != sk) {
            val toStart = bfsTo(tile.x(), tile.y(), sk)
            if (toStart.isNotEmpty()) { plan.addAll(toStart); return plan.poll() }
        }
        return anyPassable(tile)
    }

    private fun observe(tile: Tile) {
        val k = key(tile.x(), tile.y())
        if (startKey == null) startKey = k
        if (!walls.containsKey(k)) {
            val blocked = EnumSet.noneOf(Direction::class.java)
            for (d in Direction.entries) if (tile.hasWall(d)) blocked.add(d)
            walls[k] = blocked
        }
        if (tile.isClean()) clean.add(k)
    }

    private fun bfsTo(startX: Int, startY: Int, targetKey: String?): List<Direction> {
        val parent = HashMap<String, String?>()
        val stepTo = HashMap<String, Direction>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val sk = key(startX, startY)
        parent[sk] = null; queue.add(startX to startY)

        while (queue.isNotEmpty()) {
            val (px, py) = queue.poll()
            val posKey = key(px, py)
            val blocked = walls.getOrDefault(posKey, EnumSet.noneOf(Direction::class.java))
            for (d in Direction.entries) {
                if (blocked.contains(d)) continue
                val nx = px + d.dx(); val ny = py + d.dy(); val nk = key(nx, ny)
                if (parent.containsKey(nk)) continue
                parent[nk] = posKey; stepTo[nk] = d
                if (targetKey == null) {
                    if (!walls.containsKey(nk)) return reconstruct(nk, parent, stepTo)
                    queue.add(nx to ny)
                } else {
                    if (nk == targetKey) return reconstruct(nk, parent, stepTo)
                    if (walls.containsKey(nk)) queue.add(nx to ny)
                }
            }
        }
        return emptyList()
    }

    private fun reconstruct(target: String, parent: Map<String, String?>,
                             stepTo: Map<String, Direction>): List<Direction> {
        val path = ArrayDeque<Direction>()
        var cur = target
        while (stepTo.containsKey(cur)) { path.addFirst(stepTo[cur]!!); cur = parent[cur]!! }
        return path.toList()
    }

    private fun anyPassable(tile: Tile) = Direction.entries.firstOrNull { !tile.hasWall(it) } ?: Direction.NORTH
    private fun key(x: Int, y: Int) = "$x,$y"
}
```

- [x] Read `src/test/java/com/jacuum/algo/FrontierAlgoTest.java`, delete it and create `src/test/kotlin/com/jacuum/algo/FrontierAlgoTest.kt` with the same tests.

- [x] Run `./gradlew test` — must pass.

- [x] Commit:
```bash
git commit -m "refactor: convert com.jacuum.algo.impl to Kotlin"
```

---

### Task 2.7: Convert root and finalize Phase 2

**Files:**
- Delete `src/main/java/com/jacuum/JacuumApplication.java`
- Create `src/main/kotlin/com/jacuum/JacuumApplication.kt`

- [x] Delete `JacuumApplication.java`, create `src/main/kotlin/com/jacuum/JacuumApplication.kt`:
```kotlin
package com.jacuum
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JacuumApplication

fun main(args: Array<String>) {
    runApplication<JacuumApplication>(*args)
}
```

- [x] Verify no `.java` files remain in `src/main/java/`:
```bash
find src/main/java -name "*.java" | wc -l
```
Expected output: `0`

- [x] Run `./gradlew test` — must pass.

- [x] Run the app and verify it starts:
```bash
./gradlew bootRun &
sleep 10 && curl -s http://localhost:8080/api/algos
```
Expected: JSON array like `["Always Left","Frontier BFS","Random"]`

- [x] Kill the server, commit:
```bash
git commit -m "refactor: convert JacuumApplication to Kotlin — Phase 2 complete"
```

- [ ] Push and open PR from `feature/cld-pow-p2-kotlin` → `claude-powered`, merge when green.

---

# PHASE 3 — Vanilla JS → React + TypeScript

Create branch `feature/cld-pow-p3-react` from `claude-powered` after Phase 2 merges.

---

### Task 3.1: Scaffold Vite/React frontend

**Files:** Create all of `src/main/frontend/`

- [ ] Create `src/main/frontend/package.json`:
```json
{
  "name": "jacuum-frontend",
  "version": "0.0.1",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc --noEmit && vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.1"
  },
  "devDependencies": {
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@types/sockjs-client": "^1.5.4",
    "@vitejs/plugin-react": "^4.3.3",
    "@testing-library/react": "^16.0.0",
    "@testing-library/user-event": "^14.5.2",
    "@testing-library/jest-dom": "^6.6.3",
    "typescript": "^5.6.3",
    "vite": "^5.4.10",
    "vitest": "^2.1.5",
    "jsdom": "^25.0.0"
  }
}
```

- [ ] Create `src/main/frontend/tsconfig.json`:
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noEmit": true,
    "skipLibCheck": true,
    "esModuleInterop": true
  },
  "include": ["src"]
}
```

- [ ] Create `src/main/frontend/vite.config.ts`:
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../../../../build/frontend',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
  },
})
```

- [ ] Create `src/main/frontend/index.html`:
```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Jacuum Cleaner</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] Create `src/main/frontend/src/test-setup.ts`:
```typescript
import '@testing-library/jest-dom'
```

- [ ] Create minimal `src/main/frontend/src/main.tsx` (just enough to compile):
```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <div>Loading…</div>
  </React.StrictMode>
)
```

- [ ] Run `npm install` inside `src/main/frontend/`:
```bash
cd src/main/frontend && npm install
```

- [ ] Verify TypeScript compiles:
```bash
npx tsc --noEmit
```
Expected: no errors.

---

### Task 3.2: Wire Gradle for React build

**Files:** Modify `build.gradle.kts`

- [ ] Update the `node { ... }` block and tasks in `build.gradle.kts` — replace the existing node block and npm task definitions with:
```kotlin
node {
    version = "20.17.0"
    download = true
    workDir = file("${project.projectDir}/.gradle/nodejs")
    nodeProjectDir = file("${project.projectDir}/src/main/frontend")
}

val npmRunBuild by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    args = listOf("run", "build")
    dependsOn(tasks.named("npmInstall"))
    inputs.dir("src/main/frontend/src")
    inputs.file("src/main/frontend/package.json")
    inputs.file("src/main/frontend/vite.config.ts")
    outputs.dir(layout.buildDirectory.dir("frontend"))
}

val npmRunTest by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    args = listOf("test")
    dependsOn(tasks.named("npmInstall"))
}

tasks.processResources {
    dependsOn(npmRunBuild)
    from(layout.buildDirectory.dir("frontend")) {
        into("static")
    }
}

tasks.test {
    useJUnitPlatform()
    dependsOn(npmRunTest)
}
```

- [ ] Verify the build picks up the frontend — run:
```bash
./gradlew processResources
ls build/resources/main/static/
```
Expected: `index.html`, `assets/` directory from Vite output.

---

### Task 3.3: Create `types.ts`

**Files:** Create `src/main/frontend/src/types.ts`

- [ ] Create `src/main/frontend/src/types.ts`:
```typescript
export interface TileSnapshot {
  x: number; y: number
  wallNorth: boolean; wallSouth: boolean; wallEast: boolean; wallWest: boolean
}

export interface MapSnapshot {
  width: number; height: number
  startX: number; startY: number
  totalFloor: number
  tiles: TileSnapshot[]
}

export interface SessionResponse {
  sessionId: string
  status: string
  map: MapSnapshot
  robotX: number; robotY: number
  totalFloor: number
  iterationsAvailable: number
}

export interface IterationEvent {
  sessionId: string
  iteration: number
  direction: string | null
  robotX: number; robotY: number
  score: number
  totalCleaned: number; totalFloor: number
  finished: boolean
  finishReason: string | null
}

export interface TraceEvent {
  iteration: number
  direction: string | null
  x: number; y: number; score: number
}

export interface LeaderboardEntry {
  id: string; username: string; avatar: string
  mapHash: string; mapSize: string; algoName: string
  iterationsUsed: number; iterationsAvailable: number
  score: number; completedAt: string
  trace: TraceEvent[]
}

export interface GameState {
  sessionId: string
  robotX: number; robotY: number
  itersAvail: number
  score: number; totalCleaned: number; itersUsed: number
  cleanedTiles: Set<string>
  finished: boolean; finishReason: string | null
  map: MapSnapshot
  trace: TraceEvent[]
}

export interface SetupFormValues {
  hash: string; size: string; username: string
  avatar: string; algoName: string; iterations: number
}
```

---

### Task 3.4: Create `api.ts` and `api.test.ts`

**Files:**
- Create `src/main/frontend/src/api.ts`
- Create `src/main/frontend/src/__tests__/api.test.ts`

- [ ] Create `src/main/frontend/src/api.ts`:
```typescript
import type { LeaderboardEntry, SessionResponse } from './types'

const json = async <T>(r: Response): Promise<T> => {
  if (!r.ok) throw new Error(String(r.status))
  return r.json() as Promise<T>
}

export const Api = {
  algos: (): Promise<string[]> => fetch('/api/algos').then(json),
  avatars: (): Promise<string[]> => fetch('/api/avatars').then(json),
  createSession: (body: object): Promise<SessionResponse> =>
    fetch('/api/session', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then(json),
  start:  (id: string): Promise<unknown> => fetch(`/api/session/${id}/start`,  { method: 'POST' }).then(json),
  pause:  (id: string): Promise<unknown> => fetch(`/api/session/${id}/pause`,  { method: 'POST' }).then(json),
  resume: (id: string): Promise<unknown> => fetch(`/api/session/${id}/resume`, { method: 'POST' }).then(json),
  stop:   (id: string): Promise<unknown> => fetch(`/api/session/${id}/stop`,   { method: 'POST' }).then(json),
  leaderboard: (): Promise<LeaderboardEntry[]> => fetch('/api/leaderboard').then(json),
  saveToLb: (entry: LeaderboardEntry): Promise<LeaderboardEntry> =>
    fetch('/api/leaderboard', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(entry),
    }).then(json),
}
```

- [ ] Create `src/main/frontend/src/__tests__/api.test.ts`:
```typescript
import { describe, test, expect, vi, beforeEach } from 'vitest'
import { Api } from '../api'

const mockOkJson = (data: unknown) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true, json: () => Promise.resolve(data),
  } as Response)

const mockError = (status: number) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response)

beforeEach(() => vi.restoreAllMocks())

describe('Api.algos', () => {
  test('GETs /api/algos and returns parsed JSON', async () => {
    mockOkJson(['Random', 'Always Left'])
    const result = await Api.algos()
    expect(fetch).toHaveBeenCalledWith('/api/algos')
    expect(result).toEqual(['Random', 'Always Left'])
  })
  test('throws when response is not ok', async () => {
    mockError(500)
    await expect(Api.algos()).rejects.toThrow('500')
  })
})

describe('Api.createSession', () => {
  test('POSTs to /api/session with JSON body', async () => {
    const sessionResp = { sessionId: 'abc', status: 'SETUP' }
    mockOkJson(sessionResp)
    const body = { algoName: 'Random', username: 'Alice', avatar: '🤖', size: 'TINY', iterations: 100 }
    const result = await Api.createSession(body)
    expect(fetch).toHaveBeenCalledWith('/api/session', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }))
    expect(result).toEqual(sessionResp)
  })
  test('throws when server returns 400', async () => {
    mockError(400)
    await expect(Api.createSession({})).rejects.toThrow('400')
  })
})

describe('Api.start', () => {
  test('POSTs to /api/session/{id}/start', async () => {
    mockOkJson({ status: 'RUNNING' })
    await Api.start('my-session')
    expect(fetch).toHaveBeenCalledWith('/api/session/my-session/start', { method: 'POST' })
  })
})

describe('Api.leaderboard', () => {
  test('GETs /api/leaderboard and returns entries', async () => {
    const entries = [{ id: 'e1', score: 100 }]
    mockOkJson(entries)
    const result = await Api.leaderboard()
    expect(fetch).toHaveBeenCalledWith('/api/leaderboard')
    expect(result).toEqual(entries)
  })
})

describe('Api.saveToLb', () => {
  test('POSTs entry to /api/leaderboard', async () => {
    const entry = { id: 'e1', score: 100 } as any
    mockOkJson(entry)
    await Api.saveToLb(entry)
    expect(fetch).toHaveBeenCalledWith('/api/leaderboard', expect.objectContaining({
      method: 'POST', body: JSON.stringify(entry),
    }))
  })
})
```

- [ ] Run tests:
```bash
cd src/main/frontend && npm test
```
Expected: all api tests pass.

---

### Task 3.5: Create `GameLogic.ts` and `GameLogic.test.ts`

**Files:**
- Create `src/main/frontend/src/GameLogic.ts`
- Create `src/main/frontend/src/__tests__/GameLogic.test.ts`

- [ ] Create `src/main/frontend/src/GameLogic.ts`:
```typescript
import type { GameState, IterationEvent, LeaderboardEntry, SessionResponse, TraceEvent } from './types'

export function createState(session: SessionResponse): GameState {
  return {
    sessionId: session.sessionId,
    robotX: session.robotX,
    robotY: session.robotY,
    itersAvail: session.iterationsAvailable,
    score: 0, totalCleaned: 0, itersUsed: 0,
    cleanedTiles: new Set(),
    finished: false, finishReason: null,
    map: session.map, trace: [],
  }
}

export function applyEvent(state: GameState, ev: IterationEvent): GameState {
  const cleanedTiles = new Set(state.cleanedTiles)
  cleanedTiles.add(`${ev.robotX},${ev.robotY}`)
  return {
    ...state,
    robotX: ev.robotX, robotY: ev.robotY,
    score: ev.score, totalCleaned: ev.totalCleaned,
    itersUsed: ev.iteration,
    cleanedTiles,
    finished: ev.finished,
    finishReason: ev.finishReason ?? null,
    trace: [...state.trace, ev],
  }
}

export function buildLeaderboardEntry(state: GameState, setupBody: {
  username: string; avatar: string; hash: string | null; size: string; algoName: string
}): LeaderboardEntry {
  return {
    id: state.sessionId,
    username: setupBody.username,
    avatar: setupBody.avatar,
    mapHash: setupBody.hash ?? state.sessionId,
    mapSize: setupBody.size,
    algoName: setupBody.algoName,
    iterationsUsed: state.itersUsed,
    iterationsAvailable: state.itersAvail,
    score: state.score,
    completedAt: new Date().toISOString(),
    trace: state.trace.map((e): TraceEvent => ({
      iteration: e.iteration, direction: e.direction,
      x: e.robotX, y: e.robotY, score: e.score,
    })),
  }
}

export function buildReplayEvents(trace: TraceEvent[], totalFloor: number): IterationEvent[] {
  const cleanedSet = new Set<string>()
  return trace.map((t): IterationEvent => {
    cleanedSet.add(`${t.x},${t.y}`)
    return {
      sessionId: '', iteration: t.iteration, direction: t.direction,
      robotX: t.x, robotY: t.y, score: t.score,
      totalCleaned: cleanedSet.size, totalFloor,
      finished: t.iteration === trace.length,
      finishReason: t.iteration === trace.length ? 'COMPLETED' : null,
    }
  })
}
```

- [ ] Create `src/main/frontend/src/__tests__/GameLogic.test.ts`:
```typescript
import { describe, test, expect } from 'vitest'
import { createState, applyEvent, buildLeaderboardEntry, buildReplayEvents } from '../GameLogic'
import type { SessionResponse, IterationEvent } from '../types'

function makeSession(overrides: Partial<SessionResponse> = {}): SessionResponse {
  return {
    sessionId: 'sess-1', robotX: 2, robotY: 3,
    iterationsAvailable: 100, status: 'SETUP',
    map: { width: 10, height: 8, startX: 2, startY: 3, totalFloor: 15, tiles: [] },
    totalFloor: 15,
    ...overrides,
  }
}

function makeEvent(overrides: Partial<IterationEvent> = {}): IterationEvent {
  return {
    sessionId: 'sess-1', robotX: 3, robotY: 3, score: 100,
    totalCleaned: 1, iteration: 1, direction: 'EAST',
    totalFloor: 15, finished: false, finishReason: null,
    ...overrides,
  }
}

describe('createState', () => {
  test('copies session fields into initial state', () => {
    const state = createState(makeSession())
    expect(state.sessionId).toBe('sess-1')
    expect(state.robotX).toBe(2); expect(state.robotY).toBe(3)
    expect(state.itersAvail).toBe(100)
    expect(state.map.totalFloor).toBe(15)
  })
  test('all counters start at zero', () => {
    const state = createState(makeSession())
    expect(state.score).toBe(0); expect(state.totalCleaned).toBe(0); expect(state.itersUsed).toBe(0)
  })
  test('starts not finished with empty trace', () => {
    const state = createState(makeSession())
    expect(state.finished).toBe(false); expect(state.finishReason).toBeNull()
    expect(state.trace).toHaveLength(0); expect(state.cleanedTiles.size).toBe(0)
  })
})

describe('applyEvent', () => {
  test('updates robot position, score, and iteration counter', () => {
    const s1 = applyEvent(createState(makeSession()), makeEvent({ robotX: 4, robotY: 3, score: 100, totalCleaned: 1, iteration: 1 }))
    expect(s1.robotX).toBe(4); expect(s1.score).toBe(100); expect(s1.itersUsed).toBe(1)
  })
  test('adds visited tile to cleanedTiles', () => {
    const s1 = applyEvent(createState(makeSession()), makeEvent({ robotX: 4, robotY: 3 }))
    expect(s1.cleanedTiles.has('4,3')).toBe(true)
  })
  test('does not mutate original state', () => {
    const s0 = createState(makeSession())
    applyEvent(s0, makeEvent({ robotX: 4, robotY: 3 }))
    expect(s0.robotX).toBe(2); expect(s0.cleanedTiles.size).toBe(0)
  })
  test('accumulates trace across multiple events', () => {
    let state = createState(makeSession())
    state = applyEvent(state, makeEvent({ iteration: 1, robotX: 3, robotY: 3 }))
    state = applyEvent(state, makeEvent({ iteration: 2, robotX: 4, robotY: 3 }))
    expect(state.trace).toHaveLength(2)
  })
  test('marks state as finished', () => {
    const s1 = applyEvent(createState(makeSession()), makeEvent({ finished: true, finishReason: 'COMPLETED' }))
    expect(s1.finished).toBe(true); expect(s1.finishReason).toBe('COMPLETED')
  })
})

describe('buildLeaderboardEntry', () => {
  test('maps state fields to entry fields correctly', () => {
    const entry = buildLeaderboardEntry(
      createState(makeSession({ sessionId: 'abc' })),
      { username: 'Alice', avatar: '🤖', hash: 'h1', size: 'TINY', algoName: 'Random' }
    )
    expect(entry.id).toBe('abc'); expect(entry.username).toBe('Alice')
    expect(entry.mapHash).toBe('h1'); expect(entry.score).toBe(0); expect(entry.trace).toEqual([])
  })
  test('falls back to sessionId when hash is null', () => {
    const entry = buildLeaderboardEntry(
      createState(makeSession({ sessionId: 'fallback-id' })),
      { username: 'Bob', avatar: '🤖', hash: null, size: 'SMALL', algoName: 'Always Left' }
    )
    expect(entry.mapHash).toBe('fallback-id')
  })
  test('serialises trace into compact TraceEvent objects', () => {
    let state = createState(makeSession({ sessionId: 'sess' }))
    state = applyEvent(state, makeEvent({ iteration: 1, direction: 'EAST', robotX: 3, robotY: 3, score: 100 }))
    const entry = buildLeaderboardEntry(state, { username: 'Alice', avatar: '🤖', hash: 'h', size: 'TINY', algoName: 'Random' })
    expect(entry.trace).toHaveLength(1)
    expect(entry.trace[0]).toEqual({ iteration: 1, direction: 'EAST', x: 3, y: 3, score: 100 })
  })
})

describe('buildReplayEvents', () => {
  test('converts a trace into robotX/robotY events', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 3, y: 3, score: 100 },
      { iteration: 2, direction: 'EAST', x: 4, y: 3, score: 200 },
    ], 10)
    expect(events).toHaveLength(2)
    expect(events[0].robotX).toBe(3); expect(events[1].robotX).toBe(4)
  })
  test('increments totalCleaned for new tiles', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 },
      { iteration: 2, direction: 'EAST', x: 2, y: 0, score: 200 },
    ], 5)
    expect(events[0].totalCleaned).toBe(1); expect(events[1].totalCleaned).toBe(2)
  })
  test('does not double-count revisited tiles', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 },
      { iteration: 2, direction: 'WEST', x: 1, y: 0, score: 100 },
    ], 5)
    expect(events[0].totalCleaned).toBe(1); expect(events[1].totalCleaned).toBe(1)
  })
  test('marks last event as finished=true', () => {
    const events = buildReplayEvents([{ iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 }], 5)
    expect(events[0].finished).toBe(true); expect(events[0].finishReason).toBe('COMPLETED')
  })
  test('intermediate events are not finished', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 },
      { iteration: 2, direction: 'EAST', x: 2, y: 0, score: 200 },
    ], 5)
    expect(events[0].finished).toBe(false); expect(events[1].finished).toBe(true)
  })
})
```

- [ ] Run `npm test` inside `src/main/frontend/` — all tests must pass.

---

### Task 3.6: Create `App.tsx` (screen router)

**Files:** Create `src/main/frontend/src/App.tsx`

- [ ] Create `src/main/frontend/src/App.tsx`:
```typescript
import React, { useState } from 'react'
import SetupScreen from './SetupScreen'
import GameScreen from './GameScreen'
import LeaderboardScreen from './LeaderboardScreen'
import type { SessionResponse, SetupFormValues } from './types'

type Screen = 'setup' | 'game' | 'leaderboard'

export default function App() {
  const [screen, setScreen] = useState<Screen>('setup')
  const [session, setSession] = useState<SessionResponse | null>(null)
  const [setupValues, setSetupValues] = useState<SetupFormValues | null>(null)

  return (
    <>
      <nav>
        <button onClick={() => setScreen('setup')}>Setup</button>
        <button onClick={() => setScreen('leaderboard')}>Leaderboard</button>
      </nav>
      {screen === 'setup' && (
        <SetupScreen
          onStart={(sess, vals) => { setSession(sess); setSetupValues(vals); setScreen('game') }}
        />
      )}
      {screen === 'game' && session && setupValues && (
        <GameScreen
          session={session}
          setupValues={setupValues}
          onNewGame={() => setScreen('setup')}
        />
      )}
      {screen === 'leaderboard' && (
        <LeaderboardScreen
          onRetry={(hash, size, iters) => setScreen('setup')}
          onReplay={(sess, vals) => { setSession(sess); setSetupValues(vals); setScreen('game') }}
        />
      )}
    </>
  )
}
```

- [ ] Update `src/main/frontend/src/main.tsx` to use `App`:
```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><App /></React.StrictMode>
)
```

---

### Task 3.7: Create `SetupScreen.tsx`

**Files:** Create `src/main/frontend/src/SetupScreen.tsx`

- [ ] Create `src/main/frontend/src/SetupScreen.tsx`:
```typescript
import React, { useEffect, useState } from 'react'
import { Api } from './api'
import type { SessionResponse, SetupFormValues } from './types'

const ITER_VALUES = [250, 500, 1000, 2000, 5000]
const PREFS_KEY = 'jacuum_prefs'
const HEROES = ['Luke Skywalker', 'Leia Organa', 'Han Solo', 'Rey', 'Din Djarin', 'Obi-Wan Kenobi']

interface Props {
  onStart: (session: SessionResponse, values: SetupFormValues) => void
}

export default function SetupScreen({ onStart }: Props) {
  const [algos, setAlgos] = useState<string[]>([])
  const [avatars, setAvatars] = useState<string[]>([])
  const [hash, setHash] = useState('')
  const [size, setSize] = useState('SMALL')
  const [username, setUsername] = useState('')
  const [avatar, setAvatar] = useState('🤖')
  const [algoName, setAlgoName] = useState('')
  const [itersIdx, setItersIdx] = useState(1)
  const [heroPlaceholder] = useState(() => HEROES[Math.floor(Math.random() * HEROES.length)])

  useEffect(() => {
    Promise.all([Api.algos(), Api.avatars()]).then(([a, av]) => {
      setAlgos(a); setAvatars(av)
      const prefs = JSON.parse(localStorage.getItem(PREFS_KEY) || '{}')
      if (prefs.hash)   setHash(prefs.hash)
      if (prefs.size)   setSize(prefs.size)
      if (prefs.username) setUsername(prefs.username)
      if (prefs.avatar)   setAvatar(prefs.avatar)
      if (prefs.iters !== undefined) setItersIdx(Number(prefs.iters))
      if (prefs.algo && a.includes(prefs.algo)) setAlgoName(prefs.algo)
      else if (a.length > 0) setAlgoName(a[0])
    })
  }, [])

  async function handleStart() {
    const values: SetupFormValues = {
      hash, size, username: username || heroPlaceholder,
      avatar, algoName, iterations: ITER_VALUES[itersIdx],
    }
    localStorage.setItem(PREFS_KEY, JSON.stringify({ hash, size, username, avatar, algo: algoName, iters: itersIdx }))
    try {
      const session = await Api.createSession(values)
      onStart(session, values)
    } catch (e: any) {
      alert('Failed to start: ' + e.message)
    }
  }

  return (
    <section id="screen-setup">
      <h2>New Game</h2>
      <label>Map Hash <input value={hash} onChange={e => setHash(e.target.value)} placeholder="leave blank for random" /></label>
      <label>Size
        <select value={size} onChange={e => setSize(e.target.value)}>
          <option value="TINY">Tiny</option>
          <option value="SMALL">Small</option>
          <option value="MEDIUM">Medium</option>
          <option value="LARGE">Large</option>
        </select>
      </label>
      <label>Username <input value={username} onChange={e => setUsername(e.target.value)} placeholder={heroPlaceholder} /></label>
      <label>Avatar
        <div id="avatar-picker">
          {avatars.map(av => (
            <span key={av} className={'avatar-opt' + (av === avatar ? ' selected' : '')}
                  onClick={() => setAvatar(av)}>{av}</span>
          ))}
        </div>
      </label>
      <label>Algorithm
        <select value={algoName} onChange={e => setAlgoName(e.target.value)}>
          {algos.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
      </label>
      <label>Iterations
        <input type="range" min={0} max={4} step={1} value={itersIdx}
               onChange={e => setItersIdx(Number(e.target.value))} />
        <span>{ITER_VALUES[itersIdx]?.toLocaleString()}</span>
      </label>
      <button onClick={handleStart}>Start Game</button>
    </section>
  )
}
```

---

### Task 3.8: Create `GameScreen.tsx`

**Files:** Create `src/main/frontend/src/GameScreen.tsx`

- [ ] Create `src/main/frontend/src/GameScreen.tsx`:
```typescript
import React, { useEffect, useRef, useState, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Api } from './api'
import { createState, applyEvent, buildLeaderboardEntry } from './GameLogic'
import type { GameState, IterationEvent, SessionResponse, SetupFormValues } from './types'

const CELL = 22
const SPEEDS = [800, 400, 200, 100, 50, 16]
const SPEED_LABELS = ['×0.1', '×0.25', '×0.5', '×1', '×2', '×max']

interface Props {
  session: SessionResponse
  setupValues: SetupFormValues
  onNewGame: () => void
}

export default function GameScreen({ session, setupValues, onNewGame }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const stateRef = useRef<GameState>(createState(session))
  const queueRef = useRef<IterationEvent[]>([])
  const animatingRef = useRef(false)
  const speedRef = useRef(2)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const stompRef = useRef<Client | null>(null)

  const [speedIdx, setSpeedIdx] = useState(2)
  const [hud, setHud] = useState({ score: 0, cleaned: 0, itersUsed: 0 })
  const [finished, setFinished] = useState(false)
  const [finalScore, setFinalScore] = useState(0)
  const [saved, setSaved] = useState(false)

  const render = useCallback((s: GameState) => {
    const canvas = canvasRef.current; if (!canvas) return
    const ctx = canvas.getContext('2d')!
    ctx.fillStyle = '#0a0a16'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    for (const t of s.map.tiles) {
      const px = t.x * CELL, py = t.y * CELL
      ctx.fillStyle = s.cleanedTiles.has(`${t.x},${t.y}`) ? '#e8f4e8' : '#2a2a4a'
      ctx.fillRect(px + 1, py + 1, CELL - 2, CELL - 2)
      ctx.strokeStyle = '#0a0a16'; ctx.lineWidth = 2
      if (t.wallNorth) { ctx.beginPath(); ctx.moveTo(px, py);        ctx.lineTo(px + CELL, py);        ctx.stroke() }
      if (t.wallSouth) { ctx.beginPath(); ctx.moveTo(px, py + CELL); ctx.lineTo(px + CELL, py + CELL); ctx.stroke() }
      if (t.wallWest)  { ctx.beginPath(); ctx.moveTo(px, py);        ctx.lineTo(px, py + CELL);        ctx.stroke() }
      if (t.wallEast)  { ctx.beginPath(); ctx.moveTo(px + CELL, py); ctx.lineTo(px + CELL, py + CELL); ctx.stroke() }
    }
    ctx.font = `${Math.floor(CELL * 0.8)}px serif`
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText(setupValues.avatar || '🤖', s.robotX * CELL + CELL / 2, s.robotY * CELL + CELL / 2)
  }, [setupValues.avatar])

  const drainQueue = useCallback(() => {
    if (queueRef.current.length === 0) { animatingRef.current = false; return }
    animatingRef.current = true
    const ev = queueRef.current.shift()!
    stateRef.current = applyEvent(stateRef.current, ev)
    render(stateRef.current)
    setHud({ score: stateRef.current.score, cleaned: stateRef.current.totalCleaned, itersUsed: stateRef.current.itersUsed })
    if (stateRef.current.finished) {
      setFinished(true); setFinalScore(stateRef.current.score)
      return
    }
    timerRef.current = setTimeout(drainQueue, SPEEDS[speedRef.current])
  }, [render])

  useEffect(() => {
    const canvas = canvasRef.current; if (!canvas) return
    const s = stateRef.current
    canvas.width = s.map.width * CELL; canvas.height = s.map.height * CELL
    render(s)

    const stomp = new Client({
      webSocketFactory: () => new SockJS('/ws') as WebSocket,
      onConnect: () => {
        stomp.subscribe(`/topic/session/${session.sessionId}/events`, msg => {
          queueRef.current.push(JSON.parse(msg.body) as IterationEvent)
          if (!animatingRef.current) drainQueue()
        })
        Api.start(session.sessionId)
      },
    })
    stomp.activate()
    stompRef.current = stomp
    return () => { stomp.deactivate(); if (timerRef.current) clearTimeout(timerRef.current) }
  }, [session.sessionId, render, drainQueue])

  function handlePause() { Api.pause(session.sessionId) }
  function handleResume() { Api.resume(session.sessionId) }
  function handleStop() { Api.stop(session.sessionId) }

  function handleSpeedChange(e: React.ChangeEvent<HTMLInputElement>) {
    const idx = Number(e.target.value)
    speedRef.current = idx; setSpeedIdx(idx)
  }

  async function handleSaveLb() {
    const entry = buildLeaderboardEntry(stateRef.current, { ...setupValues, hash: setupValues.hash || null })
    await Api.saveToLb(entry)
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <section id="screen-game">
      <div id="game-info">
        <span>Score: <strong>{hud.score}</strong></span>
        <span>Cleaned: <strong>{hud.cleaned}</strong>/<strong>{session.totalFloor}</strong></span>
        <span>Iterations: <strong>{hud.itersUsed}</strong>/<strong>{session.iterationsAvailable}</strong></span>
      </div>
      <canvas ref={canvasRef} id="game-canvas" />
      <div id="game-controls">
        <button onClick={handlePause} disabled={finished}>Pause</button>
        <button onClick={handleResume} disabled={finished}>Resume</button>
        <button onClick={handleStop}>Stop</button>
        <label>Speed
          <input type="range" min={0} max={5} step={1} value={speedIdx} onChange={handleSpeedChange} />
          <span>{SPEED_LABELS[speedIdx]}</span>
        </label>
      </div>
      {finished && (
        <div id="finish-panel">
          <h3>Done! Score: <span>{finalScore}</span></h3>
          <button onClick={handleSaveLb} disabled={saved}>{saved ? 'Saved!' : 'Save to Leaderboard'}</button>
          <button onClick={onNewGame}>New Game</button>
        </div>
      )}
    </section>
  )
}
```

---

### Task 3.9: Create `LeaderboardScreen.tsx`

**Files:** Create `src/main/frontend/src/LeaderboardScreen.tsx`

- [ ] Create `src/main/frontend/src/LeaderboardScreen.tsx`:
```typescript
import React, { useEffect, useState } from 'react'
import { Api } from './api'
import type { LeaderboardEntry, SessionResponse, SetupFormValues } from './types'

const ITER_VALUES = [250, 500, 1000, 2000, 5000]

interface Props {
  onRetry: (hash: string, size: string, iters: number) => void
  onReplay: (session: SessionResponse, values: SetupFormValues) => void
}

export default function LeaderboardScreen({ onRetry, onReplay }: Props) {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([])
  const [empty, setEmpty] = useState(false)

  useEffect(() => {
    Api.leaderboard()
      .then(data => {
        if (!data || data.length === 0) { setEmpty(true); return }
        setEntries([...data].sort((a, b) => b.score - a.score))
      })
      .catch(() => setEmpty(true))
  }, [])

  async function handleReplay(e: LeaderboardEntry) {
    const session = await Api.createSession({
      hash: e.mapHash, size: e.mapSize,
      algoName: e.algoName, username: e.username,
      avatar: e.avatar, iterations: e.iterationsAvailable,
    })
    onReplay(session, { hash: e.mapHash, size: e.mapSize, algoName: e.algoName,
                        username: e.username, avatar: e.avatar, iterations: e.iterationsAvailable })
  }

  function handleRetry(e: LeaderboardEntry) {
    onRetry(e.mapHash, e.mapSize, e.iterationsAvailable)
  }

  return (
    <section id="screen-leaderboard">
      <h2>Leaderboard</h2>
      {empty ? (
        <p id="lb-empty">No leaderboard file configured.</p>
      ) : (
        <table id="lb-table">
          <thead><tr>
            <th>#</th><th>Avatar</th><th>Name</th><th>Map</th>
            <th>Size</th><th>Algo</th><th>Iter Used/Avail</th><th>Score</th><th>Actions</th>
          </tr></thead>
          <tbody>
            {entries.map((e, i) => (
              <tr key={e.id}>
                <td>{i + 1}</td>
                <td>{e.avatar}</td>
                <td>{e.username}</td>
                <td><code>{e.mapHash.slice(0, 8)}</code></td>
                <td>{e.mapSize}</td>
                <td>{e.algoName}</td>
                <td>{e.iterationsUsed} / {e.iterationsAvailable}</td>
                <td><strong>{e.score}</strong></td>
                <td>
                  <button onClick={() => handleReplay(e)}>Replay</button>
                  <button onClick={() => handleRetry(e)}>Retry</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
```

---

### Task 3.10: Delete old frontend files and validate

**Files:** Delete all old vanilla JS frontend

- [ ] Delete old frontend files:
```bash
rm -rf src/main/resources/static/js
rm -rf src/main/resources/static/lib
rm src/main/resources/static/index.html
rm -rf src/test/javascript
rm package.json jest.config.js package-lock.json 2>/dev/null || true
```

- [ ] Copy the CSS to the new frontend:
```bash
mkdir -p src/main/frontend/public/css
cp src/main/resources/static/css/game.css src/main/frontend/public/css/game.css
```
Add a link in `src/main/frontend/index.html`:
```html
<link rel="stylesheet" href="/css/game.css">
```

- [ ] Run Vitest to confirm all frontend tests pass:
```bash
cd src/main/frontend && npm test
```
Expected: all tests green (api.test.ts, GameLogic.test.ts).

- [ ] Run full Gradle build:
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL — all Kotlin JUnit tests pass, Vitest tests pass.

- [ ] Start the server and verify the app loads:
```bash
./gradlew bootRun &
sleep 15 && curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/
```
Expected: `200`

- [ ] Open `http://localhost:8080` in a browser. Verify: setup screen loads, algorithm dropdown populates, avatar picker shows, a game session can be started and the robot moves on the canvas.

---

### Task 3.11: Commit and merge Phase 3

- [ ] Commit:
```bash
git add -A
git commit -m "feat: replace vanilla JS with React + TypeScript (Phase 3 complete)"
```

- [ ] Push and open PR from `feature/cld-pow-p3-react` → `claude-powered`, merge when green.

---

## SELF-REVIEW

### Spec coverage check
- [x] Backend: Kotlin — covered Tasks 2.0–2.7
- [x] Build: Gradle Kotlin DSL — covered Tasks 1.1–1.5 + 2.0
- [x] Frontend: React + TypeScript — covered Tasks 3.1–3.11
- [x] Coroutines for game loop — Task 2.4 (`MemorySessions.kt`)
- [x] Vitest for frontend tests — Tasks 3.4, 3.5
- [x] Single server (Spring Boot serves React SPA) — Task 3.2 (`processResources`)
- [x] Feature branches off `claude-powered` — noted in every phase
- [x] `./gradlew bootRun` and `./gradlew test` equivalences — Task 1.4 + command references
- [x] `./gradlew bootRun --args="--leaderboard.file=./leaderboard.json"` — equivalent documented
- [x] WebSocket topics unchanged — `MemorySessions.kt` publishes to same paths
- [x] REST API shape unchanged — `SessionEndpoint.kt` identical routes/shapes

### Notes for implementer
- Java `java.util.Random` is used in `CellularMaps.kt` intentionally: Kotlin's `kotlin.random.Random` uses a different algorithm, which would generate different maps for the same hash seed.
- `MemorySessions` now requires `@PreDestroy` to cancel the coroutine scope on shutdown — Spring will call this automatically.
- The `Mutex.tryLock()` call in `pause()` returns `Boolean`: if already locked (shouldn't happen), it's a no-op. If it returns `false`, log a warning in production.
- `SilentMessaging` is `internal` — tests in the same package access it directly. If a test outside the engine package needs it, promote to `package` visibility.
