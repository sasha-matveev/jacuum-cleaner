package dev.ytype.jacuum.engine;

public final class RunEngine {

    public RunSession start(RunRequest request) {
        return new RunSession(request);
    }
}
