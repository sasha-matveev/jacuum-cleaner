package dev.ytype.jacuum.domain;

import java.util.Objects;

public record Coordinate(int x, int y) {

    public Coordinate move(Direction direction) {
        Objects.requireNonNull(direction, "direction");
        return new Coordinate(x + direction.dx(), y + direction.dy());
    }
}