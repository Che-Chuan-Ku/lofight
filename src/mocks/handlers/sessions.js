// Sessions tag handlers (issue-guest, login-google, refresh, logout, merge-previews).
import { route, ok, fail, authenticate, blank } from './util.js';
import { getDb, persist, nextId, issueToken, addLedger, GOOGLE_TOKENS, DEFAULT_GUEST_NICKNAME, DEFAULT_MEMBER_NICKNAME, BUFF_ORDER } from '../db.js';

function guestFor(deviceId) {
  const db = getDb();
  return Object.values(db.players).find(p => p.deviceId === deviceId);
}

function mergePlan(guest, sub) {
  const db = getDb();
  const target = Object.values(db.players).find(p => p.googleSub === sub && !p.isVoided) || null;
  const best = p => (p ? db.board.filter(e => e.playerId === p.playerId) : []);
  const scores = list => list.map(e => ({ boardVersion: e.boardVersion, tier: e.tier, isVictory: e.isVictory, gameSeconds: e.gameSeconds }));
  const inv = (p, q) => BUFF_ORDER.map(b => ({ buffItem: b, quantity: (p ? p.inv[b] ?? 0 : 0) + (q ? q.inv[b] ?? 0 : 0) }));
  return {
    target,
    preview: {
      targetKind: target ? 'EXISTING' : 'NEW',
      guestCoins: guest.coins, guestIsSilverUnlocked: guest.isSilverUnlocked,
      guestBuffInventories: inv(guest, null), guestBestScores: scores(best(guest)),
      mergedCoins: guest.coins + (target ? target.coins : 0),
      mergedIsSilverUnlocked: guest.isSilverUnlocked || !!(target && target.isSilverUnlocked),
      mergedBuffInventories: inv(guest, target), mergedBestScores: scores([...best(guest), ...best(target)])
    }
  };
}

export const sessionHandlers = [
  route('issueGuestSession', ({ body }) => {
    if (!body || blank(body.deviceId)) return fail('400001');
    const db = getDb();
    let p = guestFor(body.deviceId);
    if (p && p.isVoided) return fail('acc003');
    if (!p) {
      const id = nextId();
      db.players[id] = { playerId: id, nickname: DEFAULT_GUEST_NICKNAME, playerKind: 'GUEST', deviceId: body.deviceId, isSilverUnlocked: false, isVoided: false,
        coins: 0, inv: { ATTACK_SPEED: 0, ATTACK: 0, CANNON: 0, LASER: 0 }, bronzeBest: 0, googleSub: null };
      p = db.players[id]; persist();
    }
    return ok(issueToken(p.playerId, false));
  }),

  route('loginGoogle', ({ request, body }) => {
    if (!body || blank(body.idToken)) return fail('400001');
    const sub = GOOGLE_TOKENS[body.idToken];
    if (!sub) return fail('acc001');
    const auth = authenticate(request, { optional: true });
    if (auth.error) return auth.error;
    const db = getDb();
    const guest = auth.player && auth.player.playerKind === 'GUEST' ? auth.player : null;
    let target = Object.values(db.players).find(p => p.googleSub === sub && !p.isVoided);
    if (!target) {
      const id = nextId();
      db.players[id] = { playerId: id, nickname: DEFAULT_MEMBER_NICKNAME, playerKind: 'MEMBER', deviceId: null, isSilverUnlocked: false, isVoided: false,
        coins: 0, inv: { ATTACK_SPEED: 0, ATTACK: 0, CANNON: 0, LASER: 0 }, bronzeBest: 0, googleSub: sub };
      target = db.players[id];
    }
    if (guest) {
      // merge: coins and buffs add up, silver ORs, boards keep the best (rankBoard dedupes), guest voided
      if (guest.coins > 0) addLedger(target.playerId, { ledgerSource: 'MERGE_IN', coinDelta: guest.coins });
      for (const b of BUFF_ORDER) target.inv[b] = (target.inv[b] ?? 0) + (guest.inv[b] ?? 0);
      target.isSilverUnlocked = target.isSilverUnlocked || guest.isSilverUnlocked;
      target.bronzeBest = Math.max(target.bronzeBest, guest.bronzeBest);
      for (const e of db.board) if (e.playerId === guest.playerId) e.playerId = target.playerId;
      guest.isVoided = true; guest.coins = 0;
    }
    persist();
    return ok(issueToken(target.playerId, true));
  }),

  route('refreshSession', ({ body }) => {
    if (!body || blank(body.refreshToken)) return fail('400001');
    const db = getDb();
    if ((db.expiredRefresh || []).includes(body.refreshToken)) return fail('401002');
    const pid = db.refresh[body.refreshToken];
    if (!pid) return fail('401001');
    delete db.refresh[body.refreshToken];      // rotation: the old refresh token is revoked
    return ok(issueToken(pid, true));
  }),

  route('logoutSession', ({ request, body }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    if (!body || blank(body.refreshToken)) return fail('400001');
    delete getDb().refresh[body.refreshToken]; persist();
    return ok({});
  }),

  route('getMergePreview', ({ request, url }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const provider = url.searchParams.get('loginProvider');
    const token = request.headers.get('X-Provider-Token');
    if (blank(provider) || blank(token)) return fail('400001');
    if (provider !== 'GOOGLE' && provider !== 'APPLE') return fail('400012');
    if (provider === 'APPLE') return fail('acc004');
    if (auth.player.playerKind !== 'GUEST') return fail('403001');
    const sub = GOOGLE_TOKENS[token];
    if (!sub) return fail('acc001');
    return ok(mergePlan(auth.player, sub).preview);
  })
];
