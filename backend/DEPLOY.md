# Render 部署說明（後端 API）

根目錄 `render.yaml`（Blueprint）定義三個資源，push `main` 時自動部署：

| 資源 | 類型 | 說明 |
|---|---|---|
| `lofight` | static site | 前端單檔 build（`dist/`），`/api/*` rewrite 到後端 |
| `lofight-api` | web service（Docker，free） | `backend/Dockerfile`，context `backend/`；只有 `backend/**` 變動才重建 |
| `lofight-db` | PostgreSQL 16（free） | 只允許 Render 私網連線（`ipAllowList: []`） |

## 環境變數

| 變數 | 服務 | 來源 | 需手動？ |
|---|---|---|---|
| `DATABASE_URL` | lofight-api | `fromDatabase.connectionString`（`postgresql://user:pass@host:port/db`） | 否 |
| `JWT_SECRET_KEY` | lofight-api | `generateValue: true`（隨機 256-bit base64） | 否 |
| `GOOGLE_CLIENT_ID` | lofight-api | `sync: false` | **是** |
| `VITE_GOOGLE_CLIENT_ID` | lofight（build 時） | `sync: false`，值同上 | **是** |
| `PORT` | lofight-api | Render 自動注入（預設 10000） | 否 |

注意：`sync: false` 只在「第一次建立 Blueprint」時會在 Dashboard 提示填值；既有 Blueprint 同步時會被忽略。`lofight` 若已存在，就要到 Dashboard → 該服務 → Environment 手動新增；改完前端變數要重新 deploy 才會進 bundle。

`backend/docker-entrypoint.sh` 啟動時會：
- `DATABASE_URL` → `DB_URL`（`jdbc:postgresql://host:port/db`）、`DB_USERNAME`、`DB_PASSWORD`（帳密會做 %XX 解碼）
- `PORT` → `SERVER_PORT`
- 已明確設定 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`SERVER_PORT` 時不覆蓋，所以本機 `docker compose up` 行為不變（仍是 8080）。

## 首次部署步驟

1. 確認 `render.yaml` 的 `region`（目前 `singapore`）是否要改；web service 與 DB 必須同區才能用私網連線，**建立後不可改**。
2. 把 `render.yaml`、`backend/` 推上 `main`。
3. Render Dashboard → Blueprints：若 repo 已連成 Blueprint，按 **Sync**；否則 New → Blueprint 選此 repo。
4. 填入（或事後在 Environment 補上）`GOOGLE_CLIENT_ID`、`VITE_GOOGLE_CLIENT_ID`。
5. 等 `lofight-api` 部署完，看它實際的 `https://<name>.onrender.com` 網址；**若不是 `lofight-api.onrender.com`**（名稱被占用時 Render 會加後綴），改 `render.yaml` 的 rewrite `destination` 再推一次。
6. Google Cloud Console 的 OAuth client 要把前端網址（`https://lofight.onrender.com` 或自訂網域）加入 Authorized JavaScript origins。
7. 驗證：`curl -i https://<前端網址>/api/csg/v1/leaderboards` 應回 401 JSON（`未帶 Token`）。

## `/api/*` rewrite 如何運作

後端沒開 CORS，所以前端一律打同源路徑 `/api/csg/v1/...`。靜態站的 rewrite 規則把 `/api/*` 轉送到 `https://lofight-api.onrender.com/api/*`（`*` 帶入剩餘路徑），瀏覽器看到的仍是前端網域，因此不需要 CORS。

- 只能指向「公開網址」：route 不支援 `fromService`，且 free web service 收不到私網流量。
- 社群回報 rewrite 代理會多約 300–400ms 延遲；若在意，可改成後端開 CORS + 前端直打 API 網址，或前端改由後端同一服務供應。

## 免費方案注意事項

- **Postgres 30 天到期**：建立 30 天後失效，14 天寬限期內未升級會連同資料刪除。每個 workspace 只能有 1 個免費 DB，容量 1 GB。
- **Web service 休眠**：15 分鐘無流量即休眠，下次請求約 1 分鐘喚醒（期間前端 API 呼叫可能逾時）。free = 0.1 CPU / 512 MB，Spring Boot 冷啟動會比本機慢很多。
- 每月 750 free instance hours；檔案系統是暫時的（重啟即消失）。
- 無 HTTP health check：所有 API 都需 token（回 401），Render 改用預設 TCP port 探測。

參考：<https://render.com/docs/blueprint-spec>、<https://render.com/docs/redirects-rewrites>、<https://render.com/docs/free>、<https://render.com/docs/web-services#port-binding>、<https://render.com/docs/health-checks>
