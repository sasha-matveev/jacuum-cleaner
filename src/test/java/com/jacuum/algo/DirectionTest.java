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
}
