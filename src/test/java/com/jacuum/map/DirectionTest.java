package com.jacuum.map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DirectionTest {

    @Test
    void oppositesAreSymmetric() {
        for (Direction d : Direction.values()) {
            assertThat(d.opposite().opposite()).isEqualTo(d);
        }
    }

    @Test
    void deltasSumToZeroForOpposite() {
        for (Direction d : Direction.values()) {
            assertThat(d.dx + d.opposite().dx).isZero();
            assertThat(d.dy + d.opposite().dy).isZero();
        }
    }

    @Test
    void upDecreasesY() {
        assertThat(Direction.UP.dy).isNegative();
    }

    @Test
    void downIncreasesY() {
        assertThat(Direction.DOWN.dy).isPositive();
    }
}
