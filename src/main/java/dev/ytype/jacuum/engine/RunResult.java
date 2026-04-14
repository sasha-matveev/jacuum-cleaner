package dev.ytype.jacuum.engine;

import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.RunStatus;
import dev.ytype.jacuum.domain.TraceStep;

import java.util.List;
import java.util.Objects;

public record RunResult(
        RunStatus status,
        int score,
        int cleanedTileCount,
        int reachableTileCount,
        int iterationsUsed,
        int iterationLimit,
        Coordinate currentCoordinate,
        List<TraceStep> trace) {

    public RunResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(currentCoordinate, "currentCoordinate");
        Objects.requireNonNull(trace, "trace");
        trace = List.copyOf(trace);
    }

    public int iterationsRemaining() {
        return Math.max(0, iterationLimit - iterationsUsed);
    }
}
