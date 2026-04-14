package dev.ytype.jacuum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void coordinateMovesByDirection() {
        Coordinate origin = new Coordinate(3, 4);
        assertThat(origin.move(Direction.UP)).isEqualTo(new Coordinate(3, 3));
        assertThat(origin.move(Direction.RIGHT)).isEqualTo(new Coordinate(4, 4));
        assertThat(origin.move(Direction.DOWN)).isEqualTo(new Coordinate(3, 5));
        assertThat(origin.move(Direction.LEFT)).isEqualTo(new Coordinate(2, 4));
    }

    @Test
    void coordinateMoveRejectsNullDirection() {
        assertThatThrownBy(() -> new Coordinate(1, 1).move(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("direction");
    }

    @Test
    void sizePresetExposesStableDefaults() {
        assertThat(SizePreset.TINY.iterationDefault()).isLessThan(SizePreset.LARGE.iterationDefault());
        assertThat(SizePreset.MEDIUM.width()).isGreaterThan(SizePreset.SMALL.width());
    }

    @Test
    void roomMapExposesFloorWallAndReachableCount() {
        RoomMap roomMap = new RoomMap(
                4,
                3,
                new Coordinate(1, 1),
                Set.of(new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2)));

        assertThat(roomMap.isFloor(new Coordinate(1, 1))).isTrue();
        assertThat(roomMap.isWall(new Coordinate(0, 0))).isTrue();
        assertThat(roomMap.hasWall(new Coordinate(1, 1), Direction.LEFT)).isTrue();
        assertThat(roomMap.hasWall(new Coordinate(1, 1), Direction.RIGHT)).isFalse();
        assertThat(roomMap.reachableFloorCount()).isEqualTo(3);
    }

    @Test
    void roomMapDefensivelyCopiesFloorTiles() {
        Set<Coordinate> tiles = new LinkedHashSet<>();
        tiles.add(new Coordinate(1, 1));
        RoomMap roomMap = new RoomMap(3, 3, new Coordinate(1, 1), tiles);

        tiles.add(new Coordinate(2, 2));

        assertThat(roomMap.reachableFloorCount()).isEqualTo(1);
        assertThat(roomMap.isWall(new Coordinate(2, 2))).isTrue();
    }

    @Test
    void roomMapRejectsInvalidInput() {
        assertThatThrownBy(() -> new RoomMap(0, 3, new Coordinate(0, 0), Set.of(new Coordinate(0, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width");

        assertThatThrownBy(() -> new RoomMap(3, -1, new Coordinate(0, 0), Set.of(new Coordinate(0, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("height");

        assertThatThrownBy(() -> new RoomMap(3, 3, null, Set.of(new Coordinate(0, 0))))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("start");

        assertThatThrownBy(() -> new RoomMap(3, 3, new Coordinate(0, 0), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("floorTiles");

        assertThatThrownBy(() -> new RoomMap(3, 3, new Coordinate(0, 0), Set.of(new Coordinate(1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start");

        assertThatThrownBy(() -> new RoomMap(3, 3, new Coordinate(0, 0), Set.of(new Coordinate(3, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bounds");

        Set<Coordinate> tilesWithNullEntry = new LinkedHashSet<>(Arrays.asList(new Coordinate(0, 0), null));
        assertThatThrownBy(() -> new RoomMap(3, 3, new Coordinate(0, 0), tilesWithNullEntry))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("null");
    }

    @Test
    void roomMapHasWallRejectsNullArguments() {
        RoomMap roomMap = new RoomMap(3, 3, new Coordinate(1, 1), Set.of(new Coordinate(1, 1)));

        assertThatThrownBy(() -> roomMap.hasWall(null, Direction.UP))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("coordinate");

        assertThatThrownBy(() -> roomMap.hasWall(new Coordinate(1, 1), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("direction");
    }
}