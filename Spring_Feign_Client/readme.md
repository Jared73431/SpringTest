# Spring Feign Client - 用 OpenFeign 呼叫其他服務

使用 Spring Cloud OpenFeign，以「宣告一個介面」的方式呼叫 [Spring_Feign_Server](../Spring_Feign_Server) 的書籍 API，並示範：

- Server 位址從設定檔讀取（不寫死、不需要 Eureka）
- 自訂 `ErrorDecoder`：把 Server 的 404 / 400 原樣轉給呼叫端，而不是一律變成 500
- 連線逾時、Feign 請求 log
- 用 WireMock 模擬 Server 撰寫測試
- Docker Compose 一次啟動 Client、Server、PostgreSQL

這是本 Repo 早期（2022）的練習，已完成現代化（Spring Boot 2.7 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood）／ spring-cloud-openfeign 5.0.3 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、WireMock 3、TestRestTemplate |

> Spring Cloud 的版本要跟 Spring Boot 對應，不能任意搭配：Boot 3.5 → 2025.0.x（Northfields）、Boot 4.0 / 4.1 → 2025.1.x（Oakwood，Boot 4.1 從 2025.1.2 起支援）。對照表見 [Spring Cloud 官網](https://spring.io/projects/spring-cloud)。

## 架構

```text
呼叫端（curl / Postman）
  ↓ :8083
BookController / HelloController   /api/books、/api/hello
  ↓
BookClient（@FeignClient 介面）     Spring 在啟動時自動產生實作
  ↓ HTTP / JSON                    位址：book-server.url（預設 http://localhost:8082）
Spring_Feign_Server :8082
  ↓
PostgreSQL

Server 回錯誤 → BookServerErrorDecoder → BookServerException → GlobalExceptionHandler
```

## 專案結構

```text
src/main/java/com/example/demo/
├── SpringFeignClientApplication.java      # @EnableFeignClients
├── client/
│   ├── BookClient.java                    # @FeignClient 介面
│   ├── BookClientConfig.java              # 只給 BookClient 用的設定（ErrorDecoder）
│   ├── BookServerErrorDecoder.java        # Server 非 2xx → BookServerException
│   └── BookServerException.java
├── controller/BookController.java, HelloController.java
├── dto/BookRequest.java, BookResponse.java
└── exception/GlobalExceptionHandler.java  # 4xx 原樣轉回、5xx → 502、連不上 → 503
```

## 執行方式

### 本機執行

先啟動 [Spring_Feign_Server](../Spring_Feign_Server)（需要 PostgreSQL），再啟動 Client：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
curl http://localhost:8083/api/hello     # 回傳 Hello 表示兩個服務連得通
```

- Client port：`8083`
- Server 位址預設 `http://localhost:8082`，可用環境變數 `BOOK_SERVER_URL` 修改
- Server 沒啟動時，Client 仍可啟動，但呼叫 API 會得到 `503`

### Docker

只需要安裝 Docker，在**本資料夾**執行：

```bash
docker compose up --build                # 啟動 PostgreSQL + Server + Client
curl http://localhost:8083/api/hello
docker compose down                      # 停止（加上 -v 會刪除資料庫資料）
```

| 服務 | 對主機開放 | 說明 |
|---|---|---|
| `postgres` | 不開放 | 只給 `book-server` 連線，避免與本機的 PostgreSQL 衝突 |
| `book-server` | `8082` | 以 `../Spring_Feign_Server` 建置；開放 port 只是方便直接測試 |
| `book-client` | `8083` | 以 `BOOK_SERVER_URL=http://book-server:8082` 找到 Server |

> compose 放在 Client 資料夾，因為 Client 是對外的入口；Server 以相對路徑建置。

## API

Base URL：`http://localhost:8083`。每個端點都會透過 Feign 轉呼叫 Server 的同名端點。

| Method | Path | 說明 | 成功 | 失敗 |
|---|---|---|---|---|
| `GET` | `/api/hello` | 連線測試 | `200` | `503` |
| `GET` | `/api/books` | 查詢全部 | `200` | `502` / `503` |
| `GET` | `/api/books/{id}` | 查詢一筆 | `200` | `404` |
| `POST` | `/api/books` | 新增 | `201` + `Location` header | `400` |
| `PUT` | `/api/books/{id}` | 修改 | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | 刪除 | `204` | `404` |

Request Body 與 Server 相同，見 [Spring_JPA「Request Body」](../Spring_JPA/readme.md#request-bodypost--put)。

### 錯誤回應

| 情境 | Client 回應 | 理由 |
|---|---|---|
| Server 回 4xx（404、400…） | **原樣轉回**：狀態碼與 ProblemDetail 內容不變 | 錯誤原因在呼叫端（查無資料、欄位錯誤），呼叫端需要知道 |
| Server 回 5xx | `502 Bad Gateway` | 問題在下游服務，不是 Client 本身 |
| 連不上 Server（拒絕連線、逾時） | `503 Service Unavailable` | 下游服務暫時無法使用，稍後再試 |

```json
{
  "status": 404,
  "title": "Not Found",
  "detail": "Book not found: id=999",
  "instance": "/api/books/999"
}
```

## 測試

```bash
./gradlew test
```

**不需要**啟動 Server、資料庫，也不需要 Docker。測試使用 [WireMock](https://wiremock.org/) 在隨機 port 啟動一個假的 Server：

- `BookControllerTest`：每個 API 的成功情境；驗證 Client 送出的 JSON；Server 回 404 / 400 / 500 時 Client 的回應
- `BookServerUnavailableTest`：Server 連不上時回 `503`

```java
// 讓假 Server 回應 404，再檢查 Client 是否原樣轉回
server.stubFor(get("/api/books/999").willReturn(aResponse().withStatus(404)
        .withHeader("Content-Type", "application/problem+json").withBody(NOT_FOUND_PROBLEM)));
```

> 呼叫其他服務的程式，測試時通常**不連真正的服務**：對方可能還沒寫好、資料無法控制，也很難製造錯誤情境。WireMock 讓測試能精準控制「對方回什麼」。

---

## Feign 教學

### 1. Feign 是什麼

呼叫其他服務的 REST API，最原始的寫法要自己組 URL、送請求、解析 JSON：

```java
// RestTemplate：每個呼叫都要寫一次
BookResponse book = restTemplate.getForObject("http://localhost:8082/api/books/{id}", BookResponse.class, id);
```

Feign 改成**宣告一個介面**，由框架產生實作，呼叫遠端 API 就像呼叫一般方法：

```java
@FeignClient(name = "book-server", url = "${book-server.url}")
public interface BookClient {
    @GetMapping("/api/books/{id}")
    BookResponse findById(@PathVariable("id") Integer id);
}

// 使用時
BookResponse book = bookClient.findById(1);
```

註解沿用 Spring MVC 的 `@GetMapping`、`@PathVariable`、`@RequestBody`，寫法和 Server 的 Controller 幾乎一樣。

### 2. 舊寫法 vs 現在的寫法

| 項目 | 舊寫法 | 現在 | 為什麼改 |
|---|---|---|---|
| Server 位址 | `url = "http://localhost:8082"` 寫死 | `url = "${book-server.url}"` | 換環境（Docker、K8S）不必改程式 |
| client 名稱 | `name = "aac"` | `name = "book-server"` | 名稱用來對應設定，也是服務註冊中心的服務名稱 |
| 回傳型別 | `List<?>` | `List<BookResponse>` | `List<?>` 的每一筆都是 `LinkedHashMap`，無法用 `title()` 取值 |
| 套件 | `controller.service.Bookservice` | `client.BookClient` | 它不是 Service，而是「對外呼叫的 client」 |
| 錯誤處理 | 無，一律 500 | `ErrorDecoder` + `@RestControllerAdvice` | 保留 Server 的狀態碼與錯誤原因 |
| 逾時 | 預設值 | 連線 2 秒、讀取 5 秒 | 對方沒回應時，不讓請求一直卡住 |

### 3. 錯誤處理：ErrorDecoder

Server 回傳非 2xx 時，Feign 會呼叫 `ErrorDecoder` 決定要拋出什麼例外。預設拋出 `FeignException`，沒有處理就變成 500：

```text
修正前：Server 404「Book not found: id=999」 → FeignException → Client 500（原因消失）
修正後：Server 404「Book not found: id=999」 → BookServerException → Client 404（內容不變）
```

`BookClientConfig` 只套用在 `BookClient`，所以**刻意不加 `@Configuration`**：加了會被 component scan 掃到，變成所有 Feign Client 共用的設定。

```java
@FeignClient(name = "book-server", url = "${book-server.url}", configuration = BookClientConfig.class)
```

> 另一種做法是直接在 `@RestControllerAdvice` 處理 `FeignException`（它也帶有 `status()` 與回應內容）。這裡選擇 ErrorDecoder，是讓 Feign 的例外型別不擴散到程式其他地方，之後換成別的 HTTP client 時，只需要修改 `client` 套件。

### 4. 設定

```properties
# 對應 @FeignClient(name = "book-server")，只套用在這個 client
spring.cloud.openfeign.client.config.book-server.connect-timeout=2000
spring.cloud.openfeign.client.config.book-server.read-timeout=5000
spring.cloud.openfeign.client.config.book-server.logger-level=basic

# Feign 的 log 只以 DEBUG 等級輸出，要把該介面的 log 等級設為 DEBUG 才看得到
logging.level.com.example.demo.client.BookClient=DEBUG
```

`logger-level=basic` 的輸出：

```text
[BookClient#findById] ---> GET http://localhost:8082/api/books/999 HTTP/1.1
[BookClient#findById] <--- HTTP/1.1 404 Not Found (256ms)
```

`config` 後面寫 `default` 則套用到所有 Feign Client。

### 5. DTO 要不要共用？

Client 和 Server 各有一份 `BookRequest` / `BookResponse`，內容幾乎相同。

| 做法 | 優點 | 缺點 |
|---|---|---|
| **各自定義**（本模組） | 兩個服務獨立部署、獨立演進；Client 只需要宣告自己用到的欄位 | 程式碼重複；欄位名稱改了要兩邊一起改 |
| 抽成共用 jar | 不重複 | 兩個服務被同一個 jar 綁住，任一邊改版都要重新發布、兩邊一起升級 |
| 由 OpenAPI 文件產生 | 以 API 規格為準，自動產生 | 需要額外的工具與流程 |

微服務通常傾向**各自定義**或**從 OpenAPI 產生**，避免共用 jar 造成的耦合。

### 6. OpenFeign 的現況：功能完成（feature-complete）

[Spring Cloud OpenFeign 官方文件](https://docs.spring.io/spring-cloud-openfeign/reference/)寫道：

> we're now treating the Spring Cloud OpenFeign project as **feature-complete** … We suggest migrating over to **Spring HTTP Service Clients** instead.

意思是之後只修 Bug，不再加新功能，官方建議改用 Spring Framework 內建的 **HTTP Service Client**（`@HttpExchange`）：

```java
// OpenFeign（需要 Spring Cloud）
@FeignClient(name = "book-server", url = "${book-server.url}")
public interface BookClient {
    @GetMapping("/api/books/{id}")
    BookResponse findById(@PathVariable("id") Integer id);
}

// HTTP Service Client（Spring Framework 6 起內建，底層使用 RestClient）
@HttpExchange("/api/books")
public interface BookClient {
    @GetExchange("/{id}")
    BookResponse findById(@PathVariable Integer id);
}
```

| | OpenFeign | HTTP Service Client |
|---|---|---|
| 來源 | Spring Cloud（要對應 Spring Cloud 版本） | Spring Framework 內建 |
| 狀態 | 功能完成，只修 Bug | 持續發展中 |
| 註解 | 沿用 `@GetMapping` 等 | `@GetExchange`、`@PostExchange` 等 |
| 搭配服務註冊、負載平衡 | 內建整合 | 搭配 Spring Cloud LoadBalancer |
| 現況 | 許多既有專案仍在使用 | 新專案的建議選擇 |

**為什麼本模組仍使用 Feign**：很多公司的既有系統都在用，也是面試常見的題目。HTTP Service Client 之後會另外建立一個練習模組。

### 7. 需要 Eureka 嗎？

本模組直接以 `url` 指定 Server 位址，**沒有使用服務註冊中心**：

| 部署環境 | 誰負責「找到服務」 | 需要 Eureka 嗎 |
|---|---|---|
| VM / 實體機，IP 會變動 | Eureka（或 Consul） | 需要 |
| Docker Compose | compose 內建 DNS（服務名稱 `book-server`） | 不需要 |
| Kubernetes | K8S Service + DNS（也包含負載平衡） | 不需要 |

搭配 Eureka 時，`@FeignClient` 只寫 `name`、不寫 `url`，由 Eureka 查詢實際位址，見 [Spring_Eureka_Client](../Spring_Eureka_Client)。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 WireMock 測試，固定原本行為（Server 位址寫死，只能固定使用 8082） |
| 2 | Gradle 7.5.1 → 8.14.3 |
| 3 | Java 11 → 21、Spring Boot 2.7.5 → 3.5.16、Spring Cloud 2021.0.4 → 2025.0.3 |
| 4 | Spring Boot → 4.1.1、Spring Cloud → 2025.1.3、Gradle → 9.8.0 |
| 5 | 重新設計：`/api/books`、有型別的 DTO、Server 位址改從設定檔讀取、補上查詢一筆 / 修改 / 刪除、ErrorDecoder、逾時與 log 設定、移除未使用的 Lombok |
| 6 | 新增 Dockerfile 與 docker-compose（Client + Server + PostgreSQL），修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 連線測試 | `GET /HelloWorld` | `GET /api/hello` |
| 查詢全部 | `GET /findAllBook` | `GET /api/books` |
| 新增 | `POST /saveBook?ISBN=..&title=..` | `POST /api/books`（JSON），回 `201` + 新資料 |
| 查詢一筆 / 修改 / 刪除 | 無 | `GET` / `PUT` / `DELETE /api/books/{id}` |
| Server 回錯誤 | 一律 `500` | 4xx 原樣轉回、5xx → `502`、連不上 → `503` |
| Server 位址 | 寫死 `http://localhost:8082` | `book-server.url`（環境變數 `BOOK_SERVER_URL`） |

### 升級時學到的事

- **Spring Cloud 要對應 Spring Boot 版本**：升級 Boot 時一定要一起確認 Spring Cloud 的 release train。
- **程式碼不需要改**：Boot 2.7 → 4.1 的過程中，Feign 介面本身沒有任何修改，主要變動都在 build 設定與測試。
- **Boot 4 測試模組化**：`TestRestTemplate` 移至 `spring-boot-resttestclient`，需加上 `@AutoConfigureTestRestTemplate`。
- **`gradlew` 執行權限**：在 Windows 建立的專案，`gradlew` 在 Git 中可能沒有執行權限（`100644`），在 Docker（Linux）中執行會出現 Permission denied，需要用 `git update-index --chmod=+x gradlew` 修正。
