// Players tag handlers (progress, nickname, wallet, ledger, buff inventory).
import { route, ok, fail, authenticate, blank } from './util.js';
import { getDb, persist, addLedger, adRewardStatus, latestSeasonReward, now, BANNED_WORDS, LEDGER_PAGE_SIZE, BUFF_ORDER, SILVER_UNLOCK_BOSS_SEQ, AD_REWARD, TOPUP_PACKAGES, TOPUP_MAX_OPEN_ORDERS, nextId } from '../db.js';

export const progressOf = p => ({
  playerId: p.playerId, nickname: p.nickname, playerKind: p.playerKind, isSilverUnlocked: p.isSilverUnlocked,
  silverUnlockBossSeq: p.isSilverUnlocked ? null : SILVER_UNLOCK_BOSS_SEQ,
  bronzeBestBossesKilled: p.isSilverUnlocked ? null : p.bronzeBest
});

export const playerHandlers = [
  route('getMyProgress', ({ request }) => {
    const auth = authenticate(request);
    return auth.error || ok(progressOf(auth.player));
  }),

  route('updateMyNickname', ({ request, body }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const nick = body && typeof body.nickname === 'string' ? body.nickname : null;
    if (blank(nick)) return fail('400001');
    if ([...nick].length > 12) return fail('400008');
    if (BANNED_WORDS.some(w => nick.includes(w))) return fail('acc002');
    auth.player.nickname = nick; persist();
    return ok(progressOf(auth.player));
  }),

  route('getMyWallet', ({ request }) => {
    const auth = authenticate(request);
    return auth.error || ok({ coins: auth.player.coins, latestSeasonReward: latestSeasonReward(auth.player.playerId) });
  }),

  route('listMyCoinLedgerEntries', ({ request, url }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const raw = url.searchParams.get('page');
    if (raw !== null && !/^-?\d+$/.test(raw)) return fail('400009');
    const page = raw === null ? 1 : Number(raw);
    if (page < 1) return fail('400007');
    const mine = getDb().ledger.filter(e => e.playerId === auth.player.playerId)
      .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt) || Number(b.ledgerEntryId) - Number(a.ledgerEntryId));
    const items = mine.slice((page - 1) * LEDGER_PAGE_SIZE, page * LEDGER_PAGE_SIZE)
      .map(({ playerId, ...e }) => e);
    return ok({ items, totalCount: mine.length, page, hasNext: page * LEDGER_PAGE_SIZE < mine.length });
  }),

  // 領取廣告獎勵.feature: deviceId -> open (eco003) -> daily limit (eco001) -> cooldown (eco002); limit wins over cooldown
  route('claimAdReward', ({ request, body }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const deviceId = body && typeof body.deviceId === 'string' ? body.deviceId : null;
    if (blank(deviceId) || deviceId.length > 64) return fail('400001');
    const before = adRewardStatus(auth.player.playerId);
    if (!before.isEnabled) return fail('eco003');
    if (before.remainingCount === 0) return fail('eco001');
    if (before.nextClaimableAt) return fail('eco002');
    addLedger(auth.player.playerId, { ledgerSource: 'AD_REWARD', coinDelta: AD_REWARD.coins });
    const db = getDb(), entry = db.ledger[db.ledger.length - 1];
    (db.adEvents || (db.adEvents = [])).push({ playerId: auth.player.playerId, deviceId, at: now() });
    persist();
    const after = adRewardStatus(auth.player.playerId);
    return ok({ adRewardId: entry.ledgerEntryId, coinsAwarded: AD_REWARD.coins, walletCoins: auth.player.coins,
      remainingCount: after.remainingCount, nextClaimableAt: after.nextClaimableAt }, 201);
  }),

  // 建立儲值單: package id (400001 / 400012) -> open (eco004) -> member only (eco005) -> open-order cap (eco006)
  route('createTopupOrder', ({ request, body }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const packageId = body && typeof body.packageId === 'string' ? body.packageId : null;
    if (blank(packageId)) return fail('400001');
    const pkg = TOPUP_PACKAGES.find(x => x.packageId === packageId);
    if (!pkg) return fail('400012');
    const db = getDb(), p = auth.player;
    if (db.topupEnabled === false) return fail('eco004');
    if (p.playerKind === 'GUEST') return fail('eco005');
    const open = Object.values(db.topupOrders).filter(o => o.playerId === p.playerId && (o.status === 'CREATED' || o.status === 'AWAITING_PAYMENT'));
    if (open.length >= TOPUP_MAX_OPEN_ORDERS) return fail('eco006');
    const d = new Date(), z = n => String(n).padStart(2, '0');
    const topupOrderId = 'DW' + String(d.getFullYear()).slice(2) + z(d.getMonth() + 1) + z(d.getDate()) + z(d.getHours()) + z(d.getMinutes()) + z(d.getSeconds())
      + Number(nextId()).toString(36).toUpperCase().padStart(6, '0').slice(-6);
    db.topupOrders[topupOrderId] = { topupOrderId, playerId: p.playerId, ...pkg, status: 'CREATED', paymentExpireAt: null, paidAt: null };
    persist();
    // The mock "ECPay page" is this origin: mocks/browser.js turns a submit to it into a settlement + redirect to /?topup=<id>.
    return ok({ topupOrderId, packageId: pkg.packageId, amountTwd: pkg.amountTwd, coins: pkg.coins, status: 'CREATED',
      checkout: { actionUrl: db.topupActionUrl || location.origin + '/mock-ecpay', method: 'POST', fields: {
        MerchantTradeNo: topupOrderId, TotalAmount: String(pkg.amountTwd), ItemName: 'DayWar 金幣 ' + pkg.coins,
        ClientBackURL: location.origin + '/?topup=' + topupOrderId, CheckMacValue: '0'.repeat(64) } } }, 201);
  }),

  route('getTopupOrder', ({ request, params }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const o = getDb().topupOrders[params.topupOrderId];
    if (!o || o.playerId !== auth.player.playerId) return fail('404001');
    return ok({ topupOrderId: o.topupOrderId, packageId: o.packageId, amountTwd: o.amountTwd, coins: o.coins, status: o.status,
      paymentExpireAt: o.paymentExpireAt, paidAt: o.paidAt, walletCoins: auth.player.coins });
  }),

  route('listMyBuffInventories', ({ request }) => {
    const auth = authenticate(request);
    return auth.error || ok({ items: BUFF_ORDER.map(b => ({ buffItem: b, quantity: auth.player.inv[b] ?? 0 })) });
  })
];
