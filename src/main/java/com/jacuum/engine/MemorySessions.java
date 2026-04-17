package com.jacuum.engine;

import com.jacuum.map.GameMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MemorySessions implements Sessions {

    private final ConcurrentHashMap<String, ActiveSession> store = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messaging;

    public MemorySessions(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public String open(GameMap map, String algoName, String username,
                       String avatar, int iterations) throws Exception {
        String id = UUID.randomUUID().toString();
        store.put(id, new ActiveSession(id, map, algoName, username, avatar, iterations));
        return id;
    }

    @Override
    public SessionView view(String id) throws Exception {
        ActiveSession s = require(id);
        return s.toView();
    }

    @Override
    public void start(String id) throws Exception {
        // implemented in Task 8
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override public void pause(String id)  throws Exception { require(id).status = RunStatus.PAUSED; }
    @Override public void resume(String id) throws Exception { require(id).status = RunStatus.RUNNING; }
    @Override public void stop(String id)   throws Exception {
        ActiveSession s = require(id);
        s.status = RunStatus.FINISHED;
        s.finishReason = FinishReason.INTERRUPTED;
        if (s.future != null) s.future.cancel(true);
    }

    // package-private: used by the game loop (added in Task 8) within the engine package
    ActiveSession require(String id) throws Exception {
        ActiveSession s = store.get(id);
        if (s == null) throw new Exception("Unknown session: " + id);
        return s;
    }
}
