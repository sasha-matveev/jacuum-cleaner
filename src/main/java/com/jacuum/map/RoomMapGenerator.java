package com.jacuum.map;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generates room-like maps with the following properties:
 * <ul>
 *   <li>Deterministic: same hash → same map.</li>
 *   <li>All floor tiles are reachable from the start position (flood-fill guaranteed).</li>
 *   <li>Outer border is always walls.</li>
 *   <li>Interior may contain wall obstacles (clusters).</li>
 *   <li>Shape is vaguely room-like: roughly rectangular with possible corner cuts and alcoves.</li>
 * </ul>
 */
@Component
public class RoomMapGenerator implements MapGenerator {

    @Override
    public GameMap generate(String hash, SizePreset sizePreset) {
        long seed = hashToSeed(hash);
        Random rng = new Random(seed);

        int w = sizePreset.gridWidth;
        int h = sizePreset.gridHeight;

        boolean[][] walls = new boolean[h][w];

        // Step 1: fill everything as wall, then carve out the interior
        fillAll(walls, h, w, true);

        // Step 2: carve main rectangular room (1 tile inset from border)
        carveRect(walls, 1, 1, w - 2, h - 2);

        // Step 3: cut random corners to make shape less perfectly square
        cutCorners(walls, w, h, rng);

        // Step 4: add alcoves (recesses that extend the room into corners area)
        addAlcoves(walls, w, h, rng);

        // Step 5: place obstacle clusters inside the room
        placeObstacles(walls, w, h, rng, sizePreset);

        // Step 6: pick a start position (near center, must be floor)
        int[] start = findStartPosition(walls, w, h, rng);

        // Step 7: flood-fill from start; any unreachable floor tile → wall
        enforceReachability(walls, w, h, start[0], start[1]);

        return new GameMap(w, h, walls, start[0], start[1], hash, sizePreset);
    }

    // --- private helpers ---

    private long hashToSeed(String hash) {
        // Mix hash characters into a long seed for good distribution
        long seed = 0xcafebabe_deadbeefL;
        for (char c : hash.toCharArray()) {
            seed = seed * 31 + c;
        }
        return seed;
    }

    private void fillAll(boolean[][] walls, int h, int w, boolean value) {
        for (int y = 0; y < h; y++) Arrays.fill(walls[y], value);
    }

    private void carveRect(boolean[][] walls, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++)
            for (int x = x1; x < x2; x++)
                walls[y][x] = false;
    }

    private void cutCorners(boolean[][] walls, int w, int h, Random rng) {
        // Each corner has a 60% chance to be partially cut
        int maxCut = Math.max(1, Math.min(w, h) / 4);
        int[][] corners = {{0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}};
        for (int[] corner : corners) {
            if (rng.nextDouble() < 0.6) {
                int cx = corner[0];
                int cy = corner[1];
                int cut = 1 + rng.nextInt(maxCut);
                for (int dy = 0; dy < cut; dy++) {
                    for (int dx = 0; dx < cut - dy; dx++) {
                        int nx = (cx == 0) ? 1 + dx : w - 2 - dx;
                        int ny = (cy == 0) ? 1 + dy : h - 2 - dy;
                        if (nx > 0 && nx < w - 1 && ny > 0 && ny < h - 1) {
                            walls[ny][nx] = true;
                        }
                    }
                }
            }
        }
    }

    private void addAlcoves(boolean[][] walls, int w, int h, Random rng) {
        // Add 1-2 small alcoves (extend a side inward)
        int alcoves = 1 + rng.nextInt(2);
        for (int i = 0; i < alcoves; i++) {
            int side = rng.nextInt(4); // 0=top,1=bottom,2=left,3=right
            int depth = 1 + rng.nextInt(Math.max(1, Math.min(w, h) / 5));
            int len = 2 + rng.nextInt(Math.max(2, Math.min(w, h) / 3));
            if (side == 0) { // top alcove
                int sx = 2 + rng.nextInt(Math.max(1, w - 4 - len));
                for (int dy = 1; dy <= depth; dy++)
                    for (int dx = 0; dx < len && sx + dx < w - 1; dx++)
                        walls[dy][sx + dx] = true;
            } else if (side == 1) { // bottom alcove
                int sx = 2 + rng.nextInt(Math.max(1, w - 4 - len));
                for (int dy = 1; dy <= depth; dy++)
                    for (int dx = 0; dx < len && sx + dx < w - 1; dx++)
                        walls[h - 1 - dy][sx + dx] = true;
            } else if (side == 2) { // left alcove
                int sy = 2 + rng.nextInt(Math.max(1, h - 4 - len));
                for (int dx = 1; dx <= depth; dx++)
                    for (int dy = 0; dy < len && sy + dy < h - 1; dy++)
                        walls[sy + dy][dx] = true;
            } else { // right alcove
                int sy = 2 + rng.nextInt(Math.max(1, h - 4 - len));
                for (int dx = 1; dx <= depth; dx++)
                    for (int dy = 0; dy < len && sy + dy < h - 1; dy++)
                        walls[sy + dy][w - 1 - dx] = true;
            }
        }
    }

    private void placeObstacles(boolean[][] walls, int w, int h, Random rng, SizePreset size) {
        int count = switch (size) {
            case TINY   -> rng.nextInt(2);
            case SMALL  -> 1 + rng.nextInt(2);
            case MEDIUM -> 2 + rng.nextInt(3);
            case LARGE  -> 3 + rng.nextInt(4);
        };
        for (int i = 0; i < count; i++) {
            // Pick a random interior position and place a small wall cluster
            int ox = 2 + rng.nextInt(w - 4);
            int oy = 2 + rng.nextInt(h - 4);
            int clusterSize = 1 + rng.nextInt(2);
            for (int dy = 0; dy < clusterSize; dy++)
                for (int dx = 0; dx < clusterSize; dx++) {
                    int nx = ox + dx;
                    int ny = oy + dy;
                    if (nx > 0 && nx < w - 1 && ny > 0 && ny < h - 1)
                        walls[ny][nx] = true;
                }
        }
    }

    private int[] findStartPosition(boolean[][] walls, int w, int h, Random rng) {
        // Try center first, then spiral outward, then random floor tile
        int cx = w / 2, cy = h / 2;
        if (!walls[cy][cx]) return new int[]{cx, cy};

        for (int r = 1; r < Math.max(w, h); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) == r || Math.abs(dy) == r) {
                        int nx = cx + dx, ny = cy + dy;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !walls[ny][nx])
                            return new int[]{nx, ny};
                    }
                }
            }
        }
        // Fallback: first floor tile
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (!walls[y][x]) return new int[]{x, y};

        throw new IllegalStateException("No floor tiles found in generated map");
    }

    private void enforceReachability(boolean[][] walls, int w, int h, int startX, int startY) {
        boolean[][] reachable = new boolean[h][w];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        reachable[startY][startX] = true;

        int[][] deltas = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : deltas) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !walls[ny][nx] && !reachable[ny][nx]) {
                    reachable[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        // Any floor tile not reachable becomes a wall
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (!walls[y][x] && !reachable[y][x])
                    walls[y][x] = true;
    }
}
