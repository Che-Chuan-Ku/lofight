// Runs tag handlers (create, settle). Plausibility checks mirror api.yml settleRun except the server-elapsed-time
// check, which a dev fast-forward (window.__game) would always fail.
import { route, ok, fail, authenticate, blank } from './util.js';
import { getDb, persist, nextId, now, addLedger, TIER_RULES, BOARD_VERSION, SILVER_UNLOCK_BOSS_SEQ } from '../db.js';

const TIERS = ['BRONZE', 'SILVER'];
const BUFFS = ['ATTACK_SPEED', 'ATTACK', 'CANNON', 'LASER'];

function settlementOf(run) {
  return { runId: run.runId, runState: run.runState, coinsAwarded: run.coinsAwarded || 0, walletCoins: getDb().players[run.playerId].coins,
    isSilverNewlyUnlocked: !!run.silverNewlyUnlocked, bossRewards: run.bossRewards || [] };
}

export const runHandlers = [
  route('createRun', ({ request, body }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    if (!body || blank(body.tier)) return fail('400001');
    if (!TIERS.includes(body.tier)) return fail('400012');
    const buffs = body.buffItems || [];
    if (!Array.isArray(buffs) || buffs.some(b => !BUFFS.includes(b))) return fail('400012');
    if (buffs.length > 1) return fail('400007');
    if (body.tier === 'BRONZE' && buffs.length) return fail('400013');
    const p = auth.player;
    if (body.tier === 'SILVER' && !p.isSilverUnlocked) return fail('gam001');
    if (buffs.length && (p.inv[buffs[0]] ?? 0) <= 0) return fail('gam002');
    if (buffs.length) p.inv[buffs[0]] = (p.inv[buffs[0]] ?? 0) - 1;
    const run = { runId: nextId(), playerId: p.playerId, tier: body.tier, equippedBuff: buffs[0] || null, boardVersion: BOARD_VERSION, startedAt: now(), runState: 'OPEN' };
    getDb().runs[run.runId] = run; persist();
    const { playerId, ...detail } = run;
    return ok(detail, 201);
  }),

  route('settleRun', ({ request, params, body }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    if (!body || typeof body.gameSeconds !== 'number' || typeof body.isVictory !== 'boolean' || !Array.isArray(body.bossKills)) return fail('400001');
    if (!body.isVictory && blank(body.deathCause)) return fail('400001');
    const db = getDb();
    const run = db.runs[params.runId];
    if (!run) return fail('404001');
    if (run.playerId !== auth.player.playerId) return fail('403002');
    if (run.runState === 'SETTLED') return ok(settlementOf(run));     // idempotent replay
    if (run.runState === 'CHECK_FAILED') return fail('gam003');
    if (run.runState === 'ABANDONED') return fail('gam004');
    if (Date.now() - Date.parse(run.startedAt) > 24 * 3600e3) { run.runState = 'ABANDONED'; persist(); return fail('gam004'); }
    const rule = TIER_RULES[run.tier];
    const seen = new Set();
    let plausible = true;
    for (const k of body.bossKills) {
      if (k.bossSeq < 1 || k.bossSeq > rule.bossCount || seen.has(k.bossSeq) || k.killedAtSecond < rule.spawn[k.bossSeq - 1]) plausible = false;
      seen.add(k.bossSeq);
    }
    // the last boss cannot fall before the one before it when both are listed (silver 7/6: D39 X15=a, INF-X21; bronze 9/8: D42)
    if (rule.lastNotBefore) {
      const [late, early] = rule.lastNotBefore, kl = body.bossKills.find(k => k.bossSeq === late), ke = body.bossKills.find(k => k.bossSeq === early);
      if (kl && ke && kl.killedAtSecond < ke.killedAtSecond) plausible = false;
    }
    // victory threshold is inclusive since D-09 (gameSeconds >= threshold passes)
    if (body.isVictory && (body.gameSeconds < rule.victoryMin || seen.size < rule.bossCount)) plausible = false;
    if (!plausible) { run.runState = 'CHECK_FAILED'; persist(); return fail('gam003'); }
    const p = db.players[run.playerId];
    const kills = [...body.bossKills].sort((a, b) => a.bossSeq - b.bossSeq);
    run.bossRewards = kills.map(k => ({ bossSeq: k.bossSeq, coins: rule.coins[k.bossSeq - 1] }));
    for (const r of run.bossRewards) addLedger(p.playerId, { ledgerSource: 'BOSS_REWARD', coinDelta: r.coins, bossSeq: r.bossSeq, runId: run.runId });
    run.coinsAwarded = run.bossRewards.reduce((s, r) => s + r.coins, 0);
    if (run.tier === 'BRONZE') {
      p.bronzeBest = Math.max(p.bronzeBest, kills.length);
      if (!p.isSilverUnlocked && seen.has(SILVER_UNLOCK_BOSS_SEQ)) { p.isSilverUnlocked = true; run.silverNewlyUnlocked = true; }
    }
    db.board.push({ boardVersion: run.boardVersion, tier: run.tier, playerId: p.playerId, isVictory: body.isVictory, gameSeconds: body.gameSeconds, settledAt: now(), equippedBuff: run.equippedBuff });
    run.runState = 'SETTLED'; persist();
    return ok(settlementOf(run));
  })
];
