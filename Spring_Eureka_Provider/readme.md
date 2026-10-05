# Spring Eureka Provider - 註冊到 Eureka 的書籍服務

服務提供者：啟動後以 `service-provider` 的名稱註冊到 [Spring_Eureka](../Spring_Eureka)，讓 [Spring_Eureka_Client](../Spring_Eureka_Client) 用服務名稱找到並呼叫它。

> Eureka 的原理（註冊、心跳、自我保護、與 K8S 的比較）請見 [Spring_Eureka 的 README](../Spring_Eureka/readme.md)。

API 與 [Spring_Feign_Server](../Spring_Feign_Server) 相同（同樣與 Spring_JPA 共用 `book` 資料表），差別在於：

- 加上 Eureka Client，啟動時自動註冊
- `GET /api/hello` 回傳**實例 id**（`Hello from 172.25.0.5:8084`），開多台時可以看出請求送到哪一台

這是本 Repo 早期的練習，已完成現代化（Spring Boot 3.0 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood） |
| Hibernate | 7.4 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| Database | PostgreSQL |
| 測試 | JUnit 5、Testcontainers 2.0、TestRestTemplate |

## 執行方式

需要 JDK 21、PostgreSQL（資料庫名稱 `test`），並先啟動 [Spring_Eureka](../Spring_Eureka)。資料庫連線設定方式與 Spring_JPA 相同，見 [Spring_JPA「資料庫連線設定」](../Spring_JPA/readme.md#資料庫連線設定)。

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- 預設 port：`8084`；Eureka 位址預設 `http://localhost:8761/eureka/`，可用環境變數 `EUREKA_URL` 修改
- 啟動後在 Eureka dashboard（http://localhost:8761）可以看到 `SERVICE-PROVIDER`

本機想開第二個實例時，指定不同的 port 即可：

```bash
./gradlew bootRun --args='--server.port=8094'
```

## Eureka 相關設定

```yaml
spring:
  application:
    name: service-provider          # 註冊的服務名稱，呼叫端用它找到這個服務

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
  instance:
    prefer-ip-address: true         # 註冊 IP，而不是主機名稱
    instance-id: ${spring.cloud.client.ip-address}:${server.port}
```

| 設定 | 說明 |
|---|---|
| `defaultZone` | Eureka 位址。是 Map 的 key，**必須維持駝峰大小寫** |
| `prefer-ip-address` | 容器的主機名稱是隨機的 container id，其他容器無法用它連線，所以改為註冊 IP |
| `instance-id` | 每個實例的唯一 id，同一服務開多台時用來區分；`spring.cloud.client.ip-address` 由 Spring Cloud 自動偵測 |

> 新版 Spring Cloud 只要 classpath 有 `spring-cloud-starter-netflix-eureka-client` 就會自動註冊，**不需要**舊教學常見的 `@EnableEurekaClient`（已移除）或 `@EnableDiscoveryClient`。

## API

Base URL：`http://localhost:8084`

| Method | Path | 說明 | 成功 | 失敗 |
|---|---|---|---|---|
| `GET` | `/api/hello` | 回傳 `Hello from <實例 id>` | `200` | |
| `GET` | `/api/books` | 查詢全部 | `200` | |
| `GET` | `/api/books/{id}` | 查詢一筆 | `200` | `404` |
| `POST` | `/api/books` | 新增 | `201` + `Location` header | `400` |
| `PUT` | `/api/books/{id}` | 修改 | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | 刪除 | `204` | `404` |

Request Body、curl 範例與錯誤格式見 [Spring_JPA「API」](../Spring_JPA/readme.md#api)。

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**（Testcontainers 啟動臨時的 PostgreSQL，不會連到本機資料庫）。

測試時**關閉 Eureka 註冊**（`eureka.client.enabled=false`），因此不需要啟動 Eureka；實例 id 固定為 `test-instance`，讓 `/api/hello` 的回應可以精確比對。

## 已知限制（刻意保留）

與 Spring_JPA 相同：`isbn` 使用 `Integer`、`cost` 使用 `double`、由 `ddl-auto=update` 自動建表，說明見 [Spring_JPA「已知限制」](../Spring_JPA/readme.md#已知限制刻意保留)。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 整合測試（關閉 Eureka），固定原本行為（包含 Bug） |
| 2 | Gradle 7.6 → 8.14.3 |
| 3 | Java 17 → 21、Spring Boot 3.0.1 → 3.5.16、Spring Cloud 2022.0.0 → 2025.0.3；移除 Netflix 候選版本 repository 與明確的 Hibernate dialect |
| 4 | Spring Boot → 4.1.1、Spring Cloud → 2025.1.3、Gradle → 9.8.0、Testcontainers 2.0 |
| 5 | Constructor Injection、Entity 移除 `@Data`、修正更新 Bug、查無資料回 404 |
| 6 | 重新設計為 REST API（`/api/books`、DTO、新增 DELETE）；`/api/hello` 回傳實例 id；port 8081 → 8084；移除未使用的 OpenFeign；簡體註解改為繁體 |
| 7 | 新增 Dockerfile，由 Spring_Eureka_Client 的 docker-compose 啟動兩個實例 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | `8081`（與 Spring_JPA 衝突） | `8084` |
| 連線測試 | `GET /hello` → `Hello` | `GET /api/hello` → `Hello from <實例 id>` |
| 查詢全部 | `GET /findall` | `GET /api/books` |
| 新增 | `POST /saveBook?ISBN=..` | `POST /api/books`（JSON），回 `201` |
| 查詢一筆 | `GET /getOneBook/{ID}` | `GET /api/books/{id}` |
| 修改 | `POST /updateBook?ID=..`，**實際上會新增一筆重複資料** | `PUT /api/books/{id}` |
| 刪除 | 無 | `DELETE /api/books/{id}` |
| 查無資料 | `500` | `404` + ProblemDetail |

- **Hibernate 版本與舊 Bug**：Boot 3.0（Hibernate 6.1）時，「修改不存在的 id」仍會默默新增一筆；升到 Boot 3.5（Hibernate 6.6）後改為拋出例外（500），最後在步驟 5 修正為 404
- **多餘的 Maven repository**：舊版 build.gradle 加了 Netflix 的 `maven-oss-candidates`（候選版本），正式版本都在 Maven Central，額外的 repository 只會增加供應鏈風險，因此移除
