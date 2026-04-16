package com.jacuum.algo;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST  -> WEST;
            case WEST  -> EAST;
        };
    }

    public int dx() {
        return switch (this) {
            case EAST  ->  1;
            case WEST  -> -1;
            default    ->  0;
        };
    }

    public int dy() {
        return switch (this) {
            case SOUTH ->  1;
            case NORTH -> -1;
            default    ->  0;
        };
    }
}
