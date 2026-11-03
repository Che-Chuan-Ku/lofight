import { useEffect, useRef, useState } from 'react';
import { createGame } from '../game/engine.js';
import { render, themeKey } from '../game/render.js';
import { loadSprites } from '../game/tiles.js';
import { W, H, MAX_EXTRA_H, MAX_CROP_H, BOSS7 } from '../game/constants.js';

const phaseOf = S => !S.started ? 'intro' : S.over ? 'end' : S.paused ? 'paused' : S.choosing ? 'choice' : S.resting ? 'rest' : S.offering ? 'offer' : S.bonusing ? 'bonus' : 'play';
const snapshot = S => ({ phase: phaseOf(S), result: S.result, time: S.t.toFixed(0), clearTime: S.clearT.toFixed(0), choiceCards: S.choiceCards, bonusCards: S.bonusCards });

// Owns the engine instance, the canvas loop, sizing and input. React only sees a small `ui` snapshot.
// sceneryRef.current = true draws only the static battlefield (menus are open over the canvas).
export function useGame({ canvasRef, appRef, stageRef, startKeyRef, sceneryRef }) {
  const gameRef = useRef(null), startRef = useRef(null);
  const [ui, setUi] = useState({ phase: 'intro', result: null, time: '0', clearTime: '0', choiceCards: null, bonusCards: null });
  const [hint, setHint] = useState(true);

  useEffect(() => {
    const cv = canvasRef.current, ctx = cv.getContext('2d');
    let game;
    const sync = () => setUi(snapshot(game.S));
    game = createGame(sync); gameRef.current = game;
    const S = game.S;
    const SPR = loadSprites();
    // HUD numbers are written straight to the DOM (10x/s) so the game loop never waits on a React commit.
    const hudEls = {};
    for (const el of stageRef.current.querySelectorAll('[data-hud]')) hudEls[el.dataset.hud] = el;
    const updateHud = () => {
      hudEls.blue.textContent = String(S.blue.length);
      hudEls.red.textContent = String(S.red.length);
      hudEls.t.textContent = S.t.toFixed(0);
      hudEls.kills.textContent = String(S.bossKills);
    };

    // ---- sizing; `lite` drops to dpr 1 + squares after sustained slow frames ----
    // Width is always 390 logical px. Taller viewports get extra logical height above the battlefield
    // (more sky / canopy, up to MAX_EXTRA_H); shorter portrait viewports crop the top (up to MAX_CROP_H).
    // Either way a phone fills the whole screen; landscape/desktop simply fits the height.
    let scale = 1, lite = false, slowN = 0, sceneKey = '';
    const view = { h: H, oy: 0 };
    function resize() {
      const app = appRef.current, stage = stageRef.current;
      if (!app || !stage) return;
      const cw = app.clientWidth, ch = app.clientHeight;
      if (!cw || !ch) return;
      const need = Math.round(W * ch / cw);   // logical height that fills the viewport at full width
      let oy = 0;
      if (need >= H) oy = Math.min(MAX_EXTRA_H, need - H);
      else if (ch > cw) oy = -Math.min(MAX_CROP_H, H - need);
      view.h = H + oy; view.oy = oy;
      scale = Math.min(cw / W, ch / view.h);
      const dpr = Math.min(window.devicePixelRatio || 1, lite ? 1 : 2);
      cv.style.width = (W * scale) + 'px'; cv.style.height = (view.h * scale) + 'px';
      cv.width = Math.round(W * scale * dpr); cv.height = Math.round(view.h * scale * dpr);
      stage.style.width = cv.style.width; stage.style.height = cv.style.height;
      const full = Math.abs(W * scale - cw) < 2 && Math.abs(view.h * scale - ch) < 2;
      if (full) stage.dataset.full = ''; else delete stage.dataset.full;
      ctx.setTransform(scale * dpr, 0, 0, scale * dpr, 0, 0);
      sceneKey = '';   // assigning canvas width/height clears the bitmap, even to the same size: menus must repaint
    }
    window.addEventListener('resize', resize); resize();
    // WebKit can mount React before the viewport has its final size; observe the container, not just the window.
    const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(() => resize()) : null;
    if (ro) ro.observe(appRef.current);

    // ---- loop ----
    let last = performance.now(), hudT = 0, renderErr = false, raf = 0;
    function frame(now) {
      const dt = Math.min(0.05, (now - last) / 1000); last = now;
      if (S.started && !S.over && dt > 0.045) { if (++slowN > 45 && !lite) { lite = true; resize(); } } else slowN = Math.max(0, slowN - 1);
      game.update(dt);
      view.scenery = !!(sceneryRef && sceneryRef.current);
      // Menus (scenery) are a still picture: paint once per (theme, oy, transform, canvas size) and skip the other
      // frames (FE-57). Shake is cleared on the way in, otherwise a run that ended on a boss kill leaves S.shake > 0
      // (the engine stops updating, so it never decays) and the result screen jitters every frame (FE-57).
      let paint = true;
      if (view.scenery) {
        S.shake = 0;
        const key = themeKey(S, view) + '|' + view.oy + '|' + ctx.getTransform().a + '|' + cv.width + 'x' + cv.height;
        paint = key !== sceneKey; sceneKey = key;
      } else sceneKey = '';
      if (paint) try { render(ctx, S, SPR, lite, view); } catch (err) { if (!renderErr) { renderErr = true; console.error(err); } }
      hudT += dt;
      if (hudT > 0.1) { hudT = 0; updateHud(); }
      raf = requestAnimationFrame(frame);
    }
    raf = requestAnimationFrame(frame);

    // ---- input: relative drag only. The turret follows how far the finger/cursor slides from where it
    // went down; a tap without movement does nothing. Pointer + touch are both handled (idempotent). ----
    let dragging = false, hintT = 0, dragCx = 0, dragTx = 0;
    const pxToGame = dx => dx * W / cv.getBoundingClientRect().width;
    const begin = cx => { dragging = true; setHint(false); dragCx = cx; dragTx = S.turret.x; };
    const drag = cx => { if (dragging) game.setTurretX(dragTx + pxToGame(cx - dragCx)); };
    const stop = () => { dragging = false; };
    const onPointerDown = e => { begin(e.clientX); try { cv.setPointerCapture(e.pointerId); } catch (_) { /* older WebKit */ } e.preventDefault(); };
    const onPointerMove = e => { if (dragging) { drag(e.clientX); e.preventDefault(); } };
    const onTouchStart = e => { begin(e.touches[0].clientX); e.preventDefault(); };
    const onTouchMove = e => { if (dragging) drag(e.touches[0].clientX); e.preventDefault(); };
    const onTouchEnd = e => { stop(); e.preventDefault(); };
    cv.addEventListener('pointerdown', onPointerDown);
    cv.addEventListener('pointermove', onPointerMove);
    cv.addEventListener('pointerup', stop); cv.addEventListener('pointercancel', stop);
    cv.addEventListener('touchstart', onTouchStart, { passive: false });
    cv.addEventListener('touchmove', onTouchMove, { passive: false });
    cv.addEventListener('touchend', onTouchEnd, { passive: false });
    cv.addEventListener('touchcancel', stop);

    // opts: { tier: 'bronze' | 'silver', buff }; no opts = mid-run restart with the engine's own tier and buff (AMB-01)
    function start(opts) {
      if (opts) game.startGame(opts); else game.restart();
      setHint(true);
      clearTimeout(hintT); hintT = setTimeout(() => setHint(false), 3500);
    }
    startRef.current = start;
    const keys = {};
    const onKeyDown = e => {
      keys[e.key] = true;
      if ((e.key === ' ' || e.key === 'Enter') && (!S.started || S.over)) {
        if (e.target && e.target.tagName === 'INPUT' && e.key === ' ') return;   // typing a nickname
        if (startKeyRef && startKeyRef.current) startKeyRef.current();
      }
    };
    const onKeyUp = e => { keys[e.key] = false; };
    window.addEventListener('keydown', onKeyDown); window.addEventListener('keyup', onKeyUp);
    const kb = setInterval(() => {
      if (!S.started || S.over) return;
      const v = (keys.ArrowRight || keys.d ? 1 : 0) - (keys.ArrowLeft || keys.a ? 1 : 0);
      if (v) game.setTurretX(S.turret.x + v * 5);
    }, 16);

    // fast-forward helper: keep the turret out of the EXTRA boss 6 lightning column and the boss 7 purple columns
    const dodge = () => {
      const b = S.bolts[0]; if (b && S.turret.x + 16 > b.x0 && S.turret.x - 16 < b.x1) game.setTurretX(b.x0 > W / 2 ? W / 4 : W * 3 / 4);
      const shown = S.pbolts.filter(p => p.t >= 0);
      if (shown.some(p => S.turret.x + 16 > p.x0 && S.turret.x - 16 < p.x1)) {
        const w = W / 6, free = [0, 1, 2, 3, 4, 5].filter(c => !S.pbolts.some(p => p.strip === c));
        if (free.length) game.setTurretX(free[0] * w + w / 2);
      }
    };
    // dev-only inspection hook; stripped from production builds so scores cannot be forged through it
    if (import.meta.env.MODE !== 'production' && location.search.includes('debug')) window.__game = {
      start() { game.reset({ tier: S.tier, buff: S.buff }); S.started = true; sync(); },
      lose() { game.loseNow(); },
      step(sec, turretX) { if (turretX != null) S.turret.x = turretX; const n = Math.round(sec / (1 / 60)); for (let i = 0; i < n && !S.over; i++) game.update(1 / 60); render(ctx, S, SPR, lite, view); updateHud(); sync(); },
      // jump straight to the hidden-stage offer: fast-forwards a run with every boss killed on spawn and no red infantry.
      // `strong` gives an end-game build so the 150k boss can actually be fought.
      secret(strong) {
        game.reset({ tier: S.tier, buff: S.buff }); S.started = true; const reserve = S.redReserve; S.redReserve = 0;
        for (let i = 0; i < 20000 && !S.offering && !S.over; i++) {
          game.update(1 / 60);
          // each boss lives 1.5 s before it falls, so kill times and the clear time look like a real run
          for (const b of S.bosses) if (b.alive && !b.minion) { if (b.bornT === undefined) b.bornT = S.t; if (S.t - b.bornT > 1.5) game.killBoss(b); }
          if (S.resting) game.pickRest(1);
          if (S.choosing) game.pickChoice(0);
          dodge();
        }
        if (strong) { S.spread = 24; S.blueHp = 4; S.blueSpeed = 140; S.fireMul = 0.5; S.gates[1].mult = 60; }
        S.redReserve = reserve; S.red = []; render(ctx, S, SPR, lite, view); updateHud(); sync();
      },
      // EXTRA (D29-D32) fast-forward: every boss falls 1.5 s after it enters and the rest pick is taken, until boss
      // `seq` is on the field (boss 5 enters at >= 70 s, boss 6 at >= 84 s); no red infantry meanwhile (`calm`: none after either)
      extra(seq, calm) {
        game.reset({ tier: S.tier, buff: S.buff }); S.started = true; const reserve = S.redReserve; S.redReserve = 0;
        for (let i = 0; i < 20000 && !S.over; i++) {
          game.update(1 / 60);
          if (S.bosses.some(b => b.alive && !b.minion && b.idx === seq)) break;
          for (const b of S.bosses) if (b.alive && !b.minion) { if (b.bornT === undefined) b.bornT = S.t; if (S.t - b.bornT > 1.5) game.killBoss(b); }
          if (S.resting) game.pickRest(1);
          if (S.choosing) game.pickChoice(0);
          dodge();
        }
        S.redReserve = calm ? 0 : reserve; S.red = []; render(ctx, S, SPR, lite, view); updateHud(); sync();
      },
      // EXTRA boss 7 (D39) fast-forward: bosses 1-6 fall as in extra(); returns right as boss 7 appears in the centre
      boss7(calm) { this.extra(7, calm); return this.purple(); },
      // EXTRA boss 7 state: phase (center | path | arena), invulnerable seconds left, centre clock, position, current
      // full-screen round {p turret column, s safe column, order}, purple columns [strip, sweep?, t, warn, fire]
      // (t < 0 = not shown yet), arrows [x, y, angle deg, speed px/s, tail x, tail y], timers
      purple() {
        const bo = S.bosses.find(b => b.alive && b.purple);
        const A = BOSS7.arrows;
        return {
          boss: bo ? { idx: bo.idx, hp: bo.hp, max: bo.max, v: bo.v, r: bo.r, x: Math.round(bo.x), y: Math.round(bo.y), s: Math.round(bo.s), phase: bo.mode === 3 ? 'center' : bo.mode === 1 ? 'path' : 'arena', inv: +bo.inv.toFixed(3), ct: +bo.purple.ct.toFixed(3), gap: +bo.purple.gap.toFixed(3), boltT: +bo.purple.boltT.toFixed(3), arrowT: +bo.purple.arrowT.toFixed(3), round: bo.purple.round && { p: bo.purple.round.p, s: bo.purple.round.s, order: bo.purple.round.order.slice() } } : null,
          bolts: S.pbolts.map(b => [b.strip, b.sweep, +b.t.toFixed(3), b.warn, b.fire]),
          arrows: S.arrows.map(a => [Math.round(a.x), Math.round(a.y), Math.round(Math.atan2(a.uy, a.ux) * 180 / Math.PI), A.speed, Math.round(a.x - a.ux * A.len), Math.round(a.y - a.uy * A.len)]),
          // unrounded geometry of each arrow as drawn and hit-tested: [head-to-tail length px, shaft width px]
          arrowGeom: S.arrows.map(a => [+Math.hypot(a.ux * A.len, a.uy * A.len).toFixed(3), +(A.hw * 2).toFixed(3)]),
          t: S.t, over: S.over, result: S.result, kills: S.kills.slice(), death: S.death
        };
      },
      // damage through the game's own rules (ignored while the boss is invulnerable); returns whether it landed
      hit(idx, dmg) { const b = S.bosses.find(x => x.alive && x.idx === idx && !x.minion); const ok = game.hurtBoss(b, dmg); sync(); return ok; },
      // move a planned boss along its path (arc length s; 0 = mouth): lets a test walk boss 7 to the turret quickly
      bossTo(idx, s) { const b = S.bosses.find(x => x.alive && x.idx === idx && !x.minion && x.mode === 1); if (b) b.s = s; return !!b; },
      // spawn arrows from (x, y) at given angles (deg) — lets a test aim one at the turret
      arrowsAt(x, y, degs) { const A = BOSS7.arrows; for (const d of degs) { const a = d * Math.PI / 180, ux = Math.cos(a), uy = Math.sin(a); S.arrows.push({ x: x + ux * A.len, y: y + uy * A.len, ux, uy, t0: S.t }); } },
      // EXTRA boss 6 lightning state: current column (firing or warning), columns still marked on the field (only the
      // current one, FE-41), orbs [x, y, age, angle deg, speed px/s]
      storm() { const b = S.bolts[0]; return { bolt: b ? { strip: b.strip, x0: b.x0, x1: b.x1, firing: b.t >= b.warn, t: +b.t.toFixed(2) } : null, marked: S.bolts.map(x => x.strip), orbs: S.orbs.map(o => [Math.round(o.x), Math.round(o.y), +o.t.toFixed(2), Math.round(Math.atan2(o.vy, o.vx) * 180 / Math.PI), Math.round(Math.hypot(o.vx, o.vy))]), beams: S.beams.length, t: S.t, over: S.over }; },
      // soldiers-per-shot cap checks (FE-27): set spread, hang/roll/break a crate on an hp gate (index 0 or 2)
      spread(n) { if (n != null) S.spread = n; return S.spread; }, gateAbility(i, ab) { if (ab) S.gates[i].ability = ab; return S.gates[i].ability; },
      rollGate(i) { game.rebuildGate(S.gates[i]); return S.gates[i].ability; },
      breakGate(i) { const g = S.gates[i]; g.hp = 0; g.dead = true; g.respawnT = 999; game.onReward(g); render(ctx, S, SPR, lite, view); sync(); },
      // current visual theme key ('bronze' forest | 'silver' cloud sky, FE-52)
      theme() { return themeKey(S, view); },
      turretX() { return S.turret.x; }, loadout() { return { tier: S.tier, buff: S.buff, fireMin: S.fireMin, cannon: S.cannon.level, laser: !!S.laser }; }, lite() { return lite; }, choose(i) { game.pickChoice(i); }, mystery(a, b) { game.openChoice(a && b ? [a, b] : null); }, choice() { return S.choosing ? S.choice : null; }, rest(i) { game.pickRest(i); }, openRest() { game.openRest(); }, offer(yes) { yes ? game.acceptOffer() : game.declineOffer(); }, bonus(i) { game.pickBonus(i); }, bonusCards() { return S.bonusing ? S.bonus : null; }, hits(x) { return S.beams.some(b => game.beamHits(b, x, S.turret.y, 16)); }, hurt(idx, dmg) { for (const b of S.bosses) if (b.alive && b.idx === idx) { b.hp -= dmg; if (b.hp <= 0) game.killBoss(b); } }, cannon() { return S.cannon.level; },
      bench(n) { let t0 = performance.now(); for (let i = 0; i < n; i++) game.update(1 / 60); const up = (performance.now() - t0) / n; t0 = performance.now(); for (let i = 0; i < n; i++) render(ctx, S, SPR, lite, view); return { update: +up.toFixed(2), render: +((performance.now() - t0) / n).toFixed(2) }; },
      state() { let minS = 1e9, arena = 0, maxB = -1; for (const r of S.red) { if (r.mode === 0) arena++; else if (r.s < minS) minS = r.s; } for (const u of S.blue) if (u.mode === 1 && u.s > maxB) maxB = u.s; return { minRedS: Math.round(minS), redArena: arena, maxBlueS: Math.round(maxB), shots: S.blue.filter(u => u.mode === 2).length, t: S.t, over: S.over, result: S.result, blue: S.blue.length, red: S.red.length, reserve: S.redReserve, spread: S.spread, armor: S.blueHp, speed: S.blueSpeed, fireMul: +S.fireMul.toFixed(3), kills: S.bossKills, choosing: S.choosing, resting: S.resting, cannon: S.cannon.level, stage: S.stage, clearT: +S.clearT.toFixed(1), beams: S.beams.map(b => b.kind + ':' + b.t.toFixed(2)), gateX: S.gates.map(g => [g.x0, g.x1]), minions: S.bosses.filter(b => b.minion && b.alive).length, gates: S.gates.map(g => g.kind === 'hp' ? g.hp + '/' + g.max + ':' + g.ability + ':g' + g.gen : 'x' + g.mult), bosses: S.bosses.filter(b => b.alive && !b.minion).map(b => [b.idx, b.hp, b.mode, Math.round(b.s)]) }; }
    };

    return () => {
      cancelAnimationFrame(raf); clearInterval(kb); clearTimeout(hintT);
      if (ro) ro.disconnect();
      window.removeEventListener('resize', resize);
      window.removeEventListener('keydown', onKeyDown); window.removeEventListener('keyup', onKeyUp);
      cv.removeEventListener('pointerdown', onPointerDown); cv.removeEventListener('pointermove', onPointerMove);
      cv.removeEventListener('pointerup', stop); cv.removeEventListener('pointercancel', stop);
      cv.removeEventListener('touchstart', onTouchStart); cv.removeEventListener('touchmove', onTouchMove);
      cv.removeEventListener('touchend', onTouchEnd); cv.removeEventListener('touchcancel', stop);
      if (window.__game) delete window.__game;
      gameRef.current = null; startRef.current = null;
    };
  }, [canvasRef, appRef, stageRef]);

  return {
    ui, hint,
    start: opts => startRef.current && startRef.current(opts),
    // same run, same loadout buff, clock back to 0; never a new createRun (AMB-01, FE-43)
    restart: () => startRef.current && startRef.current(null),
    // what the result screen and the settlement need once a run is over
    summary: () => {
      const g = gameRef.current;
      if (!g) return null;
      const S = g.S;
      return { tier: S.tier, buff: S.buff, result: S.result, t: S.t, clearT: S.clearT, kills: S.kills.slice(), death: S.death };
    },
    pause: v => gameRef.current && gameRef.current.setPaused(v),
    pickChoice: i => gameRef.current && gameRef.current.pickChoice(i),
    pickRest: i => gameRef.current && gameRef.current.pickRest(i),
    acceptOffer: () => gameRef.current && gameRef.current.acceptOffer(),
    declineOffer: () => gameRef.current && gameRef.current.declineOffer(),
    pickBonus: i => gameRef.current && gameRef.current.pickBonus(i)
  };
}
