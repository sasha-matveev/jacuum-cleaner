package com.jacuum.map;

public enum SizePreset {
    TINY(10, 8),
    SMALL(16, 12),
    MEDIUM(24, 18),
    LARGE(34, 26);

    private final int width;
    private final int height;

    SizePreset(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width()  { return width; }
    public int height() { return height; }
}
