package com.jacuum.engine;

import com.jacuum.algo.Direction;

/**
 * Snapshot of one robot iteration, streamed to the client via WebSocket.
 *
 * <p>{@code direction} is {@code null} when the robot attempted to move into
 * a wall and stayed in place. {@code finishReason} is {@code null} when
 * {@code finished} is {@code false}.
 */
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
