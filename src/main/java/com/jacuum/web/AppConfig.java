package com.jacuum.web;

import com.jacuum.algo.*;
import com.jacuum.engine.*;
import com.jacuum.leaderboard.*;
import com.jacuum.map.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class AppConfig {

    @Bean
    public Maps maps() {
        return new CellularMaps();
    }

    @Bean
    public Algorithms algorithms(ApplicationContext ctx) {
        return new SpringAlgorithms(ctx);
    }

    @Bean
    public Sessions sessions(SimpMessagingTemplate messaging, Algorithms algorithms) {
        return new MemorySessions(messaging, algorithms);
    }

    @Bean
    public Leaderboard leaderboard(@Value("${leaderboard.file:}") String path) {
        if (path == null || path.isBlank()) return new SilentLeaderboard();
        return new JsonLeaderboard(java.nio.file.Path.of(path));
    }
}
