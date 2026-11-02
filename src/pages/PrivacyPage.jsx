// 隱私權政策頁 (specs/ui/隱私權政策頁.md; prototype privacy()). Static text, no API call; entry = home footer link.
// Contact is the Linktree link (no email given; _decisions-log change_round 2026-11-02 item 5).
import { PageHeader } from '../components/ui/Common.jsx';
import { LINKTREE_URL } from '../config.js';

const ADS_SETTINGS_URL = 'https://adssettings.google.com/';

function External({ href, children }) {
  return <a href={href} target="_blank" rel="noopener noreferrer">{children}<span className="vh">（在新分頁開啟）</span></a>;
}

export default function PrivacyPage({ onBack }) {
  return (
    <div className="page menu enter">
      <PageHeader title="隱私權政策" onBack={onBack} />
      <main className="content scroll">
        <article className="panel policy">
          <h2>廣告與 Cookie</h2>
          <p>本遊戲使用 Google 的廣告服務。Google 與第三方供應商會使用 Cookie，依你過去造訪本站或其他網站的紀錄顯示廣告。</p>
          <p>你可以到 <External href={ADS_SETTINGS_URL}>Google 廣告設定</External> 關閉個人化廣告。</p>
          <h2>使用 Google 登入時</h2>
          <p>我們會保存：</p>
          <ul><li>帳號識別</li><li>暱稱</li><li>遊戲進度</li></ul>
          <p>用來辨識你的帳號、在不同裝置保存進度與金幣，以及在排行榜顯示你的暱稱與成績。</p>
          <h2>未登入時</h2>
          <p>遊戲會在這台裝置產生一組識別碼，用來保存你的進度。清除瀏覽器資料或換裝置後，這些進度將無法找回。</p>
          <p>觀看廣告領取金幣時，我們也會記錄這組識別碼，只用於稽核。</p>
          <h2>聯絡我們</h2>
          <p>對本政策有疑問，請透過 <External href={LINKTREE_URL}>Linktree</External> 與我們聯絡。</p>
        </article>
      </main>
    </div>
  );
}
