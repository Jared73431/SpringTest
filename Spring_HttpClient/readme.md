# Spring Boot HttpClient 練習專案

這是一個用於練習Spring Boot HttpClient的完整範例專案，使用JSONPlaceholder API作為測試端點。

## 專案特色

- 🚀 使用Spring WebFlux WebClient進行HTTP請求
- 📊 支援所有HTTP方法（GET、POST、PUT、DELETE）
- 🔄 響應式程式設計（Reactive Programming）
- 🧪 完整的單元測試
- 📝 詳細的日誌輸出
- 🎯 多種測試方式

## 技術棧

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring WebFlux**
- **Gradle**
- **JUnit 5**
- **Reactor Test**

## 專案結構

```
src/
├── main/
│   └── java/
│       └── com/example/demo/
│           ├── Application.java              # 主程式
│           ├── runner
│           │   └──DemoRunner.java              # 示範執行器
│           ├── controller/
│           │   ├── HttpController.java      # REST API控制器
│           │   └── TestController.java      # 測試控制器
│           ├── model/
│           │   └── Post.java                # 資料模型
│           └── service/
│               └── HttpClientService.java   # HTTP客戶端服務
└── test/
    └── java/
        └── com/example/demo/service/
            └── HttpClientTest.java          # 單元測試
```

## 快速開始

### 1. 克隆專案

```bash
git clone <repository-url>
cd spring-boot-httpclient
```

### 2. 運行專案

```bash
./gradlew bootRun
```

### 3. 查看示範

專案啟動後會自動執行HTTP請求示範，你可以在控制台看到：
- GET請求獲取貼文
- POST請求建立貼文
- PUT請求更新貼文
- DELETE請求刪除貼文

## API 端點

### 主要API端點

| 方法 | 端點 | 描述 |
|------|------|------|
| GET | `/api/posts/{id}` | 獲取單一貼文 |
| GET | `/api/posts` | 獲取所有貼文 |
| POST | `/api/posts` | 建立新貼文 |
| PUT | `/api/posts/{id}` | 更新貼文 |
| DELETE | `/api/posts/{id}` | 刪除貼文 |

### 測試端點

| 方法 | 端點 | 描述 |
|------|------|------|
| GET | `/test/get/{id}` | 測試GET請求 |
| POST | `/test/post-simple` | 測試POST請求（URL參數） |
| POST | `/test/post-json` | 測試POST請求（JSON） |
| GET | `/test/create-sample` | 建立範例貼文 |

## 測試方式

### 1. 瀏覽器測試（最簡單）

直接在瀏覽器中訪問：
- `http://localhost:8080/test/get/1`
- `http://localhost:8080/test/create-sample`
- `http://localhost:8080/api/posts`

### 2. curl 測試

#### GET 請求
```bash
# 獲取單一貼文
curl -X GET http://localhost:8080/test/get/1

# 獲取所有貼文
curl -X GET http://localhost:8080/api/posts
```

#### POST 請求
```bash
# 使用URL參數
curl -X POST "http://localhost:8080/test/post-simple?userId=1&title=我的標題&body=我的內容"

# 使用JSON請求體
curl -X POST http://localhost:8080/test/post-json \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "我的新貼文",
    "body": "這是貼文內容"
  }'

# 建立範例貼文
curl -X GET http://localhost:8080/test/create-sample
```

#### PUT 請求
```bash
curl -X PUT http://localhost:8080/api/posts/1 \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "userId": 1,
    "title": "更新後的標題",
    "body": "更新後的內容"
  }'
```

#### DELETE 請求
```bash
curl -X DELETE http://localhost:8080/api/posts/1
```

### 3. Postman 測試

#### GET 請求
- Method: `GET`
- URL: `http://localhost:8080/test/get/1`

#### POST 請求
- Method: `POST`
- URL: `http://localhost:8080/test/post-json`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "userId": 1,
  "title": "我的標題",
  "body": "我的內容"
}
```

### 4. 單元測試

```bash
# 執行所有測試
./gradlew test

# 執行特定測試
./gradlew test --tests HttpClientTest
```

## 資料模型

```java
public class Post {
    private Long id;
    private Long userId;
    private String title;
    private String body;
    
    // 建構子、getter、setter...
}
```

## 核心服務

### HttpClientService

使用Spring WebFlux的WebClient進行HTTP請求：

```java
@Service
public class HttpClientService {
    private final WebClient webClient;
    
    // GET請求
    public Mono<Post> getPost(Long id) { ... }
    
    // POST請求
    public Mono<Post> createPost(Post post) { ... }
    
    // PUT請求
    public Mono<Post> updatePost(Long id, Post post) { ... }
    
    // DELETE請求
    public Mono<Void> deletePost(Long id) { ... }
}
```

## 常見問題解決

### 1. Required request body is missing

**問題**：POST請求時出現此錯誤
**解決方案**：
- 確保POST請求包含 `Content-Type: application/json` header
- 確保請求體不為空且格式正確
- 使用測試端點 `/test/post-simple` 進行簡單測試

### 2. JSON格式錯誤

**問題**：JSON解析失敗
**解決方案**：
- 檢查JSON語法是否正確
- 確保字串使用雙引號
- 使用JSON驗證工具檢查格式

### 3. 連線問題

**問題**：無法連接到服務
**解決方案**：
- 確保Spring Boot應用程式已啟動
- 檢查端口8080是否被佔用
- 確認防火牆設定

## 學習重點

### 1. WebClient vs RestTemplate
- WebClient是Spring 5+推薦的HTTP客戶端
- 支援響應式程式設計
- 更好的效能和非阻塞I/O

### 2. 響應式程式設計
- 使用Mono和Flux處理非同步操作
- 支援背壓（backpressure）
- 更好的資源利用率

### 3. 錯誤處理
- 使用doOnError處理錯誤
- 適當的異常處理機制
- 日誌記錄和監控

## 擴展建議

1. **添加認證機制**：實作JWT或OAuth2認證
2. **添加快取**：使用Redis或Caffeine快取
3. **添加重試機制**：處理網路不穩定情況
4. **添加監控**：使用Micrometer和Prometheus
5. **添加文檔**：使用OpenAPI/Swagger自動生成API文檔

## 參考資源

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [JSONPlaceholder API](https://jsonplaceholder.typicode.com/)
- [Project Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

## 授權

本專案使用MIT授權條款。

## 貢獻

歡迎提交Issue和Pull Request！

---

**開始你的HTTP Client學習之旅！** 🚀