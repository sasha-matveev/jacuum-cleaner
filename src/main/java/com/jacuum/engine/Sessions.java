package com.jacuum.engine;

import com.jacuum.map.GameMap;

public interface Sessions {
    String open(GameMap map, String algoName, String username,
                String avatar, int iterations) throws Exception;
    void start(String id) throws Exception;
    void pause(String id) throws Exception;
    void resume(String id) throws Exception;
    void stop(String id) throws Exception;
    SessionView view(String id) throws Exception;
}
