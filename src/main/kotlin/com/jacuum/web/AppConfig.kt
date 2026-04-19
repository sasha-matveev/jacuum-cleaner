package com.jacuum.web

import com.jacuum.algo.Algorithms
import com.jacuum.algo.SpringAlgorithms
import com.jacuum.engine.ActiveMessaging
import com.jacuum.engine.MemorySessions
import com.jacuum.engine.Sessions
import com.jacuum.leaderboard.JsonLeaderboard
import com.jacuum.leaderboard.Leaderboard
import com.jacuum.leaderboard.SilentLeaderboard
import com.jacuum.map.CellularMaps
import com.jacuum.map.Maps
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.nio.file.Path

@Configuration(proxyBeanMethods = false)
class AppConfig : Config {
    @Bean override fun maps(): Maps = CellularMaps()
    @Bean override fun algorithms(ctx: ApplicationContext): Algorithms = SpringAlgorithms(ctx)
    @Bean override fun sessions(
        messaging: SimpMessagingTemplate,
        algorithms: Algorithms,
        @Value("\${game.max-sessions:50}") maxSessions: Int
    ): Sessions = MemorySessions(ActiveMessaging(messaging), algorithms, maxSessions)
    @Bean override fun leaderboard(@Value("\${leaderboard.file:}") path: String): Leaderboard =
        if (path.isBlank()) SilentLeaderboard() else JsonLeaderboard(Path.of(path))
    @Bean override fun snapshots(): Snapshots = GameMapSnapshots()
}
