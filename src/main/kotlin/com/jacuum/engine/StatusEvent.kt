package com.jacuum.engine

data class StatusEvent(val sessionId: String, val status: RunStatus, val finishReason: FinishReason?)
