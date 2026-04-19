import { describe, test, expect, vi, beforeEach } from 'vitest'
import { Api } from '../api'

const mockOkJson = (data: unknown) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true, json: () => Promise.resolve(data),
  } as Response)

const mockError = (status: number) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response)

beforeEach(() => vi.restoreAllMocks())

describe('Api.algos', () => {
  test('GETs /api/algos and returns parsed JSON', async () => {
    mockOkJson(['Random', 'Always Left'])
    const result = await Api.algos()
    expect(fetch).toHaveBeenCalledWith('/api/algos')
    expect(result).toEqual(['Random', 'Always Left'])
  })
  test('throws when response is not ok', async () => {
    mockError(500)
    await expect(Api.algos()).rejects.toThrow('500')
  })
})

describe('Api.createSession', () => {
  test('POSTs to /api/session with JSON body', async () => {
    const sessionResp = { sessionId: 'abc', status: 'SETUP' }
    mockOkJson(sessionResp)
    const body = { algoName: 'Random', username: 'Alice', avatar: '🤖', size: 'TINY', iterations: 100 }
    const result = await Api.createSession(body)
    expect(fetch).toHaveBeenCalledWith('/api/session', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }))
    expect(result).toEqual(sessionResp)
  })
  test('throws when server returns 400', async () => {
    mockError(400)
    await expect(Api.createSession({})).rejects.toThrow('400')
  })
})

describe('Api.start', () => {
  test('POSTs to /api/session/{id}/start', async () => {
    mockOkJson({ status: 'RUNNING' })
    await Api.start('my-session')
    expect(fetch).toHaveBeenCalledWith('/api/session/my-session/start', { method: 'POST' })
  })
})

describe('Api.leaderboard', () => {
  test('GETs /api/leaderboard and returns entries', async () => {
    const entries = [{ id: 'e1', score: 100 }]
    mockOkJson(entries)
    const result = await Api.leaderboard()
    expect(fetch).toHaveBeenCalledWith('/api/leaderboard')
    expect(result).toEqual(entries)
  })
})

describe('Api.saveToLb', () => {
  test('POSTs entry to /api/leaderboard', async () => {
    const entry = { id: 'e1', score: 100 } as any
    mockOkJson(entry)
    await Api.saveToLb(entry)
    expect(fetch).toHaveBeenCalledWith('/api/leaderboard', expect.objectContaining({
      method: 'POST', body: JSON.stringify(entry),
    }))
  })
})
