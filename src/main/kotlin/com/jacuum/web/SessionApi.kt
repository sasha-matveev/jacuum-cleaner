package com.jacuum.web

import com.jacuum.engine.SessionView
import com.jacuum.web.dto.CreateSessionRequest
import com.jacuum.web.dto.SessionResponse

internal interface SessionApi {
    @Throws(Exception::class) fun create(req: CreateSessionRequest): SessionResponse
    @Throws(Exception::class) fun start(id: String): SessionView
    @Throws(Exception::class) fun pause(id: String): SessionView
    @Throws(Exception::class) fun resume(id: String): SessionView
    @Throws(Exception::class) fun stop(id: String): SessionView
    @Throws(Exception::class) fun view(id: String): SessionView
}
