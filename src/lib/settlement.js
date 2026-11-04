// Turns the engine's end-of-run summary into a settleRun request body (api.yml RunSettleRequest).
// Both tiers (D42, D39): a win = the last boss down (NORMAL boss 9, EXTRA boss 7), gameSeconds = the full run time;
// a fall to any boss is an ordinary loss (kills so far + seconds survived). Only boss sequences inside the tier range
// (bronze 1-9, silver 1-7) are sent.
const BOSS_COUNT = { bronze: 9, silver: 7 };

export function buildSettlement(summary) {
  const max = BOSS_COUNT[summary.tier] || 9;
  const isVictory = summary.result === 'win';
  const seconds = Math.max(0, Math.floor(summary.t));
  const seen = new Set();
  const bossKills = [];
  for (const k of summary.kills || []) {
    if (k.bossSeq >= 1 && k.bossSeq <= max && !seen.has(k.bossSeq)) { seen.add(k.bossSeq); bossKills.push({ bossSeq: k.bossSeq, killedAtSecond: Math.max(0, k.killedAtSecond) }); }
  }
  const body = { gameSeconds: seconds, isVictory, bossKills };
  if (!isVictory) {
    const d = summary.death || { cause: 'RED_BREACH', bossSeq: null };
    body.deathCause = d.cause;
    body.deathBossSeq = d.cause === 'BOSS_REACHED' ? d.bossSeq : null;
  }
  return body;
}

/** Text shown on the result screen under the title. */
export function deathLine(settlement) {
  if (settlement.isVictory) return '用了 ' + settlement.gameSeconds + ' 秒。';
  const cause = settlement.deathCause === 'BOSS_REACHED' ? '第 ' + settlement.deathBossSeq + ' 王抵達砲台' : '紅軍突破砲台';
  return cause + '・撐了 ' + settlement.gameSeconds + ' 秒。';
}
