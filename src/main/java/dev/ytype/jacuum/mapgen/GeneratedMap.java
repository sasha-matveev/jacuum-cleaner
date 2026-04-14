package dev.ytype.jacuum.mapgen;

import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.SizePreset;
import java.util.Objects;

public record GeneratedMap(String hash, SizePreset preset, RoomMap map) {

    public GeneratedMap {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(preset, "preset");
        Objects.requireNonNull(map, "map");
    }
}
