import type { GameState, IterationEvent, LeaderboardEntry, SessionResponse, TraceEvent } from './types'

export function createState(session: SessionResponse): GameState {
  return {
    sessionId: session.sessionId,
    robotX: session.robotX,
    robotY: session.robotY,
    itersAvail: session.iterationsAvailable,
    score: 0, totalCleaned: 0, itersUsed: 0,
    cleanedTiles: new Set(),
    finished: false, finishReason: null,
    map: session.map, trace: [],
  }
}

export function applyEvent(state: GameState, ev: IterationEvent): GameState {
  const cleanedTiles = new Set(state.cleanedTiles)
  cleanedTiles.add(`${ev.robotX},${ev.robotY}`)
  return {
    ...state,
    robotX: ev.robotX, robotY: ev.robotY,
    score: ev.score, totalCleaned: ev.totalCleaned,
    itersUsed: ev.iteration,
    cleanedTiles,
    finished: ev.finished,
    finishReason: ev.finishReason ?? null,
    trace: [...state.trace, ev],
  }
}

export function buildLeaderboardEntry(state: GameState, setupBody: {
  username: string; avatar: string; hash: string | null; size: string; algoName: string
}): LeaderboardEntry {
  return {
    id: state.sessionId,
    username: setupBody.username,
    avatar: setupBody.avatar,
    mapHash: setupBody.hash ?? state.sessionId,
    mapSize: setupBody.size,
    algoName: setupBody.algoName,
    iterationsUsed: state.itersUsed,
    iterationsAvailable: state.itersAvail,
    score: state.score,
    completedAt: new Date().toISOString(),
    trace: state.trace.map((e): TraceEvent => ({
      iteration: e.iteration, direction: e.direction,
      x: e.robotX, y: e.robotY, score: e.score,
    })),
  }
}

export function buildReplayEvents(trace: TraceEvent[], totalFloor: number): IterationEvent[] {
  const cleanedSet = new Set<string>()
  return trace.map((t): IterationEvent => {
    cleanedSet.add(`${t.x},${t.y}`)
    return {
      sessionId: '', iteration: t.iteration, direction: t.direction,
      robotX: t.x, robotY: t.y, score: t.score,
      totalCleaned: cleanedSet.size, totalFloor,
      finished: t.iteration === trace.length,
      finishReason: t.iteration === trace.length ? 'COMPLETED' : null,
    }
  })
}
