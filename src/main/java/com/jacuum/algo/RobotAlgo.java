package com.jacuum.algo;

import com.jacuum.map.Direction;
import com.jacuum.map.Tile;

/**
 * Interface every vacuum robot algorithm must implement.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>The method is called once per game iteration.</li>
 *   <li>Must return a non-null {@link Direction}.</li>
 *   <li>Returning a direction that leads into a wall is allowed — the robot will
 *       stay in place and the iteration is consumed.</li>
 *   <li><strong>Any exception thrown terminates the run immediately</strong> with
 *       {@code status=FAILED} and {@code score=0}, regardless of progress made.</li>
 * </ul>
 *
 * <h2>Registration</h2>
 * Annotate your implementation with {@link VacuumAlgo @VacuumAlgo("Display Name")}
 * and place it in the {@code com.jacuum.algo.impl} package (or any package scanned
 * by Spring). It will appear automatically in the UI algorithm selector.
 *
 * <h2>State</h2>
 * Algorithm beans are <em>prototype-scoped</em>: a fresh instance is created per
 * game session. You may safely store state in instance fields.
 */
public interface RobotAlgo {

    /**
     * Decide the next move.
     *
     * @param currentTile snapshot of the tile the robot currently occupies,
     *                    including wall information and clean status
     * @return the direction to move; never {@code null}
     */
    Direction next(Tile currentTile);
}
