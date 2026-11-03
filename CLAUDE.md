# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DayWar（repo 名 lofight）：HTML5 canvas 塔防遊戲，React 19 + Vite 8 前端，Spring Boot 3.2 / Java 17 後端，PostgreSQL。
前後端打包成**同一個 Docker image**，部署在 Render（`render.yaml` Blueprint，push `main` 自動部署）。語言：UI 文案繁體中文，程式註解英文。

## 版號規則（每次 commit 都要改）

每個 commit 前先改版號，三處同步：`package.json`（用 `npm version X.Y.Z --no-git-tag-version`，會連 `package-lock.json` 一起改）與 `backend/pom.xml` 第 16 行 `<version>`。遊戲右下角顯示的版號來自 package.json。

**版號同時是賽季鍵**：後端從 `backend/pom.xml` 版號取「大.中」當排行榜 boardVersion（`game.build-version` 由 Maven resource filtering 填入，無法解析則啟動失敗）。升**大版本或中版本**＝開新賽季：部署後後端啟動時會自動把上一賽季（最早 19.0）兩榜前 5 名的已綁定玩家發放 1,200／800／400／200／100 金幣，**不可撤回**，升版前先確認排行榜狀態。小版本不切賽季。

每次升版同時在 `src/content/changelog.js` 新增該版摘要（首頁「開發者日誌」頁顯示；玩家視角、不寫數值與機制）。

| 位 | 何時進位 | 例子 |
|---|---|---|
| 大版本 X | 系統性更換：風格、遊戲玩法、遊戲模式 | 階層改制、整體視覺換風格 |
| 中版本 Y | 影響核心玩法：新增魔王、魔王招數增減、新武器／道具上市 | 新 buff 道具、第 6 王加閃電球 |
| 小版本 Z | 零碎、不嚴重影響玩家秒數的優化：攻擊特效、機制小調整、UI 修改 | 圖示更換、文案、預警底色 |

## 常用指令

```bash
npm run dev                          # Vite，預設 MSW 假後端（src/mocks），不需後端
VITE_API_MOCK=false npm run dev      # 連真後端：/api 代理到 API_PROXY_TARGET（預設 http://localhost:8080）
npm run build                        # 單檔 dist/index.html（vite-plugin-singlefile）
npm run test:contract                # api.yml ↔ src/lib/api + zod + MSW 機械比對（欄位/必填/nullable/enum/錯誤碼）
npm run test:e2e                     # Playwright：mock 專案（port 5199）＋ prod 專案（build 後 4199，無後端可開局）
npx playwright test tests/e2e/mock/economy.spec.js --project=mock   # 單一 spec
npx playwright test --project=mock -g "閃電球"                        # 依標題過濾
npm run test:e2e:backend             # 對真後端（E2E_BACKEND_URL，預設 http://localhost:18080）

cd backend && docker compose up -d --build        # 本機後端 + Postgres（port 8080/5432），image 內含前端
backend/mvnw -q clean test -Dtest=RunCucumberTest -Dcucumber.filter.tags="not @ignore"   # 後端 e2e（Testcontainers，需 docker）
bash integration-test/run.sh         # 全端整合測試：起獨立 docker stack，Playwright 打真後端，證據到 integration-test/evidence/$IT_DATE
```

`backend/.env` 與 `.env.local` 放本機的 `GOOGLE_CLIENT_ID` / `VITE_GOOGLE_CLIENT_ID`、`ADSENSE_PUBLISHER_ID` / `VITE_ADSENSE_PUBLISHER_ID`，都已 gitignore。用 `>>` 追加前確認檔尾有換行，否則兩個變數會黏成一行。

## 架構要點

**單一服務部署**：`backend/Dockerfile`（build context = repo 根目錄，`.dockerignore` 白名單）先用 Node 階段 `npm run build`，再 Maven 打 jar；Spring Boot 同時提供 `/`（頁面）與 `/api/csg/v1`。`backend/docker-entrypoint.sh` 啟動時把 `GOOGLE_CLIENT_ID`、`ADSENSE_PUBLISHER_ID` 寫進 `index.html` 的 `<meta>`、把 Render 的 `DATABASE_URL`/`PORT` 轉成 Spring 變數、產生 `/ads.txt`。所以**換 ID 只要重新部署，不用重 build**。`ADSENSE_PUBLISHER_ID` 留空＝廣告功能關閉（無 script、商店無卡片、API 回 eco003）。前端 `src/lib/env.js` 先讀 meta，dev 才退回 `VITE_*`。

**前端分層**（`src/`）
- `game/`：純引擎，無 React。`constants.js`（BOSS_PLAN、EXTRA_PLAN、STORM、SECRET_BOSS、LASER 等所有數值）、`engine.js`（狀態 `S`、update loop、門/魔王/技能/結算摘要）、`render.js`、`abilities.js`（門與「?」選項）、`tiles.js`/`path.js`。
- `hooks/useGame.js`：把引擎接到 React；`?debug` 且非 production 時掛 `window.__game`（`secret()`、`extra()`、`storm()`、`state()` 等鉤子，測試靠它快轉），production build 會剔除。
- `App.jsx`：畫面狀態機（home/login/loadout/game/result/shop/wallet/leaderboard/privacy），對局結束後呼叫 `settle`。
- `lib/api/`：`operations.js` 一張表同時供 API 函式、MSW 與 contract 測試；`client.js` 處理回應信封、zod 驗證（schema 不符視為錯誤並 console.error）、401 自動換發一次。`lib/types/*.schema.js` 可選欄位一律 `.nullable().optional()`（後端沒有 @JsonInclude，null 會明確序列化）。
- `lib/settlement.js`：引擎摘要 → settleRun body。隱藏第 9 關是純前端：結算永遠以 NORMAL 勝局送 bossKills 1–8、**不送 secret 欄位**；打完隱藏王送全程秒數，死於隱藏王送第 8 王秒數。
- `lib/labels.js`：API 值 BRONZE/SILVER 對應顯示名 NORMAL/EXTRA 與所有玩家可見文案；`lib/banner.js` 登入提示橫幅規則（旗標只在橫幅真的顯示後才寫）；`lib/ads.js` Ad Placement API（`adBreak` → `beforeReward` 才啟用卡片 → 點擊 `showAdFn` → `adViewed` 才 `claimAdReward`）。
- `mocks/`：MSW handlers 與 `db.js`，資料對齊 features 的 Background；`failNext(op, kind)` 可製造錯誤。

**後端**（`backend/`，package `com.crowdsiege.api`，名稱改 DayWar 時刻意未改 package）：controller → service → repository，JWT filter 只守 `/api/**`。`BuffItem` 以明確代碼存 DB（新增品項放最後、API 排序用 `DISPLAY_ORDER`）。Flyway `V1__init_schema.sql`、`V2__ad_reward.sql` 已部署，**不要改既有 migration**，新增用 V3+。測試是 Cucumber e2e：`backend/src/test/resources/features/` 是指向 `specs/features/` 的 symlink，step 類在 `steps/<domain>/<handler>/`。

**規格與流程**：`specs/`（api.yml、erm.dbml、features/、ui/、activities/、clarify/）由 SDD Skill Factory 的 aiworkflow 產出，狀態在 `specs/workflow-state.yml`；`prototype/` 是 UIUX 原型與交接文件；`frontend-state/_module-state.yml` 記前端決策 FE-xx；`integration-test/REPORT.md` 是最近一輪整合測試報告。只有 `specs/features/` 進 git（讓後端測試 symlink 可解）；其餘 specs/prototype/integration-test 目錄刻意未追蹤。改遊戲規則或契約時，把裁決寫進 `specs/clarify/` 與對應 `specs/ui/*.md`，契約改動要同步 api.yml、erm.dbml、features、後端、前端 zod，並以 `npm run test:contract` 與後端 Cucumber 驗證。

## 寫碼時的固定規則

- 玩家可見文字：階層只稱 NORMAL / EXTRA（API 值仍 BRONZE/SILVER）；不得出現「經典、特殊、基本、進階、普通、困難」等分類形容詞，不得出現「贊助、支持我們」等誘導看廣告字樣；不透露系統機制。
- 排行榜暱稱只用玩家輸入或系統預設，不用 Google 名稱或 email；畫面不顯示 email。
- build 產物不得含本機路徑、email、token；`data-adbreak-test` 只在非 production。
- 不在 repo 內建 `.bak`；不主動 commit / push（push `main` 會直接部署）。
- 後端審核與整合測試常見坑：負向測試要確保只有一個條件不成立（例如先讓「伺服器經過時間 ≥ gameSeconds」成立再測進場秒數）；DTO 改欄位後跑 contract 測試。
