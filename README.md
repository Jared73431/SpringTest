# Spring Boot 技術練習集

從 Spring Boot 2.7 / Java 11 開始，一路練習到 Spring Boot 3.5 / Java 17 的 Spring 生態系範例集合。

每個資料夾都是**可以獨立執行的 Spring Boot 專案**，一個資料夾聚焦一個技術主題。
部分主題（Batch、WebSocket、Quartz、Security）保留了多個版本，可以看出同一個技術從入門到較完整實作的演進過程。

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
| [Spring_R2DBC](Spring_R2DBC) | R2DBC 入門 | 響應式資料庫存取 | 🐘 |
| [Spring_R2DBC2](Spring_R2DBC2) 📘 | R2DBC + WebFlux | 響應式 CRUD、Validation、全域例外處理、Flyway、Docker Compose | 🐘 |
| [Spring_Flyway](Spring_Flyway) 📘 | 資料庫版本控管 | SQL / Java Migration、Repeatable Migration、多路徑設定 | 🐘 |
| [Spring_Redis](Spring_Redis) 📘 | Redis 操作 | 五種資料結構、安全的 JSON 序列化、排行榜 / 限流（Lua）/ 分散式鎖、Testcontainers、RedisInsight ✅ 已現代化 | 🟥 |
| [Spring_Cache](Spring_Cache) 📘 | Spring Cache | `@Cacheable` / `@CachePut` / `@CacheEvict` 搭配 Redis | 🐘 🟥 |

### 批次與排程

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Batch](Spring_Batch) 📘 | Batch 入門 | 最小 Job / Step 設定 | — |
| [Spring_Batch-2](Spring_Batch-2) 📘 | Batch 基礎 | Tasklet、JobRepository 使用資料庫 | 🐘 |
| [Spring_Batch-3](Spring_Batch-3) 📘 | Chunk 處理 | CSV → Processor → DB、JobListener、排程觸發 | 🐘 |
| [Spring_Batch-4](Spring_Batch-4) 📘 | 多 Job | 自訂 Reader / Writer、多 Job 執行、非同步啟動 | 🐘 |
| [Spring_Batch-5](Spring_Batch-5) 📘 | 多 Job 整理版 | 延續 Batch-4 的結構整理 | 🐘 |
| [Spring_Batch-6](Spring_Batch-6) | Reader 比較 | `RepositoryItemReader` / `JpaPagingItemReader` / `JdbcCursorItemReader`、REST 觸發 Job、輸出 CSV | 🐘 |
| [Spring_Scheduleing](Spring_Scheduleing) | Spring 排程 | `@Scheduled` 的 fixedRate / fixedDelay / cron | — |
| [Spring_Quartz](Spring_Quartz) | Quartz 入門 | JobDetail / Trigger 基本設定 | — |
| [Spring_Quartz-2](Spring_Quartz-2) | Quartz 多任務 | 多個 Job 設定 | — |
| [Spring_Quartz-3](Spring_Quartz-3) | Quartz 持久化 | JDBC JobStore、REST API 動態新增 / 刪除 Job、JobFactory 注入 Spring Bean | 🐘 |

### Web / API

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_HelloWorld](Spring_HelloWorld) 📘 | 入門 | 最小 REST Controller（`GET /api/hello`）、`@WebMvcTest` 測試 ✅ 已現代化 | — |
| [Spring_Thymeleaf](Spring_Thymeleaf) | 模板引擎 | Thymeleaf 頁面渲染 | — |
| [Spring_Swagger](Spring_Swagger) 📘 | API 文件 | springdoc-openapi、統一回應格式、全域例外處理 | 🐘 |
| [Spring_Webflux](Spring_Webflux) | Reactive Streams | Mono / Flux 各種操作子示範 | — |
| [Spring_HttpClient](Spring_HttpClient) 📘 | 呼叫外部 API | RestClient（同步）與 WebClient（Reactive）對照、逾時、錯誤對應、WireMock 契約測試 ✅ 已現代化 | — |
| [Spring_Okhttp](Spring_Okhttp) 📘 | 呼叫外部 API | OkHttp 5：同步與非同步（Callback → CompletableFuture）、自訂 Interceptor、日誌遮蔽 Authorization、MockWebServer 測試 ✅ 已現代化 | — |

### 即時通訊

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Websocket](Spring_Websocket) | WebSocket 入門 | Echo Server | — |
| [Spring_Websocket2](Spring_Websocket2) | STOMP 聊天室 | STOMP + SockJS、訊息存 DB、Redis 快取 | 🐘 🟥 |
| [Spring_Websocket3](Spring_Websocket3) | 多聊天室 | 聊天室切換、私訊（`/queue`）、上下線事件 | 🐘 |
| [Spring_Websocket4](Spring_Websocket4) | 原生 WebSocket 聊天室 | `WebSocketHandler`、聊天室 / 私訊、檔案與圖片上傳 | 🐘 |

### 安全性

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Security](Spring_Security) | Security 入門 | 預設登入頁、設定檔帳密 | — |
| [Spring_Security2](Spring_Security2) | Security 6 設定 | `WebSecurityCustomizer` 忽略特定路徑（舊版 Adapter 寫法保留為註解對照） | — |
| [Spring_Security3](Spring_Security3) | 舊版設定（Boot 2.7） | `WebSecurityConfigurerAdapter`、Stateless Session | — |

### 微服務（Spring Cloud）

| 模組 | 主題 | 重點 | 需要 |
|---|---|---|---|
| [Spring_Eureka](Spring_Eureka) 📘 | 服務註冊中心 | Eureka Server、註冊 / 心跳 / 自我保護原理、Eureka vs K8S Service ✅ 已現代化 | — |
| [Spring_Eureka_Provider](Spring_Eureka_Provider) 📘 | 服務提供者 | 註冊到 Eureka 的 REST CRUD（`/api/books`），`/api/hello` 回傳實例 id ✅ 已現代化 | 🐘 |
| [Spring_Eureka_Client](Spring_Eureka_Client) 📘 | 服務消費者 | 以服務名稱呼叫（Eureka + OpenFeign + LoadBalancer）、Round Robin 負載平衡測試、Docker Compose 啟動兩個 Provider ✅ 已現代化 | — |
| [Spring_Feign_Server](Spring_Feign_Server) 📘 | Feign 被呼叫端 | 一般 REST CRUD 服務（`/api/books`），與 Spring_JPA 共用 book 資料表 ✅ 已現代化 | 🐘 |
| [Spring_Feign_Client](Spring_Feign_Client) 📘 | Feign 呼叫端 | OpenFeign、自訂 ErrorDecoder（錯誤原樣轉回）、逾時 / log 設定、WireMock 測試、Docker Compose 一次啟動三個服務、Feign vs HTTP Service Client ✅ 已現代化 | — |
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

各模組建立的時間不同，目前版本並不一致。現代化的目標版本為 **Spring Boot 4.1 + Java 21**，已完成的模組標示 ✅：

| Spring Boot | Java | 模組 |
|---|---|---|
| **4.1.x** | **21** | HelloWorld ✅、JPA ✅、JPA2 ✅、Feign_Server ✅、Feign_Client ✅、Eureka ✅、Eureka_Provider ✅、Eureka_Client ✅、Gateway_Server ✅、Gateway_Client ✅、HttpClient ✅、Okhttp ✅、Redis ✅ |
| 2.7.x | 11 / 17 | Security3 |
| 3.0.x | 17 | Quartz 1~3、R2DBC、Scheduleing、Security、Security2 |
| 3.2 ~ 3.5 | 17 | 其餘模組 |

- Build Tool：Gradle（每個模組各自附 Gradle Wrapper）
- Database：PostgreSQL
- Cache：Redis

---

## 快速開始

### 1. 環境需求

- JDK 21（已現代化的模組，標示 ✅）
- JDK 17（其餘模組；Spring Boot 2.7 的模組也可以用 JDK 11）
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
| `SECURITY_USER_PASSWORD` | `password` | Spring_Security 的登入密碼 |
| `PGADMIN_PASSWORD` | `admin` | Spring_R2DBC2 docker compose 的 pgAdmin 密碼 |

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

這個 Repo 正在進行現代化，會以小步驟持續改善：

- [x] 移除設定檔中的帳密，改用環境變數
- [x] 新增根目錄 `.gitignore`，移除 IDE 設定與 log 檔
- [x] 新增根目錄 README 與技術索引
- [ ] 補齊各模組 README
- [ ] 補上核心邏輯的單元測試 / 整合測試
- [ ] 統一升級至 Java 21 / Spring Boot 3.x
- [ ] 舊寫法現代化（Constructor Injection、統一例外處理、Logging 等）
- [ ] 統一各模組 REST API 的 URL 設計規則
- [ ] 提供 Postman Collection，可直接匯入測試各模組 API
- [ ] 練習改為 Gradle 多模組專案（附 IDE 操作步驟）
- [ ] 新增 HTTP Service Client（`@HttpExchange`）練習模組，與 Spring_Feign_Client 對照
