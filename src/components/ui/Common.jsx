// Shared building blocks for the v19 screens (prototype class names, see src/menu.css).
import { useEffect, useRef, useState } from 'react';
import { IcBack, IcClose, IcWarn, PixelCoin } from './Icons.jsx';
import { BOSS_TILES } from '../../game/constants.js';
import { NETWORK_MSG } from '../../lib/labels.js';

const reducedMotion = () => typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Moves focus to the page title when a screen opens (换頁焦點到 h1). */
export function useTitleFocus() {
  const ref = useRef(null);
  useEffect(() => { if (ref.current) ref.current.focus({ preventScroll: true }); }, []);
  return ref;
}

export function PageHeader({ title, onBack, right, titleId }) {
  const h1 = useTitleFocus();
  return (
    <header className="hdr">
      <button type="button" className="icon-btn" onClick={onBack} aria-label="返回"><IcBack /></button>
      <h1 tabIndex={-1} ref={h1} id={titleId}>{title}</h1>
      {right || <span className="slot44" aria-hidden="true" />}
    </header>
  );
}

export function BossIcon({ seq, size = 22 }) {
  const tile = BOSS_TILES[Math.max(0, Math.min(BOSS_TILES.length - 1, seq - 1))], k = size / 16;
  return (
    <i className="spr" aria-hidden="true" style={{
      width: size, height: size, backgroundSize: `${192 * k}px ${176 * k}px`,
      backgroundPosition: `-${(tile % 12) * 16 * k}px -${Math.floor(tile / 12) * 16 * k}px`
    }} />
  );
}

/** Number that rolls from the previous value to the new one (300ms; instant with reduced motion). */
export function useRolling(value) {
  const [shown, setShown] = useState(value);
  const prev = useRef(value);
  useEffect(() => {
    if (value === null || value === undefined) { setShown(value); return; }
    const from = typeof prev.current === 'number' ? prev.current : value;
    prev.current = value;
    if (from === value || reducedMotion()) { setShown(value); return; }
    let raf = 0; const t0 = performance.now();
    const tick = now => { const k = Math.min(1, (now - t0) / 300); setShown(Math.round(from + (value - from) * k)); if (k < 1) raf = requestAnimationFrame(tick); };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value]);
  return shown;
}

export function CoinChip({ coins, onClick }) {
  const shown = useRolling(coins);
  const [pop, setPop] = useState(false);
  const first = useRef(true);
  useEffect(() => {
    if (first.current) { first.current = false; return; }
    setPop(true); const t = setTimeout(() => setPop(false), 320); return () => clearTimeout(t);
  }, [coins]);
  const known = typeof coins === 'number';
  const inner = (<><PixelCoin className={pop ? 'pop' : ''} /><span>{known ? shown : '—'}</span></>);
  if (onClick) return <button type="button" className="chip" onClick={onClick} aria-label={known ? `金幣 ${coins}，查看錢包` : '查看錢包'}>{inner}</button>;
  return <span className="chip" role="img" aria-label={known ? `金幣 ${coins}` : '金幣'}>{inner}</span>;
}

export function RetryBox({ onRetry }) {
  return (
    <div className="empty" role="alert">
      <IcWarn />
      <b>{NETWORK_MSG}</b>
      <button type="button" className="btn-ghost sm" onClick={onRetry}>重試</button>
    </div>
  );
}

export function Skeletons({ n, kind }) {
  return (
    <div aria-busy="true" style={{ display: 'flex', flexDirection: 'column', gap: kind === 'card' ? 14 : kind === 'opt' ? 12 : 8 }}>
      {Array.from({ length: n }, (_, i) => <div key={i} className={'skel' + (kind ? ' ' + kind : '')} />)}
    </div>
  );
}

export function PrimaryButton({ busy, children, className = '', onClick, ...rest }) {
  return (
    <button type="button" className={'btn-primary ' + className} aria-busy={busy ? 'true' : undefined}
      onClick={e => { if (busy) return; if (onClick) onClick(e); }} {...rest}>
      <span className="lbl">{children}</span>
      {busy && <span className="dots" aria-hidden="true"><i /><i /><i /></span>}
    </button>
  );
}

/** Non-blocking login prompt (region, polite announcement, no scrim, never steals focus). */
/**
 * Non-blocking login banner. onShown runs once when it is really on screen (banner flags are stored only then);
 * loginLabel / onSecondary add the shop variant (登入 + 再等等, D-03 human ruling A).
 */
export function LoginBanner({ text, onLogin, onClose, onShown, loginLabel = '登入保存', secondaryLabel, onSecondary }) {
  const shown = useRef(onShown);
  useEffect(() => { if (shown.current) shown.current(); }, []);
  return (
    <div className={'banner in' + (onSecondary ? ' two' : '')} role="region" aria-label="保存進度提示">
      <p aria-live="polite">{text}</p>
      <button type="button" className="btn-primary sm" onClick={onLogin}><span className="lbl">{loginLabel}</span></button>
      {onSecondary && <button type="button" className="btn-ghost sm" onClick={onSecondary}>{secondaryLabel}</button>}
      <button type="button" className="icon-btn sm" onClick={onClose} aria-label="關閉提示"><IcClose /></button>
    </div>
  );
}

/** Destructive-action dialog: focus starts on 取消, Tab is trapped, Esc cancels, scrim clicks do nothing. */
export function ConfirmDialog({ title, desc, action, onCancel, onConfirm }) {
  const box = useRef(null), cancel = useRef(null);
  useEffect(() => {
    if (cancel.current) cancel.current.focus();
    const onKey = e => {
      if (e.key === 'Escape') { e.preventDefault(); onCancel(); return; }
      if (e.key !== 'Tab' || !box.current) return;
      const f = [...box.current.querySelectorAll('button')];
      const i = f.indexOf(document.activeElement);
      if (e.shiftKey && i <= 0) { e.preventDefault(); f[f.length - 1].focus(); }
      else if (!e.shiftKey && i === f.length - 1) { e.preventDefault(); f[0].focus(); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onCancel]);
  return (
    <div className="scrim" role="dialog" aria-modal="true" aria-labelledby="dlg-t" aria-describedby="dlg-d" ref={box}>
      <h2 id="dlg-t">{title}</h2>
      <p id="dlg-d">{desc}</p>
      <div className="btns">
        <button type="button" className="btn-secondary" ref={cancel} onClick={onCancel}>取消</button>
        <button type="button" className="btn-danger" onClick={onConfirm}>{action}</button>
      </div>
    </div>
  );
}

/** Small loader for screens: { status: 'loading' | 'ok' | 'error', data, reload }. */
export function useLoad(fn, deps) {
  const [st, setSt] = useState({ status: 'loading', data: null });
  const [n, setN] = useState(0);
  useEffect(() => {
    let live = true;
    setSt(s => ({ status: 'loading', data: s.data }));
    fn().then(data => { if (live) setSt({ status: 'ok', data }); }, () => { if (live) setSt(s => ({ status: 'error', data: s.data })); });
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, n]);
  return { ...st, reload: () => setN(x => x + 1), set: data => setSt({ status: 'ok', data }) };
}
