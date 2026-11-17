// Nickname helpers shared by the front end.

const DEFAULT_PREFIX = '勇者';

/**
 * True when the nickname has the shape of a system default one: "勇者" + a number 100..999.
 * MUST stay in step with the back end rule: backend/src/main/java/com/crowdsiege/api/service/gateway/RandomNicknameGenerator.java
 * (isDefaultName; callers there pass the NFKC-normalized, stripped name, so full-width digits count too).
 */
export function isDefaultNickname(nickname) {
  if (typeof nickname !== 'string') return false;
  const name = nickname.normalize('NFKC').trim();
  if (!name.startsWith(DEFAULT_PREFIX)) return false;
  const suffix = name.slice(DEFAULT_PREFIX.length);
  return /^[0-9]{3}$/.test(suffix) && Number(suffix) >= 100;
}
