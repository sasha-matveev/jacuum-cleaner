package com.jacuum.algo;

import java.util.List;

/**
 * Registry of available robot algorithms.
 * Implementations provide discovery and instantiation of {@link RobotAlgo} beans.
 */
public interface Algorithms {
    /**
     * Returns the display names of all available algorithms, in sorted order.
     */
    List<String> names();

    /**
     * Instantiates a fresh algorithm bean by display name.
     *
     * @param name algorithm display name (from {@link #names()})
     * @return a new algorithm instance
     * @throws Exception if the name is not found or instantiation fails
     */
    RobotAlgo instantiate(String name) throws Exception;
}
