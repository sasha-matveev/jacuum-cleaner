import React, { useEffect, useState } from 'react'
import { Api } from './api'
import type { LeaderboardEntry, SessionResponse, SetupFormValues } from './types'

const ITER_VALUES = [250, 500, 1000, 2000, 5000]

interface Props {
  onRetry: (hash: string, size: string, iters: number) => void
  onReplay: (session: SessionResponse, values: SetupFormValues) => void
}

export default function LeaderboardScreen({ onRetry, onReplay }: Props) {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([])
  const [empty, setEmpty] = useState(false)

  useEffect(() => {
    Api.leaderboard()
      .then(data => {
        if (!data || data.length === 0) { setEmpty(true); return }
        setEntries([...data].sort((a, b) => b.score - a.score))
      })
      .catch(() => setEmpty(true))
  }, [])

  async function handleReplay(e: LeaderboardEntry) {
    try {
      const session = await Api.createSession({
        hash: e.mapHash, size: e.mapSize,
        algoName: e.algoName, username: e.username,
        avatar: e.avatar, iterations: e.iterationsAvailable,
      })
      onReplay(session, { hash: e.mapHash, size: e.mapSize, algoName: e.algoName,
                          username: e.username, avatar: e.avatar, iterations: e.iterationsAvailable })
    } catch (e: any) {
      alert('Failed to replay: ' + e.message)
    }
  }

  function handleRetry(e: LeaderboardEntry) {
    onRetry(e.mapHash, e.mapSize, e.iterationsAvailable)
  }

  return (
    <section id="screen-leaderboard">
      <h2>Leaderboard</h2>
      {empty ? (
        <p id="lb-empty">No leaderboard file configured.</p>
      ) : (
        <table id="lb-table">
          <thead><tr>
            <th>#</th><th>Avatar</th><th>Name</th><th>Map</th>
            <th>Size</th><th>Algo</th><th>Iter Used/Avail</th><th>Score</th><th>Actions</th>
          </tr></thead>
          <tbody>
            {entries.map((e, i) => (
              <tr key={e.id}>
                <td>{i + 1}</td>
                <td>{e.avatar}</td>
                <td>{e.username}</td>
                <td><code>{e.mapHash.slice(0, 8)}</code></td>
                <td>{e.mapSize}</td>
                <td>{e.algoName}</td>
                <td>{e.iterationsUsed} / {e.iterationsAvailable}</td>
                <td><strong>{e.score}</strong></td>
                <td>
                  <button onClick={() => handleReplay(e)}>Replay</button>
                  <button onClick={() => handleRetry(e)}>Retry</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
