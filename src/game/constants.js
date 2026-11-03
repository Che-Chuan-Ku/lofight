// Game constants (logical playfield is 390x780, letterboxed by the stage).
export const W = 390, H = 780, GATE_Y = 430, GATE_H = 28, PATH_HW = 30;
export const GATE_MULT = 10;
export const MAX_EXTRA_H = 160;   // tall screens: extra logical height added above the battlefield (more sky / canopy)
export const MAX_CROP_H = 140;    // short portrait screens: how much of the top may be cropped to reach full width
export const SHOT_SPEED = 420;        // px/s, soldiers fired from the turret
export const MAXB = 9999, MAXR = 4500;   // over MAXB the rear of the blue column is culled; the turret never stops
export const FIRE_INTERVAL = 0.18;   // base seconds between shots (~5.6/s); SPD rewards shorten it via S.fireMul
export const FIRE_MIN = 0.1;         // fastest allowed fire interval
export const FIRE_MIN_BUFF = 0.08;   // fastest fire interval with the ATTACK_SPEED loadout buff (GAP-04=A)
// LASER loadout buff: every `interval` s x fireMul (never below `min`), clears a 90px column (1.5 x the old 60, FE-45) up from
// the turret and hits bosses for bossDamage x attack; `fx` is how long the beam stays drawn (D12, GAP-05=A).
export const LASER = { interval: 3, min: 1.5, halfWidth: 45, bossDamage: 200, fx: 0.25 };
export const GATE_RESPAWN = 2.0;   // seconds a broken gate stays open before it rebuilds
export const GATE_X0 = 16, GATE_GAP = 14, GATE_MIN = 44, GATE_STEP = 40;
// sprites: Kenney "Tiny Dungeon", CC0 (kenney.nl), 16x16 tiles, 12 columns
export const UNIT_PX = 11;
export const BLUE_TILE = 96, RED_TILE = 111;
export const BOSS_TILES = [110, 112, 122, 120, 124, 109, 121, 123];
export const MINION_TILE = 108;
// Stages 1-8. `at`: game second the boss spawns (Infinity = held until the rest pick after boss 7),
// `hp`, `v`: speed along the path in px/s (arena speed is fixed at 30 in the engine), `r`: body radius,
// `summon`: minions (count, hp, speed, cooldown). Unchanged since v18.6.5.
export const BOSS_PLAN = [
  { at: 14, hp: 2000, v: 14, r: 16 },
  { at: 28, hp: 5000, v: 15, r: 18 },
  { at: 42, hp: 9000, v: 16, r: 20 },
  { at: 56, hp: 20000, v: 17, r: 22 },
  { at: 70, hp: 38000, v: 18, r: 24 },
  { at: 84, hp: 70000, v: 20, r: 27 },
  { at: 98, hp: 90000, v: 21, r: 30 },
  { at: Infinity, hp: 120000, v: 21, r: 32, summon: { n: 5, hp: 2000, v: 26, cd: 5 } }
];
// EXTRA (API tier SILVER, D29-D32, D39; specs/ui/遊戲畫面.md "EXTRA 魔王機制" / "EXTRA 第 7 王"): seven bosses, boss 7 down = victory.
// Own hp table (bosses 4-6 = NORMAL 20000 x1.3 / 38000 x1.6 / 70000 x2, FE-40); speed / size of NORMAL bosses 1-6; red spawn as NORMAL (the old x1.2 is gone, AMB-E8=B).
// `minAt`: a held boss (at = Infinity) never enters before this game second (backend appearance floors 70 / 84).
// `after`: what releases a held boss — 'rest' (rest pick once the field is clear, as NORMAL boss 8) or 'kill' (the
// previous boss falls). `skills`: 'beam' = hidden-stage beams, 'storm' = lightning sweep + lightning orbs,
// 'purple' = boss 7 purple lightning (BOSS7 below).
export const EXTRA_PLAN = [
  { at: 14, hp: 2400, v: 14, r: 16 },
  { at: 28, hp: 8000, v: 15, r: 18 },
  { at: 42, hp: 15000, v: 16, r: 20 },
  { at: 56, hp: 26000, v: 17, r: 22, summon: { n: 5, hp: 500, v: 26, cd: 5 } },   // only boss 4 summons (AMB-E2=D)
  { at: Infinity, minAt: 70, after: 'rest', hp: 60800, v: 18, r: 24, skills: 'beam' },
  { at: Infinity, minAt: 84, after: 'kill', hp: 140000, v: 20, r: 27, skills: 'storm' },
  // boss 7 (D39): enters right after boss 6 falls (X1=b), backend floor 84 s; own hp, speed / size of the NORMAL hidden boss (X9=a)
  { at: Infinity, minAt: 84, after: 'kill', hp: 180000, v: 19, r: 34, skills: 'purple' }
];
// EXTRA boss 6 (D32). Sweep: the logical width (0-390) is cut into `strips` equal columns (65 px >= 2 x turret hit
// width 32); one column at a time in random order, each `warn` s telegraph (harmless) then `fire` s lightning that
// ends the run when the turret overlaps it; a round hits every column once, the next round starts `cd` s after.
// Each column's telegraph disappears the moment its bolt fires; no trace of hit columns is kept (FE-41).
// Orbs (speed 100 px/s FE-42; radius 9 x sqrt2 = twice the area, hit and drawn alike, FE-44): `n` per cast from the boss, angle 20-160 deg each (90 = straight down), `speed` px/s, bounce off the
// 390x780 edges, live `life` s, first cast `first` s after the boss enters, then every `cd` s. Neither touches soldiers.
export const STORM = {
  sweep: { strips: 6, warn: 1.2, fire: 0.6, cd: 20 },
  orbs: { n: 3, minDeg: 20, maxDeg: 160, speed: 150, life: 8, first: 7, cd: 7, r: 9 * Math.SQRT2, blink: 1.5 }
};
// Hidden stage 9, offered once boss 8 falls. Every beam telegraphs for `warn` seconds before it
// `fire`s; a turret inside a firing beam ends the run. `hw` is the beam half-width.
export const SECRET_TILE = 84;
export const SECRET_BOSS = {
  hp: 150000, v: 19, r: 34,   // same fields as a BOSS_PLAN entry (boss 8 is 120000 / 21 / 32); no minions
  solo: { cd: 5, warn: 1.2, fire: 0.6, hw: 14 },      // down from the boss: straight pillar or S-curve, picked at random
  wide: { cd: 10, warn: 1.5, fire: 0.7, hw: 11 },     // from half hp: twelve-ray star through the boss
  scurve: { amp: 60, wl: 260, triGap: 90 },            // lateral offset = amp * sin(2π * dy / wl); triGap spaces the enraged triple cast
  rays: 12                                             // star beams (lines through the boss = rays / 2)
};
export const BEAM_HIT_R = 16;   // turret radius for beam hits
// EXTRA boss 7 (D39, specs/ui/遊戲畫面.md "EXTRA 第 7 王"). Columns are the boss-6 strips (6 x 65 px).
// Centre phase: the boss appears (and on every cd comes back) at `center`, invulnerable for `inv` s while it waves,
// then casts the full-screen lightning: one random safe column, the other 5 fire in a random order that passes the
// dodge guarantee (INF-X14); the k-th column telegraphs at k*step, fires at k*step + warn for `fire` s (2.2 s round).
// After the round the boss teleports back to the path (first round: path top; later: where it left), `cd` s after
// the round ends it returns to the centre. On the path: one random column every bolt.cd s (first one bolt.cd s after
// the opening round) and `arrows.n` arrows every arrows.cd s (angle minDeg-maxDeg each, 90 = straight down; speed =
// boss-6 orb x1.3; width = half the orb diameter, length = twice it; no bounce, gone once fully off screen).
// Both timers pause while the boss is in the centre phase. Nothing here touches soldiers.
export const BOSS7 = {
  center: { x: W / 2, y: 390 }, inv: 1,
  sweep: { strips: 6, step: 0.2, warn: 0.8, fire: 0.6, cd: 20 },
  bolt: { cd: 5, warn: 0.8, fire: 0.6 },
  arrows: { n: 3, cd: 7, minDeg: 20, maxDeg: 160, speed: 195 /* boss-6 orb 150 x 1.3 */, len: 36 * Math.SQRT2 /* 50.9 px, FE-69 */, hw: 4.5 * Math.SQRT2 /* width 12.7 px */ }
};
