// 首頁 (specs/ui/首頁.md; prototype home()). Nickname, tier cards, coin chip, shop / leaderboard entries, login state.
import { useEffect, useRef, useState } from 'react';
import { CoinChip, useTitleFocus, ConfirmDialog } from '../components/ui/Common.jsx';
import { TierBadge, BuffMark, LinktreeMark } from '../components/ui/Icons.jsx';
import { updateMyNickname } from '../lib/api/index.js';
import { lockLine, nicknameError, NETWORK_MSG } from '../lib/labels.js';
import { APP_VERSION, LINKTREE_URL } from '../config.js';

function NicknameField({ progress, onSaved, notice, onNoticeShown }) {
  const [value, setValue] = useState(progress ? progress.nickname : '');
  const [err, setErr] = useState('');
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const input = useRef(null);
  const original = progress ? progress.nickname : '';
  useEffect(() => { setValue(original); }, [original]);
  // an error handed over from outside (FE-24 write-back after login) uses the same message slot
  useEffect(() => { if (notice) { setErr(notice); onNoticeShown(); } }, [notice, onNoticeShown]);
  useEffect(() => { if (!saved) return undefined; const t = setTimeout(() => setSaved(false), 2000); return () => clearTimeout(t); }, [saved]);

  const commit = async () => {
    if (!progress || saving || value === original) return;
    setSaving(true); setErr(''); setSaved(false);
    try {
      onSaved(await updateMyNickname({ nickname: value }));
      setSaved(true);
    } catch (e) {
      setErr(nicknameError(e.code)); setValue(original);
      if (input.current) input.current.focus();
    } finally { setSaving(false); }
  };
  return (
    <div className="nick">
      <label className="label" htmlFor="nick">暱稱</label>
      <input id="nick" ref={input} type="text" value={value} placeholder="輸入暱稱" autoComplete="off" enterKeyHint="done"
        disabled={!progress} style={saving ? { opacity: 0.6 } : undefined}
        className={err ? 'err' : undefined} aria-invalid={err ? 'true' : undefined} aria-describedby={err ? 'nick-msg' : undefined}
        onChange={e => { setValue(e.target.value); if (err) setErr(''); }}
        onBlur={commit} onKeyDown={e => { if (e.key === 'Enter') e.currentTarget.blur(); }} />
      {err ? <span id="nick-msg" className="msg err" role="alert">{err}</span>
        : saved ? <span className="msg ok fade" role="status">已更新</span> : null}
    </div>
  );
}

export default function HomePage({ session, progress, coins, loadError, onRetry, onNickname, nickNotice, onNickNoticeShown, onBronze, onSilver, onGo, onLogin, onLogout, loginAvailable, starting }) {
  const h1 = useTitleFocus();
  const [shake, setShake] = useState(false);
  const [dialog, setDialog] = useState(false);
  const logoutBtn = useRef(null);
  const member = session && session.playerKind === 'MEMBER';
  const unlocked = !!(progress && progress.isSilverUnlocked);
  const best = progress ? progress.bronzeBestBossesKilled : null;

  // GAP-1: getMyProgress only reports the bronze best while silver is locked; once unlocked the line is hidden.
  let bronzeBest = null;
  if (progress && !unlocked) bronzeBest = best >= 8 ? '目前最佳：全破' : best ? '目前最佳：第 ' + best + ' 王' : '目前最佳：尚無成績';

  const tapLocked = () => { setShake(false); requestAnimationFrame(() => setShake(true)); };
  const closeDialog = () => { setDialog(false); requestAnimationFrame(() => logoutBtn.current && logoutBtn.current.focus()); };

  return (
    <div className="page menu enter">
      <div className="topbar">
        {member ? (
          <div className="acct"><span className="who">{progress ? progress.nickname + ' 已登入' : '已登入'}</span><button type="button" className="btn-ghost sm" ref={logoutBtn} onClick={() => setDialog(true)}>登出</button></div>
        ) : loginAvailable ? (
          <button type="button" className="btn-ghost sm" onClick={onLogin}>登入</button>
        ) : <span className="slot44" aria-hidden="true" />}
        <div className="topright">
          {/* D36: secondary entry next to the coin chip; mark <= 70% of the chip height, 44px hit area */}
          <a className="lt" href={LINKTREE_URL} target="_blank" rel="noopener noreferrer" aria-label="Linktree"><LinktreeMark size={24} /></a>
          <CoinChip coins={coins} onClick={() => onGo('wallet')} />
        </div>
      </div>
      <main className="content" style={{ overflow: 'hidden', paddingBottom: 'var(--space-12)', gap: 'var(--space-4)' }}>
        <div className="hero"><h1 tabIndex={-1} ref={h1}>人海攻城</h1><p>左右移動砲台，守住陣地。</p></div>
        <NicknameField progress={progress} onSaved={onNickname} notice={nickNotice} onNoticeShown={onNickNoticeShown} />
        {loadError && <p className="msg err center" role="alert">{NETWORK_MSG}<button type="button" className="btn-ghost sm" onClick={onRetry}>重試</button></p>}
        <div className="grid2" style={{ marginTop: 'auto' }}>
          <button type="button" className="tier bronze" onClick={onBronze} aria-busy={starting === 'bronze' ? 'true' : undefined}
            aria-label={'NORMAL，8 王' + (bronzeBest ? '，' + bronzeBest : '') + '，點擊開局'}>
            <TierBadge tier="bronze" size={40} /><span className="ct">NORMAL</span><span className="cd">8 王</span>
            {bronzeBest && <span className="best">{bronzeBest}</span>}
          </button>
          {unlocked ? (
            // AMB-N1=C: boss count stays, "can carry a buff" is the buff mark instead of words
            <button type="button" className="tier silver" onClick={onSilver} aria-label="EXTRA，6 王，buff">
              <TierBadge tier="silver" size={40} /><span className="ct">EXTRA</span>
              <span className="cd meta">6 王<span className="buffmark"><BuffMark size={18} /></span></span>
            </button>
          ) : (
            <button type="button" className={'tier silver locked' + (shake ? ' shake' : '')} aria-disabled="true" aria-describedby="silver-cond"
              onClick={tapLocked} onAnimationEnd={e => { if (e.target === e.currentTarget) setShake(false); }}>
              <TierBadge tier="silver" size={28} /><span className="ct">EXTRA</span><span className="lock" aria-hidden="true" />
              <span className="cd cond" id="silver-cond">
                {progress ? <>{lockLine(progress.silverUnlockBossSeq, best).replace(/（.*$/, '')}<br />{lockLine(progress.silverUnlockBossSeq, best).replace(/^[^（]*/, '')}</> : '　'}
              </span>
            </button>
          )}
        </div>
        <nav aria-label="更多">
          <div className="grid2">
            <button type="button" className="btn-secondary lg" onClick={() => onGo('shop')}>商店</button>
            <button type="button" className="btn-secondary lg" onClick={() => onGo('board')}>排行榜</button>
          </div>
        </nav>
      </main>
      <footer className="sitefoot">
        <button type="button" className="linkbtn" onClick={() => onGo('privacy')}>隱私權政策</button>
        <span className="ver" aria-label={'版本 ' + APP_VERSION}>v{APP_VERSION}</span>
      </footer>
      {dialog && (
        <ConfirmDialog title="登出？" desc="這台裝置會從新的訪客開始。你的帳號進度不會消失，再登入就回來。" action="登出"
          onCancel={closeDialog} onConfirm={() => { setDialog(false); onLogout(); }} />
      )}
    </div>
  );
}
