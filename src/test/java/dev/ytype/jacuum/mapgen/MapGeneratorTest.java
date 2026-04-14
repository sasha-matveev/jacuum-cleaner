package dev.ytype.jacuum.mapgen;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.SizePreset;
import org.junit.jupiter.api.Test;

class MapGeneratorTest {

    private final MapGenerator generator = new MapGenerator();

    @Test
    void sameHashAndPresetProduceEqualRoomMap() {
        RoomMap first = generator.generate("demo-hash", SizePreset.SMALL).map();
        RoomMap second = generator.generate("demo-hash", SizePreset.SMALL).map();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentHashesUsuallyProduceDifferentLayouts() {
        RoomMap first = generator.generate("alpha", SizePreset.MEDIUM).map();
        RoomMap second = generator.generate("beta", SizePreset.MEDIUM).map();

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void allFloorTilesAreReachableFromStart() {
        RoomMap map = generator.generate("reachable-check", SizePreset.LARGE).map();

        assertThat(MapValidator.isReachableFromStart(map)).isTrue();
    }

    @Test
    void everyOutsideBoundaryBehavesAsWall() {
        RoomMap map = generator.generate("boundary-check", SizePreset.SMALL).map();

        for (int x = 0; x < map.width(); x++) {
            assertThat(map.isWall(new Coordinate(x, -1))).isTrue();
            assertThat(map.isWall(new Coordinate(x, map.height()))).isTrue();
        }
        for (int y = 0; y < map.height(); y++) {
            assertThat(map.isWall(new Coordinate(-1, y))).isTrue();
            assertThat(map.isWall(new Coordinate(map.width(), y))).isTrue();
        }
    }

    @Test
    void singleTinyMapHasAtLeastOneFloorTile() {
        RoomMap map = generator.generate("tiny-map", SizePreset.TINY).map();

        assertThat(map.reachableFloorCount()).isGreaterThan(0);
        assertThat(map.isFloor(map.start())).isTrue();
    }
}
