package com.jacuum.map;

import com.jacuum.algo.Direction;

public final class GeneratedMap implements GameMap {

    private final String hash;
    private final SizePreset size;
    private final boolean[][] floor; // [y][x]
    private final int startX;
    private final int startY;
    private final int totalFloor;

    public GeneratedMap(String hash, SizePreset size, boolean[][] floor, int startX, int startY) {
        this.hash   = hash;
        this.size   = size;
        this.floor  = floor;
        this.startX = startX;
        this.startY = startY;
        int count = 0;
        for (boolean[] row : floor)
            for (boolean cell : row)
                if (cell) count++;
        this.totalFloor = count;
    }

    @Override public String hash()         { return hash; }
    @Override public SizePreset size()     { return size; }
    @Override public int width()           { return floor[0].length; }
    @Override public int height()          { return floor.length; }
    @Override public boolean isFloor(int x, int y) { return inBounds(x, y) && floor[y][x]; }
    @Override public int startX()          { return startX; }
    @Override public int startY()          { return startY; }
    @Override public int totalFloorTiles() { return totalFloor; }

    @Override
    public boolean hasWall(int x, int y, Direction direction) {
        int nx = x + direction.dx();
        int ny = y + direction.dy();
        return !inBounds(nx, ny) || !floor[ny][nx];
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height();
    }
}
