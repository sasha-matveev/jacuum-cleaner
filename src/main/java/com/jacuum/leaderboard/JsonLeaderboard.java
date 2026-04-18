package com.jacuum.leaderboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonLeaderboard implements Leaderboard {

    private final Path file;
    private final ObjectMapper mapper;
    private final TypeReference<List<LeaderboardEntry>> type;

    public JsonLeaderboard(final Path file) {
        this.file = file;
        this.mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
        this.type = new TypeReference<>() {};
    }

    @Override
    public synchronized List<LeaderboardEntry> entries() throws Exception {
        if (!Files.exists(this.file)) return List.of();
        return this.mapper.readValue(this.file.toFile(), this.type);
    }

    @Override
    public synchronized void save(final LeaderboardEntry entry) throws Exception {
        final List<LeaderboardEntry> current = new ArrayList<>(entries());
        current.add(entry);
        this.mapper.writeValue(this.file.toFile(), current);
    }
}
