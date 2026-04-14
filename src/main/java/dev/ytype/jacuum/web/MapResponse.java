package dev.ytype.jacuum.web;

import java.util.List;

public record MapResponse(
        String hash,
        String size,
        int width,
        int height,
        StartCoordinate start,
        List<List<String>> tileMatrix) {

    public record StartCoordinate(int x, int y) {
    }
}
