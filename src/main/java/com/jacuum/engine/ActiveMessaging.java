package com.jacuum.engine;

import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class ActiveMessaging implements Messaging {
    private final SimpMessagingTemplate delegate;

    public ActiveMessaging(final SimpMessagingTemplate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void send(final String destination, final Object payload) {
        this.delegate.convertAndSend(destination, payload);
    }
}
