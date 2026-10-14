# Postman Collection

一份 Collection 涵蓋各模組的 API：每個模組一個資料夾，每個請求都附有測試腳本，按 **Run** 就能一次驗證整個模組。

| 檔案 | 說明 |
|---|---|
| `SpringTest.postman_collection.json` | Collection：每個模組一個資料夾 |
| `SpringTest.postman_environment.json` | Environment：各模組的網址（port）與練習用的帳號 |
| `files/` | 上傳檔案用的範例（Spring_Websocket3） |

## 使用方式

### Postman 桌面版

1. **Import** → 選擇 `SpringTest.postman_collection.json` 與 `SpringTest.postman_environment.json`
2. 右上角的 Environment 選 **SpringTest (localhost)**
3. 啟動要測試的模組：每個資料夾的說明（點資料夾 → Overview）寫著要怎麼啟動
4. 對資料夾按 **Run**（Collection Runner），或一個一個請求送出

需要上傳檔案的請求（Spring_Websocket3）：**Settings → General → Working directory** 設成這個 `postman` 資料夾。

### Postman CLI

```bash
# 在 Repo 根目錄執行；-i 指定資料夾（模組）
postman collection run postman/SpringTest.postman_collection.json \
    -e postman/SpringTest.postman_environment.json \
    --working-dir postman \
    -i Spring_Kafka
```

port 不同時用 `--env-var` 覆蓋，例如 `--env-var kafkaUrl=http://localhost:8312`。

## 串接的請求

需要前一個請求結果的步驟（建立後查詢、取得 Token 後呼叫 API），由測試腳本把 id、Token 存進 **collection variables**：

```javascript
pm.collectionVariables.set("jpaBookId", pm.response.json().id);       // 之後的請求用 {{jpaBookId}}
pm.collectionVariables.set("security3Token", pm.response.json().accessToken);
```

所以同一個資料夾要**依序**執行（Runner 預設就是依序）。非同步處理的模組（Kafka、Batch-4），查詢的請求會在 Pre-request script 先等幾秒。

每次執行都會建立新的資料（名稱、email 帶有時間戳），重複執行不會因為「已經存在」而失敗。

## 涵蓋範圍

| 分類 | 模組 |
|---|---|
| 入門 / Web | HelloWorld、JPA、JPA2、Swagger、Thymeleaf |
| 呼叫其他服務 | Feign_Server、Feign_Client、HttpServiceClient、HttpClient、Okhttp |
| 資料 / 快取 | Redis、Cache、Caffeine、Flyway、Webflux、R2DBC、R2DBC2 |
| 排程 / 批次 | Scheduling、Quartz、Batch-4 |
| 安全性 | Security、Security2、Security3 |
| 即時通訊 | Websocket、Websocket2、Websocket3 |
| 微服務 | Eureka、Eureka_Provider、Eureka_Client、Gateway_Server、Gateway_Client |
| 事件 / 監控 | Kafka、Kafka2（Order + Shipping）、Actuator |

不在 Collection 中的：

- **Spring_Batch、Batch-2、Batch-3**：命令列執行的一次性程式，沒有 API
- **WebSocket / STOMP 本身**：Postman CLI 不支援 WebSocket。Spring_Websocket 的資料夾說明列出連線網址，可以在 Postman 桌面版用 **New → WebSocket** 連線；STOMP 的聊天請用 Spring_Websocket2 的網頁
- **無限的 SSE 串流**（Spring_Webflux 的 `/api/ticks`）：Runner 會一直等下去，請用瀏覽器或 `curl -N`

## 帳號

Environment 中的帳密都是各模組**公開的練習用預設值**（已經寫在模組的設定檔、Flyway 與 README 中），不是真正的密碼：

| 模組 | 帳號 | 密碼 |
|---|---|---|
| Spring_Security | `user` | `password`（環境變數 `SECURITY_USER_PASSWORD` 的預設值） |
| Spring_Security2 | `alice`（USER）、`admin`（ADMIN）、`bob`（停用） | `password` |
| Spring_Security3 | `alice`（USER）、`admin`（ADMIN） | `password` |

改了模組的密碼時，在 Environment 的 **Current value** 修改即可（Current value 只存在自己的電腦，不會被匯出）。

## Windows 的保留 port

Windows（Hyper-V / WSL / Docker Desktop）會動態保留一些 port，**重新開機後範圍可能改變**。被保留的 port 無法綁定：

```text
Error response from daemon: ports are not available: ... bind: An attempt was made to access a socket in a way forbidden by its access permissions.
```

查看目前保留的範圍：

```powershell
netsh int ipv4 show excludedportrange protocol=tcp
```

遇到時：

1. 啟動時改用其他主機 port。大部分模組的 docker-compose.yml 支援 `APP_PORT`，例如 `APP_PORT=8312 docker compose up --build -d`（Spring_Kafka2_Order 是 `ORDER_PORT` / `SHIPPING_PORT`）
2. Environment 中對應的網址改成新的 port（或 CLI 用 `--env-var`）

## 驗證紀錄

每個資料夾都已經用 Postman CLI 對 Docker 中的服務實際執行過，全部的測試通過。
