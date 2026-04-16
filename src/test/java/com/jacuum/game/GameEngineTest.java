package com.jacuum.game;

import com.jacuum.algo.AlgoRegistry;
import com.jacuum.algo.RobotAlgo;
import com.jacuum.algo.VacuumAlgo;
import com.jacuum.algo.impl.RandomAlgo;
import com.jacuum.map.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GameEngineTest {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        MapGenerator generator = new RoomMapGenerator();
        engine = new GameEngine(generator, mockRegistry("randomAlgo", new RandomAlgo()));
    }

    private static AlgoRegistry mockRegistry(String beanName, RobotAlgo algo) {
        ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
        when(ctx.getBeansWithAnnotation(VacuumAlgo.class))
            .thenReturn(Map.of(beanName, algo));
        when(ctx.getBean(beanName)).thenReturn(algo);
        return new AlgoRegistry(ctx);
    }

    @Test
    void startSessionCreatesSession() {
        GameSession session = engine.startSession("test-hash", "small", "randomAlgo",
                "TestUser", "robot1", 200);
        assertThat(session).isNotNull();
        assertThat(session.getStatus()).isEqualTo(GameStatus.RUNNING);
        assertThat(session.getMaxIterations()).isEqualTo(200);
        assertThat(session.getUsername()).isEqualTo("TestUser");
    }

    @Test
    void stepAdvancesIteration() {
        GameSession session = engine.startSession("step-test", "tiny", "randomAlgo",
                "Bot", "robot1", 100);
        String id = session.getSessionId();

        StepResult result = engine.step(id);
        assertThat(result.iterationsUsed()).isEqualTo(1);
        assertThat(result.cleanedTiles()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void sessionFinishesWhenIterationsExhausted() {
        GameSession session = engine.startSession("exhaust-test", "tiny", "randomAlgo",
                "Bot", "robot1", 3);
        String id = session.getSessionId();

        StepResult last = null;
        for (int i = 0; i < 3; i++) last = engine.step(id);

        assertThat(last.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(last.iterationsUsed()).isEqualTo(3);
    }

    @Test
    void failedAlgoGivesZeroScore() {
        MapGenerator generator = new RoomMapGenerator();
        RobotAlgo throwingAlgo = tile -> { throw new RuntimeException("test failure"); };
        GameEngine eng = new GameEngine(generator, mockRegistry("badAlgo", throwingAlgo));

        GameSession session = eng.startSession("fail-test", "tiny", "badAlgo",
                "Bot", "robot1", 100);
        StepResult result = eng.step(session.getSessionId());

        assertThat(result.status()).isEqualTo(GameStatus.FAILED);
        assertThat(result.score()).isZero();
    }

    @Test
    void pauseAndResume() {
        GameSession session = engine.startSession("pause-test", "tiny", "randomAlgo",
                "Bot", "robot1", 100);
        String id = session.getSessionId();

        engine.pauseSession(id);
        assertThat(engine.getSession(id).getStatus()).isEqualTo(GameStatus.PAUSED);

        // Step while paused should not advance
        StepResult r = engine.step(id);
        assertThat(r.iterationsUsed()).isZero();

        engine.resumeSession(id);
        assertThat(engine.getSession(id).getStatus()).isEqualTo(GameStatus.RUNNING);
    }

    @Test
    void abortSetsStatusAborted() {
        GameSession session = engine.startSession("abort-test", "tiny", "randomAlgo",
                "Bot", "robot1", 100);
        String id = session.getSessionId();

        engine.abortSession(id);
        assertThat(engine.getSession(id).getStatus()).isEqualTo(GameStatus.ABORTED);
    }
}
