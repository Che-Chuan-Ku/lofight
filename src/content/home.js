// Homepage publisher copy (specs/ui/內容頁.md, D41). Shown under the stage on the React home,
// and mirrored in index.html #intro for visitors without JS. Keep the two in sync: build-pages checks it.
// No numbers (HP, seconds, coins, cooldowns, odds), no system mechanics, no forbidden tier words.
export const HOME = {
  paragraphs: [
    'DayWar 是一款打開瀏覽器就能玩的塔防遊戲。你守在山路底下，左右拖曳砲台，砲台自己會射出藍軍士兵。士兵沿著山路往上衝：穿過倍率門會變多，打破道具門會變強，問號門則要你當場挑一張卡。路的盡頭是迎面而來的紅軍，以及一個接一個的魔王。',
    '目標很單純，把魔王全部打倒。紅軍或魔王摸到砲台，這局就結束。射擊不用你按，你只負責站位。魔王出招的時候把砲台挪開；站著不動，不叫勇敢。',
    '遊戲有兩種階層。NORMAL 一進來就能玩，一路打到第 9 王。先打倒那一隻，EXTRA 才會開門：後面的魔王會出招，開局前可以從庫存裡挑一個道具帶進場，也可以空手。兩種階層各有自己的排行榜，成績分開算。',
    '打倒魔王會掉金幣，每局結束入帳，輸了也照算。商店用金幣換道具，道具要解鎖 EXTRA 之後才買得到。不登入也能玩，進度留在這台裝置的瀏覽器裡；用 Google 帳號登入之後，進度、金幣與道具跟著帳號走，換手機、換電腦都接得上。',
    '每一隻魔王怎麼出招、該往哪讓，寫在魔王攻略。操作、門、道具與賽季寫在遊戲指南。存檔、登入、金幣與暱稱，見常見問題。這個遊戲是誰做的、每次改了什麼、我們保存哪些資料，連結都在下面。'
  ],
  // Same order as the static-page footer (scripts/build-pages.mjs PAGES).
  links: [
    { href: '/guide.html', text: '遊戲指南' },
    { href: '/bosses.html', text: '魔王攻略' },
    { href: '/faq.html', text: '常見問題' },
    { href: '/about.html', text: '關於' },
    { href: '/updates.html', text: '開發者日誌' },
    { href: '/privacy.html', text: '隱私權政策' },
    { href: '/contact.html', text: '聯絡我們' },
    { href: '/terms.html', text: '服務條款' }
  ]
};
