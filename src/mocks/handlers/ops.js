// Ops tag handler (no UI). 400009 is thrown by the backend although api.yml x-error-codes omits it.
import { route, ok, fail, blank } from './util.js';
import { getDb } from '../db.js';

export const opsHandlers = [
  route('getEventStatistics', ({ url }) => {
    const from = url.searchParams.get('from'), to = url.searchParams.get('to');
    if (blank(from) || blank(to)) return fail('400001');
    const f = Date.parse(from), t = Date.parse(to);
    if (Number.isNaN(f) || Number.isNaN(t)) return fail('400009');
    if (t < f) return fail('400007');
    const buys = getDb().ledger.filter(e => e.ledgerSource === 'BUFF_PURCHASE' && Date.parse(e.occurredAt) >= f && Date.parse(e.occurredAt) < t);
    const byItem = {};
    for (const b of buys) { byItem[b.buffItem] = byItem[b.buffItem] || { buffItem: b.buffItem, count: 0, totalCoins: 0 }; byItem[b.buffItem].count++; byItem[b.buffItem].totalCoins -= b.coinDelta; }
    return ok({ from, to,
      eventTotals: [{ eventKind: 'SHOP_VIEW', count: 0, uniquePlayers: 0 }, { eventKind: 'BUFF_PURCHASE', count: buys.length, uniquePlayers: new Set(buys.map(b => b.playerId)).size }],
      purchasesByItem: Object.values(byItem) });
  })
];
