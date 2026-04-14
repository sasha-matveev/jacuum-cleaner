package dev.ytype.jacuum.engine;

import dev.ytype.jacuum.algo.RobotAlgorithm;
import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.Direction;
import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.RunStatus;
import dev.ytype.jacuum.domain.TileView;
import dev.ytype.jacuum.domain.TraceStep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class RunSession {

    private final RoomMap map;
    private final RobotAlgorithm algorithm;
    private final int iterationLimit;
    private final LinkedHashSet<Coordinate> cleanedTiles = new LinkedHashSet<>();
    private final List<TraceStep> trace = new ArrayList<>();
    private Coordinate currentCoordinate;
    private int iterationsUsed;
    private RunStatus status;
    private int score;

    public RunSession(RunRequest request) {
        Objects.requireNonNull(request, "request");
        this.map = request.map();
        this.algorithm = Objects.requireNonNull(request.algorithmFactory().get(), "algorithm");
        this.iterationLimit = request.iterationLimit();
        this.currentCoordinate = map.start();
        this.cleanedTiles.add(currentCoordinate);
        this.score = ScoreCalculator.score(cleanedTiles.size(), iterationsUsed);
        this.status = isNaturallyFinished() ? RunStatus.COMPLETED : RunStatus.RUNNING;
    }

    public RunResult step(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        if (status != RunStatus.RUNNING || count == 0) {
            return result();
        }

        int stepsToAttempt = Math.min(count, iterationLimit - iterationsUsed);
        for (int i = 0; i < stepsToAttempt && status == RunStatus.RUNNING; i++) {
            TileView tile = currentTileView();
            final Direction requestedDirection;
            try {
                requestedDirection = Objects.requireNonNull(algorithm.next(tile), "algorithm returned null direction");
            } catch (RuntimeException exception) {
                fail();
                break;
            }

            Coordinate previousCoordinate = currentCoordinate;
            boolean blocked = map.hasWall(previousCoordinate, requestedDirection);
            Coordinate resultingCoordinate = blocked ? previousCoordinate : previousCoordinate.move(requestedDirection);
            boolean newlyCleaned = !blocked && cleanedTiles.add(resultingCoordinate);

            iterationsUsed++;
            currentCoordinate = resultingCoordinate;
            score = ScoreCalculator.score(cleanedTiles.size(), iterationsUsed);

            trace.add(new TraceStep(
                    iterationsUsed,
                    previousCoordinate,
                    requestedDirection,
                    resultingCoordinate,
                    blocked,
                    newlyCleaned,
                    score));

            if (isNaturallyFinished()) {
                status = RunStatus.COMPLETED;
            }
        }

        return result();
    }

    public RunResult result() {
        return new RunResult(
                status,
                score,
                cleanedTiles.size(),
                map.reachableFloorCount(),
                iterationsUsed,
                iterationLimit,
                currentCoordinate,
                trace);
    }

    private TileView currentTileView() {
        return new TileView(
                currentCoordinate,
                cleanedTiles.contains(currentCoordinate),
                iterationsUsed + 1,
                map.hasWall(currentCoordinate, Direction.UP),
                map.hasWall(currentCoordinate, Direction.RIGHT),
                map.hasWall(currentCoordinate, Direction.DOWN),
                map.hasWall(currentCoordinate, Direction.LEFT));
    }

    private boolean isNaturallyFinished() {
        return cleanedTiles.size() == map.reachableFloorCount() || iterationsUsed >= iterationLimit;
    }

    private void fail() {
        status = RunStatus.FAILED;
        score = 0;
    }
}
