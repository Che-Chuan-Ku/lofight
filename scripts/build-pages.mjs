// D41 (specs/ui/內容頁.md): crawlable static pages next to the single-file game build.
// Runs after `vite build` (package.json "build") and writes into dist/:
//   guide.html, bosses.html, faq.html, about.html, updates.html, privacy.html  (plain HTML, no JS, no ad script)
//   assets/tiles.png, ground.png, wall.png
//   robots.txt, sitemap.xml                              (absolute URLs from SITE_URL)
// Copy comes from src/content/*.js, the same data the in-game pages use.
// Colors come from src/styles/tokens.css, the same :root the game menu uses.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { BOSS_TILES, BOSS9_TILE } from '../src/game/constants.js';
import { TILE_B64 } from '../src/game/tiles.js';
import { LOGO_TILE, writeTileAssets } from './png-tiles.mjs';

const root = new URL('../', import.meta.url);
const dist = fileURLToPath(new URL('dist/', root));
const pkg = JSON.parse(readFileSync(new URL('package.json', root), 'utf8'));
// src/config.js reads the Vite define __APP_VERSION__; provide it before importing the content modules.
globalThis.__APP_VERSION__ = pkg.version;

const { GUIDE } = await import('../src/content/guide.js');
const { BOSSES } = await import('../src/content/bosses.js');
const { FAQ } = await import('../src/content/faq.js');
const { ABOUT } = await import('../src/content/about.js');
const { PRIVACY } = await import('../src/content/privacy.js');
const { CHANGELOG } = await import('../src/content/changelog.js');
const { HOME } = await import('../src/content/home.js');
const { CONTACT } = await import('../src/content/contact.js');
const { TERMS } = await import('../src/content/terms.js');

const SITE_URL = (process.env.SITE_URL || 'https://daywar.net').replace(/\/+$/, '');
const SITE = 'DayWar';
const FONTS = 'https://fonts.googleapis.com/css2?family=Luckiest+Guy&family=Noto+Sans+TC:wght@500;700;900&display=swap';
const TOKENS = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8');

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// Footer / cross links, in the same order as the in-game home footer.
const PAGES = [
  { file: 'guide.html', label: '遊戲指南' },
  { file: 'bosses.html', label: '魔王攻略' },
  { file: 'faq.html', label: '常見問題' },
  { file: 'about.html', label: '關於' },
  { file: 'updates.html', label: '開發者日誌' },
  { file: 'privacy.html', label: '隱私權政策' },
  { file: 'contact.html', label: '聯絡我們' },
  { file: 'terms.html', label: '服務條款' }
];

function inline(v) {
  return (Array.isArray(v) ? v : [v]).map(s => {
    if (typeof s === 'string') return esc(s);
    const ext = /^https?:/.test(s.href);
    return '<a href="' + esc(s.href) + '"' + (ext ? ' target="_blank" rel="noopener noreferrer"' : '') + '>' + esc(s.text) + '</a>';
  }).join('');
}
function block(b) {
  if (b && b.ul) return '<ul>' + b.ul.map(li => '<li>' + inline(li) + '</li>').join('') + '</ul>';
  if (b && b.h3) return '<h3>' + esc(b.h3) + '</h3>\n' + (b.blocks || []).map(block).join('\n');
  return '<p>' + inline(b) + '</p>';
}
function plainInline(v) {
  return (Array.isArray(v) ? v : [v]).map(s => typeof s === 'string' ? s : s.text).join('');
}
function plainBlock(b) {
  if (b && b.ul) return b.ul.map(plainInline).join('\n');
  if (b && b.h3) return [b.h3, ...(b.blocks || []).map(plainBlock)].filter(Boolean).join('\n');
  return plainInline(b);
}
function stringsOf(v, out = []) {
  if (typeof v === 'string') out.push(v);
  else if (Array.isArray(v)) v.forEach(x => stringsOf(x, out));
  else if (v && typeof v === 'object') {
    if (v.text) out.push(v.text);
    else Object.values(v).forEach(x => stringsOf(x, out));
  }
  return out;
}

// CSS sprite on the 12-column Kenney sheet. Decorative marks omit a name; boss art gets one.
function spr(tile, { tint = '', label = '', small = false } = {}) {
  const c = tile % 12, r = (tile / 12) | 0;
  const cls = 'spr' + (small ? ' sm' : '') + (tint ? ' tint-' + tint : '');
  const style = '--c:' + c + ';--r:' + r;
  if (!label) return '<span class="' + cls + '" style="' + style + '" aria-hidden="true"></span>';
  return '<span class="' + cls + '" style="' + style + '" role="img" aria-label="' + esc(label) + '"></span>';
}

function bossLook(tier, n) {
  if (tier === 'NORMAL' && n === 9) return { tile: BOSS9_TILE, tint: 'dark' };
  if (tier === 'EXTRA' && n === 7) return { tile: BOSS9_TILE, tint: 'purple' };
  return { tile: BOSS_TILES[n - 1], tint: '' };
}
function bossId(tier, n) { return tier.toLowerCase() + '-' + n; }
function bossLabel(tier, n) { return tier + ' 第 ' + n + ' 王'; }

function heading(tag, text) {
  const latin = /^v[\d.]/.test(text) ? ' class="latin"' : '';
  return '<' + tag + latin + '>' + esc(text) + '</' + tag + '>';
}
function panel(title, inner, id) {
  const idAttr = id ? ' id="' + esc(id) + '"' : '';
  return '<section class="panel"' + idAttr + '>\n' + heading('h2', title) + '\n<div class="wall" role="presentation"></div>\n' + inner + '\n</section>';
}
const sections = list => list.map(s => panel(s.h, s.blocks.map(block).join('\n'), s.id)).join('\n');
function termsBody(list) {
  const index = '<nav class="sec-index" aria-label="條款目錄">' + list.map(s => '<a href="#' + esc(s.id) + '">' + esc(s.h) + '</a>').join('') + '</nav>';
  return index + '\n' + sections(list);
}

function bossHeading(tier, text) {
  const m = /^第 (\d+) 王$/.exec(text);
  const badge = '<span class="badge ' + (tier === 'EXTRA' ? 'extra' : 'normal') + '">' + tier + '</span>';
  const title = m ? '第 <span class="num">' + m[1] + '</span> 王' : esc(text);
  return '<h3>' + badge + title + '</h3>';
}
function bossBody(list) {
  const index = { NORMAL: [], EXTRA: [] };
  const body = list.map(s => {
    const tier = s.h === 'NORMAL' || s.h === 'EXTRA' ? s.h : null;
    const parts = [];
    for (const b of s.blocks) {
      if (b && b.h3 && tier) {
        const n = Number((/^第 (\d+) 王$/.exec(b.h3) || [])[1]);
        const look = bossLook(tier, n);
        const id = bossId(tier, n);
        const label = bossLabel(tier, n);
        index[tier].push({ id, n, look, label });
        parts.push('<article class="card" id="' + id + '">\n' + spr(look.tile, { tint: look.tint, label }) + '\n<div>\n' + bossHeading(tier, b.h3) + '\n' + (b.blocks || []).map(block).join('\n') + '\n</div>\n</article>');
      } else parts.push(block(b));
    }
    return panel(s.h, parts.join('\n'));
  }).join('\n');
  const row = (tier, items) => '<div><p class="idx-label">' + tier + '</p><div class="idx-row">' + items.map(it => '<a href="#' + it.id + '">' + spr(it.look.tile, { tint: it.look.tint, label: it.label }) + '</a>').join('') + '</div></div>';
  const nav = '<nav class="boss-index" aria-label="魔王索引">\n' + row('NORMAL', index.NORMAL) + '\n' + row('EXTRA', index.EXTRA) + '\n</nav>';
  return nav + '\n' + body;
}
function faqBody(list) {
  return list.map(s => {
    const inner = s.qa
      ? s.qa.map(item => '<article class="card qcard">\n<h3><span class="qmark" aria-hidden="true">Q</span>' + esc(item.q) + '</h3>\n' + item.a.map(block).join('\n') + '\n</article>').join('\n')
      : (s.blocks || []).map(block).join('\n');
    return panel(s.h, inner);
  }).join('\n');
}
function faqJsonLd(list) {
  const mainEntity = [];
  for (const s of list) {
    for (const item of s.qa || []) {
      mainEntity.push({
        '@type': 'Question',
        name: item.q,
        acceptedAnswer: { '@type': 'Answer', text: item.a.map(plainBlock).filter(Boolean).join('\n') }
      });
    }
  }
  return { '@context': 'https://schema.org', '@type': 'FAQPage', mainEntity };
}

const CSS = `
${TOKENS}
* { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; }
body { margin: 0; color: var(--color-text);
  font: 16px/1.75 var(--font-body);
  background-color: var(--color-bg);
  background-image: url(/assets/ground.png);
  background-repeat: repeat;
  background-size: 48px 48px;
  image-rendering: pixelated; }
img, .spr { image-rendering: pixelated; }
a { color: var(--color-ally-light); font-weight: 700; text-underline-offset: 3px; }
.wrap { position: relative; max-width: 720px; margin: 0 auto; padding: 16px 16px 32px; }
.top { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 56px; }
.brand { display: inline-flex; align-items: center; gap: 8px; min-height: 44px; text-decoration: none; }
.logo { font-family: var(--font-display); font-size: 28px; line-height: 1; letter-spacing: 1px; color: var(--color-accent); text-shadow: var(--text-shadow-title); }
.play { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; padding: 0 16px;
  background: var(--color-action); color: var(--color-text-on-action); text-decoration: none;
  border-radius: var(--radius-md); box-shadow: var(--shadow-stamp-action); font-weight: 700; }
.play:active { transform: translateY(4px); box-shadow: none; }
h1 { margin: 16px 0 8px; font-family: var(--font-body); font-weight: 900; font-size: 32px; line-height: 1.3; color: var(--color-accent); text-shadow: 0 3px 0 var(--color-stage-edge); }
.lead { margin: 0 0 16px; color: var(--sand); }
main { display: flex; flex-direction: column; gap: 20px; margin-top: 8px; }
.panel { background: var(--color-surface); border: 3px solid var(--color-stage-edge); border-radius: var(--radius-xl); box-shadow: var(--shadow-stamp-card); padding: 8px 16px 16px; }
.wall { height: 16px; margin: 4px 0 12px; background: url(/assets/wall.png) repeat-x; background-size: 32px 16px; image-rendering: pixelated; }
h2 { margin: 8px 0 0; font-family: var(--font-body); font-weight: 900; font-size: 24px; line-height: 1.3; color: var(--color-accent); text-shadow: 0 3px 0 var(--color-stage-edge); }
h2.latin { font-family: var(--font-display); font-weight: 400; letter-spacing: 1px; }
h3 { margin: 0 0 6px; font-family: var(--font-body); font-weight: 700; font-size: 18px; line-height: 1.4; color: var(--fg); text-shadow: var(--text-shadow-card); }
p { margin: 0 0 8px; }
ul { margin: 0 0 8px; padding-left: 1.3em; }
li + li { margin-top: 4px; }
.card { display: flex; gap: 12px; align-items: flex-start; margin: 12px 0 0; padding: 12px;
  background: var(--color-bg); border: 3px solid var(--color-stage-edge); border-radius: var(--radius-lg); box-shadow: var(--shadow-stamp-card); }
.qcard { flex-direction: column; gap: 8px; }
.card h3 { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.badge { font-family: var(--font-display); font-weight: 400; font-size: 13px; letter-spacing: 1px; line-height: 1; padding: 4px 8px; border-radius: var(--radius-sm); color: var(--color-text-on-tier); }
.badge.normal { background: var(--color-tier-normal); }
.badge.extra { background: var(--color-tier-extra); }
.num { font-family: var(--font-display); font-weight: 400; font-size: 22px; color: var(--color-accent); text-shadow: var(--text-shadow-title); }
.qmark { font-family: var(--font-display); font-weight: 400; font-size: 22px; color: var(--color-accent); text-shadow: var(--text-shadow-title); }
.spr { display: block; flex: none; width: 48px; height: 48px; background: url(/assets/tiles.png) no-repeat;
  background-size: calc(12 * 48px) calc(11 * 48px);
  background-position: calc(var(--c) * -48px) calc(var(--r) * -48px); }
.spr.sm { width: 32px; height: 32px; background-size: calc(12 * 32px) calc(11 * 32px); background-position: calc(var(--c) * -32px) calc(var(--r) * -32px); }
.spr.tint-dark { background-color: rgba(120, 0, 40, 0.55); background-blend-mode: multiply; }
.spr.tint-purple { background-color: rgba(150, 40, 255, 0.5); background-blend-mode: multiply; }
.boss-index { display: flex; flex-wrap: wrap; gap: 16px 28px; justify-content: center; }
.sec-index { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.sec-index a { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; padding: 0 12px;
  background: var(--color-surface); color: var(--fg); text-decoration: none; font-weight: 700;
  border: 3px solid var(--color-stage-edge); border-radius: var(--radius-md); box-shadow: var(--shadow-stamp-neutral); }
.idx-label { margin: 0 0 6px; text-align: center; font-family: var(--font-display); font-size: 16px; letter-spacing: 1px; color: var(--color-accent); text-shadow: var(--text-shadow-title); }
.idx-row { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; }
.idx-row a { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; }
.date { color: var(--sand); font-size: 14px; margin: 0 0 4px; }
.updated { color: var(--sand); font-size: 14px; margin-top: 16px; }
footer { margin-top: 24px; text-align: center; color: var(--sand); font-size: 14px; }
footer nav { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.foot-row { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }
.foot-row a, .foot-row span { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; padding: 0 12px;
  background: var(--color-surface-raised); color: var(--fg); text-decoration: none; font-weight: 700;
  border: 3px solid var(--color-stage-edge); border-radius: var(--radius-md); box-shadow: var(--shadow-stamp-neutral); }
.foot-row span { background: var(--color-accent); color: var(--color-text-on-tier); }
footer p { margin: 12px 0 0; }
`.trim();

const FOOT_GAME = [
  { file: '', label: '回到遊戲', href: '/' },
  { file: 'guide.html', label: '遊戲指南' },
  { file: 'bosses.html', label: '魔王攻略' },
  { file: 'faq.html', label: '常見問題' }
];
const FOOT_SITE = [
  { file: 'about.html', label: '關於' },
  { file: 'updates.html', label: '開發者日誌' },
  { file: 'contact.html', label: '聯絡我們' },
  { file: 'privacy.html', label: '隱私權政策' },
  { file: 'terms.html', label: '服務條款' }
];
function footRow(items, file) {
  return '<div class="foot-row">' + items.map(p => {
    if (p.file === file) return '<span aria-current="page">' + esc(p.label) + '</span>';
    return '<a href="' + (p.href || '/' + p.file) + '">' + esc(p.label) + '</a>';
  }).join('') + '</div>';
}
function page({ file, title, description, lead, body, jsonLd }) {
  const nav = footRow(FOOT_GAME, file) + '\n' + footRow(FOOT_SITE, file);
  return `<!doctype html>
<html lang="zh-Hant">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}｜${SITE}</title>
<meta name="description" content="${esc(description)}">
<link rel="canonical" href="${esc(SITE_URL + '/' + file)}">
<link rel="stylesheet" href="${FONTS}">
${jsonLd ? '<script type="application/ld+json">' + JSON.stringify(jsonLd).replace(/</g, '\\u003c') + '</script>' : ''}
<style>
${CSS}
</style>
</head>
<body>
<div class="wrap">
<header class="top"><a class="brand" href="/">${spr(LOGO_TILE, { small: true })}<span class="logo">${SITE}</span></a><a class="play" href="/">開始遊戲</a></header>
<h1>${esc(title)}</h1>
${lead ? '<p class="lead">' + esc(lead) + '</p>' : ''}
<main>
${body}
</main>
<footer>
<nav aria-label="網站資訊">
${nav}
</nav>
<p>© ${SITE}</p>
</footer>
</div>
</body>
</html>
`;
}

const out = {
  'guide.html': page({ file: 'guide.html', title: GUIDE.title, description: GUIDE.description, lead: GUIDE.lead, body: sections(GUIDE.sections) }),
  'bosses.html': page({ file: 'bosses.html', title: BOSSES.title, description: BOSSES.description, lead: BOSSES.lead, body: bossBody(BOSSES.sections) }),
  'faq.html': page({ file: 'faq.html', title: FAQ.title, description: FAQ.description, lead: FAQ.lead, body: faqBody(FAQ.sections), jsonLd: faqJsonLd(FAQ.sections) }),
  'about.html': page({ file: 'about.html', title: ABOUT.title, description: ABOUT.description, lead: ABOUT.lead, body: sections(ABOUT.sections) }),
  'updates.html': page({
    file: 'updates.html', title: '開發者日誌',
    description: 'DayWar 開發者日誌：每個版本改了什麼，以玩家看得懂的話寫成。',
    lead: '每一版改了什麼。說得很少，但都是真的。',
    body: CHANGELOG.map(v => panel('v' + v.version, '<p class="date"><time datetime="' + esc(v.date) + '">' + esc(v.date) + '</time></p>\n<ul>' + v.items.map(t => '<li>' + esc(t) + '</li>').join('') + '</ul>')).join('\n')
  }),
  'privacy.html': page({
    file: 'privacy.html', title: PRIVACY.title, description: PRIVACY.description,
    body: sections(PRIVACY.sections) + '\n<p class="updated">最後更新：<time datetime="' + esc(PRIVACY.updated) + '">' + esc(PRIVACY.updated) + '</time></p>'
  }),
  'contact.html': page({ file: 'contact.html', title: CONTACT.title, description: CONTACT.description, lead: CONTACT.lead, body: sections(CONTACT.sections) }),
  'terms.html': page({
    file: 'terms.html', title: TERMS.title, description: TERMS.description, lead: TERMS.lead,
    body: termsBody(TERMS.sections) + '\n<p class="updated">最後更新：<time datetime="' + esc(TERMS.updated) + '">' + esc(TERMS.updated) + '</time></p>'
  }),
  'robots.txt': 'User-agent: *\nAllow: /\n\nSitemap: ' + SITE_URL + '/sitemap.xml\n',
  'sitemap.xml': '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
    + ['', ...PAGES.map(p => p.file)].map(f => '  <url><loc>' + esc(SITE_URL + '/' + f) + '</loc></url>').join('\n')
    + '\n</urlset>\n'
};

mkdirSync(dist, { recursive: true });
for (const [name, text] of Object.entries(out)) writeFileSync(dist + name, text);
writeTileAssets(dist + '/assets', Buffer.from(TILE_B64, 'base64'));

// Every player-facing sentence must still be in the built pages (design changes structure, not copy).
const built = Object.fromEntries(['guide.html', 'bosses.html', 'faq.html', 'about.html', 'contact.html', 'terms.html', 'privacy.html', 'updates.html']
  .map(name => [name, readFileSync(dist + name, 'utf8')]));
function expectIn(file, texts) {
  for (const t of texts) if (t && !built[file].includes(t)) throw new Error(file + ' is missing copy: ' + t.slice(0, 40));
}
expectIn('guide.html', stringsOf(GUIDE.sections));
expectIn('about.html', stringsOf(ABOUT.sections));
expectIn('bosses.html', stringsOf(BOSSES.sections).filter(t => !/^第 \d+ 王$/.test(t)));
expectIn('faq.html', FAQ.sections.flatMap(s => s.qa ? s.qa.flatMap(q => [q.q, ...stringsOf(q.a)]) : stringsOf(s.blocks || [])));
expectIn('contact.html', stringsOf(CONTACT.sections));
expectIn('terms.html', stringsOf(TERMS.sections));
if ((built['bosses.html'].match(/<article class="card"/g) || []).length !== 16) throw new Error('bosses.html should have 16 boss cards');
if (built['faq.html'].includes('<details')) throw new Error('faq.html must not collapse answers');
if ((built['faq.html'].match(/<script/g) || []).length !== 1) throw new Error('faq.html should have only the FAQPage script');
const staticHtml = Object.values(built).join('\n');
if (/adsbygoogle|pagead|adBreak/.test(staticHtml)) throw new Error('static pages must not load ads');
if (/<details/.test(staticHtml)) throw new Error('static pages must not collapse copy');

const indexHtml = readFileSync(dist + 'index.html', 'utf8');
for (const p of HOME.paragraphs) {
  if (!indexHtml.includes(p)) throw new Error('dist/index.html #intro is out of sync with src/content/home.js');
}
for (const l of HOME.links) {
  if (!indexHtml.includes('href="' + l.href + '"')) throw new Error('dist/index.html #intro missing ' + l.href);
}
const fontLinks = indexHtml.match(/fonts\.googleapis\.com/g) || [];
if (fontLinks.length !== 1) throw new Error('index.html should load Google Fonts once, found ' + fontLinks.length);

console.log('build-pages: wrote ' + Object.keys(out).join(', ') + ', assets (SITE_URL=' + SITE_URL + ')');
