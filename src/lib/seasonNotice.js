// D38 one-time season reward notice on 首頁 (specs/ui/首頁.md, AMB-S5=a, review fix: flag key = season + board).
// Source: getMyWallet latestSeasonReward (null for guests / never rewarded). A (boardVersion, tier) pair is noticed
// once per device; the flag is written only after the notice is really on screen (same rule as banner.js), so a
// board paid out later for the same season is still noticed once.
import { readJson, writeJson } from './storage.js';
import { tierName } from './labels.js';

const KEY = 'csg.seasonNotice';
const keyOf = (boardVersion, tier) => boardVersion + '|' + tier;

/** Items of `latest` not noticed yet, NORMAL first: [{ key, text }]. */
export function pendingSeasonNotice(latest) {
  if (!latest || !Array.isArray(latest.items)) return [];
  const seen = new Set(readJson(KEY, []));
  return [...latest.items]
    .sort((a, b) => (a.tier === 'BRONZE' ? 0 : 1) - (b.tier === 'BRONZE' ? 0 : 1))
    .filter(it => !seen.has(keyOf(latest.boardVersion, it.tier)))
    .map(it => ({ key: keyOf(latest.boardVersion, it.tier), text: '上賽季 ' + tierName(it.tier) + ' 第 ' + it.rank + ' 名，獲得 ' + it.coins + ' 金幣' }));
}

/** The notice lines are on screen: remember their keys. */
export function markSeasonNoticeShown(lines) {
  const seen = readJson(KEY, []);
  const next = [...new Set([...(Array.isArray(seen) ? seen : []), ...lines.map(l => l.key)])];
  writeJson(KEY, next);
}
