package com.jacuum.map;

import com.jacuum.algo.Direction;

public interface GameMap {
    String hash();
    SizePreset size();
    int width();
    int height();
    boolean isFloor(int x, int y);
    boolean hasWall(int x, int y, Direction direction);
    int startX();
    int startY();
    int totalFloorTiles();
}
