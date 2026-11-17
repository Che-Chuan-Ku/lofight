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

// D43-D52 贊助回饋 (ECPay top-up). Player-visible copy; error codes map to their own copy, backend messages are never shown.
export const TOPUP = {
  name: '贊助回饋',
  cardDesc: 'NT$10 起，經綠界付款，回饋金幣',
  panelDesc: '每 NT$1 換 100 金幣，最低 NT$10。',
  pay: '前往綠界付款',
  redirecting: '前往綠界中…',
  payNote: '將開啟綠界（ECPay）付款頁面',
  loginFirst: '贊助回饋需要先登入，金幣才不會跟著訪客身分消失',
  checking: '正在確認付款…',
  success: coins => '已收到贊助，獲得 ' + Number(coins).toLocaleString('en-US') + ' 金幣，謝謝你',
  awaiting: at => '已取得繳費帳號，請在 ' + at + ' 前完成轉帳，入帳後金幣會自動加上',
  slow: '付款結果還在確認，金幣入帳後會自動加上；可以稍後再回商店看看',
  failed: '這次付款沒有完成，沒有扣款也沒有金幣',
  expired: '這筆付款已逾期，沒有金幣',
  notFound: '找不到這筆付款，請用付款時的帳號登入',
  ledger: '贊助儲值'
};
/** createTopupOrder error code -> panel message. */
export function topupError(code) {
  if (code === 'eco004') return '贊助回饋目前暫停';
  if (code === 'eco005') return TOPUP.loginFirst;
  if (code === 'eco006') return '未完成的付款太多了，請先完成或稍後再試';
  return NETWORK_MSG;
}
