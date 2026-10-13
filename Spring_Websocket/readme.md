# Spring WebSocket - 原生 WebSocket

WebSocket 讓瀏覽器和伺服器之間保持一條**雙向**的連線：伺服器不必等請求，隨時可以主動送訊息（聊天、通知、即時報價）。

這個模組不使用任何上層協定，直接處理「一則一則的文字訊息」，並把同一個 Echo 用兩種寫法各做一次：

| 路徑 | 寫法 |
|---|---|
| `/ws/jakarta/echo` | Jakarta WebSocket（Java 標準 API）的 `@ServerEndpoint` |
| `/ws/spring/echo` | Spring 的 `WebSocketHandler` |
| `/ws/spring/broadcast` | Spring：一條連線送來的訊息轉送給所有連線 |

WebSocket 系列：

| 模組 | 主題 |
|---|---|
| **Spring_Websocket**（本模組） | 原生 WebSocket：兩種寫法、廣播、多執行緒送訊息、Origin 檢查 |
| [Spring_Websocket2](../Spring_Websocket2) | STOMP：聊天室、私訊 |
| [Spring_Websocket3](../Spring_Websocket3) | 檔案分享：REST 上傳 + WebSocket 通知 |

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-websocket`） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Spring 的 `StandardWebSocketClient`、Awaitility |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_JakartaVsSpringTest](src/test/java/com/example/demo/lesson/L01_JakartaVsSpringTest.java) | 同一個 Echo 兩種寫法；每條連線一個物件 vs 共用一個；關閉狀態碼 |
| 2 | [L02_BroadcastTest](src/test/java/com/example/demo/lesson/L02_BroadcastTest.java) | 廣播；斷線時移除；**多執行緒同時送訊息會失敗** |
| 3 | [L03_OriginCheckTest](src/test/java/com/example/demo/lesson/L03_OriginCheckTest.java) | Origin 檢查：Spring 預設只允許同源，Jakarta 預設不檢查 |

## 專案結構

```text
src/main/java/com/example/demo/
├── jakarta/
│   ├── EchoEndpoint            @ServerEndpoint("/ws/jakarta/echo")
│   └── JakartaEndpointConfig   ServerEndpointExporter：註冊 @ServerEndpoint
└── spring/
    ├── EchoHandler             TextWebSocketHandler
    ├── BroadcastHandler        記住所有連線、ConcurrentWebSocketSessionDecorator
    └── SpringWebSocketConfig   WebSocketConfigurer：handler 註冊在哪個路徑

src/main/resources/static/index.html   示範頁面（選擇路徑、連線、送訊息）
```

## 執行方式

### 測試（主要的學習方式）

不需要資料庫，也不需要 Docker。

```bash
./gradlew test
```

### 本機執行 / Docker

```bash
./gradlew bootRun
# 或
docker build -t spring-websocket .
docker run --rm -p 8108:8108 spring-websocket     # Windows 保留了 8108 時：-p 8308:8108
```

開啟 http://localhost:8108 ，選擇路徑後連線。廣播可以開兩個分頁試試看。

### 用 Postman 測試

Postman 桌面版：**New → WebSocket**（不是 HTTP 請求）。Postman CLI 不支援 WebSocket，所以這段步驟沒有用 CLI 驗證。

1. URL 填 `ws://localhost:8108/ws/spring/echo`，按 **Connect**
2. 在 Message 輸入 `Jared`，按 **Send** → 收到 `[1791800000000] Hello Jared`
3. 送出 `bye` → 伺服器關閉連線，下方顯示 `1000 Bye`
4. 換成 `ws://localhost:8108/ws/spring/broadcast`，開兩個 WebSocket 分頁都連線，從一個送訊息，兩個都會收到

Postman 不會送 `Origin` 標頭，所以 Spring 的 Origin 檢查不影響它（第 3 課）。

### 用 curl 確認握手

WebSocket 一開始是一個普通的 HTTP 請求（握手），伺服器同意後回應 `101 Switching Protocols`，之後同一條 TCP 連線改走 WebSocket：

```bash
curl -i -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" \
     -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" http://localhost:8108/ws/spring/echo
# HTTP/1.1 101

curl -i -H "Origin: http://evil.example" -H "Connection: Upgrade" -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
     http://localhost:8108/ws/spring/echo
# HTTP/1.1 403（Jakarta 的 /ws/jakarta/echo 則是 101）
```

## WebSocket 教學

### 1. 兩種寫法（第 1 課）

| | Jakarta `@ServerEndpoint` | Spring `WebSocketHandler` |
|---|---|---|
| 是什麼 | Java 標準 API（由 Tomcat 實作） | Spring 包裝的 API |
| 物件 | **每條連線建立一個新的** | **所有連線共用一個**（Spring Bean） |
| 連線怎麼取得 | 存在欄位裡 | 每個方法的參數傳進來，**不能**存在欄位裡 |
| 能不能注入 Spring Bean | 不方便（物件由 Tomcat 建立） | 可以（一般的建構子注入） |
| 註冊 | `ServerEndpointExporter` | `WebSocketConfigurer` |
| Origin 檢查 | 預設**不檢查** | 預設只允許同源 |

使用 Spring 時，一般選 `WebSocketHandler`；`@ServerEndpoint` 適合不想依賴 Spring 的程式。需要「聊天室、訂閱」這類功能時，再往上一層用 STOMP（Spring_Websocket2）。

**關閉狀態碼**：

| 狀態碼 | 意思 |
|---|---|
| 1000 | 正常關閉（本模組：送出 `bye`） |
| 1001 | 離開（例如關掉分頁） |
| 1011 | 伺服器發生預期外的錯誤 |

關閉原因（reason）最多 123 bytes。修正前發生錯誤時直接把例外訊息當作原因，太長時關閉本身會再失敗，所以改成固定的文字。

### 2. 廣播與多執行緒（第 2 課）

廣播要自己記住所有連線（`Map`），斷線時移除，否則會一直對已經關閉的連線送訊息。

**同一條連線不能被多條執行緒同時送訊息**：兩個人同時發言，伺服器的兩條執行緒會同時對第三個人的連線 `sendMessage`。前一則還沒送完，下一則就開始送，會拋出：

```text
IllegalStateException: The remote endpoint was in state [TEXT_PARTIAL_WRITING]
```

實測：8 條執行緒同時對同一條連線送 40 則訊息，**39 則失敗**。修正前的 Spring_Websocket4 就是直接在多條執行緒中對連線送訊息。

修正：用 `ConcurrentWebSocketSessionDecorator` 包起來，同時送的訊息會排隊依序送出（實測 40 則全部送達）。它的兩個參數是保護機制：對方太慢、排隊超過時間或大小時，關閉那條連線，不讓一個慢的客戶端拖累整個伺服器。

### 3. Origin 檢查（第 3 課）

WebSocket **不受瀏覽器的同源政策限制**：任何網站的頁面都能對你的伺服器開 WebSocket，而且瀏覽器會**自動帶上你網站的 cookie**。如果伺服器靠 cookie 認人，惡意網站就能冒用使用者的身分連線。這叫 Cross-Site WebSocket Hijacking，和 CSRF 是同一類問題。

防禦：檢查握手請求的 `Origin` 標頭（瀏覽器自動帶上，頁面無法偽造）。

| | 預設 | 要開放其他網站 |
|---|---|---|
| Spring | 只允許同源，其他網站 403 | `setAllowedOrigins("https://app.example.com")` |
| Jakarta | **不檢查**，任何網站都能連 | 自己寫 `ServerEndpointConfig.Configurator` 覆寫 `checkOrigin` |

不要寫 `setAllowedOrigins("*")`：修正前的 Spring_Websocket2、Spring_Websocket4 都這樣設定，baseline 測試確認任何網站都能連線。

本模組的 Jakarta endpoint 保留預設行為（不檢查），用來對照。

## 面試時能說明的重點

**Q：WebSocket 和 HTTP 有什麼不同？**
HTTP 是「客戶端問、伺服器答」；WebSocket 建立後是一條雙向的連線，伺服器可以隨時主動送訊息。WebSocket 一開始用 HTTP 握手，伺服器回應 101 之後改走 WebSocket。

**Q：`@ServerEndpoint` 和 Spring 的 `WebSocketHandler` 差在哪？**
`@ServerEndpoint` 是 Java 標準，每條連線一個物件；`WebSocketHandler` 是 Spring Bean，所有連線共用一個，連線要從參數取得，不能存在欄位裡。

**Q：伺服器同時對很多人廣播時要注意什麼？**
同一條連線不能被多條執行緒同時送訊息，要用 `ConcurrentWebSocketSessionDecorator` 讓訊息排隊；斷線的連線要移除。

**Q：WebSocket 有什麼安全問題？**
不受同源政策限制，惡意網站可以帶著使用者的 cookie 連線（Cross-Site WebSocket Hijacking）。要檢查 Origin，不要允許 `*`。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試：Echo、`bye` 關閉連線 |
| 2 | Java 17 → 21、Spring Boot 3.5.0 → 3.5.16 → 4.1.1、Gradle 8.14 → 9.8.0 |
| 3 | 重構：Jakarta 與 Spring 兩種寫法、廣播（取自 Spring_Websocket4）；第 1～3 課 |
| 4 | Docker |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 路徑 | `/channel/echo` | `/ws/jakarta/echo`（另外新增 `/ws/spring/echo`、`/ws/spring/broadcast`） |
| Port | 8015（設定寫成 `Server.port:8015`） | 8108 |
| 頁面 | Thymeleaf 範本 | 靜態頁面 `index.html` |
| 發生錯誤時的關閉原因 | 例外訊息 | 固定文字 `Unexpected error` |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| 沒有使用的依賴 | Thymeleaf（頁面不需要伺服器端渲染）、Lombok、devtools |
| 關閉原因可能超過 123 bytes | 見「WebSocket 教學」第 1 點 |
| 其他 | 簡體中文註解；`contextLoads` 測試 |
