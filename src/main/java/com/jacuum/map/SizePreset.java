package com.jacuum.map;

/**
 * Named size presets controlling approximate floor tile count.
 */
public enum SizePreset {
    TINY(8, 8),
    SMALL(12, 12),
    MEDIUM(18, 18),
    LARGE(26, 26);

    /** Approximate grid dimensions for this preset. */
    public final int gridWidth;
    public final int gridHeight;

    SizePreset(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    public static SizePreset fromString(String s) {
        if (s == null) return MEDIUM;
        return switch (s.toLowerCase()) {
            case "tiny"   -> TINY;
            case "small"  -> SMALL;
            case "large"  -> LARGE;
            default       -> MEDIUM;
        };
    }
}
