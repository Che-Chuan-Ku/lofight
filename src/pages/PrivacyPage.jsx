// 隱私權政策頁 (specs/ui/隱私權政策頁.md; prototype privacy()). Static text, no API call; entry = home footer link.
// The text lives in content/privacy.js, shared with the crawlable dist/privacy.html (D41).
import { Fragment } from 'react';
import { PageHeader } from '../components/ui/Common.jsx';
import { PRIVACY } from '../content/privacy.js';

function External({ href, children }) {
  return <a href={href} target="_blank" rel="noopener noreferrer">{children}<span className="vh">（在新分頁開啟）</span></a>;
}

// a paragraph / list item: plain string or an array of strings and { text, href } links
const Inline = ({ v }) => (Array.isArray(v) ? v : [v]).map((s, i) =>
  typeof s === 'string' ? s : <External key={i} href={s.href}>{s.text}</External>);

function Block({ b }) {
  if (b && b.ul) return <ul>{b.ul.map((li, i) => <li key={i}><Inline v={li} /></li>)}</ul>;
  return <p><Inline v={b} /></p>;
}

export default function PrivacyPage({ onBack }) {
  return (
    <div className="page menu enter">
      <PageHeader title={PRIVACY.title} onBack={onBack} />
      <main className="content scroll">
        <article className="panel policy">
          {PRIVACY.sections.map(s => (
            <Fragment key={s.h}>
              <h2>{s.h}</h2>
              {s.blocks.map((b, i) => <Block key={i} b={b} />)}
            </Fragment>
          ))}
          <p className="updated">最後更新：<time dateTime={PRIVACY.updated}>{PRIVACY.updated}</time></p>
        </article>
      </main>
    </div>
  );
}
