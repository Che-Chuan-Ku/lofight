// Leaderboard client. Scores are keyed by BOARD_VERSION (major.minor), so only a minor release clears the board.
import { LEADERBOARD_URL, BOARD_VERSION, NAME_MAX } from './config.js';

export const leaderboardEnabled = () => !!LEADERBOARD_URL;
export const cleanName = s => String(s || '').replace(/[\u0000-\u001f]/g, '').replace(/^[=+\-@]+/, '').trim().slice(0, NAME_MAX);

async function call(url, init) {
  const ctl = new AbortController(), t = setTimeout(() => ctl.abort(), 10000);
  try {
    const r = await fetch(url, { ...init, signal: ctl.signal, cache: 'no-store' });
    if (!r.ok) throw new Error('HTTP ' + r.status);
    const j = await r.json();
    if (!j || !j.ok) throw new Error(j && j.error ? j.error : 'bad response');
    return Array.isArray(j.top) ? j.top : [];
  } finally { clearTimeout(t); }
}

export function fetchTop() {
  return call(LEADERBOARD_URL + (LEADERBOARD_URL.includes('?') ? '&' : '?') + 'version=' + encodeURIComponent(BOARD_VERSION) + '&_=' + Date.now());
}

// text/plain keeps the request "simple" (no CORS preflight), which is what Apps Script web apps support.
export function submitScore({ name, seconds, win, secret }) {
  return call(LEADERBOARD_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain;charset=utf-8' },
    body: JSON.stringify({ name: cleanName(name), seconds: Math.round(seconds), win: !!win, secret: !!secret, version: BOARD_VERSION })
  });
}

// first visit: a random default such as 砲手482; the player can edit it before starting
const DEFAULT_TITLES = ['砲手', '勇者', '守城人', '指揮官', '老兵', '新兵'];
export function defaultName() { return DEFAULT_TITLES[(Math.random() * DEFAULT_TITLES.length) | 0] + (100 + ((Math.random() * 900) | 0)); }

const STORE_KEY = 'lofight.name';
export function loadName() { try { return cleanName(localStorage.getItem(STORE_KEY) || ''); } catch (_) { return ''; } }
export function saveName(n) { try { localStorage.setItem(STORE_KEY, cleanName(n)); } catch (_) { /* private mode */ } }
