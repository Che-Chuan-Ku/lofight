// Player-facing names (prototype/_ux-copy-guidelines.md). No codes or rules are ever shown.
export const BUFFS = {
  ATTACK_SPEED: { name: '攻速 +20%', desc: '砲台射得更快' },
  CANNON: { name: '砲台', desc: '開局就有砲擊' },
  LASER: { name: '雷射', desc: '定時清掉一整排' }
};
export const BUFF_ORDER = ['ATTACK_SPEED', 'CANNON', 'LASER'];
// Boss sprite per appearance order (src/game/constants.js BOSS_TILES)
export const NETWORK_MSG = '連線不穩，請再試一次';

export function lockLine(seq, best) {
  return '擊敗 bronze 第 ' + (seq ?? 8) + ' 王解鎖（目前最佳：第 ' + (best ?? 0) + ' 王）';
}

export function nicknameError(code) {
  if (code === '400001') return '暱稱不能空白';
  if (code === '400008') return '暱稱最多 12 個字';
  if (code === 'acc002') return '暱稱含有不能使用的字';
  return NETWORK_MSG;
}

/** Result-screen "next goal" line (G09) while silver is locked; null once unlocked (FE-22: no coin target). */
export function nextGoal(progress) {
  if (!progress) return null;
  if (!progress.isSilverUnlocked) {
    const best = progress.bronzeBestBossesKilled ?? 0;
    const seq = progress.silverUnlockBossSeq ?? 8;
    if (best >= seq - 1) return { text: '挑戰 bronze 第 ' + seq + ' 王，解鎖 SILVER' };
    return { pre: '擊敗第 ', num: best + 1, post: ' 王就破紀錄' };
  }
  return null;
}
