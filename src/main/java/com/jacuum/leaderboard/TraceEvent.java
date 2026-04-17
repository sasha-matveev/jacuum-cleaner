package com.jacuum.leaderboard;

import com.jacuum.algo.Direction;

public record TraceEvent(int iteration, Direction direction, int x, int y, int score) {}
