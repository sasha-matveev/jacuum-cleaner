package com.jacuum.leaderboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonLeaderboard implements Leaderboard {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<LeaderboardEntry>> TYPE =
        new TypeReference<>() {};

    private final Path file;

    public JsonLeaderboard(final Path file) {
        this.file = file;
    }

    @Override
    public List<LeaderboardEntry> entries() throws Exception {
        if (!Files.exists(file)) return List.of();
        return MAPPER.readValue(file.toFile(), TYPE);
    }

    @Override
    public void save(final LeaderboardEntry entry) throws Exception {
        final List<LeaderboardEntry> current = new ArrayList<>(entries());
        current.add(entry);
        MAPPER.writeValue(file.toFile(), current);
    }
}
