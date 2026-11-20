// 贊助 card (D43, spec 8.2): takes the rewarded-ad card's place on the shop. Looks like .card.adcard with the sponsor icon.
import { useRef, useState } from 'react';
import TopupDialog from './TopupDialog.jsx';
import DonateDialog from './DonateDialog.jsx';
import { TOPUP } from '../../lib/labels.js';
import icon1x from '../../assets/icon-sponsor-1x.png';
import icon2x from '../../assets/icon-sponsor-2x.png';

/**
 * topup: listShopItems.topup (mode AIO opens the package panel, DONATE opens the reminder before the ECPay streamer link).
 * nickname: the player's current nickname (shown in the DONATE reminder). guest: the caller is a guest (onLoginRequired shows the login banner, nothing is sent).
 */
export default function SponsorCard({ topup, guest, nickname, onLoginRequired }) {
  const donate = topup.mode === 'DONATE';
  const [open, setOpen] = useState(false);
  const btn = useRef(null);
  const press = () => { if (guest) onLoginRequired(); else setOpen(true); };
  const close = () => { setOpen(false); setTimeout(() => { if (btn.current) btn.current.focus(); }, 0); };
  return (
    <>
      <div className="card adcard sponsorcard" data-sponsor-card="">
        <img className="sponsor-icon" src={icon1x} srcSet={icon2x + ' 2x'} width="32" height="32" alt="" />
        <div className="info"><span className="name" id="sp-t">{TOPUP.name}</span><span className="stat" id="sp-stat">{donate ? TOPUP.donateCardDesc : TOPUP.cardDesc}</span></div>
        <div className="buy">
          <button type="button" ref={btn} className="btn-primary sm" aria-describedby="sp-t sp-stat" onClick={press}><span className="lbl">{TOPUP.name}</span></button>
        </div>
      </div>
      {open && (donate
        ? <DonateDialog donateUrl={topup.donateUrl} nickname={nickname} onClose={close} onOpened={close} host={btn.current && btn.current.closest('.page')} />
        : <TopupDialog packages={topup.packages} onClose={close} host={btn.current && btn.current.closest('.page')} />)}
    </>
  );
}
