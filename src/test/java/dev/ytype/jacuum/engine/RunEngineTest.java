package dev.ytype.jacuum.engine;

import dev.ytype.jacuum.algo.RobotAlgorithm;
import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.Direction;
import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.RunStatus;
import dev.ytype.jacuum.domain.TileView;
import dev.ytype.jacuum.domain.TraceStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunEngineTest {

    private final RunEngine engine = new RunEngine();

    @Test
    void startTileIsCleanedImmediately() {
        RunSession session = start(singleTileRoom(), () -> new FixedDirectionAlgorithm(Direction.UP), 5);

        RunResult result = session.result();

        assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.cleanedTileCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(1000);
        assertThat(result.currentCoordinate()).isEqualTo(new Coordinate(0, 0));
        assertThat(result.trace()).isEmpty();
    }

    @Test
    void blockedMovementConsumesOneIteration() {
        RunSession session = start(twoTileRoom(), () -> new FixedDirectionAlgorithm(Direction.UP), 5);

        RunResult result = session.step(1);

        assertThat(result.iterationsUsed()).isEqualTo(1);
        assertThat(result.cleanedTileCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(999);
        assertThat(result.trace()).hasSize(1);

        TraceStep step = result.trace().get(0);
        assertThat(step.blocked()).isTrue();
        assertThat(step.previousCoordinate()).isEqualTo(new Coordinate(0, 0));
        assertThat(step.resultingCoordinate()).isEqualTo(new Coordinate(0, 0));
        assertThat(step.requestedDirection()).isEqualTo(Direction.UP);
    }

    @Test
    void successfulMovementCleansDirtyDestination() {
        RunSession session = start(twoTileRoom(), () -> new FixedDirectionAlgorithm(Direction.RIGHT), 5);

        RunResult result = session.step(1);

        assertThat(result.cleanedTileCount()).isEqualTo(2);
        assertThat(result.score()).isEqualTo(1999);
        assertThat(result.currentCoordinate()).isEqualTo(new Coordinate(1, 0));
        assertThat(result.trace()).hasSize(1);

        TraceStep step = result.trace().get(0);
        assertThat(step.blocked()).isFalse();
        assertThat(step.newlyCleaned()).isTrue();
        assertThat(step.resultingCoordinate()).isEqualTo(new Coordinate(1, 0));
    }

    @Test
    void scoreEqualsCleanedTilesTimesThousandMinusIterationsUsed() {
        RunSession session = start(threeTileLine(), () -> new CyclingDirectionAlgorithm(Direction.RIGHT, Direction.LEFT), 5);

        RunResult result = session.step(2);

        assertThat(result.cleanedTileCount()).isEqualTo(2);
        assertThat(result.iterationsUsed()).isEqualTo(2);
        assertThat(result.score()).isEqualTo(result.cleanedTileCount() * 1000 - result.iterationsUsed());
    }

    @Test
    void allCleanMapCompletesBeforeLimit() {
        RunSession session = start(singleTileRoom(), () -> new FixedDirectionAlgorithm(Direction.UP), 5);

        RunResult result = session.step(5);

        assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.iterationsUsed()).isZero();
        assertThat(result.trace()).isEmpty();
    }

    @Test
    void algorithmExceptionEndsWithFailedAndZeroScore() {
        RunSession session = start(twoTileRoom(), ThrowingAlgorithm::new, 5);

        RunResult result = session.step(1);

        assertThat(result.status()).isEqualTo(RunStatus.FAILED);
        assertThat(result.score()).isZero();
        assertThat(result.iterationsUsed()).isZero();
        assertThat(result.trace()).isEmpty();
    }

    @Test
    void traceCapturesRequestedDirectionAndResultingCoordinate() {
        RunSession session = start(twoTileRoom(), () -> new FixedDirectionAlgorithm(Direction.RIGHT), 5);

        RunResult result = session.step(1);

        assertThat(result.trace()).hasSize(1);
        TraceStep step = result.trace().get(0);
        assertThat(step.iteration()).isEqualTo(1);
        assertThat(step.previousCoordinate()).isEqualTo(new Coordinate(0, 0));
        assertThat(step.requestedDirection()).isEqualTo(Direction.RIGHT);
        assertThat(step.resultingCoordinate()).isEqualTo(new Coordinate(1, 0));
        assertThat(step.blocked()).isFalse();
        assertThat(step.newlyCleaned()).isTrue();
    }

    @Test
    void stepRejectsNegativeCounts() {
        RunSession session = start(singleTileRoom(), () -> new FixedDirectionAlgorithm(Direction.UP), 5);

        assertThatThrownBy(() -> session.step(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }

    private RunSession start(RoomMap map, Supplier<RobotAlgorithm> algorithmFactory, int iterationLimit) {
        return engine.start(new RunRequest(map, algorithmFactory, iterationLimit));
    }

    private static RoomMap singleTileRoom() {
        return new RoomMap(1, 1, new Coordinate(0, 0), Set.of(new Coordinate(0, 0)));
    }

    private static RoomMap twoTileRoom() {
        return new RoomMap(
                2,
                1,
                new Coordinate(0, 0),
                Set.of(new Coordinate(0, 0), new Coordinate(1, 0)));
    }

    private static RoomMap threeTileLine() {
        return new RoomMap(
                3,
                1,
                new Coordinate(0, 0),
                Set.of(new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(2, 0)));
    }

    private static final class FixedDirectionAlgorithm implements RobotAlgorithm {
        private final Direction direction;

        private FixedDirectionAlgorithm(Direction direction) {
            this.direction = direction;
        }

        @Override
        public Direction next(TileView tile) {
            return direction;
        }
    }

    private static final class CyclingDirectionAlgorithm implements RobotAlgorithm {
        private final List<Direction> directions;
        private int index;

        private CyclingDirectionAlgorithm(Direction... directions) {
            this.directions = List.of(directions);
        }

        @Override
        public Direction next(TileView tile) {
            Direction direction = directions.get(index % directions.size());
            index++;
            return direction;
        }
    }

    private static final class ThrowingAlgorithm implements RobotAlgorithm {
        @Override
        public Direction next(TileView tile) {
            throw new IllegalStateException("boom");
        }
    }
}
