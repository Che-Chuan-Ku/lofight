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

### v19 後端（帳號、金幣、商店、排行榜）

排行榜與帳號改走 v19 後端 API（`/api/csg/v1`，契約 `specs/api.yml`，後端在 `backend/`），不再使用 Google Sheet。

```
npm run dev                # 本機開發；預設用 MSW 假後端（src/mocks），不需要啟動後端
VITE_API_MOCK=false npm run dev   # 改連真後端：/api 轉送到 API_PROXY_TARGET（預設 http://localhost:8080）
npm run test:contract      # api.yml ↔ 前端 API 層／zod／MSW 機械比對
npm run test:e2e           # Playwright：MSW 流程（port 5199）＋ build 產物可開始遊玩（port 4199）
npm run test:e2e:backend   # Playwright 對真後端（需先在 E2E_BACKEND_URL，預設 http://localhost:18080 起好 API）
```

設定見 `.env.example`：`VITE_API_BASE_URL`（預設 `/api/csg/v1`，後端沒開 CORS，需同源或反向代理）、`VITE_GOOGLE_CLIENT_ID`（空白時不顯示登入入口）。
後端連不上時 bronze 仍可離線遊玩，結算畫面會說明這局沒有保存。

#### Google 登入設定

- 登入按鈕只在 `VITE_GOOGLE_CLIENT_ID` 有值時出現；沒填就只有訪客模式。
- 前端：把 client id 寫在 `.env.local`（`VITE_GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com`，改完重啟 `npm run dev`）。
- 後端：docker compose 讀環境變數 `GOOGLE_CLIENT_ID`（`backend/docker-compose.yml`），例如 `GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com docker compose up -d`。**前後端兩個值必須相同。**
- Google Cloud Console →「API 和服務」→「憑證」→ 建立 OAuth 用戶端 ID，類型選「網頁應用程式」，「已授權的 JavaScript 來源」加入 `http://localhost:5174`（正式環境再加部署網域）。

### 素材

角色圖塊：Kenney "Tiny Dungeon"（CC0）。
