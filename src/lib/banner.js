// Login banner rules (specs/ui/登入提示橫幅.md D23, ASM-08=B): counters live in localStorage only.
// guest only; never before the first settled run; at most once per page load; never again after 3 dismissals.
// Trigger flags (coinsShown, shopClicked) and the once-per-load slot are written only after a banner was really
// shown (markBannerShown, called once the banner is on screen), never when a trigger fires but no banner appears.
import { readJson, writeJson } from './storage.js';

const KEY = 'csg.banner';
let shownThisLoad = false;

const load = () => ({ closes: 0, firstRunDone: false, coinsShown: false, shopClicked: false, ...readJson(KEY, {}) });
const store = s => writeJson(KEY, s);

/** A run was settled by the backend (success only). Returns whether the "first coins" banner is still owed. */
export function markRunSettled(coinsAwarded) {
  const s = load();
  s.firstRunDone = true; store(s);
  return { firstCoins: coinsAwarded > 0 && !s.coinsShown };
}

/** True when a banner may appear now (guest, after the first settled run, < 3 dismissals, slot unused this load). */
export function bannerAllowed(isGuest) {
  const s = load();
  return Boolean(isGuest) && s.firstRunDone && s.closes < 3 && !shownThisLoad;
}

/** True when the first 購買 tap in the shop should be intercepted by the banner (D-03, human ruling A). */
export function shopBannerDue(isGuest) {
  return bannerAllowed(isGuest) && !load().shopClicked;
}

/** The banner of `kind` ('coins' | 'silver' | 'shop') is on screen: consume the slot and that trigger's flag. */
export function markBannerShown(kind) {
  shownThisLoad = true;
  const s = load();
  if (kind === 'coins') s.coinsShown = true;
  if (kind === 'shop') s.shopClicked = true;
  store(s);
}

export function dismissBanner() { const s = load(); s.closes += 1; store(s); }
