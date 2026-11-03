// 開發者日誌頁 (specs/ui/開發者日誌頁.md, D40). Static list from content/changelog.js, no API call; entry = home footer link.
import { PageHeader } from '../components/ui/Common.jsx';
import { CHANGELOG } from '../content/changelog.js';

export default function ChangelogPage({ onBack }) {
  return (
    <div className="page menu enter">
      <PageHeader title="開發者日誌" onBack={onBack} />
      <main className="content scroll">
        <article className="panel policy changelog">
          {CHANGELOG.map(v => (
            <section key={v.version} aria-labelledby={'cl-' + v.version}>
              <h2 id={'cl-' + v.version}>v{v.version}</h2>
              <p className="cl-date"><time dateTime={v.date}>{v.date}</time></p>
              <ul>{v.items.map(t => <li key={t}>{t}</li>)}</ul>
            </section>
          ))}
        </article>
      </main>
    </div>
  );
}
