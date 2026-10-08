# Spring Swagger - OpenAPI 文件

以 springdoc-openapi 為一個使用者管理 API 自動產生 OpenAPI 3.1 文件與 Swagger UI，並整理實務上常用的做法：

- 錯誤回應（ProblemDetail）的文件化
- 哪些內容會自動推導、哪些才需要註解
- 依環境關閉文件
- 把規格當成契約來測試
- 匯出規格檔

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.2 → 4.1、springdoc 2.2 → 3.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Spring MVC、Spring Data JPA、Validation） |
| springdoc-openapi | 3.1.1（`springdoc-openapi-starter-webmvc-ui`，產生 OpenAPI 3.1） |
| 資料庫 | PostgreSQL（資料庫名稱 `Swagger`），Flyway 管理資料表 |
| Build Tool | Gradle 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL）、JsonPath |

> springdoc 的版本要對應 Spring Boot：Boot 3.x 用 springdoc 2.x，Boot 4 用 springdoc 3.x。

## 架構

```text
Client / Swagger UI
  ↓
UserController       @RestController + @Operation（只寫摘要）
  ↓
UserService          重複檢查、交易
  ↓
UserRepository       JpaRepository
  ↓
PostgreSQL           users 表由 Flyway 建立

OpenApiConfig        文件資訊、錯誤回應與 ProblemDetail schema（OpenApiCustomizer）
```

```text
src/main/java/com/example/demo/
├── config/OpenApiConfig
├── controller/UserController
├── service/UserService
├── repository/UserRepository
├── entity/User
├── dto/UserRequest、UserResponse（record）
└── exception/GlobalExceptionHandler、UserNotFoundException、DuplicateUserException

src/main/resources/
├── application.yml
├── application-prod.yml           關閉 /v3/api-docs 與 Swagger UI
└── db/migration/V1__create_users.sql
```

## 執行方式

### Docker Compose（建議）

```bash
docker compose up --build
```

- Swagger UI：http://localhost:8097/swagger-ui.html
- OpenAPI 規格：http://localhost:8097/v3/api-docs（YAML：`/v3/api-docs.yaml`）

```bash
SPRING_PROFILES_ACTIVE=prod docker compose up    # 正式環境設定：文件與 Swagger UI 都是 404
docker compose --profile tools up -d             # 另外啟動 pgAdmin：http://localhost:5050
docker compose down -v                           # 停止並刪除資料庫
```

> Windows 上 port 無法綁定時（Hyper-V / WSL 動態保留了 port），改用其他主機 port：`APP_PORT=8300 docker compose up --build`。

### 本機執行

需要本機的 PostgreSQL，並先建立資料庫：

```sql
CREATE DATABASE "Swagger";
```

```bash
./gradlew bootRun                                          # Windows：gradlew.bat bootRun
./gradlew bootRun --args='--spring.profiles.active=prod'   # 正式環境設定
```

### 測試與匯出規格

需要 Docker（Testcontainers 會自動啟動 PostgreSQL，不會連到本機的資料庫）。

```bash
./gradlew test             # 全部測試
./gradlew exportOpenApi    # 匯出 build/openapi/openapi.json、openapi.yaml
```

| 測試類別 | 內容 |
|---|---|
| `UserApiTest` | API 行為：201 / 204、驗證錯誤、404、409 |
| `OpenApiContractTest` | OpenAPI 規格的契約測試，並匯出規格檔 |
| `ProdProfileTest` | prod profile 下文件與 Swagger UI 關閉，API 照常運作 |

### IDE

在 IntelliJ IDEA 開啟 `Spring_Swagger` 資料夾（或 `build.gradle`），選擇 **Open as Project**，IDE 會透過 Gradle 匯入。Gradle JVM 與 Project SDK 都使用 **JDK 21**。這個專案不使用 Lombok，不需要安裝外掛。

## API

| 方法 | URL | 成功 | 錯誤 |
|---|---|---|---|
| GET | `/api/users` | 200 | |
| GET | `/api/users/{id}` | 200 | 400、404 |
| POST | `/api/users` | 201 + `Location` | 400、409 |
| PUT | `/api/users/{id}` | 200 | 400、404、409 |
| DELETE | `/api/users/{id}` | 204 | 400、404 |

```json
{ "username": "john_doe", "name": "John Doe", "email": "john@example.com", "age": 25 }
```

| 欄位 | 驗證 | 資料表 |
|---|---|---|
| `username` | 必填，最多 50 字，不可重複 | `VARCHAR(50) NOT NULL UNIQUE` |
| `name` | 必填，最多 100 字 | `VARCHAR(100) NOT NULL` |
| `email` | 必填，Email 格式，最多 255 字，不可重複 | `VARCHAR(255) NOT NULL UNIQUE` |
| `age` | 必填，0～150 | `CHECK (age BETWEEN 0 AND 150)` |

錯誤一律使用 ProblemDetail（`application/problem+json`），驗證失敗時以 `errors` 列出每個欄位的錯誤：

```json
{ "status": 400, "title": "Bad Request", "detail": "Invalid request content.",
  "errors": { "email": "電子郵件格式不正確", "age": "年齡不可大於 150" } }
```

## 設定

| 設定 | 值 | 說明 |
|---|---|---|
| `server.port` | `8097` | |
| `spring.datasource.url` | `jdbc:postgresql://${DB_HOST:localhost}:5432/Swagger` | |
| `spring.jpa.hibernate.ddl-auto` | `validate` | 資料表由 Flyway 建立，Hibernate 只檢查一致性 |
| `springdoc.default-produces-media-type` | `application/json` | 否則文件上回應的 content type 是 `*/*` |
| `springdoc.swagger-ui.operations-sorter` | `method` | Swagger UI 依 HTTP 方法排序 |
| `springdoc.api-docs.enabled` / `swagger-ui.enabled` | `false`（prod） | 見 `application-prod.yml` |

---

## springdoc 教學

### 1. springdoc 會自動推導什麼

只要加入 `springdoc-openapi-starter-webmvc-ui`，springdoc 就會掃描 Controller，產生 `/v3/api-docs` 與 Swagger UI，**大部分內容不需要寫註解**：

| 來源 | 推導出的文件內容 |
|---|---|
| `@GetMapping("/{id}")`、`@PathVariable Long id` | 路徑、HTTP 方法、參數（必填、`int64`） |
| `@RequestBody UserRequest` | request body 的 schema |
| 方法回傳型別 `UserResponse`、`List<UserResponse>` | 回應的 schema |
| `@ResponseStatus(HttpStatus.NO_CONTENT)` | 回應狀態碼 204 |
| `@NotBlank` / `@NotNull` | `required` |
| `@Size(max = 50)` | `maxLength: 50` |
| `@Email` | `format: email` |
| `@Min(0)` / `@Max(150)` | `minimum` / `maximum` |
| record 的欄位 | schema 的 properties |

**需要補註解的地方：**

| 情況 | 做法 |
|---|---|
| 摘要、分組 | `@Operation(summary = ...)`、`@Tag` |
| 範例值 | `@Schema(example = "john_doe")`。沒有範例時，Swagger UI 的 Try it out 只會顯示 `"string"` |
| 回傳 `ResponseEntity` 時的狀態碼 | springdoc 推導不出 `ResponseEntity.created(...)` 的 201，會寫成 200，要加 `@ApiResponse(responseCode = "201")` |
| 錯誤回應 | 推導不出來，見第 2 點 |

> 修正前每個欄位都寫 `@Schema(description = ..., example = ..., required = true)`，Entity 和 DTO 各一份。`required` 已經 deprecated，而且 springdoc 本來就會從 `@NotBlank` 推導出來。
>
> 注意：`@NotBlank` 不會變成 `minLength: 1`，文件上看起來可以傳空字串（實際上驗證會擋下）。

### 2. 錯誤回應文件化

錯誤回應寫在 `GlobalExceptionHandler`，springdoc 從 Controller 方法看不出來。逐一在每個方法上寫 `@ApiResponses` 很容易漏寫或寫錯：修正前就寫了「新增成功 200」，實際上是 201。

這裡改用 `OpenApiCustomizer` 依規則統一加上：

| 規則 | 加上的回應 |
|---|---|
| 路徑有 `{id}` 或是 POST / PUT | 400 |
| 路徑有 `{id}` | 404 |
| POST / PUT | 409 |

```java
@Bean
OpenApiCustomizer problemDetailResponses() {
    return openApi -> {
        openApi.getComponents().addSchemas("ProblemDetail", problemDetailSchema());
        addErrorResponses(openApi);   // 依規則加上 application/problem+json 的 400 / 404 / 409
    };
}
```

兩個踩過的坑：

- **ProblemDetail 的 schema 要手寫。**Spring 的 `ProblemDetail` 類別有 `getProperties()`，直接交給 springdoc 推導，會多出一個不存在的 `properties` 欄位。
- **schema 要在 customizer 裡加入。**寫在 `OpenAPI` bean 的 `components` 中，會被 springdoc 掃描後產生的 components 蓋掉。

### 3. 不要寫死 servers

修正前：

```java
.servers(List.of(new Server().url("http://localhost:8080"), new Server().url("https://api.example.com")))
```

應用程式跑在 8020，Swagger UI 的「Try it out」卻打到 8080，**連這個模組自己的主題功能都無法使用**。不設定 servers 時，springdoc 會使用目前請求的網址。在 Docker 中透過 port 對應存取，也會是正確的主機 port。

### 4. 正式環境關閉文件

API 文件列出所有端點、參數與資料結構，正式環境通常會：

- 關閉，見 `application-prod.yml`：

  ```yaml
  springdoc:
    api-docs:
      enabled: false
    swagger-ui:
      enabled: false
  ```

- 或是保留，但放在需要登入的路徑或內部網路（例如搭配 Spring Security 限制 `/v3/api-docs/**`、`/swagger-ui/**`）。

### 5. 規格是契約：用測試鎖定

OpenAPI 規格是給前端、其他團隊或產生 client 的**契約**。改了 Controller 或 DTO，文件會跟著變，但不一定有人注意到。`OpenApiContractTest` 用 JsonPath 鎖定重點：

```java
assertThat(spec.read("$.paths['/api/users'].post.responses", Map.class)).containsOnlyKeys("201", "400", "409");
assertThat(spec.read("$.components.schemas.UserRequest.properties.username.maxLength", Integer.class)).isEqualTo(50);
```

欄位改名、必填改變、狀態碼改變時，測試就會失敗，提醒你確認對 API 使用者的影響。

### 6. 匯出規格檔

```bash
./gradlew exportOpenApi    # → build/openapi/openapi.json、openapi.yaml
```

規格檔可以交給前端，或用 [OpenAPI Generator](https://openapi-generator.tech/) 產生 TypeScript、Java、Kotlin… 的 client。這裡透過測試啟動應用程式（Testcontainers 提供資料庫），不需要另外準備環境。

- 匯出檔中的 `servers` 是測試時的隨機 port，產生 client 時請另外指定 base URL。
- 另一種做法是 [springdoc-openapi-gradle-plugin](https://github.com/springdoc/springdoc-openapi-gradle-plugin)：它會實際啟動應用程式，所以需要可以連線的資料庫。

### 7. Code-first vs Design-first

| | Code-first（本模組） | Design-first |
|---|---|---|
| 做法 | 先寫程式，由 springdoc 產生規格 | 先寫 `openapi.yaml`，再產生 server 介面與 client |
| 優點 | 文件和程式永遠一致，上手快 | 前後端可以先約定 API，平行開發 |
| 缺點 | 規格的品質取決於程式與註解 | 需要維護規格檔與程式碼產生流程 |

> 單一服務、團隊小時，Code-first 加上契約測試（第 5 點）通常就足夠。

### 8. 其他常用設定

| 需求 | 做法 |
|---|---|
| 依模組分組（例如 public / admin） | 定義多個 `GroupedOpenApi` bean（`pathsToMatch("/api/admin/**")`），Swagger UI 右上角可以切換 |
| 隱藏某個端點 | `@Hidden` |
| 文件中的 JWT 驗證 | `@SecurityScheme` + `@SecurityRequirement`，Swagger UI 會出現 Authorize 按鈕 |
| 自訂路徑 | `springdoc.api-docs.path`、`springdoc.swagger-ui.path` |

### 統一回應格式 vs HTTP 狀態碼 + ProblemDetail

修正前用 `ApiResponse<T>`（`code`、`message`、`data`）包裝每個回應，這在很多公司很常見：

| | 統一包裝（修正前） | 資源 + ProblemDetail（本模組） |
|---|---|---|
| 成功 | `{"code":200,"message":"取得成功","data":{...}}` | 直接回傳資源，狀態碼表達結果（200、201、204） |
| 錯誤 | `{"code":400,"message":"參數驗證失敗","data":null}` | RFC 9457 ProblemDetail |
| 優點 | 前端只要處理一種格式；可以放業務代碼 | 符合 HTTP 語意；Spring 原生支援；監控、閘道、快取都能直接依狀態碼運作 |
| 常見問題 | `code` 和 HTTP 狀態碼不一致（例如 HTTP 200 但 `code` 是錯誤）；文件要描述泛型包裝 | 需要業務錯誤代碼時，加在 ProblemDetail 的擴充欄位 |

本 Repo 的慣例是資源 + ProblemDetail。如果公司已經規定了統一包裝格式，照規定做即可，重點是**狀態碼要正確，格式要一致**：修正前的 404 就沒有使用包裝格式。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Baseline 測試（Testcontainers）：鎖定修正前的行為，取代會連到本機資料庫的 `contextLoads` |
| 2 | Java 17 → 21、Spring Boot 3.2.10 → 3.5.16、springdoc 2.2.0 → 2.8.17 |
| 3 | Spring Boot 4.1.1、springdoc 3.1.1、Gradle 8.14.3 → 9.8.0 |
| 4 | Flyway 取代 `ddl-auto: update`、明確引入 validation、移除 data-jdbc、設定清理、port 8097 |
| 5 | 修正新增 / 修改使用者的 Bug，改用資源 + ProblemDetail，record，移除 Lombok |
| 6 | springdoc：錯誤回應文件化、servers、prod profile、契約測試、匯出規格檔 |
| 7 | Dockerfile、docker compose |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| **新增 / 修改使用者完全無法使用** | `@RequestBody` import 成 Swagger 的 `io.swagger.v3.oas.annotations.parameters.RequestBody`，Spring 不讀 JSON body；`User(username, name, email, age)` 建構子的內容是空的 |
| **沒有驗證實作** | 只有 `jakarta.validation-api`，沒有 Hibernate Validator。Boot 3.2 + springdoc 2.2 時 `@Valid` 完全沒有作用：POST 回傳 200 並存入一筆**全部是 null** 的資料，PUT 把既有資料清成 null |
| 驗證錯誤的明細沒有回傳 | 收集了 `errors` 卻沒有放進回應 |
| catch-all 例外處理 | 所有錯誤都變成 500（id 不是數字、帳號重複），而且**沒有寫 log** |
| Swagger UI 的 Try it out 打錯 port | servers 寫死 8080，應用程式在 8020 |
| `@ApiResponses` 與實際不符 | 新增寫 200，實際上也是 200（應該是 201） |
| 應用程式名稱沒有設定 | YAML 寫成 `name:Spring_Swagger`，冒號後沒有空格 |
| YAML 註解看不懂 | 寫成 `連線...`，那是 `.properties` 才需要的跳脫寫法 |
| 其他 | data-jdbc 沒有使用；`ddl-auto: update`；Entity 直接當成回應；JPA Entity 使用 Lombok `@Data`；Field Injection；用詞是中國大陸用語（用戶、郵箱、獲取）；README 以 Maven 為主，但專案是 Gradle |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | 8020 | 8097 |
| 本機資料庫 | `test`（與其他模組共用） | `Swagger` |
| 資料表 | Hibernate `ddl-auto: update` | Flyway |
| OpenAPI 路徑 | `/api-docs` | `/v3/api-docs`（springdoc 預設） |
| 回應格式 | `ApiResponse` 包裝（`code`、`message`、`data`） | 直接回傳資源 |
| 新增 | 讀不到 body：存入空資料 200 → 升級後 400 | 201 + `Location` |
| 刪除 | 200 + `ApiResponse` | 204 |
| 錯誤 | `ApiResponse`（404 沒有內容），其他都是 500 | ProblemDetail：400（含 `errors`）、404、409 |
| prod profile | 無 | 關閉文件與 Swagger UI |

### 升級時學到的事

- **升級依賴可能間接改變行為**：springdoc 2.8 會間接帶入 `spring-boot-starter-validation`，升級後驗證突然生效，baseline 測試因此失敗。程式直接用到的功能（這裡是 Bean Validation），要自己明確宣告依賴，不要依賴其他套件「剛好」帶進來。
- **同名的註解很容易 import 錯**：Spring 與 Swagger 都有 `@RequestBody`。看到「JSON body 讀不到」時，第一件事是檢查 import。
- **推論要用測試確認**：分析時推測「POST 會回傳 400」，baseline 測試實際跑出來是 200 並存入空資料，原因是根本沒有驗證實作。
- **springdoc 3 的 components 會被重建**：在 `OpenAPI` bean 中加入的 schema 不見了，要改在 `OpenApiCustomizer` 中加入。
