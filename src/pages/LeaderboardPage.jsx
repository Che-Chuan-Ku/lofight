// 排行榜頁 (specs/ui/排行榜頁.md; prototype board()). Tabs NORMAL / EXTRA (API tiers BRONZE / SILVER), top 15, my rank with 2 above / 2 below.
import { useRef, useState } from 'react';
import { PageHeader, RetryBox, Skeletons, useLoad } from '../components/ui/Common.jsx';
import { IcEmptyBoard, TierBadge } from '../components/ui/Icons.jsx';
import { getLeaderboard } from '../lib/api/index.js';
import { BUFFS, tierName } from '../lib/labels.js';

function Row({ e, me, i }) {
  const isMe = me && e.rank === me.rank;
  return (
    <li className={'row lbrow' + (isMe ? ' me' : '')} aria-current={isMe ? 'true' : undefined} style={{ animationDelay: (i || 0) * 30 + 'ms' }}>
      <span className={'rk r' + e.rank}>{e.rank}</span>
      <span className="nm"><span>{e.nickname}</span>{e.equippedBuff && <span className="tag buff">{BUFFS[e.equippedBuff].name}</span>}</span>
      <span className={'tag ' + (e.isVictory ? 'win' : 'lose')}>{e.isVictory ? '勝' : '敗'}</span>
      <span className="num" style={{ width: '3.6em' }}>{e.gameSeconds} 秒</span>
    </li>
  );
}

export default function LeaderboardPage({ onBack, onPlay }) {
  const [tab, setTab] = useState('BRONZE');
  const board = useLoad(() => getLeaderboard({ tier: tab }), [tab]);
  const tabs = useRef(null);
  const d = board.status === 'ok' ? board.data : null;

  const onKey = e => {
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
    e.preventDefault();
    const next = tab === 'BRONZE' ? 'SILVER' : 'BRONZE';
    setTab(next);
    const el = tabs.current && tabs.current.querySelector('#tab-' + next.toLowerCase());
    if (el) el.focus();
  };

  let top, mine;
  if (board.status === 'loading') { top = <Skeletons n={6} />; mine = <div className="skel" style={{ height: 32, margin: '6px 0' }} />; }
  else if (board.status === 'error') { top = <RetryBox onRetry={board.reload} />; mine = <div className="skel" style={{ height: 32, margin: '6px 0' }} />; }
  else {
    // UXR-03: the empty board gets a way forward (back to the tier cards on the home screen)
    top = d.entries.length ? (
      <ol className="panel rows stagger" style={{ listStyle: 'none', margin: 0 }} aria-label="前 15 名">
        {d.entries.map((e, i) => <Row key={e.rank + ':' + e.playerId} e={e} me={d.myRank} i={i} />)}
      </ol>
    ) : (
      <div className="panel"><div className="empty"><IcEmptyBoard /><b>這個榜還沒有人上榜</b><span>打一局，來當第一個</span>
        <button type="button" className="btn-ghost sm" onClick={onPlay}>去打一局</button></div></div>
    );
    if (!d.myRank) mine = <p className="msg info center" style={{ padding: '8px 0' }}>尚無成績</p>;
    else {
      const around = [...d.neighbors, d.myRank].sort((a, b) => a.rank - b.rank);
      mine = <ol className="rows" style={{ listStyle: 'none', margin: 0, padding: 0 }}>{around.map(e => <Row key={'m' + e.rank + ':' + e.playerId} e={e} me={d.myRank} />)}</ol>;
    }
  }
  const t = tab.toLowerCase();
  return (
    <div className="page menu enter">
      <PageHeader title="排行榜" onBack={onBack} />
      <div className="subhdr">
        <span className="label">{d ? 'v' + d.boardVersion + ' 賽季' : ' '}</span>
        <div className="tabs" role="tablist" aria-label="階層" data-tab={t} ref={tabs} onKeyDown={onKey}>
          <span className="thumb" aria-hidden="true" />
          {['BRONZE', 'SILVER'].map(k => (
            <button key={k} type="button" role="tab" id={'tab-' + k.toLowerCase()} aria-controls="lb-panel" aria-selected={tab === k} tabIndex={tab === k ? 0 : -1}
              onClick={() => setTab(k)}><TierBadge tier={k} size={18} />{tierName(k)}</button>
          ))}
        </div>
      </div>
      <main className="content scroll" id="lb-panel" role="tabpanel" aria-labelledby={'tab-' + t} aria-busy={board.status === 'loading' ? 'true' : undefined}>
        {top}
      </main>
      <section className="myrank" aria-labelledby="my-t"><h2 id="my-t">我的名次</h2>{mine}</section>
    </div>
  );
}
