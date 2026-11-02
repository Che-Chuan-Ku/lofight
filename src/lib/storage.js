// localStorage wrapper: private mode / blocked storage must never break the game.
export function readJson(key, fallback) {
  try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; } catch (_) { return fallback; }
}
export function writeJson(key, value) {
  try { if (value === null || value === undefined) localStorage.removeItem(key); else localStorage.setItem(key, JSON.stringify(value)); } catch (_) { /* ignore */ }
}
