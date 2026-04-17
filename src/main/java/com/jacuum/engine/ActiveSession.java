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
