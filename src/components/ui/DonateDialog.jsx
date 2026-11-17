// 贊助回饋 reminder (DONATE mode): shown before the ECPay streamer link opens. Two reminders only: put the nickname in
// 「贊助者名稱」, and the board effect. ConfirmDialog pattern: role=dialog, Tab trap, Esc cancels, scrim clicks do nothing.
// 前往贊助 opens the link in a new tab (noopener, noreferrer) only when it is an allowed ECPay https URL.
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { PrimaryButton } from './Common.jsx';
import { IcClose } from './Icons.jsx';
import { TOPUP } from '../../lib/labels.js';
import { donateUrlAllowed } from '../../lib/topup.js';
import { isDefaultNickname } from '../../lib/nickname.js';

/** donateUrl: listShopItems.topup.donateUrl; nickname: the player's current nickname; host: the .page element the scrim covers. */
export default function DonateDialog({ donateUrl, nickname, onClose, onOpened, host }) {
  const [err, setErr] = useState(null);
  const box = useRef(null), go = useRef(null), cancel = useRef(null);
  // a system default nickname (勇者 + 3 digits) identifies nobody: the player must pick their own first
  const needName = isDefaultNickname(nickname);

  useEffect(() => { const el = needName ? cancel.current : go.current; if (el) el.focus(); }, [needName]);

  useEffect(() => {
    const onKey = e => {
      if (e.key === 'Escape') { e.preventDefault(); onClose(); return; }
      if (e.key !== 'Tab' || !box.current) return;
      const f = [...box.current.querySelectorAll('button:not([disabled])')];
      const i = f.indexOf(document.activeElement);
      if (e.shiftKey && i <= 0) { e.preventDefault(); f[f.length - 1].focus(); }
      else if (!e.shiftKey && (i === f.length - 1 || i < 0)) { e.preventDefault(); f[0].focus(); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  const proceed = () => {
    if (needName) return;
    if (!donateUrlAllowed(donateUrl)) { setErr(TOPUP.donateBlocked); return; }
    window.open(donateUrl, '_blank', 'noopener,noreferrer');
    onOpened();
  };

  return createPortal(
    <div className="scrim topup-scrim" role="dialog" aria-modal="true" aria-labelledby="dn-t" aria-describedby="dn-d" ref={box}>
      <div className="topup-panel">
        <div className="topup-head">
          <span className="slot44" aria-hidden="true" />
          <h2 id="dn-t">{TOPUP.name}</h2>
          <button type="button" className="icon-btn sm" aria-label="關閉" onClick={onClose}><IcClose /></button>
        </div>
        <div id="dn-d" className="topup-points">
          <p data-donate-name="">{nickname ? TOPUP.donateNameLine(nickname) : TOPUP.donateNameLineNoNick}</p>
          <p data-donate-effect="">{TOPUP.donateEffectLine}</p>
          {needName && <p data-donate-change-name="">{TOPUP.donateChangeName}</p>}
        </div>
        <PrimaryButton className="full" ref={go} disabled={needName} aria-disabled={needName ? 'true' : undefined} onClick={proceed}>{TOPUP.donateGo}</PrimaryButton>
        <div className="msgline" aria-live="assertive">{err && <span className="msg err">{err}</span>}</div>
        <button type="button" ref={cancel} className="btn-secondary" onClick={onClose}>取消</button>
      </div>
    </div>,
    host || document.body
  );
}
