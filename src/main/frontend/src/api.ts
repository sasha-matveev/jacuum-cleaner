import type { LeaderboardEntry, SessionResponse } from './types'

const json = <T,>(r: Response): Promise<T> => {
  if (!r.ok) return Promise.reject(new Error(String(r.status)))
  return r.json()
}

export const Api = {
  algos: (): Promise<string[]> => fetch('/api/algos').then((r) => json<string[]>(r)),
  avatars: (): Promise<string[]> => fetch('/api/avatars').then((r) => json<string[]>(r)),
  createSession: (body: object): Promise<SessionResponse> =>
    fetch('/api/session', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then((r) => json<SessionResponse>(r)),
  start:  (id: string): Promise<unknown> => fetch(`/api/session/${id}/start`,  { method: 'POST' }).then(json),
  pause:  (id: string): Promise<unknown> => fetch(`/api/session/${id}/pause`,  { method: 'POST' }).then(json),
  resume: (id: string): Promise<unknown> => fetch(`/api/session/${id}/resume`, { method: 'POST' }).then(json),
  stop:   (id: string): Promise<unknown> => fetch(`/api/session/${id}/stop`,   { method: 'POST' }).then(json),
  leaderboard: (): Promise<LeaderboardEntry[]> => fetch('/api/leaderboard').then((r) => json<LeaderboardEntry[]>(r)),
  saveToLb: (entry: LeaderboardEntry): Promise<LeaderboardEntry> =>
    fetch('/api/leaderboard', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(entry),
    }).then((r) => json<LeaderboardEntry>(r)),
}
