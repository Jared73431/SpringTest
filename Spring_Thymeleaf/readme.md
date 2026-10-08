# Spring Thymeleaf - 伺服器端渲染

用一個很小的留言板（資料存在記憶體）示範 Thymeleaf 伺服器端渲染中，**後端需要知道的部分**：

- Controller 如何把資料交給畫面
- 表單驗證
- PRG 模式
- XSS 防護
- 如何測試

畫面只有幾行 CSS，不是本模組的重點。

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.4 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Spring MVC、Thymeleaf、Validation） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、`@WebMvcTest` + MockMvc |

不需要資料庫。

## 執行方式

```bash
./gradlew bootRun                          # http://localhost:8098
docker build -t spring-thymeleaf .         # 或使用 Docker
docker run --rm -p 8098:8098 spring-thymeleaf
./gradlew test
```

| 方法 | URL | 說明 |
|---|---|---|
| GET | `/` | 重新導向到 `/messages` |
| GET | `/messages` | 留言列表與表單 |
| POST | `/messages` | 送出留言（表單）：成功時重新導向，失敗時回到同一頁顯示錯誤 |

## 重點

### 1. `@Controller` 回傳的是 view 名稱

```java
@GetMapping("/messages")
public String list(Model model) {
    model.addAttribute("messages", messageService.findAll());
    model.addAttribute("form", new MessageForm());
    return "messages";            // → templates/messages.html
}
```

| | `@RestController` | `@Controller` + Thymeleaf |
|---|---|---|
| 回傳值 | 資料本身，轉成 JSON | view 名稱，Thymeleaf 產生 HTML |
| 資料傳遞 | 回傳值 | `Model` |
| 畫面 | 前端（React、Vue…）自己畫 | 伺服器產生完整的 HTML |

### 2. 常用的 Thymeleaf 語法

| 語法 | 用途 |
|---|---|
| `th:text="${message.content}"` | 輸出文字，**會跳脫 HTML** |
| `th:each="message : ${messages}"` | 迴圈 |
| `th:if="${#lists.isEmpty(messages)}"` | 條件顯示 |
| `th:object="${form}"` + `th:field="*{author}"` | 表單綁定：產生 `name`、`id`、`value` |
| `th:errors="*{author}"` | 顯示欄位的驗證錯誤 |
| `@{/messages}` | 產生網址（自動加上 context path） |
| `#temporals.format(...)` | 格式化 `LocalDateTime` |

> 共用的頁首、頁尾可以用 `th:fragment` / `th:replace` 抽出成片段。這個模組只有一頁，所以沒有使用。

### 3. XSS：`th:text` vs `th:utext`

```html
<div th:text="${message.content}"></div>    <!-- <script> 變成 &lt;script&gt;，只會顯示成文字 -->
<div th:utext="${message.content}"></div>   <!-- 原樣輸出 HTML：使用者輸入的 <script> 會被執行 -->
```

**使用者輸入的內容一律用 `th:text`。**`th:utext` 只用在自己產生、確定安全的 HTML。測試中有驗證 `<script>` 會被跳脫。

### 4. 表單驗證：失敗時回到同一頁，不是 400

```java
@PostMapping("/messages")
public String add(@Valid @ModelAttribute("form") MessageForm form, BindingResult bindingResult, ...) {
    if (bindingResult.hasErrors()) {
        model.addAttribute("messages", messageService.findAll());
        return "messages";          // 回到同一頁：顯示錯誤，並保留使用者剛輸入的內容
    }
    ...
}
```

| | REST API | 伺服器端渲染 |
|---|---|---|
| 驗證失敗 | 回傳 400 + 錯誤內容 | 回到同一頁（200），顯示錯誤並填回輸入 |
| 寫法 | `@Valid @RequestBody`，交給 `@RestControllerAdvice` | `@Valid @ModelAttribute` + 緊接著的 `BindingResult` 參數 |

> `BindingResult` 必須**緊接在**被驗證的參數後面。沒有它的話，驗證失敗會直接丟出例外，而不是交給你處理。
>
> 表單物件 `MessageForm` 用一般的 class（getter / setter），不用 record：表單綁定與 `th:field` 都透過 getter / setter 存取。

### 5. PRG 模式（Post / Redirect / Get）

```java
messageService.add(...);
redirectAttributes.addFlashAttribute("notice", "已送出留言");
return "redirect:/messages";
```

POST 成功後如果直接回傳頁面，使用者按 F5 時瀏覽器會**重新送出表單**，造成重複留言。改成重新導向後，F5 只會重新 GET。

**flash attribute** 存在 session 中，只在重新導向後的下一個請求出現一次，適合顯示「已送出」之類的訊息。

> 因為 flash 用到 session，第一次請求時 Tomcat 會把 session id 寫進網址（`/messages;jsessionid=...`）。網址會出現在瀏覽器歷史、伺服器 log 與 Referer 中，可能外洩 session id，所以設定 `server.servlet.session.tracking-modes: cookie`，只用 cookie 追蹤 session。

### 6. 測試：不需要開瀏覽器

```java
mockMvc.perform(post("/messages").param("author", "Amy").param("content", "哈囉"))
        .andExpect(redirectedUrl("/messages"))
        .andExpect(flash().attribute("notice", "已送出留言"));

mockMvc.perform(post("/messages").param("author", "Amy").param("content", " "))
        .andExpect(view().name("messages"))
        .andExpect(model().attributeHasFieldErrors("form", "content"));
```

`@WebMvcTest` 只啟動 Web 層，可以驗證 view 名稱、Model、重新導向、flash，以及產生出來的 HTML 內容。

### 7. 什麼時候用伺服器端渲染？

| 適合 | 不適合 |
|---|---|
| 後台管理頁、內部工具 | 互動複雜的前端（拖拉、即時更新）→ 前後端分離 |
| 需要 SEO、首次載入要快的頁面 | 同一份 API 要給 App 與網頁共用 |
| Email 內容、PDF 報表的範本（Thymeleaf 不一定要搭配網頁） | 前端團隊獨立開發、部署 |

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Baseline 測試 |
| 2 | Java 17 → 21、Spring Boot 3.4.5 → 3.5.16、Gradle 8.13 → 8.14.3 |
| 3 | Spring Boot 4.1.1、Gradle 9.8.0 |
| 4 | 靜態的 hello 頁面改成留言板，修正設定 |
| 5 | Dockerfile、session 只用 cookie 追蹤 |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| Thymeleaf 設定沒有作用 | `application.yml` 把 `thymeleaf:` 寫在最外層，不是 `spring.thymeleaf.*`。值剛好都是預設值，所以看起來沒問題，現在已刪除 |
| `mode: HTML5` | 已經 deprecated，應該寫 `HTML` |
| 頁面沒有使用 Thymeleaf | `hello.html` 沒有任何 `th:` 屬性，是靜態頁面 |
| 其他 | 類別名稱 `appController` 小寫開頭；引入了沒有使用的 Lombok；YAML 註解寫成 `設...` 的跳脫字元 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | 8080 | 8098 |
| 頁面 | `GET /hello`（靜態的 Hello World） | `GET /messages` 留言板，`/` 重新導向到這裡 |
