package com.jacuum.algo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DirectionTest {
    @Test void oppositeOfNorthIsSouth() {
        assertThat(Direction.NORTH.opposite()).isEqualTo(Direction.SOUTH);
    }
    @Test void dxOfEastIsOne() {
        assertThat(Direction.EAST.dx()).isEqualTo(1);
    }
    @Test void dyOfSouthIsOne() {
        assertThat(Direction.SOUTH.dy()).isEqualTo(1);
    }
    @Test void allDirectionsHaveUniqueOffsets() {
        for (Direction d : Direction.values()) {
            assertThat(d.dx() * d.dx() + d.dy() * d.dy()).isEqualTo(1);
        }
    }
    @Test void oppositeOfSouthIsNorth() {
        assertThat(Direction.SOUTH.opposite()).isEqualTo(Direction.NORTH);
    }
    @Test void oppositeOfEastIsWest() {
        assertThat(Direction.EAST.opposite()).isEqualTo(Direction.WEST);
    }
    @Test void oppositeOfWestIsEast() {
        assertThat(Direction.WEST.opposite()).isEqualTo(Direction.EAST);
    }
    @Test void dxOfWestIsMinusOne() {
        assertThat(Direction.WEST.dx()).isEqualTo(-1);
    }
    @Test void dyOfNorthIsMinusOne() {
        assertThat(Direction.NORTH.dy()).isEqualTo(-1);
    }
    @Test void doubleOppositeIsIdentity() {
        for (Direction d : Direction.values())
            assertThat(d.opposite().opposite()).isEqualTo(d);
    }
}
