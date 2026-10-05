# Spring Okhttp - 在 Spring Boot 中使用 OkHttp

以 [OkHttp](https://square.github.io/okhttp/) 呼叫外部 API（[JSONPlaceholder](https://jsonplaceholder.typicode.com/)），示範：

- OkHttpClient 註冊成 Spring Bean：逾時、日誌、**Interceptor**
- 同步（`execute()`）與非同步（`enqueue()` → `CompletableFuture`）兩種呼叫方式
- 錯誤處理：OkHttp 不會因 4xx / 5xx 拋出例外，要自己檢查
- 用 OkHttp 官方的 **MockWebServer** 撰寫測試

API 與 [Spring_HttpClient](../Spring_HttpClient) 相同，可以直接對照 **RestClient / WebClient / OkHttp** 三種寫法。

這是 2025 年加入的練習，已完成現代化（Spring Boot 3.2 → 4.1、OkHttp 4 → 5），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Spring MVC） |
| OkHttp | 5.5.0（含 logging-interceptor） |
| JSON | Jackson 3（Spring Boot 內建） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、MockWebServer（mockwebserver3）、TestRestTemplate |

> OkHttp **不在** Spring Boot 的版本管理內（舊版 Boot 曾管理，3.5 與 4.1 的 BOM 都已沒有），需要在 build.gradle 自行指定版本；Spring Framework 7 也移除了 OkHttp 的整合類別，因此 OkHttp 無法再當作 RestClient 的底層，只能直接使用。

## 架構

```text
呼叫端
  ↓ :8088
PostController       /api/posts             → PostOkHttpClient.execute()（同步）
AsyncPostController  /api/async/posts/{id}  → PostOkHttpClient.enqueue()（非同步，CompletableFuture）
                                                   ↓
                                    OkHttpClient（Spring Bean）
                                    ├─ UserAgentInterceptor：加上 User-Agent
                                    └─ HttpLoggingInterceptor：記錄請求（遮蔽 Authorization）
                                                   ↓ HTTPS
                                    jsonplaceholder.typicode.com
GlobalExceptionHandler：外部 API 的錯誤 → ProblemDetail
DemoRunner：啟動後自動示範一次（輸出到 log）
```

## 專案結構

```text
src/main/java/com/example/demo/
├── client/
│   ├── PostOkHttpClient.java              # 用 OkHttp 呼叫外部 API（同步 + 非同步）
│   ├── UpstreamResponseException.java     # 外部 API 回非 2xx
│   └── UpstreamUnavailableException.java  # 連不上、逾時
├── config/
│   ├── JsonPlaceholderProperties.java     # 位址、逾時、日誌等級
│   ├── OkHttpConfig.java                  # OkHttpClient Bean
│   └── UserAgentInterceptor.java          # 自訂 Interceptor
├── controller/PostController.java, AsyncPostController.java
├── exception/GlobalExceptionHandler.java
├── model/Post.java                        # record
└── runner/DemoRunner.java
```

## 執行方式

需要網路（會呼叫 JSONPlaceholder）：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- Port：`8088`
- 啟動後 log 會輸出示範結果；沒有網路時只會出現警告，應用程式仍正常啟動
- 關閉啟動示範：`./gradlew bootRun --args='--demo.runner.enabled=false'`

## API

| Method | Path | 說明 | 成功 |
|---|---|---|---|
| `GET` | `/api/posts` | 查詢全部 | `200` |
| `GET` | `/api/posts/{id}` | 查詢一筆（同步） | `200` |
| `GET` | `/api/async/posts/{id}` | 查詢一筆（非同步） | `200` |
| `POST` | `/api/posts` | 新增 | `201` + `Location` |
| `PUT` | `/api/posts/{id}` | 修改 | `200` |
| `DELETE` | `/api/posts/{id}` | 刪除 | `204` |

```bash
curl http://localhost:8088/api/posts/1
curl http://localhost:8088/api/async/posts/2
curl -X POST http://localhost:8088/api/posts \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "title": "我的標題", "body": "我的內容"}'
```

> JSONPlaceholder 是假資料 API：新增、修改、刪除都會回傳成功，但資料不會真的被儲存。

### 錯誤回應

| 情境 | 回應 |
|---|---|
| 外部 API 回 4xx（例如 `/api/posts/99999`） | 相同狀態碼（404） |
| 外部 API 回 5xx | `502 Bad Gateway` |
| 連不上、逾時 | `503 Service Unavailable`，詳細原因記錄在 log |

所有錯誤皆為 ProblemDetail（`application/problem+json`）。

## 設定

```properties
server.port=8088
jsonplaceholder.base-url=https://jsonplaceholder.typicode.com
jsonplaceholder.connect-timeout=2s
jsonplaceholder.read-timeout=5s
jsonplaceholder.write-timeout=5s
jsonplaceholder.log-level=BASIC       # NONE / BASIC / HEADERS / BODY
demo.runner.enabled=true
```

## 測試

```bash
./gradlew test
```

**不需要網路**。外部 API 以 OkHttp 官方的 **MockWebServer** 模擬：

| 測試 | 內容 |
|---|---|
| `PostApiContractTest`（抽象類別） | 「查詢一筆」的共用案例：成功、404、502、逾時 503 |
| `SyncPostApiTest` | 繼承共用案例，加上查詢全部、新增、修改、刪除、User-Agent interceptor |
| `AsyncPostApiTest` | 繼承共用案例，確保非同步版本行為一致 |
| `UpstreamUnreachableTest` | 外部 API 連不上：應用程式仍可啟動、兩個版本都回 503 |
| `OkHttpConfigTest` | 單元測試：日誌等級為 HEADERS 時，`Authorization` 被遮蔽為 `██` |

---

## OkHttp 教學

### 1. OkHttp 在 Spring 生態中的位置

| | RestClient / WebClient | OkHttp |
|---|---|---|
| 來源 | Spring Framework 內建 | Square 開源，與 Spring 無關 |
| 抽象層級 | 高：自動轉換 JSON、4xx / 5xx 自動拋出例外 | 低：自己組 Request、轉換 JSON、檢查狀態碼、關閉 Response |
| 常見用途 | Spring 應用程式呼叫其他服務 | Android、Retrofit 的底層、需要細部控制連線的情境 |
| 特色 | 與 Spring 設定、observability 整合 | Interceptor、連線池、HTTP/2、WebSocket |

在 Spring Boot 專案中，一般呼叫 API 用 **RestClient** 就足夠；遇到既有程式使用 OkHttp、或需要 OkHttp 特有功能時，再參考本模組的寫法。

### 2. 同步 vs 非同步

```java
// 同步：execute() 阻塞目前執行緒，直到收到回應
try (Response response = okHttpClient.newCall(request).execute()) {
    return response.body().string();
}

// 非同步：enqueue() 立即返回，回應由 OkHttp 的執行緒呼叫 Callback
okHttpClient.newCall(request).enqueue(new Callback() {
    public void onFailure(Call call, IOException e) { ... }
    public void onResponse(Call call, Response response) { ... }
});
```

#### Callback 轉成 CompletableFuture

修正前的 `/test-async` 立即回傳「已啟動」，結果只印在 console。改為把 Callback 包成 `CompletableFuture`，Controller 直接回傳，Spring MVC 等它完成後才寫出回應：

```java
public CompletableFuture<Post> findByIdAsync(Long id) {
    CompletableFuture<Post> future = new CompletableFuture<>();
    okHttpClient.newCall(request).enqueue(new Callback() {
        public void onFailure(Call call, IOException e) {
            future.completeExceptionally(new UpstreamUnavailableException(e));
        }
        public void onResponse(Call call, Response response) {
            try (response) {
                future.complete(jsonMapper.readValue(readBody(response), Post.class));
            } catch (...) {
                future.completeExceptionally(...);
            }
        }
    });
    return future;
}
```

「把 callback 風格的 API 包成 `CompletableFuture`」是很常用的技巧，任何提供 callback 的函式庫都可以這樣整合。

### 3. 錯誤處理：OkHttp 不會因 4xx / 5xx 拋出例外

| 情況 | OkHttp 的行為 |
|---|---|
| 網路錯誤、逾時 | 拋出 `IOException`（非同步時呼叫 `onFailure`） |
| 對方回 404、500 | **不拋例外**，正常回傳 `Response`，`isSuccessful()` 為 `false` |

所以一定要自己檢查：

```java
if (!response.isSuccessful()) {
    throw new UpstreamResponseException(response.code());
}
```

#### 修正前的 Bug：失敗也回 200

```java
// Service：錯誤只印在 console
} catch (IOException e) {
    System.out.println("❌ 網路錯誤: " + e.getMessage());
}

// Controller：Service 永遠不拋例外，所以永遠回 200
try {
    okHttpService.performGetRequest();
    return ResponseEntity.ok("GET 請求執行完成，請查看控制台日誌");
} catch (Exception e) { ... }   // ← 永遠不會執行
```

**不要在底層把例外吞掉**，應該拋出有意義的例外，交給 `@RestControllerAdvice` 統一轉成正確的狀態碼。

### 4. 一定要關閉 Response

`Response` 持有連線與資料流，沒有關閉會導致**連線無法回到連線池**，最後耗盡連線。

```java
try (Response response = client.newCall(request).execute()) { ... }   // 同步
try (response) { ... }                                                 // 非同步的 onResponse（Java 9+）
```

修正前的非同步版本在 `response.close()` 之前若 `body().string()` 拋出例外，Response 就不會被關閉。

### 5. Interceptor

Interceptor 可以在**每個請求送出前、收到回應後**插入共通的處理：

```java
public Response intercept(Chain chain) throws IOException {
    Request request = chain.request().newBuilder()   // Request 不可變，要用 newBuilder() 產生新的
            .header("User-Agent", userAgent)
            .build();
    return chain.proceed(request);                   // 交給下一個 interceptor / 送出請求
}
```

| 用途 | 範例 |
|---|---|
| 共通 header | User-Agent、認證 token、trace id（本模組：`UserAgentInterceptor`） |
| 記錄 | `HttpLoggingInterceptor` |
| 重試、快取、改寫回應 | 自訂邏輯 |

Interceptor **依加入順序執行**：本模組先加入 `UserAgentInterceptor`，`HttpLoggingInterceptor` 才看得到最終送出的 header。

> OkHttp 還有 `addNetworkInterceptor()`：在實際送上網路前執行，可以看到重新導向、壓縮等底層細節；一般用途使用 `addInterceptor()` 即可。

### 6. 日誌等級與敏感資料

| 等級 | 記錄內容 | 建議 |
|---|---|---|
| `NONE` | 不記錄 | |
| `BASIC` | 方法、URL、狀態碼、耗時 | **本模組預設** |
| `HEADERS` | 加上所有 header | 開發除錯 |
| `BODY` | 加上完整的請求與回應內容 | 只在本機除錯；可能洩漏個資、log 暴增 |

```java
HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::info);  // 改走 SLF4J，預設直接印到 stdout
logging.setLevel(properties.logLevel());
logging.redactHeader("Authorization");                                     // 遮蔽成 ██
```

修正前固定使用 `BODY`。之後若呼叫需要認證的 API，token 會直接出現在 log 中。

### 7. OkHttpClient 要共用

`OkHttpClient` 內含**連線池與執行緒池**，官方建議整個應用程式共用一個實例。註冊成 Spring Bean 正好滿足這點；不要在每次請求時 `new OkHttpClient()`。

### 8. Gson vs Jackson

修正前同時存在 Gson（自行引入）與 Jackson（Spring Boot 內建），本模組改為只用 Jackson：

| | Gson | Jackson |
|---|---|---|
| 來源 | Google | FasterXML，**Spring Boot 預設** |
| 常見搭配 | Android、Retrofit | Spring 生態 |
| 設定 | 簡單 | 功能多（註解、模組、Java Time 支援） |
| 在 Spring Boot 中 | 需自行引入與設定 | 已自動設定好（`JsonMapper` Bean），Controller 的 JSON 也是它處理 |

同一個專案用兩套 JSON 函式庫，命名規則、日期格式等設定容易不一致，除非有特殊需求，選一套即可。

### 9. MockWebServer vs WireMock

| | MockWebServer（本模組） | WireMock（[Spring_HttpClient](../Spring_HttpClient)） |
|---|---|---|
| 來源 | OkHttp 官方 | 獨立專案 |
| 風格 | 依序排隊回應（`enqueue`），或自訂 `Dispatcher` | 宣告式 stub（`stubFor(get(...))`）與驗證 |
| 適合 | 輕量、OkHttp 相關測試 | 功能完整（比對條件、延遲、錄製、獨立執行） |

本模組的 DemoRunner / 多個測試會用到不同路徑，因此改用自訂 `Dispatcher` 依「方法 + 路徑」回應，避免依序排隊的回應被其他請求拿走。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 外部 API 位址改為可設定（預設不變），以 MockWebServer 測試取代連外部網路的測試，固定原本行為 |
| 2 | Java 17 → 21（改用 toolchain）、Spring Boot 3.2.0 → 3.5.16、Gradle 8.14.2 → 8.14.3、Gson 改由 Boot 管理版本 |
| 3 | Spring Boot → 4.1.1、Gradle → 9.8.0 |
| 4 | OkHttp 4.12.0 → 5.5.0（程式不需修改） |
| 5 | 改為回傳資料的 `/api/posts` CRUD 與非同步 API、錯誤處理、Jackson 取代 Gson、Interceptor、可設定的逾時與日誌、DemoRunner、mockwebserver3 |
| 6 | 重寫 README，修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | `8080` | `8088` |
| API | `/api/okhttp/test-get`、`test-post`、`test-async`、`health`，只回傳「請查看控制台日誌」 | `/api/posts` CRUD 與 `/api/async/posts/{id}`，回傳真正的資料 |
| 外部 API 失敗 | **一律 200「執行完成」** | 4xx 原樣、5xx → `502`、無法使用 → `503` |
| 逾時 | 連線 / 讀取 / 寫入各 30 秒 | 2 秒 / 5 秒 / 5 秒，可設定 |
| 日誌 | `BODY`（完整內容）印到 stdout | `BASIC` 經由 SLF4J，可設定；`Authorization` 遮蔽 |
| JSON | Gson | Jackson（Spring Boot 內建） |
| 啟動示範 | 寫在啟動類別，測試也會連外部網路 | `DemoRunner`，可關閉，失敗不影響啟動 |

### 升級時學到的事

- **OkHttp 5 的 artifact**：Gradle 透過 module metadata 自動把 `com.squareup.okhttp3:okhttp` 解析成 `okhttp-jvm`；Maven 專案需要直接使用 `okhttp-jvm`。OkHttp 5 也會帶入 kotlin-stdlib。
- **mockwebserver → mockwebserver3**：5.x 仍提供舊的 `okhttp3.mockwebserver`（相容用），新版 API 在 `mockwebserver3` 套件：`MockResponse` 改為 Builder（`new MockResponse.Builder().code(200).body(...).build()`），`RecordedRequest` 改用 `getTarget()`、`getBody()`（okio `ByteString`）。
- **Spring Boot 不再管理 OkHttp 版本**，需在 build.gradle 自行指定（本模組用 `ext.okhttpVersion` 統一管理）。
