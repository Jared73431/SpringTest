# Spring Eureka Client - 用服務名稱呼叫其他服務

服務消費者：透過 [Spring_Eureka](../Spring_Eureka) 找到 [Spring_Eureka_Provider](../Spring_Eureka_Provider)，再用 OpenFeign 呼叫。示範：

- **服務發現**：`@FeignClient` 只寫服務名稱，不寫網址
- **負載平衡**：Provider 開兩台時，請求會輪流送到兩台
- 測試不需要 Eureka：用 `SimpleDiscoveryClient` + WireMock 模擬兩台 Provider
- Docker Compose 一次啟動 Eureka、Provider ×2、Client、PostgreSQL

> - Eureka 原理（註冊、心跳、自我保護、與 K8S 的比較）見 [Spring_Eureka](../Spring_Eureka/readme.md)
> - Feign 本身（ErrorDecoder、設定、DTO 要不要共用、OpenFeign 現況）見 [Spring_Feign_Client](../Spring_Feign_Client/readme.md)

這是本 Repo 早期的練習，已完成現代化（Spring Boot 3.0 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood）：Netflix Eureka Client、OpenFeign、LoadBalancer |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、WireMock 3、TestRestTemplate |

## Feign Client vs Eureka Client

兩個模組的程式幾乎相同，差別只在「怎麼找到對方」：

```java
// Spring_Feign_Client：直接指定網址
@FeignClient(name = "book-server", url = "${book-server.url}")

// Spring_Eureka_Client（本模組）：只寫服務名稱
@FeignClient(name = "service-provider")
```

```text
bookClient.findAll()
  → 服務名稱 service-provider
  → DiscoveryClient：向 Eureka 查到 172.25.0.5:8084、172.25.0.6:8084
  → LoadBalancer：挑一台（預設 Round Robin，輪流）
  → GET http://172.25.0.6:8084/api/books
```

| | Spring_Feign_Client | Spring_Eureka_Client |
|---|---|---|
| 對方位址 | 設定檔寫死一個網址 | 執行時向 Eureka 查詢 |
| 對方開多台 | 要另外架 Load Balancer | 呼叫端自動輪流（Spring Cloud LoadBalancer） |
| 對方 IP 改變 | 要改設定 | 不用改，對方重新註冊即可 |
| 額外依賴 | 無 | 需要 Eureka Server |

## 執行方式

### 本機執行

依序啟動 [Spring_Eureka](../Spring_Eureka)、[Spring_Eureka_Provider](../Spring_Eureka_Provider)（需要 PostgreSQL），再啟動本模組：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
curl http://localhost:8085/api/hello     # Hello from <Provider 的實例 id>
```

- Port：`8085`；Eureka 位址預設 `http://localhost:8761/eureka/`，可用環境變數 `EUREKA_URL` 修改
- 剛啟動時可能回 `503`：Client 還沒從 Eureka 取得 Provider 的位址，等幾秒再試

### Docker

只需要安裝 Docker，在**本資料夾**執行：

```bash
docker compose up --build
```

| 服務 | 對主機開放 | 說明 |
|---|---|---|
| `eureka` | `8761` | dashboard：http://localhost:8761 |
| `provider` ×2 | 不開放 | `deploy.replicas: 2` 啟動兩個實例，都以 `service-provider` 註冊 |
| `client` | `8085` | 只知道服務名稱，實際位址由 Eureka 提供 |
| `postgres` | 不開放 | 只給 Provider 使用 |

等約 30 秒讓服務完成註冊後，連續呼叫幾次：

```bash
$ for i in 1 2 3 4; do curl -s http://localhost:8085/api/hello; echo; done
Hello from 172.25.0.6:8084
Hello from 172.25.0.5:8084
Hello from 172.25.0.6:8084
Hello from 172.25.0.5:8084
```

兩個 IP 輪流出現，就是負載平衡。停止其中一台後，請求會全部送到剩下的那一台。

```bash
docker compose down        # 停止（加上 -v 會刪除資料庫資料）
```

> Provider 不對主機開放 port：兩個實例不能綁定主機的同一個 port，而且 Client 是透過 Eureka 找到它們，不需要從外部連線。

## API

Base URL：`http://localhost:8085`，每個端點都透過 Feign 轉呼叫 Provider 的同名端點。

| Method | Path | 說明 | 成功 | 失敗 |
|---|---|---|---|---|
| `GET` | `/api/hello` | 回傳處理這次請求的 Provider 實例 id | `200` | `503` |
| `GET` | `/api/books` | 查詢全部 | `200` | `502` / `503` |
| `GET` | `/api/books/{id}` | 查詢一筆 | `200` | `404` |
| `POST` | `/api/books` | 新增 | `201` + `Location` header | `400` |
| `PUT` | `/api/books/{id}` | 修改 | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | 刪除 | `204` | `404` |

### 錯誤回應

| 情境 | Client 回應 |
|---|---|
| Provider 回 4xx（404、400…） | **原樣轉回**：狀態碼與 ProblemDetail 內容不變 |
| Eureka 上沒有任何 Provider 實例 | `503`（LoadBalancer 找不到實例） |
| 選到的實例連不上（已停止但尚未從清單移除、逾時） | `503` |
| Provider 回其他 5xx | `502 Bad Gateway` |

## 測試

```bash
./gradlew test
```

**不需要** Eureka、Provider、資料庫或 Docker。測試關閉 Eureka（`eureka.client.enabled=false`），改用 Spring Cloud 內建的 **SimpleDiscoveryClient**，在設定中直接列出服務的實例：

```java
registry.add("spring.cloud.discovery.client.simple.instances.service-provider[0].uri", providerA::baseUrl);
registry.add("spring.cloud.discovery.client.simple.instances.service-provider[1].uri", providerB::baseUrl);
```

因此「用服務名稱找到實例 → LoadBalancer 挑選 → 送出請求」這段流程仍然完整經過，只是清單來源從 Eureka 換成設定檔。

| 測試 | 內容 |
|---|---|
| `BookControllerTest` | 每個 API 的成功情境、Provider 回 404 / 400 / 500 時 Client 的回應 |
| `LoadBalancingTest` | 兩個 WireMock 代表兩台 Provider，確認請求輪流送到兩台 |
| `BookProviderUnavailableTest` | 沒有任何實例、實例連不上，兩種情況都回 `503` |

## 設定

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          service-provider:            # 對應 @FeignClient(name = "service-provider")
            connect-timeout: 2000
            read-timeout: 5000
            logger-level: basic        # log 中可看到這次請求送到哪個 IP
    loadbalancer:
      cache:
        ttl: 5s                        # 預設 35 秒，練習用縮短
eureka:
  client:
    registry-fetch-interval-seconds: 5 # 預設 30 秒，練習用縮短
```

縮短兩個間隔是為了練習時能較快看到新啟動的 Provider；正式環境通常保留預設，原因見 [Spring_Eureka「時間相關的預設值」](../Spring_Eureka/readme.md#3-時間相關的預設值為什麼剛啟動要等一下)。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 WireMock 測試（SimpleDiscoveryClient 取代 Eureka），固定原本行為 |
| 2 | Gradle 7.6 → 8.14.3 |
| 3 | Java 17 → 21、Spring Boot 3.0.1 → 3.5.16、Spring Cloud 2022.0.0 → 2025.0.3；移除 Netflix 候選版本 repository |
| 4 | Spring Boot → 4.1.1、Spring Cloud → 2025.1.3、Gradle → 9.8.0 |
| 5 | 重新設計：`/api/books` 完整 CRUD、有型別的 DTO、ErrorDecoder、逾時與 log 設定；port 8080 → 8085；負載平衡與無法連線的測試 |
| 6 | 新增 Dockerfile 與 docker-compose（Eureka + Provider ×2 + Client + PostgreSQL），修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | `8080` | `8085` |
| 連線測試 | `GET /Hello` | `GET /api/hello`（回傳 Provider 實例 id） |
| 查詢全部 | `GET /findall` | `GET /api/books` |
| 查詢一筆 / 新增 / 修改 / 刪除 | 無 | `GET` / `POST` / `PUT` / `DELETE /api/books[/{id}]` |
| Provider 回錯誤 | 一律 `500` | 4xx 原樣轉回、無可用實例 → `503`、其他 5xx → `502` |

### 舊寫法整理

- **`@FeignClient` 介面上的 `@Component`**：不需要，`@EnableFeignClients` 已經會掃描並註冊
- **`@Autowired(required = true)`**：`required` 預設就是 `true`；改用 Constructor Injection 後也不需要 `@Autowired`
- **`List<?>`**：每一筆都會變成 `LinkedHashMap`，改用 `List<BookResponse>`
