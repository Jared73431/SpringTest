# Spring Security 3 - 無狀態的 API：JWT

[Spring_Security](../Spring_Security)、[Spring_Security2](../Spring_Security2) 用 Session 記住「已經登入」。這個模組不用 Session：

1. 用帳密換一個 **JWT**（JSON Web Token）
2. 之後每個請求都帶 `Authorization: Bearer <JWT>`，伺服器用**公鑰**檢查簽章，就知道是誰、有什麼權限

驗證 JWT 使用 Spring Security 內建的 **OAuth2 Resource Server**，不自己寫 filter。之後改用 Keycloak 簽發 Token 時，驗證這一端幾乎不用改（見「[之後：改用 Keycloak](#之後改用-keycloak)」）。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-oauth2-resource-server`） |
| Spring Security | 7.0 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、MockMvc、spring-security-test |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_IssueTokenTest](src/test/java/com/example/demo/lesson/L01_IssueTokenTest.java) | 用帳密換 JWT；JWT 的三段；**內容任何人都看得到** |
| 2 | [L02_VerifyTokenTest](src/test/java/com/example/demo/lesson/L02_VerifyTokenTest.java) | 伺服器怎麼確認 Token 是真的：竄改、過期、別的金鑰簽的都會被拒絕 |
| 3 | [L03_StatelessApiTest](src/test/java/com/example/demo/lesson/L03_StatelessApiTest.java) | 不建立 Session、不需要 CSRF token、scope 決定權限 |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/
│   ├── SecurityConfig        兩條 SecurityFilterChain、範例帳號
│   └── JwtKeyConfig          RSA 金鑰對、JwtEncoder（私鑰簽章）、JwtDecoder（公鑰驗證）
├── token/
│   ├── TokenController       POST /api/auth/token
│   └── TokenService          簽發 JWT（iss、sub、iat、exp、scope）
└── controller/DemoController /api/me、/api/admin/stats、/api/messages
```

## 範例帳號

密碼都是 `password`（存在記憶體中，讓這個模組專注在 Token；使用者存在資料庫見 Spring_Security2）：

| 帳號 | Token 的 scope |
|---|---|
| `alice` | `USER` |
| `admin` | `ADMIN USER` |

## 執行方式

### 測試（主要的學習方式）

不需要資料庫，也不需要 Docker。

```bash
./gradlew test
```

### 本機執行 / Docker

```bash
./gradlew bootRun
# 或
docker build -t spring-security3 .
docker run --rm -p 8107:8107 spring-security3     # Windows 保留了 8107 時：-p 8307:8107
```

```bash
# 1. 用帳密換 Token
curl -X POST -u alice:password http://localhost:8107/api/auth/token
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":900}

# 2. 帶 Token 呼叫 API
TOKEN=eyJ...
curl -H "Authorization: Bearer $TOKEN" http://localhost:8107/api/me
curl -H "Authorization: Bearer $TOKEN" http://localhost:8107/api/admin/stats      # 403：alice 沒有 ADMIN
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: text/plain" \
     -d hi http://localhost:8107/api/messages                                      # 不需要 CSRF token
```

把 Token 貼到 https://jwt.io 可以直接看到內容。

## API

| 方法 | 路徑 | 需要 |
|---|---|---|
| POST | `/api/auth/token` | HTTP Basic（帳密） |
| GET | `/api/me` | Bearer Token |
| GET | `/api/admin/stats` | Bearer Token，scope 有 `ADMIN` |
| POST | `/api/messages` | Bearer Token |

## 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的服務實際執行過。重點是**讓 Postman 自動保存 Token**，不必每次手動複製。

**1. 建立 Environment**：變數 `baseUrl` = `http://localhost:8107`。

**2. Collection 統一使用 Bearer Token**：點 Collection → **Authorization** 分頁 → Type 選 **Bearer Token**，Token 填 `{{token}}`。之後新增的請求保持 **Inherit auth from parent**。

**3. 取得 Token 的請求**：`POST {{baseUrl}}/api/auth/token`

- **Authorization** 分頁：選 **Basic Auth**，Username `alice`、Password `password`（只有這個請求用帳密，不繼承 Collection 的 Bearer）
- **Scripts** 分頁 → **Post-response**，把回應中的 Token 存進 collection 變數：

  ```javascript
  pm.collectionVariables.set("token", pm.response.json().accessToken);
  ```

送出後，Collection 的 **Variables** 分頁就會看到 `token` 有值。

**4. 其他請求**（都繼承 Collection 的 Bearer Token）：

| 請求 | 結果 |
|---|---|
| `GET {{baseUrl}}/api/me` | 200，`username`、`scope`、`expiresAt` |
| `GET {{baseUrl}}/api/admin/stats` | **403**（alice 的 scope 只有 USER） |
| `POST {{baseUrl}}/api/messages`，Body 選 **raw** / **Text**，內容 `hi` | 200，`alice says: hi`（不需要 CSRF token） |
| `GET {{baseUrl}}/api/me`，Authorization 改成 **No Auth** | 401 |
| `GET {{baseUrl}}/api/me`，Authorization 改成 **Basic Auth**（alice） | 401：API 只接受 Token |

**5. 換成 admin**：複製取得 Token 的請求，帳號改成 `admin`，script 改存到另一個變數：

```javascript
pm.collectionVariables.set("adminToken", pm.response.json().accessToken);
```

`GET {{baseUrl}}/api/admin/stats` 的 Authorization 改成 **Bearer Token** `{{adminToken}}` → 200。

**6. 觀察 Token 被拒絕**：

- 在 Token 後面多加一個字元（例如 `{{token}}x`）→ 401，回應標頭 `WWW-Authenticate` 有 `invalid_token`
- 15 分鐘後再呼叫 → 401（過期）；重新送一次取得 Token 的請求即可
- 重新啟動應用程式後 → 401：金鑰在啟動時產生，舊的 Token 全部失效

把 `token` 變數的值貼到 https://jwt.io 可以看到 Token 的內容（第 1 課）。

## Security 教學

### 1. JWT 是什麼（第 1 課）

```text
eyJhbGciOiJSUzI1NiJ9 . eyJzdWIiOiJhbGljZSIsInNjb3BlIjoiVVNFUiIsLi4ufQ . kq3Zr...
      header                              payload                          signature
```

| 段 | 內容 |
|---|---|
| header | `{"alg":"RS256"}`：用什麼演算法簽章 |
| payload | Token 的內容（claims）：`iss` 誰簽發的、`sub` 代表誰、`iat` 簽發時間、`exp` 到期時間、`scope` 權限 |
| signature | 用**私鑰**對前兩段簽章的結果 |

**前兩段只是 Base64URL 編碼，不是加密，任何人都能解開**：JWT 防的是「被竄改」，不是「被看到」。所以不要在 Token 中放密碼、身分證字號這類敏感資料。

### 2. 伺服器怎麼確認 Token 是真的（第 2 課）

每個請求都用**公鑰**檢查：

| 情況 | 結果 |
|---|---|
| 正常的 Token | 200 |
| 沒有帶 Token | 401，`WWW-Authenticate: Bearer` |
| 內容被改過（例如把 scope 改成 ADMIN） | 401 `invalid_token`：簽章對不上 |
| 超過 `exp` | 401 |
| 用別的私鑰簽的（攻擊者自己簽一個 ADMIN 的 Token） | 401：伺服器的公鑰驗不過 |

**為什麼用 RSA 金鑰對**：

| | 私鑰 | 公鑰 |
|---|---|---|
| 用途 | 簽發 Token | 驗證 Token |
| 誰有 | 只有簽發 Token 的服務 | 任何要驗證 Token 的服務，可以公開 |

多個服務都要驗證 Token 時，只要給它們公鑰，它們能驗證但不能簽發。另一種做法是 HMAC（共用一把密鑰），比較簡單，但每個能驗證的服務也都能簽發。

**金鑰在啟動時產生**：這樣不必把私鑰放進 repo（就算是練習用的也不放）。代價是重新啟動後舊的 Token 全部失效，多台主機也不能共用。正式環境的私鑰放在密鑰管理服務，或交給 Keycloak 這類授權伺服器。

### 3. 無狀態（第 3 課）

| | Session（Security、Security2） | JWT（本模組） |
|---|---|---|
| 伺服器記住什麼 | 誰登入了（存在 Session） | 什麼都不記，每個請求自己帶 Token |
| 多台主機 | 要共用 Session（例如存在 Redis） | 不需要，任何一台有公鑰就能驗證 |
| 登出 / 撤銷 | 刪除 Session 就失效 | **Token 到期前一直有效**，所以有效時間要短（本模組 15 分鐘） |
| CSRF | 需要防護（瀏覽器自動帶 cookie） | 不需要（Token 要由程式明確放進標頭） |

**兩條 SecurityFilterChain**：一個請求只會走第一條符合的 chain。

```text
@Order(1) /api/auth/token   HTTP Basic：用帳密換 Token
@Order(2) 其他所有路徑       只接受 Bearer Token（不接受帳密）
```

**權限來自 Token 的 scope**：scope 是 `"ADMIN USER"` 時，Spring 自動轉成 `SCOPE_ADMIN`、`SCOPE_USER` 兩個權限，所以規則寫 `hasAuthority("SCOPE_ADMIN")`。

### 修正前：若依樣板留下的設定

修正前的設定看起來是從若依（RuoYi）這類 token 樣板改來的，但只留下一半：

| 設定 | 樣板中的用途 | 這個專案中 |
|---|---|---|
| JWT filter、登入 API | 驗證 Token | 都是註解：**沒有任何登入方式，需要登入的路徑任何人都進不去**（baseline 測試確認一律 403） |
| `/login`、`/captchaImage` 只允許匿名 | 登入頁、驗證碼 | 都不存在 |
| `*.html`、`*.css`、`*.js`、`/profile/**` 開放 | 前端檔案、頭像 | 都不存在 |
| `frameOptions().disable()` | 後台用 iframe | 讓頁面可以被其他網站嵌入（clickjacking） |

若依自己寫了一個 `JwtAuthenticationTokenFilter` 解析 Token；本模組改用 Spring Security 內建的 Resource Server，不需要自己寫 filter。

### 之後：改用 Keycloak

本模組自己簽發 Token（`TokenController`、`JwtEncoder`、私鑰）。改用 Keycloak 這類授權伺服器時：

| 元件 | 改用 Keycloak 後 |
|---|---|
| `TokenController`、`TokenService`、私鑰、使用者 | 移除：登入與簽發 Token 交給 Keycloak |
| `JwtDecoder` | 改成 `spring.security.oauth2.resourceserver.jwt.issuer-uri=<Keycloak 網址>`，公鑰自動從 Keycloak 取得 |
| `apiChain`（`oauth2ResourceServer`） | **不變** |
| 權限 | Keycloak 的角色放在別的 claim，需要一個轉換設定 |

## 面試時能說明的重點

**Q：JWT 是什麼？裡面的內容安全嗎？**
三段：header、payload、signature。前兩段只是 Base64URL 編碼，任何人都能讀，所以不能放敏感資料；簽章保證內容沒有被竄改。

**Q：伺服器怎麼驗證 JWT？**
用公鑰檢查簽章，再檢查到期時間。不需要查資料庫，也不需要 Session。

**Q：為什麼用 RSA（非對稱）而不用 HMAC（對稱）？**
RSA 用私鑰簽發、公鑰驗證，驗證 Token 的服務拿不到私鑰，不能自己簽發；HMAC 共用一把密鑰，能驗證就能簽發。

**Q：JWT 和 Session 的差別？JWT 的缺點是什麼？**
JWT 無狀態，伺服器不記住登入者，容易擴充到多台主機；缺點是發出去之後到期前無法撤銷，所以有效時間要短。

**Q：為什麼 JWT 的 API 可以關掉 CSRF？**
CSRF 利用瀏覽器自動帶 cookie；Bearer Token 要由程式明確放進 `Authorization` 標頭，惡意網站無法讓瀏覽器自動帶上。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試：一律 403、樣板留下的開放路徑、沒有 X-Frame-Options |
| 2 | Gradle 7.6.1 → 8.14.3、Java 17 → 21（Spring Boot 2.7.9 → 2.7.18，設定不變） |
| 3 | Spring Boot 2.7.18 → 3.5.16：`WebSecurityConfigurerAdapter` → `SecurityFilterChain`（行為不變） |
| 4 | Spring Boot 3.5.16 → 4.1.1、Gradle → 9.8.0 |
| 5 | 重構：JWT（Resource Server）；第 1～3 課 |
| 6 | Docker |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 登入方式 | 沒有（任何人都進不去） | 帳密換 JWT，之後用 Bearer Token |
| 開放的路徑 | `/login`（匿名）、`*.html` 等、`/profile/**` | 只有 `/api/auth/token`（需要帳密） |
| X-Frame-Options | 關閉 | 預設（`DENY`） |
| Port | 8080 | 8107 |

### 升級時學到的事

- **`WebSecurityConfigurerAdapter` 在 Spring Security 6 被移除**：

  | Spring Security 5 | Spring Security 6 以後 |
  |---|---|
  | `extends WebSecurityConfigurerAdapter`，覆寫 `configure(HttpSecurity)` | 宣告 `@Bean SecurityFilterChain` |
  | `.authorizeRequests().antMatchers(...)` | `.authorizeHttpRequests(auth -> auth.requestMatchers(...))` |
  | `.csrf().disable().and()...` | `.csrf(csrf -> csrf.disable())`，每一項各自一個 lambda，沒有 `.and()` |

- **先升級 Build 和 Java，再升級 Spring Boot**：Boot 2.7.18 可以在 Gradle 8.14.3、Java 21 上執行，所以先在不改程式的情況下完成這一步。
- **`/**/*.html` 這種路徑在 Spring Security 6.5、7 仍然可以用**：原本以為新的路徑比對規則不允許 `**` 後面還有內容，實測沒有問題。
- **Spring Security 7 的 `FACTOR_PASSWORD`**：登入後會多一個代表「用密碼登入」的權限（見 Spring_Security2）。簽發 Token 時要過濾掉，否則實測它會被寫進 scope。
- **`WWW-Authenticate` 的 `resource_metadata`**：Spring Security 7 在 401 的 `WWW-Authenticate: Bearer` 後面附上一個網址，說明這個 API 接受哪裡簽發的 Token（OAuth2 的標準 RFC 9728），本專案不使用。
