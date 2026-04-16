package com.jacuum.game;

public enum GameStatus {
    RUNNING,
    PAUSED,
    FINISHED,   // All floor tiles cleaned or iterations exhausted (normally)
    FAILED,     // Algo threw an exception
    ABORTED     // User interrupted
}
