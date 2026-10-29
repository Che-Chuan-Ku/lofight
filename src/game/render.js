// Canvas drawing for one frame. Pure function of the state; `lite` drops sprites for slow devices.
// `view.oy` shifts the world: positive adds red sea above the battlefield (tall screens), negative crops the top.
import { W, H, GATE_Y, GATE_H, PATH_HW, GATE_RESPAWN, UNIT_PX } from './constants.js';
import { PL, PATH_LEN, pathPos } from './path.js';
import { ABILITIES } from './abilities.js';

export function render(ctx, S, SPR, lite, view) {
  const oy = view ? view.oy : 0;
  function roundRect(x, y, w, h, r) { ctx.beginPath(); ctx.roundRect(x, y, w, h, r); }
  function drawCrowd(list, color, shade, spr) {
    // The path column saturates long before the unit cap, so above `target` only every
    // `stride`-th path unit is drawn (chosen by the unit's own phase, so the sample is stable).
    const n = list.length, target = lite ? 700 : 2200, stride = n > target ? Math.ceil(n / target) : 1;
    const pick = u => stride === 1 || u.mode !== 1 || ((u.w * 1000) | 0) % stride === 0;
    if (!SPR.ready || lite) {
      ctx.fillStyle = color;
      for (const u of list) if (pick(u)) ctx.fillRect(u.x - 2, u.y - 2, 4, 4);
      ctx.fillStyle = shade;
      for (const u of list) if (pick(u)) ctx.fillRect(u.x - 2, u.y - 2, 4, 1);
      return;
    }
    const h = UNIT_PX / 2, ft = (S.t * 7) | 0;
    for (const u of list) { if (!pick(u)) continue; const f = (ft + ((u.x + u.y) * 0.37 | 0)) & 1; ctx.drawImage(spr[f], (u.x - h) | 0, (u.y - h) | 0); }
  }
  const sx = S.shake ? (Math.random() - 0.5) * 6 * S.shake : 0, sy = S.shake ? (Math.random() - 0.5) * 6 * S.shake : 0;
  ctx.save(); ctx.translate(sx, sy + oy);
  // cliffs
  ctx.fillStyle = '#3d4148'; ctx.fillRect(-10, -10 - oy, W + 20, GATE_Y + 20 + oy);
  ctx.fillStyle = '#2d3036';
  for (let i = 0; i < 9; i++) ctx.fillRect(-10, i * 52 - 10, W + 20, 6);
  // arena ground with grass rim
  ctx.fillStyle = '#5c8f3a'; ctx.fillRect(-10, GATE_Y + GATE_H, W + 20, H);
  ctx.fillStyle = '#d9b97c'; ctx.fillRect(10, GATE_Y + GATE_H + 8, W - 20, H);
  ctx.fillStyle = 'rgba(0,0,0,.08)';
  for (let y = GATE_Y + GATE_H + 30; y < H; y += 44) ctx.fillRect(10, y, W - 20, 2);
  // path (rock rim + sand)
  ctx.lineJoin = 'round'; ctx.lineCap = 'round';
  ctx.beginPath(); ctx.moveTo(PL[0][0], PL[0][1]);
  for (let i = 1; i < PL.length; i++) ctx.lineTo(PL[i][0], PL[i][1]);
  ctx.strokeStyle = '#1e2025'; ctx.lineWidth = PATH_HW * 2 + 16; ctx.stroke();
  ctx.strokeStyle = '#6f9a44'; ctx.lineWidth = PATH_HW * 2 + 7; ctx.stroke();
  ctx.strokeStyle = '#cdb67e'; ctx.lineWidth = PATH_HW * 2; ctx.stroke();
  // red sea at the top edge (reserve)
  ctx.fillStyle = '#c21f28'; ctx.fillRect(-10, -10 - oy, W + 20, 36 + oy);
  ctx.fillStyle = '#e03a42';
  for (let x = 0; x < W; x += 5) for (let y = 2 - oy; y < 24; y += 5) if (((x * 7 + y * 13) % 11) < 5) ctx.fillRect(x, y, 2, 2);
  ctx.fillStyle = '#7a0f15';
  for (let x = 0; x < W; x += 9) ctx.fillRect(x, 26 + (x % 3), 5, 2);

  // red sea: solid fill only where the path actually holds reds, so shell craters open real gaps
  if (S.red.length) {
    const SEG = 6, nseg = Math.ceil(PATH_LEN / SEG) + 1, occ = new Uint16Array(nseg);
    for (const r of S.red) if (r.mode === 1) occ[Math.max(0, Math.min(nseg - 1, (r.s / SEG) | 0))]++;
    ctx.beginPath();
    let run = -1;
    for (let k = 0; k <= nseg; k++) {
      const on = k < nseg && (occ[k] > 0 || (k > 0 && k + 1 < nseg && occ[k - 1] > 0 && occ[k + 1] > 0));
      if (on && run < 0) run = k;
      if (!on && run >= 0) {
        const s0 = Math.min(PATH_LEN, run * SEG + 8), s1 = Math.min(PATH_LEN, k * SEG);
        if (s1 > s0) {
          let p = pathPos(s0, 0); ctx.moveTo(p[0], p[1]);
          for (let q = s0 + 6; q < s1; q += 6) { p = pathPos(q, 0); ctx.lineTo(p[0], p[1]); }
          p = pathPos(s1, 0); ctx.lineTo(p[0], p[1]);
        }
        run = -1;
      }
    }
    ctx.strokeStyle = '#c21f28'; ctx.lineWidth = PATH_HW * 2 - 2; ctx.stroke();
    ctx.strokeStyle = '#a3141c'; ctx.setLineDash([2, 7]); ctx.lineDashOffset = -S.t * 20; ctx.stroke(); ctx.setLineDash([]);
  }
  // units
  drawCrowd(S.red, '#d6242c', '#f27079', SPR.red);
  drawCrowd(S.blue, '#2f66e0', '#8fb3ff', SPR.blue);

  // gates
  for (const g of S.gates) {
    const w = g.x1 - g.x0;
    if (g.kind === 'mult') {
      ctx.fillStyle = '#b8860b'; roundRect(g.x0, GATE_Y, w, GATE_H, 4); ctx.fill();
      ctx.fillStyle = '#f2c233'; roundRect(g.x0 + 2, GATE_Y + 2, w - 4, GATE_H - 8, 3); ctx.fill();
      ctx.fillStyle = '#fff'; ctx.fillRect(g.x0 - 5, GATE_Y - 6, 7, GATE_H + 12); ctx.fillRect(g.x1 - 2, GATE_Y - 6, 7, GATE_H + 12);
      ctx.fillStyle = '#2b1d00'; ctx.font = 'bold 22px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText('x' + g.mult, g.x0 + w / 2, GATE_Y + GATE_H / 2 - 1);
    } else if (!g.dead) {
      // weapon crate above the bar
      const cx = g.x0 + w / 2, top = GATE_Y - 40, ab = ABILITIES[g.ability];
      ctx.fillStyle = '#8c6a2a'; roundRect(cx - 28, top, 56, 42, 6); ctx.fill();
      const cols = Math.min(2 + g.gen, 4), rows = Math.min(2 + (g.gen >> 1), 3), r0 = cols > 3 ? 5 : 6;
      for (let i = 0; i < cols; i++) for (let j = 0; j < rows; j++) {
        const bx = cx - (cols - 1) * (r0 + 2) + i * (r0 * 2 + 4), by = top + 9 + j * (r0 * 2 + 1);
        ctx.fillStyle = ab.color; ctx.beginPath(); ctx.arc(bx, by, r0, 0, 6.28); ctx.fill();
        ctx.fillStyle = '#1a1a1a'; ctx.beginPath(); ctx.arc(bx, by, r0 * 0.45, 0, 6.28); ctx.fill();
      }
      ctx.fillStyle = ab.color; ctx.font = 'bold 14px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.lineWidth = 3; ctx.strokeStyle = '#2b1d00'; ctx.strokeText(ab.tag, cx, top - 10); ctx.fillText(ab.tag, cx, top - 10);
      ctx.fillStyle = '#7a1a1a'; roundRect(g.x0, GATE_Y, w, GATE_H, 4); ctx.fill();
      ctx.fillStyle = '#f2c233'; roundRect(g.x0 + 2, GATE_Y + 2, (w - 4) * (g.hp / g.max), GATE_H - 8, 3); ctx.fill();
      ctx.fillStyle = '#fff'; ctx.font = 'bold 20px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.lineWidth = 3; ctx.strokeStyle = '#2b1d00'; ctx.strokeText(String(g.hp), cx, GATE_Y + GATE_H / 2 - 1); ctx.fillText(String(g.hp), cx, GATE_Y + GATE_H / 2 - 1);
    } else {
      // open gate rebuilding: dark slot with a progress bar
      ctx.fillStyle = 'rgba(0,0,0,.35)'; roundRect(g.x0, GATE_Y, w, GATE_H, 4); ctx.fill();
      const f = 1 - g.respawnT / GATE_RESPAWN;
      ctx.fillStyle = '#f2c233'; roundRect(g.x0 + 2, GATE_Y + GATE_H - 8, (w - 4) * f, 5, 2); ctx.fill();
      ctx.fillStyle = '#fff'; ctx.font = 'bold 13px "Noto Sans TC", "PingFang TC", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    }
  }

  // bosses
  for (const bo of S.bosses) {
    if (!bo.alive) continue;
    const R = bo.r;
    const bset = bo.minion ? SPR.minion : SPR.bosses[bo.idx - 1];   // idx is 1-based
    if (SPR.ready && !lite && bset) {
      const im = bset[bo.hitT > 0 ? 1 : 0];
      const bob = Math.abs(Math.sin(S.t * 9)) * 3;
      ctx.fillStyle = 'rgba(0,0,0,.3)'; ctx.beginPath(); ctx.ellipse(bo.x, bo.y + R - 2, R, R * 0.35, 0, 0, 6.28); ctx.fill();
      ctx.drawImage(im, (bo.x - im.width / 2) | 0, (bo.y - im.height / 2 - bob) | 0);
    } else {
    ctx.save(); ctx.translate(bo.x, bo.y);
    ctx.fillStyle = bo.hitT > 0 ? '#ffd0d0' : '#8e1016';
    ctx.beginPath(); ctx.arc(0, 0, R, 0, 6.28); ctx.fill();
    ctx.fillStyle = bo.hitT > 0 ? '#fff' : '#d6242c';
    ctx.beginPath(); ctx.arc(0, -2, R * 0.68, 0, 6.28); ctx.fill();
    ctx.fillStyle = '#e6e6e6';
    const spikes = 5 + bo.idx;
    for (let i = 0; i < spikes; i++) { const a = i / spikes * 6.28 + S.t * 2; ctx.beginPath(); ctx.moveTo(Math.cos(a) * R, Math.sin(a) * R); ctx.lineTo(Math.cos(a) * (R + 8), Math.sin(a) * (R + 8)); ctx.lineTo(Math.cos(a + .4) * (R - 1), Math.sin(a + .4) * (R - 1)); ctx.fill(); }
    ctx.restore();
    }
    const bw = bo.minion ? 22 : 40 + bo.idx * 4;
    ctx.fillStyle = '#111'; roundRect(bo.x - bw / 2 - 2, bo.y - R - 20, bw + 4, 9, 3); ctx.fill();
    ctx.fillStyle = '#ff3b3b'; roundRect(bo.x - bw / 2, bo.y - R - 18, bw * (bo.hp / bo.max), 5, 2); ctx.fill();
    ctx.fillStyle = '#fff'; ctx.font = 'bold 11px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'bottom';
    ctx.fillText(bo.hp >= 10000 ? (bo.hp / 1000).toFixed(1) + 'k' : String(bo.hp), bo.x, bo.y - R - 21);
  }

  // turret
  const T = S.turret;
  ctx.save(); ctx.translate(T.x, T.y);
  ctx.fillStyle = 'rgba(0,0,0,.25)'; ctx.beginPath(); ctx.ellipse(0, 10, 22, 8, 0, 0, 6.28); ctx.fill();
  ctx.fillStyle = '#6b4a1f'; roundRect(-18, -2, 36, 16, 4); ctx.fill();
  ctx.fillStyle = '#2f66e0'; roundRect(-16, -14, 32, 18, 5); ctx.fill();
  ctx.fillStyle = '#8fb3ff'; roundRect(-13, -12, 26, 5, 2); ctx.fill();
  const barrels = Math.min(S.spread, 8);
  for (let i = 0; i < barrels; i++) {
    const ox = (i - (barrels - 1) / 2) * (barrels > 3 ? 6 : 9);
    const bw = barrels > 3 ? 5 : 7;
    ctx.fillStyle = '#1d3f8f'; roundRect(ox - bw / 2, -34, bw, 24, 2); ctx.fill();
    ctx.fillStyle = '#9ec1ff'; ctx.fillRect(ox - 1, -34, 2, 20);
  }
  // field cannon parked behind the launcher: carriage, two wheels, barrel with recoil and muzzle flash
  if (S.cannon.level > 0) {
    const lv = S.cannon.level, k = S.cannon.flash / 0.16, rec = k * 7, bw = 12 + lv * 2, bl = 30 + lv * 3;
    ctx.fillStyle = 'rgba(0,0,0,.25)'; ctx.beginPath(); ctx.ellipse(0, 46, 24, 7, 0, 0, 6.28); ctx.fill();
    ctx.fillStyle = '#6b4a1f'; roundRect(-15, 30, 30, 11, 3); ctx.fill();
    for (const wx of [-13, 13]) {
      ctx.fillStyle = '#3b2a12'; ctx.beginPath(); ctx.arc(wx, 40, 8, 0, 6.28); ctx.fill();
      ctx.fillStyle = '#9c7a3c'; ctx.beginPath(); ctx.arc(wx, 40, 5, 0, 6.28); ctx.fill();
      ctx.strokeStyle = '#3b2a12'; ctx.lineWidth = 1.5; ctx.beginPath();
      for (let i = 0; i < 3; i++) { const a = i * Math.PI / 3 + S.t * 2; ctx.moveTo(wx - Math.cos(a) * 5, 40 - Math.sin(a) * 5); ctx.lineTo(wx + Math.cos(a) * 5, 40 + Math.sin(a) * 5); }
      ctx.stroke();
    }
    ctx.fillStyle = '#1e1e22'; roundRect(-bw / 2, 34 - bl + rec, bw, bl, bw / 2); ctx.fill();
    ctx.fillStyle = '#4a4a52'; roundRect(-bw / 2 - 2, 34 - bl + rec + 4, bw + 4, 6, 3); ctx.fill();
    ctx.fillStyle = '#4a4a52'; roundRect(-bw / 2 - 1, 26 + rec, bw + 2, 5, 2); ctx.fill();
    ctx.fillStyle = '#7a7a84'; ctx.fillRect(-bw / 2 + 2, 34 - bl + rec + 2, 2, bl - 10);
    if (k > 0) {
      ctx.fillStyle = 'rgba(255,170,60,' + (0.9 * k) + ')'; ctx.beginPath(); ctx.arc(0, 34 - bl + rec - 4, 8 + 10 * k, 0, 6.28); ctx.fill();
      ctx.fillStyle = 'rgba(255,240,160,' + (0.9 * k) + ')'; ctx.beginPath(); ctx.arc(0, 34 - bl + rec - 4, 4 + 5 * k, 0, 6.28); ctx.fill();
    }
  }
  if (T.flash > 0) { ctx.fillStyle = 'rgba(255,240,160,.9)'; ctx.beginPath(); ctx.arc(0, -36, 9 + barrels * 2, 0, 6.28); ctx.fill(); }
  ctx.restore();

  // shells + blast rings
  for (const sh of S.shells) {
    ctx.fillStyle = 'rgba(0,0,0,.25)'; ctx.beginPath(); ctx.ellipse(sh.x, sh.y0 + (sh.y1 - sh.y0) * Math.min(1, sh.t / sh.T) + 4, 6, 3, 0, 0, 6.28); ctx.fill();
    ctx.fillStyle = '#1a1a1a'; ctx.beginPath(); ctx.arc(sh.x, sh.y, 6, 0, 6.28); ctx.fill();
    ctx.fillStyle = '#ff9a3c'; ctx.beginPath(); ctx.arc(sh.x - 2, sh.y - 2, 2, 0, 6.28); ctx.fill();
  }
  for (const b of S.booms) {
    const k = b.t / 0.25; ctx.globalAlpha = 1 - k; ctx.lineWidth = 4; ctx.strokeStyle = '#ffcf5a';
    ctx.beginPath(); ctx.arc(b.x, b.y, b.R * (0.4 + 0.6 * k), 0, 6.28); ctx.stroke();
  }
  ctx.globalAlpha = 1;
  // fx
  for (const f of S.fx) { ctx.globalAlpha = 1 - f.t / f.life; ctx.fillStyle = f.color; ctx.fillRect(f.x - 1.5, f.y - 1.5, 3, 3); }
  ctx.globalAlpha = 1;
  // popups
  for (const p of S.pops) {
    const a = p.t < 0.9 ? 1 : 1 - (p.t - 0.9) / 0.3;
    ctx.globalAlpha = Math.max(0, a);
    ctx.font = 'bold 30px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.lineWidth = 5; ctx.strokeStyle = '#2b1d00'; ctx.fillStyle = p.color;
    // popups spawned near the top stay below the HUD when the top is cropped
    const y = (p.y < GATE_Y ? p.y + Math.max(0, -oy) : p.y) - p.t * 70;
    ctx.strokeText(p.text, p.x, y); ctx.fillText(p.text, p.x, y);
    if (p.sub) {
      ctx.font = 'bold 15px "Noto Sans TC", "PingFang TC", sans-serif'; ctx.lineWidth = 4; ctx.fillStyle = '#fff';
      ctx.strokeText(p.sub, p.x, y + 24); ctx.fillText(p.sub, p.x, y + 24);
    }
  }
  ctx.globalAlpha = 1;
  ctx.restore();
}
