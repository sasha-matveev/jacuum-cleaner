# Jacuum Cleaner — Developer Notes

## Running the app
```bash
./gradlew bootRun
# or with leaderboard file:
./gradlew bootRun --args="--leaderboard.file=./leaderboard.json"
```
Open http://localhost:8080

## Running tests
```bash
./gradlew test
```

## Adding a new algorithm
1. Create a class in `src/main/java/com/jacuum/algo/impl/`
2. Implement `com.jacuum.algo.RobotAlgo`
3. Annotate with `@RobotAlgorithm("My Algo Name")` and `@Scope("prototype")`
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
- Algorithms are prototype-scoped Spring beans; fresh instance per session
