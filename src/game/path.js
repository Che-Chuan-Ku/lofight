// Winding mountain path: Catmull-Rom spline through control points, bottom mouth at the middle gate.
const CP = [[195, 436], [118, 388], [70, 322], [108, 256], [212, 222], [302, 168], [292, 102], [222, 52], [150, 14], [112, -40]];
function catmull(p, segs) {
  const out = [];
  for (let i = 0; i < p.length - 1; i++) {
    const p0 = p[Math.max(i - 1, 0)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(i + 2, p.length - 1)];
    for (let j = 0; j < segs; j++) {
      const t = j / segs, t2 = t * t, t3 = t2 * t;
      out.push([
        0.5 * (2 * p1[0] + (-p0[0] + p2[0]) * t + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3),
        0.5 * (2 * p1[1] + (-p0[1] + p2[1]) * t + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)
      ]);
    }
  }
  out.push(p[p.length - 1]);
  return out;
}
const PL = catmull(CP, 14);
const CUM = [0];
for (let i = 1; i < PL.length; i++) CUM[i] = CUM[i - 1] + Math.hypot(PL[i][0] - PL[i - 1][0], PL[i][1] - PL[i - 1][1]);
const PATH_LEN = CUM[CUM.length - 1];
// 1px lookup table: x, y, nx, ny
const TAB = new Float32Array((Math.ceil(PATH_LEN) + 2) * 4);
(function buildTab() {
  let seg = 0;
  for (let s = 0; s <= Math.ceil(PATH_LEN) + 1; s++) {
    const ss = Math.min(s, PATH_LEN);
    while (seg < PL.length - 2 && CUM[seg + 1] < ss) seg++;
    const a = PL[seg], b = PL[seg + 1], L = CUM[seg + 1] - CUM[seg] || 1;
    const t = (ss - CUM[seg]) / L;
    const dx = (b[0] - a[0]) / L, dy = (b[1] - a[1]) / L;
    TAB[s * 4] = a[0] + (b[0] - a[0]) * t;
    TAB[s * 4 + 1] = a[1] + (b[1] - a[1]) * t;
    TAB[s * 4 + 2] = -dy; TAB[s * 4 + 3] = dx;
  }
})();
function pathPos(s, lat) {
  const i = Math.max(0, Math.min(TAB.length / 4 - 1, Math.round(s))) * 4;
  return [TAB[i] + TAB[i + 2] * lat, TAB[i + 1] + TAB[i + 3] * lat];
}

export { PL, PATH_LEN, TAB, pathPos };
