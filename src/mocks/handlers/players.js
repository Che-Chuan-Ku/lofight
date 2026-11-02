// Players tag handlers (progress, nickname, wallet, ledger, buff inventory).
import { route, ok, fail, authenticate, blank } from './util.js';
import { getDb, persist, BANNED_WORDS, LEDGER_PAGE_SIZE, BUFF_ORDER, SILVER_UNLOCK_BOSS_SEQ } from '../db.js';

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
    return auth.error || ok({ coins: auth.player.coins });
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

  route('listMyBuffInventories', ({ request }) => {
    const auth = authenticate(request);
    return auth.error || ok({ items: BUFF_ORDER.map(b => ({ buffItem: b, quantity: auth.player.inv[b] })) });
  })
];
