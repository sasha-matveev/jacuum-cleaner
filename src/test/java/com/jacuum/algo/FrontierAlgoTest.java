package com.jacuum.algo;

import com.jacuum.algo.impl.FrontierAlgo;
import com.jacuum.map.*;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class FrontierAlgoTest {

    // -----------------------------------------------------------------------
    // Map builders
    // -----------------------------------------------------------------------

    /** Open 5×5 room with a one-tile border (25 floor tiles). */
    private static GameMap openRoom() {
        boolean[][] f = new boolean[7][7];
        for (int y = 1; y <= 5; y++)
            for (int x = 1; x <= 5; x++)
                f[y][x] = true;
        return new GeneratedMap("open5x5", SizePreset.TINY, f, 3, 3);
    }

    /** Seven-tile horizontal corridor: row 1, columns 1–7. */
    private static GameMap corridor() {
        boolean[][] f = new boolean[3][9];
        for (int x = 1; x <= 7; x++) f[1][x] = true;
        return new GeneratedMap("corr7", SizePreset.TINY, f, 4, 1);
    }

    /** L-shaped map — tests non-convex coverage. */
    private static GameMap lShape() {
        boolean[][] f = new boolean[5][5];
        // vertical part: col 1, rows 1–3
        for (int y = 1; y <= 3; y++) f[y][1] = true;
        // horizontal part: row 3, cols 1–3
        for (int x = 1; x <= 3; x++) f[3][x] = true;
        return new GeneratedMap("lshape", SizePreset.TINY, f, 1, 1);
    }

    // -----------------------------------------------------------------------
    // Test harness
    // -----------------------------------------------------------------------

    /**
     * Simulates the engine's run loop for up to {@code maxIter} iterations.
     * Returns the number of distinct tiles cleaned.
     * Fails immediately if the algorithm returns {@code null} or throws.
     */
    private int simulate(final GameMap map, final int maxIter) throws Exception {
        final RobotAlgo algo = new FrontierAlgo();
        int x = map.startX(), y = map.startY();
        final Set<String> cleaned = new HashSet<>();

        for (int i = 0; i < maxIter; i++) {
            final int fx = x, fy = y;
            final Set<String> snap = Set.copyOf(cleaned);
            final Tile tile = new Tile() {
                @Override public int x()                        { return fx; }
                @Override public int y()                        { return fy; }
                @Override public boolean isClean()              { return snap.contains(fx + "," + fy); }
                @Override public boolean hasWall(Direction dir) { return map.hasWall(fx, fy, dir); }
            };

            final Direction dir = algo.next(tile);
            assertThat(dir).as("algo returned null on iteration %d", i).isNotNull();

            if (!map.hasWall(x, y, dir)) {
                x += dir.dx();
                y += dir.dy();
            }
            cleaned.add(x + "," + y);

            if (cleaned.size() == map.totalFloorTiles()) break;
        }
        return cleaned.size();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void coversEveryTileInOpenRoom() throws Exception {
        final GameMap map = openRoom();
        // 25 floor tiles; generous budget to allow for backtracking
        assertThat(simulate(map, 200)).isEqualTo(map.totalFloorTiles());
    }

    @Test
    void coversEveryTileInCorridor() throws Exception {
        final GameMap map = corridor();
        // 7 tiles; 20 iterations is more than enough
        assertThat(simulate(map, 20)).isEqualTo(map.totalFloorTiles());
    }

    @Test
    void coversEveryTileInLShape() throws Exception {
        final GameMap map = lShape();
        assertThat(simulate(map, 30)).isEqualTo(map.totalFloorTiles());
    }

    @Test
    void neverReturnsNullDirection() throws Exception {
        // Smoke: algo must not return null on any of the first 50 iterations
        simulate(openRoom(), 50);
    }

    @Test
    void doesNotWasteIterationsBumpingWalls() throws Exception {
        final GameMap map = corridor();
        // 7 tiles in a corridor from the centre (col 4).
        // Optimal path visits all 7 tiles in at most 12 iterations (6 unique moves + start return).
        // Allow a 2× slack for any backtracking overhead.
        assertThat(simulate(map, 15)).isEqualTo(map.totalFloorTiles());
    }
}
