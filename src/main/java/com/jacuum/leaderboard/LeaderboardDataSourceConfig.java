package com.jacuum.leaderboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configures the DataSource based on the optional {@code leaderboard.path} property.
 *
 * <ul>
 *   <li>When set: H2 file-mode DB at the given path — data persists across restarts.</li>
 *   <li>When absent: H2 in-memory DB — leaderboard data is lost on restart.</li>
 * </ul>
 */
@Configuration
public class LeaderboardDataSourceConfig {

    @Value("${leaderboard.path:}")
    private String leaderboardPath;

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        if (leaderboardPath != null && !leaderboardPath.isBlank()) {
            // Strip .db extension if user included it; H2 adds it automatically
            String path = leaderboardPath.replaceAll("\\.db$", "");
            properties.setUrl("jdbc:h2:file:" + path + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        }
        // else: use whatever is in application.properties (defaults to in-memory)
        return properties.initializeDataSourceBuilder().build();
    }
}
