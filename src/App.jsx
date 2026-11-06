import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { useGame } from './hooks/useGame.js';
import Hud from './components/Hud.jsx';
import ChoiceBox from './components/ChoiceBox.jsx';
import RestBox from './components/RestBox.jsx';
import ConfirmBox from './components/ConfirmBox.jsx';
import HomePage from './pages/HomePage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import LoadoutPage from './pages/LoadoutPage.jsx';
import ResultPage from './pages/ResultPage.jsx';
import WalletPage from './pages/WalletPage.jsx';
import PrivacyPage from './pages/PrivacyPage.jsx';
import ChangelogPage from './pages/ChangelogPage.jsx';
import ShopPage from './pages/ShopPage.jsx';
import LeaderboardPage from './pages/LeaderboardPage.jsx';
import { getMyProgress, getMyWallet, createRun, settleRun, updateMyNickname } from './lib/api/index.js';
import { ensureSession, applyLogin, logout, subscribe, getSession } from './lib/session.js';
import { buildSettlement } from './lib/settlement.js';
import { pendingSeasonNotice, markSeasonNoticeShown } from './lib/seasonNotice.js';
import { bannerAllowed, dismissBanner, markBannerShown, markRunSettled, shopBannerDue } from './lib/banner.js';
import { nextGoal, nicknameError } from './lib/labels.js';
import { LOGIN_AVAILABLE } from './lib/env.js';
import { TILES_DATA_URI } from './game/tiles.js';

// settleRun error codes that mean the run is finished without a result (BDY-02 check failed, BDY-06 abandoned)
const SETTLE_INVALID_CODES = new Set(['gam003', 'gam004']);

// Screens: home | login | ready (silver loadout) | game | result | wallet | shop | board | privacy | changelog.
export default function App() {
  const appRef = useRef(null), stageRef = useRef(null), canvasRef = useRef(null), startKeyRef = useRef(null);
  const sceneryRef = useRef(true);
  const { ui, hint, start, restart, pause, pickChoice, pickRest, leave, summary } = useGame({ canvasRef, appRef, stageRef, startKeyRef, sceneryRef });

  const session = useSyncExternalStore(subscribe, getSession);
  const [screen, setScreen] = useState('home');
  const [loginFrom, setLoginFrom] = useState('home');
  const [progress, setProgress] = useState(null);
  const [coins, setCoins] = useState(null);
  const [loadError, setLoadError] = useState(false);
  const [nickNotice, setNickNotice] = useState(null);   // FE-24: nickname write-back failure, shown once on 首頁
  const [starting, setStarting] = useState(null);
  const [run, setRun] = useState(null);              // { runId|null, tier, buff, status, body, result, settlement, seen }
  const [lastBuff, setLastBuff] = useState(null);
  const [seasonNotice, setSeasonNotice] = useState(null);   // D38: [{ key, text }] lines still owed on 首頁
  const [banner, setBanner] = useState(null);        // { text, on: 'result' | 'shop', kind: 'coins' | 'silver' | 'shop' }

  useEffect(() => { document.documentElement.style.setProperty('--tiles', `url("${TILES_DATA_URI}")`); }, []);
  useEffect(() => {
    document.body.classList.toggle('home', screen === 'home');
    document.body.classList.toggle('in-run', screen !== 'home');
    if (screen !== 'home') window.scrollTo(0, 0);
    window.dispatchEvent(new Event('resize'));
  }, [screen]);

  const refreshAccount = useCallback(async () => {
    setLoadError(false);
    try {
      await ensureSession();
      const [p, w] = await Promise.all([getMyProgress(), getMyWallet()]);
      setProgress(p); setCoins(w.coins);
      // FE-53: the season notice rides on the wallet call 首頁 already makes (no extra request)
      setSeasonNotice(pendingSeasonNotice(w.latestSeasonReward));
      return p;
    } catch (_) { setLoadError(true); return null; }
  }, []);
  useEffect(() => { refreshAccount(); }, [refreshAccount]);

  const go = to => {
    if (screen === 'result' && run) setRun(r => ({ ...r, seen: true }));
    if (to !== 'result' && to !== 'shop') setBanner(null);
    setScreen(to);
  };
  const openLogin = from => { setLoginFrom(from); go('login'); };

  // ---- runs ----
  const launch = (tier, buff, runId) => {
    start({ tier, buff });              // engine first: its phase leaves 'end' in the same render as the screen switch
    setRun({ runId, tier, buff, status: 'playing' });
    setScreen('game');
  };
  const startBronze = async () => {
    if (starting) return;
    setStarting('bronze');
    try {
      await ensureSession();
      const r = await createRun({ tier: 'BRONZE' });
      launch('bronze', null, r.runId);
    } catch (_) {
      // Backend unreachable: bronze still plays (the game must never be blocked); the result says it was not saved.
      launch('bronze', null, null);
    } finally { setStarting(null); }
  };
  // silver loadout start: returns 'ok' | 'rejected' (gam002) | 'locked' (gam001) | 'network'
  const startSilver = async buff => {
    try {
      const r = await createRun({ tier: 'SILVER', buffItems: buff ? [buff] : [] });
      setLastBuff(buff);
      launch('silver', buff, r.runId);
      return 'ok';
    } catch (e) {
      if (e.code === 'gam002') return 'rejected';
      if (e.code === 'gam001') { refreshAccount(); go('home'); return 'locked'; }
      return 'network';
    }
  };
  // same run, no new createRun, no buff spent again (AMB-01). The engine restarts with the tier and buff it is
  // running (FE-43), so the loadout can never be lost through React state; settlement keeps run.runId.
  const restartNow = () => restart();
  // 回首頁 from the restart box (user 2026-11-04): the run is dropped unsettled (no coins, an equipped buff stays spent;
  // the backend run is left open and times out as abandoned), the engine goes back to its intro state.
  const leaveRun = () => { leave(); setRun(null); refreshAccount(); go('home'); };

  const settle = useCallback(async (r) => {
    setRun(x => ({ ...x, status: 'submitting' }));
    try {
      const s = await settleRun({ runId: r.runId, settlement: r.body });
      setCoins(s.walletCoins);
      const { firstCoins } = markRunSettled(s.coinsAwarded);
      setRun(x => ({ ...x, status: 'ok', settlement: s }));
      getMyProgress().then(setProgress, () => {});
      const guest = getSession() && getSession().playerKind === 'GUEST';
      const kind = s.isSilverNewlyUnlocked ? 'silver' : firstCoins ? 'coins' : null;
      const text = kind === 'silver' ? '登入保存你的 EXTRA 解鎖' : kind === 'coins' ? '登入保存 ' + s.coinsAwarded + ' 金幣' : null;
      if (LOGIN_AVAILABLE && text && bannerAllowed(guest)) setBanner({ text, on: 'result', kind });
    } catch (e) {
      // D-02 (結算對局 BDY-02/06): only gam003 (check failed) / gam004 (abandoned) make the run 本局成績無效 — the run
      // is over on the backend. Everything else (network, 5xx, other 4xx, schema drift) is a retryable error and
      // the run is not marked settled; a resend is idempotent on the backend.
      if (e.kind === 'http' && SETTLE_INVALID_CODES.has(e.code)) { markRunSettled(0); setRun(x => ({ ...x, status: 'invalid' })); }
      else setRun(x => ({ ...x, status: 'error' }));
    }
  }, []);

  // a finished game goes to the result screen and settles once
  useEffect(() => {
    if (ui.phase !== 'end' || screen !== 'game' || !run || run.status !== 'playing') return;
    const sum = summary();
    const body = buildSettlement(sum);
    const next = { ...run, body, result: sum.result, status: run.runId ? 'submitting' : 'offline' };
    setRun(next);
    setScreen('result');
    if (run.runId) settle(next);
  }, [ui.phase, screen, run, summary, settle]);

  const again = () => {
    if (!run || run.tier === 'bronze') { go('home'); startBronze(); }
    else go('ready');
  };

  // FE-24: a NEW account starts with the system default nickname, so the guest's nickname is written back;
  // an EXISTING account keeps its own. A failed write-back never blocks the login: the backend nickname stays.
  const onLoggedIn = async (detail, { targetKind } = {}) => {
    const guestNick = session && session.playerKind === 'GUEST' && progress ? progress.nickname : null;
    applyLogin(detail);
    setBanner(null);
    setNickNotice(null);
    const p = await refreshAccount();
    if (targetKind === 'NEW' && guestNick && p && p.nickname !== guestNick) {
      try { setProgress(await updateMyNickname({ nickname: guestNick })); }
      catch (e) { setNickNotice(nicknameError(e.code)); }
    }
    go(loginFrom);
  };

  const playing = screen === 'game' && ui.phase !== 'intro' && ui.phase !== 'end';
  sceneryRef.current = screen !== 'game';
  const bannerFor = on => (banner && banner.on === on ? banner.text : null);
  const closeBanner = () => { dismissBanner(); setBanner(null); };
  // banner flags are written only once a banner is really on screen (D-03; same rule for the result banners)
  const bannerShown = () => { if (banner) markBannerShown(banner.kind); };
  // D-03 (human ruling A): the first 購買 tap as a guest shows the banner instead of buying; returns true when it did
  const interceptBuy = () => {
    const guest = getSession() && getSession().playerKind === 'GUEST';
    if (!LOGIN_AVAILABLE || !shopBannerDue(guest)) return false;
    setBanner({ text: '尚未登入，帳號資料可能會消失，建議先登入', on: 'shop', kind: 'shop' });
    return true;
  };
  const goal = nextGoal(progress);

  let page = null;
  if (screen === 'home') {
    page = <HomePage session={session} progress={progress} coins={coins} loadError={loadError} onRetry={refreshAccount}
      onNickname={setProgress} nickNotice={nickNotice} onNickNoticeShown={() => setNickNotice(null)} onBronze={startBronze} onSilver={() => go('ready')} onGo={go} starting={starting}
      onLogin={() => openLogin('home')} onLogout={async () => { try { await logout(); } catch (_) { /* offline: next call recovers */ } refreshAccount(); }}
      loginAvailable={LOGIN_AVAILABLE} seasonNotice={seasonNotice}
      onSeasonNoticeShown={() => { if (seasonNotice) markSeasonNoticeShown(seasonNotice); }} onSeasonNoticeClose={() => setSeasonNotice(null)} />;
  } else if (screen === 'login') page = <LoginPage onBack={() => go(loginFrom)} onLoggedIn={onLoggedIn} />;
  else if (screen === 'ready') page = <LoadoutPage preselect={lastBuff} onBack={() => go('home')} onStart={startSilver} />;
  else if (screen === 'result' && run && run.body) {
    page = <ResultPage run={run} goal={goal} banner={bannerFor('result')} onAgain={again} onHome={() => { refreshAccount(); go('home'); }}
      onRetry={() => settle(run)} onBannerLogin={() => openLogin('result')} onBannerClose={closeBanner} onBannerShown={bannerShown} />;
  } else if (screen === 'wallet') page = <WalletPage onBack={() => go('home')} onShop={() => go('shop')} onCoins={setCoins} />;
  else if (screen === 'shop') {
    page = <ShopPage onBack={() => go('home')} coins={coins} onCoins={setCoins} banner={bannerFor('shop')}
      onBuyIntercept={interceptBuy} onBannerDone={() => setBanner(null)}
      onBannerLogin={() => openLogin('shop')} onBannerClose={closeBanner} onBannerShown={bannerShown} />;
  } else if (screen === 'board') page = <LeaderboardPage onBack={() => go('home')} onPlay={() => go('home')} />;
  else if (screen === 'privacy') page = <PrivacyPage onBack={() => go('home')} />;
  else if (screen === 'changelog') page = <ChangelogPage onBack={() => go('home')} />;

  return (
    <div id="app" ref={appRef}>
      <div id="stage" ref={stageRef} data-menu={screen !== 'game' ? '' : undefined}>
        <canvas id="c" ref={canvasRef} aria-label="戰場" role="img" />
        <Hud />
        <div id="hint" style={{ opacity: hint && screen === 'game' ? 1 : 0 }}>左右拖曳移動砲台</div>
        {playing && (
          <button id="restart" type="button" aria-label="重新開始" title="重新開始"
            onClick={e => { pause(true); e.currentTarget.blur(); }}>&#8635;</button>
        )}
        {screen === 'game' && ui.phase === 'paused' && <ConfirmBox onYes={restartNow} onNo={() => pause(false)} onHome={leaveRun} />}
        {screen === 'game' && ui.phase === 'rest' && <RestBox onPick={pickRest} />}
        {screen === 'game' && ui.phase === 'choice' && <ChoiceBox cards={ui.choiceCards} onPick={pickChoice} />}
        {page && <div className="v19">{page}</div>}
      </div>
    </div>
  );
}
