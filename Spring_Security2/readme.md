# Spring Security 2 - 自訂設定：誰可以進哪裡？

[Spring_Security](../Spring_Security) 只用自動設定；這個模組自己寫 Security 設定：

- **路徑規則**：哪些路徑不需要登入、哪些需要登入、哪些需要 ADMIN
- **方法層級的權限**：`@PreAuthorize`
- **使用者存在資料庫**：Spring Security 內建的 `users`、`authorities` 表，密碼存 BCrypt 雜湊值

登入方式仍然是表單登入與 HTTP Basic（使用 Session）。不用 Session 的 Token 驗證在 [Spring_Security3](../Spring_Security3)。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Security | 7.0 |
| 資料庫 | PostgreSQL（資料庫名稱 `Security2`），Flyway 建立 `users`、`authorities` |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、MockMvc、spring-security-test、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_AuthorizationRulesTest](src/test/java/com/example/demo/lesson/L01_AuthorizationRulesTest.java) | 路徑規則；401 vs 403 |
| 2 | [L02_PermitAllVsIgnoringTest](src/test/java/com/example/demo/lesson/L02_PermitAllVsIgnoringTest.java) | `permitAll()` 和 `web.ignoring()` 的差別 |
| 3 | [L03_MethodSecurityTest](src/test/java/com/example/demo/lesson/L03_MethodSecurityTest.java) | `@PreAuthorize`：保護方法，不只保護網址 |
| 4 | [L04_DatabaseUsersTest](src/test/java/com/example/demo/lesson/L04_DatabaseUsersTest.java) | 使用者存在資料庫：BCrypt、停用帳號、`ROLE_` 前綴 |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/SecurityConfig        路徑規則、JdbcUserDetailsManager、PasswordEncoder、@EnableMethodSecurity
├── controller/DemoController    /api/public/hello、/api/me、/api/admin/stats、/api/reports/sales
└── report/ReportService         @PreAuthorize("hasRole('ADMIN')")

src/main/resources/db/migration/V1__users.sql   users、authorities 表與 3 個範例帳號
```

## 範例帳號

密碼都是 `password`（資料庫只存 BCrypt 雜湊值）：

| 帳號 | 角色 | 說明 |
|---|---|---|
| `alice` | USER | 一般使用者 |
| `admin` | USER、ADMIN | 管理者 |
| `bob` | USER | **停用**（`enabled = false`），密碼正確也不能登入 |

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers），不會連到本機的資料庫。

```bash
./gradlew test
```

### Docker Compose

```bash
docker compose up --build -d
# Windows 保留了 8106 時：APP_PORT=8306 docker compose up --build -d

curl http://localhost:8106/api/public/hello                       # Hello, guest!
curl -u alice:password http://localhost:8106/api/me
curl -u alice:password http://localhost:8106/api/admin/stats      # 403
curl -u admin:password http://localhost:8106/api/reports/sales
docker compose down -v
```

### 本機執行

需要本機的 PostgreSQL 有 `Security2` 資料庫，表由 Flyway 自動建立。

```bash
./gradlew bootRun
```

## API

| 路徑 | 權限 | 規則寫在 |
|---|---|---|
| `GET /api/public/hello` | 不需要登入 | `SecurityConfig`：`permitAll()` |
| `GET /api/me` | 需要登入 | `SecurityConfig`：`anyRequest().authenticated()` |
| `GET /api/admin/stats` | ADMIN | `SecurityConfig`：`hasRole("ADMIN")` |
| `GET /api/reports/sales` | 需要登入，而且 ADMIN | `ReportService`：`@PreAuthorize` |

## 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的服務實際執行過。

**1. 建立 Environment**：變數 `baseUrl` = `http://localhost:8106`。

**2. 不需要登入的路徑**：`GET {{baseUrl}}/api/public/hello`，**Authorization** 分頁選 **No Auth** → `Hello, guest!`

**3. 用 Basic Auth 登入**：**Authorization** 分頁選 **Basic Auth**，Username `alice`、Password `password`。

| 請求 | 帳號 | 結果 |
|---|---|---|
| `GET {{baseUrl}}/api/me` | alice | 200，`username`、`authorities` |
| `GET {{baseUrl}}/api/me` | alice，密碼打錯 | 401 |
| `GET {{baseUrl}}/api/admin/stats` | alice | **403**（沒有 ADMIN） |
| `GET {{baseUrl}}/api/admin/stats` | admin | 200 |
| `GET {{baseUrl}}/api/reports/sales` | admin | 200（`@PreAuthorize`） |
| `GET {{baseUrl}}/api/me` | bob | 401（停用的帳號） |

**小技巧**：在 **Collection** 的 Authorization 設定 Basic Auth，每個請求選 **Inherit auth from parent**。要換帳號時只改一個地方。

**會不會因為 cookie 而「一直保持登入」？** 不會。Spring Security 6 起，HTTP Basic 預設不建立 Session，回應沒有 `Set-Cookie`，所以把密碼改錯就會立刻得到 401（實測）。表單登入（瀏覽器的登入頁）才會建立 Session。

## Security 教學

### 1. 路徑規則（第 1 課）

```java
http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/public/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
    .formLogin(Customizer.withDefaults())
    .httpBasic(Customizer.withDefaults());
```

- **由上往下比對，第一個符合的就決定結果**：範圍小的規則寫在前面，`anyRequest()` 一定放最後。
- **`anyRequest().authenticated()`**：沒有列出的路徑也需要登入，忘了設定也不會意外開放。
- **自訂 `SecurityFilterChain` 之後，Spring Boot 的預設就不再套用**：登入方式（`formLogin`、`httpBasic`）要自己加回來。

| 狀態碼 | 意思 | 例子 |
|---|---|---|
| 401 | 不知道你是誰 | 沒有登入就呼叫 `/api/me` |
| 403 | 知道你是誰，但你沒有權限 | `alice` 呼叫 `/api/admin/stats` |

### 2. `permitAll()` vs `web.ignoring()`（第 2 課）

兩者都能讓路徑「不需要登入」，但差很多：

| | `permitAll()` | `web.ignoring()` |
|---|---|---|
| Security 的 filter | 仍然執行 | 整個跳過 |
| 有登入時，知道是誰 | 知道 | **不知道** |
| 安全性標頭（`X-Content-Type-Options` 等） | 有 | **沒有** |
| Spring Security 的建議 | 建議使用 | 啟動時印出警告；只適合完全不需要保護的靜態檔案 |

修正前的 Spring_Security2 想用 `web.ignoring()` 開放 `/ignore1`，但根本沒有生效（見下方「修正前發現的問題」）。

### 3. 方法層級的權限（第 3 課）

```java
@PreAuthorize("hasRole('ADMIN')")
public String salesReport() { ... }
```

路徑規則保護的是「網址」；`@PreAuthorize` 保護的是「方法」。之後就算有別的 Controller 或排程呼叫這個方法，也一樣會檢查。需要在設定類別加上 `@EnableMethodSecurity`。

### 4. 使用者存在資料庫（第 4 課）

使用 Spring Security **內建**的實作，不必自己寫：

| 元件 | 做什麼 |
|---|---|
| `users` 表 | 帳號、密碼（BCrypt 雜湊值）、是否啟用 |
| `authorities` 表 | 帳號擁有的角色（`ROLE_USER`、`ROLE_ADMIN`） |
| `JdbcUserDetailsManager` | 登入時用帳號查這兩張表；也能新增、刪除使用者 |
| `PasswordEncoder` | 比對輸入的密碼和資料庫的雜湊值；新增使用者時算出雜湊值 |

**`ROLE_` 前綴**：資料庫存 `ROLE_ADMIN`，程式寫 `hasRole("ADMIN")`，Spring Security 會自動加上 `ROLE_`。直接寫 `hasRole("ROLE_ADMIN")` 反而會出錯。

**`FACTOR_PASSWORD`**：`/api/me` 會看到一個資料庫裡沒有的權限。這是 Spring Security 7 在登入成功後自動加上的，表示「這次是用密碼登入的」，是為多因素驗證設計的（例如要求某些操作必須「密碼 + 一次性驗證碼」都通過）。本專案不使用。

**為什麼用內建的表，不自己設計**：自己設計 `member` 表並實作 `UserDetailsService` 也很常見，但內建的表已經涵蓋「帳號、密碼、啟用、角色」，練習時先理解它，之後自訂時才知道要提供哪些資訊。

## 面試時能說明的重點

**Q：`SecurityFilterChain` 的規則怎麼比對？**
由上往下，第一個符合的規則決定結果，所以範圍小的寫前面，`anyRequest()` 放最後。

**Q：`permitAll()` 和 `web.ignoring()` 差在哪？**
`permitAll()` 仍然經過 Security 的 filter，只是不要求登入；`web.ignoring()` 完全跳過 Security，連安全性標頭都沒有，所以 Spring Security 建議用 `permitAll()`。

**Q：路徑規則和 `@PreAuthorize` 有什麼不同？**
路徑規則保護網址；`@PreAuthorize` 保護方法，不論從哪裡呼叫都會檢查。

**Q：使用者存在資料庫時，登入的流程是什麼？**
`UserDetailsService`（這裡是 `JdbcUserDetailsManager`）用帳號查出使用者、密碼雜湊值與角色，`PasswordEncoder` 把輸入的密碼再算一次並比對。資料庫只存雜湊值。

**Q：`hasRole("ADMIN")` 和 `hasAuthority("ROLE_ADMIN")` 一樣嗎？**
一樣。`hasRole` 會自動加上 `ROLE_` 前綴。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試：確認設定類別沒有被載入 |
| 2 | Java 17 → 21、Spring Boot 3.0.3 → 3.5.16、Gradle 7.6 → 8.14.3 |
| 3 | Spring Boot 3.5.16 → 4.1.1、Gradle 8.14.3 → 9.8.0 |
| 4 | 重構：`SecurityConfig`、使用者存在資料庫、`@PreAuthorize`；第 1～4 課 |
| 5 | Docker |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| **[Potential Bug] 設定類別沒有被載入** | `DemoWebSecurityConfig` 只標了 `@EnableWebSecurity`。Spring Security 6 起它不再包含 `@Configuration`，component scan 不會載入這個類別：`WebSecurityCustomizer` 不存在，`/ignore1` 仍然需要登入（baseline 測試確認） |
| 每次啟動產生隨機密碼 | 沒有設定任何使用者，Spring Boot 產生帳號 `user` 與隨機密碼 |
| 用 `web.ignoring()` 開放路徑 | 見「Security 教學」第 2 點 |
| 其他 | 舊版 `WebSecurityConfigurerAdapter` 的寫法留在註解中；簡體中文註解；沒有使用的 Lombok、devtools；`contextLoads` 測試 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | 8080 | 8106 |
| 使用者 | Boot 產生的 `user` + 隨機密碼 | 資料庫：`alice`、`admin`、`bob` |
| API | `GET /`、`GET /ignore1`（都需要登入） | 見「API」 |

### 升級時學到的事

- **`@EnableWebSecurity` 不再包含 `@Configuration`**（Spring Security 6）：自訂設定的類別要標 `@Configuration`。Spring Boot 已經自動啟用 Web Security，通常不需要再寫 `@EnableWebSecurity`。
- **Spring Security 7 的 `FACTOR_PASSWORD`**：見「Security 教學」第 4 點。
- **官方的 `users.ddl` 是 HSQLDB 語法**：`varchar_ignorecase` 在 PostgreSQL 不能用，migration 要自己改寫。
