# Spring Security - 自動設定：什麼都不寫，會發生什麼事？

只加上 `spring-boot-starter-security`，**不寫任何 Security 設定**，觀察 Spring Boot 預設做了什麼：

- 所有路徑都需要登入
- 自動產生登入頁（`/login`），支援表單登入與 HTTP Basic
- 會改變資料的請求需要 CSRF token
- 帳密來自設定檔（`spring.security.user.*`）

Security 系列由淺到深：

| 模組 | 主題 |
|---|---|
| **Spring_Security**（本模組） | 自動設定：預設的保護、CSRF、密碼怎麼存 |
| [Spring_Security2](../Spring_Security2) | 自訂設定：哪些路徑開放、角色權限、使用者存在資料庫 |
| [Spring_Security3](../Spring_Security3) | 無狀態的 API：JWT |

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Security | 7.0（Spring Boot 管理） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、MockMvc、spring-security-test |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_DefaultSecurityTest](src/test/java/com/example/demo/lesson/L01_DefaultSecurityTest.java) | 瀏覽器導向登入頁 vs 程式收到 401；表單登入與 Session；HTTP Basic |
| 2 | [L02_CsrfTest](src/test/java/com/example/demo/lesson/L02_CsrfTest.java) | CSRF：為什麼已經登入了，POST 還是被拒絕 |
| 3 | [L03_PasswordStorageTest](src/test/java/com/example/demo/lesson/L03_PasswordStorageTest.java) | 設定檔的密碼是明碼；BCrypt 雜湊與 salt |

## 專案結構

```text
src/main/java/com/example/demo/
├── SpringSecurityApplication
└── controller/DemoController     GET /api/hello、POST /api/messages（沒有任何 Security 設定）

src/main/resources/application.properties   帳號 user，密碼來自環境變數 SECURITY_USER_PASSWORD
```

## 執行方式

### 測試（主要的學習方式）

不需要資料庫，也不需要 Docker。

```bash
./gradlew test
```

### 本機執行

```bash
./gradlew bootRun                                    # 密碼預設是 password
curl -u user:password http://localhost:8105/api/hello
```

瀏覽器開啟 http://localhost:8105/api/hello 會被導向登入頁。

### Docker

```bash
docker build -t spring-security .
docker run --rm -p 8105:8105 -e SECURITY_USER_PASSWORD=secret spring-security
# Windows 保留了 8105 時改用其他主機 port：-p 8305:8105
curl -u user:secret http://localhost:8105/api/hello
```

## Security 教學

### 1. 預設的保護（第 1 課）

| 請求 | 沒有登入時 |
|---|---|
| 瀏覽器（`Accept: text/html`） | 302 導向 `/login` |
| 程式（其他 Accept） | 401，`WWW-Authenticate: Basic` |

兩種登入方式：

| | 表單登入 | HTTP Basic |
|---|---|---|
| 怎麼登入 | 在登入頁輸入一次帳密 | **每個請求**都在 `Authorization` 標頭帶帳密 |
| 怎麼記住已經登入 | Session（瀏覽器的 `JSESSIONID` cookie） | 不需要記住，每次都重新驗證 |
| 適合 | 瀏覽器 | 程式呼叫、測試（curl） |

HTTP Basic 的帳密只是 **Base64 編碼，不是加密**，任何人攔截到都能還原，所以一定要搭配 HTTPS。

### 2. CSRF（第 2 課）

**CSRF（跨站請求偽造）**：你登入了銀行網站，又開了一個惡意網站。惡意網站偷偷送出一個「轉帳」表單到銀行，瀏覽器會**自動帶上**銀行的 cookie，銀行就以為是你本人送出的。

**防禦**：會改變資料的請求（POST、PUT、DELETE）必須帶一個 CSRF token。token 只有銀行自己的頁面拿得到，惡意網站拿不到，所以偽造的請求會被拒絕。GET 不改變資料，不需要 token。

**[Learning] 用 curl 測試 POST 收到 401 時，先檢查是不是少了 CSRF token**：

帳密正確、沒有 CSRF token 的 POST，在測試（MockMvc）中是 403，在真正的伺服器上卻是 **401**（實測）。原因：

1. CSRF 檢查在 HTTP Basic 登入**之前**，被拒絕時還沒有登入。
2. 403 被轉到錯誤頁 `/error`，而 `/error` 也需要登入；轉過去時不會再做一次 Basic 登入，於是變成 401。

真正的原因（CSRF）被 401 蓋掉了，很容易誤以為是帳密錯誤。

什麼時候可以關掉 CSRF，見 [Spring_Security3](../Spring_Security3)（不用 cookie 的 API）。

### 3. 密碼怎麼存（第 3 課）

設定檔的 `spring.security.user.password` 是**明碼**：Spring Boot 在前面加上 `{noop}`（不編碼）存在記憶體中。這只適合練習。

正式系統要存**雜湊值（hash）**：

| | 加密（encryption） | 雜湊（hash） |
|---|---|---|
| 能不能還原 | 可以，有金鑰就能解密 | **不能** |
| 登入時怎麼比對 | 解密後比對 | 把輸入的密碼再算一次，比對結果是否相同 |
| 用在密碼 | 不適合：金鑰外洩，所有密碼都外洩 | 適合 |

為什麼用 **BCrypt**：

- **刻意算得很慢**：正常登入時感覺不出來，但攻擊者想一個一個猜密碼，就會非常慢。
- **自動加上隨機的 salt**：同一個密碼每次算出不同的雜湊值。兩個使用者用同一個密碼，在資料庫裡看起來也不一樣，攻擊者不能用事先算好的密碼表一次破解。

`{bcrypt}`、`{noop}` 這些前綴告訴 Spring Security 密碼是用什麼方式存的，日後要換演算法時，舊密碼仍然能驗證。

Spring_Security2 的使用者存在資料庫，密碼就是 BCrypt。

## 面試時能說明的重點

**Q：加上 spring-boot-starter-security 之後，會發生什麼事？**
所有路徑都需要登入，並且自動產生登入頁，支援表單登入和 HTTP Basic，POST 等請求需要 CSRF token。沒有設定帳密時，帳號是 `user`，密碼在每次啟動時隨機產生並印在 log。

**Q：401 和 403 有什麼不同？**
401 是「不知道你是誰」（沒有登入或帳密錯誤）；403 是「知道你是誰，但你沒有權限」（或請求被拒絕，例如少了 CSRF token）。

**Q：什麼是 CSRF？Spring Security 怎麼防？**
惡意網站利用瀏覽器會自動帶 cookie 的特性，冒用使用者送出請求。Spring Security 要求會改變資料的請求帶一個惡意網站拿不到的 token。

**Q：密碼為什麼不能加密存，而要雜湊？為什麼用 BCrypt？**
加密可以還原，金鑰外洩所有密碼都外洩；雜湊不能還原。BCrypt 刻意算得慢，而且自動加 salt，讓暴力破解和事先算好的密碼表都很難成功。

**Q：HTTP Basic 安全嗎？**
帳密只是 Base64 編碼，不是加密，必須搭配 HTTPS。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試：登入頁、表單登入、HTTP Basic、401 |
| 2 | Java 17 → 21、Spring Boot 3.0.2 → 3.5.16、Gradle 7.6 → 8.14.3 |
| 3 | Spring Boot 3.5.16 → 4.1.1、Gradle 8.14.3 → 9.8.0 |
| 4 | 重構：port、帳號、`/api` 路徑；第 1～3 課 |
| 5 | Docker |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port、context path | 8080、`/demo` | 8105、無 |
| 帳號 | `Jared` | `user`（密碼仍然來自 `SECURITY_USER_PASSWORD`） |
| API | `GET /` 回傳固定文字 | `GET /api/hello` 回傳登入者名稱、`POST /api/messages` |
| 導向登入頁的 Location | `http://localhost/login` | `/login`（Spring Security 7 的變更，見下方） |

### 升級時學到的事

- **Gradle 7.6 不能在 Java 21 上執行**：build script 一修改就要重新編譯，出現 `Unsupported class file major version 65`。先直接把 `gradle-wrapper.properties` 的網址改成 8.14.3，再用新版 Gradle 重新產生 wrapper。
- **Spring Security 7 導向登入頁改用相對路徑**：`Location` 從 `http://localhost/login` 變成 `/login`。放在 reverse proxy 後面時，不會因為內部的主機名稱或 http / https 而導向錯誤的網址。
- **Boot 4 的測試依賴**：`spring-boot-starter-webmvc-test`、`spring-boot-starter-security-test`；`@AutoConfigureMockMvc` 移到 `org.springframework.boot.webmvc.test.autoconfigure`。
- **`.gitattributes`**：沒有它時，`core.autocrlf=true` 會把 `gradlew` 轉成 CRLF，在 Linux 的 Docker build 中無法執行。
