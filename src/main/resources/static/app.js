// ===== Constants =====
const AVATARS = ['🤖','🦾','🛸','🚀','⚡','🔧','💡','🎯','🧲','🌀','🦿','🔮'];
const ITER_VALUES = [100, 250, 500, 1000, 2500, 5000];
const TILE_SIZE = 22; // px per tile
const ROBOT_COLOR = '#7ec8e3';
const WALL_COLOR  = '#1a1a2e';
const FLOOR_COLOR = '#1e2d3a';
const CLEAN_COLOR = '#1e3a2a';
const GRID_COLOR  = '#22223a';
const ROBOT_GLOW  = 'rgba(126,200,227,0.35)';

// ===== State =====
let currentSession = null; // { sessionId, hash, size, width, height, startX, startY, walls, floorTiles, username, avatar, algoId, maxIterations }
let eventSource = null;
let robot = { x: 0, y: 0, px: 0, py: 0, progress: 1 }; // pixel-level animation
let animFrame = null;
let speedLevel = 3;
let lastStepResult = null;

// ===== DOM =====
const $ = id => document.getElementById(id);
const canvas = $('game-canvas');
const ctx = canvas.getContext('2d');

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
  loadPreferences();
  setupAvatarGrid();
  loadAlgos();
  bindEvents();
  checkLeaderboardStatus();
  refreshLeaderboard();
});

// ===== Preferences (localStorage) =====
const PREF_KEYS = ['hash-input','size-select','algo-select','username-input','iter-slider'];

function loadPreferences() {
  PREF_KEYS.forEach(id => {
    const val = localStorage.getItem('pref_' + id);
    if (val && $(id)) $(id).value = val;
  });
  const savedAvatar = localStorage.getItem('pref_avatar') || '🤖';
  updateIterDisplay();
  // Avatar selection restored after grid is built
  window._savedAvatar = savedAvatar;
  const savedSpeed = parseInt(localStorage.getItem('pref_speed') || '3');
  speedLevel = savedSpeed;
}

function savePreference(key, value) {
  localStorage.setItem('pref_' + key, value);
}

// ===== Avatar Grid =====
function setupAvatarGrid() {
  const grid = $('avatar-grid');
  AVATARS.forEach(emoji => {
    const btn = document.createElement('button');
    btn.className = 'avatar-btn';
    btn.textContent = emoji;
    btn.dataset.avatar = emoji;
    if (emoji === window._savedAvatar) btn.classList.add('selected');
    btn.onclick = () => {
      grid.querySelectorAll('.avatar-btn').forEach(b => b.classList.remove('selected'));
      btn.classList.add('selected');
      savePreference('avatar', emoji);
    };
    grid.appendChild(btn);
  });
  // Default selection
  if (!grid.querySelector('.selected')) grid.querySelector('.avatar-btn').classList.add('selected');
}

function selectedAvatar() {
  const btn = $('avatar-grid').querySelector('.selected');
  return btn ? btn.dataset.avatar : '🤖';
}

// ===== Algos =====
async function loadAlgos() {
  try {
    const algos = await fetch('/api/algos').then(r => r.json());
    const sel = $('algo-select');
    sel.innerHTML = '';
    algos.forEach(a => {
      const opt = document.createElement('option');
      opt.value = a.id; opt.textContent = a.name;
      sel.appendChild(opt);
    });
    const saved = localStorage.getItem('pref_algo-select');
    if (saved) sel.value = saved;
  } catch(e) { console.error('Failed to load algos', e); }
}

// ===== Iter Slider =====
function updateIterDisplay() {
  const idx = parseInt($('iter-slider').value);
  $('iter-display').textContent = ITER_VALUES[idx].toLocaleString() + ' iterations';
}

// ===== Tab Navigation =====
function switchTab(name) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab-pane').forEach(p => p.classList.toggle('active', p.id === name + '-pane'));
  if (name === 'leaderboard') refreshLeaderboard();
}

// ===== Events =====
function bindEvents() {
  // Tabs
  document.querySelectorAll('.tab-btn').forEach(b =>
    b.addEventListener('click', () => switchTab(b.dataset.tab)));

  // Iter slider
  $('iter-slider').addEventListener('input', () => {
    updateIterDisplay();
    savePreference('iter-slider', $('iter-slider').value);
  });

  // Hash / name randoms
  $('random-hash-btn').addEventListener('click', () => {
    $('hash-input').value = crypto.randomUUID();
    savePreference('hash-input', $('hash-input').value);
  });
  $('random-name-btn').addEventListener('click', () => {
    $('username-input').value = '';
    savePreference('username-input', '');
  });

  // Auto-save setup fields
  ['hash-input','size-select','algo-select','username-input'].forEach(id => {
    const el = $(id);
    if (el) el.addEventListener('change', () => savePreference(id, el.value));
  });

  // Start
  $('start-btn').addEventListener('click', startGame);

  // Controls
  $('btn-run').addEventListener('click', () => {
    if (!currentSession) return;
    fetch(`/api/game/${currentSession.sessionId}/control`, {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({action:'RESUME'})
    });
    setControlState('running');
    openStream();
  });

  $('btn-pause').addEventListener('click', () => {
    if (!currentSession) return;
    fetch(`/api/game/${currentSession.sessionId}/control`, {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({action:'PAUSE'})
    });
    setControlState('paused');
  });

  $('btn-abort').addEventListener('click', () => {
    if (!currentSession) return;
    fetch(`/api/game/${currentSession.sessionId}/control`, {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({action:'ABORT'})
    });
    closeStream();
    showEndOverlay({status:'ABORTED', score: lastStepResult?.score || 0});
  });

  // Speed dots
  $('speed-dots').addEventListener('click', e => {
    const dot = e.target.closest('.speed-dot');
    if (!dot) return;
    speedLevel = parseInt(dot.dataset.level);
    updateSpeedDots();
    savePreference('speed', speedLevel);
    if (currentSession) {
      fetch(`/api/game/${currentSession.sessionId}/control`, {
        method: 'POST', headers: {'Content-Type':'application/json'},
        body: JSON.stringify({action:'SPEED', level: speedLevel})
      });
    }
  });
  updateSpeedDots();

  // End overlay buttons
  $('btn-save-lb').addEventListener('click', saveToLeaderboard);
  $('btn-retry').addEventListener('click', retryGame);
  $('btn-new-game').addEventListener('click', () => {
    $('end-overlay').classList.add('hidden');
    switchTab('setup');
  });
}

function updateSpeedDots() {
  document.querySelectorAll('.speed-dot').forEach(d => {
    d.classList.toggle('active', parseInt(d.dataset.level) <= speedLevel);
  });
}

// ===== Start Game =====
async function startGame() {
  const hash = $('hash-input').value.trim();
  const size = $('size-select').value;
  const algoId = $('algo-select').value;
  const username = $('username-input').value.trim();
  const avatar = selectedAvatar();
  const maxIterations = ITER_VALUES[parseInt($('iter-slider').value)];

  if (!algoId) { toast('Select an algorithm first'); return; }

  $('start-btn').disabled = true;
  try {
    const session = await fetch('/api/game/start', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({hash, size, algoId, username, avatar, maxIterations})
    }).then(r => r.json());

    currentSession = session;
    lastStepResult = null;
    robot = { x: session.startX, y: session.startY,
              px: session.startX, py: session.startY, progress: 1 };

    initCanvas(session);
    drawMap(session.walls, session.width, session.height);
    drawRobot(session.startX, session.startY, 1);

    $('stat-player').textContent = session.username;
    $('stat-avatar').textContent = session.avatar;
    $('stat-cleaned').textContent = `0 / ${session.floorTiles}`;
    $('stat-score').textContent = '0';
    updateIterBar(0, session.maxIterations);

    switchTab('game');
    setControlState('running');
    openStream();
  } catch(e) {
    toast('Failed to start game: ' + e.message);
  } finally {
    $('start-btn').disabled = false;
  }
}

// ===== Canvas =====
function initCanvas(session) {
  canvas.width  = session.width  * TILE_SIZE;
  canvas.height = session.height * TILE_SIZE;
}

function drawMap(walls, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      ctx.fillStyle = walls[y][x] ? WALL_COLOR : FLOOR_COLOR;
      ctx.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
      if (!walls[y][x]) {
        ctx.strokeStyle = GRID_COLOR;
        ctx.lineWidth = 0.5;
        ctx.strokeRect(x * TILE_SIZE + 0.5, y * TILE_SIZE + 0.5, TILE_SIZE - 1, TILE_SIZE - 1);
      }
    }
  }
}

function drawTile(x, y, walls, cleaned) {
  const isWall = walls[y][x];
  ctx.fillStyle = isWall ? WALL_COLOR : (cleaned ? CLEAN_COLOR : FLOOR_COLOR);
  ctx.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
  if (!isWall) {
    ctx.strokeStyle = GRID_COLOR;
    ctx.lineWidth = 0.5;
    ctx.strokeRect(x * TILE_SIZE + 0.5, y * TILE_SIZE + 0.5, TILE_SIZE - 1, TILE_SIZE - 1);
  }
}

function drawRobot(x, y, alpha) {
  const cx = x * TILE_SIZE + TILE_SIZE / 2;
  const cy = y * TILE_SIZE + TILE_SIZE / 2;
  const r  = TILE_SIZE * 0.38;

  // Glow
  ctx.globalAlpha = alpha * 0.4;
  ctx.beginPath();
  ctx.arc(cx, cy, r * 1.7, 0, Math.PI * 2);
  const grd = ctx.createRadialGradient(cx, cy, 0, cx, cy, r * 1.7);
  grd.addColorStop(0, ROBOT_GLOW);
  grd.addColorStop(1, 'transparent');
  ctx.fillStyle = grd;
  ctx.fill();

  // Body
  ctx.globalAlpha = alpha;
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.fillStyle = ROBOT_COLOR;
  ctx.fill();

  // Eyes
  ctx.fillStyle = '#0f0f1a';
  ctx.beginPath(); ctx.arc(cx - r * 0.3, cy - r * 0.2, r * 0.18, 0, Math.PI * 2); ctx.fill();
  ctx.beginPath(); ctx.arc(cx + r * 0.3, cy - r * 0.2, r * 0.18, 0, Math.PI * 2); ctx.fill();

  ctx.globalAlpha = 1;
}

// ===== Animation =====
function animateRobotTo(tx, ty, walls, cleanedSet, onDone) {
  const fromX = robot.x, fromY = robot.y;
  const startTime = performance.now();
  const duration = 120; // ms

  if (animFrame) cancelAnimationFrame(animFrame);

  function frame(now) {
    const t = Math.min((now - startTime) / duration, 1);
    const ease = t < 0.5 ? 2*t*t : -1+(4-2*t)*t; // ease-in-out

    // Clear previous robot position
    const drawX = fromX + (tx - fromX) * ease;
    const drawY = fromY + (ty - fromY) * ease;

    // Redraw area around old and new position
    [fromX, tx].forEach(x => [fromY, ty].forEach(y => {
      if (x >= 0 && y >= 0) drawTile(x, y, walls, cleanedSet.has(`${x},${y}`));
    }));

    drawRobot(drawX, drawY, 1);

    if (t < 1) {
      animFrame = requestAnimationFrame(frame);
    } else {
      robot.x = tx; robot.y = ty;
      animFrame = null;
      onDone?.();
    }
  }
  animFrame = requestAnimationFrame(frame);
}

// ===== SSE Stream =====
let cleanedSet = new Set();

function openStream() {
  closeStream();
  if (!currentSession) return;
  cleanedSet = new Set([`${currentSession.startX},${currentSession.startY}`]);

  eventSource = new EventSource(`/api/game/${currentSession.sessionId}/stream`);

  eventSource.addEventListener('step', e => {
    const data = JSON.parse(e.data);
    lastStepResult = data;
    handleStep(data);
  });

  eventSource.onerror = () => {
    closeStream();
  };
}

function closeStream() {
  if (eventSource) { eventSource.close(); eventSource = null; }
}

function handleStep(data) {
  const {robotX, robotY, justCleaned, cleanedTiles, totalFloorTiles,
         iterationsUsed, maxIterations, score, status} = data;

  // Update cleaned set
  cleanedSet.add(`${robotX},${robotY}`);

  // Animate robot
  const walls = currentSession.walls;
  animateRobotTo(robotX, robotY, walls, cleanedSet, null);

  // Update stats
  $('stat-cleaned').textContent = `${cleanedTiles} / ${totalFloorTiles}`;
  $('stat-score').textContent = score.toLocaleString();
  updateIterBar(iterationsUsed, maxIterations);

  // Terminal states
  if (status !== 'RUNNING' && status !== 'PAUSED') {
    closeStream();
    setControlState('ended');
    setTimeout(() => showEndOverlay(data), 400);
  }
}

function updateIterBar(used, max) {
  const pct = max > 0 ? (used / max * 100) : 0;
  $('iter-fill').style.width = pct + '%';
  $('iter-bar-label').textContent = `${used} / ${max}`;
}

// ===== Controls state =====
function setControlState(state) {
  const run   = $('btn-run');
  const pause = $('btn-pause');
  const abort = $('btn-abort');

  run.disabled   = state !== 'paused';
  pause.disabled = state !== 'running';
  abort.disabled = state === 'ended';

  run.classList.toggle('active', state === 'paused');
  pause.classList.toggle('active', state === 'running');
}

// ===== End Overlay =====
function showEndOverlay(data) {
  const {status, score, cleanedTiles, totalFloorTiles, iterationsUsed, maxIterations} = data;

  const title = $('end-title');
  const overlay = $('end-overlay');

  if (status === 'FINISHED') {
    title.textContent = cleanedTiles >= totalFloorTiles ? '✓ All Clean!' : 'Time\'s Up!';
    title.className = 'win';
  } else if (status === 'FAILED') {
    title.textContent = '✗ Algorithm Error';
    title.className = 'fail';
  } else {
    title.textContent = 'Aborted';
    title.className = 'fail';
  }

  $('end-score').textContent = (score || 0).toLocaleString();
  $('end-stats').innerHTML =
    `Cleaned: <b>${cleanedTiles || 0} / ${totalFloorTiles || '?'}</b> tiles<br>
     Iterations: <b>${iterationsUsed || 0} / ${maxIterations || '?'}</b><br>
     Map: <b>${currentSession?.hash || '?'}</b>`;

  $('btn-save-lb').style.display = (status === 'FINISHED' || status === 'ABORTED') ? '' : 'none';
  overlay.classList.remove('hidden');
}

// ===== Save to Leaderboard =====
async function saveToLeaderboard() {
  if (!currentSession) return;
  try {
    await fetch('/api/leaderboard', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({sessionId: currentSession.sessionId})
    });
    toast('Saved to leaderboard!');
    $('btn-save-lb').disabled = true;
  } catch(e) {
    toast('Failed to save: ' + e.message);
  }
}

// ===== Retry =====
function retryGame() {
  $('end-overlay').classList.add('hidden');
  if (!currentSession) { switchTab('setup'); return; }
  // Pre-fill setup with same map
  $('hash-input').value = currentSession.hash;
  $('size-select').value = currentSession.size;
  // Keep algo, iterations — user can change
  switchTab('setup');
}

// ===== Leaderboard Status =====
async function checkLeaderboardStatus() {
  try {
    const status = await fetch('/api/leaderboard/status').then(r => r.json());
    if (!status.persistent) {
      const note = document.createElement('div');
      note.style.cssText = 'background:#1a2a1a;border:1px solid #2a4a2a;border-radius:6px;padding:10px 16px;margin-bottom:16px;font-size:.85rem;color:#7ea07e;';
      note.innerHTML = '&#9432; Leaderboard is in-memory (session only). To persist scores across restarts, set <code>leaderboard.path=/path/to/file</code> in application.properties or as a CLI argument.';
      $('leaderboard-pane').insertBefore(note, $('lb-content'));
    } else {
      const note = document.createElement('div');
      note.style.cssText = 'background:#1a2a3a;border:1px solid #2a4a5a;border-radius:6px;padding:10px 16px;margin-bottom:16px;font-size:.85rem;color:#7ea0b0;';
      note.innerHTML = `&#10003; Leaderboard persisted to <code>${escHtml(status.path)}</code>`;
      $('leaderboard-pane').insertBefore(note, $('lb-content'));
    }
  } catch(e) { /* ignore */ }
}

// ===== Leaderboard =====
async function refreshLeaderboard() {
  const content = $('lb-content');
  try {
    const entries = await fetch('/api/leaderboard').then(r => r.json());
    if (!entries.length) {
      content.innerHTML = '<p class="lb-empty">No entries yet. Play a game and save your score!</p>';
      return;
    }
    const rows = entries.map((e, i) => `
      <tr>
        <td class="rank">#${i+1}</td>
        <td>${e.avatar || ''} ${escHtml(e.username)}</td>
        <td class="score-val">${e.score.toLocaleString()}</td>
        <td>${escHtml(e.mapHash)}</td>
        <td>${e.mapSize}</td>
        <td>${e.iterationsUsed} / ${e.maxIterations}</td>
        <td>${new Date(e.playedAt).toLocaleDateString()}</td>
        <td>
          <div class="lb-actions">
            <button onclick="watchReplay('${e.id}','${e.mapHash}','${e.mapSize}',${e.maxIterations})">▶ Watch</button>
            <button onclick="retryFromLb('${e.mapHash}','${e.mapSize}',${e.maxIterations})">↺ Retry</button>
          </div>
        </td>
      </tr>`).join('');
    content.innerHTML = `
      <table class="lb-table">
        <thead><tr>
          <th>#</th><th>Player</th><th>Score</th><th>Map Hash</th><th>Size</th>
          <th>Iterations</th><th>Date</th><th>Actions</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>`;
  } catch(e) {
    content.innerHTML = '<p class="lb-empty">Leaderboard unavailable.</p>';
  }
}

window.retryFromLb = (hash, size, maxIter) => {
  $('hash-input').value = hash;
  $('size-select').value = size;
  // Snap iter slider to closest value
  const idx = ITER_VALUES.reduce((best, v, i) =>
    Math.abs(v - maxIter) < Math.abs(ITER_VALUES[best] - maxIter) ? i : best, 0);
  $('iter-slider').value = idx;
  updateIterDisplay();
  switchTab('setup');
};

window.watchReplay = async (entryId, hash, size, maxIter) => {
  try {
    const replay = await fetch(`/api/leaderboard/${entryId}/replay`).then(r => r.json());
    startReplay(replay);
  } catch(e) {
    toast('Failed to load replay');
  }
};

async function startReplay(replay) {
  // Generate map
  const session = await fetch('/api/game/start', {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({
      hash: replay.hash, size: replay.size,
      algoId: $('algo-select').value || 'randomAlgo',
      username: 'Replay', avatar: '📼',
      maxIterations: replay.maxIterations
    })
  }).then(r => r.json());

  currentSession = session;
  robot = { x: session.startX, y: session.startY, px: session.startX, py: session.startY, progress: 1 };
  cleanedSet = new Set([`${session.startX},${session.startY}`]);

  initCanvas(session);
  drawMap(session.walls, session.width, session.height);
  drawRobot(session.startX, session.startY, 1);

  $('stat-player').textContent = 'Replay';
  $('stat-avatar').textContent = '📼';
  $('stat-cleaned').textContent = `0 / ${session.floorTiles}`;
  $('stat-score').textContent = '0';
  updateIterBar(0, replay.maxIterations);

  switchTab('game');
  setControlState('ended');

  // Immediately abort the server-side session (we drive it ourselves)
  await fetch(`/api/game/${session.sessionId}/control`, {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({action:'ABORT'})
  });

  // Play back trace
  const DIRECTIONS = { UP:[0,-1], DOWN:[0,1], LEFT:[-1,0], RIGHT:[1,0] };
  const delay = ms => new Promise(r => setTimeout(r, ms));
  let rx = session.startX, ry = session.startY;
  let cleaned = 0;

  for (const dirName of replay.trace) {
    const [dx, dy] = DIRECTIONS[dirName] || [0,0];
    const nx = rx + dx, ny = ry + dy;
    if (!session.walls[ny]?.[nx]) { rx = nx; ry = ny; }
    cleanedSet.add(`${rx},${ry}`);
    cleaned++;

    // redraw
    for (const [cx,cy] of [[rx-dx,ry-dy],[rx,ry]]) {
      if (cx >= 0 && cy >= 0) drawTile(cx, cy, session.walls, cleanedSet);
    }
    drawRobot(rx, ry, 1);
    $('stat-cleaned').textContent = `${cleanedSet.size} / ${session.floorTiles}`;
    updateIterBar(cleaned, replay.maxIterations);

    await delay(150);
  }
}

// ===== Helpers =====
function escHtml(s) {
  return String(s).replace(/[&<>"']/g, c =>
    ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function toast(msg, duration = 2800) {
  const t = $('toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), duration);
}
