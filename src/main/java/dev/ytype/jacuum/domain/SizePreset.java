package dev.ytype.jacuum.domain;

public enum SizePreset {
    TINY(10, 8, 120, 24),
    SMALL(14, 11, 220, 44),
    MEDIUM(20, 15, 360, 90),
    LARGE(28, 21, 560, 168);

    private final int width;
    private final int height;
    private final int iterationDefault;
    private final int approximateFloorTiles;

    SizePreset(int width, int height, int iterationDefault, int approximateFloorTiles) {
        this.width = width;
        this.height = height;
        this.iterationDefault = iterationDefault;
        this.approximateFloorTiles = approximateFloorTiles;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int iterationDefault() {
        return iterationDefault;
    }

    public int approximateFloorTiles() {
        return approximateFloorTiles;
    }
}