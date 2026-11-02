// 局末結算畫面 (specs/ui/局末結算畫面.md; prototype result()).
// status: submitting | ok | invalid (gam003/gam004) | error (network, retry is idempotent) | offline (run never opened)
import { useEffect, useRef, useState } from 'react';
import { BossIcon, RetryBox, Skeletons, LoginBanner, useTitleFocus } from '../components/ui/Common.jsx';
import { deathLine } from '../lib/settlement.js';

const STEP = 250;

function GoalLine({ goal, late }) {
  if (!goal) return null;
  return (
    <div className={'goal' + (late ? ' late' : '')}>
      <span className="flag" aria-hidden="true" />
      <span>{goal.text || <>{goal.pre}<b className="gold">{goal.num}</b>{goal.post}</>}</span>
    </div>
  );
}
const reduced = () => window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

function Breakdown({ settlement, onDone, skip, animate }) {
  const rewards = settlement.bossRewards;
  const [total, setTotal] = useState(animate ? 0 : settlement.coinsAwarded);
  useEffect(() => {
    if (!animate || skip) { setTotal(settlement.coinsAwarded); onDone(); return undefined; }
    const timers = rewards.map((r, i) => setTimeout(() => setTotal(t => t + r.coins), (i + 1) * STEP));
    timers.push(setTimeout(onDone, rewards.length * STEP + 50));
    return () => timers.forEach(clearTimeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [skip]);
  return (
    <>
      {rewards.length ? rewards.map((r, i) => (
        <div key={r.bossSeq} className={'row' + (animate && !skip ? ' stamp' : '')} style={animate && !skip ? { animationDelay: i * STEP + 'ms' } : undefined}>
          <BossIcon seq={r.bossSeq} size={22} /><span className="grow">第 {r.bossSeq} 王</span><span className="plus">+{r.coins}</span>
        </div>
      )) : <div className="row"><span className="grow sub" style={{ textAlign: 'center' }}>這局沒有擊破魔王</span></div>}
      <div className="total"><span>本局合計</span><b>{total}</b></div>
    </>
  );
}

export default function ResultPage({ run, goal, banner, onAgain, onHome, onRetry, onBannerLogin, onBannerClose, onBannerShown }) {
  const h1 = useTitleFocus();
  const content = useRef(null), unlockRef = useRef(null);
  const [skip, setSkip] = useState(false);
  const [done, setDone] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [slow, setSlow] = useState(false);
  const animate = useRef(!reduced() && !run.seen).current;   // coming back from the login page: no replay
  const { status, body, settlement } = run;
  const invalid = status === 'invalid';
  const win = body.isVictory && !invalid;
  const title = run.result === 'secret' && !invalid ? '㊙ Secret Clear!' : body.isVictory ? 'Victory!' : 'Defeat in Battle';

  // long submit (> 2 s): add the progress sentence above the skeleton
  useEffect(() => {
    if (status !== 'submitting') { setSlow(false); return undefined; }
    const t = setTimeout(() => setSlow(true), 2000);
    return () => clearTimeout(t);
  }, [status]);

  // UXR-04: bring the unlock card into view without cutting off the first row (nearest, not start), and mark the
  // list as scrolled so its top edge fades instead of looking cropped.
  useEffect(() => {
    if (!done || !unlockRef.current) return;
    unlockRef.current.scrollIntoView({ block: 'nearest', behavior: animate ? 'smooth' : 'auto' });
  }, [done, animate]);

  let mid;
  if (status === 'submitting') {
    mid = <>{slow && <p className="msg info center" role="status">正在送出成績…</p>}<Skeletons n={3} /></>;
  } else if (status === 'error') {
    mid = <RetryBox onRetry={onRetry} />;
  } else if (status === 'offline') {
    mid = <div className="invalid" role="status">連線不穩，這局沒有保存</div>;
  } else if (invalid) {
    mid = <div className="invalid" role="status">本局成績無效<small>這局不會入帳，也不會上榜</small></div>;
  } else {
    const s = settlement;
    mid = (
      <>
        <section className="panel breakdown" aria-label="本局金幣（點一下跳過動畫）" onClick={() => setSkip(true)}>
          <h2>本局金幣</h2>
          <Breakdown settlement={s} animate={animate} skip={skip} onDone={() => setDone(true)} />
          <div className="row" style={{ border: 0, paddingTop: 0 }}><span className="grow sub">目前餘額 {s.walletCoins}</span></div>
        </section>
        {s.isSilverNewlyUnlocked && done && (
          <div className={'unlock' + (animate && !skip ? ' pop' : '')} role="status" ref={unlockRef}>
            <h2>SILVER 已解鎖</h2><p>現在可以帶 buff 進 silver 了</p>
          </div>
        )}
        {done && <GoalLine goal={goal} late={animate && !skip} />}
        <p className="vh" aria-live="polite">{done ? `本局獲得 ${s.coinsAwarded} 金幣，目前餘額 ${s.walletCoins}` : ''}</p>
      </>
    );
  }
  return (
    <div className="page menu enter">
      <div>{banner && <LoginBanner text={banner} onLogin={onBannerLogin} onClose={onBannerClose} onShown={onBannerShown} />}</div>
      <div className="result-title">
        <h1 tabIndex={-1} ref={h1} className={win ? undefined : 'lose'}>{title}</h1>
        <p>{deathLine(body)}</p>
      </div>
      <main className={'content' + (scrolled ? ' scrolled' : '')} ref={content} style={{ paddingTop: 'var(--space-4)' }}
        onScroll={e => setScrolled(e.currentTarget.scrollTop > 0)}>
        {mid}
      </main>
      <footer className="actionbar">
        <button type="button" className="btn-primary full" onClick={onAgain}><span className="lbl">再來一局</span></button>
        <button type="button" className="btn-ghost" onClick={onHome}>返回首頁</button>
      </footer>
    </div>
  );
}
