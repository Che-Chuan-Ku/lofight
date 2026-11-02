// In-memory mock backend state for MSW (dev only). Seed values come from specs/features/** Background tables
// (nicknames 勇者248 / 丙 / 丁 / 戊 / 阿明, Google tokens g-ok-1 / g-ok-2 / g-bad, ledger rows, leaderboard rows).
// State is mirrored to localStorage so a page reload keeps the mock world.
import { readJson, writeJson } from '../lib/storage.js';

const KEY = 'mock.db';
export const BOARD_VERSION = '19.0';                       // 查詢排行榜.feature: 後端設定的目前版本前兩碼
export const SHOP_PRICES = { ATTACK_SPEED: 200, CANNON: 400, LASER: 600 };   // 瀏覽商店.feature / GAP-02=A
export const BUFF_ORDER = ['ATTACK_SPEED', 'CANNON', 'LASER'];
export const TIER_RULES = {
  BRONZE: { bossCount: 8, victoryAbove: 98, spawn: [14, 28, 42, 56, 70, 84, 98, 98], coins: [1, 2, 4, 8, 16, 24, 48, 96] },
  SILVER: { bossCount: 6, victoryAbove: 84, spawn: [14, 28, 42, 56, 70, 84], coins: [4, 8, 16, 32, 64, 128] }
};
export const SILVER_UNLOCK_BOSS_SEQ = 8;
export const LEDGER_PAGE_SIZE = 20;
export const BANNED_WORDS = ['管理員', 'GM'];             // backend keeps the real list in config; prototype simulates these two
export const DEFAULT_GUEST_NICKNAME = '勇者248';           // 建立guest身分.feature: 系統預設暱稱產生器下一個值
export const DEFAULT_MEMBER_NICKNAME = '老兵512';          // Google登入.feature: a new account takes the system default, not the guest's nickname
// Google id_token -> verification result (查詢合併預覽.feature Background)
export const GOOGLE_TOKENS = { 'g-ok-1': 'g-sub-1', 'g-ok-2': 'g-sub-2' };

const iso = ms => new Date(ms).toISOString().replace('Z', '+00:00');

function player(id, nickname, kind, extra) {
  return { playerId: id, nickname, playerKind: kind, deviceId: null, isSilverUnlocked: false, isVoided: false,
    coins: 0, inv: { ATTACK_SPEED: 0, CANNON: 0, LASER: 0 }, bronzeBest: 0, googleSub: null, ...extra };
}

export function seed() {
  const players = {
    1001: player('1001', '勇者248', 'GUEST', { deviceId: 'dev-a', isSilverUnlocked: true, bronzeBest: 8 }),
    1002: player('1002', '丙', 'MEMBER', { isSilverUnlocked: true, bronzeBest: 8 }),
    1003: player('1003', '丁', 'GUEST', { deviceId: 'dev-d' }),
    1004: player('1004', '戊', 'GUEST', { deviceId: 'dev-e' }),
    1005: player('1005', '阿明', 'MEMBER', { coins: 300, googleSub: 'g-sub-1' })
  };
  const board = [
    { boardVersion: '19.0', tier: 'BRONZE', playerId: '1001', isVictory: true, gameSeconds: 120, settledAt: '2026-11-01T10:02:00+00:00', equippedBuff: null },
    { boardVersion: '19.0', tier: 'BRONZE', playerId: '1002', isVictory: true, gameSeconds: 110, settledAt: '2026-11-01T10:05:00+00:00', equippedBuff: null },
    { boardVersion: '19.0', tier: 'BRONZE', playerId: '1003', isVictory: false, gameSeconds: 95, settledAt: '2026-11-01T10:00:00+00:00', equippedBuff: null },
    { boardVersion: '19.0', tier: 'BRONZE', playerId: '1004', isVictory: false, gameSeconds: 90, settledAt: '2026-11-01T10:00:00+00:00', equippedBuff: null },
    { boardVersion: '18.6', tier: 'BRONZE', playerId: '1001', isVictory: true, gameSeconds: 100, settledAt: '2026-10-01T10:00:00+00:00', equippedBuff: null },
    { boardVersion: '19.0', tier: 'SILVER', playerId: '1001', isVictory: true, gameSeconds: 100, settledAt: '2026-11-01T11:05:00+00:00', equippedBuff: 'LASER' },
    { boardVersion: '19.0', tier: 'SILVER', playerId: '1002', isVictory: false, gameSeconds: 70, settledAt: '2026-11-01T11:05:00+00:00', equippedBuff: null }
  ];
  return { seq: 2000, players, tokens: {}, refresh: {}, ledger: [], runs: {}, board, fail: {} };
}

let db = readJson(KEY, null) || seed();
export const getDb = () => db;
export function persist() { writeJson(KEY, db); }
export function resetDb(next) { db = next || seed(); persist(); return db; }
export const nextId = () => String(++db.seq);
export const now = () => iso(Date.now());

export function issueToken(playerId, withRefresh) {
  const accessToken = 'mock-at-' + nextId();
  db.tokens[accessToken] = { playerId, expired: false };
  let refreshToken = null;
  if (withRefresh) { refreshToken = 'mock-rt-' + nextId(); db.refresh[refreshToken] = playerId; }
  persist();
  return {
    playerId, playerKind: db.players[playerId].playerKind, accessToken, accessTokenExpiresIn: 900,
    hasRefreshToken: !!withRefresh, refreshToken,
    refreshTokenExpiresIn: withRefresh ? 2592000 : null,
    refreshTokenExpiresAt: withRefresh ? iso(Date.now() + 2592000e3) : null
  };
}

export function addLedger(playerId, entry) {
  const p = db.players[playerId];
  p.coins += entry.coinDelta;
  db.ledger.push({ ledgerEntryId: nextId(), playerId, bossSeq: null, buffItem: null, runId: null, occurredAt: now(), ...entry, coinsAfter: p.coins });
}

/** Leaderboard ranking: wins first, faster wins, longer losses, earlier settlement; best entry per player. */
export function rankBoard(boardVersion, tier) {
  const better = (a, b) => (a.isVictory !== b.isVictory ? (a.isVictory ? -1 : 1)
    : a.isVictory ? a.gameSeconds - b.gameSeconds : b.gameSeconds - a.gameSeconds) || a.settledAt.localeCompare(b.settledAt);
  const best = new Map();
  for (const e of db.board) {
    if (e.boardVersion !== boardVersion || e.tier !== tier) continue;
    const cur = best.get(e.playerId);
    if (!cur || better(e, cur) < 0) best.set(e.playerId, e);
  }
  return [...best.values()].sort(better).map((e, i) => ({
    rank: i + 1, playerId: e.playerId, nickname: db.players[e.playerId] ? db.players[e.playerId].nickname : '',
    isVictory: e.isVictory, gameSeconds: e.gameSeconds, equippedBuff: tier === 'SILVER' ? e.equippedBuff : null, settledAt: e.settledAt
  }));
}

// Test controls (dev mock only): window.__mockDb, see mocks/browser.js.
export const controls = {
  reset() { resetDb(); },
  state() { return db; },
  /** The next `times` calls of `operationId` fail: 'network', 'schema' (200 with a malformed payload) or an error code string. */
  failNext(operationId, how, times = 1) { db.fail[operationId] = { how, left: times }; persist(); },
  clearFail(operationId) { delete db.fail[operationId]; persist(); },
  expireTokens() { for (const t of Object.values(db.tokens)) t.expired = true; persist(); },
  /** Refresh tokens past their 30 days: the next refresh answers 401002. */
  expireRefreshTokens() { db.expiredRefresh = Object.keys(db.refresh); persist(); },
  /** Patch the player behind the current device's session (coins, isSilverUnlocked, inv, bronzeBest...). */
  patchPlayer(playerId, patch) { Object.assign(db.players[playerId], patch); persist(); },
  seedLedger(playerId, n) {
    for (let i = 0; i < n; i++) addLedger(playerId, i % 9 === 4 ? { ledgerSource: 'MERGE_IN', coinDelta: 50 } : { ledgerSource: 'BOSS_REWARD', coinDelta: TIER_RULES.BRONZE.coins[i % 4], bossSeq: 1 + (i % 4), runId: 'r-' + Math.floor(i / 4) });
    persist();
  },
  clearBoard() { db.board = []; persist(); }
};
