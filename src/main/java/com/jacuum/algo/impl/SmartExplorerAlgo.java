package com.jacuum.algo.impl;

import com.jacuum.algo.RobotAlgo;
import com.jacuum.algo.VacuumAlgo;
import com.jacuum.map.Direction;
import com.jacuum.map.Tile;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import java.util.*;

/**
 * Best-effort complete-coverage algorithm using online BFS map exploration.
 *
 * <h2>Strategy</h2>
 * The robot maintains an internal model of the discovered map and at every
 * decision point navigates to the <em>nearest unvisited floor tile</em> via a
 * BFS-planned shortest path.  As a result:
 * <ul>
 *   <li>It <strong>never bumps a wall</strong> — all paths are planned
 *       exclusively through tiles whose walls are fully known.</li>
 *   <li>It achieves <strong>complete coverage</strong> of every reachable floor
 *       tile regardless of map shape.</li>
 *   <li>Backtracking overhead is minimised because we always pick the
 *       <em>nearest</em> remaining target (greedy BFS).</li>
 * </ul>
 *
 * <h2>Internal map model</h2>
 * <pre>
 *   knownFloor  — all tiles confirmed to be floor (visited OR seen as a
 *                 passable neighbour of a visited tile — "frontier")
 *   visited     — tiles the robot has physically stood on; their full
 *                 4-direction wall configuration is known
 *   wallsAt     — blocked directions recorded for each visited tile
 * </pre>
 *
 * <h2>Per-step logic</h2>
 * <ol>
 *   <li>Record wall/floor data from the current tile.</li>
 *   <li>If a pre-planned path is queued, execute the next step.</li>
 *   <li>Otherwise BFS-search for the nearest frontier tile, plan the
 *       shortest path to it through visited tiles, queue remaining steps,
 *       and execute the first step.</li>
 *   <li>If no frontier remains, all reachable tiles are clean — return any
 *       valid move (the game will end next iteration).</li>
 * </ol>
 *
 * <p><strong>Requires prototype scope</strong> — this algo is stateful.
 * Works correctly once BUG-01 (AlgoRegistry singleton issue) is fixed.
 */
@VacuumAlgo("Smart Explorer")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SmartExplorerAlgo implements RobotAlgo {

    /** Compact immutable 2-D coordinate used as map key. */
    private record Pt(int x, int y) {}

    /** Wall directions recorded for every visited tile. */
    private final Map<Pt, EnumSet<Direction>> wallsAt = new HashMap<>();

    /**
     * All tiles confirmed to be floor:
     * visited tiles AND tiles seen as passable from a neighbour (frontier).
     */
    private final Set<Pt> knownFloor = new HashSet<>();

    /** Tiles the robot has stood on; all four walls are known here. */
    private final Set<Pt> visited = new HashSet<>();

    /** Pre-planned sequence of moves toward the current navigation target. */
    private final Deque<Direction> moveQueue = new ArrayDeque<>();

    private int posX, posY;
    private boolean initialized = false;

    // =========================================================================
    //  Public API
    // =========================================================================

    @Override
    public Direction next(Tile tile) {
        int tx = tile.getX(), ty = tile.getY();

        // Sync internal position with the authoritative tile position.
        // Also handles the very first call and detects a fresh game (BUG-01
        // workaround for singleton scope: if the tile is not in our known
        // map, the previous session's data is stale — reset everything).
        if (!initialized || posX != tx || posY != ty) {
            if (!knownFloor.contains(new Pt(tx, ty))) {
                // Unknown position → must be a new game; discard old state
                wallsAt.clear();
                knownFloor.clear();
                visited.clear();
            }
            posX = tx;
            posY = ty;
            moveQueue.clear();
            initialized = true;
        }

        // Learn from the current tile before making any decision
        observe(tile);

        // Step 1: execute the next pre-planned move if the queue is non-empty
        if (!moveQueue.isEmpty()) {
            return advance();
        }

        // Step 2: plan and execute a move toward the nearest frontier
        return planAndAdvance(tile);
    }

    // =========================================================================
    //  Observation — update internal map from the current tile
    // =========================================================================

    private void observe(Tile tile) {
        Pt pos = pt(posX, posY);
        visited.add(pos);
        knownFloor.add(pos);

        EnumSet<Direction> blocked = EnumSet.noneOf(Direction.class);
        for (Direction d : Direction.values()) {
            if (tile.hasWall(d)) {
                blocked.add(d);
            } else {
                // Non-wall neighbour is a floor tile; add to frontier if new
                knownFloor.add(pt(posX + d.dx, posY + d.dy));
            }
        }
        wallsAt.put(pos, blocked);
    }

    // =========================================================================
    //  Move execution
    // =========================================================================

    /** Pop the next move from the queue and update predicted position. */
    private Direction advance() {
        Direction dir = moveQueue.poll();
        posX += dir.dx;
        posY += dir.dy;
        return dir;
    }

    // =========================================================================
    //  Planning — find nearest frontier and navigate there
    // =========================================================================

    private Direction planAndAdvance(Tile tile) {
        // Loop skips unreachable frontier tiles (in practice should never occur
        // because every frontier tile is adjacent to a visited tile, but kept
        // for defensive correctness).
        while (true) {
            Pt target = nearestFrontier();

            if (target == null) {
                // All reachable floor tiles have been visited — we are done.
                // Return any valid move; the game engine will mark FINISHED after
                // detecting countCleanedTiles == countFloorTiles.
                for (Direction d : Direction.values()) {
                    if (!tile.hasWall(d)) return d;
                }
                return Direction.UP; // fully enclosed (degenerate map)
            }

            List<Direction> path = bfsPath(pt(posX, posY), target);
            if (path != null && !path.isEmpty()) {
                // Queue all steps; advance() pops and executes the first one
                moveQueue.addAll(path);
                return advance();
            }

            // Cannot reach this frontier — exclude it and try the next nearest
            knownFloor.remove(target);
            visited.add(target); // prevents nearestFrontier() from returning it again
        }
    }

    // =========================================================================
    //  BFS 1 — find nearest unvisited (frontier) tile
    //
    //  Expands through visited tiles only (walls fully known).
    //  Returns immediately when a frontier tile is reached.
    // =========================================================================

    private Pt nearestFrontier() {
        Pt start = pt(posX, posY);
        Queue<Pt> queue = new ArrayDeque<>();
        Set<Pt> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty()) {
            Pt cur = queue.poll();

            // Frontier condition: known floor tile that hasn't been visited yet
            if (!visited.contains(cur)) return cur;

            // Expand through visited tiles using their recorded wall info
            EnumSet<Direction> blocked = wallsAt.getOrDefault(cur, EnumSet.noneOf(Direction.class));
            for (Direction d : Direction.values()) {
                if (blocked.contains(d)) continue;
                Pt next = pt(cur.x() + d.dx, cur.y() + d.dy);
                if (!seen.contains(next) && knownFloor.contains(next)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return null; // no reachable frontier
    }

    // =========================================================================
    //  BFS 2 — shortest path from `from` to `to`
    //
    //  Traverses visited tiles (known walls); frontier tiles are valid
    //  destinations but not intermediate nodes (walls unknown there).
    // =========================================================================

    private List<Direction> bfsPath(Pt from, Pt to) {
        if (from.equals(to)) return List.of();

        Queue<Pt> queue = new ArrayDeque<>();
        Map<Pt, Direction> cameVia = new HashMap<>(); // tile → direction taken to reach it
        Set<Pt> seen = new HashSet<>();

        queue.add(from);
        seen.add(from);

        while (!queue.isEmpty()) {
            Pt cur = queue.poll();

            if (cur.equals(to)) return buildPath(cameVia, from, to);

            // Only expand from visited tiles — walls are known here
            if (!visited.contains(cur)) continue;

            EnumSet<Direction> blocked = wallsAt.getOrDefault(cur, EnumSet.noneOf(Direction.class));
            for (Direction d : Direction.values()) {
                if (blocked.contains(d)) continue;
                Pt next = pt(cur.x() + d.dx, cur.y() + d.dy);
                if (!seen.contains(next) && knownFloor.contains(next)) {
                    seen.add(next);
                    cameVia.put(next, d);
                    queue.add(next);
                }
            }
        }
        return null; // no path found
    }

    // =========================================================================
    //  Path reconstruction — walk backwards through cameVia map
    // =========================================================================

    private List<Direction> buildPath(Map<Pt, Direction> cameVia, Pt from, Pt to) {
        LinkedList<Direction> path = new LinkedList<>();
        Pt cur = to;
        while (!cur.equals(from)) {
            Direction d = cameVia.get(cur);
            path.addFirst(d);
            // Walk backwards: if we arrived at `cur` by going direction `d`,
            // the previous tile is cur offset by the opposite delta
            cur = pt(cur.x() - d.dx, cur.y() - d.dy);
        }
        return path;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private static Pt pt(int x, int y) {
        return new Pt(x, y);
    }
}
