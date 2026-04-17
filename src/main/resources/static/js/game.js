const Game = (() => {
  const CELL  = 22;
  const SPEEDS = [800, 400, 200, 100, 50, 16]; // ms per frame
  let stompClient, sessionId, mapData, setupBody;
  let robotX, robotY, eventQueue = [], animating = false, speedIdx = 2;
  let score = 0, cleaned = 0, itersUsed = 0, itersAvail = 0;
  let cleanedSet = new Set();
  let finished = false, sessionTrace = [];

  function init(session, body) {
    sessionId  = session.sessionId;
    mapData    = session.map;
    setupBody  = body;
    robotX     = session.robotX;
    robotY     = session.robotY;
    itersAvail = session.iterationsAvailable;
    score = cleaned = itersUsed = 0;
    cleanedSet.clear();
    eventQueue = [];
    finished   = false;
    sessionTrace = [];

    const canvas = document.getElementById('game-canvas');
    canvas.width  = mapData.width  * CELL;
    canvas.height = mapData.height * CELL;

    document.getElementById('total-floor').textContent  = mapData.totalFloor;
    document.getElementById('iters-avail').textContent  = itersAvail;
    document.getElementById('finish-panel').classList.add('hidden');
    document.getElementById('btn-pause').disabled  = false;
    document.getElementById('btn-resume').disabled = true;

    updateSpeedLabel();
    document.getElementById('speed-range').value = speedIdx;
    document.getElementById('speed-range').oninput = () => {
      speedIdx = parseInt(document.getElementById('speed-range').value);
      updateSpeedLabel();
    };

    if (stompClient) stompClient.deactivate();
    stompClient = new StompJs.Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        stompClient.subscribe(`/topic/session/${sessionId}/events`, msg => {
          eventQueue.push(JSON.parse(msg.body));
          if (!animating) drainQueue();
        });
      }
    });
    stompClient.activate();

    drawMap();
    drawRobot(robotX, robotY);

    document.getElementById('btn-pause').onclick  = () => Api.pause(sessionId)
      .then(() => { document.getElementById('btn-pause').disabled  = true;
                    document.getElementById('btn-resume').disabled = false; });
    document.getElementById('btn-resume').onclick = () => Api.resume(sessionId)
      .then(() => { document.getElementById('btn-resume').disabled = true;
                    document.getElementById('btn-pause').disabled  = false; });
    document.getElementById('btn-stop').onclick   = () => Api.stop(sessionId);
    document.getElementById('btn-save-lb').onclick = saveLb;
  }

  function initReplay(session, body, trace) {
    init(session, body);
    setTimeout(() => {
      if (stompClient) stompClient.deactivate();
      const replaySet = new Set();
      eventQueue = trace.map(t => {
        replaySet.add(t.x + ',' + t.y);
        return {
          robotX: t.x, robotY: t.y, score: t.score,
          iteration: t.iteration, direction: t.direction,
          totalCleaned: replaySet.size, totalFloor: mapData.totalFloor,
          finished: t.iteration === trace.length,
          finishReason: t.iteration === trace.length ? 'COMPLETED' : null,
        };
      });
      drainQueue();
    }, 500);
  }

  function drainQueue() {
    if (eventQueue.length === 0) { animating = false; return; }
    animating = true;
    const ev  = eventQueue.shift();
    applyEvent(ev);
    setTimeout(drainQueue, SPEEDS[speedIdx]);
  }

  function applyEvent(ev) {
    sessionTrace.push(ev);
    robotX    = ev.robotX;
    robotY    = ev.robotY;
    score     = ev.score;
    cleaned   = ev.totalCleaned;
    itersUsed = ev.iteration;
    cleanedSet.add(ev.robotX + ',' + ev.robotY);

    drawMap();
    drawRobot(robotX, robotY);

    document.getElementById('score').textContent    = score;
    document.getElementById('cleaned').textContent  = cleaned;
    document.getElementById('iters-used').textContent = itersUsed;

    if (ev.finished) {
      finished = true;
      document.getElementById('finish-panel').classList.remove('hidden');
      document.getElementById('final-score').textContent = score;
      document.getElementById('btn-pause').disabled  = true;
      document.getElementById('btn-resume').disabled = true;
    }
  }

  function drawMap() {
    const canvas = document.getElementById('game-canvas');
    const ctx    = canvas.getContext('2d');
    ctx.fillStyle = '#0a0a16';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    for (const t of mapData.tiles) {
      const px = t.x * CELL, py = t.y * CELL;
      ctx.fillStyle = cleanedSet.has(t.x + ',' + t.y) ? '#e8f4e8' : '#2a2a4a';
      ctx.fillRect(px + 1, py + 1, CELL - 2, CELL - 2);

      ctx.strokeStyle = '#0a0a16';
      ctx.lineWidth   = 2;
      if (t.wallNorth) { ctx.beginPath(); ctx.moveTo(px, py);       ctx.lineTo(px+CELL, py);       ctx.stroke(); }
      if (t.wallSouth) { ctx.beginPath(); ctx.moveTo(px, py+CELL);  ctx.lineTo(px+CELL, py+CELL);  ctx.stroke(); }
      if (t.wallWest)  { ctx.beginPath(); ctx.moveTo(px, py);       ctx.lineTo(px, py+CELL);       ctx.stroke(); }
      if (t.wallEast)  { ctx.beginPath(); ctx.moveTo(px+CELL, py);  ctx.lineTo(px+CELL, py+CELL);  ctx.stroke(); }
    }
  }

  function drawRobot(x, y) {
    const canvas = document.getElementById('game-canvas');
    const ctx    = canvas.getContext('2d');
    ctx.font      = Math.floor(CELL * 0.8) + 'px serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(setupBody.avatar || '🤖', x * CELL + CELL/2, y * CELL + CELL/2);
  }

  function updateSpeedLabel() {
    const labels = ['×0.1','×0.25','×0.5','×1','×2','×max'];
    document.getElementById('speed-label').textContent = labels[speedIdx] || '';
  }

  async function saveLb() {
    const entry = {
      id:                  sessionId,
      username:            setupBody.username,
      avatar:              setupBody.avatar,
      mapHash:             setupBody.hash || sessionId,
      mapSize:             setupBody.size,
      algoName:            setupBody.algoName,
      iterationsUsed:      itersUsed,
      iterationsAvailable: itersAvail,
      score:               score,
      completedAt:         new Date().toISOString(),
      trace:               sessionTrace.map(e => ({
        iteration: e.iteration, direction: e.direction,
        x: e.robotX, y: e.robotY, score: e.score
      })),
    };
    await Api.saveToLb(entry);
    alert('Saved!');
  }

  return { init, initReplay };
})();
