# Spring Boot Hello World

最小的 Spring Boot REST API 範例：一個 Controller、一個 GET 端點。

這是本 Repo 的第一個練習專案，也是第一個完成現代化（Spring Boot 2.7 → 4.1）的模組，升級過程記錄在下方的「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Gradle | 9.8.0（使用 Gradle Wrapper，不需另外安裝） |
| 測試 | JUnit 5、MockMvc（`@WebMvcTest`） |

## 專案結構

```text
src/
├── main/java/com/example/demo/
│   ├── SpringHelloWorldApplication.java   # 啟動類別
│   └── controller/
│       └── HelloController.java           # REST API
└── test/java/com/example/demo/
    ├── SpringHelloWorldApplicationTests.java  # 確認 Spring Context 可正常啟動
    └── controller/
        └── HelloControllerTest.java           # API 行為測試
```

## 執行方式

需要 JDK 21。

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

或先打包再執行：

```bash
./gradlew bootJar
java -jar build/libs/Spring_HelloWorld-0.0.1-SNAPSHOT.jar
```

## API

### `GET /api/hello`

```bash
curl http://localhost:8080/api/hello
```

| 項目 | 內容 |
|---|---|
| Status | `200 OK` |
| Content-Type | `text/plain;charset=UTF-8` |
| Body | `Hello World` |

其他情況：

| 請求 | 結果 |
|---|---|
| `POST /api/hello` | `405 Method Not Allowed` |
| `GET /api/hello/`（結尾多一個斜線） | `404 Not Found` |

## 測試

```bash
./gradlew test
```

`HelloControllerTest` 使用 `@WebMvcTest`，只載入 Web 層（不啟動完整應用程式），速度快且專注於 Controller 行為：

- `hello_shouldReturnHelloWorld_whenGetRequest`
- `hello_shouldReturnNotFound_whenPathHasTrailingSlash`
- `hello_shouldReturnMethodNotAllowed_whenPostRequest`
- `hello_shouldReturnNotFound_whenUsingLegacyPathWithoutApiPrefix`

## 程式碼說明

### HelloController

| 寫法 | 說明 |
|---|---|
| `@RestController` | 方法回傳值直接作為 HTTP Response Body |
| `@RequestMapping("/api")` | Class 層級的共同路徑前綴 |
| `@GetMapping("/hello")` | 對應 `GET /api/hello` |
| `LoggerFactory.getLogger(...)` | 使用 SLF4J 記錄 log（Spring Boot 內建，不需額外依賴） |

`HelloController` 裡另有一個 `main()` 方法，是當年練習 Java 語法（`ArrayList`、Lambda、`forEach`）留下的草稿，與 API 無關，Spring 啟動時也不會執行。目前刻意保留作為學習紀錄。

## 現代化紀錄

| 步驟 | 內容 | 驗證方式 |
|---|---|---|
| 1 | 先補上 MockMvc 測試，固定原本的 API 行為 | Boot 2.7 上測試通過 |
| 2 | Gradle 7.5 → 8.14.3 | 測試通過 |
| 3 | Java 11 → 21、Spring Boot 2.7.4 → 3.5.16 | 測試通過（結尾斜線行為依預期改變） |
| 4 | Spring Boot 3.5.16 → 4.1.1、Gradle → 9.8.0 | 測試通過，並實際啟動比對回應 |
| 5 | `System.out` 改為 SLF4J Logger | 測試通過 |
| 6 | API 路徑改為 `/api/hello`（專案統一 URL 規則） | 測試通過 |

### 行為變更

| 項目 | 升級前 | 升級後 | 原因 |
|---|---|---|---|
| API 路徑 | `GET /hello` | `GET /api/hello` | 專案統一 URL 規則：API 皆加上 `/api` 前綴 |
| 結尾斜線 | `GET /hello/` 回 200 | `GET /api/hello/` 回 404 | Spring Framework 6 起預設不再比對結尾斜線 |
| Console 輸出 | `System.out.println("成功")` | `log.info("hello API 呼叫成功")` | Logger 可控制層級，並自動附上時間、Thread、Class |

### 升級時學到的事

- **Gradle Wrapper 要執行兩次**：第一次由舊版 Gradle 執行，只會更新版本設定；第二次由新版執行，才會更新 `gradle-wrapper.jar` 與 `gradlew` 腳本。
- **`sourceCompatibility` → `java { toolchain }`**：舊寫法在 Gradle 9 已移除；Toolchain 會自動找到對應版本的 JDK，執行 Gradle 的 JDK 與編譯用的 JDK 可以不同。
- **Spring Boot 4 的模組化**：`spring-boot-starter-web` 更名為 `spring-boot-starter-webmvc`，測試改用 `spring-boot-starter-webmvc-test`，`@WebMvcTest` 的 package 也改為 `org.springframework.boot.webmvc.test.autoconfigure`。
- **Java 18+ 預設 UTF-8（JEP 400）**：`file.encoding` 統一為 UTF-8，但 `System.out` 仍依照 Console 編碼（Windows 繁中環境為 MS950），因此中文輸出在不同環境仍可能不同。
