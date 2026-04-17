package com.jacuum.engine;

import com.jacuum.algo.Direction;
import com.jacuum.algo.Tile;
import java.util.Set;

final class SessionTile implements Tile {
    private final int x;
    private final int y;
    private final com.jacuum.map.GameMap map;
    private final Set<String> cleaned;

    SessionTile(int x, int y, com.jacuum.map.GameMap map, Set<String> cleaned) {
        this.x       = x;
        this.y       = y;
        this.map     = map;
        this.cleaned = cleaned;
    }

    @Override public int x()                          { return x; }
    @Override public int y()                          { return y; }
    @Override public boolean isClean()                { return cleaned.contains(x + "," + y); }
    @Override public boolean hasWall(Direction dir)   { return map.hasWall(x, y, dir); }
}
