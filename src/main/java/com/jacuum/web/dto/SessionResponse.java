package com.jacuum.web.dto;

public record SessionResponse(
    String sessionId,
    String status,
    MapSnapshot map,
    int robotX,
    int robotY,
    int totalFloor,
    int iterationsAvailable
) {}
