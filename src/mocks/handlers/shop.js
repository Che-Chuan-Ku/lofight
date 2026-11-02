// Shop tag handlers (browse, purchase).
import { route, ok, fail, authenticate } from './util.js';
import { persist, addLedger, adRewardStatus, SHOP_PRICES, BUFF_ORDER, SILVER_UNLOCK_BOSS_SEQ } from '../db.js';

export const shopHandlers = [
  route('listShopItems', ({ request }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const p = auth.player;
    return ok({ items: BUFF_ORDER.map(b => ({
      shopItemId: b, buffItem: b, price: SHOP_PRICES[b], isPurchasable: p.isSilverUnlocked,
      silverUnlockBossSeq: p.isSilverUnlocked ? null : SILVER_UNLOCK_BOSS_SEQ,
      bronzeBestBossesKilled: p.isSilverUnlocked ? null : p.bronzeBest
    })), adReward: adRewardStatus(p.playerId) });
  }),

  route('purchaseShopItem', ({ request, params }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const item = params.shopItemId;
    if (!SHOP_PRICES[item]) return fail('404001');
    const p = auth.player;
    if (!p.isSilverUnlocked) return fail('shp002');
    if (p.coins < SHOP_PRICES[item]) return fail('shp001');
    addLedger(p.playerId, { ledgerSource: 'BUFF_PURCHASE', coinDelta: -SHOP_PRICES[item], buffItem: item });
    p.inv[item] += 1; persist();
    return ok({ buffItem: item, pricePaid: SHOP_PRICES[item], walletCoins: p.coins, quantity: p.inv[item] });
  })
];
