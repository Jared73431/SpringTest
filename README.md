# Spring Boot 技術練習集

從 Spring Boot 2.7 / Java 11 開始，一路練習到 Spring Boot 3.5 / Java 17 的 Spring 生態系範例集合。

每個資料夾都是**可以獨立執行的 Spring Boot 專案**，一個資料夾聚焦一個技術主題。
部分主題（Batch、WebSocket、Security）分成幾個模組，由淺到深：從最小的範例到較完整的應用。

這個 Repo 的用途：

- **技術索引**：遇到某個技術時，可以直接找到對應的最小可執行範例
- **學習紀錄**：保留各階段的寫法，搭配重構紀錄比較新舊差異
- **持續現代化**：逐步補上測試、文件，並將舊寫法升級為現代 Java / Spring 寫法

---

## 技術索引

> 📘 = 模組內有獨立 README　🐘 = 需要 PostgreSQL　🟥 = 需要 Redis

### 資料存取

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_JPA](Spring_JPA) 📘 | JPA 入門 | REST CRUD（`/api/books`）、DTO（Record）、ProblemDetail、Testcontainers 整合測試、Docker ✅ 已現代化 | 🐘 |
| [Spring_JPA2](Spring_JPA2) 📘 | JPA 進階 | 一對多 / 多對多、複合主鍵、訂單狀態機、`@Version` 樂觀鎖、JPA Auditing、Bean Validation、圖片存 bytea ✅ 已現代化 | 🐘 |
| [Spring_R2DBC](Spring_R2DBC) 📘 | R2DBC 入門 | 三種存取方式（Repository / R2dbcEntityTemplate / DatabaseClient）、主鍵由程式指定時 save() 默默不寫入、自訂 Converter、Reactive 交易、沒有關聯時的 N+1 與 JOIN、Flyway ✅ 已現代化 | 🐘 |
| [Spring_R2DBC2](Spring_R2DBC2) 📘 | R2DBC + WebFlux | 完整的 Reactive CRUD（`/api/cars`）：Criteria 多條件搜尋、驗證與資料表一致、ProblemDetail、R2DBC Auditing、Flyway、JPA vs R2DBC 對照 ✅ 已現代化 | 🐘 |
| [Spring_Flyway](Spring_Flyway) 📘 | 資料庫版本控管 | SQL / Java Migration、Repeatable Migration 的正確與錯誤用法、checksum 驗證、baseline / out-of-order 陷阱、`/actuator/flyway` ✅ 已現代化 | 🐘 |
| [Spring_Redis](Spring_Redis) 📘 | Redis 操作 | 五種資料結構、安全的 JSON 序列化、排行榜 / 限流（Lua）/ 分散式鎖、Testcontainers、RedisInsight ✅ 已現代化 | 🟥 |
| [Spring_Cache](Spring_Cache) 📘 | Spring Cache | `@Cacheable` / `@CachePut` / `@CacheEvict` 搭配 Redis、快取一致性、`unless` 與 Optional、型別安全的序列化、`transactionAware` ✅ 已現代化 | 🐘 🟥 |
| [Spring_Caffeine](Spring_Caffeine) 📘 | 本機快取 | 與 Spring_Cache 相同的註解改用 Caffeine：容量上限與過期、命中統計（Actuator）、兩個實例示範本機快取的不一致 🆕 | 🐘 |

### 批次與排程

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Batch](Spring_Batch) 📘 | Batch 入門 | 最小的 Job / Step / Tasklet、批次是「做完就結束」的程式、用 exit code 回報 Job 成功或失敗、Boot 4 改用 `starter-batch-jdbc` ✅ 已現代化 | — |
| [Spring_Batch-2](Spring_Batch-2) 📘 | 批次的資料庫 | 3 課：6 張 `BATCH_*` 表、JobInstance 與識別參數（同一天不能處理兩次）、失敗後從失敗的 Step 重新執行；Flyway 建表（`initialize-schema` 升級 Batch 6 時 duplicate key） ✅ 已現代化 | 🐘 |
| [Spring_Batch-3](Spring_Batch-3) 📘 | Chunk：CSV 匯入 | 官方教學的 CSV → 轉大寫 → DB：chunk 大小與 commit 次數、失敗只 rollback 當下的 chunk、`JdbcBatchItemWriter` vs JPA、為什麼不能重新執行（時間戳 vs 業務參數） ✅ 已現代化 | 🐘 |
| [Spring_Batch-4](Spring_Batch-4) 📘 | 多 Job、Reader / Writer | 由 Batch-4、5、6 合併的書店批次，6 課：五種 Reader 比較、`@StepScope` Writer、多個 Job 用名稱區分與條件流程、Skip / Retry、從中斷處繼續、狀態欄位 + 時間戳（OFFSET 分頁會漏資料）；REST 在背景啟動 Job ✅ 已現代化 | 🐘 |
| [Spring_Scheduling](Spring_Scheduling) 📘 | Spring 排程 | `@Scheduled` 6 課：fixedRate vs fixedDelay、cron 與時區（夏令時間會跳過或重複執行）、單一執行緒的陷阱、例外處理、ShedLock 多台主機只執行一次 ✅ 已現代化 | 🐘 |
| [Spring_Quartz](Spring_Quartz) 📘 | Quartz 排程 | 由原本 3 個模組合併：動態排程 API、JDBC JobStore、misfire 補做策略、`@DisallowConcurrentExecution`、叢集（一台停止另一台接手）、Quartz 與 Spring cron 的差異 ✅ 已現代化 | 🐘 |

### Web / API

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_HelloWorld](Spring_HelloWorld) 📘 | 入門 | 最小 REST Controller（`GET /api/hello`）、`@WebMvcTest` 測試 ✅ 已現代化 | — |
| [Spring_Thymeleaf](Spring_Thymeleaf) 📘 | 模板引擎 | 伺服器端渲染的後端重點：Model 與 th:* 語法、th:text 防 XSS、表單驗證回到同一頁、PRG 與 flash、MockMvc 測試畫面 ✅ 已現代化 | — |
| [Spring_Swagger](Spring_Swagger) 📘 | API 文件 | springdoc-openapi 3：自動推導與必要的註解、錯誤回應（ProblemDetail）文件化、prod 關閉文件、OpenAPI 契約測試、匯出規格檔、統一回應格式 vs ProblemDetail ✅ 已現代化 | 🐘 |
| [Spring_Webflux](Spring_Webflux) 📘 | Reactive 程式設計 | 依「反直覺的地方」分成 12 課：Flow 規範、延遲執行、Hot / Cold、忽略回傳值的 Bug、flatMap 順序、錯誤與空值、Scheduler、背壓、generate / create、Context；Annotated vs Functional、JSON / NDJSON / SSE、Sinks 廣播、阻塞呼叫 ✅ 已現代化 | — |
| [Spring_HttpClient](Spring_HttpClient) 📘 | 呼叫外部 API | RestClient（同步）與 WebClient（Reactive）對照、逾時、錯誤對應、WireMock 契約測試 ✅ 已現代化 | — |
| [Spring_Okhttp](Spring_Okhttp) 📘 | 呼叫外部 API | OkHttp 5：同步與非同步（Callback → CompletableFuture）、自訂 Interceptor、日誌遮蔽 Authorization、MockWebServer 測試 ✅ 已現代化 | — |

### 即時通訊

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Websocket](Spring_Websocket) 📘 | 原生 WebSocket | 3 課：Jakarta `@ServerEndpoint` vs Spring `WebSocketHandler`、廣播與多執行緒送訊息（`ConcurrentWebSocketSessionDecorator`）、Origin 檢查；附面試重點 ✅ 已現代化 | — |
| [Spring_Websocket2](Spring_Websocket2) 📘 | STOMP 聊天室 | 4 課：SEND / SUBSCRIBE 與握手時決定 Principal、上下線、私訊（`convertAndSendToUser`）、錯誤送回發送者與訊息順序；附面試重點 ✅ 已現代化 | 🐘 |
| [Spring_Websocket3](Spring_Websocket3) 📘 | 檔案分享 | 3 課：檔案走 HTTP、通知走 STOMP（`SimpMessagingTemplate`）、使用者上傳的檔名 / 類型 / 內容 / 大小都不可信（UUID 檔名、附件 + nosniff、413 與 Tomcat 的 max-swallow-size）；附面試重點 ✅ 已現代化 | 🐘 |

### 事件與訊息

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Kafka](Spring_Kafka) 📘 | Kafka 訂單事件 | 5 課：producer / consumer、partition 與 key（順序）、consumer group（分工 vs 廣播）、offset 與冪等的 consumer、重試與 Dead Letter Topic（poison pill）；KRaft、兩個 listener 的 Docker 設定；附面試重點 🆕 | Kafka |
| [Spring_Kafka2_Order](Spring_Kafka2_Order) 📘 | 兩個服務透過 Kafka 溝通：Order | 3 課：約定是 JSON 不是 Java 類別（關掉 `__TypeId__`）、回覆也是事件與最終一致性（PENDING → SHIPPED / REJECTED）、冪等的 consumer 與「寫入資料庫 + 送出事件」的一致性（Outbox pattern 說明）；Docker Compose 一次啟動整套系統；附面試重點 🆕 | Kafka、🐘 |
| [Spring_Kafka2_Shipping](Spring_Kafka2_Shipping) 📘 | 兩個服務透過 Kafka 溝通：Shipping | 3 課：收到事件 → 出貨 → 回覆、業務上的拒絕 vs 技術上的失敗（DLT）、一個交易包住「處理過的紀錄 + 出貨 + 回覆」與重試 🆕 | Kafka、🐘 |

### 維運與監控

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Actuator](Spring_Actuator) 📘 | Actuator 與監控 | 5 課：端點的公開範圍、health 與自訂 `HealthIndicator`（liveness vs readiness）、info 與 Micrometer metrics（自訂 Counter / Timer、Prometheus 格式）、執行中修改 log level、management port；Docker Compose 附 Prometheus + Grafana dashboard；附面試重點 🆕 | 🐘 |

### 安全性

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Security](Spring_Security) 📘 | Security 自動設定 | 什麼都不寫時 Spring Security 做了什麼：登入頁 vs 401、表單登入與 HTTP Basic、CSRF（curl 收到 401 的陷阱）、明碼 vs BCrypt；附面試重點 ✅ 已現代化 | — |
| [Spring_Security2](Spring_Security2) 📘 | Security 自訂設定 | 4 課：路徑規則與 401 vs 403、`permitAll` vs `web.ignoring`、`@PreAuthorize`、使用者存在資料庫（`JdbcUserDetailsManager`、BCrypt、`ROLE_` 前綴）；附面試重點 ✅ 已現代化 | 🐘 |
| [Spring_Security3](Spring_Security3) 📘 | 無狀態的 API：JWT | 由 Boot 2.7 升級（`WebSecurityConfigurerAdapter` → `SecurityFilterChain`）；3 課：用帳密換 JWT、RSA 簽章與驗證（竄改 / 過期 / 別的金鑰都會被拒絕）、無狀態與 scope；OAuth2 Resource Server，之後可接 Keycloak；附面試重點 ✅ 已現代化 | — |

### 微服務（Spring Cloud）

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Eureka](Spring_Eureka) 📘 | 服務註冊中心 | Eureka Server、註冊 / 心跳 / 自我保護原理、Eureka vs K8S Service ✅ 已現代化 | — |
| [Spring_Eureka_Provider](Spring_Eureka_Provider) 📘 | 服務提供者 | 註冊到 Eureka 的 REST CRUD（`/api/books`），`/api/hello` 回傳實例 id ✅ 已現代化 | 🐘 |
| [Spring_Eureka_Client](Spring_Eureka_Client) 📘 | 服務消費者 | 以服務名稱呼叫（Eureka + OpenFeign + LoadBalancer）、Round Robin 負載平衡測試、Docker Compose 啟動兩個 Provider ✅ 已現代化 | — |
| [Spring_Feign_Server](Spring_Feign_Server) 📘 | Feign 被呼叫端 | 一般 REST CRUD 服務（`/api/books`），與 Spring_JPA 共用 book 資料表 ✅ 已現代化 | 🐘 |
| [Spring_Feign_Client](Spring_Feign_Client) 📘 | Feign 呼叫端 | OpenFeign、自訂 ErrorDecoder（錯誤原樣轉回）、逾時 / log 設定、WireMock 測試、Docker Compose 一次啟動三個服務、Feign vs HTTP Service Client ✅ 已現代化 | — |
| [Spring_HttpServiceClient](Spring_HttpServiceClient) 📘 | HTTP Service Client | 和 Spring_Feign_Client 相同的 API 改用 `@HttpExchange` + `@ImportHttpServices`：群組設定（位址 / 逾時 / 預設標頭）、不需要 ErrorDecoder、WireMock 測試、OpenFeign vs HTTP Service Client 對照；附面試重點 🆕 | — |
| [Spring_Gateway_Server](Spring_Gateway_Server) 📘 | API Gateway | 固定網址與 `lb://`（Eureka）兩種路由、StripPrefix vs RewritePath、自訂 GlobalFilter、改寫 Location header、Docker Compose 以 Gateway 為唯一入口 ✅ 已現代化 | — |
| [Spring_Gateway_Client](Spring_Gateway_Client) 📘 | Gateway 後端服務 | 被 Gateway 轉送的 first-service（`/api/hello`） ✅ 已現代化 | — |

微服務模組需要**同時啟動多個專案**，建議啟動順序：

```text
Eureka：   Spring_Eureka → Spring_Eureka_Provider → Spring_Eureka_Client
Feign：    Spring_Feign_Server → Spring_Feign_Client
Gateway：  Spring_Gateway_Client → Spring_Gateway_Server
```

---

## 技術版本

所有模組都已現代化到 **Spring Boot 4.1 + Java 21**（✅；🆕 為現代化時新增的模組）：

| Spring Boot | Java | 模組 |
|---|---|---|
| **4.1.x** | **21** | HelloWorld ✅、JPA ✅、JPA2 ✅、Feign_Server ✅、Feign_Client ✅、Eureka ✅、Eureka_Provider ✅、Eureka_Client ✅、Gateway_Server ✅、Gateway_Client ✅、HttpClient ✅、Okhttp ✅、Redis ✅、Cache ✅、Caffeine 🆕、Flyway ✅、Webflux ✅、R2DBC ✅、R2DBC2 ✅、Swagger ✅、Thymeleaf ✅、Scheduling ✅、Quartz ✅、Batch ✅、Batch2 ✅、Batch3 ✅、Batch4 ✅、Security ✅、Security2 ✅、Security3 ✅、Websocket ✅、Websocket2 ✅、Websocket3 ✅、HttpServiceClient 🆕、Kafka 🆕、Kafka2_Order 🆕、Kafka2_Shipping 🆕、Actuator 🆕 |

- Build Tool：Gradle（每個模組各自附 Gradle Wrapper）
- Database：PostgreSQL
- Cache：Redis

---

## 快速開始

### 1. 環境需求

- JDK 21
- PostgreSQL（標示 🐘 的模組）
- Redis（標示 🟥 的模組）

如果本機沒有安裝資料庫，可以用 Docker 快速啟動：

```bash
docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15-alpine
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

各模組使用的資料庫名稱請參考模組內的 `application.properties` / `application.yml`（多數為 `test`）。

### 2. 設定環境變數

設定檔**不包含任何真實帳密**，改從環境變數讀取；沒有設定時，會使用括號內的本機開發預設值：

| 環境變數 | 預設值 | 用途 |
|---|---|---|
| `DB_USERNAME` | `postgres` | 資料庫帳號 |
| `DB_PASSWORD` | `postgres` | 資料庫密碼 |
| `SECURITY_USER_PASSWORD` | `password` | Spring_Security 的登入密碼（帳號 `user`） |
| `PGADMIN_PASSWORD` | `admin` | Spring_R2DBC、Spring_R2DBC2 docker compose 的 pgAdmin 密碼（`--profile tools`） |

設定檔中的寫法：

```properties
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
```

> **IntelliJ IDEA**：Run → Edit Configurations → 選擇應用程式 → Environment variables
> 填入 `DB_USERNAME=xxx;DB_PASSWORD=xxx`

### 3. 執行模組

```bash
cd Spring_JPA2
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

> 多數模組預設使用 8015 / 8080 等 port，同時啟動多個模組時請注意 port 衝突。

### 4. 用 Postman 測試

[postman/](postman) 有一份涵蓋各模組 API 的 Collection：每個模組一個資料夾，每個請求都附有測試腳本，匯入後對資料夾按 **Run** 就能驗證整個模組。說明見 [postman/readme.md](postman/readme.md)。

```bash
postman collection run postman/SpringTest.postman_collection.json \
    -e postman/SpringTest.postman_environment.json --working-dir postman -i Spring_JPA2
```

---

## 專案結構

每個模組都是獨立的 Gradle 專案，大部分採用一般的分層結構：

```text
Spring_Xxx/
├── build.gradle
├── gradlew / gradlew.bat
├── readme.md                    # 模組說明（部分模組）
└── src/main/java/com/example/demo/
    ├── controller/              # REST API
    ├── service/                 # 商業邏輯
    ├── repository/              # 資料存取
    ├── entity/                  # JPA Entity
    ├── dto/                     # 請求 / 回應物件
    └── config/                  # Spring 設定
```

---

## Roadmap

這個 Repo 以小步驟持續現代化。每個模組都先建立測試、確認原本的行為，再升級與重構：

- [x] 移除設定檔中的帳密，改用環境變數
- [x] 新增根目錄 `.gitignore`，移除 IDE 設定與 log 檔
- [x] 新增根目錄 README 與技術索引
- [x] 補齊各模組 README（含教學說明與現代化紀錄）
- [x] 補上測試：baseline 測試鎖定原本的行為，學習測試說明各主題；資料庫、Redis 使用 Testcontainers
- [x] 統一升級至 Java 21 / Spring Boot 4.1 / Gradle 9
- [x] 舊寫法現代化（Constructor Injection、統一例外處理與 ProblemDetail、Flyway 管理資料表等）
- [x] 統一各模組 REST API 的 URL 設計規則（`/api` 開頭、複數名詞、kebab-case）
- [x] 新增 HTTP Service Client（`@HttpExchange`）練習模組，與 Spring_Feign_Client 對照
- [x] 每個模組都可以用 Docker 執行（Dockerfile；需要資料庫的模組附 Docker Compose）
- [x] 新增 Kafka 練習模組（Spring_Kafka：基礎 5 課）
- [x] 新增 Kafka 情境模組（Spring_Kafka2_Order / Shipping：兩個服務透過 Kafka 溝通）
- [x] 新增 Actuator 練習模組（health、metrics，搭配 Prometheus + Grafana）
- [x] 提供 [Postman Collection](postman)，可直接匯入測試各模組 API（每個請求都有測試腳本）
- [ ] 練習改為 Gradle 多模組專案（附 IntelliJ 設定與操作步驟）
- [ ] 新增 OAuth2 / Keycloak 練習（銜接 Spring_Security3 的 JWT）
