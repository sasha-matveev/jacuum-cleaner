package dev.ytype.jacuum.mapgen;

import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.SizePreset;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MapGenerator {

    public GeneratedMap generate(String hash, SizePreset preset) {
        Objects.requireNonNull(preset, "preset");

        String effectiveHash = (hash == null || hash.isBlank()) ? randomHash() : hash;
        Random random = new Random(seedFor(effectiveHash, preset));

        int width = preset.width();
        int height = preset.height();
        Coordinate start = new Coordinate(width / 2, height / 2);

        LinkedHashSet<Coordinate> floorTiles = new LinkedHashSet<>();
        floorTiles.add(start);

        List<Coordinate> frontier = new ArrayList<>();
        addFrontier(frontier, floorTiles, start, width, height);

        int targetFloorTiles = Math.min(Math.max(1, preset.approximateFloorTiles()), interiorTileCount(width, height));
        while (floorTiles.size() < targetFloorTiles && !frontier.isEmpty()) {
            Coordinate candidate = frontier.remove(random.nextInt(frontier.size()));
            if (floorTiles.add(candidate)) {
                addFrontier(frontier, floorTiles, candidate, width, height);
            }
        }

        addInternalObstacles(random, floorTiles, start, width, height);

        RoomMap roomMap = new RoomMap(width, height, start, floorTiles);
        if (!MapValidator.isValid(roomMap)) {
            throw new IllegalStateException("generated map is invalid");
        }
        return new GeneratedMap(effectiveHash, preset, roomMap);
    }

    private static void addInternalObstacles(Random random, Set<Coordinate> floorTiles, Coordinate start, int width, int height) {
        int obstacleAttempts = Math.max(1, floorTiles.size() / 24);
        List<Coordinate> candidates = new ArrayList<>(floorTiles);
        for (int i = 0; i < obstacleAttempts && candidates.size() > 1; i++) {
            Coordinate candidate = candidates.get(random.nextInt(candidates.size()));
            if (candidate.equals(start) || candidate.x() <= 0 || candidate.y() <= 0
                    || candidate.x() >= width - 1 || candidate.y() >= height - 1) {
                continue;
            }
            if (neighborFloorCount(floorTiles, candidate) < 3) {
                continue;
            }

            LinkedHashSet<Coordinate> probe = new LinkedHashSet<>(floorTiles);
            probe.remove(candidate);
            RoomMap probeMap = new RoomMap(width, height, start, probe);
            if (MapValidator.isReachableFromStart(probeMap)) {
                floorTiles.remove(candidate);
                candidates.remove(candidate);
            }
        }
    }

    private static void addFrontier(List<Coordinate> frontier, Set<Coordinate> floorTiles, Coordinate origin, int width, int height) {
        for (Coordinate neighbor : adjacentTiles(origin)) {
            if (isInterior(neighbor, width, height) && !floorTiles.contains(neighbor) && !frontier.contains(neighbor)) {
                frontier.add(neighbor);
            }
        }
    }

    private static List<Coordinate> adjacentTiles(Coordinate coordinate) {
        return List.of(
                new Coordinate(coordinate.x(), coordinate.y() - 1),
                new Coordinate(coordinate.x() + 1, coordinate.y()),
                new Coordinate(coordinate.x(), coordinate.y() + 1),
                new Coordinate(coordinate.x() - 1, coordinate.y()));
    }

    private static boolean isInterior(Coordinate coordinate, int width, int height) {
        return coordinate.x() > 0 && coordinate.x() < width - 1
                && coordinate.y() > 0 && coordinate.y() < height - 1;
    }

    private static int interiorTileCount(int width, int height) {
        return Math.max(1, (width - 2) * (height - 2));
    }

    private static int neighborFloorCount(Set<Coordinate> floorTiles, Coordinate candidate) {
        int count = 0;
        for (Coordinate adjacent : adjacentTiles(candidate)) {
            if (floorTiles.contains(adjacent)) {
                count++;
            }
        }
        return count;
    }

    private static long seedFor(String hash, SizePreset preset) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((hash + ":" + preset.name()).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String randomHash() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
