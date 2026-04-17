package com.jacuum.web.dto;

public record CreateSessionRequest(
    String hash,
    String size,
    String algoName,
    String username,
    String avatar,
    int iterations
) {}
