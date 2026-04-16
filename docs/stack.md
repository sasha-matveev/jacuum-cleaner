# Technology Stack

## Backend

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Language | Java 21 | Required. Virtual threads, records, pattern matching. |
| Framework | Spring Boot 3.4.x | Required. Latest stable, Java 21 native. |
| Build | Maven (Maven Wrapper) | Standard, single `./mvnw spring-boot:run` entry point. |
| Web | Spring MVC (embedded Tomcat) | Serves static UI + REST API on same port. |
| Real-time | Spring SSE (`SseEmitter`) | Server → client stream for iteration events. Simpler than WebSocket for one-directional flow. |
| Persistence | Spring Data JPA + H2 (file mode) | File-based leaderboard DB, zero config, embedded. File path configurable via `leaderboard.path`. App works without it. |
| Fake data | Java Faker (`com.github.javafaker`) | Robot names, hero usernames. |
| Utilities | Lombok | Reduce boilerplate on model classes. |
| JSON | Jackson (Spring Boot default) | REST request/response serialization. |
| Testing | JUnit 5 + AssertJ + Mockito | Unit tests for infra and algo contract. Spring Boot Test for integration. |

## Frontend

| Concern | Choice | Rationale |
|---------|--------|-----------|
| HTML/CSS | Vanilla, served as static resource | "UI is thin" — no build step needed. |
| JS | Vanilla ES2022 (modules) | Simple interactivity; no framework overhead. |
| Map rendering | HTML5 `<canvas>` | Smooth robot animation, tile drawing. |
| Real-time | `EventSource` (SSE) | Native browser API, consumes the `/api/game/{id}/stream` endpoint. |
| State persistence | `localStorage` | Save user preferences between page loads (username, avatar, iterations, algo, map size). |

## Project Structure

```
src/
  main/
    java/com/jacuum/
      JacuumApplication.java          # Entry point
      map/                            # Map model + generation
        Direction.java
        Tile.java                     # Interface
        TileImpl.java
        GameMap.java
        MapGenerator.java
        RoomMapGenerator.java
      algo/                           # Robot algo interface + implementations
        RobotAlgo.java                # Interface
        VacuumAlgo.java               # @VacuumAlgo annotation
        AlgoRegistry.java             # Spring component: collects all @VacuumAlgo beans
        impl/
          RandomAlgo.java
          AlwaysLeftAlgo.java
      game/                           # Game engine
        GameSession.java
        GameEngine.java
        SseGameService.java           # Streams iteration events via SSE
      leaderboard/                    # Optional persistence
        LeaderboardEntry.java
        LeaderboardRepository.java
        LeaderboardService.java
      api/                            # REST controllers
        MapController.java
        GameController.java
        LeaderboardController.java
    resources/
      static/                         # UI files
        index.html
        app.js
        style.css
      application.properties
  test/
    java/com/jacuum/
      map/                            # Map gen tests
      algo/                           # Algo contract tests (parameterized)
      game/                           # Game engine tests
      leaderboard/                    # Leaderboard I/O tests
```

## Key Dependencies (pom.xml)

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
com.h2database:h2
com.github.javafaker:javafaker
org.projectlombok:lombok
spring-boot-starter-test
```
