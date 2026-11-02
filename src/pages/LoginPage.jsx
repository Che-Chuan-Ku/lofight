// 登入頁 (specs/ui/登入頁.md; prototype login()). Google only (Apple deferred, BDY-01=B).
// Flow: Google id_token -> getMergePreview (nothing written) -> 確認合併 -> loginGoogle with the guest token (merge).
import { useEffect, useRef, useState } from 'react';
import { PageHeader, PrimaryButton } from '../components/ui/Common.jsx';
import { GLogo } from '../components/ui/Icons.jsx';
import { getMergePreview, loginGoogle } from '../lib/api/index.js';
import { ensureSession, getAccessToken, recover } from '../lib/session.js';
import { MOCK_API } from '../lib/env.js';
import { renderGoogleButton } from '../lib/googleIdentity.js';
import { BUFFS, BUFF_ORDER, NETWORK_MSG } from '../lib/labels.js';

const FAIL_MSG = '登入驗證失敗，請再試一次';

function bestText(scores, tier) {
  const s = scores.filter(x => x.tier === tier).sort((a, b) => (a.isVictory === b.isVictory ? (a.isVictory ? a.gameSeconds - b.gameSeconds : b.gameSeconds - a.gameSeconds) : a.isVictory ? -1 : 1))[0];
  // GAP-2: BestScore has win/seconds, not a boss count
  if (!s) return '無';
  return s.isVictory ? '勝 ' + s.gameSeconds + ' 秒' : '撐了 ' + s.gameSeconds + ' 秒';
}

function MergeCard({ preview }) {
  const existing = preview.targetKind === 'EXISTING';
  const buffs = BUFF_ORDER.map(b => [b, (preview.guestBuffInventories.find(x => x.buffItem === b) || { quantity: 0 }).quantity]).filter(([, q]) => q > 0);
  return (
    <>
      <section className="panel merge" aria-labelledby="merge-t">
        <h2 id="merge-t">將合併到你的帳號</h2>
        <dl>
          <div className="row"><dt>目標帳號</dt><dd><span className="tag acct">{existing ? '已有帳號' : '新帳號'}</span></dd></div>
          <div className="row"><dt>金幣</dt><dd className="gold">{preview.guestCoins || '無'}</dd></div>
          <div className="row"><dt>buff</dt><dd>{buffs.length ? buffs.map(([b, q]) => <span key={b} className="tag buff">{BUFFS[b].name} ×{q}</span>) : '無'}</dd></div>
          <div className="row"><dt>SILVER 解鎖</dt><dd>{preview.guestIsSilverUnlocked ? '是' : '否'}</dd></div>
          <div className="row"><dt>成績</dt><dd>bronze {bestText(preview.guestBestScores, 'BRONZE')}・silver {bestText(preview.guestBestScores, 'SILVER')}</dd></div>
        </dl>
      </section>
      {existing && <p className="msg info">這個 Google 帳號已有進度，上面的內容會加進去。</p>}
    </>
  );
}

function GoogleButton({ busy, onToken }) {
  const slot = useRef(null);
  const [gsiError, setGsiError] = useState(false);
  useEffect(() => {
    if (MOCK_API || !slot.current) return;
    renderGoogleButton(slot.current, onToken, slot.current.clientWidth).catch(() => setGsiError(true));
  }, [onToken]);
  if (!MOCK_API) {
    return (
      <>
        <div className="gsi" ref={slot} aria-busy={busy ? 'true' : undefined} style={busy ? { pointerEvents: 'none', opacity: 0.6 } : undefined} />
        {gsiError && <p className="msg err center" role="alert">{NETWORK_MSG}</p>}
      </>
    );
  }
  // dev mock: the prototype's own button; the token is read from localStorage 'mock.googleToken' (default a valid one)
  const mockToken = () => { try { return localStorage.getItem('mock.googleToken') || 'g-ok-2'; } catch (_) { return 'g-ok-2'; } };
  return (
    <button type="button" className="gbtn" disabled={busy} aria-busy={busy ? 'true' : undefined} onClick={() => onToken(mockToken())}>
      {busy ? <span className="spin" aria-hidden="true" /> : <GLogo />}使用 Google 登入
    </button>
  );
}

export default function LoginPage({ onBack, onLoggedIn }) {
  const [st, setSt] = useState({ step: 'choose' });   // choose | verifying | fail | merge | merging
  const tokenRef = useRef(null);

  const onToken = useRef(null);
  onToken.current = async idToken => {
    tokenRef.current = idToken;
    setSt({ step: 'verifying' });
    try {
      await ensureSession();
      const preview = await getMergePreview({ loginProvider: 'GOOGLE', providerToken: idToken });
      setSt({ step: 'merge', preview });
    } catch (e) {
      setSt({ step: 'fail', msg: e.kind === 'network' ? NETWORK_MSG : FAIL_MSG });
    }
  };
  const stableOnToken = useRef(t => onToken.current(t)).current;

  const confirm = async () => {
    if (st.step === 'merging') return;
    setSt(s => ({ ...s, step: 'merging' }));
    try {
      let detail;
      try {
        detail = await loginGoogle({ idToken: tokenRef.current, guestAccessToken: getAccessToken() });
      } catch (e) {
        // the guest token expired while the preview was open: renew it once and merge again
        if (e.kind !== 'http' || e.code !== '401001') throw e;
        const fresh = await recover();
        if (!fresh) throw e;
        detail = await loginGoogle({ idToken: tokenRef.current, guestAccessToken: fresh });
      }
      await onLoggedIn(detail, { targetKind: st.preview.targetKind });
    } catch (e) {
      setSt({ step: 'fail', msg: e.kind === 'network' ? NETWORK_MSG : FAIL_MSG });
    }
  };

  if (st.step === 'merge' || st.step === 'merging') {
    return (
      <div className="page menu enter">
        <PageHeader title="保存進度" onBack={onBack} />
        <main className="content"><MergeCard preview={st.preview} /></main>
        <footer className="actionbar">
          <PrimaryButton className="full" busy={st.step === 'merging'} onClick={confirm} aria-describedby="merge-t">確認合併</PrimaryButton>
          <button type="button" className="btn-ghost" onClick={onBack}>先不要</button>
        </footer>
      </div>
    );
  }
  return (
    <div className="page menu enter">
      <PageHeader title="保存進度" onBack={onBack} />
      <main className="content" style={{ justifyContent: 'center', gap: 'var(--space-6)', paddingBottom: 120 }}>
        <div className="panel" style={{ padding: '24px 16px', display: 'flex', flexDirection: 'column', gap: 20 }}>
          {st.step === 'fail'
            ? <p className="msg err center" role="alert">{st.msg}</p>
            : <p className="lead">登入後，金幣、buff 和成績都會保存在你的帳號，換手機也能接著玩。</p>}
          <GoogleButton busy={st.step === 'verifying'} onToken={stableOnToken} />
        </div>
      </main>
    </div>
  );
}
