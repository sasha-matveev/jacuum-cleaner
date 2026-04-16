package com.jacuum.game;

/**
 * Result returned after executing one game iteration.
 */
public record StepResult(
    int robotX,
    int robotY,
    boolean justCleaned,
    int cleanedTiles,
    int totalFloorTiles,
    int iterationsUsed,
    int maxIterations,
    int score,
    GameStatus status
) {}
