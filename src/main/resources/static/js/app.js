const App = (() => {
  const screens = ['setup', 'game', 'leaderboard'];

  function show(name) {
    screens.forEach(s => {
      document.getElementById('screen-' + s).classList.toggle('hidden', s !== name);
    });
    if (name === 'leaderboard') Leaderboard.load();
  }

  document.addEventListener('DOMContentLoaded', () => {
    Setup.init();
    show('setup');
  });

  return { show };
})();
