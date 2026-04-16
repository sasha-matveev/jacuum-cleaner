package com.jacuum.map;

/**
 * Generates a {@link GameMap} from a hash string and a size preset.
 *
 * <p>Identical {@code hash} + {@code size} inputs must always produce identical maps.
 */
public interface MapGenerator {

    /**
     * Generate a map.
     *
     * @param hash       seed string — same hash produces the same map
     * @param sizePreset controls approximate tile count
     * @return a fully initialized {@link GameMap} with all floor tiles reachable
     */
    GameMap generate(String hash, SizePreset sizePreset);
}
