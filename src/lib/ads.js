// Google Ad Placement API (H5 Games Ads) for the shop's rewarded card (specs/ui/商店頁.md R-AD1..R-AD5, D33-D35).
// Google draws the ad, its overlay and close button; this module only loads the API and forwards adBreak().
import { adsensePublisherId } from './env.js';

const SCRIPT_SRC = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js';
let installed = false;

/** Loads adsbygoogle.js once when a publisher id is configured; without one nothing is loaded (D35). */
export function installAds() {
  const id = adsensePublisherId();
  if (!id || installed || typeof window === 'undefined') return !!id;
  installed = true;
  window.adsbygoogle = window.adsbygoogle || [];
  // a dev stand-in (src/mocks/ads.js) may already provide adBreak; then the real script is not fetched
  const standIn = typeof window.adBreak === 'function';
  if (!standIn) {
    window.adBreak = window.adConfig = o => window.adsbygoogle.push(o);
    const s = document.createElement('script');
    s.async = true;
    s.src = SCRIPT_SRC + '?client=' + encodeURIComponent(id);
    s.crossOrigin = 'anonymous';
    s.setAttribute('data-ad-client', id);
    s.setAttribute('data-ad-frequency-hint', '30s');
    // test ads outside production builds only (infrastructure.yml site_ads.test_flag)
    if (import.meta.env.MODE !== 'production') s.setAttribute('data-adbreak-test', 'on');
    document.head.appendChild(s);
  }
  return true;
}

/** True when the page may offer a rewarded ad at all (publisher id present). */
export const adsConfigured = () => !!adsensePublisherId();

/**
 * One rewarded break (Google order: beforeReward -> [showAdFn() -> beforeAd -> adViewed | adDismissed -> afterAd] -> adBreakDone).
 * Returns false when no ad API is available.
 */
export function requestRewardAd(callbacks) {
  if (!installAds() || typeof window.adBreak !== 'function') return false;
  window.adBreak({ type: 'reward', name: 'shop_coins', ...callbacks });
  return true;
}
