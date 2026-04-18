// Pure game state logic — no DOM, no network, no STOMP.
const GameLogic = (() => {

  // Creates initial state from the session creation API response.
  function createState(session) {
    return {
      sessionId:    session.sessionId,
      robotX:       session.robotX,
      robotY:       session.robotY,
      itersAvail:   session.iterationsAvailable,
      score:        0,
      totalCleaned: 0,
      itersUsed:    0,
      cleanedTiles: new Set(),
      finished:     false,
      finishReason: null,
      map:          session.map,
      trace:        [],
    };
  }

  // Applies one IterationEvent to produce a new state. Does NOT mutate the original.
  function applyEvent(state, ev) {
    const cleanedTiles = new Set(state.cleanedTiles);
    cleanedTiles.add(ev.robotX + ',' + ev.robotY);
    return Object.assign({}, state, {
      robotX:       ev.robotX,
      robotY:       ev.robotY,
      score:        ev.score,
      totalCleaned: ev.totalCleaned,
      itersUsed:    ev.iteration,
      cleanedTiles,
      finished:     ev.finished,
      finishReason: ev.finishReason || null,
      trace:        state.trace.concat([ev]),
    });
  }

  // Builds the leaderboard entry object to POST to /api/leaderboard.
  function buildLeaderboardEntry(state, setupBody) {
    return {
      id:                  state.sessionId,
      username:            setupBody.username,
      avatar:              setupBody.avatar,
      mapHash:             setupBody.hash || state.sessionId,
      mapSize:             setupBody.size,
      algoName:            setupBody.algoName,
      iterationsUsed:      state.itersUsed,
      iterationsAvailable: state.itersAvail,
      score:               state.score,
      completedAt:         new Date().toISOString(),
      trace: state.trace.map(function(e) {
        return { iteration: e.iteration, direction: e.direction,
                 x: e.robotX, y: e.robotY, score: e.score };
      }),
    };
  }

  // Converts a stored trace array into the synthetic event objects used by the replay queue.
  function buildReplayEvents(trace, totalFloor) {
    const cleanedSet = new Set();
    return trace.map(function(t) {
      cleanedSet.add(t.x + ',' + t.y);
      return {
        robotX:       t.x,
        robotY:       t.y,
        score:        t.score,
        iteration:    t.iteration,
        direction:    t.direction,
        totalCleaned: cleanedSet.size,
        totalFloor:   totalFloor,
        finished:     t.iteration === trace.length,
        finishReason: t.iteration === trace.length ? 'COMPLETED' : null,
      };
    });
  }

  return { createState, applyEvent, buildLeaderboardEntry, buildReplayEvents };
})();

// CommonJS export for Jest tests (no-op in browser)
if (typeof module !== 'undefined') module.exports = GameLogic;
