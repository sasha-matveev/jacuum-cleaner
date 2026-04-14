package dev.ytype.jacuum.domain;

public record TraceStep(
        int iteration,
        Coordinate previousCoordinate,
        Direction requestedDirection,
        Coordinate resultingCoordinate,
        boolean blocked,
        boolean newlyCleaned,
        int scoreAfterStep) {
}