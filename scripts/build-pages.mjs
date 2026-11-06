// D41 (specs/ui/內容頁.md): crawlable static pages next to the single-file game build.
// Runs after `vite build` (package.json "build") and writes into dist/:
//   guide.html, bosses.html, faq.html, about.html, updates.html, privacy.html  (plain HTML, no JS, no ad script)
//   robots.txt, sitemap.xml                              (absolute URLs from SITE_URL)
// Copy comes from src/content/*.js, the same data the in-game pages use.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

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

const SITE_URL = (process.env.SITE_URL || 'https://daywar.net').replace(/\/+$/, '');
const SITE = 'DayWar';

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// Footer / cross links, in the same order as the in-game home footer.
const PAGES = [
  { file: 'guide.html', label: '遊戲指南' },
  { file: 'bosses.html', label: '魔王攻略' },
  { file: 'faq.html', label: '常見問題' },
  { file: 'about.html', label: '關於' },
  { file: 'updates.html', label: '開發者日誌' },
  { file: 'privacy.html', label: '隱私權政策' }
];

// Inline value: a string or an array of strings and { text, href } links. Site-relative links stay in this tab.
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
function faqBody(sections) {
  return sections.map(s => {
    const inner = s.qa
      ? s.qa.map(item => '<section>\n<h3>' + esc(item.q) + '</h3>\n' + item.a.map(block).join('\n') + '\n</section>').join('\n')
      : (s.blocks || []).map(block).join('\n');
    return '<section>\n<h2>' + esc(s.h) + '</h2>\n' + inner + '\n</section>';
  }).join('\n');
}
function faqJsonLd(sections) {
  const mainEntity = [];
  for (const s of sections) {
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
const sections = list => list.map(s => '<section>\n<h2>' + esc(s.h) + '</h2>\n' + s.blocks.map(block).join('\n') + '\n</section>').join('\n');

const CSS = `
:root { color-scheme: dark; --bg: #14171f; --panel: #1d2230; --text: #eef1f7; --muted: #a9b1c3; --line: #2e3547; --link: #8fd3ff; }
* { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; }
body { margin: 0; background: var(--bg); color: var(--text);
  font: 16px/1.75 "Noto Sans TC", -apple-system, BlinkMacSystemFont, "PingFang TC", "Microsoft JhengHei", "Heiti TC", system-ui, sans-serif; }
a { color: var(--link); font-weight: 700; text-underline-offset: 3px; }
.wrap { max-width: 720px; margin: 0 auto; padding: 16px 16px 32px; }
.top { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 44px; }
.top .back { display: inline-flex; align-items: center; min-height: 44px; }
.top .brand { color: var(--muted); font-weight: 700; letter-spacing: 1px; }
h1 { margin: 16px 0 8px; font-size: 28px; line-height: 1.3; }
.lead { margin: 0 0 8px; color: var(--muted); }
main { background: var(--panel); border: 1px solid var(--line); border-radius: 12px; padding: 4px 16px 16px; margin-top: 16px; }
h2 { margin: 20px 0 6px; font-size: 19px; line-height: 1.4; }
h3 { margin: 16px 0 6px; font-size: 17px; line-height: 1.4; }
p { margin: 0 0 8px; }
ul { margin: 0 0 8px; padding-left: 1.3em; }
li + li { margin-top: 4px; }
.date { color: var(--muted); font-size: 14px; margin: 0 0 4px; }
.updated { color: var(--muted); font-size: 14px; margin-top: 16px; }
footer { margin-top: 24px; text-align: center; color: var(--muted); font-size: 14px; }
footer nav { display: flex; flex-wrap: wrap; justify-content: center; gap: 0 4px; }
footer nav a, footer nav span { display: inline-flex; align-items: center; min-height: 44px; padding: 0 8px; }
footer nav span { color: var(--text); font-weight: 700; }
footer p { margin: 8px 0 0; }
`.trim();

function page({ file, title, description, lead, body, jsonLd }) {
  const nav = [{ file: '', label: '回到遊戲', href: '/' }, ...PAGES].map(p => {
    if (p.file === file) return '<span aria-current="page">' + esc(p.label) + '</span>';
    return '<a href="' + (p.href || '/' + p.file) + '">' + esc(p.label) + '</a>';
  }).join('\n');
  return `<!doctype html>
<html lang="zh-Hant">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}｜${SITE}</title>
<meta name="description" content="${esc(description)}">
<link rel="canonical" href="${esc(SITE_URL + '/' + file)}">
${jsonLd ? '<script type="application/ld+json">' + JSON.stringify(jsonLd).replace(/</g, '\\u003c') + '</script>' : ''}
<style>
${CSS}
</style>
</head>
<body>
<div class="wrap">
<header class="top"><a class="back" href="/">← 返回遊戲</a><span class="brand">${SITE}</span></header>
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
  'bosses.html': page({ file: 'bosses.html', title: BOSSES.title, description: BOSSES.description, lead: BOSSES.lead, body: sections(BOSSES.sections) }),
  'faq.html': page({ file: 'faq.html', title: FAQ.title, description: FAQ.description, lead: FAQ.lead, body: faqBody(FAQ.sections), jsonLd: faqJsonLd(FAQ.sections) }),
  'about.html': page({ file: 'about.html', title: ABOUT.title, description: ABOUT.description, lead: ABOUT.lead, body: sections(ABOUT.sections) }),
  'updates.html': page({
    file: 'updates.html', title: '開發者日誌',
    description: 'DayWar 開發者日誌：每個版本改了什麼，以玩家看得懂的話寫成。',
    lead: '每一版改了什麼。說得很少，但都是真的。',
    // every entry (the in-game page caps at 20 lines, the archive does not)
    body: CHANGELOG.map(v => '<section>\n<h2>v' + esc(v.version) + '</h2>\n<p class="date"><time datetime="' + esc(v.date) + '">' + esc(v.date) + '</time></p>\n<ul>' + v.items.map(t => '<li>' + esc(t) + '</li>').join('') + '</ul>\n</section>').join('\n')
  }),
  'privacy.html': page({
    file: 'privacy.html', title: PRIVACY.title, description: PRIVACY.description,
    body: sections(PRIVACY.sections) + '\n<p class="updated">最後更新：<time datetime="' + esc(PRIVACY.updated) + '">' + esc(PRIVACY.updated) + '</time></p>'
  }),
  'robots.txt': 'User-agent: *\nAllow: /\n\nSitemap: ' + SITE_URL + '/sitemap.xml\n',
  'sitemap.xml': '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
    + ['', ...PAGES.map(p => p.file)].map(f => '  <url><loc>' + esc(SITE_URL + '/' + f) + '</loc></url>').join('\n')
    + '\n</urlset>\n'
};

mkdirSync(dist, { recursive: true });
for (const [name, text] of Object.entries(out)) writeFileSync(dist + name, text);

// index.html #intro is the no-JS copy of the React home blurb. Vite has already emitted dist/index.html.
const indexHtml = readFileSync(dist + 'index.html', 'utf8');
for (const p of HOME.paragraphs) {
  if (!indexHtml.includes(p)) throw new Error('dist/index.html #intro is out of sync with src/content/home.js');
}
for (const l of HOME.links) {
  if (!indexHtml.includes('href="' + l.href + '"')) throw new Error('dist/index.html #intro missing ' + l.href);
}

console.log('build-pages: wrote ' + Object.keys(out).join(', ') + ' (SITE_URL=' + SITE_URL + ')');
