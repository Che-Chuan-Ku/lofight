// Simulation: state, spawning, movement, combat, gates, bosses, rest/choice pauses.
// Copied from the single-file v17 game; UI changes are reported through `onChange` instead of touching the DOM.
import { W, GATE_Y, GATE_H, PATH_HW, GATE_MULT, SHOT_SPEED, MAXB, MAXR, FIRE_INTERVAL, FIRE_MIN, BOSS_PLAN, GATE_RESPAWN, GATE_X0, GATE_GAP, GATE_MIN, GATE_STEP } from './constants.js';
import { PATH_LEN, pathPos } from './path.js';
import { ABILITIES, CHOICES, cardFor } from './abilities.js';

export function createGame(onChange) {
  const notify = () => { if (onChange) onChange(); };

  function openChoice(force) {
    const keys = Object.keys(CHOICES);
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
    c.apply(S, api); addPop(c.title, W / 2, 560, c.good ? '#ffe066' : '#c0c0c0');
  }
  const ABILITY_POOL = Object.keys(ABILITIES);
  // random pick that avoids this gate's previous ability and the other gate's current one
  function pickAbility(g, others) {
    const avoid = new Set([g.ability, ...others.map(o => o.ability)]);
    let pool = ABILITY_POOL.filter(a => !avoid.has(a));
    if (!pool.length) pool = ABILITY_POOL;
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
  function reset() {
    S.t = 0; S.over = false; S.result = null; S.started = false;
    S.gates = makeGates();
    S.blue = []; S.red = []; S.bosses = []; S.pops = []; S.fx = []; S.multPop = null;
    S.spread = 1; S.blueHp = 1; S.blueSpeed = 62; S.bossKills = 0; S.fireMul = 1;
    S.turret = { x: W / 2, y: 700, fireT: 0, flash: 0 };
    S.redReserve = 1e9; S.redRate = 70; S.redRamp = 2.5; S.redAcc = 0;
    S.bossQueue = BOSS_PLAN.map(b => ({ ...b }));
    S.shake = 0;
    S.choosing = false; S.choice = null; S.cannon = { level: 0, t: 0, flash: 0 }; S.shells = []; S.booms = [];
    S.resting = false; S.bossCount = 0; S.choiceCards = null;
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
    S.bosses.push({ x: p[0], y: p[1], s: PATH_LEN - 1, hp: b.hp, max: b.hp, mode: 1, alive: true, v: b.v, r: b.r, hitT: 0, idx: S.bossCount, minion: false, summon: b.summon || null, cd: b.summon ? 2.5 : 0 });
    addPop('BOSS!', W / 2, 120, '#ff6b6b');
  }
  function endGame(win) {
    S.over = true; S.result = win ? 'win' : 'lose';
    S.choosing = false; S.resting = false; S.choiceCards = null;
    notify();
  }

  function cannonTarget() {
    // arena threats first (closest to the turret), otherwise the front of the path (red or boss, lowest s)
    const T = S.turret; let best = null, bd = 1e9;
    for (const r of S.red) if (r.alive && r.mode === 0) { const d = Math.hypot(r.x - T.x, r.y - T.y); if (d < bd) { bd = d; best = [r.x, r.y]; } }
    for (const b of S.bosses) if (b.alive && b.mode === 0) { const d = Math.hypot(b.x - T.x, b.y - T.y); if (d < bd) { bd = d; best = [b.x, b.y]; } }
    if (best) return best;
    let ms = 1e9;
    for (const r of S.red) if (r.alive && r.mode === 1 && r.s < ms) { ms = r.s; best = [r.x, r.y]; }
    for (const b of S.bosses) if (b.alive && b.mode === 1 && b.s < ms) { ms = b.s; best = [b.x, b.y]; }
    return best;
  }
  function explode(x, y) {
    const lv = S.cannon.level, R = 34 + lv * 4, dmg = 350 * lv;
    for (const r of S.red) if (r.alive && Math.abs(r.x - x) < R && Math.abs(r.y - y) < R && Math.hypot(r.x - x, r.y - y) < R) r.alive = false;
    for (const b of S.bosses) if (b.alive && Math.hypot(b.x - x, b.y - y) < R + b.r) { b.hp -= dmg; b.hitT = 0.12; if (b.hp <= 0) killBoss(b); }
    addFx(x, y, '#ffb347', 18, 200); addFx(x, y, '#fff2a8', 10, 120); S.shake = Math.max(S.shake, 0.35);
    S.booms.push({ x, y, t: 0, R });
  }
  function update(dt) {
    if (S.over || !S.started || S.choosing || S.resting) return;
    S.t += dt;
    const T = S.turret;
    const gates = S.gates;

    // turret fire
    T.fireT -= dt; T.flash = Math.max(0, T.flash - dt); S.cannon.flash = Math.max(0, S.cannon.flash - dt);
    while (T.fireT <= 0) {
      T.fireT += Math.max(FIRE_MIN, FIRE_INTERVAL * S.fireMul); T.flash = 0.06;
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
        if (d < 16) { endGame(false); return; }
      }
    }
    // boss movement
    for (const bo of S.bosses) {
      if (!bo.alive) continue;
      bo.hitT = Math.max(0, bo.hitT - dt);
      if (bo.mode === 1) {
        bo.s -= bo.v * dt;
        if (bo.s <= 0) { bo.mode = 0; }
        const p = pathPos(Math.max(bo.s, 0), 0); bo.x = p[0]; bo.y = p[1];
      } else {
        const dx = T.x - bo.x, dy = T.y - bo.y, d = Math.hypot(dx, dy) || 1;
        const sp = bo.minion ? 36 : 30;
        bo.x += dx / d * sp * dt; bo.y += dy / d * sp * dt;
        if (d < 10 + bo.r) { endGame(false); return; }
      }
      if (bo.summon) { bo.cd -= dt; if (bo.cd <= 0) { bo.cd = bo.summon.cd; for (let i = 0; i < bo.summon.n; i++) spawnMinion(bo); addFx(bo.x, bo.y, '#ff4b4b', 16, 160); } }
    }
    if (S.bosses.some(b => b.minion && !b.alive)) S.bosses = S.bosses.filter(b => b.alive || !b.minion);
    const pathBosses = S.bosses.filter(b => b.alive && b.mode === 1), arenaBosses = S.bosses.filter(b => b.alive && b.mode === 0);

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
    S.blue = S.blue.filter(u => u.alive);
    if (S.blue.length > MAXB) {
      const ss = []; for (const u of S.blue) if (u.mode === 1) ss.push(u.s);
      ss.sort((a, b) => a - b);
      const thr = ss[Math.min(ss.length - 1, S.blue.length - MAXB)];
      S.blue = S.blue.filter(u => u.mode !== 1 || u.s > thr);
    }
    S.red = S.red.filter(r => r.alive);

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
    addFx(bo.x, bo.y, '#ff4b4b', 40, 220); addPop('BOSS DOWN', W / 2, 200, '#ffe066');
    if (bo.summon) for (const m of S.bosses) if (m.minion && m.alive) killBoss(m);
    // a manually-spawned boss waits in the queue: pause for the rest choice before it enters
    if (S.bossQueue.length && S.bossQueue[0].at === Infinity && !S.bosses.some(b => b.alive && !b.minion)) openRest();
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
    if (S.bossQueue.length && S.bossQueue[0].at === Infinity) spawnBoss(S.bossQueue.shift());
  }
  function onReward(g) {
    const ab = ABILITIES[g.ability];
    addFx((g.x0 + g.x1) / 2, GATE_Y + 10, ab.color, 30, 180);
    ab.apply(S, api);
    addPop(ab.name, W / 2, 560, ab.color);
  }


  function startGame() { reset(); S.started = true; notify(); }
  function setTurretX(x) { if (Number.isFinite(x)) S.turret.x = Math.max(22, Math.min(W - 22, x)); }

  const api = { S, reset, update, startGame, setTurretX, openChoice, pickChoice, openRest, pickRest, killBoss };
  reset();
  return api;
}
