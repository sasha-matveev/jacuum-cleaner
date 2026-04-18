const Leaderboard = (() => {
  let lbEntries = [];

  async function load() {
    const entries = await Api.leaderboard().catch(() => null);
    const tbody   = document.getElementById('lb-body');
    const empty   = document.getElementById('lb-empty');
    if (!entries || entries.length === 0) {
      tbody.innerHTML = '';
      empty.classList.remove('hidden');
      return;
    }
    empty.classList.add('hidden');
    const sorted = [...entries].sort((a, b) => b.score - a.score);
    lbEntries = sorted;

    tbody.innerHTML = '';
    sorted.forEach((e, i) => {
      const tr = document.createElement('tr');
      const cells = [
        String(i + 1),
        e.avatar,
        e.username,
        (e.mapHash || '').slice(0, 8),
        e.mapSize,
        e.algoName,
        `${e.iterationsUsed} / ${e.iterationsAvailable}`,
        String(e.score),
      ];
      cells.forEach((text, colIdx) => {
        const td = document.createElement('td');
        if (colIdx === 7) { // score — bold
          const strong = document.createElement('strong');
          strong.textContent = text;
          td.appendChild(strong);
        } else if (colIdx === 3) { // mapHash — code
          const code = document.createElement('code');
          code.textContent = text;
          td.appendChild(code);
        } else {
          td.textContent = text;
        }
        tr.appendChild(td);
      });

      // Action buttons
      const actionTd = document.createElement('td');
      const replayBtn = document.createElement('button');
      replayBtn.textContent = 'Replay';
      const retryBtn = document.createElement('button');
      retryBtn.textContent = 'Retry';
      replayBtn.addEventListener('click', () => replay(e));
      retryBtn.addEventListener('click', () => retry(e));
      actionTd.appendChild(replayBtn);
      actionTd.appendChild(retryBtn);
      tr.appendChild(actionTd);

      tbody.appendChild(tr);
    });
  }

  async function replay(e) {
    const session = await Api.createSession({
      hash: e.mapHash, size: e.mapSize,
      algoName: e.algoName, username: e.username,
      avatar: e.avatar, iterations: e.iterationsAvailable,
    });
    const fakeBody = { hash: e.mapHash, size: e.mapSize, algoName: e.algoName,
                       username: e.username, avatar: e.avatar };
    App.show('game');
    await Game.initReplay(session, fakeBody, e.trace);
  }

  async function retry(e) {
    document.getElementById('map-hash').value  = e.mapHash;
    document.getElementById('map-size').value  = e.mapSize;
    const ITER_VALUES = [250, 500, 1000, 2000, 5000];
    const iIdx = ITER_VALUES.indexOf(e.iterationsAvailable);
    if (iIdx >= 0) document.getElementById('iterations-range').value = iIdx;
    App.show('setup');
  }

  return { load, replay, retry };
})();
