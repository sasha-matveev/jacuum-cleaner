package com.jacuum.map;

import java.util.Arrays;

/**
 * Internal mutable representation of the game map.
 *
 * <p>The grid is stored as a 2D boolean array: {@code walls[y][x] = true} means the cell is a wall.
 * Floor cells start dirty; the game engine marks them clean as the robot visits them.
 */
public class GameMap {

    private final int width;
    private final int height;
    /** walls[y][x] = true → impassable wall */
    private final boolean[][] walls;
    /** cleaned[y][x] = true → robot has visited this floor cell */
    private final boolean[][] cleaned;
    private final int startX;
    private final int startY;
    private final String hash;
    private final SizePreset sizePreset;

    public GameMap(int width, int height, boolean[][] walls, int startX, int startY,
                   String hash, SizePreset sizePreset) {
        this.width = width;
        this.height = height;
        this.walls = walls;
        this.cleaned = new boolean[height][width];
        this.startX = startX;
        this.startY = startY;
        this.hash = hash;
        this.sizePreset = sizePreset;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public String getHash() { return hash; }
    public SizePreset getSizePreset() { return sizePreset; }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return true;
        return walls[y][x];
    }

    public boolean isCleaned(int x, int y) {
        return !isWall(x, y) && cleaned[y][x];
    }

    public void markCleaned(int x, int y) {
        if (!isWall(x, y)) cleaned[y][x] = true;
    }

    public int countFloorTiles() {
        int count = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (!walls[y][x]) count++;
        return count;
    }

    public int countCleanedTiles() {
        int count = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (cleaned[y][x]) count++;
        return count;
    }

    /** Returns a snapshot TileView for the given floor position. */
    public TileView tileViewAt(int x, int y) {
        return new TileView(
            x, y,
            isCleaned(x, y),
            isWall(x, y - 1),  // UP
            isWall(x, y + 1),  // DOWN
            isWall(x - 1, y),  // LEFT
            isWall(x + 1, y)   // RIGHT
        );
    }

    /** Returns a deep copy of the walls grid (for serialization / testing). */
    public boolean[][] getWallsCopy() {
        boolean[][] copy = new boolean[height][width];
        for (int y = 0; y < height; y++) copy[y] = Arrays.copyOf(walls[y], width);
        return copy;
    }
}
