# lofight
那些被廣告氣死的遊戲我做給你玩

## 人海攻城

HTML5 canvas 遊戲，React + Vite。砲台射出士兵，穿過倍率門、打破道具門，沿山路攻向紅軍與魔王。

### 開發

```
npm install
npm run dev      # 本機開發
npm run build    # 輸出 dist/index.html（單檔）
```

### 部署

Render 靜態網站，設定在 `render.yaml`（Blueprint）。推到 `main` 就會自動 build 與部署。

### 排行榜（Google Sheet）

排行榜資料存在你自己的 Google Sheet，透過 Apps Script 當 API，不需要伺服器。

1. 建一個新的 Google Sheet。
2. 選單「擴充功能 → Apps Script」，把 `tools/leaderboard.gs` 全部貼進去取代預設內容，存檔。
3. 右上「部署 → 新增部署作業」，類型選「網頁應用程式」，「執行身分：我」、「誰可以存取：所有人」，按部署並授權。
4. 複製產生的網頁應用程式網址（`https://script.google.com/macros/s/.../exec`）。
5. 在 Render 的 lofight 服務 → Environment 新增 `VITE_LEADERBOARD_URL` = 那個網址，存檔後 Render 會自動重新 build。

沒設定網址時遊戲不會顯示排行榜。每個版本（`package.json` 的 `version`）各自一份榜單，改版號就等於重置；舊資料仍留在試算表。
排名規則：通關者在前（秒數少者優先），未通關者在後（撐越久越前），同一暱稱只留最佳成績。

### 素材

角色圖塊：Kenney "Tiny Dungeon"（CC0）。
