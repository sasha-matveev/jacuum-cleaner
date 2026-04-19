import React, { useEffect, useRef, useState, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Api } from './api'
import { createState, applyEvent, buildLeaderboardEntry } from './GameLogic'
import type { GameState, IterationEvent, SessionResponse, SetupFormValues } from './types'

const CELL = 22
const SPEEDS = [800, 400, 200, 100, 50, 16]
const SPEED_LABELS = ['×0.1', '×0.25', '×0.5', '×1', '×2', '×max']

interface Props {
  session: SessionResponse
  setupValues: SetupFormValues
  onNewGame: () => void
}

export default function GameScreen({ session, setupValues, onNewGame }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const stateRef = useRef<GameState>(createState(session))
  const queueRef = useRef<IterationEvent[]>([])
  const animatingRef = useRef(false)
  const speedRef = useRef(2)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const stompRef = useRef<Client | null>(null)

  const [speedIdx, setSpeedIdx] = useState(2)
  const [hud, setHud] = useState({ score: 0, cleaned: 0, itersUsed: 0 })
  const [finished, setFinished] = useState(false)
  const [finalScore, setFinalScore] = useState(0)
  const [saved, setSaved] = useState(false)

  const render = useCallback((s: GameState) => {
    const canvas = canvasRef.current; if (!canvas) return
    const ctx = canvas.getContext('2d')!
    ctx.fillStyle = '#0a0a16'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    for (const t of s.map.tiles) {
      const px = t.x * CELL, py = t.y * CELL
      ctx.fillStyle = s.cleanedTiles.has(`${t.x},${t.y}`) ? '#e8f4e8' : '#2a2a4a'
      ctx.fillRect(px + 1, py + 1, CELL - 2, CELL - 2)
      ctx.strokeStyle = '#0a0a16'; ctx.lineWidth = 2
      if (t.wallNorth) { ctx.beginPath(); ctx.moveTo(px, py);        ctx.lineTo(px + CELL, py);        ctx.stroke() }
      if (t.wallSouth) { ctx.beginPath(); ctx.moveTo(px, py + CELL); ctx.lineTo(px + CELL, py + CELL); ctx.stroke() }
      if (t.wallWest)  { ctx.beginPath(); ctx.moveTo(px, py);        ctx.lineTo(px, py + CELL);        ctx.stroke() }
      if (t.wallEast)  { ctx.beginPath(); ctx.moveTo(px + CELL, py); ctx.lineTo(px + CELL, py + CELL); ctx.stroke() }
    }
    ctx.font = `${Math.floor(CELL * 0.8)}px serif`
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText(setupValues.avatar || '🤖', s.robotX * CELL + CELL / 2, s.robotY * CELL + CELL / 2)
  }, [setupValues.avatar])

  const drainQueue = useCallback(() => {
    if (queueRef.current.length === 0) { animatingRef.current = false; return }
    animatingRef.current = true
    const ev = queueRef.current.shift()!
    stateRef.current = applyEvent(stateRef.current, ev)
    render(stateRef.current)
    setHud({ score: stateRef.current.score, cleaned: stateRef.current.totalCleaned, itersUsed: stateRef.current.itersUsed })
    if (stateRef.current.finished) {
      setFinished(true); setFinalScore(stateRef.current.score)
      return
    }
    timerRef.current = setTimeout(drainQueue, SPEEDS[speedRef.current])
  }, [render])

  useEffect(() => {
    const canvas = canvasRef.current; if (!canvas) return
    const s = stateRef.current
    canvas.width = s.map.width * CELL; canvas.height = s.map.height * CELL
    render(s)

    const stomp = new Client({
      webSocketFactory: () => new SockJS('/ws') as WebSocket,
      onConnect: () => {
        stomp.subscribe(`/topic/session/${session.sessionId}/events`, msg => {
          queueRef.current.push(JSON.parse(msg.body) as IterationEvent)
          if (!animatingRef.current) drainQueue()
        })
        Api.start(session.sessionId)
      },
    })
    stomp.activate()
    stompRef.current = stomp
    return () => { stomp.deactivate(); if (timerRef.current) clearTimeout(timerRef.current) }
  }, [session.sessionId, render, drainQueue])

  function handlePause() { Api.pause(session.sessionId) }
  function handleResume() { Api.resume(session.sessionId) }
  function handleStop() { Api.stop(session.sessionId) }

  function handleSpeedChange(e: React.ChangeEvent<HTMLInputElement>) {
    const idx = Number(e.target.value)
    speedRef.current = idx; setSpeedIdx(idx)
  }

  async function handleSaveLb() {
    const entry = buildLeaderboardEntry(stateRef.current, { ...setupValues, hash: setupValues.hash || null })
    await Api.saveToLb(entry)
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <section id="screen-game">
      <div id="game-info">
        <span>Score: <strong>{hud.score}</strong></span>
        <span>Cleaned: <strong>{hud.cleaned}</strong>/<strong>{session.totalFloor}</strong></span>
        <span>Iterations: <strong>{hud.itersUsed}</strong>/<strong>{session.iterationsAvailable}</strong></span>
      </div>
      <canvas ref={canvasRef} id="game-canvas" />
      <div id="game-controls">
        <button onClick={handlePause} disabled={finished}>Pause</button>
        <button onClick={handleResume} disabled={finished}>Resume</button>
        <button onClick={handleStop}>Stop</button>
        <label>Speed
          <input type="range" min={0} max={5} step={1} value={speedIdx} onChange={handleSpeedChange} />
          <span>{SPEED_LABELS[speedIdx]}</span>
        </label>
      </div>
      {finished && (
        <div id="finish-panel">
          <h3>Done! Score: <span>{finalScore}</span></h3>
          <button onClick={handleSaveLb} disabled={saved}>{saved ? 'Saved!' : 'Save to Leaderboard'}</button>
          <button onClick={onNewGame}>New Game</button>
        </div>
      )}
    </section>
  )
}
