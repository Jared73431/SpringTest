# Spring Data JPA - Book CRUD API

使用 Spring Data JPA + PostgreSQL 實作書籍的 CRUD REST API，並附有 Dockerfile 與 docker-compose 設定。

這是本 Repo 早期（2022）的 JPA 練習，已完成現代化（Spring Boot 2.7 → 4.1），過程中修正了更新功能的 Bug 並重新設計 API，詳見下方「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Hibernate | 7.4 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| Database | PostgreSQL |
| 測試 | JUnit 5、Testcontainers 2.0、TestRestTemplate |

## 架構

```text
Client
  ↓  JSON
BookController        /api/books，Request / Response 使用 DTO（Java Record）
  ↓
BookService           商業邏輯、交易邊界（@Transactional）
  ↓
BookRepo              Spring Data JPA（JpaRepository）
  ↓
PostgreSQL            資料表由 Hibernate 自動建立（ddl-auto=update）

GlobalExceptionHandler（@RestControllerAdvice）統一將錯誤轉為 ProblemDetail
```

## 專案結構

```text
src/main/java/com/example/demo/
├── controller/BookController.java        # REST API
├── dto/BookRequest.java                  # 新增 / 修改的 Request（含驗證）
├── dto/BookResponse.java                 # 回應格式
├── entity/Book.java                      # JPA Entity
├── exception/BookNotFoundException.java
├── exception/GlobalExceptionHandler.java # 404 → ProblemDetail
├── repository/BookRepo.java
└── service/BookService.java, impl/BookServiceImpl.java
```

## 執行方式

### 本機執行

需要 JDK 21 與 PostgreSQL（資料庫名稱 `test`）。

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- 預設 port：`8081`
- 資料庫帳密由環境變數 `DB_USERNAME` / `DB_PASSWORD` 提供（預設 `postgres` / `postgres`）

### Docker

```bash
./gradlew bootJar
docker compose up --build
```

- 使用 `docker` profile，port `8015`
- 需要事先存在名為 `database_net_postgres` 的 Docker network，且 PostgreSQL 容器在該網路中的名稱為 `postgres`

## API

Base URL：`http://localhost:8081/api/books`

| Method | Path | 說明 | 成功 | 失敗 |
|---|---|---|---|---|
| `GET` | `/api/books` | 查詢全部 | `200` | |
| `GET` | `/api/books/{id}` | 查詢一筆 | `200` | `404` |
| `POST` | `/api/books` | 新增 | `201` + `Location` header | `400` |
| `PUT` | `/api/books/{id}` | 修改 | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | 刪除 | `204` | `404` |

### Request Body（`POST` / `PUT`）

所有欄位皆為必填：

```json
{
  "isbn": 12345,
  "title": "Java",
  "author": "Tom",
  "year": 2020,
  "publisher": "OReilly",
  "cost": 450.5
}
```

### 範例

```bash
# 新增
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"isbn":12345,"title":"Java","author":"Tom","year":2020,"publisher":"OReilly","cost":450.5}'

# 查詢
curl http://localhost:8081/api/books/1

# 修改
curl -X PUT http://localhost:8081/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"isbn":12345,"title":"Java 2nd","author":"Tom","year":2021,"publisher":"OReilly","cost":500}'

# 刪除
curl -X DELETE http://localhost:8081/api/books/1
```

### 錯誤格式

所有錯誤回應皆使用 [RFC 9457 ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457)（`Content-Type: application/problem+json`）：

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Book not found: id=999",
  "instance": "/api/books/999"
}
```

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**。測試使用 Testcontainers 自動啟動臨時的 PostgreSQL 容器，測試結束後自動刪除，不會連到本機開發用的資料庫。

`BookControllerIntegrationTest` 透過真實 HTTP 呼叫測試所有 API，包含成功、驗證失敗、404、刪除等情境。

## 已知限制（刻意保留）

| 項目 | 說明 |
|---|---|
| `isbn` 使用 `Integer` | 練習用設計，無法存放真實的 13 碼 ISBN（超過 Integer 範圍會回 400） |
| `cost` 使用 `double` | 金額計算可能有浮點誤差，實務上建議使用 `BigDecimal` |
| `ddl-auto=update` | 由 Hibernate 自動建表；實務上建議使用 Flyway 管理 Schema（參考 [Spring_Flyway](../Spring_Flyway)） |
| 400 錯誤訊息 | 目前只顯示 `Invalid request content.`，未列出是哪個欄位驗證失敗 |

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 整合測試，固定原本行為（包含 Bug） |
| 2 | Gradle 7.5.1 → 8.14.3 |
| 3 | Java 11 → 21、Spring Boot 2.7.5 → 3.5.16、`javax` → `jakarta`、Hibernate 5 → 6 |
| 4 | Spring Boot 3.5.16 → 4.1.1（Hibernate 7）、Gradle → 9.8.0、Testcontainers 1.x → 2.x |
| 5 | Field Injection → Constructor Injection、Entity 移除 `@Data` |
| 6 | 修正更新 Bug、查無資料回 404（`@RestControllerAdvice` + ProblemDetail） |
| 7 | 重新設計為 REST API（`/api/books`、DTO、新增 DELETE） |
| 8 | 修正 Docker 設定（`eclipse-temurin:21-jre`、啟用 `docker` profile） |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 查詢全部 | `GET /findall` | `GET /api/books` |
| 新增 | `POST /saveBook?ISBN=..&title=..`，回 `200` 無 body | `POST /api/books`（JSON），回 `201` + 新資料 |
| 查詢一筆 | `GET /getOneBook/{ID}` | `GET /api/books/{id}` |
| 修改 | `POST /updateBook?ID=..`，**實際上會新增一筆重複資料** | `PUT /api/books/{id}`，更新原資料 |
| 刪除 | 無 | `DELETE /api/books/{id}` |
| 查無資料 | `500` | `404` + ProblemDetail |
| Docker | Port 對不上（程式 8081、對外 8015），且 image 已停止維護 | 啟用 `docker` profile（8015），改用 `eclipse-temurin:21-jre` |

### 升級時學到的事

- **`javax` → `jakarta`**：Java EE 移交 Eclipse 基金會後更名為 Jakarta EE，Spring Boot 3 起全面改用 `jakarta.*`。
- **Hibernate 6 自動偵測 Dialect**：不需要再設定 `hibernate.dialect`（否則會出現警告 `HHH90000025`）。
- **Hibernate 6.6 的 merge 行為改變**：對「資料庫中不存在的 id」執行 `save()`，Hibernate 5 會默默新增一筆，Hibernate 6.6 則拋出 `StaleObjectStateException`。舊行為正是造成更新 Bug 的原因之一。
- **Spring Boot 4 測試模組化**：`TestRestTemplate` 移至 `spring-boot-resttestclient`，需加上 `@AutoConfigureTestRestTemplate` 與 `spring-boot-restclient` 依賴。
- **JPA Entity 不要用 `@Data`**：它產生的 `equals` / `hashCode` 使用所有欄位，Entity 放入 `Set` 或 id 變動時容易出錯。
- **Constructor Injection**：依賴一目了然、可宣告為 `final`，也能在不啟動 Spring 的情況下撰寫單元測試。
- **測試不要連本機資料庫**：原本的 `contextLoads()` 會連到 `localhost:5432` 並觸發 `ddl-auto=update`；改用 Testcontainers 後測試環境與開發資料完全隔離。
