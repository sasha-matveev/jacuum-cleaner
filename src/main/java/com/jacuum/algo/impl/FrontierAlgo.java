package com.jacuum.algo.impl;

import com.jacuum.algo.*;
import java.util.*;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * Frontier-BFS exploration: at each decision point, navigates to the nearest
 * unvisited reachable tile using the shortest known path, then returns for the
 * start tile if it was not cleaned on departure.
 *
 * <p>Invariants maintained per call to {@link #next(Tile)}:
 * <ul>
 *   <li><b>walls</b> – wall bitmask for every tile the robot has stood on.
 *       Built from {@code tile.hasWall(d)} calls, so it is always accurate.</li>
 *   <li><b>clean</b> – tiles confirmed cleaned by the engine.
 *       A tile enters this set the first time {@code tile.isClean()} returns
 *       {@code true} while the robot is standing on it.</li>
 *   <li><b>plan</b> – a pre-computed queue of directions to execute.
 *       Computed by BFS when empty; followed blindly otherwise.</li>
 * </ul>
 *
 * <p>Why a start-tile fixup is needed: the engine adds a tile to its
 * {@code cleaned} set only after the robot <em>moves</em> to it.  The very
 * first call to {@link #next(Tile)} is at the start position, where
 * {@code tile.isClean()} is still {@code false}.  After the robot departs,
 * the start tile is never re-targeted by the frontier BFS (it is already
 * {@code known}).  So once all unknown frontiers are exhausted, this
 * implementation navigates back to the start tile if it is not yet clean.
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Frontier BFS")
public final class FrontierAlgo implements RobotAlgo {

    private final Map<String, EnumSet<Direction>> walls;
    private final Set<String> clean;
    private final Deque<Direction> plan;
    private String startKey;

    public FrontierAlgo() {
        this.walls    = new HashMap<>();
        this.clean    = new HashSet<>();
        this.plan     = new ArrayDeque<>();
        this.startKey = null;
    }

    @Override
    public Direction next(final Tile tile) throws Exception {
        observe(tile);

        if (!this.plan.isEmpty()) {
            return this.plan.poll();
        }

        final int    cx  = tile.x();
        final int    cy  = tile.y();
        final String cur = key(cx, cy);

        // Phase 1 — navigate to nearest unknown tile.
        final List<Direction> toFrontier = bfsTo(cx, cy, null);
        if (!toFrontier.isEmpty()) {
            enqueue(toFrontier);
            return this.plan.poll();
        }

        // Phase 2 — all tiles known; return to start tile if it was never cleaned.
        if (this.startKey != null && !this.clean.contains(this.startKey) && !cur.equals(this.startKey)) {
            final List<Direction> toStart = bfsTo(cx, cy, this.startKey);
            if (!toStart.isEmpty()) {
                enqueue(toStart);
                return this.plan.poll();
            }
        }

        // Fully explored (or robot is stuck inside walls on all sides).
        return anyPassable(tile);
    }

    // -------------------------------------------------------------------------
    // Observation
    // -------------------------------------------------------------------------

    private void observe(final Tile tile) {
        final String k = key(tile.x(), tile.y());
        if (this.startKey == null) {
            this.startKey = k;
        }
        if (!this.walls.containsKey(k)) {
            final EnumSet<Direction> blocked = EnumSet.noneOf(Direction.class);
            for (final Direction d : Direction.values()) {
                if (tile.hasWall(d)) blocked.add(d);
            }
            this.walls.put(k, blocked);
        }
        if (tile.isClean()) {
            this.clean.add(k);
        }
    }

    // -------------------------------------------------------------------------
    // BFS
    // -------------------------------------------------------------------------

    /**
     * BFS over tiles whose walls are known (i.e. the robot has stood on them).
     *
     * <p>When {@code targetKey} is {@code null}: finds the nearest tile
     * adjacent to a known tile that has no wall toward it and is itself
     * <em>unknown</em> (not yet in {@code walls}).
     *
     * <p>When {@code targetKey} is non-null: finds the shortest path to that
     * specific known tile.
     *
     * @return ordered list of directions, or empty list when unreachable.
     */
    private List<Direction> bfsTo(final int startX, final int startY,
                                   final String targetKey) {
        final Map<String, String>    parent = new HashMap<>();
        final Map<String, Direction> stepTo = new HashMap<>();
        final Deque<int[]>           queue  = new ArrayDeque<>();
        final String                 sk     = key(startX, startY);

        parent.put(sk, null);
        queue.add(new int[]{startX, startY});

        while (!queue.isEmpty()) {
            final int[]  pos     = queue.poll();
            final String posKey  = key(pos[0], pos[1]);
            final EnumSet<Direction> blocked =
                this.walls.getOrDefault(posKey, EnumSet.noneOf(Direction.class));

            for (final Direction d : Direction.values()) {
                if (blocked.contains(d)) continue;

                final int    nx = pos[0] + d.dx();
                final int    ny = pos[1] + d.dy();
                final String nk = key(nx, ny);

                if (parent.containsKey(nk)) continue;
                parent.put(nk, posKey);
                stepTo.put(nk, d);

                if (targetKey == null) {
                    // Frontier mode: stop at first unknown neighbour.
                    if (!this.walls.containsKey(nk)) {
                        return reconstruct(nk, parent, stepTo);
                    }
                    // Known tile — keep expanding.
                    queue.add(new int[]{nx, ny});
                } else {
                    // Target mode: stop when we reach the specific tile.
                    if (nk.equals(targetKey)) {
                        return reconstruct(nk, parent, stepTo);
                    }
                    if (this.walls.containsKey(nk)) {
                        queue.add(new int[]{nx, ny});
                    }
                    // Unknown tiles are not traversable in target mode
                    // (we have no wall data for them).
                }
            }
        }
        return List.of();
    }

    private List<Direction> reconstruct(final String target,
                                         final Map<String, String>    parent,
                                         final Map<String, Direction> stepTo) {
        final Deque<Direction> path = new ArrayDeque<>();
        String cur = target;
        while (stepTo.containsKey(cur)) {
            path.addFirst(stepTo.get(cur));
            cur = parent.get(cur);
        }
        return new ArrayList<>(path);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void enqueue(final List<Direction> path) {
        for (final Direction d : path) this.plan.add(d);
    }

    private Direction anyPassable(final Tile tile) {
        for (final Direction d : Direction.values()) {
            if (!tile.hasWall(d)) return d;
        }
        return Direction.NORTH;
    }

    private String key(final int x, final int y) {
        return x + "," + y;
    }
}
