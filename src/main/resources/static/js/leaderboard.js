const Leaderboard = (() => {
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
    tbody.innerHTML = sorted.map((e, i) => `
      <tr>
        <td>${i + 1}</td>
        <td>${e.avatar}</td>
        <td>${e.username}</td>
        <td><code>${(e.mapHash || '').slice(0, 8)}</code></td>
        <td>${e.mapSize}</td>
        <td>${e.algoName}</td>
        <td>${e.iterationsUsed} / ${e.iterationsAvailable}</td>
        <td><strong>${e.score}</strong></td>
        <td>
          <button onclick="Leaderboard.replay(${i})">Replay</button>
          <button onclick="Leaderboard.retry(${i})">Retry</button>
        </td>
      </tr>`).join('');
    window._lbEntries = sorted;
  }

  async function replay(idx) {
    const e = window._lbEntries[idx];
    const session = await Api.createSession({
      hash: e.mapHash, size: e.mapSize,
      algoName: e.algoName, username: e.username,
      avatar: e.avatar, iterations: e.iterationsAvailable,
    });
    const fakeBody = { hash: e.mapHash, size: e.mapSize, algoName: e.algoName,
                       username: e.username, avatar: e.avatar };
    Game.initReplay(session, fakeBody, e.trace);
    App.show('game');
  }

  async function retry(idx) {
    const e       = window._lbEntries[idx];
    document.getElementById('map-hash').value  = e.mapHash;
    document.getElementById('map-size').value  = e.mapSize;
    const ITER_VALUES = [250, 500, 1000, 2000, 5000];
    const iIdx = ITER_VALUES.indexOf(e.iterationsAvailable);
    if (iIdx >= 0) document.getElementById('iterations-range').value = iIdx;
    App.show('setup');
  }

  return { load, replay, retry };
})();
