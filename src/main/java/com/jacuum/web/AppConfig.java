package com.jacuum.web;

import com.jacuum.algo.Algorithms;
import com.jacuum.algo.SpringAlgorithms;
import com.jacuum.engine.MemorySessions;
import com.jacuum.engine.Sessions;
import com.jacuum.leaderboard.JsonLeaderboard;
import com.jacuum.leaderboard.Leaderboard;
import com.jacuum.leaderboard.SilentLeaderboard;
import com.jacuum.map.CellularMaps;
import com.jacuum.map.Maps;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration(proxyBeanMethods = false)
public final class AppConfig {

    @Bean
    public Maps maps() {
        return new CellularMaps();
    }

    @Bean
    public Algorithms algorithms(final ApplicationContext ctx) {
        return new SpringAlgorithms(ctx);
    }

    @Bean
    public Sessions sessions(final SimpMessagingTemplate messaging,
                             final Algorithms algorithms) {
        return new MemorySessions(messaging, algorithms);
    }

    @Bean
    public Leaderboard leaderboard(
            @Value("${leaderboard.file:}") final String path) {
        if (path.isBlank()) return new SilentLeaderboard();
        return new JsonLeaderboard(Path.of(path));
    }
}
