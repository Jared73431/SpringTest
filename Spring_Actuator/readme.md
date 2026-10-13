# Spring Boot Actuator - 觀察執行中的應用程式

應用程式上線後，維運人員與監控系統需要知道：**活著嗎？能接流量嗎？跑多快？出了什麼錯？** Actuator 提供這些資訊的端點，不必自己寫。

```text
                    ┌── 8115  /api/books        對外的 API
應用程式 ───────────┤
                    └── 9115  /actuator/**      只對內：health、info、metrics、prometheus、loggers
                                  │
                    Prometheus（每 5 秒抓 /actuator/prometheus）──→ Grafana（dashboard）
```

被觀察的應用程式是一個簡單的書籍 API（PostgreSQL），讓 health、metrics 有實際的內容。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-actuator`） |
| Micrometer | 隨 Spring Boot（`micrometer-registry-prometheus`） |
| Prometheus / Grafana | v3.13 / 13.0（Docker） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL）、真實的 server（`RestClient`） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_EndpointExposureTest](src/test/java/com/example/demo/lesson/L01_EndpointExposureTest.java) | 有哪些端點、透過 HTTP 公開哪些；哪些不能公開 |
| 2 | [L02_HealthTest](src/test/java/com/example/demo/lesson/L02_HealthTest.java)、[L02_HealthDownTest](src/test/java/com/example/demo/lesson/L02_HealthDownTest.java) | 各項檢查、自訂 `HealthIndicator`、liveness vs readiness |
| 3 | [L03_InfoAndMetricsTest](src/test/java/com/example/demo/lesson/L03_InfoAndMetricsTest.java) | build 資訊；自動的 `http.server.requests`；自訂的 Counter / Timer；Prometheus 格式 |
| 4 | [L04_LoggersTest](src/test/java/com/example/demo/lesson/L04_LoggersTest.java) | 不重新啟動就改變 log level |
| 5 | [L05_ManagementPortTest](src/test/java/com/example/demo/lesson/L05_ManagementPortTest.java) | Actuator 放在另一個 port，和對外的 API 分開 |

## 專案結構

```text
src/main/java/com/example/demo/
├── book/                                 被觀察的書籍 API（/api/books）
│   └── BookService                       自訂 metrics：bookstore.books.added、bookstore.books.search
└── health/CoverStorageHealthIndicator    自訂的健康檢查：封面目錄是否存在、可以寫入
src/main/resources/application.properties Actuator 的設定（依課程分段說明）
monitoring/
├── prometheus.yml                        Prometheus 抓哪裡
└── grafana/                              自動建立資料來源與 dashboard
```

## 執行方式

### 測試

需要 Docker（Testcontainers 啟動 PostgreSQL，不會連到本機的資料庫）。

```bash
./gradlew test
```

### Docker Compose（含 Prometheus、Grafana）

```bash
docker compose up --build -d
# Windows 保留了 8115 時：APP_PORT=8315 docker compose up --build -d

curl http://localhost:8115/api/books
curl http://localhost:9115/actuator/health
curl http://localhost:9115/actuator/prometheus

# Prometheus：http://localhost:9090   （Status → Targets：spring-actuator 是 UP）
# Grafana：   http://localhost:3000   （Dashboards → Spring → Spring Actuator，不需要登入）

docker compose down -v
```

Grafana 的圖需要一點流量和時間：多呼叫幾次 API，等 1 分鐘左右（`rate()` 需要至少兩次抓取的資料）。

### 本機執行

```bash
./gradlew bootRun      # 需要資料庫 Actuator；API 在 8115，Actuator 在 9115
```

### 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的服務實際執行過。Environment 變數：`baseUrl` = `http://localhost:8115`、`managementUrl` = `http://localhost:9115`。

| 請求 | 設定 | 結果 |
|---|---|---|
| `GET {{managementUrl}}/actuator/health` | | `UP`，`components` 有 db、diskSpace、coverStorage |
| `GET {{managementUrl}}/actuator/health/readiness` | | `UP` |
| `GET {{managementUrl}}/actuator/info` | | `build.artifact`：`Spring_Actuator` |
| `GET {{managementUrl}}/actuator/env` | | 404（沒有公開） |
| `GET {{baseUrl}}/actuator/health` | | 404（不在 API 的 port） |
| `POST {{baseUrl}}/api/books` | Body **raw** / **JSON**：`{"title":"Clean Code","author":"Robert C. Martin"}` | 201 |
| `GET {{managementUrl}}/actuator/metrics/bookstore.books.added` | | `measurements[0].value` ≥ 1 |
| `GET {{managementUrl}}/actuator/metrics/http.server.requests?tag=uri:/api/books` | | `/api/books` 的請求次數 |
| `POST {{managementUrl}}/actuator/loggers/com.example.demo.book` | Body：`{"configuredLevel":"DEBUG"}` | 204 |
| `GET {{managementUrl}}/actuator/loggers/com.example.demo.book` | | `effectiveLevel`：`DEBUG` |
| `POST {{managementUrl}}/actuator/loggers/com.example.demo.book` | Body：`{"configuredLevel":null}` | 204（恢復） |

## 教學

### 1. 端點：存在 ≠ 公開（第 1 課）

| 端點 | 用途 | 本模組 |
|---|---|---|
| `health` | 健康狀態 | ✅ 公開（預設也只公開這個） |
| `info` | 版本、Java、自訂資訊 | ✅ |
| `metrics` / `prometheus` | 指標 | ✅ |
| `loggers` | 查看 / 修改 log level | ✅ |
| `env`、`configprops` | 所有設定值（可能有密碼） | ❌ |
| `heapdump`、`threaddump` | 記憶體內容、執行緒狀態 | ❌ |
| `shutdown` | 關閉應用程式 | ❌（預設也停用） |

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus,loggers
```

**不要用 `include=*`**：`env`、`heapdump` 會把資料庫密碼、記憶體中的資料交給任何連得到的人。

### 2. Health 與 liveness / readiness（第 2 課）

`/actuator/health` 是所有檢查項目的總和，任何一項 DOWN 就是 DOWN（HTTP 503）。Spring Boot 依照使用的技術自動加上檢查（`db`、`diskSpace`、Redis、Kafka…），也可以自己寫：

```java
@Component
public class CoverStorageHealthIndicator extends AbstractHealthIndicator {   // 名稱 → coverStorage
    protected void doHealthCheck(Health.Builder builder) {
        if (Files.isDirectory(directory) && Files.isWritable(directory)) builder.up();
        else builder.down().withDetail("error", "...");
    }
}
```

K8S 用兩種探測決定怎麼處理容器：

| | liveness（活著嗎？） | readiness（能接流量嗎？） |
|---|---|---|
| 失敗時 | **重新啟動**容器 | **先不送流量**過來，不重新啟動 |
| 本模組包含 | `livenessState` | `readinessState`、`db`、`coverStorage` |

```properties
management.endpoint.health.group.readiness.include=readinessState,db,coverStorage
```

**資料庫不要放進 liveness**：資料庫斷線時重新啟動應用程式也沒用，反而所有容器一起重新啟動，讓狀況更糟。

應用程式也可以自己宣告暫時不接流量（例如預熱快取）：`AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC)`。

### 3. Info 與 Metrics（第 3 課）

**Info**：`build.gradle` 的 `springBoot { buildInfo() }` 產生版本、build 時間；`management.info.java.enabled` 加上 Java 版本。上線後可以直接確認「現在跑的是哪一版」。

**Metrics**（Micrometer）：

| 來源 | 例子 |
|---|---|
| Spring Boot 自動記錄 | `http.server.requests`（每個 API 的次數、時間，依 uri / status 分類）、`jvm.memory.used`、`hikaricp.connections.active`、`logback.events` |
| 自己記錄 | `bookstore.books.added`（Counter）、`bookstore.books.search`（Timer） |

```java
Counter booksAdded = Counter.builder("bookstore.books.added").register(meterRegistry);
booksAdded.increment();

Timer searchTimer = Timer.builder("bookstore.books.search").register(meterRegistry);
searchTimer.record(() -> bookRepository.searchByTitle(keyword));
```

| 類型 | 用途 |
|---|---|
| Counter | 只會增加的次數（建立了幾本書、發生幾次錯誤） |
| Timer | 次數 + 花了多久（API、搜尋） |
| Gauge | 目前的值，可增可減（佇列長度、連線數） |

查詢：`/actuator/metrics/http.server.requests?tag=uri:/api/books&tag=status:200`。

**Prometheus**：Prometheus 定時來「拉」（pull）`/actuator/prometheus`。名稱的 `.` 換成 `_`，Counter 加上 `_total`；`management.metrics.tags.application` 讓每個 metric 都帶上應用程式名稱。

### 4. Loggers（第 4 課）

線上有問題時，暫時打開某個 package 的 DEBUG，不必改設定、重新部署：

```bash
curl -X POST http://localhost:9115/actuator/loggers/com.example.demo.book \
     -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
# 查完恢復
curl -X POST ... -d '{"configuredLevel":null}'
```

修改只在記憶體中：重新啟動後恢復設定檔的值；多個實例時每個實例都要改。

### 5. 保護 Actuator：另一個 port（第 5 課）

```properties
management.server.port=9115
```

| | 8115 | 9115 |
|---|---|---|
| `/api/**` | ✅ | 404 |
| `/actuator/**` | 404 | ✅ |

防火牆、Docker、K8S 只對外開放 8115，Actuator 只有內部的程式（Prometheus、K8S 探測）連得到。本模組的 Docker Compose 把 9115 綁在 `127.0.0.1`，只有本機可以查看。

正式環境通常**再加上 Spring Security**（例如 `/actuator/**` 需要 ADMIN 角色，做法見 [Spring_Security2](../Spring_Security2)），兩者可以一起使用。只用 Security 而不分 port 也可以，但要注意 Actuator 的路徑規則不要寫錯。

## 沒有實作的進階主題

| 主題 | 說明 |
|---|---|
| **分散式追蹤（Tracing）** | 一個請求經過好幾個服務時，用同一個 trace id 串起來，看每一段花了多久。Spring Boot 透過 Micrometer Tracing / OpenTelemetry 支援，搭配 Zipkin、Jaeger、Grafana Tempo 查看 |
| **告警（Alerting）** | Prometheus 的 alert rule（例如 5 分鐘內 5xx 超過 1%）+ Alertmanager 發通知；或用 Grafana 的告警 |
| **集中的 log** | 多個實例的 log 送到 Loki / Elasticsearch，用 trace id 找到同一個請求的所有 log |
| **Metrics、Logs、Traces** | 可觀測性（Observability）的三個面向：本模組做的是 Metrics，加上 Health |

## 面試時能說明的重點

**Q：Actuator 是什麼？**
Spring Boot 提供的維運端點：health（健康狀態）、info（版本）、metrics（指標）、loggers（log level）等，讓監控系統與維運人員觀察執行中的應用程式。

**Q：liveness 和 readiness 有什麼不同？**
liveness 失敗，K8S 會重新啟動容器；readiness 失敗，K8S 只是先不送流量過來。資料庫斷線這類外部問題放 readiness，不放 liveness，否則所有容器會一起被重新啟動。

**Q：Actuator 要怎麼保護？**
只公開需要的端點，不公開 env、heapdump；Actuator 放在另一個只對內開放的 port；正式環境再加上 Spring Security。

**Q：怎麼監控 API 的回應時間？**
Spring Boot 自動把每個 API 的次數與時間記錄在 `http.server.requests`；用 Prometheus 定時抓 `/actuator/prometheus`，在 Grafana 畫出每秒請求數、p95 回應時間。

**Q：怎麼加自己的指標？**
注入 `MeterRegistry`，用 Counter 記次數、Timer 記時間、Gauge 記目前的值。

## 開發時遇到的問題

- **[Learning] Counter 的名稱不要用 `created` 結尾**：一開始取名 `bookstore.books.created`，在 Prometheus 格式中卻變成 `bookstore_books_total`。OpenMetrics 把 `_created` 保留給「counter 建立的時間」，Prometheus client 會把這個結尾去掉。改名為 `bookstore.books.added`。
- **Spring Boot 4 的 package 變更**：`HealthIndicator`、`AbstractHealthIndicator`、`Health` 移到 `org.springframework.boot.health.contributor`（舊版是 `org.springframework.boot.actuate.health`）。
- **`rate()` 一開始沒有資料**：Prometheus 的 `rate()` 至少要兩次抓取的資料，新的 metric 要等一下 Grafana 才有圖。
- **測試中 `RestClient` 的 URI 樣板**：查詢 `tag=uri:/api/books/{id}` 時，`{id}` 會被當成樣板變數而失敗，要改用 URI 變數傳入。
