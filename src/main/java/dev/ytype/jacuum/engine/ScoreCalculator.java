package dev.ytype.jacuum.engine;

public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    public static int score(int cleanedTiles, int iterationsUsed) {
        if (cleanedTiles <= 0) {
            throw new IllegalArgumentException("cleanedTiles must be positive");
        }
        if (iterationsUsed < 0) {
            throw new IllegalArgumentException("iterationsUsed must be non-negative");
        }
        return cleanedTiles * 1000 - iterationsUsed;
    }
}
