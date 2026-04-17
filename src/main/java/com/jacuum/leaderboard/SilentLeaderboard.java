package com.jacuum.leaderboard;

import java.util.List;

public final class SilentLeaderboard implements Leaderboard {
    @Override
    public List<?> entries() {
        return List.of();
    }
}
