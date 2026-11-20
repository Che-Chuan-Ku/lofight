// 贊助 result block (D49, spec 8.4): polls getTopupOrder after the player comes back from ECPay.
// Every 2 s, at most 15 times (30 s); PAID / FAILED / EXPIRED / 404 stop it, AWAITING_PAYMENT stops it and keeps the pending id.
import { useEffect, useRef, useState } from 'react';
import { getTopupOrder } from '../../lib/api/index.js';
import { ensureSession } from '../../lib/session.js';
import { TOPUP, NETWORK_MSG } from '../../lib/labels.js';
import { clearPendingTopup } from '../../lib/topup.js';

const POLL_MS = 2000, POLL_MAX = 15, BACK_FAIL_MS = 5000;
const pad = n => String(n).padStart(2, '0');
const mdhm = iso => { const d = new Date(iso); return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`; };

/**
 * orderId: the order to confirm. onCoins(walletCoins) refreshes the coin chip on PAID (unchanged now, kept in sync anyway).
 * fromBack: the id came from the pending marker only (no ?topup= return, the player went back from ECPay): an order still
 * CREATED after BACK_FAIL_MS is shown as failed.
 */
export default function TopupResult({ orderId, onCoins, fromBack = false }) {
  const [res, setRes] = useState({ kind: 'checking' });   // checking | paid | awaiting | slow | failed | expired | notfound | net
  const [run, setRun] = useState(0);                       // bumped by 重新整理 / 重試
  const coinsRef = useRef(onCoins);
  coinsRef.current = onCoins;

  useEffect(() => {
    let live = true, timer = 0, tries = 0;
    const started = Date.now();
    setRes({ kind: 'checking' });
    const poll = async () => {
      tries += 1;
      try {
        await ensureSession();
        const r = await getTopupOrder({ topupOrderId: orderId });
        if (!live) return;
        if (r.status === 'PAID') { coinsRef.current(r.walletCoins); clearPendingTopup(); setRes({ kind: 'paid', packageId: r.packageId }); return; }
        if (r.status === 'AWAITING_PAYMENT') { setRes({ kind: 'awaiting', at: r.paymentExpireAt ? mdhm(r.paymentExpireAt) : '' }); return; }
        if (r.status === 'FAILED' || r.status === 'EXPIRED') { clearPendingTopup(); setRes({ kind: r.status === 'FAILED' ? 'failed' : 'expired' }); return; }
        if (fromBack && r.status === 'CREATED' && Date.now() - started >= BACK_FAIL_MS) { clearPendingTopup(); setRes({ kind: 'failed' }); return; }
        if (tries >= POLL_MAX) { setRes({ kind: 'slow' }); return; }
        timer = setTimeout(poll, POLL_MS);
      } catch (e) {
        if (!live) return;
        if (e && e.kind === 'http' && e.code === '404001') { clearPendingTopup(); setRes({ kind: 'notfound' }); return; }
        setRes({ kind: 'net' });
      }
    };
    poll();
    return () => { live = false; clearTimeout(timer); };
  }, [orderId, run, fromBack]);

  const again = <button type="button" className="btn-ghost sm" onClick={() => setRun(x => x + 1)}>{res.kind === 'net' ? '重試' : '重新整理'}</button>;
  return (
    <div className="topup-result" data-topup-result={res.kind} aria-live={res.kind === 'net' ? 'assertive' : 'polite'}>
      {res.kind === 'checking' && <span className="msg info">{TOPUP.checking}</span>}
      {res.kind === 'paid' && <span className="msg ok">{TOPUP.success(res.packageId)}</span>}
      {res.kind === 'awaiting' && <span className="msg info">{TOPUP.awaiting(res.at)}</span>}
      {res.kind === 'slow' && <><span className="msg info">{TOPUP.slow}</span>{again}</>}
      {res.kind === 'failed' && <span className="msg err">{TOPUP.failed}</span>}
      {res.kind === 'expired' && <span className="msg err">{TOPUP.expired}</span>}
      {res.kind === 'notfound' && <span className="msg err">{TOPUP.notFound}</span>}
      {res.kind === 'net' && <><span className="msg err">{NETWORK_MSG}</span>{again}</>}
    </div>
  );
}
