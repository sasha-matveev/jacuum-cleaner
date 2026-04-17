package com.jacuum.leaderboard;

import java.nio.file.Path;
import java.util.List;

public final class JsonLeaderboard implements Leaderboard {
    private final Path path;

    public JsonLeaderboard(final Path path) {
        this.path = path;
    }

    @Override
    public List<?> entries() throws Exception {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
