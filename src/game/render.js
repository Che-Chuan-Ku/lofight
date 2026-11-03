// Canvas drawing for one frame. Pure function of the state; `lite` drops sprites for slow devices.
// `view.oy` shifts the world: positive adds sky / canopy above the battlefield (tall screens), negative crops the top.
import { W, H, GATE_Y, GATE_H, PATH_HW, GATE_RESPAWN, UNIT_PX, SECRET_BOSS, LASER, MAX_EXTRA_H, STORM, BOSS7 } from './constants.js';
import { PL, PATH_LEN, pathPos } from './path.js';
import { ABILITIES, spreadMaxed } from './abilities.js';

// Per-tier visual theme (FE-52): NORMAL (bronze) = pixel forest, EXTRA (silver) = misty cloud sky.
// Only colours / background / decoration differ; geometry (path, gates, turret zone, hit areas, HUD) is shared.
export const THEME = {
  bronze: {
    upper: ['#1d3320'],                      // forest floor in shade (single colour)
    horizon: '#0b170d',                      // dark tree line at the top edge, reds come out of it
    moss: '#24402a',
    tree: { shadow: 'rgba(0,0,0,.28)', trunk: '#4a2f18', dark: '#25502a', mid: '#347036', light: '#55934a' },
    gateBand: '#2a1c10',
    arenaRim: '#2f4f22', arena: '#5f8a3e', arenaLine: 'rgba(0,0,0,.08)', tuft: '#77a64f',
    pathEdge: '#2a1a0c', pathRim: '#4d7a2c', path: '#9a7044', pathSeam: 'rgba(58,34,14,.45)',
    gate: { multFrame: '#6b4420', multPost: '#c89b5e', crate: '#7a5526', hpBack: '#5c2412' },
    clouds: 0, fog: null, stars: false
  },
  silver: {
    upper: ['#121634', '#2c3272', '#5a60a8'],  // night-indigo sky down to a lilac haze at the gates
    horizon: '#070a1f',
    cloud: 'rgba(214,220,255,.30)', cloudHi: 'rgba(240,242,255,.35)', star: '#e8e6ff',
    gateBand: '#262a5c',
    arenaRim: '#5a5398', arena: '#868cc0', arenaLine: 'rgba(40,40,100,.10)', tuft: 'rgba(255,255,255,.18)',
    pathEdge: 'rgba(200,206,255,.35)', pathRim: '#8a8fd0', path: '#dfe2f7', pathSeam: 'rgba(90,90,150,.35)',
    gate: { multFrame: '#6c63a6', multPost: '#e4e0fb', crate: '#5e5a96', hpBack: '#3f2a6b' },
    clouds: 5, fog: 'rgba(225,230,255,', stars: true,
    // boss 6 lightning / orbs: dark outlines keep the violet readable on both the dark sky and the pale platform
    boltWarn: 'rgba(120,70,230,', boltEdge: '#f4ecff', boltEdgeDark: '#1b0f40', boltGlow: 'rgba(185,140,255,.45)',
    // boss 7 purple lightning (D39, INF-X11): bright core + dark outline, a redder purple than boss 6's violet
    pWarn: 'rgba(190,40,255,', pEdge: '#fbe6ff', pEdgeDark: '#22042f', pGlow: 'rgba(214,90,255,.45)', pBody: '#c23cff', pCore: '#fff0ff'
  }
};
// Menus (scenery) and the hidden stage 9 always use NORMAL's forest; a run uses its own tier.
export function themeKey(S, view) { return view && view.scenery ? 'bronze' : S.tier === 'silver' ? 'silver' : 'bronze'; }

// Deterministic decoration layouts (built once; cheap rects per frame).
function lcg(seed) { return () => { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; }; }
const TREES = (() => {
  const r = lcg(7), out = [];
  for (let y = -MAX_EXTRA_H + 6; y < GATE_Y - 30; y += 34) for (let x = -6; x < W; x += 40) out.push([x + r() * 22, y + r() * 16, 18 + ((r() * 3) | 0) * 6]);
  return out;
})();
const MOSS = (() => { const r = lcg(11), out = []; for (let i = 0; i < 40; i++) out.push([r() * W, -MAX_EXTRA_H + r() * (GATE_Y + MAX_EXTRA_H), 6 + r() * 14]); return out; })();
const TUFTS = (() => { const r = lcg(23), out = []; for (let i = 0; i < 26; i++) out.push([14 + r() * (W - 34), GATE_Y + GATE_H + 16 + r() * (H - GATE_Y - GATE_H - 24)]); return out; })();
const STARS = (() => { const r = lcg(5), out = []; for (let i = 0; i < 28; i++) out.push([r() * W, -MAX_EXTRA_H + r() * (MAX_EXTRA_H + 150), r() * 6.28]); return out; })();
const CLOUDS = [[20, 40, 0.9, 6], [230, 130, 1.1, 4], [90, 250, 0.8, 7], [300, 330, 1.0, 5], [160, -80, 1.2, 3]];   // x, y, scale, drift px/s

// Static backdrop (ground, trees, horizon, arena, gate strip, path) for one theme, painted once into an offscreen
// canvas at device resolution and blitted every frame; only stars, clouds and fog are drawn live.
let BG = null;
function backdrop(th, key, oy, scale) {
  const id = key + ':' + oy + ':' + scale;
  if (BG && BG.id === id) return BG.canvas;
  const cv = (BG && BG.canvas) || document.createElement('canvas');
  cv.width = Math.ceil((W + 20) * scale); cv.height = Math.ceil((H + oy + 20) * scale);
  const c = cv.getContext('2d');
  c.setTransform(scale, 0, 0, scale, 10 * scale, (10 + oy) * scale);   // logical (-10, -10 - oy) maps to the bitmap origin
  // upper field: forest floor (NORMAL) or night-indigo sky (EXTRA); no red reserve band any more (FE-52)
  if (th.upper.length > 1) {
    const g = c.createLinearGradient(0, -oy, 0, GATE_Y + 10);
    th.upper.forEach((col, i) => g.addColorStop(i / (th.upper.length - 1), col));
    c.fillStyle = g;
  } else c.fillStyle = th.upper[0];
  c.fillRect(-10, -10 - oy, W + 20, GATE_Y + 20 + oy);
  if (th.tree) {
    c.fillStyle = th.moss;
    for (const m of MOSS) c.fillRect(m[0] | 0, m[1] | 0, m[2] | 0, (m[2] * 0.5) | 0);
    const T = th.tree;
    for (const t of TREES) {
      const x = t[0] | 0, y = t[1] | 0, s = t[2];
      c.fillStyle = T.shadow; c.fillRect(x + 4, y + 6, s, s * 0.8);
      c.fillStyle = T.trunk; c.fillRect(x + s / 2 - 2, y + s * 0.7, 4, 6);
      c.fillStyle = T.dark; c.fillRect(x, y, s, s * 0.8);
      c.fillStyle = T.mid; c.fillRect(x + 2, y + 2, s - 6, s * 0.5);
      c.fillStyle = T.light; c.fillRect(x + 4, y + 3, (s / 3) | 0, 3);
    }
  }
  // top edge: a dark horizon (tree line / deep sky) fading into the field so red units read against it
  const hz = c.createLinearGradient(0, -oy, 0, 40);
  hz.addColorStop(0, th.horizon); hz.addColorStop(1, 'rgba(0,0,0,0)');
  c.fillStyle = hz; c.fillRect(-10, -10 - oy, W + 20, 50 + oy);
  // arena ground with rim
  c.fillStyle = th.arenaRim; c.fillRect(-10, GATE_Y + GATE_H, W + 20, H);
  c.fillStyle = th.arena; c.fillRect(10, GATE_Y + GATE_H + 8, W - 20, H);
  c.fillStyle = th.arenaLine;
  for (let y = GATE_Y + GATE_H + 30; y < H; y += 44) c.fillRect(10, y, W - 20, 2);
  c.fillStyle = th.tuft;
  for (const t of TUFTS) { c.fillRect(t[0] | 0, t[1] | 0, 2, 4); c.fillRect((t[0] | 0) + 3, (t[1] | 0) - 1, 2, 5); }
  // gate strip, always repainted so the last run's gates do not linger behind menus
  c.fillStyle = th.gateBand; c.fillRect(-10, GATE_Y + 10, W + 20, GATE_H - 10);
  // path: edge + rim + surface, then cross seams (wooden planks / cloud-bridge steps) via a dashed wide stroke
  c.lineJoin = 'round'; c.lineCap = 'round';
  c.beginPath(); c.moveTo(PL[0][0], PL[0][1]);
  for (let i = 1; i < PL.length; i++) c.lineTo(PL[i][0], PL[i][1]);
  c.strokeStyle = th.pathEdge; c.lineWidth = PATH_HW * 2 + 16; c.stroke();
  c.strokeStyle = th.pathRim; c.lineWidth = PATH_HW * 2 + 7; c.stroke();
  c.strokeStyle = th.path; c.lineWidth = PATH_HW * 2; c.stroke();
  c.lineCap = 'butt'; c.strokeStyle = th.pathSeam; c.lineWidth = PATH_HW * 2 - 4; c.setLineDash([2, 10]); c.stroke();
  c.setLineDash([]); c.lineCap = 'round';
  BG = { id, canvas: cv };
  return cv;
}

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
  const key = themeKey(S, view), th = THEME[key];
  const scale = Math.max(1, Math.round(Math.abs(ctx.getTransform().a) * 100) / 100);
  ctx.drawImage(backdrop(th, key, oy, scale), -10, -10 - oy, W + 20, H + oy + 20);
  ctx.lineJoin = 'round'; ctx.lineCap = 'round';
  if (th.stars) {
    ctx.fillStyle = th.star;
    for (const st of STARS) { ctx.globalAlpha = 0.35 + 0.35 * Math.sin(S.t * 1.3 + st[2]); ctx.fillRect(st[0] | 0, st[1] | 0, 2, 2); }
    ctx.globalAlpha = 1;
  }
  // drifting clouds (EXTRA): a handful of pixel-stepped puffs, behind everything else
  for (let i = 0; i < th.clouds; i++) {
    const c = CLOUDS[i], sc = c[2], span = W + 160;
    const x = ((c[0] + S.t * c[3]) % span + span) % span - 80, y = c[1];
    ctx.fillStyle = th.cloud;
    roundRect(x, y, 70 * sc, 14 * sc, 7 * sc); ctx.fill();
    roundRect(x + 14 * sc, y - 9 * sc, 40 * sc, 14 * sc, 7 * sc); ctx.fill();
    ctx.fillStyle = th.cloudHi; roundRect(x + 20 * sc, y - 7 * sc, 22 * sc, 4 * sc, 2 * sc); ctx.fill();
  }
  // fog (EXTRA): one slowly breathing horizontal gradient band, drawn under units and telegraphs
  if (th.fog) {
    const fy = 210 + Math.sin(S.t * 0.4) * 30, fg = ctx.createLinearGradient(0, fy - 90, 0, fy + 90);
    fg.addColorStop(0, th.fog + '0)'); fg.addColorStop(0.5, th.fog + '.22)'); fg.addColorStop(1, th.fog + '0)');
    ctx.fillStyle = fg; ctx.fillRect(-10, fy - 90, W + 20, 180);
  }

  // out-of-game screens (v19): static scenery only — no armies, gates, bosses or turret behind the menus (UXR-01)
  if (view && view.scenery) { ctx.restore(); return; }
  // queued red column: solid fill only where the path actually holds reds, so shell craters open real gaps
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
      ctx.fillStyle = th.gate.multFrame; roundRect(g.x0, GATE_Y, w, GATE_H, 4); ctx.fill();
      ctx.fillStyle = '#f2c233'; roundRect(g.x0 + 2, GATE_Y + 2, w - 4, GATE_H - 8, 3); ctx.fill();
      ctx.fillStyle = th.gate.multPost; ctx.fillRect(g.x0 - 5, GATE_Y - 6, 7, GATE_H + 12); ctx.fillRect(g.x1 - 2, GATE_Y - 6, 7, GATE_H + 12);
      ctx.fillStyle = '#2b1d00'; ctx.font = 'bold 22px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText('x' + g.mult, g.x0 + w / 2, GATE_Y + GATE_H / 2 - 1);
    } else if (!g.dead) {
      // weapon crate above the bar
      const cx = g.x0 + w / 2, top = GATE_Y - 40, ab = ABILITIES[g.ability];
      ctx.fillStyle = th.gate.crate; roundRect(cx - 28, top, 56, 42, 6); ctx.fill();
      const cols = Math.min(2 + g.gen, 4), rows = Math.min(2 + (g.gen >> 1), 3), r0 = cols > 3 ? 5 : 6;
      for (let i = 0; i < cols; i++) for (let j = 0; j < rows; j++) {
        const bx = cx - (cols - 1) * (r0 + 2) + i * (r0 * 2 + 4), by = top + 9 + j * (r0 * 2 + 1);
        ctx.fillStyle = ab.color; ctx.beginPath(); ctx.arc(bx, by, r0, 0, 6.28); ctx.fill();
        ctx.fillStyle = '#1a1a1a'; ctx.beginPath(); ctx.arc(bx, by, r0 * 0.45, 0, 6.28); ctx.fill();
      }
      ctx.fillStyle = ab.color; ctx.font = 'bold 14px "Luckiest Guy", "Arial Black", sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      // a multiplier crate reads MAX once soldiers per shot is maxed
      const tag = ab.mult && spreadMaxed(S) ? 'MAX' : ab.tag;
      ctx.lineWidth = 3; ctx.strokeStyle = '#2b1d00'; ctx.strokeText(tag, cx, top - 10); ctx.fillText(tag, cx, top - 10);
      ctx.fillStyle = th.gate.hpBack; roundRect(g.x0, GATE_Y, w, GATE_H, 4); ctx.fill();
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
    const bset = bo.minion ? SPR.minion : bo.purple ? SPR.purple : SPR.bosses[bo.idx - 1];   // idx is 1-based
    // EXTRA boss 7 in the centre: invulnerable ring, and the wave (sprite hops up + swells) while invulnerable,
    // then the raised pose (held larger) while the round is cast (D39, X9=a)
    let lift = 0, grow = 1;
    if (bo.purple && bo.mode === 3) {
      const wv = Math.abs(Math.sin(S.t * 10));
      if (bo.inv > 0) { lift = 4 + wv * 8; grow = 1.08 + wv * 0.14; } else { lift = 6; grow = 1.16; }
      if (bo.inv > 0) {
        ctx.strokeStyle = THEME.silver.pEdge; ctx.globalAlpha = 0.5 + 0.3 * wv; ctx.lineWidth = 3;
        ctx.beginPath(); ctx.arc(bo.x, bo.y, R + 10, 0, 6.28); ctx.stroke(); ctx.globalAlpha = 1;
      }
    }
    if (SPR.ready && !lite && bset) {
      const im = bset[bo.hitT > 0 ? 1 : 0];
      const bob = Math.abs(Math.sin(S.t * 9)) * 3 + lift;
      ctx.fillStyle = 'rgba(0,0,0,.3)'; ctx.beginPath(); ctx.ellipse(bo.x, bo.y + R - 2, R, R * 0.35, 0, 0, 6.28); ctx.fill();
      if (grow !== 1) { const w = im.width * grow, h = im.height * grow; ctx.drawImage(im, (bo.x - w / 2) | 0, (bo.y - h / 2 - bob) | 0, w | 0, h | 0); }
      else ctx.drawImage(im, (bo.x - im.width / 2) | 0, (bo.y - im.height / 2 - bob) | 0);
    } else {
    ctx.save(); ctx.translate(bo.x, bo.y - lift); if (grow !== 1) ctx.scale(grow, grow);
    ctx.fillStyle = bo.hitT > 0 ? '#ffd0d0' : bo.purple ? '#5a1080' : '#8e1016';
    ctx.beginPath(); ctx.arc(0, 0, R, 0, 6.28); ctx.fill();
    ctx.fillStyle = bo.hitT > 0 ? '#fff' : bo.purple ? '#c23cff' : '#d6242c';
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
  // LASER loadout buff: a short-lived blue column up from the turret (2 x LASER.halfWidth = 90px wide, fading out)
  if (S.laserFx && S.laserFx.length) {
    for (const lz of S.laserFx) {
      const k = 1 - lz.t / LASER.fx, top = -MAX_EXTRA_H, y0 = S.turret.y - 20;
      ctx.globalAlpha = 0.25 * k; ctx.fillStyle = '#8fb3ff'; ctx.fillRect(lz.x - LASER.halfWidth, top, LASER.halfWidth * 2, y0 - top);
      ctx.globalAlpha = 0.85 * k; ctx.fillStyle = '#e6efff'; ctx.fillRect(lz.x - 4, top, 8, y0 - top);
    }
    ctx.globalAlpha = 1;
  }
  // hidden-boss beams: a pulsing dashed telegraph while warning, then a glowing beam. Each kind is a path:
  // pillar (straight down), scurve (sinusoidal down), cross (star of lines through the boss).
  const L = 1400, TAU = Math.PI * 2, SB = SECRET_BOSS;
  function beamPath(b) {
    ctx.beginPath();
    if (b.kind === 'pillar') { ctx.moveTo(b.x, b.y); ctx.lineTo(b.x, H + 20); }
    else if (b.kind === 'scurve') {
      ctx.moveTo(b.x, b.y);
      for (let y = b.y + 6; y <= H + 20; y += 6) ctx.lineTo(b.x + SB.scurve.amp * Math.sin((y - b.y) / SB.scurve.wl * TAU + b.ph), y);
    } else {
      const lines = SB.rays / 2;
      for (let i = 0; i < lines; i++) { const a = b.a + i * Math.PI / lines, c = Math.cos(a) * L, sn = Math.sin(a) * L; ctx.moveTo(b.x - c, b.y - sn); ctx.lineTo(b.x + c, b.y + sn); }
    }
  }
  ctx.lineCap = 'butt';
  for (const b of S.beams) {
    const firing = b.t >= b.warn, k = firing ? 1 - (b.t - b.warn) / b.fire : b.t / b.warn, hw = b.hw;
    beamPath(b);
    if (!firing) {
      ctx.globalAlpha = 0.12 * k; ctx.strokeStyle = '#ff3b3b'; ctx.lineWidth = hw * 2; ctx.stroke();
      ctx.globalAlpha = 0.35 + 0.5 * Math.abs(Math.sin(S.t * 14)); ctx.lineWidth = 2 + 2 * k; ctx.setLineDash([8, 6]); ctx.lineDashOffset = -S.t * 40; ctx.stroke(); ctx.setLineDash([]);
    } else {
      ctx.globalAlpha = 0.3 + 0.6 * k;
      ctx.strokeStyle = 'rgba(255,40,40,.5)'; ctx.lineWidth = hw * 2 + 16; ctx.stroke();
      ctx.strokeStyle = '#ff4040'; ctx.lineWidth = hw * 2; ctx.stroke();
      ctx.strokeStyle = '#fff0f0'; ctx.lineWidth = hw * 0.7; ctx.stroke();
    }
  }
  ctx.lineCap = 'round';
  ctx.globalAlpha = 1;
  // EXTRA boss 6 lightning (_ui-spec.md §5.3): the warned column pulses with a light fill and dashed edges; the
  // firing column is a three-layer violet bolt; once it has fired nothing of it is drawn (FE-41). No text.
  const BT = THEME.silver;   // lightning only exists in EXTRA
  for (const bl of S.bolts || []) {
    const top = -MAX_EXTRA_H, bot = H + 20, bw = bl.x1 - bl.x0, cx = (bl.x0 + bl.x1) / 2;
    if (bl.t < bl.warn) {
      ctx.globalAlpha = 1;
      ctx.fillStyle = BT.boltWarn + (0.14 + 0.14 * (0.5 + 0.5 * Math.sin(S.t * 9))) + ')';
      ctx.fillRect(bl.x0, top, bw, bot - top);
      ctx.beginPath(); ctx.moveTo(bl.x0 + 1, top); ctx.lineTo(bl.x0 + 1, bot); ctx.moveTo(bl.x1 - 1, top); ctx.lineTo(bl.x1 - 1, bot);
      ctx.strokeStyle = BT.boltEdgeDark; ctx.lineWidth = 4; ctx.stroke();
      ctx.strokeStyle = BT.boltEdge; ctx.lineWidth = 2; ctx.setLineDash([8, 6]); ctx.lineDashOffset = -S.t * 40; ctx.stroke();
      ctx.setLineDash([]);
    } else {
      const k = 1 - (bl.t - bl.warn) / bl.fire;
      ctx.globalAlpha = 0.35 + 0.65 * k;
      ctx.fillStyle = BT.boltGlow; ctx.fillRect(bl.x0, top, bw, bot - top);
      // zigzag bolt; its shape changes at most 3 times per second (flash-safe)
      let seed = (bl.strip * 97 + Math.floor(bl.t * 3) * 31) | 0;
      const rnd = () => { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; };
      ctx.beginPath(); ctx.moveTo(cx, top);
      for (let y = top + 40; y <= bot; y += 40) ctx.lineTo(cx + (rnd() - 0.5) * bw * 0.6, y);
      ctx.lineJoin = 'miter';
      ctx.strokeStyle = BT.boltEdgeDark; ctx.lineWidth = bw * 0.45 + 6; ctx.stroke();
      ctx.strokeStyle = '#b98cff'; ctx.lineWidth = bw * 0.45; ctx.stroke();
      ctx.strokeStyle = '#fbf4ff'; ctx.lineWidth = bw * 0.14; ctx.stroke();
      ctx.lineJoin = 'round';
    }
  }
  ctx.globalAlpha = 1;
  for (const o of S.orbs || []) {
    const left = STORM.orbs.life - o.t;
    // last 1.5 s: blink at 2.5 per second (<= 3 flashes per second) before the orb fades out
    if (left < STORM.orbs.blink && Math.floor(o.t * 5) % 2 === 1) continue;
    const R = STORM.orbs.r;
    // orb keeps the original look (soft glow + body + core); no dark outline (user ruling 2026-11-03)
    ctx.fillStyle = 'rgba(185,140,255,.45)'; ctx.beginPath(); ctx.arc(o.x, o.y, R + 5, 0, 6.28); ctx.fill();
    ctx.fillStyle = '#b98cff'; ctx.beginPath(); ctx.arc(o.x, o.y, R, 0, 6.28); ctx.fill();
    ctx.fillStyle = '#fbf4ff'; ctx.beginPath(); ctx.arc(o.x, o.y, R * 0.45, 0, 6.28); ctx.fill();
  }
  // EXTRA boss 7 purple lightning (D39): same column look as boss 6 in a purple palette; columns with t < 0 are not
  // shown yet; a column that has fired is gone (INF-X4). The safe column carries no mark (X3=b). No text.
  for (const bl of S.pbolts || []) {
    if (bl.t < 0) continue;
    const top = -MAX_EXTRA_H, bot = H + 20, bw = bl.x1 - bl.x0, cx = (bl.x0 + bl.x1) / 2;
    if (bl.t < bl.warn) {
      ctx.globalAlpha = 1;
      ctx.fillStyle = BT.pWarn + (0.14 + 0.14 * (0.5 + 0.5 * Math.sin(S.t * 9))) + ')';
      ctx.fillRect(bl.x0, top, bw, bot - top);
      ctx.beginPath(); ctx.moveTo(bl.x0 + 1, top); ctx.lineTo(bl.x0 + 1, bot); ctx.moveTo(bl.x1 - 1, top); ctx.lineTo(bl.x1 - 1, bot);
      ctx.strokeStyle = BT.pEdgeDark; ctx.lineWidth = 4; ctx.stroke();
      ctx.strokeStyle = BT.pEdge; ctx.lineWidth = 2; ctx.setLineDash([8, 6]); ctx.lineDashOffset = -S.t * 40; ctx.stroke();
      ctx.setLineDash([]);
    } else {
      const k = 1 - (bl.t - bl.warn) / bl.fire;
      ctx.globalAlpha = 0.35 + 0.65 * k;
      ctx.fillStyle = BT.pGlow; ctx.fillRect(bl.x0, top, bw, bot - top);
      let seed = (bl.strip * 131 + Math.floor(bl.t * 3) * 17 + 7) | 0;   // shape changes at most 3 times per second
      const rnd = () => { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; };
      ctx.beginPath(); ctx.moveTo(cx, top);
      for (let y = top + 40; y <= bot; y += 40) ctx.lineTo(cx + (rnd() - 0.5) * bw * 0.6, y);
      ctx.lineJoin = 'miter';
      ctx.strokeStyle = BT.pEdgeDark; ctx.lineWidth = bw * 0.45 + 6; ctx.stroke();
      ctx.strokeStyle = BT.pBody; ctx.lineWidth = bw * 0.45; ctx.stroke();
      ctx.strokeStyle = BT.pCore; ctx.lineWidth = bw * 0.14; ctx.stroke();
      ctx.lineJoin = 'round';
    }
  }
  ctx.globalAlpha = 1;
  // lightning arrows: shaft along the flight direction (len x width as the hit capsule), dark outline, bright core, arrowhead
  if (S.arrows && S.arrows.length) {
    const A = BOSS7.arrows;
    ctx.lineCap = 'round';
    for (const a of S.arrows) {
      const tx = a.x - a.ux * A.len, ty = a.y - a.uy * A.len;
      ctx.beginPath(); ctx.moveTo(tx, ty); ctx.lineTo(a.x, a.y);
      ctx.strokeStyle = BT.pGlow; ctx.lineWidth = A.hw * 2 + 8; ctx.stroke();
      ctx.strokeStyle = BT.pEdgeDark; ctx.lineWidth = A.hw * 2 + 3; ctx.stroke();
      ctx.strokeStyle = BT.pBody; ctx.lineWidth = A.hw * 2; ctx.stroke();
      ctx.strokeStyle = BT.pCore; ctx.lineWidth = A.hw * 0.7; ctx.stroke();
      const hx = a.x + a.ux * 6, hy = a.y + a.uy * 6, nx = -a.uy, ny = a.ux;
      ctx.beginPath(); ctx.moveTo(hx + a.ux * 8, hy + a.uy * 8); ctx.lineTo(hx + nx * 9, hy + ny * 9); ctx.lineTo(hx - nx * 9, hy - ny * 9); ctx.closePath();
      ctx.fillStyle = BT.pCore; ctx.fill(); ctx.lineWidth = 2; ctx.strokeStyle = BT.pEdgeDark; ctx.stroke();
    }
  }
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
