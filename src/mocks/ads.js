// Dev-only stand-in for the Google Ad Placement API (loaded with MSW; never in production builds).
// window.__mockAds controls it: fill (beforeReward arrives or not), outcome ('viewed' | 'dismissed'), playMs,
// publisher(on) puts a test publisher id into <meta name="adsense-publisher-id"> or clears it.
const TEST_PUBLISHER = 'ca-pub-0000000000000000';

export function installMockAds() {
  const meta = document.querySelector('meta[name="adsense-publisher-id"]');
  const ctl = {
    fill: true, outcome: 'viewed', playMs: 300, calls: [],
    publisher(on) { if (meta) meta.setAttribute('content', on ? TEST_PUBLISHER : ''); }
  };
  if (meta && /^(__[A-Z_]+__)?$/.test(meta.getAttribute('content') || '')) ctl.publisher(true);
  window.adsbygoogle = window.adsbygoogle || [];
  window.adBreak = window.adConfig = o => {
    ctl.calls.push(o.type + ':' + (o.name || ''));
    if (o.type !== 'reward') return;
    setTimeout(() => {
      if (!ctl.fill) { if (o.adBreakDone) o.adBreakDone({ breakStatus: 'notReady' }); return; }
      let used = false;
      o.beforeReward(() => {
        if (used) return; used = true;
        if (o.beforeAd) o.beforeAd();
        setTimeout(() => {
          if (ctl.outcome === 'viewed') { if (o.adViewed) o.adViewed(); } else if (o.adDismissed) o.adDismissed();
          if (o.afterAd) o.afterAd();
          if (o.adBreakDone) o.adBreakDone({ breakStatus: ctl.outcome });
        }, ctl.playMs);
      });
    }, 30);
  };
  window.__mockAds = ctl;
}
