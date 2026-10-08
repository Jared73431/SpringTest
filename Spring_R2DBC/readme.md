# Spring R2DBC - Reactive 資料庫存取

R2DBC（Reactive Relational Database Connectivity）是 JDBC 的 Reactive 版本：查詢不會阻塞執行緒，結果以 `Mono` / `Flux` 回傳。

這個模組以同一個 Product 資料表，比較 Spring Data R2DBC 的**三種存取方式**，並整理 R2DBC 和 JPA 最不一樣的幾個地方：主鍵由程式指定時的陷阱、自訂型別轉換、Reactive 交易、沒有關聯對應。另外附一個完整的 `/api/products` CRUD API。

完整的 Reactive CRUD API（搜尋、驗證、Auditing）請見 [Spring_R2DBC2](../Spring_R2DBC2)；Reactive 本身的觀念（Mono / Flux、訂閱、Scheduler）請先看 [Spring_Webflux](../Spring_Webflux)。

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.0 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（WebFlux、Spring Data R2DBC） |
| 資料庫 | PostgreSQL（`r2dbc-postgresql` 驅動，資料庫名稱 `R2DBC`） |
| 遷移 | Flyway（使用 JDBC 驅動） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、StepVerifier、WebTestClient、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_RepositoryTest](src/test/java/com/example/demo/lesson/L01_RepositoryTest.java) | `ReactiveCrudRepository`：方法名稱產生查詢、分頁、`@Query`、`@Modifying` |
| 2 | [L02_EntityTemplateTest](src/test/java/com/example/demo/lesson/L02_EntityTemplateTest.java) | `R2dbcEntityTemplate` + `Criteria`：動態查詢、批次更新與刪除 |
| 3 | [L03_DatabaseClientTest](src/test/java/com/example/demo/lesson/L03_DatabaseClientTest.java) | `DatabaseClient`：原生 SQL、統計、取得產生的 id、**SQL Injection** |
| 4 | [L04_PersistableTest](src/test/java/com/example/demo/lesson/L04_PersistableTest.java) | 主鍵由程式指定時，`save()` **默默地沒有寫入**；`template.insert()`、`Persistable`、`@Version` 三種解法 |
| 5 | [L05_ConverterTest](src/test/java/com/example/demo/lesson/L05_ConverterTest.java) | 自訂 Converter：enum ↔ 一個字元的代碼 |
| 6 | [L06_TransactionTest](src/test/java/com/example/demo/lesson/L06_TransactionTest.java) | Reactive 交易：`@Transactional` 與 `TransactionalOperator` |
| 7 | [L07_RelationshipTest](src/test/java/com/example/demo/lesson/L07_RelationshipTest.java) | 沒有 `@ManyToOne`：N+1、批次載入、JOIN |

每一課都是一個測試類別，使用 Testcontainers 啟動的 PostgreSQL，每個測試前重新放入同樣的 5 筆商品。

## 專案結構

```text
src/main/java/com/example/demo/
├── controller/ProductController   /api/products（WebFlux）
├── service/ProductService         回傳 Mono / Flux，查不到時轉成 404
├── repository/ProductRepo         ReactiveCrudRepository
├── entity/Product                 id 由資料庫產生（SERIAL）
├── dto/ProductRequest、ProductResponse（record）
└── exception/ProductNotFoundException（ErrorResponseException → ProblemDetail）

src/main/resources/db/migration/   Flyway：V1 product、V2 金額改 NUMERIC、V3 category（課程用）

src/test/java/com/example/demo/
├── ProductApiTest、ConfigurationTest
└── lesson/                        第 1～7 課，以及課程專用的 Entity、Repository、Converter 設定
```

課程專用的程式（`Category`、`ProductQueryRepository`、Converter 設定…）放在**測試程式碼**中。它們位於 `com.example.demo` 底下，執行測試時會被 Spring 掃描到；正式程式碼不需要為了示範而多出用不到的類別。

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers 會自動啟動 PostgreSQL，不會連到本機的資料庫）。

```bash
./gradlew test                       # 全部
./gradlew test --tests '*L04*'       # 只跑第 4 課
```

### Docker Compose

```bash
docker compose up --build                  # PostgreSQL + 應用程式，Flyway 自動建表
docker compose --profile tools up -d       # 另外啟動 pgAdmin：http://localhost:5050
docker compose down -v                     # 停止並刪除資料庫
```

> **Windows 上 port 無法綁定？**Hyper-V / WSL 會在開機時動態保留一段 port（用 `netsh interface ipv4 show excludedportrange protocol=tcp` 查看），8095 可能剛好在範圍內。可以改用其他主機 port：`APP_PORT=8300 docker compose up --build`。

### 本機執行

需要本機的 PostgreSQL，並先建立資料庫：

```sql
CREATE DATABASE "R2DBC";
```

```bash
./gradlew bootRun
```

### API

| 方法 | URL | 說明 |
|---|---|---|
| GET | `/api/products` | 全部商品 |
| GET | `/api/products/{id}` | 單一商品，查不到時 404 ProblemDetail |
| POST | `/api/products` | 新增（`{"description": "...", "price": 1200.50}`），回傳 201 與 `Location` |
| PUT | `/api/products/{id}` | 修改 |
| DELETE | `/api/products/{id}` | 刪除，回傳 204 |

```bash
curl -H 'Content-Type: application/json' -d '{"description":"Keyboard","price":1200.50}' localhost:8095/api/products
curl localhost:8095/api/products
curl localhost:8095/api/products/99      # {"detail":"找不到商品：99","status":404,...}
```

## 設定

| 設定 | 值 | 說明 |
|---|---|---|
| `server.port` | `8095` | |
| `spring.r2dbc.url` | `r2dbc:postgresql://${DB_HOST:localhost}:5432/R2DBC` | 應用程式用 R2DBC 存取 |
| `spring.r2dbc.pool.*` | initial-size 5、max-size 10 | 連線池 |
| `spring.flyway.url` | `jdbc:postgresql://${DB_HOST:localhost}:5432/R2DBC` | Flyway 只支援 JDBC，另外提供 JDBC 連線 |
| `spring.webflux.problemdetails.enabled` | `true` | 錯誤回應使用 ProblemDetail |

---

## R2DBC 教學

### R2DBC 和 JDBC / JPA 的差別

| | JDBC / JPA | R2DBC |
|---|---|---|
| 等待資料庫回應時 | 執行緒阻塞 | 不阻塞，結果以 Mono / Flux 回傳 |
| 搭配 | Spring MVC（或 Virtual Threads） | Spring WebFlux |
| 關聯（`@OneToMany`）、lazy loading | ✅ | ❌（第 7 課） |
| 一級快取、dirty checking | ✅（JPA） | ❌ 修改後一定要 `save()` |
| 自動建表（`ddl-auto`） | ✅（JPA） | ❌ 用 Flyway / Liquibase |
| Flyway / Liquibase | 直接使用 | 仍然要用 **JDBC** 驅動執行遷移 |

所以這個專案同時有兩個驅動：`r2dbc-postgresql` 給應用程式用，`postgresql`（JDBC）只給 Flyway 用。

> 修正前用 R2DBC 搭配 Spring MVC，在 Service 裡呼叫 `.block()` 把結果變回同步。這樣每個請求仍然佔用一條執行緒等待資料庫，等於同時承擔兩邊的複雜度，卻沒有得到 Reactive 的好處。R2DBC 要搭配 WebFlux，從 Controller 到資料庫全程都是 Mono / Flux。

### 三種存取方式（第 1～3 課）

| | Repository | R2dbcEntityTemplate | DatabaseClient |
|---|---|---|---|
| 程式碼量 | 最少 | 中等 | 最多 |
| 動態條件 | ❌（方法名稱固定） | ✅ `Criteria` | ✅ 自己組 SQL |
| 結果型別 | Entity | Entity | 任意（自己對應） |
| JOIN / GROUP BY | 只能寫在 `@Query` | ❌ | ✅ |
| 適合 | 固定條件的 CRUD | 搜尋畫面（有填的條件才加入） | 報表、統計、JOIN |

三種方式可以在同一個專案中混用：先用 Repository，不夠用時再往下一層。

**Repository（第 1 課）：**

```java
Flux<Product> findByDescriptionContainingIgnoreCase(String keyword);   // 依方法名稱產生 SQL
Flux<Product> findAllBy(Pageable pageable);                            // 分頁：回傳 Flux，沒有 Page

@Modifying
@Query("UPDATE product SET price = price * :rate WHERE price >= :min")
Mono<Integer> raisePrice(BigDecimal rate, BigDecimal min);             // 回傳受影響的筆數
```

> R2DBC 沒有 `Page`（不會自動 count），需要總筆數時另外呼叫 `count()`。

**R2dbcEntityTemplate（第 2 課）：**動態條件，參數是 null 就不加入：

```java
Criteria criteria = Criteria.empty();
if (keyword != null)  criteria = criteria.and(where("description").like("%" + keyword + "%").ignoreCase(true));
if (minPrice != null) criteria = criteria.and(where("price").greaterThanOrEquals(minPrice));
template.select(Product.class).matching(Query.query(criteria)).all();
```

**DatabaseClient（第 3 課）：**自己寫 SQL，**參數一定要用 `bind`**：

```java
// ❌ 輸入 x' OR '1'='1 就會查出全部資料（SQL Injection）
databaseClient.sql("SELECT * FROM product WHERE description = '" + input + "'")
// ✅ 值與 SQL 分開送給資料庫
databaseClient.sql("SELECT * FROM product WHERE description = :description").bind("description", input)
```

### 主鍵由程式指定時，save() 默默地沒有寫入（第 4 課）

`save()` 用 `isNew()` 決定 INSERT 或 UPDATE，預設的判斷是「**id 是 null 才是新的**」。

- Product 的 id 由資料庫產生（SERIAL），新物件的 id 是 null，所以沒有問題。
- 主鍵由程式指定時（代碼、UUID…），id 一開始就有值，`save()` 會執行 **UPDATE**，更新 0 筆。

**目前版本的 Spring Data R2DBC 不會報錯**：`save()` 回傳了物件，看起來成功了，資料卻沒有寫入。較舊的版本至少會丟出 `Row with Id [...] does not exist`。

| 解法 | 說明 |
|---|---|
| `template.insert(entity)` | 明確指定 INSERT，最直接 |
| 實作 `Persistable` | 自己決定 `isNew()`，用 `@Transient` 欄位記錄是不是新建立的 |
| 加上 `@Version` | 有版本欄位時改用「version 是不是 null」判斷，順便得到樂觀鎖 |

> 修正前的 Product 實作了 `Persistable`，但它的 id 是資料庫產生的，根本不需要；而且 `isNew()` 被 Jackson 當成 getter，API 回應多出 `new`、`newProduct` 兩個欄位。

### 自訂型別轉換（第 5 課）

R2DBC 沒有 JPA 的 `@Enumerated` / `@Converter`。預設 enum 以名稱（`"ACTIVE"`）存入；要存成代碼（`'A'`）時，註冊 `@WritingConverter` 與 `@ReadingConverter`：

```java
@Bean
R2dbcCustomConversions r2dbcCustomConversions() {
    return R2dbcCustomConversions.of(PostgresDialect.INSTANCE,
            List.of(new CategoryStatusWritingConverter(), new CategoryStatusReadingConverter()));
}
```

查詢條件中的 enum（`where("status").is(SUSPENDED)`）也會經過 Converter。

### Reactive 交易（第 6 課）

| 寫法 | 說明 |
|---|---|
| `@Transactional` | 寫在回傳 Mono / Flux 的方法上，和 Spring MVC 相同 |
| `TransactionalOperator` | `operator.transactional(mono)`，用程式指定範圍 |

Reactive 的交易包住的是「回傳的 Mono / Flux」：訂閱時開始，完成或錯誤時 commit / rollback。交易資訊放在 Reactor Context，而不是 ThreadLocal（因為執行緒會切換，見 Spring_Webflux 第 9 課）。所以在方法裡另外 `.subscribe()` 的操作**不在交易內**。

### 沒有關聯對應（第 7 課）

R2DBC 的 Entity 只存外鍵（`categoryCode`），沒有 `Category category` 這樣的物件屬性：

| 做法 | 查詢次數（5 個商品） |
|---|---|
| 每個商品 `flatMap` 查一次分類 | 1 + 5（**N+1**） |
| 收集代碼後 `findAllById` 一次查完，`collectMap` 組合 | 1 + 1 |
| `DatabaseClient` 寫 JOIN | 1 |

沒有關聯對應看起來不方便，但每一次查詢都是明確寫出來的；JPA 的 lazy loading 則常在不知不覺中產生 N+1。

---

## 延伸閱讀

- [從零開始 Reactive Programming - Spring](https://ithelp.ithome.com.tw/users/20141418/ironman/4617)（2021 iThome 鐵人賽）Day 25～26：R2DBC 入門、Repository、自訂 Converter、ID 處理。文中使用的 MySQL 驅動 `dev.miku:r2dbc-mysql` 已停止維護，後續由 `io.asyncer:r2dbc-mysql` 接手。
- [Spring Data R2DBC 官方文件](https://docs.spring.io/spring-data/relational/reference/r2dbc.html)

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Baseline 測試（Testcontainers）：鎖定修正前的行為，取代會連到本機資料庫的 `contextLoads` |
| 2 | Java 17 → 21、Spring Boot 3.0.1 → 3.5.16、Gradle 7.6 → 8.14.3 |
| 3 | Spring Boot 4.1.1、Gradle 9.8.0 |
| 4 | Flyway 建表、修正無效的設定、port 8095、資料庫改名 `R2DBC` |
| 5 | 改用 WebFlux，`/api/products` REST API，ProblemDetail |
| 6 | 第 1～7 課 |
| 7 | Dockerfile、docker compose（pgAdmin 為選用 profile） |

### 修正前發現的問題

| 問題 | 說明 | 處理 |
|---|---|---|
| R2DBC 搭配 Spring MVC + `.block()` | 每個請求仍佔用一條執行緒等待資料庫，失去 Reactive 的意義 | 改用 WebFlux，全程 Mono / Flux |
| 連線池設定沒有生效 | `spring.pool.*` 前綴錯誤，應為 `spring.r2dbc.pool.*` | 修正，並用測試證明有綁定 |
| `r2dbcs:` + `sslMode=disable` | 啟用 SSL 又關閉 SSL，互相矛盾 | 改為 `r2dbc:` |
| API 回應多出 `new`、`newProduct` | 不必要的 `Persistable`，`isNew()` 被 Jackson 輸出 | 移除 `Persistable` |
| 查不到時回傳 200 且沒有內容 | Service 回傳 null | 404 ProblemDetail |
| 沒有 schema | 要在本機手動建表 | Flyway |
| 金額用 `Double` | 浮點數誤差 | `NUMERIC(10,2)` + `BigDecimal` |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Web | Spring MVC（Tomcat） | WebFlux（Netty） |
| Port | 8080 | 8095 |
| 本機資料庫 | `test` | `R2DBC` |
| 查詢全部 | `GET /findAll` | `GET /api/products` |
| 新增 | `POST /SaveProduct?description=&price=`，200 沒有內容 | `POST /api/products`（JSON），201 + `Location` |
| 查詢單筆 | `GET /getbyId/{Id}`，查不到 200 沒有內容 | `GET /api/products/{id}`，查不到 404 |
| 修改、刪除 | 無 | `PUT`、`DELETE`（204） |
| 回應欄位 | `id`、`description`、`price`、`new`、`newProduct` | `id`、`description`、`price` |

### 升級時學到的事

- **Gradle 7.6 無法載入 Boot 3.5 的 plugin**：要先把 wrapper 升級到 8.14，再修改 Boot 的版本。
- **Spring Data R2DBC 4 預設會把識別字加上引號**：`@Column("ID")` 在 Boot 3 產生 `ID`，PostgreSQL 會自動轉成小寫，所以對得上；Boot 4 產生 `"ID"`，加了引號就區分大小寫，對不上資料庫中的 `id`，所有查詢都變成 500。欄位名稱改成和資料庫一致的小寫。
- **`save()` 更新 0 筆不再報錯**：第 4 課第一版的測試預期會出錯，實際上 `save()` 回傳成功、資料表卻是空的。
- **Windows 會動態保留 port**：Hyper-V / WSL 開機時保留的範圍（例如 8054～8153）每次可能不同，docker compose 因此加上 `APP_PORT` 覆蓋主機 port。
- **測試不要假設資料表是空的**：Flyway 的設定測試原本斷言 product 表沒有資料，在課程測試之後執行就會失敗（測試結果依執行順序而不同），改成檢查資料表是否存在。
