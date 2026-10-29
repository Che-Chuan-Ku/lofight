// Google Apps Script web-app URL that backs the leaderboard (deploy tools/leaderboard.gs, paste the URL here
// or set VITE_LEADERBOARD_URL at build time). Empty string = leaderboard hidden.
export const LEADERBOARD_URL = import.meta.env.VITE_LEADERBOARD_URL || '';
export const APP_VERSION = __APP_VERSION__;
// Leaderboard key is major.minor only: a patch release (18.6.3 -> 18.6.4) keeps the board, a minor bump resets it.
export const BOARD_VERSION = APP_VERSION.split('.').slice(0, 2).join('.');
export const BOARD_SHOW = 15;
export const NAME_MAX = 12;
