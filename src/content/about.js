// 關於 data (specs/ui/內容頁.md, D41). Rendered to dist/about.html by scripts/build-pages.mjs.
// No personal data beyond the public Linktree; block format as in guide.js.
import { LINKTREE_URL } from '../config.js';

export const ABOUT = {
  title: '關於 DayWar',
  description: 'DayWar 是個人開發的 HTML5 塔防遊戲，打開瀏覽器就能玩。素材來源與聯絡方式。',
  lead: '一個人開發，一個人維護。所以遊戲出問題時，責任歸屬非常明確。',
  sections: [
    {
      h: '這是什麼？',
      blocks: [
        'DayWar 是一款個人開發的 HTML5 塔防遊戲，支援手機與電腦瀏覽器，無需下載或安裝，打開網頁即可遊玩。',
        '你負責操控砲台，士兵負責衝鋒，魔王負責讓一切從頭開始。各司其職。',
        ['遊戲將持續更新，版本調整與新增內容都會記錄在', { text: '開發者日誌', href: '/updates.html' }, '中。畢竟，改了什麼還是得交代一下。'],
        ['怎麼玩見 ', { text: '遊戲指南', href: '/guide.html' }, '，魔王見 ', { text: '魔王攻略', href: '/bosses.html' }, '。']
      ]
    },
    {
      h: '素材來源',
      blocks: [
        ['角色圖塊採用 ', { text: 'Kenney - Tiny Dungeon', href: 'https://kenney.nl/assets/tiny-dungeon' }, '，以 CC0 授權釋出。'],
        '感謝 Kenney，讓這個像素世界不必從第一個方塊開始。'
      ]
    },
    {
      h: '聯絡方式',
      blocks: [
        ['有問題、發現錯誤，或只是有些想法，都歡迎透過 ', { text: 'Linktree', href: LINKTREE_URL }, ' 聯絡我。'],
        '訊息會看，也會回覆。至於速度，就看當時有沒有在修 Bug。'
      ]
    }
  ]
};
