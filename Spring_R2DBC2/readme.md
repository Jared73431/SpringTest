# Spring R2DBC2 - Reactive 汽車管理 API

以 WebFlux + Spring Data R2DBC + PostgreSQL 實作的完整 CRUD API：多條件搜尋、Bean Validation、ProblemDetail 錯誤回應、R2DBC Auditing、Flyway。

R2DBC 的基礎（三種存取方式、主鍵由程式指定的陷阱、交易、沒有關聯對應）請先看 [Spring_R2DBC](../Spring_R2DBC)；同樣的 CRUD 用 JPA（阻塞式）實作，請見 [Spring_JPA](../Spring_JPA)，本文最後有兩者的對照。

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.5 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（WebFlux、Spring Data R2DBC、Validation） |
| 資料庫 | PostgreSQL（`r2dbc-postgresql` 驅動，資料庫名稱 `R2DBC2`） |
| 遷移 | Flyway（使用 JDBC 驅動） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Mockito、WebTestClient、StepVerifier、Testcontainers 2.0（PostgreSQL） |

## 架構

```text
Client
  ↓
CarController        @RestController（WebFlux），回傳 Mono / Flux
  ↓
CarService           CRUD 用 CarRepository；搜尋用 R2dbcEntityTemplate + Criteria
  ↓
CarRepository        ReactiveCrudRepository
  ↓
PostgreSQL           car 表由 Flyway 建立（V1 附 5 筆範例資料）
```

```text
src/main/java/com/example/demo/
├── controller/CarController
├── service/CarService
├── repository/CarRepository
├── entity/Car                       @CreatedDate / @LastModifiedDate
├── dto/CarRequest、CarResponse、CarSearchCriteria（record）
├── exception/CarNotFoundException、GlobalExceptionHandler
└── config/R2dbcConfig               @EnableR2dbcAuditing
```

## 執行方式

### Docker Compose（建議）

```bash
docker compose up --build                  # PostgreSQL + 應用程式，Flyway 建表並放入 5 筆範例資料
docker compose --profile tools up -d       # 另外啟動 pgAdmin：http://localhost:5050
docker compose down -v                     # 停止並刪除資料庫
```

> Windows 上 port 無法綁定時（Hyper-V / WSL 動態保留了 port），改用其他主機 port：`APP_PORT=8300 docker compose up --build`。

### 本機執行

需要本機的 PostgreSQL，並先建立資料庫：

```sql
CREATE DATABASE "R2DBC2";
```

```bash
./gradlew bootRun
```

### 測試

需要 Docker（Testcontainers 會自動啟動 PostgreSQL，不會連到本機的資料庫）。

```bash
./gradlew test
```

| 測試類別 | 類型 | 內容 |
|---|---|---|
| `CarApiTest` | 整合測試（Testcontainers） | 完整的 HTTP → 資料庫：搜尋的 SQL、驗證、錯誤回應、Auditing 的時間 |
| `CarControllerTest` | `@WebFluxTest` + `@MockitoBean` | 路由、參數綁定、驗證、錯誤回應 |
| `CarServiceTest` | Mockito | 查不到時的錯誤、請求如何套用到 Entity |
| `ConfigurationTest` | 整合測試 | Flyway 設定 |

> 搜尋條件組合出來的 SQL、Auditing 填入的時間，都需要真的資料庫才能驗證，所以放在整合測試。修正前的 Service 單元測試讓 mock 回傳已經有時間的物件，再斷言時間不是 null，測到的其實是 mock。

## API

| 方法 | URL | 說明 |
|---|---|---|
| GET | `/api/cars` | 全部汽車 |
| GET | `/api/cars/{id}` | 單一汽車，查不到時 404 |
| POST | `/api/cars` | 新增，回傳 201 與 `Location` |
| PUT | `/api/cars/{id}` | 修改 |
| DELETE | `/api/cars/{id}` | 刪除，回傳 204 |
| GET | `/api/cars/search` | 多條件搜尋，條件全部選填、以 AND 組合 |
| GET | `/api/cars/count?make=` | 計算某品牌的數量（不分大小寫） |

**新增 / 修改的請求：**

```json
{ "make": "Toyota", "model": "Camry", "year": 2022, "color": "White", "price": 25000.00 }
```

| 欄位 | 驗證 | 對應資料表 |
|---|---|---|
| `make`、`model` | 必填，最多 50 字 | `VARCHAR(50) NOT NULL` |
| `year` | 必填，1901～2100 | `CHECK (year > 1900 AND year <= 2100)` |
| `color` | 選填，最多 30 字 | `VARCHAR(30)` |
| `price` | 選填，大於 0，整數最多 8 位、小數最多 2 位 | `DECIMAL(10, 2)` |

**搜尋參數**：`make`、`model`（不分大小寫）、`year`、`minPrice`、`maxPrice`、`yearFrom`、`yearTo`。

```bash
curl 'localhost:8096/api/cars/search?make=toyota&yearFrom=2020'
curl 'localhost:8096/api/cars/search?yearFrom=2022&maxPrice=43000'   # Toyota Camry、Mercedes C-Class
curl 'localhost:8096/api/cars/count?make=toyota'                     # 1
```

**錯誤回應（ProblemDetail）：**

| 情況 | 狀態碼 | 範例 |
|---|---|---|
| 驗證失敗 | 400 | `{"status":400,"detail":"Invalid request content.","errors":{"year":"Year must be greater than 1900"}}` |
| id 不是數字、缺少必填參數 | 400 | |
| 查不到 | 404 | `{"status":404,"detail":"找不到汽車：99"}` |
| 違反資料庫限制 | 409 | `{"status":409,"detail":"資料違反資料庫的限制條件"}`（不帶資料庫的訊息） |
| 未預期的錯誤 | 500 | 不帶例外訊息 |

## 設定

| 設定 | 值 | 說明 |
|---|---|---|
| `server.port` | `8096` | |
| `spring.r2dbc.url` | `r2dbc:postgresql://${DB_HOST:localhost}:5432/R2DBC2` | |
| `spring.flyway.url` | `jdbc:postgresql://${DB_HOST:localhost}:5432/R2DBC2` | Flyway 只支援 JDBC |
| `logging.level.io.r2dbc.postgresql.QUERY` | （註解） | 需要查看 SQL 時改成 `DEBUG` |

---

## 設計說明

### 動態搜尋：Criteria 取代 if / else

修正前依序判斷參數，**只使用第一個有值的條件**：

```java
if (make != null)  return carService.getCarsByMake(make);    // make=Toyota&year=2021 → year 被忽略
if (model != null) return carService.getCarsByModel(model);
...
if (minPrice != null && maxPrice != null) ...                 // 只給 minPrice → 條件被忽略，回傳全部
```

現在查詢參數綁定到 `CarSearchCriteria` record，用 `Criteria` 把有給的條件全部用 AND 組合（Spring_R2DBC 第 2 課）：

```java
Criteria criteria = Criteria.empty();
if (c.make() != null)     criteria = criteria.and(where("make").is(c.make()).ignoreCase(true));
if (c.minPrice() != null) criteria = criteria.and(where("price").greaterThanOrEquals(c.minPrice()));
...
template.select(Car.class).matching(Query.query(criteria).sort(Sort.by("id"))).all();
```

計數使用同一套條件。修正前搜尋用 `ILIKE`、計數用 `=`，`make=toyota` 搜得到 1 台，計數卻是 0。

### 驗證要和資料表一致

驗證條件比資料表寬鬆時，錯誤會在**寫入資料庫時**才發生，變成 500：

| 修正前 | 問題 |
|---|---|
| `@Min(1900)`，資料表 `CHECK (year > 1900)` | 1900 通過驗證，寫入失敗。錯誤訊息本來就寫「必須大於 1900」，可見是 `@Min` 寫錯 |
| `make`、`model`、`color` 沒有長度限制，資料表 `VARCHAR(50)` / `VARCHAR(30)` | 太長的值寫入失敗 |
| `price` 沒有位數限制，資料表 `DECIMAL(10, 2)` | 超出範圍時寫入失敗 |

> PostgreSQL 的 R2DBC 驅動把「value too long」歸類為 bad grammar（`BadSqlGrammarException`），而不是 `DataIntegrityViolationException`，所以只靠例外處理無法正確回應 400，必須在驗證階段就擋下來。

### 錯誤處理：不要用 catch-all

修正前有 `@ExceptionHandler(Exception.class)`：

- 所有例外都變成 500。id 不是數字、缺少必填參數本來應該是 400。
- 把 `ex.getMessage()` 直接回傳，洩漏資料庫的 constraint 名稱等內部資訊。

現在 `GlobalExceptionHandler` 繼承 `ResponseEntityExceptionHandler`：Spring 的標準例外都會轉成 ProblemDetail，並使用正確的狀態碼；驗證失敗時額外加上每個欄位的錯誤訊息。只有 `DataIntegrityViolationException` 自己處理（409），詳細原因只寫進 log。

### Auditing 與「回應要和資料庫一致」

`@CreatedDate` / `@LastModifiedDate` 加上 `@EnableR2dbcAuditing`，取代 Service 中手動設定的 `LocalDateTime.now()`。

新增後直接回傳的是**記憶體中的物件**，不是重新從資料庫查出來的，所以要注意精度：

| 欄位 | 記憶體中 | 資料庫 | 處理 |
|---|---|---|---|
| 時間 | 奈秒（`.449447900`） | `TIMESTAMP` 只存到微秒（`.449448`） | Auditing 的 `DateTimeProvider` 截到微秒 |
| 價格 | 請求的寫法（`30000`） | `DECIMAL(10, 2)`（`30000.00`） | 寫入前 `setScale(2)` |

否則 POST 的回應和之後 GET 查到的資料會不一樣（`CarApiTest` 有測試驗證兩者相同）。JPA 也有同樣的問題。

---

## JPA vs R2DBC

同樣是 CRUD，[Spring_JPA](../Spring_JPA)（Spring MVC + JPA）與本模組（WebFlux + R2DBC）的差別：

| | Spring_JPA | Spring_R2DBC2 |
|---|---|---|
| 回傳型別 | `Book`、`List<Book>` | `Mono<CarResponse>`、`Flux<CarResponse>` |
| 執行緒 | 每個請求一條執行緒，等待資料庫時阻塞 | 不阻塞，少量執行緒處理大量請求 |
| 修改資料 | 交易內修改 Entity，自動 dirty checking | 修改後一定要 `save()` |
| 查不到 | `Optional.orElseThrow(...)` | `switchIfEmpty(Mono.error(...))` |
| 動態查詢 | Specification / Criteria API / QueryDSL | `R2dbcEntityTemplate` + `Criteria` |
| 時間戳記 | `@EnableJpaAuditing`（或 Hibernate 的 `@CreationTimestamp`） | `@EnableR2dbcAuditing` |
| 關聯 | `@OneToMany`、`@ManyToOne`、lazy loading | 沒有，自己查詢或 JOIN（見 Spring_R2DBC 第 7 課） |
| 建表 | `ddl-auto` 或 Flyway | 只能用 Flyway / Liquibase（仍需 JDBC 驅動） |
| 測試 | MockMvc、`@DataJpaTest` | WebTestClient、StepVerifier |

**怎麼選？**一般的 CRUD 服務，Spring MVC + JPA（Java 21 之後可以搭配 Virtual Threads）通常比較簡單，生態系也比較完整。以串流為核心，或整條呼叫鏈都已經是 Reactive 時才選 R2DBC（見 Spring_Webflux 第 11 課）。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Baseline 測試（Testcontainers）：鎖定修正前的行為，確認推測的問題，取代會連到本機資料庫的 `contextLoads` |
| 2 | Java 17 → 21、Spring Boot 3.5.3 → 3.5.16、Gradle 8.14.2 → 8.14.3 |
| 3 | Spring Boot 4.1.1、Gradle 9.8.0 |
| 4 | 設定清理：port 8096、資料庫 `R2DBC2`、移除 `baseline-on-migrate` |
| 5 | 錯誤處理改成 ProblemDetail，404、204 |
| 6 | 修正搜尋、計數與驗證的問題 |
| 7 | DTO 改成 record、移除 Lombok、R2DBC Auditing |
| 8 | Docker 重寫，刪除 Postman collection |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| 搜尋只看第一個條件 | `make=Toyota&year=2021` 忽略 year；只給 `minPrice` 回傳全部 |
| 搜尋與計數不一致 | 搜尋 `ILIKE`、計數 `=`：`make=toyota` 搜得到、計數為 0 |
| 驗證比資料表寬鬆 | 年份 1900、太長的字串、超出位數的價格都在寫入時失敗，回傳 500 |
| catch-all 例外處理 | 400 變成 500；回傳 `ex.getMessage()`，洩漏 constraint 名稱 |
| `baseline-on-migrate=true` + `baseline-version=0` | 已有 car 表的資料庫會再執行 V1，`CREATE INDEX` 失敗 |
| Docker 無法使用 | `openjdk:17-jdk-slim` 已下架；healthcheck 需要不存在的 actuator 與 curl；掛載不存在的 `./init-db`；PostgreSQL 開放 5432 與本機衝突 |
| 測試會連到本機資料庫 | `contextLoads` 需要本機的 `test2` 資料庫 |
| Service 時間戳記的測試沒有意義 | mock 回傳已經有時間的物件，再斷言時間不是 null |
| 預設印出所有 SQL | `logging.level...=DEBUG` |
| 沒有用到的程式 | 3 個 Repository 方法、`CarDto.id`、Entity 上的驗證註解、Controller 未使用的 `objectMapper` |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | 8080 | 8096 |
| 本機資料庫 | `test2` | `R2DBC2` |
| 搜尋 | 只用第一個有值的條件 | 所有條件以 AND 組合 |
| 計數 | 分大小寫 | 不分大小寫（與搜尋一致） |
| `model` 搜尋 | 分大小寫 | 不分大小寫 |
| 年份 1900 | 500（帶出資料庫訊息） | 400 |
| id 不是數字、缺少 make | 500 | 400 |
| 查不到 | 404，沒有內容 | 404 ProblemDetail |
| 驗證錯誤格式 | `{"message":"Validation failed","errors":{...}}` | ProblemDetail + `errors` |
| 違反資料庫限制 | 500（帶出資料庫訊息） | 409（不帶資料庫訊息） |
| 刪除 | 200 | 204 |
| 新增 | 201 | 201 + `Location` |
| 回應中的價格 | 新增時是請求的寫法（`30000`） | 一律 2 位小數（`30000.00`） |

### 升級時學到的事

- **Boot 4 只有 `flyway-core` 時遷移不會執行**，必須改用 `spring-boot-starter-flyway`（Spring_Flyway 模組也遇過）。
- **Boot 4 的測試套件搬家了**：`@WebFluxTest` 在 `org.springframework.boot.webflux.test.autoconfigure`，`@AutoConfigureWebTestClient` 在 `org.springframework.boot.webtestclient.autoconfigure`；mock 改用 `@MockitoBean`。
- **Testcontainers**：Boot 3.5.3 管理的 1.21.2 無法連到 Docker 29，baseline 階段暫時固定 1.21.4；Boot 4 使用 Testcontainers 2.0（artifact 改名為 `testcontainers-postgresql`，`PostgreSQLContainer` 移到 `org.testcontainers.postgresql`，不再有泛型）。
- **`ResponseEntityExceptionHandler` 與 `spring.webflux.problemdetails.enabled`**：自己繼承 `ResponseEntityExceptionHandler` 時，Spring Boot 就不會再註冊預設的 ProblemDetail 處理器，所以不需要那個設定。
- **回傳記憶體中的物件，要注意和資料庫的精度差異**：時間（奈秒 vs 微秒）與 `BigDecimal` 的小數位數，都是寫了「新增後再查詢應該相同」的測試才發現的。
