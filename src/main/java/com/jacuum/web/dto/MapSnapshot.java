package com.jacuum.web.dto;

import java.util.List;

public record MapSnapshot(
    int width,
    int height,
    int startX,
    int startY,
    int totalFloor,
    List<TileSnapshot> tiles
) {
    public record TileSnapshot(
        int x, int y,
        boolean wallNorth, boolean wallSouth, boolean wallEast, boolean wallWest
    ) {}
}
