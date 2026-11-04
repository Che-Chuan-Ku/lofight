// Simulation: state, spawning, movement, combat, gates, bosses, rest/choice pauses.
// Copied from the single-file v17 game; UI changes are reported through `onChange` instead of touching the DOM.
import { W, GATE_Y, GATE_H, PATH_HW, GATE_MULT, SHOT_SPEED, MAXB, MAXR, FIRE_INTERVAL, FIRE_MIN, FIRE_MIN_BUFF, BOSS_PLAN, EXTRA_PLAN, STORM, H, LASER, GATE_RESPAWN, GATE_X0, GATE_GAP, GATE_MIN, GATE_STEP, BEAM, BEAM_HIT_R, BOSS7 } from './constants.js';
import { PATH_LEN, pathPos } from './path.js';
import { ABILITIES, CHOICES, cardFor, spreadMaxed } from './abilities.js';

// Keeps the elements passing `keep`, in order, in the same array (no per-frame allocation).
function compact(a, keep) {
  let j = 0;
  for (let i = 0; i < a.length; i++) { const x = a[i]; if (keep(x)) a[j++] = x; }
  a.length = j;
}
// 1px arc-length histogram for the blue cull (reused every frame)
const CULL_BINS = new Uint32Array(Math.ceil(PATH_LEN) + 2);

// ---- EXTRA boss 7 full-screen lightning order (D39, INF-X14 dodge guarantee) ----
// `order` lists the 5 firing columns (every column but the safe one `s`) in firing order; `p` is the turret column
// when the round starts. Valid when (1) the columns from p towards s (p included, s excluded) fire in strictly
// increasing order, and (2) every column on the far side of p (away from s) fires before p. p === s: anything goes.
export function sweepOrderOk(order, p, s) {
  if (p === s) return true;
  const rank = []; order.forEach((c, i) => { rank[c] = i; });
  const dir = s > p ? 1 : -1;
  for (let c = p; c + dir !== s; c += dir) if (!(rank[c] < rank[c + dir])) return false;
  for (let c = p - dir; c >= 0 && c < BOSS7.sweep.strips; c -= dir) if (!(rank[c] < rank[p])) return false;
  return true;
}
// Uniformly random permutation of the non-safe columns, redrawn until it passes sweepOrderOk (worst case p and s at
// opposite edges: 1 valid order out of 120). The fallback (never reached in practice) is the sweep from p towards s.
export function sweepOrder(p, s, rand = Math.random) {
  const cols = [];
  for (let c = 0; c < BOSS7.sweep.strips; c++) if (c !== s) cols.push(c);
  for (let tries = 0; tries < 5000; tries++) {
    const o = cols.slice();
    for (let i = o.length - 1; i > 0; i--) { const j = (rand() * (i + 1)) | 0; const t = o[i]; o[i] = o[j]; o[j] = t; }
    if (sweepOrderOk(o, p, s)) return o;
  }
  return cols.slice().sort((a, b) => Math.abs(a - s) === Math.abs(b - s) ? a - b : Math.abs(b - s) - Math.abs(a - s));
}
// column (0 = leftmost) holding x
export const stripOf = x => Math.max(0, Math.min(BOSS7.sweep.strips - 1, Math.floor(x / (W / BOSS7.sweep.strips))));

export function createGame(onChange) {
  const notify = () => { if (onChange) onChange(); };

  function openChoice(force) {
    // multiplying picks drop out once soldiers per shot is maxed
    const keys = Object.keys(CHOICES).filter(k => !(CHOICES[k].mult && spreadMaxed(S)));
    let a = keys[(Math.random() * keys.length) | 0];
    let b = a; while (b === a) b = keys[(Math.random() * keys.length) | 0];
    if (force) [a, b] = force;
    S.choice = [a, b]; S.choosing = true;
    S.choiceCards = [a, b].map(k => cardFor(k, S));
    notify();
  }
  function pickChoice(i) {
    if (!S.choosing) return;
    const k = S.choice[i], c = CHOICES[k];
    S.choosing = false; S.choiceCards = null; notify();
    c.apply(S, api); addPop(c.mult && spreadMaxed(S) ? 'MAX' : c.title, W / 2, 560, c.good ? '#ffe066' : '#c0c0c0');
  }
  const ABILITY_POOL = Object.keys(ABILITIES);
  // random pick that avoids this gate's previous ability and the other gate's current one;
  // multiplying rewards are never picked once soldiers per shot is maxed
  function pickAbility(g, others) {
    const avoid = new Set([g.ability, ...others.map(o => o.ability)]);
    const base = ABILITY_POOL.filter(a => !(ABILITIES[a].mult && spreadMaxed(S)));
    let pool = base.filter(a => !avoid.has(a));
    if (!pool.length) pool = base;
    return pool[(Math.random() * pool.length) | 0];
  }
  const otherHpGates = (gs, g) => gs.filter(o => o !== g && o.kind === 'hp');
  function gateHp(g, gen) { return Math.round(g.baseHp * Math.pow(g.growth, gen)); }
  function makeGates() {
    const gs = [
      { x0: 16, x1: 120, kind: 'hp', baseHp: 10, growth: 3, gen: 0, dead: false, respawnT: 0 },
      { x0: 134, x1: 256, kind: 'mult', mult: GATE_MULT },
      { x0: 270, x1: 374, kind: 'hp', baseHp: 286, growth: 2, gen: 0, dead: false, respawnT: 0 }
    ];
    for (const g of gs) if (g.kind === 'hp') { g.hp = g.max = gateHp(g, 0); g.ability = pickAbility(g, otherHpGates(gs, g)); }
    return gs;
  }
  function rebuildGate(g) {
    g.gen++; g.hp = g.max = gateHp(g, g.gen); g.ability = pickAbility(g, otherHpGates(S.gates, g)); g.dead = false; g.respawnT = 0;
    addFx((g.x0 + g.x1) / 2, GATE_Y + 10, '#ffffff', 14, 120);
  }

  const S = {};
  // opts: { tier: 'bronze' | 'silver', buff: null | 'ATTACK_SPEED' | 'ATTACK' | 'CANNON' | 'LASER' } (v19; buffs only apply in silver)
  function reset(opts) {
    const tier = opts && opts.tier === 'silver' ? 'silver' : 'bronze';
    const buff = tier === 'silver' && opts ? opts.buff || null : null;
    S.tier = tier; S.buff = buff;
    S.t = 0; S.over = false; S.result = null; S.started = false;
    S.spread = 1;   // before makeGates: the ability pool depends on it
    S.gates = makeGates();
    S.blue = []; S.red = []; S.bosses = []; S.pops = []; S.fx = []; S.multPop = null;
    S.spread = 1; S.blueHp = 1; S.blueSpeed = 62; S.bossKills = 0; S.fireMul = 1;
    S.turret = { x: W / 2, y: 700, fireT: 0, flash: 0 };
    S.redReserve = 1e9; S.redRate = 70; S.redRamp = 2.5; S.redAcc = 0;
    // EXTRA (silver): its own six-boss plan (D29-D32); red spawn stays as NORMAL (AMB-E8=B)
    S.bossQueue = (tier === 'silver' ? EXTRA_PLAN : BOSS_PLAN).map(b => ({ ...b }));
    // EXTRA boss 6: current sweep column (telegraph or firing; nothing is kept once it has fired) and orbs
    S.bolts = []; S.orbs = [];
    // EXTRA boss 7: purple columns (full-screen round and single ones; t < 0 = not shown yet) and lightning arrows
    S.pbolts = []; S.arrows = [];
    // settlement data: every planned boss kill (appearance order + game second) and how the run was lost
    S.kills = []; S.death = null;
    S.shake = 0;
    S.choosing = false; S.choice = null; S.cannon = { level: 0, t: 0, flash: 0 }; S.shells = []; S.booms = [];
    S.resting = false; S.bossCount = 0; S.choiceCards = null; S.paused = false;
    // beams of NORMAL boss 9 / EXTRA boss 5
    S.beams = [];
    // loadout buffs (D10-D12)
    S.fireMin = FIRE_MIN; S.laser = null; S.laserFx = [];
    if (buff === 'ATTACK_SPEED') { S.fireMul *= 0.8; S.fireMin = FIRE_MIN_BUFF; }
    if (buff === 'ATTACK') S.blueHp += 1;   // ATK+1 loadout buff (D37): same effect as the in-run ATK+1 ability
    if (buff === 'CANNON') { S.cannon.level = 1; S.cannon.t = 0.4; }
    if (buff === 'LASER') S.laser = { t: LASER.interval };
  }
  const rnd = (a, b) => a + Math.random() * (b - a);
  function addPop(text, x, y, color, sub) { const p = { text, sub: sub || '', x, y, color, t: 0, t0: S.t, n: 0 }; S.pops.push(p); return p; }
  function addFx(x, y, color, n, speed) {
    for (let i = 0; i < n; i++) {
      const a = Math.random() * Math.PI * 2, v = rnd(speed * 0.3, speed);
      S.fx.push({ x, y, vx: Math.cos(a) * v, vy: Math.sin(a) * v - 60, t: 0, life: rnd(0.3, 0.7), color });
    }
  }
  function spawnRed() {
    if (S.red.length >= MAXR || S.redReserve <= 0) return false;
    S.redReserve--;
    const lat = rnd(-PATH_HW + 3, PATH_HW - 3);
    const p = pathPos(PATH_LEN - 1, lat);
    S.red.push({ x: p[0], y: p[1], s: PATH_LEN - 1, lat, mode: 1, alive: true, v: rnd(26, 34), w: Math.random() * 6.28 });
    return true;
  }
  function spawnBoss(b) {
    const p = pathPos(PATH_LEN - 1, 0);
    S.bossCount++;
    const bo = { x: p[0], y: p[1], s: PATH_LEN - 1, hp: b.hp, max: b.hp, mode: 1, alive: true, v: b.v, r: b.r, hitT: 0, idx: S.bossCount, minion: false, summon: b.summon || null, cd: b.summon ? 2.5 : 0 };
    // NORMAL boss 9 fights with the full beam set (D42); EXTRA boss 5 with the same beams minus the enraged triple (D31);
    // EXTRA boss 6 with the lightning storm (D32). `breach`: reaching the turret counts as RED_BREACH (NORMAL boss 9)
    if (b.skills === 'beam') Object.assign(bo, { beam: true, enrage: !!b.enrage, soloCd: 4, wideCd: 0, enraged: false });
    if (b.breach) bo.breach = true;
    if (b.skills === 'storm') { bo.storm = { round: null, gap: 0, orbT: STORM.orbs.first }; startSweep(bo); }
    // EXTRA boss 7 (D39): appears straight in the centre; single column / arrow timers start after the opening round
    if (b.skills === 'purple') { bo.purple = { first: true, round: null, ct: 0, gap: 0, boltT: BOSS7.bolt.cd, arrowT: BOSS7.arrows.cd, home: null, cast: 0 }; enterCenter(bo); }
    S.bosses.push(bo);
    addPop('BOSS!', W / 2, 120, '#ff6b6b');
  }
  // result: win (last boss down; NORMAL boss 9, EXTRA boss 7) | lose
  function endGame(win, death) {
    S.over = true;
    if (!win) S.death = death || { cause: 'RED_BREACH', bossSeq: null };
    S.result = win ? 'win' : 'lose';
    S.choosing = false; S.resting = false; S.choiceCards = null;
    S.beams = []; S.bolts = []; S.orbs = []; S.pbolts = []; S.arrows = [];
    notify();
  }

  function cannonTarget() {
    // arena threats first (closest to the turret), otherwise the front of the path (red or boss, lowest s)
    const T = S.turret; let best = null, bd = 1e9;
    for (const r of S.red) if (r.alive && r.mode === 0) { const d = Math.hypot(r.x - T.x, r.y - T.y); if (d < bd) { bd = d; best = [r.x, r.y]; } }
    for (const b of S.bosses) if (b.alive && (b.mode === 0 || b.mode === 3)) { const d = Math.hypot(b.x - T.x, b.y - T.y); if (d < bd) { bd = d; best = [b.x, b.y]; } }
    if (best) return best;
    let ms = 1e9;
    for (const r of S.red) if (r.alive && r.mode === 1 && r.s < ms) { ms = r.s; best = [r.x, r.y]; }
    for (const b of S.bosses) if (b.alive && b.mode === 1 && b.s < ms) { ms = b.s; best = [b.x, b.y]; }
    return best;
  }
  function explode(x, y) {
    const lv = S.cannon.level, R = 34 + lv * 4, dmg = 350 * lv;
    for (const r of S.red) if (r.alive && Math.abs(r.x - x) < R && Math.abs(r.y - y) < R && Math.hypot(r.x - x, r.y - y) < R) r.alive = false;
    for (const b of S.bosses) if (b.alive && !(b.inv > 0) && Math.hypot(b.x - x, b.y - y) < R + b.r) { b.hp -= dmg; b.hitT = 0.12; if (b.hp <= 0) killBoss(b); }
    addFx(x, y, '#ffb347', 18, 200); addFx(x, y, '#fff2a8', 10, 120); S.shake = Math.max(S.shake, 0.35);
    S.booms.push({ x, y, t: 0, R });
  }
  function update(dt) {
    if (S.over || !S.started || S.choosing || S.resting || S.paused) return;
    S.t += dt;
    const T = S.turret;
    const gates = S.gates;

    // turret fire
    T.fireT -= dt; T.flash = Math.max(0, T.flash - dt); S.cannon.flash = Math.max(0, S.cannon.flash - dt);
    while (T.fireT <= 0) {
      T.fireT += Math.max(S.fireMin, FIRE_INTERVAL * S.fireMul); T.flash = 0.06;
      const n = S.spread, gap = Math.min(6, 70 / n);
      for (let k = 0; k < n; k++) {
        const c = k - (n - 1) / 2;
        S.blue.push({ x: T.x + c * gap, y: T.y - 26 - Math.abs(c) * 3, vx: c * 8, s: 0, lat: 0, mode: 2, alive: true, hp: S.blueHp, w: Math.random() * 6.28 });
      }
    }

    // cannon: slow lobbed shell at the nearest threat; SPD+ stacks shorten the reload a little
    if (S.cannon.level > 0) {
      S.cannon.t -= dt;
      if (S.cannon.t <= 0) {
        const tgt = cannonTarget();
        if (tgt) {
          const spdStacks = Math.max(0, Math.round((S.blueSpeed - 62) / 18));
          S.cannon.t = Math.max(0.9, 2.6 * Math.pow(0.88, spdStacks) * Math.pow(0.85, S.cannon.level - 1));
          S.shells.push({ x0: T.x, y0: T.y + 6, x1: tgt[0], y1: tgt[1], t: 0, T: 0.8, x: T.x, y: T.y + 6 });
          S.cannon.flash = 0.16;
        } else S.cannon.t = 0.3;
      }
    }
    // red spawn
    S.redAcc += (S.redRate + S.redRamp * S.t) * dt;
    while (S.redAcc >= 1) { S.redAcc--; if (!spawnRed()) { S.redAcc = 0; break; } }
    while (S.bossQueue.length && S.t >= S.bossQueue[0].at) spawnBoss(S.bossQueue.shift());
    for (const g of gates) if (g.kind === 'hp' && g.dead) { g.respawnT -= dt; if (g.respawnT <= 0) rebuildGate(g); }

    // red movement
    for (const r of S.red) {
      if (!r.alive) continue;
      if (r.mode === 1) {
        r.s -= r.v * dt;
        if (r.s <= 0) { r.mode = 0; const p = pathPos(0, r.lat); r.x = p[0]; r.y = p[1] + 4; }
        else { const p = pathPos(r.s, r.lat + Math.sin(S.t * 3 + r.w) * 1.2); r.x = p[0]; r.y = p[1]; }
      } else {
        const dx = T.x - r.x, dy = T.y - r.y, d = Math.hypot(dx, dy) || 1;
        r.x += dx / d * 46 * dt; r.y += dy / d * 46 * dt;
        if (d < 16) { endGame(false, { cause: 'RED_BREACH', bossSeq: null }); return; }
      }
    }
    // boss movement
    for (const bo of S.bosses) {
      if (!bo.alive) continue;
      bo.hitT = Math.max(0, bo.hitT - dt);
      if (bo.inv > 0) bo.inv = Math.max(0, bo.inv - dt);
      // EXTRA boss 7 casting pause (user 2026-11-03): no movement while casting; path progress s stays put
      const casting = !!bo.purple && bo.purple.cast > 1e-6;
      if (casting) bo.purple.cast = Math.max(0, bo.purple.cast - dt);
      if (bo.mode === 3 || casting) {
        // EXTRA boss 7 centre phase: stands still at the centre while it casts (D39)
      } else if (bo.mode === 1) {
        bo.s -= bo.v * dt;
        if (bo.s <= 0) { bo.mode = 0; }
        const p = pathPos(Math.max(bo.s, 0), 0); bo.x = p[0]; bo.y = p[1];
      } else {
        const dx = T.x - bo.x, dy = T.y - bo.y, d = Math.hypot(dx, dy) || 1;
        const sp = bo.minion ? 36 : 30;
        bo.x += dx / d * sp * dt; bo.y += dy / d * sp * dt;
        // a summoned minion is part of the red sea; a planned boss reaching the turret is "第 N 王抵達砲台"
        if (d < 10 + bo.r) { endGame(false, bo.minion || bo.breach ? { cause: 'RED_BREACH', bossSeq: null } : { cause: 'BOSS_REACHED', bossSeq: bo.idx }); return; }
      }
      if (bo.summon) { bo.cd -= dt; if (bo.cd <= 0) { bo.cd = bo.summon.cd; for (let i = 0; i < bo.summon.n; i++) spawnMinion(bo); addFx(bo.x, bo.y, '#ff4b4b', 16, 160); } }
      if (bo.beam) beamSkills(bo, dt);
      if (bo.storm && stormSkills(bo, dt)) return;
      if (bo.purple) purpleSkills(bo, dt);
    }
    // laser buff: a vertical sweep up from the turret every few seconds (GAP-05=A)
    if (S.laser) {
      S.laser.t -= dt;
      if (S.laser.t <= 0) { S.laser.t = Math.max(LASER.min, LASER.interval * S.fireMul); fireLaser(); }
    }
    for (let i = S.laserFx.length - 1; i >= 0; i--) { S.laserFx[i].t += dt; if (S.laserFx[i].t > LASER.fx) S.laserFx.splice(i, 1); }
    // beams: the warn phase is harmless; while firing, a turret inside the beam loses the run and soldiers in flight burn
    for (let i = S.beams.length - 1; i >= 0; i--) {
      const b = S.beams[i]; b.t += dt;
      if (b.t >= b.warn + b.fire) { S.beams.splice(i, 1); continue; }
      if (b.t < b.warn) continue;
      if (beamHits(b, T.x, T.y, BEAM_HIT_R)) { addFx(T.x, T.y, '#ff3b3b', 40, 220); S.shake = 1; endGame(false); return; }
      for (const u of S.blue) if (u.alive && u.mode === 2 && beamHits(b, u.x, u.y, 2)) u.alive = false;
    }
    // EXTRA boss 6 lightning: a firing column or an orb touching the turret ends the run (RED_BREACH, AMB-E9=B);
    // soldiers pass through both (human ruling: the sweep does not burn soldiers)
    for (const bl of S.bolts) {
      if (bl.t >= bl.warn && T.x + BEAM_HIT_R > bl.x0 && T.x - BEAM_HIT_R < bl.x1) { addFx(T.x, T.y, '#b98cff', 40, 220); S.shake = 1; endGame(false, { cause: 'RED_BREACH', bossSeq: null }); return; }
    }
    for (let i = S.orbs.length - 1; i >= 0; i--) {
      const o = S.orbs[i]; o.t += dt;
      if (o.t >= STORM.orbs.life) { S.orbs.splice(i, 1); continue; }
      o.x += o.vx * dt; o.y += o.vy * dt;
      const R = STORM.orbs.r;
      if (o.x < R) { o.x = R; o.vx = Math.abs(o.vx); } else if (o.x > W - R) { o.x = W - R; o.vx = -Math.abs(o.vx); }
      if (o.y < R) { o.y = R; o.vy = Math.abs(o.vy); } else if (o.y > H - R) { o.y = H - R; o.vy = -Math.abs(o.vy); }
      if (Math.hypot(o.x - T.x, o.y - T.y) < R + BEAM_HIT_R) { addFx(T.x, T.y, '#b98cff', 40, 220); S.shake = 1; endGame(false, { cause: 'RED_BREACH', bossSeq: null }); return; }
    }
    // EXTRA boss 7 purple lightning (D39): a firing column (full-screen round or single) or an arrow touching the
    // turret ends the run (RED_BREACH, X10=a); soldiers pass through. A column is gone the moment its firing time ends.
    for (let i = S.pbolts.length - 1; i >= 0; i--) {
      const bl = S.pbolts[i]; bl.t += dt;
      if (bl.t >= bl.warn + bl.fire) { S.pbolts.splice(i, 1); continue; }
      if (bl.t >= bl.warn && T.x + BEAM_HIT_R > bl.x0 && T.x - BEAM_HIT_R < bl.x1) { addFx(T.x, T.y, '#d36bff', 40, 220); S.shake = 1; endGame(false, { cause: 'RED_BREACH', bossSeq: null }); return; }
    }
    for (let i = S.arrows.length - 1; i >= 0; i--) {
      const a = S.arrows[i], A = BOSS7.arrows;
      a.x += a.ux * A.speed * dt; a.y += a.uy * A.speed * dt;
      // (x, y) is the head; the tail trails by `len`. No bounce: gone once the tail is off screen and moving away.
      const tx = a.x - a.ux * A.len, ty = a.y - a.uy * A.len;
      if ((tx < 0 && a.ux <= 0) || (tx > W && a.ux >= 0) || (ty < 0 && a.uy <= 0) || (ty > H && a.uy >= 0)) { S.arrows.splice(i, 1); continue; }
      // capsule (shaft segment, half-width hw) vs turret circle
      const k = Math.max(0, Math.min(A.len, (T.x - tx) * a.ux + (T.y - ty) * a.uy));
      if (Math.hypot(tx + a.ux * k - T.x, ty + a.uy * k - T.y) < A.hw + BEAM_HIT_R) { addFx(T.x, T.y, '#d36bff', 40, 220); S.shake = 1; endGame(false, { cause: 'RED_BREACH', bossSeq: null }); return; }
    }
    if (S.bosses.some(b => b.minion && !b.alive)) S.bosses = S.bosses.filter(b => b.alive || !b.minion);
    const pathBosses = S.bosses.filter(b => b.alive && b.mode === 1), arenaBosses = S.bosses.filter(b => b.alive && b.mode === 0);
    const centerBosses = S.bosses.filter(b => b.alive && b.mode === 3);

    // blue movement + gate crossing
    const mg = gates[1];
    let multCount = 0;
    for (let i = 0; i < S.blue.length; i++) {
      const u = S.blue[i];
      if (!u.alive) continue;
      if (u.mode === 2) {
        // fired soldier flying straight up the arena
        u.x += u.vx * dt; u.y -= SHOT_SPEED * dt;
        if (u.y <= GATE_Y + GATE_H) {
          const g = u.x < gates[0].x1 + GATE_GAP / 2 ? gates[0] : u.x < gates[1].x1 + GATE_GAP / 2 ? gates[1] : gates[2];
          if (g.kind === 'mult') {
            u.mode = 1; u.s = rnd(0, 10); u.lat = rnd(-PATH_HW + 3, PATH_HW - 3);
            const n = g.mult - 1;
            for (let k = 0; k < n; k++) {
              S.blue.push({ x: u.x, y: u.y, s: rnd(0, 60), lat: rnd(-PATH_HW + 3, PATH_HW - 3), mode: 1, alive: true, hp: u.hp, w: Math.random() * 6.28 });
            }
            multCount++;
            if (Math.random() < 0.3) addFx(u.x, GATE_Y + 10, '#8fb3ff', 3, 120);
          } else if (!g.dead) {
            g.hp -= 1; u.alive = false;
            addFx(u.x, GATE_Y + GATE_H, '#f2c233', 2, 90);
            if (g.hp <= 0) { g.hp = 0; g.dead = true; g.respawnT = GATE_RESPAWN; onReward(g); }
          } else {
            u.alive = false; addFx(u.x, GATE_Y + GATE_H, '#777', 1, 50);
          }
          continue;
        }
        let hitBoss = false;
        for (const bo of arenaBosses) {
          if (bo.alive && Math.hypot(bo.x - u.x, bo.y - u.y) < bo.r + 4) {
            bo.hp -= u.hp; bo.hitT = 0.1; u.alive = false; hitBoss = true; addFx(u.x, u.y, '#ffd6d6', 2, 80);
            if (bo.hp <= 0) killBoss(bo);
            break;
          }
        }
        if (hitBoss) continue;
        for (const r of S.red) {
          if (r.mode === 0 && r.alive && Math.abs(r.x - u.x) < 6 && Math.abs(r.y - u.y) < 7) { r.alive = false; u.alive = false; addFx(r.x, r.y, '#ff5a5a', 3, 70); break; }
        }
      } else {
        u.s += S.blueSpeed * dt;
        if (u.s >= PATH_LEN) { u.alive = false; S.redReserve = Math.max(0, S.redReserve - 1); continue; }
        const p = pathPos(u.s, u.lat + Math.sin(S.t * 4 + u.w) * 1.0); u.x = p[0]; u.y = p[1];
      }
    }

    if (multCount) {
      const mp = S.multPop;
      if (mp && S.t - mp.t0 < 0.6 && mp.t < 1.2) { mp.n += multCount; }
      else { S.multPop = addPop('x' + mg.mult, (mg.x0 + mg.x1) / 2, GATE_Y - 50, '#ffe066'); S.multPop.n = multCount; }
    }

    // combat on path: bucket reds by s
    const BK = 6, nb = Math.ceil(PATH_LEN / BK) + 3;
    const buckets = new Array(nb);
    for (let i = 0; i < S.red.length; i++) {
      const r = S.red[i];
      if (!r.alive || r.mode !== 1) continue;
      const k = Math.max(0, Math.min(nb - 1, (r.s / BK) | 0));
      (buckets[k] || (buckets[k] = [])).push(r);
    }
    for (const u of S.blue) {
      if (!u.alive || u.mode !== 1) continue;
      let hitBoss = false;
      for (const bo of pathBosses) {
        if (bo.alive && Math.abs(bo.s - u.s) < 12) { u.alive = false; bo.hp -= u.hp; bo.hitT = 0.08; hitBoss = true; if (bo.hp <= 0) killBoss(bo); break; }
      }
      // EXTRA boss 7 in the centre: path soldiers passing within reach hit it (no damage while invulnerable)
      if (!hitBoss) for (const bo of centerBosses) {
        if (bo.alive && Math.abs(bo.x - u.x) < bo.r + 4 && Math.abs(bo.y - u.y) < bo.r + 4 && Math.hypot(bo.x - u.x, bo.y - u.y) < bo.r + 4) { u.alive = false; hurtBoss(bo, u.hp, 0.08); hitBoss = true; break; }
      }
      if (hitBoss) continue;
      const k = (u.s / BK) | 0;
      let killed = false;
      for (let j = k; j <= k + 2 && j < nb && !killed; j++) {
        const list = buckets[j]; if (!list) continue;
        for (let m = 0; m < list.length; m++) {
          const r = list[m];
          if (r.alive && r.s >= u.s - 3 && r.s <= u.s + 9) {
            r.alive = false; u.hp -= 1; if (u.hp <= 0) u.alive = false; killed = true;
            if (Math.random() < 0.08) addFx(u.x, u.y, '#fff2a8', 2, 60);
            break;
          }
        }
      }
    }
    // drop dead units: in-place compaction, no new arrays per frame (FE-59). The same pass fills a 1px arc-length
    // histogram of path units for the cap below.
    const BL = S.blue, top = CULL_BINS.length - 1;
    CULL_BINS.fill(0);
    let nBlue = 0;
    for (let i = 0; i < BL.length; i++) {
      const u = BL[i]; if (!u.alive) continue;
      BL[nBlue++] = u;
      if (u.mode === 1) { const b = u.s | 0; CULL_BINS[b < 0 ? 0 : b > top ? top : b]++; }
    }
    BL.length = nBlue;
    if (nBlue > MAXB) {
      // Over the cap: cull the rear of the path column. Walk the histogram from the rear until the excess is covered and
      // drop every path unit in those buckets (at most one bucket more than the exact excess). O(n), no sort.
      let need = nBlue - MAXB, k = 0;
      while (k <= top && need > 0) need -= CULL_BINS[k++];
      let n = 0;
      for (let i = 0; i < nBlue; i++) { const u = BL[i]; if (u.mode !== 1 || (u.s | 0) >= k) BL[n++] = u; }
      BL.length = n;
    }
    compact(S.red, r => r.alive);

    // shells
    for (let i = S.shells.length - 1; i >= 0; i--) {
      const sh = S.shells[i]; sh.t += dt;
      const k = Math.min(1, sh.t / sh.T);
      sh.x = sh.x0 + (sh.x1 - sh.x0) * k; sh.y = sh.y0 + (sh.y1 - sh.y0) * k - Math.sin(k * Math.PI) * 120;
      if (k >= 1) { explode(sh.x1, sh.y1); S.shells.splice(i, 1); }
    }
    // fx + pops
    for (let i = S.booms.length - 1; i >= 0; i--) { const b = S.booms[i]; b.t += dt; if (b.t > 0.25) S.booms.splice(i, 1); }
    for (let i = S.fx.length - 1; i >= 0; i--) {
      const f = S.fx[i]; f.t += dt; f.x += f.vx * dt; f.y += f.vy * dt; f.vy += 260 * dt;
      if (f.t > f.life) { S.fx[i] = S.fx[S.fx.length - 1]; S.fx.pop(); }
    }
    for (let i = S.pops.length - 1; i >= 0; i--) { const p = S.pops[i]; p.t += dt; if (p.t > 1.2) S.pops.splice(i, 1); }
    S.shake = Math.max(0, S.shake - dt * 3);

    // victory
    if (S.bossQueue.length === 0 && S.bosses.length && S.bosses.every(b => !b.alive)) endGame(true);
  }
  function fireLaser() {
    const T = S.turret, hw = LASER.halfWidth;
    for (const r of S.red) if (r.alive && Math.abs(r.x - T.x) < hw && r.y < T.y) r.alive = false;
    for (const b of S.bosses) {
      if (!b.alive || Math.abs(b.x - T.x) >= hw + b.r || b.y >= T.y) continue;
      if (b.minion) { killBoss(b); continue; }
      if (b.inv > 0) continue;   // EXTRA boss 7 during its invulnerable second
      b.hp -= LASER.bossDamage * S.blueHp; b.hitT = 0.12;
      if (b.hp <= 0) killBoss(b);
    }
    S.laserFx.push({ x: T.x, t: 0 });
  }
  // ---- EXTRA boss 6 (D32): lightning sweep + lightning orbs ----
  function startSweep(bo) {
    const n = STORM.sweep.strips, order = [];
    for (let i = 0; i < n; i++) order.splice((Math.random() * (order.length + 1)) | 0, 0, i);   // random order, no repeats
    bo.storm.round = { order, i: 0 };
    nextBolt(bo);
  }
  function nextBolt(bo) {
    const r = bo.storm.round, w = W / STORM.sweep.strips, k = r.order[r.i];
    S.bolts = [{ strip: k, x0: k * w, x1: (k + 1) * w, t: 0, warn: STORM.sweep.warn, fire: STORM.sweep.fire }];
  }
  // returns true when the run ended
  function stormSkills(bo, dt) {
    const st = bo.storm;
    if (st.round) {
      const bl = S.bolts[0];
      if (bl) {
        bl.t += dt;
        // the column stops colliding and its telegraph is gone the moment its firing time is over (FE-41)
        if (bl.t >= bl.warn + bl.fire) {
          S.bolts = [];
          if (++st.round.i < st.round.order.length) nextBolt(bo);
          else { st.round = null; st.gap = STORM.sweep.cd; }   // cd counts from the end of the round
        }
      }
    } else {
      st.gap -= dt;
      if (st.gap <= 0) startSweep(bo);
    }
    st.orbT -= dt;
    if (st.orbT <= 0) {
      st.orbT = STORM.orbs.cd;
      const O = STORM.orbs;
      for (let i = 0; i < O.n; i++) {
        const a = (O.minDeg + Math.random() * (O.maxDeg - O.minDeg)) * Math.PI / 180;   // each orb its own angle
        S.orbs.push({ x: bo.x, y: bo.y, vx: Math.cos(a) * O.speed, vy: Math.sin(a) * O.speed, t: 0 });
      }
    }
    return false;
  }
  // ---- EXTRA boss 7 (D39): centre phase + full-screen lightning, single columns, lightning arrows ----
  // damage that respects the invulnerable second (the dev hook uses it too); returns whether it landed.
  // Nothing lands once the run is over, so no hp loss and no extra kills after the result is decided.
  function hurtBoss(bo, dmg, flash) {
    if (S.over || !bo || !bo.alive || bo.inv > 0) return false;
    bo.hp -= dmg; bo.hitT = flash || 0.1;
    if (bo.hp <= 0) killBoss(bo);
    return true;
  }
  function enterCenter(bo) {
    const P = bo.purple, C = BOSS7.center;
    if (!P.first) P.home = { mode: bo.mode, s: bo.s, x: bo.x, y: bo.y };   // a later round goes back to this spot
    bo.mode = 3; bo.x = C.x; bo.y = C.y; bo.inv = BOSS7.inv; P.ct = 0; P.round = null;
    addFx(C.x, C.y, '#d36bff', 30, 200);
  }
  function leaveCenter(bo) {
    const P = bo.purple;
    if (P.first || !P.home) { bo.mode = 1; bo.s = PATH_LEN - 1; const p = pathPos(bo.s, 0); bo.x = p[0]; bo.y = p[1]; }
    else { bo.mode = P.home.mode; bo.s = P.home.s; bo.x = P.home.x; bo.y = P.home.y; }
    addFx(bo.x, bo.y, '#d36bff', 20, 160);
    P.first = false; P.home = null; P.round = null; P.gap = BOSS7.sweep.cd;   // cd counts from the end of the round
  }
  function startPurpleRound(bo, dt) {
    const P = bo.purple, SW = BOSS7.sweep, w = W / SW.strips;
    const p = stripOf(S.turret.x), s = (Math.random() * SW.strips) | 0, order = sweepOrder(p, s);
    P.round = { p, s, order, t0: S.t };
    P.cast = BOSS7.cast.sweep;   // casting pose for the whole round's telegraph sequence
    // bolt clocks are advanced later this frame: start them so that the k-th shows at k * step after the round start
    const over = P.ct - BOSS7.inv - dt;
    order.forEach((c, k) => S.pbolts.push({ strip: c, x0: c * w, x1: (c + 1) * w, t: over - k * SW.step, warn: SW.warn, fire: SW.fire, sweep: true, k, slant: randSlant() }));
  }
  // visual-only drift of a purple band at y = 0 relative to the turret row (px, + = right); drawn once per column
  // when it is created, shared by its telegraph and its firing. Hit test ignores it (turret-row column only).
  function randSlant() { return Math.round((Math.random() * 2 - 1) * BOSS7.slantMax); }
  function purpleSkills(bo, dt) {
    const P = bo.purple, B = BOSS7;
    if (bo.mode === 3) {
      P.ct += dt;
      if (!P.round) { if (P.ct >= B.inv) startPurpleRound(bo, dt); }
      // the centre phase lasts at least until the 2 s cast is over (user 2026-11-03), then the existing rule applies
      else if (P.cast <= 1e-6 && !S.pbolts.some(b => b.sweep)) leaveCenter(bo);
      return;
    }
    // cd over: back to the centre once no single column is in progress (INF-X6); timers stay paused meanwhile
    P.gap -= dt;
    if (P.gap <= 0) { if (!S.pbolts.length) enterCenter(bo); return; }
    const w = W / B.sweep.strips;
    P.boltT -= dt;
    if (P.boltT <= 0) {
      const c = (Math.random() * B.sweep.strips) | 0;
      S.pbolts.push({ strip: c, x0: c * w, x1: (c + 1) * w, t: -P.boltT - dt, warn: B.bolt.warn, fire: B.bolt.fire, sweep: false, slant: randSlant() });
      P.boltT += B.bolt.cd;
      P.cast = Math.max(P.cast, B.cast.bolt);
    }
    P.arrowT -= dt;
    if (P.arrowT <= 0) {
      P.arrowT += B.arrows.cd;
      P.cast = Math.max(P.cast, B.cast.arrows);
      const A = B.arrows;
      for (let i = 0; i < A.n; i++) {
        const a = (A.minDeg + Math.random() * (A.maxDeg - A.minDeg)) * Math.PI / 180, ux = Math.cos(a), uy = Math.sin(a);
        // the tail leaves from the boss; (x, y) is the head
        S.arrows.push({ x: bo.x + ux * A.len, y: bo.y + uy * A.len, ux, uy, t0: S.t });
      }
    }
  }
  // ---- beams (NORMAL boss 9, EXTRA boss 5) ----
  // solo beam every solo.cd from wherever the boss stands: pillar or S-curve at random, and once enraged
  // also a triple of parallel S-curves; from half hp a twelve-ray star every wide.cd.
  // The star and the triple never overlap: the triple is not picked while a star is up, and the star waits for the triple to end.
  function beamSkills(bo, dt) {
    const B = BEAM, TAU = Math.PI * 2;
    const starUp = S.beams.some(b => b.kind === 'cross'), tripleUp = S.beams.some(b => b.tri);
    bo.soloCd -= dt;
    if (bo.soloCd <= 0) {
      bo.soloCd = B.solo.cd;
      const pick = (Math.random() * (bo.enrage && bo.enraged && !starUp ? 3 : 2)) | 0, ph = Math.random() * TAU;
      const solo = (kind, dx, tri) => S.beams.push({ kind, x: bo.x + dx, y: bo.y, ph, tri: !!tri, t: 0, warn: B.solo.warn, fire: B.solo.fire, hw: B.solo.hw });
      if (pick === 0) solo('pillar', 0);
      else if (pick === 1) solo('scurve', 0);
      else for (const dx of [-B.scurve.triGap, 0, B.scurve.triGap]) solo('scurve', dx, true);
    }
    // EXTRA boss 5 (no bo.enrage): no enraged triple and no ENRAGE text (D31; effects carry no words)
    if (!bo.enraged && bo.hp <= bo.max / 2) { bo.enraged = true; bo.wideCd = 1.5; S.shake = Math.max(S.shake, 0.8); if (bo.enrage) addPop('ENRAGE', W / 2, 200, '#ff3b3b'); }
    if (bo.enraged) {
      bo.wideCd -= dt;
      if (bo.wideCd <= 0 && !tripleUp) {
        bo.wideCd = B.wide.cd;
        S.beams.push({ kind: 'cross', x: bo.x, y: bo.y, a: Math.random() * TAU / B.rays, t: 0, warn: B.wide.warn, fire: B.wide.fire, hw: B.wide.hw });
      }
    }
  }
  // point (x, y) with radius r touches the beam. Curved beams use the perpendicular distance
  // (offset divided by sqrt(1 + slope²)) so steep stretches are not wider than straight ones.
  function beamHits(b, x, y, r) {
    const B = BEAM, TAU = Math.PI * 2, R = b.hw + r;
    if (b.kind === 'pillar') return y > b.y && Math.abs(x - b.x) < R;
    if (b.kind === 'scurve') {
      if (y <= b.y) return false;
      const u = (y - b.y) / B.scurve.wl * TAU, cx = b.x + B.scurve.amp * Math.sin(u + b.ph), slope = B.scurve.amp * TAU / B.scurve.wl * Math.cos(u + b.ph);
      return Math.abs(x - cx) / Math.sqrt(1 + slope * slope) < R;
    }
    const lines = B.rays / 2;
    for (let k = 0; k < lines; k++) {
      const a = b.a + k * Math.PI / lines;
      if (Math.abs((x - b.x) * Math.sin(a) - (y - b.y) * Math.cos(a)) < R) return true;
    }
    return false;
  }
  function spawnMinion(bo) {
    const m = { hp: bo.summon.hp, max: bo.summon.hp, alive: true, v: bo.summon.v, r: 11, hitT: 0, idx: 0, minion: true, summon: null, cd: 0, mode: bo.mode };
    if (bo.mode === 1) { m.s = Math.max(1, bo.s - rnd(8, 36)); const p = pathPos(m.s, rnd(-PATH_HW + 8, PATH_HW - 8)); m.x = p[0]; m.y = p[1]; }
    else { m.s = 0; m.x = bo.x + rnd(-24, 24); m.y = bo.y + rnd(-10, 10); }
    S.bosses.push(m); addFx(m.x, m.y, '#ff8a8a', 6, 90);
  }
  function killBoss(bo) {
    if (!bo || !bo.alive) return;
    bo.alive = false; bo.hp = 0;
    if (bo.minion) { addFx(bo.x, bo.y, '#ff8a8a', 10, 120); return; }
    S.shake = 1; S.bossKills++;
    S.kills.push({ bossSeq: bo.idx, killedAtSecond: Math.floor(S.t) });
    if (bo.beam) S.beams = [];
    if (bo.storm) { S.bolts = []; S.orbs = []; bo.storm = null; }
    if (bo.purple) { S.pbolts = []; S.arrows = []; bo.purple = null; bo.inv = 0; }   // INF-X7
    addFx(bo.x, bo.y, '#ff4b4b', 40, 220); addPop('BOSS DOWN', W / 2, 200, '#ffe066');
    if (bo.summon) for (const m of S.bosses) if (m.minion && m.alive) killBoss(m);
    // a held boss waits in the queue: with the field clear it is released by the rest pick (NORMAL boss 8, EXTRA
    // boss 5) or right away for 'kill' (NORMAL boss 9, EXTRA bosses 6 and 7), never before its minAt second (spawned by the update loop)
    const head = S.bossQueue[0];
    if (head && head.at === Infinity && !S.bosses.some(b => b.alive && !b.minion)) {
      if (head.after === 'kill') head.at = Math.max(S.t, head.minAt || 0);
      else openRest();
    }
  }
  // ---- rest: widen one gate, total width stays the same ----
  function resizeGates(which) {
    const gs = S.gates, w = gs.map(g => g.x1 - g.x0);
    if (which === 1) { const d = Math.min(GATE_STEP / 2, w[0] - GATE_MIN, w[2] - GATE_MIN); w[0] -= d; w[2] -= d; w[1] += 2 * d; }
    else { const o = which === 0 ? 2 : 0, d = Math.min(GATE_STEP, w[o] - GATE_MIN); w[o] -= d; w[which] += d; }
    let x = GATE_X0;
    gs.forEach((g, i) => { g.x0 = x; g.x1 = x + w[i]; x = g.x1 + GATE_GAP; });
  }
  function openRest() { S.resting = true; notify(); }
  function pickRest(i) {
    if (!S.resting) return;
    resizeGates(i); S.resting = false; notify();
    addFx((S.gates[i].x0 + S.gates[i].x1) / 2, GATE_Y + 10, '#ffe066', 24, 160);
    const head = S.bossQueue[0];
    if (head && head.at === Infinity) {
      if (!head.minAt || S.t >= head.minAt) spawnBoss(S.bossQueue.shift());
      else head.at = head.minAt;   // EXTRA boss 5 never enters before 70 s (the rest keeps the clock stopped)
    }
  }
  function onReward(g) {
    const ab = ABILITIES[g.ability];
    addFx((g.x0 + g.x1) / 2, GATE_Y + 10, ab.color, 30, 180);
    // a multiplier crate hung before the cap was reached reads MAX (see render) and does nothing
    if (ab.mult && spreadMaxed(S)) { addPop('MAX', W / 2, 560, ab.color); return; }
    ab.apply(S, api);
    addPop(ab.mult && spreadMaxed(S) ? 'MAX' : ab.name, W / 2, 560, ab.color);
  }


  function startGame(opts) { reset(opts); S.started = true; notify(); }
  // mid-run restart (AMB-01, FE-43): same run, same tier and loadout buff taken from the engine itself, clock back to 0
  function restart() { startGame({ tier: S.tier, buff: S.buff }); }
  // dev-only (window.__game): end the run as a loss right now
  function loseNow() { if (S.started && !S.over) endGame(false, { cause: 'RED_BREACH', bossSeq: null }); }
  function setPaused(v) { if (S.started && !S.over && S.paused !== !!v) { S.paused = !!v; notify(); } }
  function setTurretX(x) { if (Number.isFinite(x)) S.turret.x = Math.max(22, Math.min(W - 22, x)); }

  const api = { S, reset, update, startGame, restart, setPaused, setTurretX, openChoice, pickChoice, openRest, pickRest, killBoss, hurtBoss, beamHits, loseNow, rebuildGate, onReward };
  reset();
  return api;
}
