# Render 部署說明（前後端單一服務）

前端（Vite 單檔 build）已併入後端 Docker image：同一個網址同時提供遊戲頁面（`/`）與 API（`/api/csg/v1/...`），不需要 rewrite、也不需要 CORS。

根目錄 `render.yaml`（Blueprint）只定義兩個資源，push `main` 時自動部署：

| 資源 | 類型 | 說明 |
|---|---|---|
| `lofight` | web service（Docker，free） | `backend/Dockerfile`，build context＝repo 根目錄（`dockerContext: .`）；`src/**`、`backend/**`、`package*.json`、`vite.config.js`、`index.html`、`.dockerignore` 變動才重建 |
| `lofight-db` | PostgreSQL 16（free） | 只允許 Render 私網連線（`ipAllowList: []`） |

## Image 怎麼組成

`backend/Dockerfile` 三階段：

1. `node:22.12.0-alpine`（與 `.node-version` 相同）：`npm ci && npm run build` → `dist/index.html`。
2. `eclipse-temurin:17-jdk`：Maven 打包 Spring Boot jar（不跑測試）。
3. `eclipse-temurin:17-jre`：放 jar 與頁面樣板 `/app/web/index.html`；`SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/static/`。

根目錄 `.dockerignore` 採白名單，只放 `package*.json`、`index.html`、`vite.config.js`、`src/`、`backend/` 進 build context；所有 `.env*`（含 `.env.local`、`backend/.env`）、`node_modules`、`dist`、`backend/target`、`backend/src/test` 一律排除。

後端行為：

- `GET /`、`/index.html` 回遊戲頁面，`Cache-Control: no-cache`（每次重新驗證，部署後立即生效）。
- JWT 檢查只作用在 `/api/**`；頁面不需要 token。
- 其他未知路徑回 404 JSON。

## 環境變數

| 變數 | 來源 | 需手動？ |
|---|---|---|
| `DATABASE_URL` | `fromDatabase.connectionString`（`postgresql://user:pass@host:port/db`） | 否 |
| `JWT_SECRET_KEY` | `generateValue: true`（隨機 256-bit base64） | 否 |
| `GOOGLE_CLIENT_ID` | `sync: false` | **是** |
| `PORT` | Render 自動注入（預設 10000） | 否 |

**只需要一個 `GOOGLE_CLIENT_ID`**：後端用它驗證 Google ID token，`docker-entrypoint.sh` 啟動時也把它寫進頁面的 `<meta name="google-client-id">`，前端從這裡讀。沒填（或含非 `[A-Za-z0-9._-]` 字元）時頁面不顯示登入入口，只有訪客模式。改值後重啟／重新部署即生效，**不需要重 build 前端**。舊的 `VITE_GOOGLE_CLIENT_ID` 已不再用於部署（只剩本機 `npm run dev` 用，見 README）。

`sync: false` 只在「第一次建立 Blueprint」時會在 Dashboard 提示填值；既有 Blueprint 同步時會被忽略（[blueprint-spec › Prompting for secret values](https://render.com/docs/blueprint-spec)）。所以在既有 Blueprint 裡新增的服務，要到 Dashboard → `lofight` → Environment 手動加 `GOOGLE_CLIENT_ID`。

`backend/docker-entrypoint.sh` 啟動時會：
- `DATABASE_URL` → `DB_URL`（`jdbc:postgresql://host:port/db`）、`DB_USERNAME`、`DB_PASSWORD`（帳密會做 %XX 解碼）
- `PORT` → `SERVER_PORT`
- `GOOGLE_CLIENT_ID` → 由 `/app/web/index.html` 產生 `/app/static/index.html`
- 已明確設定 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`SERVER_PORT` 時不覆蓋，所以本機 `docker compose up` 行為不變（仍是 8080，頁面在 <http://localhost:8080/>）。

## Render 對「從 render.yaml 移除資源」與「同名服務」的處理（官方文件）

- **移除不會刪除**：「Changes to a Blueprint never cause a resource to be deleted.」從 `render.yaml` 拿掉的資源會留在原處、變成 unmanaged，要自己到 Dashboard／CLI 刪除；反過來，若先在 Dashboard 刪掉一個仍寫在 Blueprint 裡的資源，下次 sync 會被重建。出處：[Infrastructure as Code › Deleting resources](https://render.com/docs/infrastructure-as-code)。
- **同名即套用到既有服務**：「If you add the name of an existing service to your Blueprint file, Render attempts to apply the Blueprint's configuration to that existing service.」出處：[blueprint-spec › `name`](https://render.com/docs/blueprint-spec)。
- **型別不能改**：`type`「You can't modify this value after creation」；`runtime` 可改「except for static sites」。出處：[blueprint-spec › `type` / `runtime`](https://render.com/docs/blueprint-spec)。
  → 既有靜態站 `lofight` 還在時直接 sync，Render 會嘗試把 Docker 設定套到這個靜態站上，而靜態站的 runtime 不可改，**必須先刪掉舊靜態站**。
- 建立新 Blueprint 時若名稱撞到 workspace 既有資源，Render 會給新資源加後綴（[Infrastructure as Code](https://render.com/docs/infrastructure-as-code)）。
- 欄位：`dockerfilePath`／`dockerContext` 皆相對 repo 根目錄（[blueprint-spec › Docker](https://render.com/docs/blueprint-spec)）；`buildFilter.paths`／`ignoredPaths` 為 glob、相對 repo 根目錄（[Build filters](https://render.com/docs/build-filters)、[JSON schema](https://render.com/schema/render.yaml.json)）；`fromDatabase` + `property: connectionString`、`generateValue: true`、`sync: false`（[blueprint-spec › Environment variables](https://render.com/docs/blueprint-spec)）。本檔 `render.yaml` 已用官方 JSON schema 驗證通過。

## 首次切換步驟（靜態站 + API → 單一服務）

> **網址風險**：官方文件沒有說明刪除服務後 `onrender.com` 子網域能否給新服務沿用；Render 社群論壇的工作人員回覆是「已刪除服務的 onrender.com 網址不能重用」（[community 討論](https://community.render.com/t/can-i-reuse-a-web-service-url-for-a-static-site/38072)、[Reusing Old URL](https://community.render.com/t/reusing-old-url/26327)）。因此新的 `lofight` 很可能拿到 `lofight-xxxx.onrender.com`。要固定網址請改用自訂網域（Settings → Custom Domains）。

1. 確認 `render.yaml` 的 `region`（目前 `singapore`）；web service 與 DB 必須同區才能用私網連線，**建立後不可改**。
2. **先刪舊靜態站**：Render Dashboard → `lofight`（static site）→ Settings → Delete。此時還沒 push 新的 `render.yaml`，所以不會有 sync 把它重建。
3. 把新的 `render.yaml`、`.dockerignore`、`backend/`、前端改動推上 `main`。
4. Dashboard → Blueprints → 此 repo：若有 auto sync 會自動套用，否則按 **Sync**（預覽應顯示「新增 web service `lofight`（docker）」、`lofight-db` 不變）。若改成 New → Blueprint 重新建立，則會提示填 `GOOGLE_CLIENT_ID`。
5. Dashboard → `lofight` → Environment 加 `GOOGLE_CLIENT_ID`（值＝Google OAuth 用戶端 ID）→ Save（會重新部署）。
6. 若先前已建立 `lofight-api`：它已從 `render.yaml` 移除、變成 unmanaged，確認新服務正常後到 Dashboard 手動刪除。`lofight-db` 仍由 Blueprint 管理，新服務經 `fromDatabase` 連同一顆 DB，資料不受影響。
7. 看 `lofight` 實際網址，把它（與自訂網域，如果有）加入 Google Cloud Console OAuth client 的 Authorized JavaScript origins。
8. 驗證：
   - `curl -i https://<網址>/` → 200、HTML 含「人海攻城」、`Cache-Control: no-cache`、`<meta name="google-client-id" content="....apps.googleusercontent.com">`
   - `curl -i https://<網址>/api/csg/v1/leaderboard` → 401 JSON（`未帶 Token`）

## 本機建置與驗證

```bash
# 在 repo 根目錄（build context 是根目錄）
docker build -f backend/Dockerfile -t lofight:local .
# 或沿用 compose（backend/docker-compose.yml 的 build context 已指到 ..）
cd backend && GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com docker compose up -d --build
```

## 免費方案注意事項

- **整站休眠**：頁面與 API 是同一個 free web service，15 分鐘無流量即休眠，下一個請求（包含打開首頁）要等約 1 分鐘喚醒；喚醒後頁面與 API 同時可用，不再有「頁面秒開但 API 逾時」的情形。free = 0.1 CPU / 512 MB，Spring Boot 冷啟動比本機慢很多。（[Free instances](https://render.com/docs/free)）
- **Postgres 30 天到期**：建立 30 天後失效，14 天寬限期內未升級會連同資料刪除。每個 workspace 只能有 1 個免費 DB，容量 1 GB。
- 每月 750 free instance hours；檔案系統是暫時的（重啟即消失；`/app/static/index.html` 每次啟動重新產生，不受影響）。
- 無 HTTP health check（未設 `healthCheckPath`），Render 用預設 TCP port 探測（[Health checks](https://render.com/docs/health-checks)）。

參考：<https://render.com/docs/blueprint-spec>、<https://render.com/docs/infrastructure-as-code>、<https://render.com/docs/build-filters>、<https://render.com/docs/free>、<https://render.com/docs/web-services#port-binding>、<https://render.com/docs/health-checks>

## 廣告功能開關（看廣告 +200）

- 開關就是 `ADSENSE_PUBLISHER_ID`：**留空＝關閉**（render.yaml 預設 `value: ""`）。關閉時頁面不載入 Google 廣告 script、商店不顯示看廣告卡片、`claimAdReward` 回 eco003、`/ads.txt` 回 404，其餘功能不受影響。
- 要開啟：AdSense 與 H5 Games Ads 核准後，到 Dashboard → `lofight` → Environment 把 `ADSENSE_PUBLISHER_ID` 填成 `ca-pub-<數字>` → Save（自動重新部署，不需重 build）。啟動腳本會把 ID 注入頁面並產生 `/ads.txt`（`google.com, pub-<數字>, DIRECT, f08c47fec0942fa0`）。
- 要暫停廣告：把值清空再 Save 即可。
- 每日次數（5）、冷卻（60 秒）、金幣（200）在 `backend/src/main/resources/application.yml` 的 `ad-reward` 區段。
