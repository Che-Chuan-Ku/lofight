// 商店頁 (specs/ui/商店頁.md; prototype shop()). Items + prices from listShopItems, owned counts from
// listMyBuffInventories, chip from getMyWallet. SPEC-01: api.yml returns no "約 N 次通關" value, so it is not shown.
// The rewarded-ad card (D33-D35) sits on top and takes listShopItems.adReward; no other call is made for it.
import { useEffect, useState } from 'react';
import { PageHeader, CoinChip, RetryBox, Skeletons, PrimaryButton, LoginBanner } from '../components/ui/Common.jsx';
import { PixelCoin, BuffIcon } from '../components/ui/Icons.jsx';
import AdRewardCard from '../components/ui/AdRewardCard.jsx';
import { listShopItems, listMyBuffInventories, getMyWallet, purchaseShopItem } from '../lib/api/index.js';
import { BUFFS, lockLine, NETWORK_MSG, EXTRA_LOCKED_MSG } from '../lib/labels.js';

export default function ShopPage({ onBack, coins, onCoins, banner, onBuyIntercept, onBannerDone, onBannerLogin, onBannerClose, onBannerShown }) {
  const [st, setSt] = useState({ status: 'loading', items: [], inv: {}, adReward: null });
  const [n, setN] = useState(0);
  const [msg, setMsg] = useState({});        // buffItem -> ok | short | locked | net
  const [busy, setBusy] = useState(null);
  const [pop, setPop] = useState(null);
  const [pending, setPending] = useState(null);   // item whose 購買 the login banner intercepted (D-03)

  useEffect(() => {
    let live = true;
    setSt(s => ({ ...s, status: 'loading' }));
    Promise.all([listShopItems(), listMyBuffInventories(), getMyWallet()]).then(([shop, inv, w]) => {
      if (!live) return;
      onCoins(w.coins);
      setSt({ status: 'ok', items: shop.items, adReward: shop.adReward, inv: Object.fromEntries(inv.items.map(i => [i.buffItem, i.quantity])) });
    }, () => { if (live) setSt(s => ({ ...s, status: 'error' })); });
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [n]);

  useEffect(() => {
    const k = Object.keys(msg).find(b => msg[b] === 'ok');
    if (!k) return undefined;
    const t = setTimeout(() => setMsg(m => ({ ...m, [k]: null })), 2000);
    return () => clearTimeout(t);
  }, [msg]);

  // direct: the player chose 直接購買 on the banner, so no interception
  const buy = async (item, direct = false) => {
    if (busy) return;
    setMsg({});
    if (!item.isPurchasable) { setMsg({ [item.buffItem]: 'locked' }); return; }
    // D-03 (human ruling A): a guest's first 購買 shows the banner (先登入 / 直接購買) and sends nothing yet
    // While the banner holds a pending item, any other 購買 tap only swaps the pending item (nothing is sent)
    if (!direct && banner && pending) { setPending(item); return; }
    if (!direct && onBuyIntercept()) { setPending(item); return; }
    setPending(null);
    setBusy(item.buffItem);
    try {
      const r = await purchaseShopItem({ shopItemId: item.shopItemId });
      onCoins(r.walletCoins);
      setSt(s => ({ ...s, inv: { ...s.inv, [r.buffItem]: r.quantity } }));
      setPop(r.buffItem); setTimeout(() => setPop(null), 320);
      setMsg({ [item.buffItem]: 'ok' });
    } catch (e) {
      if (e.code === 'shp001') { setMsg({ [item.buffItem]: 'short' }); getMyWallet().then(w => onCoins(w.coins), () => {}); }
      else if (e.code === 'shp002') { setMsg({ [item.buffItem]: 'locked' }); setN(x => x + 1); }
      else setMsg({ [item.buffItem]: 'net' });
    } finally { setBusy(null); }
  };

  const buyDirect = () => { const item = pending; onBannerDone(); if (item) buy(item, true); };
  const locked = st.items.find(i => !i.isPurchasable);
  let cards;
  if (st.status === 'loading' && !st.items.length) cards = <Skeletons n={4} kind="card" />;
  else if (st.status === 'error') cards = <RetryBox onRetry={() => setN(x => x + 1)} />;
  else {
    cards = (
      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {st.items.map((item, i) => {
          const b = BUFFS[item.buffItem], m = msg[item.buffItem];
          return (
            <div key={item.shopItemId} className={'card' + (item.isPurchasable ? '' : ' locked')} style={{ animationDelay: i * 30 + 'ms' }}>
              <div className="info">
                <span className="name"><BuffIcon buff={item.buffItem} />{b.name}</span>
                <span>{b.desc}</span>
                <span className="own">持有 <b className={pop === item.buffItem ? 'pop' : undefined}>{st.inv[item.buffItem] ?? 0}</b></span>
              </div>
              <div className="buy">
                <span className="price"><PixelCoin />{item.price}</span>
                <PrimaryButton className="sm" busy={busy === item.buffItem} onClick={() => buy(item)}
                  aria-label={`購買 ${b.name}，${item.price} 金幣`}
                  aria-disabled={item.isPurchasable ? undefined : 'true'} aria-describedby={item.isPurchasable ? undefined : 'lockline'}>購買</PrimaryButton>
              </div>
              <div className="msgline" aria-live={m === 'short' ? 'assertive' : 'polite'}>
                {m === 'ok' && <span className="msg ok">已購買</span>}
                {m === 'short' && <span className="msg err">餘額不足</span>}
                {m === 'locked' && <span className="msg err">{EXTRA_LOCKED_MSG}</span>}
                {m === 'net' && <span className="msg err">{NETWORK_MSG}</span>}
              </div>
            </div>
          );
        })}
      </div>
    );
  }
  return (
    <div className="page menu enter">
      <PageHeader title="商店" onBack={onBack} right={<CoinChip coins={coins} />} />
      <div>{banner && (
        <LoginBanner text={banner} onLogin={onBannerLogin} onClose={() => { setPending(null); onBannerClose(); }} onShown={onBannerShown}
          loginLabel={pending ? '登入' : undefined} secondaryLabel="再等等" onSecondary={pending ? buyDirect : undefined} />
      )}</div>
      <main className="content scroll" style={{ paddingTop: 'var(--space-3)' }} aria-busy={st.status === 'loading' ? 'true' : undefined}>
        {st.status === 'ok' && st.adReward && <AdRewardCard status={st.adReward} onCoins={onCoins} />}
        {st.status === 'ok' && locked && (
          <p id="lockline" className="msg warn"><span className="lock" aria-hidden="true" style={{ margin: '6px 6px 0 0' }} />{lockLine(locked.silverUnlockBossSeq, locked.bronzeBestBossesKilled)}</p>
        )}
        {cards}
      </main>
    </div>
  );
}
