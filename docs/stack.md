# Technology Stack

## Backend

| Concern | Choice | Rationale |
|---|---|---|
| Language | Java 21 | Specified in requirements; virtual threads, records, pattern matching available |
| Framework | Spring Boot 3.x (latest stable) | Specified; auto-configuration, embedded Tomcat, WebSocket support |
| Build | Maven (mvnw wrapper) | Standard Spring ecosystem tooling |
| WebSocket | Spring WebSocket + STOMP over SockJS | Real-time iteration streaming without polling; built into Spring |
| JSON | Jackson (via spring-boot-starter-web) | Industry standard; already on classpath |
| Random data | java-faker 1.x | Specified; hero names, avatar suggestions |
| Leaderboard DB | Plain JSON file (Jackson) | No remote connections needed; human-readable; portable |
| Testing | JUnit 5 + AssertJ + Spring Boot Test + MockMvc | Built into Spring Boot; expressive assertions |
| Logging | SLF4J + Logback (via Spring Boot default) | Standard; zero config needed |

## Frontend

| Concern | Choice | Rationale |
|---|---|---|
| Language | Vanilla JS (ES2020) | Thin client per spec; no build step; no npm |
| Rendering | HTML Canvas 2D | Grid animation, robot sliding motion |
| WebSocket client | SockJS + STOMP.js (local copies in static/) | Matches server transport; works offline |
| Styles | Plain CSS | Sufficient for game grid UI |
| Persistence | localStorage | Save user preferences between page loads |

## Project Structure

```
jacuum-cleaner/
├── src/
│   ├── main/
│   │   ├── java/com/jacuum/
│   │   │   ├── JacuumApplication.java          # entry point
│   │   │   ├── algo/                           # RobotAlgo interface, @RobotAlgorithm, registry
│   │   │   │   ├── RobotAlgo.java
│   │   │   │   ├── RobotAlgorithm.java         # annotation
│   │   │   │   ├── AlgoRegistry.java
│   │   │   │   └── impl/
│   │   │   │       ├── RandomAlgo.java
│   │   │   │       └── AlwaysLeftAlgo.java
│   │   │   ├── model/                          # Direction, Tile, Map, Session, etc.
│   │   │   ├── map/                            # MapGenerator, CellularAutomataGenerator
│   │   │   ├── engine/                         # GameEngine, SessionStore
│   │   │   ├── web/                            # REST controllers, WebSocket config
│   │   │   └── leaderboard/                    # LeaderboardEntry, LeaderboardStore
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/                         # index.html, app.js, game.css, sockjs, stomp
│   └── test/
│       └── java/com/jacuum/
│           ├── map/
│           ├── engine/
│           ├── algo/
│           ├── leaderboard/
│           └── web/
├── docs/
│   ├── task-definition.md
│   ├── stack.md                                # this file
│   └── superpowers/specs/
│       └── 2026-04-16-jacuum-cleaner-design.md
└── pom.xml
```

## Key Dependencies (pom.xml)

```xml
spring-boot-starter-web
spring-boot-starter-websocket
spring-boot-starter-test
com.github.javafaker:javafaker:1.0.2
```

No database driver, no ORM, no extra persistence layer.
