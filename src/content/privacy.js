// 隱私權政策 data (specs/ui/隱私權政策頁.md, specs/ui/內容頁.md D41). Shared by the in-game page (src/pages/PrivacyPage.jsx)
// and dist/privacy.html (scripts/build-pages.mjs) so both always say the same thing. No jokes on this page.
// Contact is the Linktree link (no email given; _decisions-log change_round 2026-11-02 item 5). Block format as in guide.js.
import { LINKTREE_URL } from '../config.js';

export const ADS_SETTINGS_URL = 'https://adssettings.google.com/';
export const GOOGLE_ADS_POLICY_URL = 'https://policies.google.com/technologies/ads';

export const PRIVACY = {
  title: '隱私權政策',
  description: 'DayWar 隱私權政策：Google 廣告與 Cookie、如何關閉個人化廣告、使用 Google 登入時保存的資料與用途、未登入時的裝置識別碼。',
  updated: '2026-11-04',
  sections: [
    {
      h: '適用範圍',
      blocks: [
        '本政策說明你遊玩 DayWar 時，我們與我們使用的第三方服務會收集哪些資料、如何使用，以及你可以怎麼選擇。'
      ]
    },
    {
      h: '廣告與 Cookie',
      blocks: [
        '本遊戲使用 Google 的廣告服務。',
        { ul: [
          'Google 等第三方供應商會使用廣告 Cookie，依你過去造訪本站或其他網站的紀錄，向你顯示廣告。',
          ['你可以到 ', { text: 'Google 廣告設定', href: ADS_SETTINGS_URL }, ' 關閉個人化廣告。'],
          ['Google 如何使用這些資料，請見 ', { text: 'Google 廣告相關政策', href: GOOGLE_ADS_POLICY_URL }, '。']
        ] }
      ]
    },
    {
      h: '使用 Google 登入時',
      blocks: [
        '我們會保存：',
        { ul: ['帳號識別', '暱稱', '遊戲進度'] },
        '用途：',
        { ul: [
          '辨識你的帳號。',
          '在不同裝置保存進度與金幣。',
          '在排行榜顯示你的暱稱與成績。'
        ] },
        '排行榜只顯示你在遊戲中設定的暱稱，不顯示你的 Google 名稱或電子郵件。'
      ]
    },
    {
      h: '未登入時',
      blocks: [
        { ul: [
          '遊戲會在這台裝置產生一組識別碼，用來保存你的進度。',
          '清除瀏覽器資料或換裝置後，這些進度將無法找回。',
          '觀看廣告領取金幣時，我們也會記錄這組識別碼，只用於稽核。'
        ] }
      ]
    },
    {
      h: '聯絡我們',
      blocks: [
        ['對本政策有疑問，請透過 ', { text: 'Linktree', href: LINKTREE_URL }, ' 與我們聯絡。']
      ]
    }
  ]
};
