package com.jacuum.engine

import org.springframework.messaging.simp.SimpMessagingTemplate

class ActiveMessaging(private val delegate: SimpMessagingTemplate) : Messaging {
    override fun send(destination: String, payload: Any) {
        delegate.convertAndSend(destination, payload)
    }
}
