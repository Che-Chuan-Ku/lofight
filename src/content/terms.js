// 服務條款. Rendered to dist/terms.html. Only statements already supported by the game,
// the guide, and the privacy policy. No court, company legal name, refunds, or cash sales.
import { LINKTREE_URL } from '../config.js';
import { ADS_SETTINGS_URL, GOOGLE_ADS_POLICY_URL } from './privacy.js';

export const TERMS = {
  title: '服務條款',
  description: 'DayWar 服務條款：免費遊玩、Google 登入、金幣與道具沒有現金價值、暱稱、廣告、服務中斷與免責，以及聯絡方式。',
  lead: '下面這些，都是遊戲現在已經在做的事。',
  updated: '2026-11-06',
  sections: [
    {
      id: 'free',
      h: '免費遊玩',
      blocks: [
        'DayWar 打開瀏覽器就能玩，不用下載，也不用付錢。',
        '遊戲裡沒有用現金購買金幣或道具的入口。'
      ]
    },
    {
      id: 'login',
      h: '登入',
      blocks: [
        '不登入也能玩。進度、金幣與道具留在這台裝置的瀏覽器裡。清除瀏覽器資料或換裝置，這些進度找不回來。',
        '使用 Google 帳號登入後，進度、金幣與道具跟著帳號走，換手機、換電腦都接得上。',
        '登入時保存的是帳號識別、暱稱與遊戲進度，用來辨識帳號、在不同裝置保存進度與金幣，以及在排行榜顯示暱稱與成績。',
        '排行榜只顯示你在遊戲中設定的暱稱，不顯示你的 Google 名稱或電子郵件。',
        ['細節見 ', { text: '隱私權政策', href: '/privacy.html' }, '。']
      ]
    },
    {
      id: 'coins',
      h: '金幣與道具',
      blocks: [
        '金幣從魔王身上掉。每局結束時，你打倒的魔王會換成金幣入帳，輸了也照算。',
        '商店用金幣購買道具。道具要解鎖 EXTRA 之後才能購買。',
        '廣告服務核准後，商店會出現看廣告領金幣。看完才入帳。核准之前沒有這個選項。',
        '金幣與道具只存在遊戲裡，不能換成現金，也沒有現金價值。'
      ]
    },
    {
      id: 'nick',
      h: '暱稱',
      blocks: [
        '暱稱在首頁修改。排行榜顯示的是這個暱稱。',
        '暱稱不能空白，最多 12 個字，也不能含有不能使用的字。'
      ]
    },
    {
      id: 'board',
      h: '成績與排行榜',
      blocks: [
        'NORMAL 與 EXTRA 各有一個排行榜。通關的比誰快，沒通關的比誰撐得久。',
        '送上來的成績若對不上這一局實際發生的事，這一局不會入帳，也不會寫進排行榜。',
        '排行榜以賽季為單位。賽季結束時，會依兩個榜的名次發放金幣給已登入的玩家。未登入的成績一樣會上榜，賽季獎勵只發給登入玩家。'
      ]
    },
    {
      id: 'ads',
      h: '廣告',
      blocks: [
        '本遊戲使用 Google 的廣告服務。',
        'Google 等第三方供應商會使用廣告 Cookie，依你過去造訪本站或其他網站的紀錄，向你顯示廣告。',
        ['你可以到 ', { text: 'Google 廣告設定', href: ADS_SETTINGS_URL }, ' 關閉個人化廣告。Google 如何使用這些資料，見 ', { text: 'Google 廣告相關政策', href: GOOGLE_ADS_POLICY_URL }, '。'],
        '未登入而觀看廣告領取金幣時，我們會記錄這台裝置的識別碼，只用於稽核。'
      ]
    },
    {
      id: 'changes',
      h: '服務調整',
      blocks: [
        ['遊戲會更新。改了什麼記在 ', { text: '開發者日誌', href: '/updates.html' }, '。'],
        '你打開遊戲時看到的內容與規則，就是當時有效的版本。'
      ]
    },
    {
      id: 'stop',
      h: '免責與中斷',
      blocks: [
        '本服務依現況提供。不保證不中斷，也不保證隨時可以開啟。',
        '我們可能因維護或其他原因暫停、調整或停止服務。',
        '服務停止時，金幣、道具和其他虛擬物品不另作補償。',
        ['想關閉帳號，只能透過 ', { text: '聯絡頁', href: '/contact.html' }, ' 提出。遊戲裡沒有自行刪除帳號的功能。'],
        '在法律允許的範圍內，對於因使用本服務而生的損害，我們不負賠償責任。'
      ]
    },
    {
      id: 'reach',
      h: '聯絡',
      blocks: [
        ['對本條款有疑問，請看 ', { text: '聯絡頁', href: '/contact.html' }, '，或透過 ', { text: 'Linktree', href: LINKTREE_URL }, ' 與我們聯絡。']
      ]
    }
  ]
};
