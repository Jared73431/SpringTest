# Spring Feign Server - 被 Feign 呼叫的書籍服務

[Spring_Feign_Client](../Spring_Feign_Client) 透過 OpenFeign 呼叫的一方，本身是一般的 Spring Boot REST CRUD 服務，**不需要任何 Feign 相關設定**。

> Feign 的寫法、錯誤處理與 Docker Compose 一次啟動兩個服務的方式，請見 [Spring_Feign_Client 的 README](../Spring_Feign_Client/readme.md)。

這是本 Repo 早期（2022）的練習，已完成現代化（Spring Boot 2.7 → 4.1），過程中修正了更新功能的 Bug 並重新設計 API，詳見「[現代化紀錄](#現代化紀錄)」。

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
Spring_Feign_Client（:8083）
  ↓  HTTP / JSON（Feign）
BookController / HelloController   /api/books、/api/hello
  ↓
BookService                        商業邏輯、交易邊界（@Transactional）
  ↓
BookRepo                           Spring Data JPA
  ↓
PostgreSQL                         資料庫 test 的 book 資料表
```

程式結構與 [Spring_JPA](../Spring_JPA) 相同，**也共用同一張 `book` 資料表**（同一個資料庫、相同的 Entity 定義）。差別只在多了一個連線測試用的 `GET /api/hello`。

## 執行方式

需要 JDK 21 與 PostgreSQL（資料庫名稱 `test`），連線資訊的設定方式與 Spring_JPA 相同，請見 [Spring_JPA「資料庫連線設定」](../Spring_JPA/readme.md#資料庫連線設定)。

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- 預設 port：`8082`（Client 預設就是呼叫 `http://localhost:8082`）
- 要連同 Client 與資料庫一起用 Docker 啟動，請見 [Spring_Feign_Client「Docker」](../Spring_Feign_Client/readme.md#docker)

## API

Base URL：`http://localhost:8082`

| Method | Path | 說明 | 成功 | 失敗 |
|---|---|---|---|---|
| `GET` | `/api/hello` | 連線測試，回傳 `Hello` | `200` | |
| `GET` | `/api/books` | 查詢全部 | `200` | |
| `GET` | `/api/books/{id}` | 查詢一筆 | `200` | `404` |
| `POST` | `/api/books` | 新增 | `201` + `Location` header | `400` |
| `PUT` | `/api/books/{id}` | 修改 | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | 刪除 | `204` | `404` |

Request Body、curl 範例與錯誤格式（ProblemDetail）與 Spring_JPA 相同，請見 [Spring_JPA「API」](../Spring_JPA/readme.md#api)。

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**。測試使用 Testcontainers 啟動臨時的 PostgreSQL 容器，不會連到本機開發用的資料庫。

`BookControllerIntegrationTest` 透過真實 HTTP 呼叫測試所有 API，包含成功、驗證失敗、404、刪除，以及舊 URL 已不再提供。

## 已知限制（刻意保留）

與 Spring_JPA 相同的練習用設計：

| 項目 | 說明 |
|---|---|
| `isbn` 使用 `Integer` | 無法存放真實的 13 碼 ISBN（超過 Integer 範圍會回 400） |
| `cost` 使用 `double` | 金額計算可能有浮點誤差，實務上建議使用 `BigDecimal` |
| `ddl-auto=update` | 由 Hibernate 自動建表；實務上建議使用 Flyway 管理 Schema |

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 整合測試，固定原本行為（包含 Bug） |
| 2 | Gradle 7.5.1 → 8.14.3 |
| 3 | Java 11 → 21、Spring Boot 2.7.5 → 3.5.16、`javax` → `jakarta` |
| 4 | Spring Boot 3.5.16 → 4.1.1（Hibernate 7）、Gradle → 9.8.0 |
| 5 | Constructor Injection、Entity 移除 `@Data`、修正更新 Bug、查無資料回 404 |
| 6 | 重新設計為 REST API（`/api/books`、DTO、新增 DELETE、`/hello` → `/api/hello`） |
| 7 | 新增 Dockerfile，由 Client 的 docker-compose 一起啟動 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 連線測試 | `GET /hello` | `GET /api/hello` |
| 查詢全部 | `GET /findall` | `GET /api/books` |
| 新增 | `POST /saveBook?ISBN=..&title=..`，回 `200` 無 body | `POST /api/books`（JSON），回 `201` + 新資料 |
| 查詢一筆 | `GET /getOneBook/{ID}` | `GET /api/books/{id}` |
| 修改 | `POST /updateBook?ID=..`，**實際上會新增一筆重複資料** | `PUT /api/books/{id}`，更新原資料 |
| 刪除 | 無（Service 只有 `// TODO`） | `DELETE /api/books/{id}` |
| 查無資料 | `500` | `404` + ProblemDetail |

> 修正前因「更新」而多出來的重複資料，不會自動刪除。

升級過程學到的事（`jakarta`、Hibernate 6.6 merge 行為、Boot 4 測試模組化等）與 Spring_JPA 相同，請見 [Spring_JPA「升級時學到的事」](../Spring_JPA/readme.md#升級時學到的事)。
