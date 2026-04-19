package com.jacuum.algo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DirectionTest {
    @Test fun oppositeOfNorthIsSouth() = assertThat(Direction.NORTH.opposite()).isEqualTo(Direction.SOUTH)
    @Test fun dxOfEastIsOne() = assertThat(Direction.EAST.dx()).isEqualTo(1)
    @Test fun dyOfSouthIsOne() = assertThat(Direction.SOUTH.dy()).isEqualTo(1)
    @Test fun allDirectionsHaveUniqueOffsets() {
        for (d in Direction.entries) assertThat(d.dx() * d.dx() + d.dy() * d.dy()).isEqualTo(1)
    }
    @Test fun oppositeOfSouthIsNorth() = assertThat(Direction.SOUTH.opposite()).isEqualTo(Direction.NORTH)
    @Test fun oppositeOfEastIsWest() = assertThat(Direction.EAST.opposite()).isEqualTo(Direction.WEST)
    @Test fun oppositeOfWestIsEast() = assertThat(Direction.WEST.opposite()).isEqualTo(Direction.EAST)
    @Test fun dxOfWestIsMinusOne() = assertThat(Direction.WEST.dx()).isEqualTo(-1)
    @Test fun dyOfNorthIsMinusOne() = assertThat(Direction.NORTH.dy()).isEqualTo(-1)
    @Test fun doubleOppositeIsIdentity() {
        for (d in Direction.entries) assertThat(d.opposite().opposite()).isEqualTo(d)
    }
}
