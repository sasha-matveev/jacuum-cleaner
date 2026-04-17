package com.jacuum.engine;

public record StatusEvent(String sessionId, RunStatus status, FinishReason finishReason) {}
