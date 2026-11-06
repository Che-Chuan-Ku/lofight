// D41: crawlable static pages next to the single-file game build.
// Runs after `vite build` and writes into dist/:
//   intro.html, guide.html, bosses.html, faq.html, games.html, updates.html,
//   privacy.html, contact.html, terms.html, about.html (redirect to intro)
//   assets/tiles.png, ground.png, wall.png, logo-sprite.png, game-thumb-daywar.png
//   robots.txt, sitemap.xml
// Copy comes from src/content/*.js. Chrome uses --site-* (src/styles/site.css). No ad script.
import { readFileSync, writeFileSync, mkdirSync, copyFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { BOSS_TILES, BOSS9_TILE } from '../src/game/constants.js';
import { TILE_B64 } from '../src/game/tiles.js';
import { writeTileAssets } from './png-tiles.mjs';

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
const FONTS = 'https://fonts.googleapis.com/css2?family=Noto+Sans+TC:wght@500;700&display=swap';
const TOKENS = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8');
const SITE_CSS = readFileSync(new URL('../src/styles/site.css', import.meta.url), 'utf8');

const PRIMARY = [
  { file: 'intro.html', label: '遊戲介紹' },
  { file: 'guide.html', label: '遊戲指南' },
  { file: 'bosses.html', label: '魔王攻略' },
  { file: 'faq.html', label: '常見問題' },
  { file: 'games.html', label: '更多遊戲' }
];
const FOOTER = [
  { file: 'updates.html', label: '開發者日誌' },
  { file: 'contact.html', label: '聯絡我們' },
  { file: 'terms.html', label: '服務條款' },
  { file: 'privacy.html', label: '隱私權政策' }
];
const SITEMAP = ['', 'intro.html', 'guide.html', 'bosses.html', 'faq.html', 'games.html', 'updates.html', 'contact.html', 'terms.html', 'privacy.html'];

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

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
  return '<' + tag + '>' + esc(text) + '</' + tag + '>';
}
function panel(title, inner, id) {
  const idAttr = id ? ' id="' + esc(id) + '"' : '';
  return '<article class="article"' + idAttr + '>\n' + heading('h2', title) + '\n' + inner + '\n</article>';
}
function subnav(items) {
  return '<nav class="subnav" aria-label="本頁目錄">' + items.map(s => '<a href="#' + esc(s.id) + '">' + esc(s.h) + '</a>').join('') + '</nav>';
}
function withIds(list) {
  return list.map((s, i) => ({ ...s, id: s.id || ('sec-' + i) }));
}
function sections(list) {
  const items = withIds(list);
  return subnav(items) + '\n' + items.map(s => panel(s.h, (s.blocks || []).map(block).join('\n'), s.id)).join('\n');
}
function termsBody(list) {
  return sections(list);
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
  const items = withIds(list);
  const body = items.map(s => {
    const inner = s.qa
      ? s.qa.map(item => '<article class="card qcard">\n<h3><span class="qmark" aria-hidden="true">Q</span>' + esc(item.q) + '</h3>\n' + item.a.map(block).join('\n') + '\n</article>').join('\n')
      : (s.blocks || []).map(block).join('\n');
    return panel(s.h, inner, s.id);
  }).join('\n');
  return subnav(items) + '\n' + body;
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

const EXTRA = `
img, .spr { image-rendering: pixelated; }
.card { display: flex; gap: 12px; align-items: flex-start; margin: 12px 0 0; padding: 12px;
  background: var(--site-bg); border: 1px solid var(--site-border); border-radius: var(--site-radius); }
.qcard { flex-direction: column; gap: 8px; }
.card h3 { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.badge { font-weight: 700; font-size: 13px; letter-spacing: 1px; line-height: 1; padding: 4px 8px; border-radius: var(--radius-sm); color: var(--color-text-on-tier); }
.badge.normal { background: var(--color-tier-normal); }
.badge.extra { background: var(--color-tier-extra); }
.qmark { font-weight: 700; font-size: 18px; color: var(--site-accent); margin-right: 6px; }
.spr { display: block; flex: none; width: 48px; height: 48px; background: url(/assets/tiles.png) no-repeat;
  background-size: calc(12 * 48px) calc(11 * 48px);
  background-position: calc(var(--c) * -48px) calc(var(--r) * -48px); }
.spr.tint-dark { background-color: rgba(120, 0, 40, 0.55); background-blend-mode: multiply; }
.spr.tint-purple { background-color: rgba(150, 40, 255, 0.5); background-blend-mode: multiply; }
.boss-index { display: flex; flex-wrap: wrap; gap: 16px 28px; justify-content: center; margin: 0 0 24px; }
.idx-label { margin: 0 0 6px; text-align: center; font-size: 14px; letter-spacing: 1px; color: var(--site-text); font-weight: 700; }
.idx-row { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; }
.idx-row a { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; }
`.trim();

const CSS = TOKENS + '\n' + SITE_CSS + '\n' + EXTRA;

function linkList(items, file) {
  return items.map(p => {
    const cur = p.file === file ? ' aria-current="page"' : '';
    return '<a href="/' + p.file + '"' + cur + '>' + esc(p.label) + '</a>';
  }).join('\n');
}
function footerNav(file) {
  const row = items => '<div class="foot-row">' + items.map(p => {
    const cur = p.file === file ? ' aria-current="page"' : '';
    return '<a href="/' + p.file + '"' + cur + '>' + esc(p.label) + '</a>';
  }).join('') + '</div>';
  return row(FOOTER.slice(0, 2)) + '\n' + row(FOOTER.slice(2));
}
const DRAWER_JS = `<script>
(function () {
  var btn = document.querySelector('.menu-btn');
  var drawer = document.getElementById('site-drawer');
  if (!btn || !drawer) return;
  btn.addEventListener('click', function () {
    var open = drawer.classList.toggle('open');
    btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    btn.setAttribute('aria-label', open ? '關閉選單' : '開啟選單');
  });
})();
</script>`;

function chrome(file) {
  return `<a class="skip" href="#content">跳到內容</a>
<header class="site-header">
  <div class="header-inner">
    <a class="brand" href="/"><img class="brand-sprite" src="/assets/logo-sprite.png" width="32" height="32" alt="">${SITE}</a>
    <nav class="nav-primary" aria-label="主要">
${linkList(PRIMARY, file)}
    </nav>
    <div class="header-actions">
      <a class="btn-cta" href="/#stage">開始遊戲</a>
      <button class="menu-btn" type="button" aria-expanded="false" aria-controls="site-drawer" aria-label="開啟選單"><span></span><span></span><span></span></button>
    </div>
  </div>
</header>
<div class="nav-drawer" id="site-drawer">
  <nav aria-label="主要">
${linkList(PRIMARY, file)}
  </nav>
  <nav class="drawer-sub" aria-label="網站資訊">
${linkList(FOOTER, file)}
  </nav>
</div>`;
}

function page({ file, title, description, lead, body, jsonLd, mainClass = 'reading' }) {
  return `<!doctype html>
<html lang="zh-Hant">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
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
${chrome(file)}
<main class="${mainClass}" id="content">
<h1>${esc(title)}</h1>
${lead ? '<p class="lead">' + esc(lead) + '</p>' : ''}
${body}
</main>
<footer class="site-footer">
<nav aria-label="網站資訊">
${footerNav(file)}
</nav>
<p>© ${SITE}</p>
</footer>
${DRAWER_JS}
</body>
</html>
`;
}

function unlink(node, href) {
  if (typeof node === 'string' || node == null) return node;
  if (Array.isArray(node)) return node.map(x => unlink(x, href));
  if (typeof node === 'object') {
    if (node.href === href && node.text) return node.text;
    const out = {};
    for (const [k, v] of Object.entries(node)) out[k] = unlink(v, href);
    return out;
  }
  return node;
}
const guideSections = GUIDE.sections
  .filter(s => s.h !== '還想看細的')
  .map(s => unlink(s, '/privacy.html'));

function introBody() {
  const paras = HOME.paragraphs.slice(0, 4).map(p => '<p>' + esc(p) + '</p>').join('\n');
  const about = ABOUT.sections.map((s, i) => ({ ...s, id: ['what-about', 'credits', 'contact'][i] || ('about-' + i) }));
  const chips = [{ id: 'what-game', h: '這款遊戲在做什麼' }, ...about.map(s => ({ id: s.id, h: s.h }))];
  const home = '<article class="article" id="what-game">\n<h2>這款遊戲在做什麼</h2>\n' + paras + '\n</article>';
  const rest = about.map(s => panel(s.h, s.blocks.map(block).join('\n'), s.id)).join('\n');
  return subnav(chips) + '\n' + home + '\n' + rest;
}

// Card shape: { id, name, description, thumbnail, href, comingSoon }.
// The two new sentences are the ones approved with the round-8 design package.
const GAMES = [
  {
    id: 'daywar',
    name: 'DayWar',
    description: 'DayWar 是一款打開瀏覽器就能玩的塔防遊戲。',
    thumbnail: '/assets/game-thumb-daywar.png',
    href: '/#stage',
    comingSoon: false
  },
  {
    id: 'coming-soon',
    name: null,
    description: '尚未公開，敬請期待。',
    thumbnail: null,
    href: null,
    comingSoon: true
  }
];
function gamesBody() {
  const cards = GAMES.map(g => {
    if (g.comingSoon) {
      return '<li><article class="game-card game-card--soon"><div class="game-card__thumb"><span class="game-card__soon-mark">即將推出</span></div><h2>即將推出</h2><p>' + esc(g.description) + '</p><button class="btn-cta" type="button" disabled aria-disabled="true">開始玩</button></article></li>';
    }
    return '<li><article class="game-card"><div class="game-card__thumb"><img src="' + esc(g.thumbnail) + '" width="224" height="126" alt="' + esc(g.name) + ' 遊戲畫面縮圖"></div><h2>' + esc(g.name) + '</h2><p>' + esc(g.description) + '</p><a class="btn-cta" href="' + esc(g.href) + '">開始玩</a></article></li>';
  }).join('\n');
  return '<ul class="games-grid">\n' + cards + '\n</ul>';
}
function updatesBody() {
  const items = CHANGELOG.map(v => ({
    id: 'v' + v.version.replace(/\./g, '-'),
    h: 'v' + v.version,
    date: v.date,
    items: v.items
  }));
  return subnav(items) + '\n' + items.map(v => panel(v.h, '<p class="date"><time datetime="' + esc(v.date) + '">' + esc(v.date) + '</time></p>\n<ul>' + v.items.map(t => '<li>' + esc(t) + '</li>').join('') + '</ul>', v.id)).join('\n');
}

const aboutRedirect = `<!doctype html>
<html lang="zh-Hant">
<head>
<meta charset="utf-8">
<meta http-equiv="refresh" content="0; url=/intro.html">
<link rel="canonical" href="${SITE_URL}/intro.html">
<title>遊戲介紹｜${SITE}</title>
<script>location.replace('/intro.html');</script>
</head>
<body>
<p><a href="/intro.html">遊戲介紹</a></p>
</body>
</html>
`;

const out = {
  'intro.html': page({
    file: 'intro.html', title: '遊戲介紹',
    description: 'DayWar 是一款打開瀏覽器就能玩的塔防遊戲。個人開發，素材來源與聯絡方式。',
    lead: ABOUT.lead, body: introBody()
  }),
  'guide.html': page({ file: 'guide.html', title: GUIDE.title, description: GUIDE.description, lead: GUIDE.lead, body: sections(guideSections) }),
  'bosses.html': page({ file: 'bosses.html', title: BOSSES.title, description: BOSSES.description, lead: BOSSES.lead, body: bossBody(BOSSES.sections) }),
  'faq.html': page({ file: 'faq.html', title: FAQ.title, description: FAQ.description, lead: FAQ.lead, body: faqBody(FAQ.sections), jsonLd: faqJsonLd(FAQ.sections) }),
  'games.html': page({
    file: 'games.html', title: '更多遊戲', mainClass: 'games',
    description: 'DayWar 是一款打開瀏覽器就能玩的塔防遊戲。',
    lead: '同一個站裡可以玩的遊戲。新的會列在這裡。',
    body: gamesBody()
  }),
  'updates.html': page({
    file: 'updates.html', title: '開發者日誌',
    description: 'DayWar 開發者日誌：每個版本改了什麼，以玩家看得懂的話寫成。',
    lead: '每一版改了什麼。說得很少，但都是真的。',
    body: updatesBody()
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
  'about.html': aboutRedirect,
  'robots.txt': 'User-agent: *\nAllow: /\n\nSitemap: ' + SITE_URL + '/sitemap.xml\n',
  'sitemap.xml': '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
    + SITEMAP.map(f => '  <url><loc>' + esc(SITE_URL + '/' + f) + '</loc></url>').join('\n')
    + '\n</urlset>\n'
};

mkdirSync(dist, { recursive: true });
for (const [name, text] of Object.entries(out)) writeFileSync(dist + name, text);
writeTileAssets(dist + '/assets', Buffer.from(TILE_B64, 'base64'));
copyFileSync(fileURLToPath(new URL('../public/assets/logo-sprite.png', import.meta.url)), dist + '/assets/logo-sprite.png');
copyFileSync(fileURLToPath(new URL('../public/assets/game-thumb-daywar.png', import.meta.url)), dist + '/assets/game-thumb-daywar.png');

const htmlFiles = ['intro.html', 'guide.html', 'bosses.html', 'faq.html', 'about.html', 'contact.html', 'terms.html', 'privacy.html', 'updates.html', 'games.html'];
const built = Object.fromEntries(htmlFiles.map(name => [name, readFileSync(dist + name, 'utf8')]));
function expectIn(file, texts) {
  for (const t of texts) if (t && !built[file].includes(t)) throw new Error(file + ' is missing copy: ' + t.slice(0, 40));
}
expectIn('guide.html', stringsOf(guideSections));
expectIn('intro.html', HOME.paragraphs.slice(0, 4));
expectIn('intro.html', [ABOUT.lead, ...stringsOf(ABOUT.sections)]);
expectIn('bosses.html', stringsOf(BOSSES.sections).filter(t => !/^第 \d+ 王$/.test(t)));
expectIn('faq.html', FAQ.sections.flatMap(s => s.qa ? s.qa.flatMap(q => [q.q, ...stringsOf(q.a)]) : stringsOf(s.blocks || [])));
expectIn('contact.html', stringsOf(CONTACT.sections));
expectIn('terms.html', stringsOf(TERMS.sections));
expectIn('privacy.html', stringsOf(PRIVACY.sections));
if (built['intro.html'].includes(HOME.paragraphs[4])) throw new Error('intro.html must not include the removed nav paragraph');
if (built['guide.html'].includes('還想看細的')) throw new Error('guide.html must omit the nav-only section');
if ((built['guide.html'].match(/href="\/privacy\.html"/g) || []).length !== 2) throw new Error('privacy link should be drawer + footer only');
if (!built['guide.html'].includes('我們會保存哪些資料，請看 隱私權政策')) throw new Error('guide account sentence lost its privacy words');
if ((built['bosses.html'].match(/<article class="card"/g) || []).length !== 16) throw new Error('bosses.html should have 16 boss cards');
if (built['faq.html'].includes('<details')) throw new Error('faq.html must not collapse answers');
if ((built['faq.html'].match(/<script/g) || []).length !== 2) throw new Error('faq.html should have the FAQPage script and the drawer script');
if (!built['faq.html'].includes('application/ld+json')) throw new Error('faq.html missing FAQPage json-ld');
if (!built['games.html'].includes('即將推出') || !built['games.html'].includes('disabled')) throw new Error('games.html placeholder missing');
if (!built['about.html'].includes('url=/intro.html')) throw new Error('about.html must redirect to intro');
const sitemap = readFileSync(dist + 'sitemap.xml', 'utf8');
for (const f of ['intro.html', 'games.html']) if (!sitemap.includes('/' + f)) throw new Error('sitemap missing ' + f);
if (sitemap.includes('/about.html')) throw new Error('sitemap should not list the about redirect');
const staticHtml = Object.values(built).join('\n');
if (/adsbygoogle|pagead|adBreak/.test(staticHtml)) throw new Error('static pages must not load ads');
if (/<details/.test(staticHtml)) throw new Error('static pages must not collapse copy');
if (/ground\.png|wall\.png/.test(staticHtml)) throw new Error('static pages must not use the brick texture');

const indexHtml = readFileSync(dist + 'index.html', 'utf8');
if (indexHtml.includes(HOME.paragraphs[4])) throw new Error('home must not render the removed nav paragraph');
for (const href of ['/intro.html', '/guide.html', '/bosses.html', '/faq.html', '/games.html', '/updates.html', '/contact.html', '/terms.html', '/privacy.html']) {
  if (!indexHtml.includes('href="' + href + '"')) throw new Error('dist/index.html missing ' + href);
}
const fontLinks = indexHtml.match(/fonts\.googleapis\.com/g) || [];
if (fontLinks.length !== 1) throw new Error('index.html should load Google Fonts once, found ' + fontLinks.length);

console.log('build-pages: wrote ' + Object.keys(out).join(', ') + ', assets (SITE_URL=' + SITE_URL + ')');
