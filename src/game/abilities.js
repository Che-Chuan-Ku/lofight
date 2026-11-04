// Gate rewards and the pick-one-of-two pool. `apply(S, game)` mutates the game state directly.
// Soldiers per shot (S.spread): multiplying rewards (`mult: true`) stop at SPREAD_CAP and leave the pools once it is reached;
// additive "?" picks are not capped (FE-27).
export const SPREAD_CAP = 24;
export const spreadMaxed = S => S.spread >= SPREAD_CAP;
// never lowers a spread that additive picks already pushed past the cap
const multCapped = k => S => { S.spread = Math.max(S.spread, Math.min(S.spread * k, SPREAD_CAP)); };
export const ABILITIES = {
  shot2: { tag: 'x2', name: 'SHOT x2', color: '#ffe066', mult: true, apply: multCapped(2) },
  shot3: { tag: 'x3', name: 'SHOT x3', color: '#ffe066', mult: true, apply: multCapped(3) },
  armor: { tag: 'ATK+1', name: 'ATK +1', color: '#8fb3ff', apply: S => { S.blueHp += 1; } },
  gate: { tag: 'GATE+5', name: 'GATE +5', color: '#ffb347', apply: S => { S.gates[1].mult += 5; } },
  speed: { tag: 'SPD+', name: 'SPEED UP', color: '#7dffa7', apply: S => { S.blueSpeed += 18; S.fireMul *= 0.85; } },
  mystery: { tag: '?', name: '???', color: '#d9a4ff', apply: (S, game) => { game.openChoice(); } }
};
// Pick-one-of-two pool shown when a "?" gate breaks. `good` is only used for the card colour.
export const CHOICES = {
  shotP2: { title: '+2', desc: '每發士兵 +2', good: false, apply: S => { S.spread += 2; } },
  shotX3: { title: 'x3', desc: '每發士兵 x3', good: true, mult: true, apply: multCapped(3) },
  shotP3: { title: '+3', desc: '每發士兵 +3', good: false, apply: S => { S.spread += 3; } },
  shotX4: { title: 'x4', desc: '每發士兵 x4', good: true, mult: true, apply: multCapped(4) },
  gateP2: { title: 'GATE+2', desc: '中門倍率 +2', good: false, apply: S => { S.gates[1].mult += 2; } },
  gateP10: { title: 'GATE+10', desc: '中門倍率 +10', good: true, apply: S => { S.gates[1].mult += 10; } },
  spdP6: { title: 'SPD+6', desc: '士兵速度 +6、攻速 +5%', good: false, apply: S => { S.blueSpeed += 6; S.fireMul *= 0.95; } },
  spdP40: { title: 'SPD+40', desc: '士兵速度 +40、攻速 +25%', good: true, apply: S => { S.blueSpeed += 40; S.fireMul *= 0.8; } }
};

// Card text shown on the choice screen.
export function cardFor(k, S, pool = CHOICES) {
  const c = pool[k];
  return { title: c.title, desc: c.desc, good: c.good };
}
