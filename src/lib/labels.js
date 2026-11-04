// Player-facing names (prototype/_ux-copy-guidelines.md). No codes or rules are ever shown.
export const BUFFS = {
  ATTACK_SPEED: { name: '攻速 +20%', desc: '砲台射得更快' },
  ATTACK: { name: 'ATK+1', desc: '士兵攻擊 +1' },
  CANNON: { name: '砲台', desc: '開局就有砲擊' },
  LASER: { name: '雷射', desc: '定時清掉一整排' }
};
export const BUFF_ORDER = ['ATTACK_SPEED', 'ATTACK', 'CANNON', 'LASER'];
// Boss sprite per appearance order (src/game/constants.js BOSS_TILES)
export const NETWORK_MSG = '連線不穩，請再試一次';

// D26: player-visible tier names. API / data keep BRONZE / SILVER; the screen only ever shows NORMAL / EXTRA, and the
// difference between them is carried by the badge and colors (D27), never by descriptive words.
export const TIER_NAME = { BRONZE: 'NORMAL', SILVER: 'EXTRA', bronze: 'NORMAL', silver: 'EXTRA' };
export const tierName = t => TIER_NAME[t] || '';
// gam001 / shp002 carry the old tier name in the backend message: the screen uses this copy instead (INF-1)
export const EXTRA_LOCKED_MSG = 'EXTRA 尚未解鎖';

// FE-47: no result yet reads 尚無成績 instead of 第 0 王. D42: the condition is "NORMAL cleared" (backend: boss `seq` = 9
// down), so the boss number is no longer part of the copy; `seq` is kept for the call sites.
export function lockLine(seq, best) {
  return 'NORMAL 通關後解鎖（目前最佳：' + (best ? '第 ' + best + ' 王' : '尚無成績') + '）';
}

export function nicknameError(code) {
  if (code === '400001') return '暱稱不能空白';
  if (code === '400008') return '暱稱最多 12 個字';
  if (code === 'acc002') return '暱稱含有不能使用的字';
  return NETWORK_MSG;
}

/** Result-screen "next goal" line (G09) while EXTRA is locked; null once unlocked (FE-22: no coin target). */
export function nextGoal(progress) {
  if (!progress) return null;
  if (!progress.isSilverUnlocked) {
    const best = progress.bronzeBestBossesKilled ?? 0;
    const seq = progress.silverUnlockBossSeq ?? 9;
    if (best >= seq - 1) return { text: '挑戰 NORMAL 第 ' + seq + ' 王，解鎖 EXTRA' };
    return { pre: '擊敗第 ', num: best + 1, post: ' 王就破紀錄' };
  }
  return null;
}
