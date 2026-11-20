// Shop tag handlers (browse, purchase).
import { route, ok, fail, authenticate } from './util.js';
import { persist, addLedger, adRewardStatus, TOPUP_PACKAGES, DONATE_URL, getDb, SHOP_PRICES, BUFF_ORDER, SILVER_UNLOCK_BOSS_SEQ } from '../db.js';

export const shopHandlers = [
  route('listShopItems', ({ request }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const p = auth.player;
    return ok({ items: BUFF_ORDER.map(b => ({
      shopItemId: b, buffItem: b, price: SHOP_PRICES[b], isPurchasable: p.isSilverUnlocked,
      silverUnlockBossSeq: p.isSilverUnlocked ? null : SILVER_UNLOCK_BOSS_SEQ,
      bronzeBestBossesKilled: p.isSilverUnlocked ? null : p.bronzeBest
    })), adReward: adRewardStatus(p.playerId),
      topup: getDb().topupEnabled === false
        ? { isEnabled: false, requiresLogin: p.playerKind === 'GUEST', minAmountTwd: 10, coinsPerTwd: 0, packages: [], mode: 'AIO', donateUrl: null }
        : getDb().topupMode === 'DONATE'
          ? { isEnabled: true, requiresLogin: p.playerKind === 'GUEST', minAmountTwd: 10, coinsPerTwd: 0, packages: [], mode: 'DONATE', donateUrl: getDb().donateUrl || DONATE_URL }
          : { isEnabled: true, requiresLogin: p.playerKind === 'GUEST', minAmountTwd: 10, coinsPerTwd: 0, packages: TOPUP_PACKAGES, mode: 'AIO', donateUrl: null } });
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
    p.inv[item] = (p.inv[item] ?? 0) + 1; persist();
    return ok({ buffItem: item, pricePaid: SHOP_PRICES[item], walletCoins: p.coins, quantity: p.inv[item] });
  })
];
