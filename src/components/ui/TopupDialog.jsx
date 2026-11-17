// 贊助回饋 amount panel (D44, spec 8.3): five fixed packages (the server owns amount and coins), then a form post to ECPay.
// ConfirmDialog pattern: role=dialog, Tab trap, Esc cancels, scrim clicks do nothing. Focus starts on the first option.
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { PrimaryButton } from './Common.jsx';
import { IcClose, PixelCoin } from './Icons.jsx';
import { createTopupOrder } from '../../lib/api/index.js';
import { TOPUP, topupError, NETWORK_MSG } from '../../lib/labels.js';
import { checkoutUrlAllowed, setPendingTopup, submitCheckout } from '../../lib/topup.js';

const fmt = n => Number(n).toLocaleString('en-US');

/** packages: listShopItems.topup.packages; host: the .page element the scrim covers (inside .v19 so the menu styles apply). */
export default function TopupDialog({ packages, onClose, host }) {
  const [sel, setSel] = useState(null);            // package id; nothing is preselected
  const [state, setState] = useState('select');    // select | creating | redirecting | error
  const [err, setErr] = useState(null);
  const box = useRef(null), group = useRef(null);
  const busy = state === 'creating' || state === 'redirecting';

  useEffect(() => {
    const first = group.current && group.current.querySelector('[role=radio]');
    if (first) first.focus();
  }, []);

  useEffect(() => {
    const onKey = e => {
      if (e.key === 'Escape') { e.preventDefault(); if (!busy) onClose(); return; }
      if (e.key !== 'Tab' || !box.current) return;
      const f = [...box.current.querySelectorAll('button:not([disabled]), [role=radio]:not([disabled])')];
      const i = f.indexOf(document.activeElement);
      if (e.shiftKey && i <= 0) { e.preventDefault(); f[f.length - 1].focus(); }
      else if (!e.shiftKey && (i === f.length - 1 || i < 0)) { e.preventDefault(); f[0].focus(); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [busy, onClose]);

  const onKey = e => {
    if (busy || !['ArrowDown', 'ArrowUp', 'ArrowRight', 'ArrowLeft'].includes(e.key)) return;
    e.preventDefault();
    const i = packages.findIndex(p => p.packageId === sel), d = e.key === 'ArrowDown' || e.key === 'ArrowRight' ? 1 : -1;
    const next = packages[i < 0 ? (d > 0 ? 0 : packages.length - 1) : (i + d + packages.length) % packages.length];
    setSel(next.packageId);
    const el = group.current.querySelector(`[data-k="${next.packageId}"]`);
    if (el) el.focus();
  };

  const pay = async () => {
    if (!sel || busy) return;          // nothing chosen: aria-disabled, nothing is sent
    setState('creating'); setErr(null);
    try {
      const r = await createTopupOrder({ packageId: sel });
      if (!checkoutUrlAllowed(r.checkout.actionUrl)) { setState('error'); setErr(NETWORK_MSG); return; }
      setPendingTopup(r.topupOrderId);
      setState('redirecting');
      submitCheckout(r.checkout);
    } catch (e) { setState('error'); setErr(e && e.kind === 'http' ? topupError(e.code) : NETWORK_MSG); }
  };

  return createPortal(
    <div className="scrim topup-scrim" role="dialog" aria-modal="true" aria-labelledby="tp-t" aria-describedby="tp-d" ref={box}>
      <div className="topup-panel">
        <div className="topup-head">
          <span className="slot44" aria-hidden="true" />
          <h2 id="tp-t">{TOPUP.name}</h2>
          <button type="button" className="icon-btn sm" aria-label="關閉" disabled={busy} onClick={onClose}><IcClose /></button>
        </div>
        <p id="tp-d">{TOPUP.panelDesc}</p>
        <div role="radiogroup" aria-labelledby="tp-t" className="topup-opts" ref={group} onKeyDown={onKey}>
          {packages.map((p, i) => {
            const on = sel === p.packageId;
            return (
              <button key={p.packageId} type="button" className="topup-opt" role="radio" data-k={p.packageId} aria-checked={on}
                tabIndex={on || (sel === null && i === 0) ? 0 : -1} disabled={busy} onClick={() => setSel(p.packageId)}>
                <span className="dot" aria-hidden="true" />
                <span className="amt">NT${p.amountTwd}</span>
                <span className="cn"><PixelCoin /><b>{fmt(p.coins)}</b> 金幣</span>
              </button>
            );
          })}
        </div>
        <PrimaryButton className="full" busy={state === 'creating'} disabled={state === 'redirecting'}
          aria-disabled={!sel && !busy ? 'true' : undefined} onClick={pay}>{state === 'redirecting' ? TOPUP.redirecting : TOPUP.pay}</PrimaryButton>
        <div className="msgline" aria-live="assertive">{state === 'error' && err && <span className="msg err">{err}</span>}</div>
        <p className="note">{TOPUP.payNote}</p>
        <button type="button" className="btn-secondary" disabled={busy} onClick={onClose}>取消</button>
      </div>
    </div>,
    host || document.body
  );
}
