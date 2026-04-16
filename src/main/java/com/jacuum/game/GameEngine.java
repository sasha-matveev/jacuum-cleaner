package com.jacuum.game;

import com.jacuum.algo.AlgoRegistry;
import com.jacuum.algo.RobotAlgo;
import com.jacuum.map.Direction;
import com.jacuum.map.GameMap;
import com.jacuum.map.MapGenerator;
import com.jacuum.map.SizePreset;
import com.jacuum.map.Tile;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game logic: creates sessions, advances them one step at a time.
 */
@Service
public class GameEngine {

    private final MapGenerator mapGenerator;
    private final AlgoRegistry algoRegistry;
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Faker faker = new Faker();

    public GameEngine(MapGenerator mapGenerator, AlgoRegistry algoRegistry) {
        this.mapGenerator = mapGenerator;
        this.algoRegistry = algoRegistry;
    }

    /**
     * Create and register a new game session.
     *
     * @param hash          map seed (or random UUID if blank)
     * @param sizeLabel     size preset label (tiny/small/medium/large)
     * @param algoId        registered algo bean name
     * @param username      player name (generated if blank)
     * @param avatar        avatar identifier
     * @param maxIterations iteration budget
     * @return the newly created session
     */
    public GameSession startSession(String hash, String sizeLabel, String algoId,
                                    String username, String avatar, int maxIterations) {
        String resolvedHash = (hash == null || hash.isBlank()) ? UUID.randomUUID().toString() : hash;
        SizePreset size = SizePreset.fromString(sizeLabel);
        GameMap map = mapGenerator.generate(resolvedHash, size);
        RobotAlgo algo = algoRegistry.getAlgo(algoId);

        String resolvedName = (username == null || username.isBlank())
                ? faker.superhero().name()
                : username;

        String sessionId = UUID.randomUUID().toString();
        GameSession session = new GameSession(sessionId, map, algo, algoId,
                resolvedName, avatar, maxIterations);
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Execute one iteration of the given session.
     *
     * @return the step result; if already finished/failed/aborted returns current state
     */
    public StepResult step(String sessionId) {
        GameSession session = getSession(sessionId);

        if (session.getStatus() != GameStatus.RUNNING) {
            return snapshot(session, false);
        }

        GameMap map = session.getMap();
        int x = session.getRobotX();
        int y = session.getRobotY();

        Direction dir;
        try {
            Tile tile = map.tileViewAt(x, y);
            dir = session.getAlgo().next(tile);
            if (dir == null) throw new IllegalStateException("Algo returned null direction");
        } catch (Exception e) {
            session.setStatus(GameStatus.FAILED);
            return snapshot(session, false);
        }

        session.addTrace(dir.name());
        session.incrementIterations();

        int nx = x + dir.dx;
        int ny = y + dir.dy;
        boolean moved = !map.isWall(nx, ny);
        boolean justCleaned = false;
        if (moved) {
            session.setRobotPosition(nx, ny);
            justCleaned = !map.isCleaned(nx, ny);
            map.markCleaned(nx, ny);
        }

        // Check terminal conditions
        if (map.countCleanedTiles() >= map.countFloorTiles()
                || session.getIterationsUsed() >= session.getMaxIterations()) {
            session.setStatus(GameStatus.FINISHED);
        }

        return snapshot(session, justCleaned);
    }

    public void pauseSession(String sessionId) {
        GameSession s = getSession(sessionId);
        if (s.getStatus() == GameStatus.RUNNING) s.setStatus(GameStatus.PAUSED);
    }

    public void resumeSession(String sessionId) {
        GameSession s = getSession(sessionId);
        if (s.getStatus() == GameStatus.PAUSED) s.setStatus(GameStatus.RUNNING);
    }

    public void abortSession(String sessionId) {
        GameSession s = getSession(sessionId);
        if (s.isActive()) s.setStatus(GameStatus.ABORTED);
    }

    public GameSession getSession(String sessionId) {
        GameSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalArgumentException("Unknown session: " + sessionId);
        return s;
    }

    private StepResult snapshot(GameSession s, boolean justCleaned) {
        GameMap map = s.getMap();
        return new StepResult(
            s.getRobotX(), s.getRobotY(),
            justCleaned,
            map.countCleanedTiles(),
            map.countFloorTiles(),
            s.getIterationsUsed(),
            s.getMaxIterations(),
            s.getScore(),
            s.getStatus()
        );
    }
}
