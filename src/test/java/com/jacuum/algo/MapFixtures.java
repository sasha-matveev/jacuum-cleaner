package com.jacuum.algo;

import com.jacuum.map.GameMap;
import com.jacuum.map.SizePreset;

/**
 * Predefined maps for algo contract testing.
 *
 * Convention: '#' = wall, '.' = floor.
 * First '.' from top-left is used as the robot start position.
 */
public final class MapFixtures {

    private MapFixtures() {}

    /** 7x7 open square room. */
    public static GameMap square5x5() {
        return fromAscii("square5x5", new String[]{
            "#######",
            "#.....#",
            "#.....#",
            "#.....#",
            "#.....#",
            "#.....#",
            "#######"
        });
    }

    /** Horizontal corridor 1 tile high, 10 tiles wide. */
    public static GameMap corridor1x10() {
        return fromAscii("corridor1x10", new String[]{
            "############",
            "#..........#",
            "############"
        });
    }

    /** L-shaped room. */
    public static GameMap lShape() {
        return fromAscii("lShape", new String[]{
            "#########",
            "#....####",
            "#....####",
            "#....####",
            "#.......#",
            "#.......#",
            "#########"
        });
    }

    /** Open room with one inner obstacle tile. */
    public static GameMap roomWithPillar() {
        return fromAscii("roomWithPillar", new String[]{
            "#######",
            "#.....#",
            "#.....#",
            "#..#..#",
            "#.....#",
            "#.....#",
            "#######"
        });
    }

    // --- builder ---

    static GameMap fromAscii(String id, String[] rows) {
        int h = rows.length;
        int w = rows[0].length();
        // Normalize row widths
        boolean[][] walls = new boolean[h][w];
        int startX = -1, startY = -1;
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < w && x < row.length(); x++) {
                walls[y][x] = row.charAt(x) == '#';
                if (!walls[y][x] && startX == -1) {
                    startX = x;
                    startY = y;
                }
            }
            // fill remainder as walls if row is shorter
            for (int x = row.length(); x < w; x++) {
                walls[y][x] = true;
            }
        }
        if (startX == -1) throw new IllegalArgumentException("No floor tile in fixture: " + id);
        return new GameMap(w, h, walls, startX, startY, id, SizePreset.SMALL);
    }
}
