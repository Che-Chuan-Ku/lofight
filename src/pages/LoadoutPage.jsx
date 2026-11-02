// 裝填畫面 (specs/ui/裝填畫面.md; prototype ready()). EXTRA (API tier SILVER) only: pick none or one buff, then createRun.
import { useEffect, useRef, useState } from 'react';
import { PageHeader, PrimaryButton, RetryBox, Skeletons, useLoad } from '../components/ui/Common.jsx';
import { listMyBuffInventories } from '../lib/api/index.js';
import { TierBadge, BuffIcon } from '../components/ui/Icons.jsx';
import { BUFFS, BUFF_ORDER, NETWORK_MSG } from '../lib/labels.js';

export default function LoadoutPage({ preselect, onBack, onStart }) {
  const inv = useLoad(() => listMyBuffInventories(), []);
  const [sel, setSel] = useState('none');
  const [warn, setWarn] = useState(null);
  const [busy, setBusy] = useState(false);
  const picked = useRef(false);
  const qty = b => ((inv.data && inv.data.items.find(x => x.buffItem === b)) || { quantity: 0 }).quantity;

  // 再來一局 preselects the last buff; if it ran out, fall back to 不裝填 (BDY-03)
  useEffect(() => {
    if (inv.status !== 'ok' || picked.current) return;
    picked.current = true;
    setSel(preselect && qty(preselect) > 0 ? preselect : 'none');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inv.status]);

  const options = [{ k: 'none', name: '不裝填', desc: '直接開打', n: null }, ...BUFF_ORDER.map(b => ({ k: b, name: BUFFS[b].name, desc: BUFFS[b].desc, n: qty(b) }))];
  const enabled = options.filter(o => o.n !== 0);

  const onKey = e => {
    if (!['ArrowDown', 'ArrowUp', 'ArrowRight', 'ArrowLeft'].includes(e.key)) return;
    e.preventDefault();
    const i = enabled.findIndex(o => o.k === sel), d = e.key === 'ArrowDown' || e.key === 'ArrowRight' ? 1 : -1;
    const next = enabled[(i + d + enabled.length) % enabled.length];
    setSel(next.k);
    const el = e.currentTarget.querySelector(`[data-k="${next.k}"]`);
    if (el) el.focus();
  };

  const start = async () => {
    if (busy || inv.status !== 'ok') return;
    setBusy(true); setWarn(null);
    const res = await onStart(sel === 'none' ? null : sel);
    setBusy(false);
    if (res === 'rejected') {          // gam002: stock used up (maybe on another device) -> reload, back to 不裝填
      setWarn('庫存不足，請重新選擇。'); setSel('none'); inv.reload();
    } else if (res === 'network') setWarn(NETWORK_MSG);
  };

  let list;
  if (inv.status === 'loading' && !inv.data) list = <Skeletons n={5} kind="opt" />;
  else if (inv.status === 'error') list = <RetryBox onRetry={inv.reload} />;
  else {
    list = (
      <div role="radiogroup" aria-labelledby="ready-t" className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 14 }} onKeyDown={onKey}>
        {options.map((o, i) => {
          const dis = o.n === 0, on = sel === o.k;
          return (
            <button key={o.k} type="button" className="opt" role="radio" data-k={o.k} aria-checked={on} tabIndex={on ? 0 : -1}
              aria-disabled={dis ? 'true' : undefined} aria-label={o.name + (o.n === null ? '' : '，持有 ' + o.n)}
              style={{ animationDelay: i * 30 + 'ms' }} onClick={() => { if (!dis) setSel(o.k); }}>
              <span className="grow"><span className="nmline"><BuffIcon buff={o.k} />{o.name}</span><span className="sub">{o.desc}</span></span>
              {o.n !== null && <span className="cnt">×{o.n}</span>}
            </button>
          );
        })}
      </div>
    );
  }
  return (
    <div className="page menu enter">
      <PageHeader title={<><TierBadge tier="silver" size={30} />EXTRA 裝填</>} titleId="ready-t" onBack={onBack} />
      <main className="content" style={{ paddingTop: 'var(--space-4)' }}>
        {warn && <p className="msg warn" role="alert">{warn}</p>}
        {list}
      </main>
      <footer className="actionbar">
        <PrimaryButton className="full" busy={busy} disabled={inv.status !== 'ok'} onClick={start}>開始</PrimaryButton>
      </footer>
    </div>
  );
}
