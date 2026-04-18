const Game = (() => {
  const CELL   = 22;
  const SPEEDS = [800, 400, 200, 100, 50, 16];

  let state, setupBody, stompClient;
  let eventQueue = [], animating = false, speedIdx = 2;

  async function init(session, body) {
    setupBody  = body;
    state      = GameLogic.createState(session);
    eventQueue = [];
    animating  = false;

    const canvas   = document.getElementById('game-canvas');
    canvas.width   = state.map.width  * CELL;
    canvas.height  = state.map.height * CELL;

    document.getElementById('total-floor').textContent  = state.map.totalFloor;
    document.getElementById('iters-avail').textContent  = state.itersAvail;
    document.getElementById('finish-panel').classList.add('hidden');
    document.getElementById('btn-pause').disabled  = false;
    document.getElementById('btn-resume').disabled = true;

    document.getElementById('speed-range').value   = speedIdx;
    document.getElementById('speed-range').oninput = onSpeedChange;
    updateSpeedLabel();

    document.getElementById('btn-pause').onclick   = onPause;
    document.getElementById('btn-resume').onclick  = onResume;
    document.getElementById('btn-stop').onclick    = onStop;
    document.getElementById('btn-save-lb').onclick = saveLb;

    render(state);

    if (stompClient) stompClient.deactivate();
    return new Promise(function(resolve) {
      stompClient = new StompJs.Client({
        webSocketFactory: function() { return new SockJS('/ws'); },
        onConnect: function() {
          stompClient.subscribe('/topic/session/' + state.sessionId + '/events', function(msg) {
            eventQueue.push(JSON.parse(msg.body));
            if (!animating) drainQueue();
          });
          resolve();
        },
      });
      stompClient.activate();
    });
  }

  async function initReplay(session, body, trace) {
    await init(session, body);
    stompClient.deactivate();
    eventQueue = GameLogic.buildReplayEvents(trace, state.map.totalFloor);
    drainQueue();
  }

  function drainQueue() {
    if (eventQueue.length === 0) { animating = false; return; }
    animating = true;
    const ev = eventQueue.shift();
    state = GameLogic.applyEvent(state, ev);
    render(state);
    updateHud(state);
    if (state.finished) showFinish(state);
    setTimeout(drainQueue, SPEEDS[speedIdx]);
  }

  function render(s) {
    const canvas = document.getElementById('game-canvas');
    const ctx    = canvas.getContext('2d');
    ctx.fillStyle = '#0a0a16';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    for (const t of s.map.tiles) {
      const px = t.x * CELL, py = t.y * CELL;
      ctx.fillStyle = s.cleanedTiles.has(t.x + ',' + t.y) ? '#e8f4e8' : '#2a2a4a';
      ctx.fillRect(px + 1, py + 1, CELL - 2, CELL - 2);
      ctx.strokeStyle = '#0a0a16';
      ctx.lineWidth   = 2;
      if (t.wallNorth) { ctx.beginPath(); ctx.moveTo(px, py);      ctx.lineTo(px + CELL, py);      ctx.stroke(); }
      if (t.wallSouth) { ctx.beginPath(); ctx.moveTo(px, py+CELL); ctx.lineTo(px + CELL, py+CELL); ctx.stroke(); }
      if (t.wallWest)  { ctx.beginPath(); ctx.moveTo(px, py);      ctx.lineTo(px, py + CELL);      ctx.stroke(); }
      if (t.wallEast)  { ctx.beginPath(); ctx.moveTo(px+CELL, py); ctx.lineTo(px + CELL, py+CELL); ctx.stroke(); }
    }
    ctx.font         = Math.floor(CELL * 0.8) + 'px serif';
    ctx.textAlign    = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(setupBody.avatar || '🤖', s.robotX * CELL + CELL / 2, s.robotY * CELL + CELL / 2);
  }

  function updateHud(s) {
    document.getElementById('score').textContent       = s.score;
    document.getElementById('cleaned').textContent     = s.totalCleaned;
    document.getElementById('iters-used').textContent  = s.itersUsed;
  }

  function showFinish(s) {
    document.getElementById('finish-panel').classList.remove('hidden');
    document.getElementById('final-score').textContent = s.score;
    document.getElementById('btn-pause').disabled  = true;
    document.getElementById('btn-resume').disabled = true;
  }

  function updateSpeedLabel() {
    const labels = ['×0.1', '×0.25', '×0.5', '×1', '×2', '×max'];
    document.getElementById('speed-label').textContent = labels[speedIdx] || '';
  }

  function onSpeedChange() {
    speedIdx = parseInt(document.getElementById('speed-range').value);
    updateSpeedLabel();
  }

  function onPause() {
    Api.pause(state.sessionId).then(function() {
      document.getElementById('btn-pause').disabled  = true;
      document.getElementById('btn-resume').disabled = false;
    });
  }

  function onResume() {
    Api.resume(state.sessionId).then(function() {
      document.getElementById('btn-resume').disabled = true;
      document.getElementById('btn-pause').disabled  = false;
    });
  }

  function onStop() { Api.stop(state.sessionId); }

  async function saveLb() {
    const entry = GameLogic.buildLeaderboardEntry(state, setupBody);
    await Api.saveToLb(entry);
    const btn      = document.getElementById('btn-save-lb');
    const original = btn.textContent;
    btn.textContent = 'Saved!';
    btn.disabled    = true;
    setTimeout(function() { btn.textContent = original; btn.disabled = false; }, 2000);
  }

  return { init, initReplay };
})();
