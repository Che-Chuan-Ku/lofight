// 開發者日誌 display cap (specs/ui/開發者日誌頁.md, user 2026-11-03): walk versions newest first and keep items until
// `max` in total; older items are dropped (FIFO), a partly cut version keeps its first items, empty versions vanish.
// The data file keeps every entry.
export const CHANGELOG_MAX_ITEMS = 20;

export function capChangelog(list, max = CHANGELOG_MAX_ITEMS) {
  const out = [];
  let left = max;
  for (const v of list) {
    if (left <= 0) break;
    const items = v.items.slice(0, left);
    if (!items.length) continue;
    out.push({ ...v, items });
    left -= items.length;
  }
  return out;
}
