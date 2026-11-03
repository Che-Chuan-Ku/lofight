// 錢包頁 (specs/ui/錢包頁.md; prototype wallet()). Balance + ledger, newest first, 20 per page, 載入更多 while hasNext.
import { useEffect, useState } from 'react';
import { PageHeader, RetryBox, Skeletons } from '../components/ui/Common.jsx';
import { IcEmptyLedger, PixelCoin } from '../components/ui/Icons.jsx';
import { getMyWallet, listMyCoinLedgerEntries } from '../lib/api/index.js';
import { BUFFS, tierName } from '../lib/labels.js';

const pad = n => String(n).padStart(2, '0');
const when = iso => { const d = new Date(iso); return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`; };

// AD_REWARD = 廣告獎勵 (AMB-A4 final ruling, AdSense wording); BUFF_PURCHASE falls through to 購買 <buff>
const LEDGER_LABEL = { BOSS_REWARD: '對局獲得', MERGE_IN: '帳號合併', AD_REWARD: '廣告獎勵' };

// D38 (AMB-S5=a): 賽季獎勵 NORMAL 第 N 名; the amount column carries +X
const ledgerLabel = e => (e.ledgerSource === 'SEASON_REWARD'
  ? '賽季獎勵 ' + tierName(e.seasonTier) + ' 第 ' + e.seasonRank + ' 名'
  : LEDGER_LABEL[e.ledgerSource] || '購買 ' + (e.buffItem ? BUFFS[e.buffItem].name : 'buff'));

// GAP-4: the API books one BOSS_REWARD entry per boss; consecutive entries of the same run show as one 對局獲得 row.
export function groupLedger(items) {
  const rows = [];
  for (const e of items) {
    const last = rows[rows.length - 1];
    if (e.ledgerSource === 'BOSS_REWARD' && e.runId && last && last.runId === e.runId && last.source === 'BOSS_REWARD') { last.amount += e.coinDelta; continue; }
    rows.push({
      key: e.ledgerEntryId, source: e.ledgerSource, runId: e.ledgerSource === 'BOSS_REWARD' ? e.runId : null, amount: e.coinDelta, at: e.occurredAt,
      label: ledgerLabel(e)
    });
  }
  return rows;
}

export default function WalletPage({ onBack, onShop, onCoins }) {
  const [st, setSt] = useState({ status: 'loading', coins: null, items: [], page: 0, hasNext: false, more: false });
  const [n, setN] = useState(0);
  useEffect(() => {
    let live = true;
    setSt(s => ({ ...s, status: 'loading' }));
    Promise.all([getMyWallet(), listMyCoinLedgerEntries({ page: 1 })]).then(([w, l]) => {
      if (!live) return;
      onCoins(w.coins);
      setSt({ status: 'ok', coins: w.coins, items: l.items, page: l.page, hasNext: l.hasNext, more: false });
    }, () => { if (live) setSt(s => ({ ...s, status: 'error' })); });
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [n]);

  const more = async () => {
    if (st.more) return;
    setSt(s => ({ ...s, more: true }));
    try {
      const l = await listMyCoinLedgerEntries({ page: st.page + 1 });
      setSt(s => ({ ...s, items: [...s.items, ...l.items], page: l.page, hasNext: l.hasNext, more: false }));
    } catch (_) { setSt(s => ({ ...s, more: false, moreErr: true })); }
  };

  let list;
  if (st.status === 'loading') list = <Skeletons n={5} />;
  else if (st.status === 'error') list = <RetryBox onRetry={() => setN(x => x + 1)} />;
  else if (!st.items.length) {
    list = <div className="panel"><div className="empty"><IcEmptyLedger /><b>還沒有金幣紀錄</b><span>打一局就會出現</span></div></div>;
  } else {
    const rows = groupLedger(st.items);
    list = (
      <>
        <ol className="panel rows stagger" style={{ listStyle: 'none', margin: 0 }}>
          {rows.map((r, i) => (
            <li key={r.key} className="row" style={{ animationDelay: Math.min(i, 14) * 30 + 'ms' }}>
              <span className="grow">{r.label}<span className="sub">{when(r.at)}</span></span>
              <span className={'num ' + (r.amount > 0 ? 'plus' : 'minus')}>{r.amount > 0 ? '+' : '−'}{Math.abs(r.amount)}</span>
            </li>
          ))}
        </ol>
        {st.hasNext
          ? <button type="button" className="btn-ghost" style={{ alignSelf: 'center' }} disabled={st.more} onClick={more}>載入更多</button>
          : <p className="msg info center">沒有更早的紀錄了</p>}
        {st.moreErr && !st.more && <p className="msg err center" role="alert">連線不穩，請再試一次</p>}
      </>
    );
  }
  return (
    <div className="page menu enter">
      <PageHeader title="錢包" onBack={onBack} />
      <div className="balance">
        <span className="label">目前金幣</span>
        <span className="big"><PixelCoin className="lg" />
          {st.status === 'loading' && st.coins === null ? <span className="skel" style={{ width: 120, height: 44, display: 'inline-block' }} /> : st.coins ?? '—'}</span>
        <button type="button" className="btn-ghost sm" onClick={onShop}>前往商店</button>
      </div>
      <main className="content scroll" aria-busy={st.status === 'loading' ? 'true' : undefined}>
        <h2 className="sec-title">帳本明細</h2>
        {list}
      </main>
    </div>
  );
}
