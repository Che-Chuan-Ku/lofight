// 看廣告卡片 (specs/ui/商店頁.md D33-D35, R-AD1..R-AD5; prototype adCard(); _interaction-rules.md §7.1).
// Google official reward flow: adBreak({type:'reward'}) on shop entry -> beforeReward(showAdFn) enables the card ->
// a tap calls showAdFn() -> only adViewed claims (claimAdReward) -> adBreakDone returns to the card state.
// Google draws the ad, its overlay and close button; this card never does (R-AD1). Copy = action + reward only (R-AD5).
import { useCallback, useEffect, useRef, useState } from 'react';
import { PrimaryButton } from './Common.jsx';
import { AdScreen } from './Icons.jsx';
import { claimAdReward } from '../../lib/api/index.js';
import { getDeviceId } from '../../lib/session.js';
import { adsConfigured, requestRewardAd } from '../../lib/ads.js';
import { NETWORK_MSG } from '../../lib/labels.js';

const COOLDOWN_FALLBACK_MS = 60000;   // eco002 without a known nextClaimableAt (backend cooldown, AMB-A1=a)
const pad = n => String(n).padStart(2, '0');
const hhmm = iso => { const d = new Date(iso); return pad(d.getHours()) + ':' + pad(d.getMinutes()); };

/** The game has no audio today; beforeAd / afterAd still flip one page-wide flag so future sound can honour it. */
const setAdPlaying = on => { if (typeof document !== 'undefined') document.documentElement.toggleAttribute('data-ad-playing', on); };

/**
 * status: listShopItems.adReward ({ isEnabled, remainingCount, nextClaimableAt }).
 * onCoins(walletCoins) updates the coin chip after a claim.
 */
export default function AdRewardCard({ status, onCoins }) {
  const [st, setSt] = useState(status);
  const [hidden, setHidden] = useState(false);         // eco003 hides the card for this visit
  const [phase, setPhase] = useState('idle');          // idle | playing | claiming
  const [fill, setFill] = useState(false);             // a showAdFn from beforeReward is waiting
  const [msg, setMsg] = useState(null);                // ok | dismissed | limit | cool | net
  const [now, setNow] = useState(() => Date.now());
  const showFn = useRef(null), live = useRef(true), arming = useRef(false), shown = useRef(false);
  const visible = adsConfigured() && st && st.isEnabled && !hidden;

  const claim = useCallback(async () => {
    setPhase('claiming'); setMsg(null);
    try {
      const r = await claimAdReward({ deviceId: getDeviceId() });
      if (!live.current) return;
      onCoins(r.walletCoins);
      setSt({ isEnabled: true, remainingCount: r.remainingCount, nextClaimableAt: r.nextClaimableAt });
      setMsg('ok');
    } catch (e) {
      if (!live.current) return;
      // error code -> own copy; the backend message is never shown, listShopItems is not called again (INF-A8, INF-A15)
      if (e.code === 'eco001') { setSt(s => ({ ...s, remainingCount: 0 })); setMsg('limit'); }
      else if (e.code === 'eco002') {
        setSt(s => (s.nextClaimableAt && Date.parse(s.nextClaimableAt) > Date.now() ? s : { ...s, nextClaimableAt: new Date(Date.now() + COOLDOWN_FALLBACK_MS).toISOString() }));
        setMsg('cool');
      } else if (e.code === 'eco003') setHidden(true);
      else setMsg('net');   // SPEC-03 (FE-37): the ad was watched; 重試 sends the same claim again
    } finally { if (live.current) setPhase('idle'); }
  }, [onCoins]);

  // Preload one rewarded break; beforeReward is what enables the card (R-AD4).
  const arm = useCallback(() => {
    if (arming.current) return;
    arming.current = true; shown.current = false;
    const started = requestRewardAd({
      beforeReward: showAdFn => { if (!live.current) return; showFn.current = showAdFn; setFill(true); },
      beforeAd: () => { shown.current = true; setAdPlaying(true); },
      afterAd: () => setAdPlaying(false),
      adViewed: () => { if (live.current) claim(); },
      adDismissed: () => { if (live.current) setMsg('dismissed'); },
      adBreakDone: () => {
        arming.current = false; showFn.current = null; setAdPlaying(false);
        if (!live.current) return;
        setFill(false);
        setPhase(p => (p === 'playing' ? 'idle' : p));
        // showAdFn is spent: ask for the next one only after an ad was really shown (no retry loop on no-fill)
        if (shown.current) arm();
      }
    });
    if (!started) arming.current = false;
  }, [claim]);

  useEffect(() => {
    live.current = true;
    if (visible) arm();
    return () => { live.current = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  const cooling = !!(st && st.nextClaimableAt && Date.parse(st.nextClaimableAt) > now);
  const limit = !!(st && st.remainingCount === 0);
  // cooldown countdown: one tick per second, then a single re-arm if no ad is waiting
  useEffect(() => {
    if (!visible || !cooling) return undefined;
    const t = setInterval(() => {
      const n = Date.now(); setNow(n);
      if (Date.parse(st.nextClaimableAt) <= n) { clearInterval(t); setMsg(m => (m === 'ok' || m === 'cool' ? null : m)); if (!showFn.current) arm(); }
    }, 1000);
    return () => clearInterval(t);
  }, [visible, cooling, st, arm]);
  useEffect(() => { if (msg !== 'ok') return undefined; const t = setTimeout(() => setMsg(m => (m === 'ok' ? null : m)), 4000); return () => clearTimeout(t); }, [msg]);

  if (!visible) return null;
  const state = phase !== 'idle' ? phase : limit ? 'limit' : cooling ? 'cooldown' : fill ? 'ready' : 'nofill';
  const stat = {
    ready: '今天還可以看 ' + st.remainingCount + ' 次',
    nofill: '目前沒有廣告，稍後再試',
    cooldown: Math.max(1, Math.ceil((Date.parse(st.nextClaimableAt) - now) / 1000)) + ' 秒後可以再看',
    limit: '今天的次數用完了' + (st.nextClaimableAt ? '，' + hhmm(st.nextClaimableAt) + ' 後可以再看' : ''),
    playing: '廣告播放中…',
    claiming: '領取中…'
  }[state];
  const busy = state === 'playing' || state === 'claiming', off = state !== 'ready';
  const watch = () => {
    if (state !== 'ready' || !showFn.current) return;   // disabled states: nothing is called, nothing is counted
    const fn = showFn.current; showFn.current = null;
    setMsg(null); setPhase('playing');
    fn();
  };
  return (
    <div className={'card adcard' + (off && !busy ? ' idle' : '')} data-ad-state={state}>
      <AdScreen size={32} />
      <div className="info"><span className="name" id="ad-t">觀看廣告，獲得 200 金幣</span><span className="stat" id="ad-stat">{stat}</span></div>
      <div className="buy">
        <PrimaryButton className="sm" busy={busy} onClick={watch} aria-describedby="ad-t ad-stat" aria-disabled={off ? 'true' : undefined}>觀看</PrimaryButton>
      </div>
      <div className="msgline" aria-live="polite">
        {msg === 'ok' && <span className="msg ok">獲得 200 金幣</span>}
        {msg === 'dismissed' && <span className="msg info">沒有看完，這次沒有金幣</span>}
        {msg === 'limit' && <span className="msg err">今天的次數用完了，這次沒有金幣</span>}
        {msg === 'cool' && <span className="msg err">還要等一下才能再領，這次沒有金幣</span>}
        {msg === 'net' && <><span className="msg err">{NETWORK_MSG}</span>
          <button type="button" className="btn-ghost sm" disabled={phase === 'claiming'} onClick={claim}>重試</button></>}
      </div>
    </div>
  );
}
