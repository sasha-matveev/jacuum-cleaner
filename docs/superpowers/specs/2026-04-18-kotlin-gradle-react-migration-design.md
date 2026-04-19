# Migration Design: Java/Maven/Vanilla JS → Kotlin/Gradle/React

**Date:** 2026-04-18  
**Status:** Approved  
**Base branch:** `claude-powered`  
**Branch strategy:** Feature branches named `feature/cld-pow-<slug>` branched from and merged back to `claude-powered`. One branch per phase (and sub-branches per package within Phase 2 if desired). `claude-powered` is never merged to `main` as part of this migration.

---

## Goal

Migrate the Jacuum Cleaner project to:

- **Backend language:** Kotlin (JVM 21)
- **Build system:** Gradle with Kotlin DSL (`build.gradle.kts`)
- **Frontend:** React 18 + TypeScript, bundled by Vite
- **Frontend tests:** Vitest + React Testing Library
- **Game loop concurrency:** Kotlin coroutines (replacing Java virtual threads)

All other requirements stay the same: single Spring Boot process, `./gradlew bootRun` to run, `./gradlew test` to test, same REST API, same WebSocket topics, same leaderboard format.

---

## Phase Boundary Rule

**Every phase must end with `./gradlew test` passing and the app running correctly.** No phase ends in a broken state. Each phase is a separate branch and PR.

---

## Phase 1 — Maven → Gradle

**Goal:** Replace the build system without touching any source code.

### Files removed
- `pom.xml`
- `mvnw`, `mvnw.cmd`
- `.mvn/`

### Files added
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`, `gradlew.bat`

### Gradle configuration
- Spring Boot Gradle plugin (`org.springframework.boot`)
- `io.spring.dependency-management` plugin
- `java` plugin (Java source still compiled at this stage)
- `com.github.node-gradle.node` plugin for frontend tasks

### Frontend tasks (Phase 1 — still Jest)
| Task | Command |
|---|---|
| `npmInstall` | `npm install` |
| `npmTest` | `npm test` |
| `npmBuild` | no-op in Phase 1 (no bundler yet) |

`npmInstall` wired into `processResources`; `npmTest` wired into Gradle's `test` lifecycle.

### Command equivalences
| Maven | Gradle |
|---|---|
| `./mvnw spring-boot:run` | `./gradlew bootRun` |
| `./mvnw spring-boot:run -Dspring-boot.run.arguments="--leaderboard.file=./leaderboard.json"` | `./gradlew bootRun --args="--leaderboard.file=./leaderboard.json"` |
| `./mvnw test` | `./gradlew test` |

### Validation gate
`./gradlew test` passes with zero changes to Java or JS source files.

---

## Phase 2 — Java → Kotlin

**Goal:** Rewrite all backend source files in Kotlin, package by package. Replace virtual threads with coroutines.

### Gradle additions
```kotlin
plugins {
    kotlin("jvm") version "..."
    kotlin("plugin.spring") version "..."
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

kotlin {
    jvmToolchain(21)
}
```

### Migration order
Dependencies-first, leaves-last. Each package is a separate commit; `./gradlew test` passes after each.

| Step | Package | Notes |
|---|---|---|
| 1 | `com.jacuum.algo` | Interfaces, enums, `@RobotAlgorithm` annotation — pure Kotlin, no Spring |
| 2 | `com.jacuum.map` | Map generation, no coroutines needed |
| 3 | `com.jacuum.leaderboard` | File I/O, minimal Spring wiring |
| 4 | `com.jacuum.engine` | Game loop — virtual threads → coroutines (see below) |
| 5 | `com.jacuum.web` | REST controllers, WebSocket config, DTOs |
| 6 | Root | `JacuumApplication.kt` |

Each step: delete the `.java` files for the package, add `.kt` equivalents, run `./gradlew test`.

### Coroutine design (engine package)

Replaces `Thread.ofVirtual().start { runLoop() }`:

- `MemorySessions` owns a `CoroutineScope(SupervisorJob() + Dispatchers.Default)` — created in constructor, cancelled on `@PreDestroy`
- Each session runs as a `Job` returned by `scope.launch { runLoop(s, algo) }`
- Pause: replace `Thread.sleep(50)` busy-wait with a `Mutex` — game loop calls `pauseMutex.lock()` / `pauseMutex.unlock()` at each iteration boundary
- Stop: `job.cancelAndJoin()` — the loop checks `isActive` (cooperative cancellation)
- Finish signals: unchanged — STOMP publish on `/topic/session/{id}/events` and `/topic/session/{id}/status`

### Kotlin OOP conventions (yegor256 rules applied)

- All classes `final` by default in Kotlin — no `open` unless extending an abstract class
- No `data class` for domain objects with behavior; `data class` only for structural DTOs and events
- No `companion object` with static methods
- No `object` singletons
- All fields `val` (immutable); mutable engine state uses `@Volatile var` only where the current Java code uses it
- Checked exceptions don't exist in Kotlin — internal code uses typed returns or propagates; `@Throws` added only on public API methods called from Java interop contexts
- No `null` returns; use `kotlin.Result`, nullable types with `?: throw`, or the Null Object pattern

### Validation gate
`./gradlew test` passes after each package commit. No Java files remain in `src/main/java/` after the final commit of this phase.

---

## Phase 3 — Vanilla JS → React + TypeScript

**Goal:** Replace the static JS frontend with a React + TypeScript SPA bundled by Vite. Frontend tests move from Jest to Vitest.

### Frontend location
```
src/main/frontend/        ← new Vite project root
  index.html
  vite.config.ts
  tsconfig.json
  package.json
  src/
    main.tsx
    api.ts                ← api.js
    types.ts              ← shared TypeScript types (SessionView, LeaderboardEntry, etc.)
    App.tsx               ← app.js (screen router)
    SetupScreen.tsx       ← setup.js
    GameScreen.tsx        ← game.js + game-logic.js
    LeaderboardScreen.tsx ← leaderboard.js
    __tests__/
      api.test.ts         ← api.test.js
      GameScreen.test.tsx ← game-logic.test.js
```

The old `src/main/resources/static/` JS files and `src/test/javascript/` are deleted.

### npm dependencies
| Package | Role |
|---|---|
| `react`, `react-dom` | UI framework |
| `typescript` | Language |
| `vite`, `@vitejs/plugin-react` | Bundler / dev server |
| `vitest`, `@vitest/ui` | Test runner |
| `@testing-library/react`, `@testing-library/user-event` | Component tests |
| `@stomp/stompjs`, `sockjs-client` | WebSocket (replaces vendored `lib/` files) |
| `@types/react`, `@types/react-dom`, `@types/sockjs-client` | Type definitions |

### Gradle wiring
`build.gradle.kts` Node plugin configuration:
- Working directory: `src/main/frontend`
- `npmInstall` runs `npm ci`
- `npmBuild` runs `vite build --outDir ../../../../build/frontend` — wired into `processResources`
- `npmTest` runs `vitest run` — wired into Gradle `test` lifecycle
- `processResources` copies `build/frontend/**` into `build/resources/main/static/`

### Dev workflow
- Production / CI: `./gradlew bootRun` — serves the pre-built bundle
- Frontend dev: `npm run dev` inside `src/main/frontend/` (Vite dev server on `:5173` with `proxy: { '/api': 'http://localhost:8080', '/ws': 'http://localhost:8080' }`)

### Vite proxy (dev only)
```ts
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080',
    '/ws':  { target: 'ws://localhost:8080', ws: true }
  }
}
```

### Test migration
| Old | New |
|---|---|
| `src/test/javascript/api.test.js` (Jest) | `src/main/frontend/src/__tests__/api.test.ts` (Vitest) |
| `src/test/javascript/game-logic.test.js` (Jest) | `src/main/frontend/src/__tests__/GameScreen.test.tsx` (Vitest + RTL) |

Vitest API is Jest-compatible for `describe/test/expect` — migration is mostly syntax and import changes.

### Files removed
- `src/main/resources/static/js/` (all files)
- `src/main/resources/static/lib/` (vendored sockjs/stomp)
- `src/main/resources/static/index.html`
- `src/test/javascript/`
- Root `package.json`, `jest.config.js`, `package-lock.json`

### Validation gate
`./gradlew test` passes (backend JUnit + Kotlin tests) and Vitest reports all frontend tests green. App loads correctly at `http://localhost:8080` after `./gradlew bootRun`.

---

## Unchanged Throughout All Phases

- REST API endpoints and response shapes
- WebSocket topics (`/topic/session/{id}/events`, `/topic/session/{id}/status`)
- Leaderboard JSON file format
- `RobotAlgo`, `Tile`, `Direction` public interfaces (algorithm implementations remain compatible)
- Spring Boot application properties (`game.default-iterations`, `game.max-sessions`, `leaderboard.file`)
- Port: `8080`
