import React, { useState } from 'react'
import SetupScreen from './SetupScreen'
import GameScreen from './GameScreen'
import LeaderboardScreen from './LeaderboardScreen'
import type { SessionResponse, SetupFormValues } from './types'

type Screen = 'setup' | 'game' | 'leaderboard'

export default function App() {
  const [screen, setScreen] = useState<Screen>('setup')
  const [session, setSession] = useState<SessionResponse | null>(null)
  const [setupValues, setSetupValues] = useState<SetupFormValues | null>(null)

  return (
    <>
      <nav>
        <button onClick={() => setScreen('setup')}>Setup</button>
        <button onClick={() => setScreen('leaderboard')}>Leaderboard</button>
      </nav>
      {screen === 'setup' && (
        <SetupScreen
          onStart={(sess: SessionResponse, vals: SetupFormValues) => { setSession(sess); setSetupValues(vals); setScreen('game') }}
        />
      )}
      {screen === 'game' && session && setupValues && (
        <GameScreen
          session={session}
          setupValues={setupValues}
          onNewGame={() => setScreen('setup')}
        />
      )}
      {screen === 'leaderboard' && (
        <LeaderboardScreen
          onRetry={(hash: string, size: string, iters: number) => setScreen('setup')}
          onReplay={(sess: SessionResponse, vals: SetupFormValues) => { setSession(sess); setSetupValues(vals); setScreen('game') }}
        />
      )}
    </>
  )
}
