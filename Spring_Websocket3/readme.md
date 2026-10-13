# Spring WebSocket 3 - 檔案分享：HTTP 傳檔案、WebSocket 發通知

多人分享檔案：任何人上傳一個檔案，所有開著頁面的人**立刻**看到它。

```text
上傳者 ──HTTP POST /api/files──→ 伺服器 ──存檔、寫入資料庫
                                    └──STOMP /topic/files──→ 每個開著頁面的人
下載者 ──HTTP GET /api/files/{id}/content──→ 伺服器
```

重點是**分工**：檔案本身走 HTTP，「有新檔案」這件事走 WebSocket。另外，使用者上傳的檔名、類型、內容都不可信，要安全地存放與下載。

WebSocket 系列：

| 模組 | 主題 |
|---|---|
| [Spring_Websocket](../Spring_Websocket) | 原生 WebSocket：兩種寫法、廣播、多執行緒送訊息、Origin 檢查 |
| [Spring_Websocket2](../Spring_Websocket2) | STOMP：聊天室、私訊 |
| **Spring_Websocket3**（本模組） | 檔案分享：HTTP 上傳下載 + WebSocket 通知、安全地存放檔案 |

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-websocket`、`spring-boot-starter-webmvc`） |
| 資料庫 | PostgreSQL（資料庫名稱 `Websocket3`），Flyway 建立 `shared_file` |
| 前端 | 一個靜態頁面，`@stomp/stompjs` 7.0.0（CDN） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、RestClient、`WebSocketStompClient`、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_UploadDownloadTest](src/test/java/com/example/demo/lesson/L01_UploadDownloadTest.java) | 上傳（201 + Location）、下載（附件、原本的檔名）、列表；舊檔案也下載得到 |
| 2 | [L02_NotificationTest](src/test/java/com/example/demo/lesson/L02_NotificationTest.java) | 上傳完成後，用 STOMP 通知所有人 |
| 3 | [L03_SafeStorageTest](src/test/java/com/example/demo/lesson/L03_SafeStorageTest.java) | 檔名、類型、內容、大小都不可信 |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/WebSocketConfig        STOMP 端點 /ws、broker /topic
└── file/
    ├── FileController            /api/files：上傳、列表、資料、下載
    ├── FileService               檢查 → 存檔 → 寫入資料庫 → 通知 /topic/files
    ├── FileStorage               磁碟上的檔名一律是 UUID
    ├── SharedFileRepository      JdbcClient
    ├── SharedFile                record（Info：對外的資料）
    └── FileExceptionHandler      ProblemDetail：400、404、413

src/main/resources/
├── static/index.html             示範頁面（上傳、列表、即時出現新檔案）
└── db/migration/V1__shared_file.sql
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers），不會連到本機的資料庫，檔案存到暫存目錄。

```bash
./gradlew test
```

### Docker Compose

```bash
docker compose up --build -d
# Windows 保留了 8110 時：APP_PORT=8310 docker compose up --build -d
```

開啟 http://localhost:8110 ，開兩個分頁，一邊上傳，另一邊會立刻出現。

```bash
curl -F file=@readme.md -F uploadedBy=alice http://localhost:8110/api/files    # 201 + Location
curl http://localhost:8110/api/files
curl -OJ http://localhost:8110/api/files/{id}/content                           # 用原本的檔名存檔
docker compose down -v                                                          # 刪除資料庫與上傳的檔案
```

### 本機執行

需要本機的 PostgreSQL 有 `Websocket3` 資料庫。檔案存到 `uploads/`（可用 `STORAGE_DIRECTORY` 修改）。

```bash
./gradlew bootRun
```

### 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的服務實際執行過。Environment 變數 `baseUrl` = `http://localhost:8110`。

**1. 上傳**：`POST {{baseUrl}}/api/files`

- **Body** 選 **form-data**：
  - key `file`，右邊的類型從 Text 改成 **File**，選一個檔案
  - key `uploadedBy`，值 `alice`
- **Scripts** → **Post-response**，把檔案的 id 存起來：

  ```javascript
  pm.collectionVariables.set("fileId", pm.response.json().id);
  ```

回應 `201 Created`，標頭 `Location: /api/files/{id}`。

**2. 其他請求**：

| 請求 | 結果 |
|---|---|
| `GET {{baseUrl}}/api/files` | 最近上傳的檔案，新的在前 |
| `GET {{baseUrl}}/api/files/{{fileId}}/content` | 檔案內容；Headers 分頁看得到 `Content-Disposition: attachment`、`X-Content-Type-Options: nosniff` |
| `GET {{baseUrl}}/api/files/00000000-0000-0000-0000-000000000000/content` | 404（ProblemDetail） |
| 上傳一個空檔案 | 400 |
| 上傳超過 5MB 的檔案 | 413 |

通知（`/topic/files`）是 STOMP，Postman 不方便測試，請用示範頁面觀察。

## API

| 方法 | 路徑 | 說明 | 回應 |
|---|---|---|---|
| POST | `/api/files` | multipart：`file`、`uploadedBy`（1～20 個字母、數字、`_`、`-`） | 201 + `Location`；400、413 |
| GET | `/api/files` | 最近 50 個，新的在前 | 200 |
| GET | `/api/files/{id}` | 檔案的資料 | 200、404 |
| GET | `/api/files/{id}/content` | 下載 | 200、404 |

上傳成功時，伺服器同時送出通知到 STOMP 的 `/topic/files`（內容和上傳的回應相同）。

## WebSocket 教學

### 1. 為什麼檔案走 HTTP、通知走 WebSocket（第 1、2 課）

| | HTTP | WebSocket |
|---|---|---|
| 上傳大檔案 | multipart、大小限制、進度都有現成的支援 | 要自己切塊、自己組回來 |
| 下載 | 檔名、類型、快取、續傳都是標準標頭 | 要自己定義 |
| 伺服器主動通知 | 做不到（客戶端要一直問） | 擅長 |

所以各做擅長的事：檔案用 HTTP 上傳下載；上傳完成後，伺服器用 `SimpMessagingTemplate` 主動送一則小小的通知到 `/topic/files`。

伺服器可以在**任何地方**主動送 WebSocket 訊息（這裡是 `FileService`），不一定要由客戶端的 WebSocket 訊息觸發。

### 2. 使用者上傳的東西都不可信（第 3 課）

| 不可信的 | 可能的攻擊 | 本模組的做法 |
|---|---|---|
| **檔名** | `../../etc/passwd` 這類路徑（路徑穿越）、和別人的檔名重複 | 磁碟上一律用新產生的 **UUID** 當檔名；原本的檔名只存在資料庫，下載時放回 `Content-Disposition`；另外檢查解析後的路徑還在存放目錄裡 |
| **Content-Type** | 上傳時由客戶端宣稱，可以偽造 | 下載時由**伺服器**依副檔名決定，只認得 png、jpg、gif、pdf、txt，其他一律 `application/octet-stream` |
| **內容** | 上傳一個 HTML，誘騙別人在你的網站上打開（XSS） | 一律當成**附件**下載（`attachment`），加上 `X-Content-Type-Options: nosniff`，瀏覽器只會存檔，不會在網站上執行 |
| **大小** | 塞爆磁碟 | 單一檔案最大 5MB，超過回應 413 |

**[Learning] 收不到 413，只看到連線中斷**：實測上傳 6MB 時，客戶端收到的是「連線中斷」，不是 413。

- 原因：Tomcat 拒絕請求時，最多只會讀掉 **2MB**（`server.tomcat.max-swallow-size` 的預設值）還沒送完的內容，超過就直接關閉連線。
- 解法：設定 `server.tomcat.max-swallow-size=10MB`（比上傳限制大）之後，才收得到 413。

**檔案位置記在資料庫**：下載時用 id 查資料庫，得到磁碟上的檔名，再讀檔。存檔成功、但資料庫寫入失敗時，會刪掉剛存的檔案，不留下沒有紀錄的檔案。

## 面試時能說明的重點

**Q：為什麼不用 WebSocket 傳檔案？**
HTTP 已經處理好 multipart、大小限制、下載檔名與類型、快取、續傳；WebSocket 適合伺服器主動送出的小訊息，例如「有新檔案」的通知。

**Q：處理使用者上傳的檔案要注意什麼？**
檔名可能有路徑（路徑穿越），所以磁碟上用伺服器產生的名稱；Content-Type 可以偽造，所以下載時由伺服器決定；內容可能是 HTML，所以當附件下載並加上 nosniff；限制大小。

**Q：檔案存在哪裡、資料庫存什麼？**
檔案內容存在磁碟（或物件儲存，例如 S3），資料庫存檔案的資料：原本的檔名、磁碟上的名稱、大小、上傳者、時間。下載時用資料庫的紀錄找到檔案。

**Q：伺服器怎麼主動送 WebSocket 訊息？**
注入 `SimpMessagingTemplate`，在任何地方 `convertAndSend("/topic/files", 資料)`，訂閱這個目的地的人都會收到。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試（Testcontainers）：鎖定修正前的上傳、下載、聊天訊息 |
| 2 | Java 17 → 21、Spring Boot 3.5.0 → 3.5.16 → 4.1.1、Gradle 8.14 → 9.8.0 |
| 3 | 重構：檔案分享（HTTP + STOMP 通知）；第 1 課 |
| 4 | 第 2、3 課 |
| 5 | Docker |

### 修正前的版本

修正前是一個聊天室（原生 `WebSocketHandler`、多聊天室、私訊）加上檔案上傳，前端頁面超過 1300 行。重構時只保留檔案分享：

| 修正前 | 修正後 |
|---|---|
| 上傳：`POST /api/chat/upload`（multipart + JSON 字串 `messageData`） | `POST /api/files`（multipart：`file`、`uploadedBy`） |
| 下載：`/api/files/download/{磁碟上的檔名}` | `/api/files/{id}/content` |
| 縮圖（imgscalr） | 移除 |
| 聊天室、私訊 | 移除（聊天見 Spring_Websocket2） |

### 修正前發現的問題（baseline 確認）

| 問題 | 說明 |
|---|---|
| **3 天前的檔案下載不到** | 資料庫存了檔案的路徑，下載時卻不使用，而是依序到「今天、昨天、前天」的日期目錄找 |
| 相信客戶端的 Content-Type | 檢查與下載都依客戶端宣稱的類型 |
| **冒充他人** | 身分由網址的 `?username=` 與訊息的 `senderUsername` 決定 |
| 任何網站都能連線 | `setAllowedOrigins("*")`，CORS 也允許任何網站帶憑證 |
| **訊息格式錯誤時靜默失敗** | 例外被吞掉，只寫 log，發送者不知道訊息沒送出 |
| 每日清理排程從未執行 | 有 `@Scheduled`，沒有 `@EnableScheduling` |
| 連到其他模組的資料庫 | 資料庫網址指向 `Flyway`，而且 `ddl-auto: update` 會在裡面建表 |
| 上傳目錄寫死 | `D:/uploads`，在 Linux / Docker 不能用 |
| 其他 | 多執行緒同時對同一條連線送訊息（見 Spring_Websocket 第 2 課）；任何人都能用 `currentUser` 參數讀別人的私訊；commons-io 2.11.0 有已知漏洞；用不到的 commons-fileupload |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 功能 | 聊天室 + 檔案上傳 | 檔案分享 |
| 資料庫 | `Flyway`（其他模組的資料庫） | `Websocket3`（Flyway 建表） |
| 檔案目錄 | `D:/uploads/yyyy/MM/dd/` | `STORAGE_DIRECTORY`（預設 `uploads/`），不分日期目錄 |
| 單一檔案上限 | 10MB | 5MB |
| Port | 8080 | 8110 |

### 升級時學到的事

- **[Potential Bug] Jackson 3 讓應用程式無法啟動**：`spring.jackson.serialization.write-dates-as-timestamps` 在 Jackson 3 已經不屬於 `SerializationFeature`，Boot 4 啟動時就失敗（`No enum constant`）。Jackson 3 預設已經輸出 ISO 格式的日期、內建 `java.time`，這個設定和 `JavaTimeModule` 直接拿掉即可。
- **`MultipartBodyBuilder` 需要 Reactive Streams**：沒有 WebFlux 的專案，測試用 `LinkedMultiValueMap` + `HttpEntity` 組 multipart。
- **Tomcat 的 `max-swallow-size`**：見「WebSocket 教學」第 2 點。
