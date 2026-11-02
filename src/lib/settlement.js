// Turns the engine's end-of-run summary into a settleRun request body (api.yml RunSettleRequest).
// The hidden stage 9 stays a front-end effect: a secret clear / a fall to the hidden boss settles as the bronze
// stage-8 victory at the stage-8 clear time; only boss sequences inside the tier range (bronze 1-8, silver 1-6) are sent.
const BOSS_COUNT = { bronze: 8, silver: 6 };

export function buildSettlement(summary) {
  const max = BOSS_COUNT[summary.tier] || 8;
  const isVictory = summary.result !== 'lose';
  const seconds = Math.max(0, Math.floor(isVictory ? summary.clearT : summary.t));
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
