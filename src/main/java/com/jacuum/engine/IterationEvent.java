package com.jacuum.engine;

import com.jacuum.algo.Direction;

public record IterationEvent(
    String sessionId,
    int iteration,
    Direction direction,
    int robotX,
    int robotY,
    int score,
    int totalCleaned,
    int totalFloor,
    boolean finished,
    FinishReason finishReason
) {}
