package dev.ytype.jacuum.engine;

import dev.ytype.jacuum.algo.RobotAlgorithm;
import dev.ytype.jacuum.domain.RoomMap;

import java.util.Objects;
import java.util.function.Supplier;

public record RunRequest(RoomMap map, Supplier<? extends RobotAlgorithm> algorithmFactory, int iterationLimit) {

    public RunRequest {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(algorithmFactory, "algorithmFactory");
        if (iterationLimit < 0) {
            throw new IllegalArgumentException("iterationLimit must be non-negative");
        }
    }
}
