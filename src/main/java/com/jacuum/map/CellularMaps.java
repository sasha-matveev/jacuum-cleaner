package com.jacuum.map;

import com.jacuum.algo.Direction;
import java.util.*;

public final class CellularMaps implements Maps {

    private final int smoothingPasses;
    private final double fillRatio;

    public CellularMaps() {
        this.smoothingPasses = 5;
        this.fillRatio = 0.45;
    }

    @Override
    public GameMap generate(String hash, SizePreset size) throws Exception {
        long seed = seedFrom(hash);
        Random rng = new Random(seed);
        int w = size.width(), h = size.height();

        boolean[][] floor = initialFloor(rng, w, h);
        for (int i = 0; i < this.smoothingPasses; i++)
            floor = smooth(floor, w, h);

        floor = keepLargestRegion(floor, w, h);

        int[] start = centroidOfFloor(floor, w, h);
        return new GeneratedMap(hash, size, floor, start[0], start[1]);
    }

    private long seedFrom(String hash) {
        long h = 0xcbf29ce484222325L;
        for (char c : hash.toCharArray())
            h = (h ^ c) * 0x100000001b3L;
        return h;
    }

    private boolean[][] initialFloor(Random rng, int w, int h) {
        boolean[][] f = new boolean[h][w];
        for (int y = 1; y < h - 1; y++)
            for (int x = 1; x < w - 1; x++)
                f[y][x] = rng.nextDouble() > this.fillRatio;
        return f;
    }

    private boolean[][] smooth(boolean[][] f, int w, int h) {
        boolean[][] next = new boolean[h][w];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int walls = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                        if (!f[y + dy][x + dx]) walls++;
                next[y][x] = walls < 5;
            }
        }
        return next;
    }

    private boolean[][] keepLargestRegion(boolean[][] floor, int w, int h) {
        boolean[][] visited = new boolean[h][w];
        List<List<int[]>> regions = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (floor[y][x] && !visited[y][x]) {
                    List<int[]> region = new ArrayList<>();
                    Queue<int[]> q = new ArrayDeque<>();
                    q.add(new int[]{x, y});
                    visited[y][x] = true;
                    while (!q.isEmpty()) {
                        int[] cur = q.poll();
                        region.add(cur);
                        for (Direction d : Direction.values()) {
                            int nx = cur[0] + d.dx(), ny = cur[1] + d.dy();
                            if (nx >= 0 && ny >= 0 && nx < w && ny < h
                                && floor[ny][nx] && !visited[ny][nx]) {
                                visited[ny][nx] = true;
                                q.add(new int[]{nx, ny});
                            }
                        }
                    }
                    regions.add(region);
                }
            }
        }
        if (regions.isEmpty()) {
            // Fallback: open 3x3 centre
            boolean[][] fallback = new boolean[h][w];
            int cx = w / 2, cy = h / 2;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++)
                    fallback[cy + dy][cx + dx] = true;
            return fallback;
        }
        List<int[]> largest = regions.stream()
            .max(Comparator.comparingInt(List::size)).orElseThrow();
        boolean[][] result = new boolean[h][w];
        for (int[] cell : largest)
            result[cell[1]][cell[0]] = true;
        return result;
    }

    private int[] centroidOfFloor(boolean[][] floor, int w, int h) {
        long sx = 0, sy = 0, count = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (floor[y][x]) { sx += x; sy += y; count++; }
        if (count == 0) return new int[]{w / 2, h / 2};
        int cx = (int) (sx / count), cy = (int) (sy / count);
        // Snap to nearest floor tile
        int best = Integer.MAX_VALUE;
        int[] result = new int[]{cx, cy};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (floor[y][x]) {
                    int dist = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                    if (dist < best) { best = dist; result = new int[]{x, y}; }
                }
            }
        }
        return result;
    }
}
