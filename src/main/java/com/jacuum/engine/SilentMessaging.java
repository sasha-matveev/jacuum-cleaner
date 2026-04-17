package com.jacuum.engine;

final class SilentMessaging implements Messaging {
    @Override
    public void send(final String destination, final Object payload) {
        // do nothing — null object
    }
}
