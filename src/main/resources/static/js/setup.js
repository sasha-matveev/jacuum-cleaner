const Setup = (() => {
  const ITER_VALUES = [250, 500, 1000, 2000, 5000];
  const PREFS_KEY   = 'jacuum_prefs';
  let selectedAvatar = '🤖';
  let pendingAlgo = null;

  function savePrefs() {
    localStorage.setItem(PREFS_KEY, JSON.stringify({
      hash:     document.getElementById('map-hash').value,
      size:     document.getElementById('map-size').value,
      username: document.getElementById('username').value,
      avatar:   selectedAvatar,
      algo:     document.getElementById('algo-select').value,
      iters:    document.getElementById('iterations-range').value,
    }));
  }

  function loadPrefs() {
    try {
      const p = JSON.parse(localStorage.getItem(PREFS_KEY) || '{}');
      if (p.hash)     document.getElementById('map-hash').value = p.hash;
      if (p.size)     document.getElementById('map-size').value = p.size;
      if (p.username) document.getElementById('username').value = p.username;
      if (p.iters !== undefined) document.getElementById('iterations-range').value = p.iters;
      if (p.avatar)   selectedAvatar = p.avatar;
      if (p.algo)     pendingAlgo = p.algo;
    } catch (err) {
      console.warn('Failed to load preferences:', err);
    }
    updateIterLabel();
  }

  function updateIterLabel() {
    const idx = parseInt(document.getElementById('iterations-range').value);
    document.getElementById('iterations-label').textContent = ITER_VALUES[idx].toLocaleString();
  }

  async function init() {
    document.getElementById('iterations-range').addEventListener('input', updateIterLabel);

    // Load avatars
    const avatars = await Api.avatars();
    const picker  = document.getElementById('avatar-picker');
    picker.innerHTML = '';
    avatars.forEach(av => {
      const span = document.createElement('span');
      span.className = 'avatar-opt' + (av === selectedAvatar ? ' selected' : '');
      span.textContent = av;
      span.onclick = () => {
        selectedAvatar = av;
        document.querySelectorAll('.avatar-opt').forEach(el => el.classList.remove('selected'));
        span.classList.add('selected');
      };
      picker.appendChild(span);
    });

    // Load algos
    const algos  = await Api.algos();
    const sel    = document.getElementById('algo-select');
    sel.innerHTML = algos.map(a => `<option value="${a}">${a}</option>`).join('');
    if (pendingAlgo) sel.value = pendingAlgo;

    // Random username placeholder
    const HEROES = ['Luke Skywalker','Leia Organa','Han Solo','Rey','Din Djarin','Obi-Wan Kenobi'];
    if (!document.getElementById('username').value)
      document.getElementById('username').placeholder = HEROES[Math.floor(Math.random() * HEROES.length)];

    loadPrefs();

    document.getElementById('btn-start-game').addEventListener('click', async () => {
      savePrefs();
      const idx  = parseInt(document.getElementById('iterations-range').value);
      const body = {
        hash:      document.getElementById('map-hash').value || null,
        size:      document.getElementById('map-size').value,
        algoName:  document.getElementById('algo-select').value,
        username:  document.getElementById('username').value ||
                   document.getElementById('username').placeholder,
        avatar:    selectedAvatar,
        iterations: ITER_VALUES[idx],
      };
      try {
        const session = await Api.createSession(body);
        Game.init(session, body);
        App.show('game');
        await Api.start(session.sessionId);
      } catch (e) {
        alert('Failed to start: ' + e.message);
      }
    });
  }

  return { init };
})();
