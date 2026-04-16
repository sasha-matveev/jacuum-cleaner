package com.jacuum.game;

import com.jacuum.algo.RobotAlgo;
import com.jacuum.map.GameMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the mutable state of one game run.
 */
@Getter
public class GameSession {

    private final String sessionId;
    private final GameMap map;
    private final RobotAlgo algo;
    private final String algoId;
    private final String username;
    private final String avatar;
    private final int maxIterations;

    private int robotX;
    private int robotY;
    private int iterationsUsed;
    private GameStatus status;

    /** Trace of directions taken — used for leaderboard replay. */
    private final List<String> trace = new ArrayList<>();

    public GameSession(String sessionId, GameMap map, RobotAlgo algo, String algoId,
                       String username, String avatar, int maxIterations) {
        this.sessionId = sessionId;
        this.map = map;
        this.algo = algo;
        this.algoId = algoId;
        this.username = username;
        this.avatar = avatar;
        this.maxIterations = maxIterations;
        this.robotX = map.getStartX();
        this.robotY = map.getStartY();
        this.status = GameStatus.RUNNING;
        // Mark starting tile as cleaned
        map.markCleaned(robotX, robotY);
    }

    public void setRobotPosition(int x, int y) {
        this.robotX = x;
        this.robotY = y;
    }

    public void incrementIterations() {
        this.iterationsUsed++;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public void addTrace(String direction) {
        trace.add(direction);
    }

    public List<String> getTrace() {
        return Collections.unmodifiableList(trace);
    }

    public int getScore() {
        if (status == GameStatus.FAILED) return 0;
        return Math.max(0, map.countCleanedTiles() * 100 - iterationsUsed);
    }

    public boolean isActive() {
        return status == GameStatus.RUNNING || status == GameStatus.PAUSED;
    }
}
