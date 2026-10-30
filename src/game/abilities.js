// Gate rewards and the pick-one-of-two pool. `apply(S, game)` mutates the game state directly.
export const ABILITIES = {
  shot2: { tag: 'x2', name: 'SHOT x2', color: '#ffe066', apply: S => { S.spread = Math.min(S.spread * 2, 24); } },
  shot3: { tag: 'x3', name: 'SHOT x3', color: '#ffe066', apply: S => { S.spread = Math.min(S.spread * 3, 24); } },
  armor: { tag: 'ATK+1', name: 'ATK +1', color: '#8fb3ff', apply: S => { S.blueHp += 1; } },
  gate: { tag: 'GATE+5', name: 'GATE +5', color: '#ffb347', apply: S => { S.gates[1].mult += 5; } },
  speed: { tag: 'SPD+', name: 'SPEED UP', color: '#7dffa7', apply: S => { S.blueSpeed += 18; S.fireMul *= 0.85; } },
  mystery: { tag: '?', name: '???', color: '#d9a4ff', apply: (S, game) => { game.openChoice(); } }
};
// Pick-one-of-two pool shown when a "?" gate breaks. `good` is only used for the card colour.
export const CHOICES = {
  shotP2: { title: '+2', desc: '每發士兵 +2', good: false, apply: S => { S.spread = Math.min(S.spread + 2, 24); } },
  shotX3: { title: 'x3', desc: '每發士兵 x3', good: true, apply: S => { S.spread = Math.min(S.spread * 3, 24); } },
  shotP3: { title: '+3', desc: '每發士兵 +3', good: false, apply: S => { S.spread = Math.min(S.spread + 3, 24); } },
  shotX4: { title: 'x4', desc: '每發士兵 x4', good: true, apply: S => { S.spread = Math.min(S.spread * 4, 24); } },
  gateP2: { title: 'GATE+2', desc: '中門倍率 +2', good: false, apply: S => { S.gates[1].mult += 2; } },
  gateP10: { title: 'GATE+10', desc: '中門倍率 +10', good: true, apply: S => { S.gates[1].mult += 10; } },
  spdP6: { title: 'SPD+6', desc: '士兵速度 +6、攻速 +5%', good: false, apply: S => { S.blueSpeed += 6; S.fireMul *= 0.95; } },
  spdP40: { title: 'SPD+40', desc: '士兵速度 +40、攻速 +25%', good: true, apply: S => { S.blueSpeed += 40; S.fireMul *= 0.8; } },
  cannon: { title: '砲擊', desc: '砲台加裝砲彈，範圍爆破', good: true, apply: S => { S.cannon.level += 1; S.cannon.t = 0.5; } }
};

// Pick-one-of-three offered before the hidden stage; every card is a strong buff.
export const BONUS = {
  shotX4: CHOICES.shotX4,
  gateP10: CHOICES.gateP10,
  spdP40: CHOICES.spdP40,
  cannon: CHOICES.cannon,
  atkP3: { title: 'ATK+3', desc: '每個士兵攻擊 +3', good: true, apply: S => { S.blueHp += 3; } },
  rapid: { title: 'RAPID', desc: '攻速 +40%', good: true, apply: S => { S.fireMul *= 0.6; } }
};

// Card text shown on the choice screen; a repeated cannon pick reads as an upgrade.
export function cardFor(k, S, pool = CHOICES) {
const c = pool[k];
if (k === 'cannon' && S.cannon.level > 0) return { title: '砲擊+', desc: '砲彈更強、更快', good: c.good };
return { title: c.title, desc: c.desc, good: c.good };
}
