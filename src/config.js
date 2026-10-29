// Google Apps Script web-app URL that backs the leaderboard (deploy tools/leaderboard.gs, paste the URL here
// or set VITE_LEADERBOARD_URL at build time). Empty string = leaderboard hidden.
export const LEADERBOARD_URL = import.meta.env.VITE_LEADERBOARD_URL || '';
export const APP_VERSION = __APP_VERSION__;
export const NAME_MAX = 12;
