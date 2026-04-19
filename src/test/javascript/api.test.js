'use strict';

// Provide a minimal fetch mock before requiring api.js
global.fetch = jest.fn();

const Api = require('../../main/resources/static/js/api.js');

beforeEach(() => {
  jest.clearAllMocks();
});

function mockOkJson(data) {
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(data),
  });
}

function mockErrorResponse(status) {
  global.fetch.mockResolvedValue({ ok: false, status });
}

describe('Api.algos', () => {
  test('GETs /api/algos and returns parsed JSON', async () => {
    mockOkJson(['Random', 'Always Left']);
    const result = await Api.algos();
    expect(global.fetch).toHaveBeenCalledWith('/api/algos');
    expect(result).toEqual(['Random', 'Always Left']);
  });

  test('throws when response is not ok', async () => {
    mockErrorResponse(500);
    await expect(Api.algos()).rejects.toThrow('500');
  });
});

describe('Api.createSession', () => {
  test('POSTs to /api/session with JSON body', async () => {
    const sessionResp = { sessionId: 'abc', status: 'SETUP' };
    mockOkJson(sessionResp);
    const body = { algoName: 'Random', username: 'Alice', avatar: '🤖', size: 'TINY', iterations: 100 };
    const result = await Api.createSession(body);
    expect(global.fetch).toHaveBeenCalledWith('/api/session', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }));
    expect(result).toEqual(sessionResp);
  });

  test('throws when server returns 400', async () => {
    mockErrorResponse(400);
    await expect(Api.createSession({})).rejects.toThrow('400');
  });
});

describe('Api.start', () => {
  test('POSTs to /api/session/{id}/start', async () => {
    mockOkJson({ status: 'RUNNING' });
    await Api.start('my-session');
    expect(global.fetch).toHaveBeenCalledWith('/api/session/my-session/start', { method: 'POST' });
  });
});

describe('Api.leaderboard', () => {
  test('GETs /api/leaderboard and returns entries', async () => {
    const entries = [{ id: 'e1', score: 100 }];
    mockOkJson(entries);
    const result = await Api.leaderboard();
    expect(global.fetch).toHaveBeenCalledWith('/api/leaderboard');
    expect(result).toEqual(entries);
  });
});

describe('Api.saveToLb', () => {
  test('POSTs entry to /api/leaderboard', async () => {
    const entry = { id: 'e1', score: 100 };
    mockOkJson(entry);
    await Api.saveToLb(entry);
    expect(global.fetch).toHaveBeenCalledWith('/api/leaderboard', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(entry),
    }));
  });
});
