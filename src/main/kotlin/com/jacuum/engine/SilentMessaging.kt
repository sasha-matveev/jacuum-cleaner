package com.jacuum.engine

internal class SilentMessaging : Messaging {
    override fun send(destination: String, payload: Any) {}
}
