const Api = (() => {
  const json = (r) => { if (!r.ok) throw new Error(r.status); return r.json(); };

  return {
    algos:         () => fetch('/api/algos').then(json),
    avatars:       () => fetch('/api/avatars').then(json),
    createSession: (body) => fetch('/api/session', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify(body)
    }).then(json),
    start:   (id) => fetch(`/api/session/${id}/start`,  {method:'POST'}).then(json),
    pause:   (id) => fetch(`/api/session/${id}/pause`,  {method:'POST'}).then(json),
    resume:  (id) => fetch(`/api/session/${id}/resume`, {method:'POST'}).then(json),
    stop:    (id) => fetch(`/api/session/${id}/stop`,   {method:'POST'}).then(json),
    leaderboard:   () => fetch('/api/leaderboard').then(json),
    saveToLb: (entry) => fetch('/api/leaderboard', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify(entry)
    }).then(json),
  };
})();
