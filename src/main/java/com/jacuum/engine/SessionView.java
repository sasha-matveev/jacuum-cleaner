package com.jacuum.engine;

public record SessionView(
    String id,
    RunStatus status,
    int robotX,
    int robotY,
    int score,
    int totalCleaned,
    int iterationsUsed,
    int iterationsAvailable,
    int totalFloor,
    FinishReason finishReason
) {}
