# Spring HTTP Service Client - 用 `@HttpExchange` 呼叫其他服務

和 [Spring_Feign_Client](../Spring_Feign_Client) 做**完全相同**的事：呼叫 [Spring_Feign_Server](../Spring_Feign_Server) 的書籍 API，API 與錯誤處理規則都一樣。差別只在 client 的寫法，從 OpenFeign 換成 Spring Framework 內建的 **HTTP Service Client**。

兩個模組放在一起看，就能逐項比較這兩種寫法。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-restclient`，不需要 Spring Cloud） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、WireMock |

## 架構

```text
呼叫端（curl / Postman）
  ↓ :8111
BookController / HelloController   /api/books、/api/hello
  ↓
BookClient（@HttpExchange 介面）    Spring 在啟動時產生實作，底層是 RestClient
  ↓ HTTP / JSON                    位址：spring.http.serviceclient.book-server.base-url
Spring_Feign_Server :8082
  ↓
PostgreSQL

Server 回錯誤 → RestClientResponseException / ResourceAccessException → GlobalExceptionHandler
```

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_DeclarativeClientTest](src/test/java/com/example/demo/lesson/L01_DeclarativeClientTest.java) | 只寫介面、Spring 產生實作；方法怎麼變成 HTTP 請求 |
| 2 | [L02_ErrorHandlingTest](src/test/java/com/example/demo/lesson/L02_ErrorHandlingTest.java) | 4xx 原樣轉回、5xx → 502、連不上或逾時 → 503；不需要 ErrorDecoder |
| 3 | [L03_GroupConfigurationTest](src/test/java/com/example/demo/lesson/L03_GroupConfigurationTest.java) | 群組設定：位址、逾時、預設標頭 |

## 專案結構

```text
src/main/java/com/example/demo/
├── client/
│   ├── BookClient                @HttpExchange 介面
│   └── HttpServicesConfig        @ImportHttpServices(group = "book-server")
├── controller/                   BookController、HelloController（和 Spring_Feign_Client 相同）
├── dto/                          BookRequest、BookResponse（和 Spring_Feign_Client 相同）
└── exception/GlobalExceptionHandler   ProblemDetail：4xx 原樣、502、503

src/main/resources/application.properties   spring.http.serviceclient.book-server.*
```

## 執行方式

### 測試（主要的學習方式）

用 WireMock 模擬 Server，不需要 Server、資料庫或 Docker。

```bash
./gradlew test
```

### Docker Compose

一次啟動 PostgreSQL、Spring_Feign_Server、本服務：

```bash
docker compose up --build -d
# Windows 保留了 8111 時：APP_PORT=8311 docker compose up --build -d

curl http://localhost:8111/api/hello          # Hello
curl -X POST http://localhost:8111/api/books -H "Content-Type: application/json" \
     -d '{"isbn":12345,"title":"Java","author":"Tom","year":2020,"publisher":"OReilly","cost":450.5}'
curl http://localhost:8111/api/books
docker compose stop book-server
curl http://localhost:8111/api/books          # 503
docker compose down -v
```

### 本機執行

先啟動 Spring_Feign_Server（localhost:8082），再執行：

```bash
./gradlew bootRun
```

### 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的服務實際執行過。Environment 變數 `baseUrl` = `http://localhost:8111`。

| 請求 | 設定 | 結果 |
|---|---|---|
| `GET {{baseUrl}}/api/hello` | | `Hello` |
| `POST {{baseUrl}}/api/books` | Body 選 **raw** / **JSON**：`{"isbn":12345,"title":"Java","author":"Tom","year":2020,"publisher":"OReilly","cost":450.5}`；Scripts → Post-response：`pm.collectionVariables.set("bookId", pm.response.json().id);` | 201，`Location: /api/books/{id}` |
| `GET {{baseUrl}}/api/books/{{bookId}}` | | 200 |
| `GET {{baseUrl}}/api/books/999` | | 404：Server 的 ProblemDetail 原樣轉回 |
| `DELETE {{baseUrl}}/api/books/{{bookId}}` | | 204 |

把同一組請求的 `baseUrl` 換成 `http://localhost:8083`（Spring_Feign_Client），結果完全相同。

## API

和 Spring_Feign_Client 相同，Base URL 改成 `http://localhost:8111`：

| Method | Path | 成功 | 失敗 |
|---|---|---|---|
| `GET` | `/api/hello` | `200` | `503` |
| `GET` | `/api/books` | `200` | `502` / `503` |
| `GET` | `/api/books/{id}` | `200` | `404` |
| `POST` | `/api/books` | `201` + `Location` | `400` |
| `PUT` | `/api/books/{id}` | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | `204` | `404` |

| 情境 | 回應 |
|---|---|
| Server 回 4xx | 原樣轉回（狀態碼與 ProblemDetail 不變） |
| Server 回 5xx | `502 Bad Gateway` |
| 連不上 Server、讀取逾時 | `503 Service Unavailable` |

## HTTP Service Client 教學

### 1. 只寫介面（第 1 課）

```java
@HttpExchange("/api")
public interface BookClient {

    @GetExchange("/books/{id}")
    BookResponse findById(@PathVariable Integer id);

    @PostExchange("/books")
    BookResponse create(@RequestBody BookRequest request);
}
```

```java
@Configuration
@ImportHttpServices(group = "book-server", types = BookClient.class)
public class HttpServicesConfig {
}
```

Spring 在啟動時為介面產生實作並宣告成 Bean，Controller 直接注入。呼叫 `create(request)` 時：

```text
@HttpExchange("/api") + @PostExchange("/books") → POST {base-url}/api/books
request → JSON（Request Body）
回應的 JSON → BookResponse
```

### 2. 錯誤處理（第 2 課）

底層是 `RestClient`，遇到錯誤時直接拋出帶有完整資訊的例外：

| 情況 | 例外 | 裡面有 |
|---|---|---|
| Server 回 4xx、5xx | `RestClientResponseException` | 狀態碼、標頭、回應內容 |
| 連不上、讀取逾時 | `ResourceAccessException` | 原因（`IOException`） |

`GlobalExceptionHandler` 直接處理這兩個例外即可，**不需要像 Feign 一樣自己寫 `ErrorDecoder` 和自訂的例外**。

### 3. 群組設定（第 3 課）

```properties
spring.http.serviceclient.book-server.base-url=${BOOK_SERVER_URL:http://localhost:8082}
spring.http.serviceclient.book-server.connect-timeout=2s
spring.http.serviceclient.book-server.read-timeout=5s
spring.http.serviceclient.book-server.default-header.X-Client-Name=spring-http-service-client
```

`book-server` 是 `@ImportHttpServices` 的群組名稱。同一個服務有多個介面時，放在同一個群組，就共用位址與設定。

### 4. OpenFeign vs HTTP Service Client

| | OpenFeign（Spring_Feign_Client） | HTTP Service Client（本模組） |
|---|---|---|
| 來源 | Spring Cloud OpenFeign | Spring Framework 內建（6 起；Boot 4 加上群組設定） |
| 依賴 | `spring-cloud-starter-openfeign` + Spring Cloud BOM | `spring-boot-starter-restclient` |
| 介面的註解 | `@FeignClient` + Spring MVC 的 `@GetMapping` | `@HttpExchange`、`@GetExchange`… |
| 註冊 | `@EnableFeignClients`（掃描） | `@ImportHttpServices(group = ..., types = ...)` |
| 設定 | `spring.cloud.openfeign.client.config.<name>.*` | `spring.http.serviceclient.<group>.*` |
| 錯誤 | 自己寫 `ErrorDecoder`；I/O 錯誤是 `RetryableException` | `RestClientResponseException`；I/O 錯誤是 `ResourceAccessException` |
| 底層 | Feign 自己的 client（預設 `HttpURLConnection`） | `RestClient`（Boot 4 預設 JDK `HttpClient`） |
| 現況 | 官方宣布 **feature-complete**（只修 Bug），建議改用 HTTP Service Client | 持續發展中 |

**怎麼選**：新專案用 HTTP Service Client。既有系統已經在用 Feign 時，不必急著改，它仍然會修 Bug。兩者都是「只寫介面」，所以遷移時主要是換註解與設定，Controller 和 DTO 不用改（本模組的 Controller、DTO 和 Spring_Feign_Client 完全相同）。

## 面試時能說明的重點

**Q：HTTP Service Client 是什麼？**
只宣告一個介面，用 `@GetExchange` 這類註解描述 HTTP 請求，Spring 在啟動時產生實作，底層用 `RestClient` 送出請求、把 JSON 轉成物件。

**Q：和 OpenFeign 有什麼不同？為什麼要改？**
寫法很像，但 HTTP Service Client 是 Spring Framework 內建的，不需要 Spring Cloud；錯誤處理直接用 RestClient 的例外，不必寫 ErrorDecoder。OpenFeign 已經宣布只修 Bug，官方建議改用 HTTP Service Client。

**Q：呼叫其他服務時，錯誤要怎麼回應呼叫端？**
對方回 4xx（例如查無資料）原樣轉回，呼叫端才知道真正的原因；對方回 5xx 用 502，表示問題在下游；連不上或逾時用 503。

**Q：為什麼要設定逾時？**
沒有逾時時，對方沒有回應，請求就一直卡住，佔用執行緒，最後拖垮自己。

## 開發時遇到的問題

- **[Learning] WireMock 與 HTTP/2**：Boot 4 的 RestClient 預設使用 JDK 的 `HttpClient`，會嘗試把明文連線升級成 HTTP/2（h2c）。測試中 WireMock（Jetty）接受了升級，但帶 body 的 POST 失敗（`RST_STREAM: Stream cancelled`）。真正的 Server 是 Tomcat，不支援 h2c，會退回 HTTP/1.1；Docker 中對真正的 Server POST 是正常的（實測 201）。所以只在測試的 WireMock 加上 `http2PlainDisabled(true)`。Feign 沒有遇到，因為它預設用 HTTP/1.1 的 `HttpURLConnection`。
- **測試 context 的重用**：Spring 會重用相同設定的測試 context。每個測試類別各自啟動 WireMock 時，port 每次都不同，但重用的 context 還記著舊的位址。所以整個測試期間只啟動一個 WireMock。
