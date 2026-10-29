// Game constants (logical playfield is 390x780, letterboxed by the stage).
export const W = 390, H = 780, GATE_Y = 430, GATE_H = 28, PATH_HW = 30;
export const GATE_MULT = 10;
export const MAX_EXTRA_H = 160;   // tall screens: extra logical height added above the battlefield (more red sea)
export const MAX_CROP_H = 140;    // short portrait screens: how much of the top may be cropped to reach full width
export const SHOT_SPEED = 420;        // px/s, soldiers fired from the turret
export const MAXB = 9000, MAXR = 5000;   // over MAXB the rear of the blue column is culled; the turret never stops
export const FIRE_INTERVAL = 0.2;    // seconds between shots (5/s); never changes, rewards multiply soldiers per shot instead
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
  { at: 56, hp: 30000, v: 17, r: 22 },
  { at: 70, hp: 48000, v: 18, r: 24 },
  { at: 84, hp: 80000, v: 20, r: 27 },
  { at: 98, hp: 100000, v: 21, r: 30 },
  { at: Infinity, hp: 130000, v: 21, r: 32, summon: { n: 5, hp: 3000, cd: 5 } }
];
