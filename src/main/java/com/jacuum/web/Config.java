package com.jacuum.web;

import com.jacuum.algo.Algorithms;
import com.jacuum.engine.Sessions;
import com.jacuum.leaderboard.Leaderboard;
import com.jacuum.map.Maps;

interface Config {
    Maps maps();
    Algorithms algorithms(org.springframework.context.ApplicationContext ctx);
    Sessions sessions(org.springframework.messaging.simp.SimpMessagingTemplate messaging,
                      Algorithms algorithms, int maxSessions);
    Leaderboard leaderboard(String path);
    Snapshots snapshots();
}
