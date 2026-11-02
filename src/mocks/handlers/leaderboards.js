// Leaderboards tag handler.
import { route, ok, fail, authenticate, blank } from './util.js';
import { rankBoard, BOARD_VERSION } from '../db.js';

export const leaderboardHandlers = [
  route('getLeaderboard', ({ request, url }) => {
    const auth = authenticate(request);
    if (auth.error) return auth.error;
    const tier = url.searchParams.get('tier');
    if (blank(tier)) return fail('400001');
    if (tier !== 'BRONZE' && tier !== 'SILVER') return fail('400012');
    const boardVersion = url.searchParams.get('boardVersion') || BOARD_VERSION;
    const all = rankBoard(boardVersion, tier);
    const i = all.findIndex(e => e.playerId === auth.player.playerId);
    return ok({
      boardVersion, tier, entries: all.slice(0, 15),
      myRank: i >= 0 ? all[i] : null,
      neighbors: i >= 0 ? [...all.slice(Math.max(0, i - 2), i), ...all.slice(i + 1, i + 3)] : []
    });
  })
];
