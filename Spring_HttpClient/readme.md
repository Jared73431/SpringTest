# Spring HttpClient - RestClient 與 WebClient 對照

以兩種 HTTP client 呼叫同一個外部 API（[JSONPlaceholder](https://jsonplaceholder.typicode.com/)，公開的假資料 API），對照寫法與行為：

| API | HTTP client | 風格 |
|---|---|---|
| `/api/posts` | **RestClient**（Spring 6.1+） | 同步：呼叫後直接拿到結果 |
| `/api/reactive/posts` | **WebClient**（Spring 5+） | Reactive：回傳 `Mono` / `Flux` |

兩者功能完全相同，測試也用同一組案例驗證。其他寫法（RestTemplate、Feign、HTTP Service Client）見「[四種 HTTP client 比較](#1-四種-http-client-比較)」。

這是 2025 年加入的練習，已完成現代化（Spring Boot 3.5 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Spring MVC，Tomcat） |
| HTTP client | RestClient（JDK HttpClient）、WebClient（Reactor Netty） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、WireMock 3、TestRestTemplate |

## 架構

```text
呼叫端
  ↓ :8087
PostController          /api/posts            → PostRestClient（RestClient）─┐
ReactivePostController  /api/reactive/posts   → PostWebClient（WebClient）───┤ HTTPS
                                                                              ▼
                                                        jsonplaceholder.typicode.com
GlobalExceptionHandler：外部 API 的錯誤 → ProblemDetail
DemoRunner：啟動後自動示範一次 GET / POST / PUT / DELETE（輸出到 log）
```

## 專案結構

```text
src/main/java/com/example/demo/
├── client/
│   ├── JsonPlaceholderProperties.java   # 外部 API 的位址與逾時（@ConfigurationProperties）
│   ├── PostRestClient.java              # RestClient 版本
│   └── PostWebClient.java               # WebClient 版本
├── controller/
│   ├── PostController.java              # /api/posts
│   └── ReactivePostController.java      # /api/reactive/posts
├── exception/GlobalExceptionHandler.java
├── model/Post.java                      # record
└── runner/DemoRunner.java               # 啟動示範
```

## 執行方式

需要網路（會呼叫 JSONPlaceholder）：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- Port：`8087`
- 啟動後 log 會輸出示範結果；沒有網路時只會出現警告，應用程式仍正常啟動
- 關閉啟動示範：`./gradlew bootRun --args='--demo.runner.enabled=false'`

## API

| Method | RestClient 版本 | WebClient 版本 | 說明 | 成功 |
|---|---|---|---|---|
| `GET` | `/api/posts` | `/api/reactive/posts` | 查詢全部（100 筆） | `200` |
| `GET` | `/api/posts/{id}` | `/api/reactive/posts/{id}` | 查詢一筆 | `200` |
| `POST` | `/api/posts` | `/api/reactive/posts` | 新增 | `201` + `Location` |
| `PUT` | `/api/posts/{id}` | `/api/reactive/posts/{id}` | 修改 | `200` |
| `DELETE` | `/api/posts/{id}` | `/api/reactive/posts/{id}` | 刪除 | `204` |

```bash
curl http://localhost:8087/api/posts/1
curl -X POST http://localhost:8087/api/reactive/posts \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "title": "我的標題", "body": "我的內容"}'
```

> JSONPlaceholder 是假資料 API：新增、修改、刪除都會回傳成功，但**資料不會真的被儲存**（新增的 id 永遠是 101）。

### 錯誤回應

| 情境 | 回應 |
|---|---|
| 外部 API 回 4xx（例如 `/api/posts/99999` 查無資料） | 相同狀態碼（404） |
| 外部 API 回 5xx | `502 Bad Gateway` |
| 連不上、逾時（連線 2 秒 / 讀取 5 秒） | `503 Service Unavailable`，詳細原因記錄在 log |

所有錯誤皆為 ProblemDetail（`application/problem+json`）。

## 設定

```properties
server.port=8087
jsonplaceholder.base-url=https://jsonplaceholder.typicode.com
jsonplaceholder.connect-timeout=2s
jsonplaceholder.read-timeout=5s
demo.runner.enabled=true
```

`jsonplaceholder.*` 對應 [JsonPlaceholderProperties](src/main/java/com/example/demo/client/JsonPlaceholderProperties.java)（record + `@ConfigurationProperties`），比多個 `@Value` 更集中、有型別檢查；`Duration` 可以寫成 `2s`、`500ms`。

## 測試

```bash
./gradlew test
```

**不需要網路**。JSONPlaceholder 以 WireMock 模擬：

| 測試 | 內容 |
|---|---|
| `PostApiContractTest`（抽象類別） | 測試案例只寫一次：CRUD、404、502、逾時 503 |
| `RestClientPostApiTest` / `WebClientPostApiTest` | 繼承上面的案例，分別測 `/api/posts` 與 `/api/reactive/posts`，**確保兩個版本行為一致** |
| `UpstreamUnreachableTest` | 外部 API 連不上：應用程式仍可啟動（DemoRunner 只記錄警告）、兩個版本都回 503 |

> 修正前的測試直接呼叫真正的 JSONPlaceholder：沒有網路就失敗，而且依賴對方的資料（例如「剛好 100 筆」）。呼叫外部服務的程式，測試時應該用 WireMock 這類工具模擬，才能穩定重現成功、錯誤、逾時等情境。

---

## HTTP Client 教學

### 1. 四種 HTTP client 比較

| Client | 出現版本 | 風格 | 現況 | 本 Repo 範例 |
|---|---|---|---|---|
| `RestTemplate` | Spring 3 | 同步，方法很多（`getForObject`、`exchange`…） | 維護模式，新程式不建議使用 | — |
| `WebClient` | Spring 5 | Reactive，回傳 `Mono` / `Flux` | 適合 WebFlux 應用程式 | 本模組 |
| **`RestClient`** | Spring 6.1 | 同步，流暢 API（與 WebClient 相似） | **Spring MVC 應用程式的建議選擇** | 本模組 |
| OpenFeign | Spring Cloud | 宣告式介面 | 功能完成（只修 Bug） | [Spring_Feign_Client](../Spring_Feign_Client) |
| OkHttp | Square（非 Spring） | 較底層，需自行處理 JSON 與狀態碼 | 持續維護；Spring 7 已移除整合，只能直接使用 | [Spring_Okhttp](../Spring_Okhttp) |
| HTTP Service Client（`@HttpExchange`） | Spring 6 | 宣告式介面，底層用 RestClient / WebClient | 官方建議取代 Feign | 之後另建模組 |

### 2. 同一件事的兩種寫法

```java
// RestClient：同步，呼叫完就拿到 Post
public Post findById(Long id) {
    return restClient.get()
            .uri("/posts/{id}", id)
            .retrieve()
            .body(Post.class);
}

// WebClient：回傳 Mono<Post>，此時「還沒送出請求」，要等有人訂閱才會執行
public Mono<Post> findById(Long id) {
    return webClient.get()
            .uri("/posts/{id}", id)
            .retrieve()
            .bodyToMono(Post.class);
}
```

| | RestClient | WebClient |
|---|---|---|
| 回傳 | `Post`、`List<Post>` | `Mono<Post>`、`Flux<Post>` |
| 執行時機 | 呼叫方法時立即送出 | 被訂閱時才送出（lazy） |
| 錯誤例外 | `RestClientResponseException`（4xx / 5xx）、`ResourceAccessException`（I/O） | `WebClientResponseException`、`WebClientRequestException` |
| 底層 | JDK `HttpClient`（本專案指定） | Reactor Netty |
| 需要的依賴 | `spring-boot-starter-restclient` | `spring-boot-starter-webclient` |

### 3. 在 Spring MVC 裡使用 WebClient：不等於「全 Reactive」

本應用程式是 **Spring MVC（Tomcat）**。Controller 回傳 `Mono` / `Flux` 時，Spring MVC 會以非同步請求處理、完成後再寫出回應，**但伺服器仍是 Servlet**。

| 情境 | 建議 |
|---|---|
| Spring MVC 應用程式 | **RestClient**（寫法直覺，不必學 Reactor） |
| Spring WebFlux 應用程式 | **WebClient**（全程非阻塞，不可以呼叫 `block()`） |
| MVC 中要同時呼叫多個 API 並行處理 | WebClient 的 `Mono.zip(...)` 很方便；或用 RestClient 搭配 Virtual Threads |

修正前的 README 寫「響應式程式設計、非阻塞 I/O、更好的效能」，但在 MVC 應用程式中並不成立；本模組只引入 `spring-boot-starter-webclient`（不是整個 WebFlux），讓應用程式明確是 MVC。

### 4. 使用 Spring Boot 提供的 Builder

```java
// ✘ 修正前：自己建立 builder，不會套用 Spring Boot 的設定
this.webClient = WebClient.builder().baseUrl("https://...").build();

// ✔ 修正後：注入 Spring Boot 提供的 Builder
public PostWebClient(WebClient.Builder builder, JsonPlaceholderProperties properties) {
    this.webClient = builder.baseUrl(properties.baseUrl())...build();
}
```

Spring Boot 提供的 `RestClient.Builder` / `WebClient.Builder` 已經套用了 Jackson 設定、observability（追蹤與 metrics）、自訂的 customizer，自己 `builder()` 建立的就沒有這些。

### 5. 逾時

沒有設定逾時，外部 API 不回應時請求會一直卡住，佔用執行緒與連線。

```java
// RestClient（JDK HttpClient）
HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(properties.connectTimeout())   // 連線逾時
        .build();
JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
requestFactory.setReadTimeout(properties.readTimeout()); // 讀取逾時

// WebClient（Reactor Netty）
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis())
        .responseTimeout(properties.readTimeout());
```

#### 踩坑：JDK HttpClient 的 HTTP/2 升級

測試時 RestClient 的 **PUT** 失敗（`EOFException: EOF reached while reading`），GET / POST / DELETE 正常，WebClient 版本也正常。

**原因**：JDK `HttpClient` 預設使用 HTTP/2；在 `http://`（非 HTTPS）連線上，會先送出「升級到 HTTP/2」（h2c）的請求。部分伺服器（例如 WireMock 使用的 Jetty）收到**帶有 body 的升級請求**時直接斷線。

**解法**：`version(HttpClient.Version.HTTP_1_1)`。HTTPS 連線用另一種方式（ALPN）協商 HTTP/2，不受影響，所以正式呼叫 JSONPlaceholder 時看不出問題，**只有在測試或呼叫內部的 http:// 服務時才會出現**。

### 6. 錯誤處理

```java
@ExceptionHandler(RestClientResponseException.class)   // RestClient：4xx / 5xx
@ExceptionHandler(WebClientResponseException.class)    // WebClient：4xx / 5xx
@ExceptionHandler(ResourceAccessException.class)       // RestClient：連不上、逾時
@ExceptionHandler(WebClientRequestException.class)     // WebClient：連不上、逾時
```

兩種 client 的例外類別不同，但對應規則相同（4xx 原樣、5xx → 502、無法使用 → 503），與 [Spring_Feign_Client](../Spring_Feign_Client) 的做法一致。

### 7. 啟動示範（CommandLineRunner）的注意事項

```java
@ConditionalOnProperty(name = "demo.runner.enabled", havingValue = "true", matchIfMissing = true)
public class DemoRunner implements CommandLineRunner { ... }
```

- **`CommandLineRunner` 拋出例外會讓應用程式啟動失敗**。修正前用 `block()` 呼叫外部 API，沒有網路就無法啟動，連 `contextLoads` 測試都會失敗；現在改為 catch 後只記錄警告
- 用 `@ConditionalOnProperty` 加上開關，測試時關閉，避免每次啟動都呼叫外部 API

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 外部 API 位址改為可設定（預設不變），以 WireMock 測試取代連外部網路的測試，固定原本行為 |
| 2 | Java 17 → 21、Spring Boot 3.5.3 → 3.5.16、Gradle 8.14.2 → 8.14.3 |
| 3 | Spring Boot → 4.1.1、Gradle → 9.8.0 |
| 4 | RestClient / WebClient 兩個版本、逾時、錯誤處理、DemoRunner 開關、刪除 TestController、`Post` 改為 record、port 8080 → 8087 |
| 5 | 重寫 README，修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | `8080` | `8087` |
| WebClient 版本的 API | `/api/posts` | `/api/reactive/posts`（`/api/posts` 改由 RestClient 處理） |
| 新增 | 回 `200` | 回 `201` + `Location` |
| 刪除 | 回 `200` | 回 `204` |
| 外部 API 錯誤 | 一律 `500` | 4xx 原樣、5xx → `502`、無法使用 → `503` |
| 沒有網路時啟動 | **啟動失敗** | 正常啟動，示範只記錄警告 |
| 測試端點 `/test/...` | 存在（`GET /test/create-sample` 會新增資料） | 移除 |
