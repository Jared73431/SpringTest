# Spring WebSocket 2 - STOMP 聊天室

[Spring_Websocket](../Spring_Websocket) 的原生 WebSocket 只有「一則一則的文字」：格式、誰在哪個聊天室、要轉給誰，都要自己處理。

**STOMP** 在 WebSocket 上加一層「目的地」，Spring 依目的地分派，就像 HTTP 的 `@RequestMapping`：

```text
客戶端 SEND      /app/rooms/java/messages   → ChatController 的 @MessageMapping 處理
客戶端 SUBSCRIBE /topic/rooms/java          → 送到這裡的訊息，broker 轉給所有訂閱者
客戶端 SUBSCRIBE /user/queue/private        → 只收到給「自己」的訊息
```

這個模組是一個聊天室：多個聊天室、聊天紀錄、誰在線上、私訊、錯誤回報。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-websocket`） |
| 資料庫 | PostgreSQL（資料庫名稱 `Websocket2`），Flyway 建立 `chat_message` |
| 前端 | 一個靜態頁面，`@stomp/stompjs` 7.0.0（CDN） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Spring 的 `WebSocketStompClient`、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_StompBasicsTest](src/test/java/com/example/demo/lesson/L01_StompBasicsTest.java) | SEND / SUBSCRIBE、目的地；發送者來自連線而不是訊息；聊天紀錄用 REST |
| 2 | [L02_PresenceTest](src/test/java/com/example/demo/lesson/L02_PresenceTest.java) | 誰在線上：加入時廣播、**斷線時**移除 |
| 3 | [L03_PrivateMessageTest](src/test/java/com/example/demo/lesson/L03_PrivateMessageTest.java) | 私訊：`convertAndSendToUser` 依 Principal 找到對方 |
| 4 | [L04_ErrorsAndOrderTest](src/test/java/com/example/demo/lesson/L04_ErrorsAndOrderTest.java) | 錯誤送回發送者；訊息順序；Origin |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/WebSocketConfig          STOMP 端點 /ws、broker、握手時決定 Principal、順序
└── chat/
    ├── ChatController              @MessageMapping：加入、發言、私訊；@MessageExceptionHandler
    ├── PresenceEventListener       SessionDisconnectEvent：斷線時廣播 LEAVE
    ├── RoomPresence                每個聊天室的線上名單（記憶體）
    ├── RoomHistoryController       GET /api/rooms/{room}/messages
    ├── ChatMessageRepository       JdbcClient
    └── ChatUser、ChatMessage       record

src/main/resources/
├── static/index.html               示範頁面
└── db/migration/V1__chat_message.sql
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers），不會連到本機的資料庫。

```bash
./gradlew test
```

### Docker Compose

```bash
docker compose up --build -d
# Windows 保留了 8109 時：APP_PORT=8309 docker compose up --build -d
```

開啟 http://localhost:8109 ，開兩個分頁，用不同名稱加入同一個聊天室，互相發言、私訊。

```bash
curl http://localhost:8109/api/rooms/java/messages     # 聊天紀錄
docker compose down -v
```

### 本機執行

需要本機的 PostgreSQL 有 `Websocket2` 資料庫，表由 Flyway 自動建立。

```bash
./gradlew bootRun
```

### Postman？

STOMP 的每個 frame 要用一個 NUL 字元（`\0`）結尾，Postman 的 WebSocket 介面不方便輸入，所以這個模組建議用示範頁面或課程測試。只看握手可以用 curl：

```bash
curl -i -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" \
     -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" "http://localhost:8109/ws?name=alice"
# 101；沒有 name 是 400；加上 -H "Origin: http://evil.example" 是 403
```

## 目的地

| 方向 | 目的地 | 內容 |
|---|---|---|
| 客戶端 → 伺服器 | `/app/rooms/{room}/join` | 加入聊天室 |
| 客戶端 → 伺服器 | `/app/rooms/{room}/messages` | 發言：`{"content": "hi"}` |
| 客戶端 → 伺服器 | `/app/private` | 私訊：`{"to": "bob", "content": "hi"}` |
| 伺服器 → 聊天室 | `/topic/rooms/{room}` | 發言（CHAT）、加入（JOIN）、離開（LEAVE） |
| 伺服器 → 聊天室 | `/topic/rooms/{room}/users` | 線上名單 |
| 伺服器 → 個人 | `/user/queue/private` | 私訊（對方與自己都收到） |
| 伺服器 → 個人 | `/user/queue/errors` | 錯誤 |
| HTTP | `GET /api/rooms/{room}/messages?limit=50` | 最近的聊天紀錄（舊的在前） |

連線：`ws://localhost:8109/ws?name=alice`（名稱 1～20 個字母、數字、`_`、`-`）。

## WebSocket 教學

### 1. STOMP 與「我是誰」（第 1 課）

| | 原生 WebSocket | STOMP |
|---|---|---|
| 訊息 | 一段文字，格式自己決定 | 有目的地、標頭、內容 |
| 誰收到 | 自己記住所有連線、自己轉送 | 訂閱同一個目的地的人 |
| 伺服器端 | `WebSocketHandler` | `@MessageMapping`，像寫 Controller |

**發送者來自連線，不是訊息**：連線時（握手）從 `?name=` 決定這條連線的 **Principal**，之後每則訊息的發送者都由伺服器從 Principal 取得。修正前的發送者是訊息裡的 `sender` 欄位，由客戶端自己填，任何人都能冒充別人（baseline 確認）。

練習用的名稱沒有密碼。真實系統由 Spring Security 驗證身分後決定 Principal，例如用 [Spring_Security3](../Spring_Security3) 的 JWT。

**「查過去」用 REST、「收即時」用 WebSocket**：進入聊天室時，先用 HTTP 取得最近的訊息，再訂閱收新的訊息。

### 2. 誰在線上（第 2 課）

斷線不一定是使用者按「離開」，關掉分頁、網路中斷也算。Spring 在連線關閉時發出 `SessionDisconnectEvent`，在這裡把人從名單移除。

修正前的名單存在 Redis，但斷線時沒有移除，要等 1 小時才過期（baseline 確認）。只有一台主機時，名單放在記憶體就夠了。

**多台主機**：內建的 broker 在記憶體中，連到 A 主機的人收不到 B 主機的訊息。要改用 RabbitMQ 這類外部 broker（`enableStompBrokerRelay`），名單也要改存在共用的地方。

### 3. 私訊（第 3 課）

```java
messagingTemplate.convertAndSendToUser("bob", "/queue/private", message);
```

每個人都訂閱 `/user/queue/private`（不寫自己的名字），Spring 依**名稱**（Principal）找到 bob 的連線，只送給他。

修正前的版本沒有 Principal，Spring 找不到任何屬於 bob 的連線，**私訊永遠送不到**（baseline 確認）。

### 4. 錯誤、順序與 Origin（第 4 課）

**錯誤送回發送者**：

```java
@MessageExceptionHandler(IllegalArgumentException.class)
@SendToUser(destinations = "/queue/errors", broadcast = false)
public String handleError(IllegalArgumentException e) { ... }
```

訊息不合法（空白、超過 500 字、聊天室名稱不合法、對方不在線上）時，只有發送者的 `/user/queue/errors` 收到原因。修正前的 Spring_Websocket4 只寫 log，發送者不知道訊息沒送出（baseline 確認）。

**順序**：客戶端送來的訊息預設由執行緒池平行處理，Spring **不保證順序**。修正前的 baseline 遇過一次「加入」和「發言」顛倒。本模組設定：

| 設定 | 作用 |
|---|---|
| `setPreserveReceiveOrder(true)` | 同一個連線送來的訊息依序處理 |
| `setPreservePublishOrder(true)` | 送給同一個連線的訊息依序送出 |

實測：關掉這兩個設定，連續送 50 則訊息跑 3 次都沒有顛倒（處理得太快），所以不容易重現，但不代表不會發生。

**Origin**：沒有 `setAllowedOriginPatterns("*")`，其他網站的頁面不能連進來（403）。原因見 Spring_Websocket 第 3 課。

**XSS**：伺服器存的是使用者輸入的原文，**顯示時**才要處理。頁面一律用 `textContent`，訊息中的 HTML 不會被執行。修正前用 `innerHTML`，任何人送出 `<img src=x onerror=...>`，所有人的瀏覽器都會執行。

## 面試時能說明的重點

**Q：為什麼在 WebSocket 上用 STOMP？**
原生 WebSocket 只傳文字，誰該收到什麼要自己處理；STOMP 加上目的地與訂閱，Spring 用 `@MessageMapping` 處理、broker 負責轉送，寫法像 Controller。

**Q：`/topic`、`/queue`、`/app`、`/user` 各是什麼？**
`/app` 開頭的送到 `@MessageMapping`；`/topic` 是一對多的廣播；`/queue` 慣例上用在一對一；`/user/queue/...` 會被 Spring 轉成「這個使用者的連線」。

**Q：私訊怎麼送給特定的人？**
`convertAndSendToUser(名稱, 目的地, 訊息)`，Spring 依連線的 Principal 找到對方。所以連線時一定要決定 Principal。

**Q：為什麼發送者不能從訊息內容讀取？**
訊息內容由客戶端決定，可以冒充別人；發送者要從伺服器確認過的連線身分取得。

**Q：聊天室要擴充到多台主機時要注意什麼？**
內建的 broker 在記憶體中，要改用外部 broker（例如 RabbitMQ）；線上名單也要放在共用的地方。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試（Testcontainers）：鎖定修正前的聊天室、私訊、線上名單 |
| 2 | Java 17 → 21、Spring Boot 3.5.0 → 3.5.16 → 4.1.1、Gradle 8.14 → 9.8.0 |
| 3 | 重構：STOMP 聊天室，第 1 課 |
| 4 | 第 2～4 課 |
| 5 | Docker |

### 修正前的版本

| 修正前 | 修正後 |
|---|---|
| 訊息存 PostgreSQL（JPA）、Redis 記錄線上名單與快取、SockJS | 聊天紀錄改用 JdbcClient + Flyway；名單改放記憶體；移除快取與 SockJS |
| 多聊天室、私訊、上下線 | 保留；私訊修正為可以送達 |

### 修正前發現的問題（baseline 確認）

| 問題 | 說明 |
|---|---|
| **XSS** | 頁面用 `innerHTML` 顯示訊息（有的只有系統訊息沒有跳脫，而系統訊息的內容來自客戶端） |
| 冒充他人 | 發送者由訊息的 `sender` 欄位決定 |
| 任何網站都能連線 | `setAllowedOriginPatterns("*")` |
| **私訊永遠送不到** | 沒有 Principal |
| 「最近訊息」API 一律 500 | Redis 快取用 Java 序列化，`ChatMessage` 沒有實作 `Serializable`；若先快取到空的 List，又因為清除快取的 key 對不上（`recent_messages:` vs `recentMessages::`）而一直是空的 |
| 斷線後仍在線上名單 | 斷線時沒有從 Redis 移除 |
| 其他 | `thymeleaf.*` 少了 `spring.` 前綴，沒有作用；log 套件名稱寫錯；static 的全域狀態；`System.out`；JPA、Redis、multipart 設定用不到；測試連到本機資料庫與 Redis |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 連線 | `/ws`（SockJS），任何網站 | `/ws?name=...`（原生 WebSocket），只允許同源 |
| 發送者 | 訊息的 `sender` 欄位 | 連線的 Principal |
| 目的地 | `/app/chat.sendMessage`、`/app/chat/{roomId}`、`/topic/public`… | 見「目的地」 |
| REST | `/api/chat/recent/{room}`、`/history`、`/search`、`/online` | `GET /api/rooms/{room}/messages` |
| 資料庫 | `test`（JPA 建表） | `Websocket2`（Flyway） |
| Port | 8015 | 8109 |
| 頁面 | 352 行、771 行的 Thymeleaf 頁面 | 一個靜態頁面 |

### 升級時學到的事

- **[Potential Bug] Jackson 3 改用有參數的建構子**：Boot 4 升級時，baseline 抓到訊息存不進資料庫。原因是 Jackson 3 改用 `ChatMessage` 有 4 個參數的建構子建立物件，客戶端沒送的 `roomId` 變成 null，而不是欄位的預設值 `"public"`，違反 not-null。在無參數建構子加上 `@JsonCreator` 才恢復原本的行為。編譯完全正常，只有執行時才會發現。
- **Jackson 3 的新類別**：`GenericJackson2JsonRedisSerializer` → `GenericJacksonJsonRedisSerializer`；`MappingJackson2MessageConverter` → `JacksonJsonMessageConverter`（內建支援 `java.time`）。
- **伺服器回傳 `String` 時是 `text/plain`**：測試客戶端只設定 JSON 的 converter 時收不到錯誤訊息，要再加 `StringMessageConverter`。
