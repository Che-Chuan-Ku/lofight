import { useEffect, useRef, useState } from 'react';
import { createGame } from '../game/engine.js';
import { render } from '../game/render.js';
import { loadSprites } from '../game/tiles.js';
import { W, H, MAX_EXTRA_H, MAX_CROP_H } from '../game/constants.js';

const phaseOf = S => !S.started ? 'intro' : S.over ? 'end' : S.paused ? 'paused' : S.choosing ? 'choice' : S.resting ? 'rest' : S.offering ? 'offer' : S.bonusing ? 'bonus' : 'play';
const snapshot = S => ({ phase: phaseOf(S), result: S.result, time: S.t.toFixed(0), clearTime: S.clearT.toFixed(0), choiceCards: S.choiceCards, bonusCards: S.bonusCards });

// Owns the engine instance, the canvas loop, sizing and input. React only sees a small `ui` snapshot.
export function useGame({ canvasRef, appRef, stageRef, startKeyRef }) {
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
    // (more red sea, up to MAX_EXTRA_H); shorter portrait viewports crop the top (up to MAX_CROP_H).
    // Either way a phone fills the whole screen; landscape/desktop simply fits the height.
    let scale = 1, lite = false, slowN = 0;
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
      try { render(ctx, S, SPR, lite, view); } catch (err) { if (!renderErr) { renderErr = true; console.error(err); } }
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

    function start() {
      game.startGame(); setHint(true);
      clearTimeout(hintT); hintT = setTimeout(() => setHint(false), 3500);
    }
    startRef.current = start;
    const keys = {};
    const onKeyDown = e => {
      keys[e.key] = true;
      if ((e.key === ' ' || e.key === 'Enter') && (!S.started || S.over)) {
        if (e.target && e.target.tagName === 'INPUT' && e.key === ' ') return;   // typing a nickname
        if (startKeyRef && startKeyRef.current) startKeyRef.current(); else start();
      }
    };
    const onKeyUp = e => { keys[e.key] = false; };
    window.addEventListener('keydown', onKeyDown); window.addEventListener('keyup', onKeyUp);
    const kb = setInterval(() => {
      if (!S.started || S.over) return;
      const v = (keys.ArrowRight || keys.d ? 1 : 0) - (keys.ArrowLeft || keys.a ? 1 : 0);
      if (v) game.setTurretX(S.turret.x + v * 5);
    }, 16);

    // dev-only inspection hook; stripped from production builds so scores cannot be forged through it
    if (import.meta.env.MODE !== 'production' && location.search.includes('debug')) window.__game = {
      start() { game.reset(); S.started = true; sync(); },
      step(sec, turretX) { if (turretX != null) S.turret.x = turretX; const n = Math.round(sec / (1 / 60)); for (let i = 0; i < n && !S.over; i++) game.update(1 / 60); render(ctx, S, SPR, lite, view); updateHud(); sync(); },
      // jump straight to the hidden-stage offer: fast-forwards a run with every boss killed on spawn and no red infantry.
      // `strong` gives an end-game build so the 150k boss can actually be fought.
      secret(strong) {
        game.reset(); S.started = true; const reserve = S.redReserve; S.redReserve = 0;
        for (let i = 0; i < 20000 && !S.offering && !S.over; i++) {
          game.update(1 / 60);
          for (const b of S.bosses) if (b.alive && !b.minion) game.killBoss(b);
          if (S.resting) game.pickRest(1);
          if (S.choosing) game.pickChoice(0);
        }
        if (strong) { S.spread = 24; S.blueHp = 4; S.blueSpeed = 140; S.fireMul = 0.5; S.gates[1].mult = 60; S.cannon.level = 2; }
        S.redReserve = reserve; S.red = []; render(ctx, S, SPR, lite, view); updateHud(); sync();
      },
      turretX() { return S.turret.x; }, lite() { return lite; }, choose(i) { game.pickChoice(i); }, mystery(a, b) { game.openChoice(a && b ? [a, b] : null); }, choice() { return S.choosing ? S.choice : null; }, rest(i) { game.pickRest(i); }, openRest() { game.openRest(); }, offer(yes) { yes ? game.acceptOffer() : game.declineOffer(); }, bonus(i) { game.pickBonus(i); }, bonusCards() { return S.bonusing ? S.bonus : null; }, hits(x) { return S.beams.some(b => game.beamHits(b, x, S.turret.y, 16)); }, hurt(idx, dmg) { for (const b of S.bosses) if (b.alive && b.idx === idx) { b.hp -= dmg; if (b.hp <= 0) game.killBoss(b); } }, cannon() { return S.cannon.level; },
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
    start: () => startRef.current && startRef.current(),
    pause: v => gameRef.current && gameRef.current.setPaused(v),
    pickChoice: i => gameRef.current && gameRef.current.pickChoice(i),
    pickRest: i => gameRef.current && gameRef.current.pickRest(i),
    acceptOffer: () => gameRef.current && gameRef.current.acceptOffer(),
    declineOffer: () => gameRef.current && gameRef.current.declineOffer(),
    pickBonus: i => gameRef.current && gameRef.current.pickBonus(i)
  };
}
