# Spring Gateway Server - API Gateway

使用 Spring Cloud Gateway 建立 **API Gateway**：所有外部請求的統一入口，依路徑轉送到後端服務。示範兩種轉送方式：

| 路由 | 路徑 | 轉送到 | 方式 |
|---|---|---|---|
| `first-service` | `/first-service/**` | [Spring_Gateway_Client](../Spring_Gateway_Client)（:8086） | **固定網址** + `StripPrefix` |
| `books-service` | `/books-service/**` | [Spring_Eureka_Provider](../Spring_Eureka_Provider)（可多台） | **`lb://` 透過 Eureka 找到實例** + `RewritePath` |

一次啟動全部服務的 Docker Compose 也在本模組，見「[Docker](#docker)」。

這是本 Repo 早期的練習（參考 JavaInUse 教學），已完成現代化（Spring Boot 3.0 → 4.1），過程中修正了「經過 Gateway 永遠 404」的路由 Bug，詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood）：Gateway 5.0（WebFlux）、Netflix Eureka Client、LoadBalancer |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、WebTestClient、WireMock 3 |

## 架構

```text
                                   呼叫端
                                     │ :10000（唯一入口）
                          ┌──────────▼──────────┐
                          │     API Gateway      │  RequestLoggingFilter（全域）
                          │  default-filters：   │  AddResponseHeader X-Gateway
                          └──┬───────────────┬──┘
     /first-service/api/hello │               │ /books-service/api/books
         StripPrefix=1        │               │ RewritePath + lb://service-provider
                  ▼           │               │          ▲ 查詢實例
           /api/hello         │               ▼          │
   ┌────────────────────┐     │   ┌──────────────────┐  ┌┴─────────┐
   │ first-service :8086│◀────┘   │ Provider ×2 :8084│─▶│ Eureka   │
   │（固定網址）         │         │（輪流）           │註冊│ :8761   │
   └────────────────────┘         └──────────────────┘  └──────────┘
```

## 專案結構

```text
src/main/java/com/example/demo/
├── SpringGatewayServerApplication.java
└── filter/RequestLoggingFilter.java     # 自訂全域 filter：記錄每個請求
src/main/resources/application.yml       # 路由設定
docker-compose.yml                       # Gateway + first-service + Eureka + Provider ×2 + PostgreSQL
```

## 執行方式

### 本機執行

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- Port：`10000`
- `first-service` 路由需要先啟動 [Spring_Gateway_Client](../Spring_Gateway_Client)（:8086）
- `books-service` 路由需要先啟動 [Spring_Eureka](../Spring_Eureka) 與 [Spring_Eureka_Provider](../Spring_Eureka_Provider)

```bash
curl -i http://localhost:10000/first-service/api/hello
curl http://localhost:10000/books-service/api/books
```

### Docker

在**本資料夾**執行：

```bash
docker compose up --build
```

| 服務 | 對主機開放 | 說明 |
|---|---|---|
| `gateway` | `10000` | **唯一的 API 入口** |
| `eureka` | `8761` | 只開放 dashboard 方便觀察 |
| `first-service` | 不開放 | Gateway 以 `FIRST_SERVICE_URL=http://first-service:8086` 轉送 |
| `provider` ×2 | 不開放 | Gateway 以 `lb://service-provider` 轉送 |
| `postgres` | 不開放 | 只給 Provider 使用 |

等約 30 秒讓 Provider 完成註冊後：

```bash
$ curl -i http://localhost:10000/first-service/api/hello
HTTP/1.1 200 OK
X-Gateway: api-gateway
Hello from first-service

$ for i in 1 2 3 4; do curl -s http://localhost:10000/books-service/api/hello; echo; done
Hello from 172.25.0.6:8084
Hello from 172.25.0.7:8084
Hello from 172.25.0.6:8084
Hello from 172.25.0.7:8084
```

Gateway 的 log 會顯示每個請求實際轉送到哪一台：

```text
GET /books-service/api/hello -> http://172.25.0.6:8084/api/hello 200 OK (4 ms)
GET /books-service/api/hello -> http://172.25.0.7:8084/api/hello 200 OK (4 ms)
```

```bash
docker compose down        # 停止（加上 -v 會刪除資料庫資料）
```

## 測試

```bash
./gradlew test
```

**不需要**啟動任何後端或 Docker。用 WireMock 模擬後端，`WebTestClient` 呼叫 Gateway；`books-service` 的兩台 Provider 用兩個 WireMock 代表，並以 `SimpleDiscoveryClient` 取代 Eureka。

| 測試 | 驗證內容 |
|---|---|
| `RouteTest` | StripPrefix / RewritePath 轉送後的路徑、後端狀態碼原樣轉回、兩台 Provider 輪流、`X-Gateway` header、`Location` 改寫、log 內容、未定義路徑與舊路由回 404 |
| `NoProviderInstanceTest` | `lb://` 找不到任何實例時回 `503` |

---

## Gateway 教學

### 1. 為什麼需要 API Gateway

沒有 Gateway 時，呼叫端要知道每個服務的位址，每個服務也都要各自處理登入驗證、CORS、記錄等共通功能：

```text
沒有 Gateway                          有 Gateway
呼叫端 ──▶ first-service :8086        呼叫端 ──▶ Gateway :10000 ──▶ first-service
呼叫端 ──▶ provider-1    :8084                                  └─▶ provider ×N
呼叫端 ──▶ provider-2    :8084
```

| 功能 | 說明 |
|---|---|
| **統一入口** | 呼叫端只需要知道一個網址，後端可以自由拆分、搬移、擴充 |
| **路由** | 依路徑、header、host 等條件轉送到不同服務 |
| **共通功能集中處理** | 驗證（JWT）、CORS、限流、記錄、加上 header，不必每個服務都寫一次 |
| **隱藏內部結構** | 後端服務不對外開放 port（本模組的 Compose 就是如此） |

### 2. 路由的組成

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: first-service                               # 路由名稱
              uri: ${FIRST_SERVICE_URL:http://localhost:8086} # 轉送到哪裡
              predicates:                                     # 條件：符合才走這條路由
                - Path=/first-service/**
              filters:                                        # 轉送前後的加工
                - StripPrefix=1
```

```text
請求進來 → 依序比對每條路由的 predicates → 第一個符合的路由
        → 執行 filters（改寫路徑、加 header…）→ 轉送到 uri → 回應再經過 filters → 回傳
沒有任何路由符合 → 404
```

常用的 predicate 還有 `Method=GET`、`Header=X-Version, v2`、`Host=**.example.com`、`After=<時間>` 等，可以組合使用。

### 3. StripPrefix vs RewritePath

兩條路由用不同方式做到同一件事：**去掉 Gateway 用來分辨服務的前綴**。

```yaml
- StripPrefix=1                                               # 去掉第一段路徑
- RewritePath=/books-service/(?<segment>.*), /$\{segment}    # 用正規表示式改寫
```

| | StripPrefix | RewritePath |
|---|---|---|
| 寫法 | 簡單，只指定要去掉幾段 | 正規表示式，彈性較大 |
| 適合 | 單純去掉前綴 | 前綴改名、插入或調整路徑結構（例如 `/v1/books` → `/api/books`） |

> YAML 中的 `$\{segment}`：`${...}` 會被 Spring 當成設定值的佔位符，所以要寫成 `$\{...}`。

#### 修正前的 Bug：經過 Gateway 永遠 404

```yaml
- Path=/employee/**
- StripPrefix=1
```

```text
呼叫 Gateway：     GET /employee/message
StripPrefix=1 後： GET /message           ← 轉送到後端的路徑
後端實際路徑：     /employee/message      → 對不上，404
```

**後端路徑**與 **Gateway 的前綴設計**要一起考慮。本專案的做法：後端只提供 `/api/...`，前綴（`/first-service`）只存在於 Gateway，用來判斷要轉給誰。

### 4. `lb://`：透過服務註冊中心轉送

```yaml
uri: lb://service-provider
```

`lb://` 表示「這是服務名稱，不是網址」：Gateway 向 Eureka 查詢 `service-provider` 的實例，再由 Spring Cloud LoadBalancer 挑一台。這與 [Spring_Eureka_Client](../Spring_Eureka_Client) 的 `@FeignClient(name = "service-provider")` 是同一套機制。

| 情境 | 結果 |
|---|---|
| 有兩個實例 | 輪流轉送（Round Robin） |
| 找不到任何實例 | `503 Service Unavailable` |

### 5. Filters

#### 套用到所有路由：`default-filters`

```yaml
default-filters:
  - AddResponseHeader=X-Gateway, api-gateway
```

#### 改寫回應 header：`RewriteResponseHeader`

在 Docker 實測時發現的問題：Provider 新增資料後回傳 `Location: /api/books/1`，因為它不知道自己在 Gateway 後面。呼叫端拿這個路徑回來呼叫 Gateway 會得到 404。

```yaml
- RewriteResponseHeader=Location, ^/api/, /books-service/api/
```

改寫後 `Location: /books-service/api/books/1`，可以直接透過 Gateway 使用。**改寫請求路徑時，也要注意回應中有沒有包含路徑的 header。**

#### 自訂全域 filter：`GlobalFilter`

[RequestLoggingFilter](src/main/java/com/example/demo/filter/RequestLoggingFilter.java) 記錄每個請求，套用到所有路由，不需要在 YAML 設定：

```java
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return chain.filter(exchange)            // 交給後面的 filter 與後端
                .doFinally(signal -> log.info(...)); // 整個請求結束後才執行
    }

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;       // 最先執行，才能算到完整耗時
    }
}
```

Gateway 建立在 WebFlux（非阻塞）之上，filter 回傳 `Mono`：不能用一般的 try / finally 計時，要用 `doFinally` 在非同步流程結束時處理。

| 類型 | 套用範圍 | 寫法 |
|---|---|---|
| `GatewayFilter` | 單一路由 | YAML 的 `filters`，或自訂 `GatewayFilterFactory` |
| `default-filters` | 所有路由 | YAML |
| `GlobalFilter` | 所有路由 | 實作介面並註冊成 Bean |

### 6. YAML vs Java 設定路由

本模組使用 YAML。同樣的路由也可以用 Java 設定：

```java
@Bean
RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
            .route("first-service", r -> r.path("/first-service/**")
                    .filters(f -> f.stripPrefix(1))
                    .uri("http://localhost:8086"))
            .build();
}
```

| | YAML | Java（`RouteLocator`） |
|---|---|---|
| 修改方式 | 改設定，不必重新編譯；可搭配 Config Server 動態更新 | 改程式碼，要重新編譯部署 |
| 彈性 | 使用內建的 predicate / filter | 可以寫任意判斷邏輯，有型別檢查 |
| 適合 | 大多數路由 | 需要複雜條件或自訂邏輯的路由 |

### 7. WebFlux 版 vs WebMVC 版

Spring Cloud Gateway 現在有兩種版本：

| | WebFlux（本模組） | WebMVC |
|---|---|---|
| Starter | `spring-cloud-starter-gateway-server-webflux` | `spring-cloud-starter-gateway-server-webmvc` |
| 執行環境 | Netty，非阻塞 | Servlet（Tomcat），可搭配 Virtual Threads |
| 路由設定前綴 | `spring.cloud.gateway.server.webflux.routes` | `spring.cloud.gateway.server.webmvc.routes` |
| 自訂 filter | `GlobalFilter`、`Mono` | 一般的 `HandlerFilterFunction` |
| 適合 | 大量連線、既有專案（最早的版本，文件最完整） | 團隊熟悉 Servlet、不想學 Reactive |

### 8. Gateway vs Nginx / Kubernetes Ingress

| | Spring Cloud Gateway | Nginx / K8S Ingress |
|---|---|---|
| 設定 | Java / YAML，與 Spring 生態整合（Eureka、Security） | Nginx 設定檔 / Ingress YAML |
| 自訂邏輯 | 用 Java 寫 filter | 較受限（Lua、annotation） |
| 服務發現 | `lb://` 整合 Eureka | K8S Service |
| 常見分工 | 應用層：驗證、依使用者限流、API 組合 | 基礎設施層：TLS、靜態檔案、外部流量入口 |

實務上兩者常**同時存在**：Ingress / Nginx 處理外部流量與 TLS，再轉給 Gateway 處理應用層的共通邏輯。

### 9. 錯誤回應的格式

Gateway 自己產生的錯誤（沒有符合的路由 404、沒有可用實例 503）使用 Spring Boot WebFlux 預設的 JSON 格式，**不是** ProblemDetail；後端服務回傳的錯誤（例如 Provider 的 404 ProblemDetail）則原樣轉回。若要統一成 ProblemDetail，需要自訂 `ErrorWebExceptionHandler`，本模組暫不處理。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 WireMock 路由測試（固定使用 8081），確認「經過 Gateway 永遠 404」的 Bug |
| 2 | Gradle 7.6 → 8.14.3 |
| 3 | Java 17 → 21、Spring Boot 3.0.2 → 3.5.16、Spring Cloud 2022.0.1 → 2025.0.3；改用新的 starter 與設定前綴（見下方） |
| 4 | Spring Boot → 4.1.1、Spring Cloud → 2025.1.3（Gateway 5.0）、Gradle → 9.8.0 |
| 5 | 修正 first-service 路由（`/first-service/**` + StripPrefix），移除指向不存在服務的 `consumerModule` |
| 6 | 新增 `books-service` 路由（`lb://` + Eureka + RewritePath）、`default-filters`、自訂 `GlobalFilter`；移除未使用的 Lombok |
| 7 | 改寫 `books-service` 回應的 `Location` header（Docker 實測時發現） |
| 8 | 新增 Dockerfile 與 docker-compose（Gateway 為唯一入口），修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| first-service | `GET /employee/message` → 轉成 `/message`，**永遠 404** | `GET /first-service/api/hello` → `/api/hello` |
| 第二條路由 | `/consumer/**` → `localhost:8082`（不存在的服務） | `/books-service/**` → `lb://service-provider` |
| 回應 header | 無 | 所有回應加上 `X-Gateway: api-gateway` |
| 後端位址 | 寫死 `localhost:8081` | `FIRST_SERVICE_URL`（預設 `localhost:8086`） |

### 升級時學到的事

- **Gateway 的 starter 與設定前綴改名**（Spring Cloud 2025.0 起）：

  | 舊 | 新 |
  |---|---|
  | `spring-cloud-starter-gateway` | `spring-cloud-starter-gateway-server-webflux` |
  | `spring.cloud.gateway.routes` | `spring.cloud.gateway.server.webflux.routes` |

  舊名稱在 2025.0 仍可運作，但啟動時會出現 WARN：「spring-cloud-starter-gateway is deprecated. It will be removed in the next major release.」以及設定 key 的對照。**先升版本、看 WARN、再依指示改名**，比一次全部改掉更容易確認每一步沒有出錯。
- **不需要另外加 `spring-boot-starter-webflux`**：Gateway 的 starter 已經包含。
- **Boot 4 的 `WebTestClient`**：`@SpringBootTest` 不再自動建立，需要加上 `@AutoConfigureWebTestClient`（`spring-boot-webtestclient`，由 `spring-boot-starter-webflux-test` 帶入）。
- **啟動時的 `HV000271` WARN**：來自 Gateway 本身的設定類別（`@Valid` 用在 List 上），與本專案的程式無關。
