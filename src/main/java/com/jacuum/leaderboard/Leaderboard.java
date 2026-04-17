package com.jacuum.leaderboard;

import java.util.List;

public interface Leaderboard {
    List<?> entries() throws Exception;
}
