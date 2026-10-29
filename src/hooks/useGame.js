import { useEffect, useRef, useState } from 'react';
import { createGame } from '../game/engine.js';
import { render } from '../game/render.js';
import { loadSprites } from '../game/tiles.js';
import { W, H } from '../game/constants.js';

const phaseOf = S => !S.started ? 'intro' : S.over ? 'end' : S.choosing ? 'choice' : S.resting ? 'rest' : 'play';
const snapshot = S => ({
  phase: phaseOf(S), result: S.result, time: S.t.toFixed(0), choiceCards: S.choiceCards,
  hud: { blue: S.blue.length, red: S.red.length, t: S.t.toFixed(0), kills: S.bossKills }
});

// Owns the engine instance, the canvas loop, sizing and input. React only sees a small `ui` snapshot.
export function useGame({ canvasRef, appRef, stageRef }) {
  const gameRef = useRef(null), startRef = useRef(null);
  const [ui, setUi] = useState({ phase: 'intro', result: null, time: '0', choiceCards: null, hud: { blue: 0, red: 0, t: '0', kills: 0 } });
  const [hint, setHint] = useState(true);

  useEffect(() => {
    const cv = canvasRef.current, ctx = cv.getContext('2d');
    let game;
    const sync = () => setUi(snapshot(game.S));
    game = createGame(sync); gameRef.current = game;
    const S = game.S;
    const SPR = loadSprites();

    // ---- sizing; `lite` drops to dpr 1 + squares after sustained slow frames ----
    let scale = 1, lite = false, slowN = 0;
    function resize() {
      const app = appRef.current, stage = stageRef.current;
      scale = Math.min(app.clientWidth / W, app.clientHeight / H);
      const dpr = Math.min(window.devicePixelRatio || 1, lite ? 1 : 2);
      cv.style.width = (W * scale) + 'px'; cv.style.height = (H * scale) + 'px';
      cv.width = Math.round(W * scale * dpr); cv.height = Math.round(H * scale * dpr);
      stage.style.width = cv.style.width; stage.style.height = cv.style.height;
      ctx.setTransform(scale * dpr, 0, 0, scale * dpr, 0, 0);
    }
    window.addEventListener('resize', resize); resize();

    // ---- loop ----
    let last = performance.now(), hudT = 0, renderErr = false, raf = 0;
    function frame(now) {
      const dt = Math.min(0.05, (now - last) / 1000); last = now;
      if (S.started && !S.over && dt > 0.045) { if (++slowN > 45 && !lite) { lite = true; resize(); } } else slowN = Math.max(0, slowN - 1);
      game.update(dt);
      try { render(ctx, S, SPR, lite); } catch (err) { if (!renderErr) { renderErr = true; console.error(err); } }
      hudT += dt;
      if (hudT > 0.1) { hudT = 0; sync(); }
      raf = requestAnimationFrame(frame);
    }
    raf = requestAnimationFrame(frame);

    // ---- input: finger/cursor x maps directly to the turret x; pointer + touch both handled ----
    let dragging = false, hintT = 0;
    const clientToGame = cx => { const r = cv.getBoundingClientRect(); return (cx - r.left) / r.width * W; };
    const begin = cx => { dragging = true; setHint(false); game.setTurretX(clientToGame(cx)); };
    const stop = () => { dragging = false; };
    const onPointerDown = e => { begin(e.clientX); try { cv.setPointerCapture(e.pointerId); } catch (_) { /* older WebKit */ } e.preventDefault(); };
    const onPointerMove = e => { if (dragging) { game.setTurretX(clientToGame(e.clientX)); e.preventDefault(); } };
    const onTouchStart = e => { begin(e.touches[0].clientX); e.preventDefault(); };
    const onTouchMove = e => { if (dragging) game.setTurretX(clientToGame(e.touches[0].clientX)); e.preventDefault(); };
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
    const onKeyDown = e => { keys[e.key] = true; if ((e.key === ' ' || e.key === 'Enter') && (!S.started || S.over)) start(); };
    const onKeyUp = e => { keys[e.key] = false; };
    window.addEventListener('keydown', onKeyDown); window.addEventListener('keyup', onKeyUp);
    const kb = setInterval(() => {
      if (!S.started || S.over) return;
      const v = (keys.ArrowRight || keys.d ? 1 : 0) - (keys.ArrowLeft || keys.a ? 1 : 0);
      if (v) game.setTurretX(S.turret.x + v * 5);
    }, 16);

    if (location.search.includes('debug')) window.__game = {
      start() { game.reset(); S.started = true; sync(); },
      step(sec, turretX) { if (turretX != null) S.turret.x = turretX; const n = Math.round(sec / (1 / 60)); for (let i = 0; i < n && !S.over; i++) game.update(1 / 60); render(ctx, S, SPR, lite); sync(); },
      turretX() { return S.turret.x; }, lite() { return lite; }, choose(i) { game.pickChoice(i); }, mystery(a, b) { game.openChoice(a && b ? [a, b] : null); }, choice() { return S.choosing ? S.choice : null; }, rest(i) { game.pickRest(i); }, openRest() { game.openRest(); }, hurt(idx, dmg) { for (const b of S.bosses) if (b.alive && b.idx === idx) { b.hp -= dmg; if (b.hp <= 0) game.killBoss(b); } }, cannon() { return S.cannon.level; },
      bench(n) { let t0 = performance.now(); for (let i = 0; i < n; i++) game.update(1 / 60); const up = (performance.now() - t0) / n; t0 = performance.now(); for (let i = 0; i < n; i++) render(ctx, S, SPR, lite); return { update: +up.toFixed(2), render: +((performance.now() - t0) / n).toFixed(2) }; },
      state() { let minS = 1e9, arena = 0, maxB = -1; for (const r of S.red) { if (r.mode === 0) arena++; else if (r.s < minS) minS = r.s; } for (const u of S.blue) if (u.mode === 1 && u.s > maxB) maxB = u.s; return { minRedS: Math.round(minS), redArena: arena, maxBlueS: Math.round(maxB), shots: S.blue.filter(u => u.mode === 2).length, t: S.t, over: S.over, result: S.result, blue: S.blue.length, red: S.red.length, reserve: S.redReserve, spread: S.spread, armor: S.blueHp, speed: S.blueSpeed, kills: S.bossKills, choosing: S.choosing, resting: S.resting, cannon: S.cannon.level, gateX: S.gates.map(g => [g.x0, g.x1]), minions: S.bosses.filter(b => b.minion && b.alive).length, gates: S.gates.map(g => g.kind === 'hp' ? g.hp + '/' + g.max + ':' + g.ability + ':g' + g.gen : 'x' + g.mult), bosses: S.bosses.filter(b => b.alive && !b.minion).map(b => [b.idx, b.hp, b.mode, Math.round(b.s)]) }; }
    };

    return () => {
      cancelAnimationFrame(raf); clearInterval(kb); clearTimeout(hintT);
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
    pickChoice: i => gameRef.current && gameRef.current.pickChoice(i),
    pickRest: i => gameRef.current && gameRef.current.pickRest(i)
  };
}
