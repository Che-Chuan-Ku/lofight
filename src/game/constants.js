// Game constants (logical playfield is 390x780, letterboxed by the stage).
export const W = 390, H = 780, GATE_Y = 430, GATE_H = 28, PATH_HW = 30;
export const GATE_MULT = 10;
export const MAX_EXTRA_H = 160;   // tall screens: extra logical height added above the battlefield (more red sea)
export const MAX_CROP_H = 140;    // short portrait screens: how much of the top may be cropped to reach full width
export const SHOT_SPEED = 420;        // px/s, soldiers fired from the turret
export const MAXB = 9999, MAXR = 4500;   // over MAXB the rear of the blue column is culled; the turret never stops
export const FIRE_INTERVAL = 0.18;   // base seconds between shots (~5.6/s); SPD rewards shorten it via S.fireMul
export const FIRE_MIN = 0.1;         // fastest allowed fire interval
export const GATE_RESPAWN = 2.0;   // seconds a broken gate stays open before it rebuilds
export const GATE_X0 = 16, GATE_GAP = 14, GATE_MIN = 44, GATE_STEP = 40;
// sprites: Kenney "Tiny Dungeon", CC0 (kenney.nl), 16x16 tiles, 12 columns
export const UNIT_PX = 11;
export const BLUE_TILE = 96, RED_TILE = 111;
export const BOSS_TILES = [110, 112, 122, 120, 124, 109, 121, 123];
export const MINION_TILE = 108;
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
// Hidden stage 9, offered once boss 8 falls. Every beam telegraphs for `warn` seconds before it
// `fire`s; a turret inside a firing beam ends the run. `hw` is the beam half-width.
export const SECRET_TILE = 84;
export const SECRET_BOSS = {
  hp: 150000, v: 19, r: 34,
  solo: { cd: 5, warn: 1.2, fire: 0.6, hw: 14 },      // down from the boss: straight pillar or S-curve, picked at random
  wide: { cd: 10, warn: 1.5, fire: 0.7, hw: 11 },     // from half hp: ten-ray star through the boss
  scurve: { amp: 60, wl: 260, triGap: 90 },            // lateral offset = amp * sin(2π * dy / wl); triGap spaces the enraged triple cast
  rays: 10                                             // star beams (lines through the boss = rays / 2)
};
export const BEAM_HIT_R = 16;   // turret radius for beam hits
