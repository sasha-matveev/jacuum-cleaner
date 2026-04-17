package com.jacuum.engine;

public interface Messaging {
    void send(String destination, Object payload);
}
