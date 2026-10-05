# Spring Gateway Client - 放在 Gateway 後面的後端服務（first-service）

[Spring_Gateway_Server](../Spring_Gateway_Server) 的 `first-service` 路由所轉送的後端服務。

> 名稱雖然叫 Gateway **Client**，實際上是「**被 Gateway 轉送的一方**」：一般的 Spring MVC 應用程式，本身不需要任何 Gateway 相關依賴。Gateway 的說明請見 [Spring_Gateway_Server 的 README](../Spring_Gateway_Server/readme.md)。

這是本 Repo 早期的練習，已完成現代化（Spring Boot 3.0 → 4.1）。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、MockMvc（`@WebMvcTest`） |

## 執行方式

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

| 呼叫方式 | 網址 |
|---|---|
| 直接呼叫 | `http://localhost:8086/api/hello` |
| 經過 Gateway | `http://localhost:10000/first-service/api/hello` |

兩者都回傳 `Hello from first-service`。在 Gateway 的 Docker Compose 中，本服務**不對主機開放 port**，只能經過 Gateway 呼叫。

## 路徑設計

```text
呼叫端 → Gateway：/first-service/api/hello
                     │ StripPrefix=1（去掉 /first-service）
                     ▼
本服務：             /api/hello
```

本服務只提供 `/api/...` 路徑，**不需要知道自己在 Gateway 後面**；前綴 `/first-service` 只存在於 Gateway，用來判斷「轉給誰」。

## 測試

```bash
./gradlew test
```

`HelloControllerTest` 使用 `@WebMvcTest` 只載入 Web 層，不啟動完整應用程式。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 MockMvc 測試，固定原本行為 |
| 2 | Gradle 7.6 → 8.14.3 |
| 3 | Java 17 → 21、Spring Boot 3.0.2 → 3.5.16 |
| 4 | Spring Boot → 4.1.1（`@WebMvcTest` 移至 `org.springframework.boot.webmvc.test.autoconfigure`）、Gradle → 9.8.0 |
| 5 | `/employee/message` → `/api/hello`、`System.out` → SLF4J、port 8081 → 8086、移除未使用的 Lombok |
| 6 | 新增 Dockerfile，由 Spring_Gateway_Server 的 docker-compose 啟動；修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 路徑 | `GET /employee/message` | `GET /api/hello` |
| 回應 | `Hello JavaInUse Called in First Service` | `Hello from first-service` |
| Port | `8081`（與 Spring_JPA 衝突） | `8086` |
| 記錄 | `System.out.println("success")` | SLF4J `log.info(...)` |
