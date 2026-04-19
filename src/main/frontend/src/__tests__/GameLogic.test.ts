import { describe, test, expect } from 'vitest'
import { createState, applyEvent, buildLeaderboardEntry, buildReplayEvents } from '../GameLogic'
import type { SessionResponse, IterationEvent } from '../types'

function makeSession(overrides: Partial<SessionResponse> = {}): SessionResponse {
  return {
    sessionId: 'sess-1', robotX: 2, robotY: 3,
    iterationsAvailable: 100, status: 'SETUP',
    map: { width: 10, height: 8, startX: 2, startY: 3, totalFloor: 15, tiles: [] },
    totalFloor: 15,
    ...overrides,
  }
}

function makeEvent(overrides: Partial<IterationEvent> = {}): IterationEvent {
  return {
    sessionId: 'sess-1', robotX: 3, robotY: 3, score: 100,
    totalCleaned: 1, iteration: 1, direction: 'EAST',
    totalFloor: 15, finished: false, finishReason: null,
    ...overrides,
  }
}

describe('createState', () => {
  test('copies session fields into initial state', () => {
    const state = createState(makeSession())
    expect(state.sessionId).toBe('sess-1')
    expect(state.robotX).toBe(2); expect(state.robotY).toBe(3)
    expect(state.itersAvail).toBe(100)
    expect(state.map.totalFloor).toBe(15)
  })
  test('all counters start at zero', () => {
    const state = createState(makeSession())
    expect(state.score).toBe(0); expect(state.totalCleaned).toBe(0); expect(state.itersUsed).toBe(0)
  })
  test('starts not finished with empty trace', () => {
    const state = createState(makeSession())
    expect(state.finished).toBe(false); expect(state.finishReason).toBeNull()
    expect(state.trace).toHaveLength(0); expect(state.cleanedTiles.size).toBe(0)
  })
})

describe('applyEvent', () => {
  test('updates robot position, score, and iteration counter', () => {
    const s1 = applyEvent(createState(makeSession()), makeEvent({ robotX: 4, robotY: 3, score: 100, totalCleaned: 1, iteration: 1 }))
    expect(s1.robotX).toBe(4); expect(s1.score).toBe(100); expect(s1.itersUsed).toBe(1)
  })
  test('adds visited tile to cleanedTiles', () => {
    const s1 = applyEvent(createState(makeSession()), makeEvent({ robotX: 4, robotY: 3 }))
    expect(s1.cleanedTiles.has('4,3')).toBe(true)
  })
  test('does not mutate original state', () => {
    const s0 = createState(makeSession())
    applyEvent(s0, makeEvent({ robotX: 4, robotY: 3 }))
    expect(s0.robotX).toBe(2); expect(s0.cleanedTiles.size).toBe(0)
  })
  test('accumulates trace across multiple events', () => {
    let state = createState(makeSession())
    state = applyEvent(state, makeEvent({ iteration: 1, robotX: 3, robotY: 3 }))
    state = applyEvent(state, makeEvent({ iteration: 2, robotX: 4, robotY: 3 }))
    expect(state.trace).toHaveLength(2)
  })
  test('marks state as finished', () => {
    const s1 = applyEvent(createState(makeSession()), makeEvent({ finished: true, finishReason: 'COMPLETED' }))
    expect(s1.finished).toBe(true); expect(s1.finishReason).toBe('COMPLETED')
  })
})

describe('buildLeaderboardEntry', () => {
  test('maps state fields to entry fields correctly', () => {
    const entry = buildLeaderboardEntry(
      createState(makeSession({ sessionId: 'abc' })),
      { username: 'Alice', avatar: '🤖', hash: 'h1', size: 'TINY', algoName: 'Random' }
    )
    expect(entry.id).toBe('abc'); expect(entry.username).toBe('Alice')
    expect(entry.mapHash).toBe('h1'); expect(entry.score).toBe(0); expect(entry.trace).toEqual([])
  })
  test('falls back to sessionId when hash is null', () => {
    const entry = buildLeaderboardEntry(
      createState(makeSession({ sessionId: 'fallback-id' })),
      { username: 'Bob', avatar: '🤖', hash: null, size: 'SMALL', algoName: 'Always Left' }
    )
    expect(entry.mapHash).toBe('fallback-id')
  })
  test('serialises trace into compact TraceEvent objects', () => {
    let state = createState(makeSession({ sessionId: 'sess' }))
    state = applyEvent(state, makeEvent({ iteration: 1, direction: 'EAST', robotX: 3, robotY: 3, score: 100 }))
    const entry = buildLeaderboardEntry(state, { username: 'Alice', avatar: '🤖', hash: 'h', size: 'TINY', algoName: 'Random' })
    expect(entry.trace).toHaveLength(1)
    expect(entry.trace[0]).toEqual({ iteration: 1, direction: 'EAST', x: 3, y: 3, score: 100 })
  })
})

describe('buildReplayEvents', () => {
  test('converts a trace into robotX/robotY events', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 3, y: 3, score: 100 },
      { iteration: 2, direction: 'EAST', x: 4, y: 3, score: 200 },
    ], 10)
    expect(events).toHaveLength(2)
    expect(events[0].robotX).toBe(3); expect(events[1].robotX).toBe(4)
  })
  test('increments totalCleaned for new tiles', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 },
      { iteration: 2, direction: 'EAST', x: 2, y: 0, score: 200 },
    ], 5)
    expect(events[0].totalCleaned).toBe(1); expect(events[1].totalCleaned).toBe(2)
  })
  test('does not double-count revisited tiles', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 },
      { iteration: 2, direction: 'WEST', x: 1, y: 0, score: 100 },
    ], 5)
    expect(events[0].totalCleaned).toBe(1); expect(events[1].totalCleaned).toBe(1)
  })
  test('marks last event as finished=true', () => {
    const events = buildReplayEvents([{ iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 }], 5)
    expect(events[0].finished).toBe(true); expect(events[0].finishReason).toBe('COMPLETED')
  })
  test('intermediate events are not finished', () => {
    const events = buildReplayEvents([
      { iteration: 1, direction: 'EAST', x: 1, y: 0, score: 100 },
      { iteration: 2, direction: 'EAST', x: 2, y: 0, score: 200 },
    ], 5)
    expect(events[0].finished).toBe(false); expect(events[1].finished).toBe(true)
  })
})
