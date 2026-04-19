import React, { useEffect, useState } from 'react'
import { Api } from './api'
import type { SessionResponse, SetupFormValues } from './types'

const ITER_VALUES = [250, 500, 1000, 2000, 5000]
const PREFS_KEY = 'jacuum_prefs'
const HEROES = ['Luke Skywalker', 'Leia Organa', 'Han Solo', 'Rey', 'Din Djarin', 'Obi-Wan Kenobi']

interface Props {
  onStart: (session: SessionResponse, values: SetupFormValues) => void
}

export default function SetupScreen({ onStart }: Props) {
  const [algos, setAlgos] = useState<string[]>([])
  const [avatars, setAvatars] = useState<string[]>([])
  const [hash, setHash] = useState('')
  const [size, setSize] = useState('SMALL')
  const [username, setUsername] = useState('')
  const [avatar, setAvatar] = useState('🤖')
  const [algoName, setAlgoName] = useState('')
  const [itersIdx, setItersIdx] = useState(1)
  const [heroPlaceholder] = useState(() => HEROES[Math.floor(Math.random() * HEROES.length)])

  useEffect(() => {
    Promise.all([Api.algos(), Api.avatars()]).then(([a, av]) => {
      setAlgos(a); setAvatars(av)
      const prefs = JSON.parse(localStorage.getItem(PREFS_KEY) || '{}')
      if (prefs.hash)   setHash(prefs.hash)
      if (prefs.size)   setSize(prefs.size)
      if (prefs.username) setUsername(prefs.username)
      if (prefs.avatar)   setAvatar(prefs.avatar)
      if (prefs.iters !== undefined) setItersIdx(Number(prefs.iters))
      if (prefs.algo && a.includes(prefs.algo)) setAlgoName(prefs.algo)
      else if (a.length > 0) setAlgoName(a[0])
    })
  }, [])

  async function handleStart() {
    const values: SetupFormValues = {
      hash, size, username: username || heroPlaceholder,
      avatar, algoName, iterations: ITER_VALUES[itersIdx],
    }
    localStorage.setItem(PREFS_KEY, JSON.stringify({ hash, size, username, avatar, algo: algoName, iters: itersIdx }))
    try {
      const session = await Api.createSession(values)
      onStart(session, values)
    } catch (e: any) {
      alert('Failed to start: ' + e.message)
    }
  }

  return (
    <section id="screen-setup">
      <h2>New Game</h2>
      <label>Map Hash <input value={hash} onChange={e => setHash(e.target.value)} placeholder="leave blank for random" /></label>
      <label>Size
        <select value={size} onChange={e => setSize(e.target.value)}>
          <option value="TINY">Tiny</option>
          <option value="SMALL">Small</option>
          <option value="MEDIUM">Medium</option>
          <option value="LARGE">Large</option>
        </select>
      </label>
      <label>Username <input value={username} onChange={e => setUsername(e.target.value)} placeholder={heroPlaceholder} /></label>
      <label>Avatar
        <div id="avatar-picker">
          {avatars.map(av => (
            <span key={av} className={'avatar-opt' + (av === avatar ? ' selected' : '')}
                  onClick={() => setAvatar(av)}>{av}</span>
          ))}
        </div>
      </label>
      <label>Algorithm
        <select value={algoName} onChange={e => setAlgoName(e.target.value)}>
          {algos.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
      </label>
      <label>Iterations
        <input type="range" min={0} max={4} step={1} value={itersIdx}
               onChange={e => setItersIdx(Number(e.target.value))} />
        <span>{ITER_VALUES[itersIdx]?.toLocaleString()}</span>
      </label>
      <button onClick={handleStart}>Start Game</button>
    </section>
  )
}
