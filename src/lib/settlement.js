// Turns the engine's end-of-run summary into a settleRun request body (api.yml RunSettleRequest).
// The hidden stage 9 stays a front-end effect: a secret clear / a fall to the hidden boss settles as the bronze
// stage-8 victory; only boss sequences inside the tier range (bronze 1-8, silver 1-7) are sent, never a secret field.
// Seconds (FE-39): a secret clear sends the full run time incl. stage 9; a fall to the hidden boss sends the stage-8 clear time.
// EXTRA (D39, INF-X16): seven regular bosses, no hidden-stage rule: a win = boss 7 down, gameSeconds = the full run
// time (clearT is set at the kill); a fall to boss 7 is an ordinary loss (kills 1-6 + seconds survived).
const BOSS_COUNT = { bronze: 8, silver: 7 };

export function buildSettlement(summary) {
  const max = BOSS_COUNT[summary.tier] || 8;
  const isVictory = summary.result !== 'lose';
  const seconds = Math.max(0, Math.floor(summary.result === 'secret' ? summary.t : isVictory ? summary.clearT : summary.t));
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
export function deathLine(settlement, result) {
  // v18.7.5: falling to the hidden boss still counts as the stage-8 clear
  if (result === 'secretfail') return '第 8 關已通關，成績 ' + settlement.gameSeconds + ' 秒已計入，隱藏魔王下次再戰。';
  if (settlement.isVictory) return '用了 ' + settlement.gameSeconds + ' 秒。';
  const cause = settlement.deathCause === 'BOSS_REACHED' ? '第 ' + settlement.deathBossSeq + ' 王抵達砲台' : '紅軍突破砲台';
  return cause + '・撐了 ' + settlement.gameSeconds + ' 秒。';
}
