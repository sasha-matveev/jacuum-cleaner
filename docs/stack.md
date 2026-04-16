# Technology Stack

## Runtime

- Java 21
- Spring Boot 4.0.5
- Maven Wrapper

Java 21 matches the requested runtime and gives access to records, sealed types where useful, and modern collection APIs. Spring Boot 4.0.5 is the current stable Spring Boot release and supports Java 21.

## Server

- Spring Web MVC for HTTP endpoints and static resource serving
- Jackson for JSON request/response and leaderboard persistence
- Spring configuration properties for optional local file paths
- Spring component scanning for algorithm discovery

The first version does not need WebFlux, WebSockets, messaging, remote databases, or authentication. The application is local-first and should remain simple to run.

## UI

- Plain HTML, CSS, and JavaScript served by Spring Boot static resources
- Browser `localStorage` for UI preferences
- `fetch` for JSON API communication
- CSS transitions or `requestAnimationFrame` for smooth robot movement

The UI is intentionally thin. A frontend framework is not selected for the first version because the product needs one local screen, simple controls, and server-owned simulation state.

## Testing

- JUnit 5
- AssertJ
- Spring Boot Test for web and application wiring tests
- Maven Surefire

Core domain, map generation, engine, scoring, and leaderboard behavior should be tested without starting the full Spring context where possible. Spring tests are reserved for controller and registry wiring.

## Random Data

- Datafaker 2.5.4

The task asks for Faker-style generated data. Datafaker is the maintained Java faker library choice for generated usernames and other non-critical display data.

## Version Sources

- Spring Boot current stable version and system requirements: https://docs.spring.io/spring-boot/index.html and https://docs.spring.io/spring-boot/system-requirements.html
- Datafaker current release: https://www.datafaker.net/documentation/getting-started/

## Local Persistence

- JSON file storage through Jackson

Leaderboard persistence is optional and local. A JSON file is easy to inspect, easy to version during development, and avoids adding a database before the product needs one.

## Build And Run

The application should support a single primary command:

```powershell
.\mvnw spring-boot:run
```

Tests should run with:

```powershell
.\mvnw test
```

The implementation should include Maven Wrapper files so the project does not depend on a globally installed Maven.

## Excluded For Initial Version

- Remote database
- User accounts or authentication
- WebSockets
- Frontend build pipeline
- Docker packaging
- Multiplayer or shared remote leaderboard
- Plugin loading from external jars

These choices keep the first product local, testable, and easy to resume.
