export interface TileSnapshot {
  x: number; y: number
  wallNorth: boolean; wallSouth: boolean; wallEast: boolean; wallWest: boolean
}

export interface MapSnapshot {
  width: number; height: number
  startX: number; startY: number
  totalFloor: number
  tiles: TileSnapshot[]
}

export interface SessionResponse {
  sessionId: string
  status: string
  map: MapSnapshot
  robotX: number; robotY: number
  totalFloor: number
  iterationsAvailable: number
}

export interface IterationEvent {
  sessionId: string
  iteration: number
  direction: string | null
  robotX: number; robotY: number
  score: number
  totalCleaned: number; totalFloor: number
  finished: boolean
  finishReason: string | null
}

export interface TraceEvent {
  iteration: number
  direction: string | null
  x: number; y: number; score: number
}

export interface LeaderboardEntry {
  id: string; username: string; avatar: string
  mapHash: string; mapSize: string; algoName: string
  iterationsUsed: number; iterationsAvailable: number
  score: number; completedAt: string
  trace: TraceEvent[]
}

export interface GameState {
  sessionId: string
  robotX: number; robotY: number
  itersAvail: number
  score: number; totalCleaned: number; itersUsed: number
  cleanedTiles: Set<string>
  finished: boolean; finishReason: string | null
  map: MapSnapshot
  trace: IterationEvent[]
}

export interface SetupFormValues {
  hash: string; size: string; username: string
  avatar: string; algoName: string; iterations: number
}
