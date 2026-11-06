// 聯絡頁. Rendered to dist/contact.html. No email: the only channel is LINKTREE_URL (src/config.js).
// Tone matches about.js. Block format as in guide.js.
import { LINKTREE_URL } from '../config.js';

export const CONTACT = {
  title: '聯絡我們',
  description: '聯絡 DayWar：回報錯誤、提供建議、詢問帳號或隱私。目前只透過 Linktree，沒有電子郵件。',
  lead: '一個人收訊息。事情說清楚，我就回得準。',
  sections: [
    {
      h: '怎麼聯絡',
      blocks: [
        '目前沒有電子郵件。',
        ['有事請透過 ', { text: 'Linktree', href: LINKTREE_URL }, ' 找我。'],
        '訊息會看，也會回覆。至於速度，就看當時有沒有在修 Bug。'
      ]
    },
    {
      h: '可以聯絡什麼',
      blocks: [
        { ul: [
          '遊戲錯誤：畫面、操作或結算不對勁。',
          '建議：你覺得哪裡可以更好。',
          '帳號：登入、進度、金幣或道具對不上。',
          '隱私：對隱私權政策有疑問。'
        ] }
      ]
    },
    {
      h: '相關頁面',
      blocks: [
        ['我們保存哪些資料，見 ', { text: '隱私權政策', href: '/privacy.html' }, '。'],
        ['遊玩時已經在做的事，見 ', { text: '服務條款', href: '/terms.html' }, '。'],
        ['這個遊戲是誰做的，見 ', { text: '關於', href: '/about.html' }, '。']
      ]
    }
  ]
};
