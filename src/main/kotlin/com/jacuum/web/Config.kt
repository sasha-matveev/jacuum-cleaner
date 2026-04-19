package com.jacuum.web

import com.jacuum.algo.Algorithms
import com.jacuum.engine.Sessions
import com.jacuum.leaderboard.Leaderboard
import com.jacuum.map.Maps
import org.springframework.context.ApplicationContext
import org.springframework.messaging.simp.SimpMessagingTemplate

internal interface Config {
    fun maps(): Maps
    fun algorithms(ctx: ApplicationContext): Algorithms
    fun sessions(messaging: SimpMessagingTemplate, algorithms: Algorithms, maxSessions: Int): Sessions
    fun leaderboard(path: String): Leaderboard
    fun snapshots(): Snapshots
}
