# OKHttp Spring Boot 練習項目

## 📖 項目簡介

這是一個 Spring Boot 項目，展示如何在 Spring 框架中整合和使用 OKHttp 客戶端進行 HTTP 請求。項目包含同步和異步請求的範例，以及完整的日誌記錄功能。

## 🎯 功能特色

- **Spring Boot 整合**：使用 Spring 的依賴注入管理 OKHttp 客戶端
- **HTTP 請求範例**：包含 GET、POST 和異步請求
- **日誌記錄**：詳細的請求和響應日誌
- **JSON 處理**：使用 Gson 進行 JSON 序列化和反序列化
- **錯誤處理**：完善的異常處理機制
- **超時配置**：可配置的連接、讀取和寫入超時

## 🛠️ 技術棧

- **Spring Boot** - 主框架
- **OKHttp3** - HTTP 客戶端
- **Gson** - JSON 處理
- **HttpLoggingInterceptor** - 請求日誌記錄

## 📁 項目結構

```
src/main/java/com/example/demo/
├── config/
│   └── OKHttpConfig.java          # OKHttp 配置類
├── controller/
│   └── OKHttpController.java      # REST 控制器
└── service/
    └── OKHttpService.java         # 業務邏輯服務
```

## 🚀 快速開始

### 1. 克隆項目
```bash
git clone [your-repository-url]
cd [project-directory]
```

### 2. 運行項目
```bash
./mvnw spring-boot:run
```

### 3. 測試 API 端點

項目啟動後，可以訪問以下端點：

- **健康檢查**：`GET http://localhost:8080/api/okhttp/health`
- **測試 GET 請求**：`GET http://localhost:8080/api/okhttp/test-get`
- **測試 POST 請求**：`POST http://localhost:8080/api/okhttp/test-post`
- **測試異步請求**：`GET http://localhost:8080/api/okhttp/test-async`

## 📋 API 端點詳細說明

### GET /api/okhttp/health
- **功能**：健康檢查端點
- **響應**：確認服務正在運行

### GET /api/okhttp/test-get
- **功能**：執行同步 GET 請求到 JSONPlaceholder API
- **目標 URL**：`https://jsonplaceholder.typicode.com/posts/1`
- **響應**：請求執行狀態，詳細信息查看控制台日誌

### POST /api/okhttp/test-post
- **功能**：執行同步 POST 請求到 JSONPlaceholder API
- **目標 URL**：`https://jsonplaceholder.typicode.com/posts`
- **請求體**：包含測試文章數據的 JSON
- **響應**：請求執行狀態，詳細信息查看控制台日誌

### GET /api/okhttp/test-async
- **功能**：執行異步 GET 請求
- **目標 URL**：`https://jsonplaceholder.typicode.com/posts/1`
- **響應**：立即返回，實際請求結果在控制台日誌中查看

## ⚙️ 配置說明

### OKHttp 客戶端配置 (OKHttpConfig.java)

```java
@Bean
public OkHttpClient okHttpClient(HttpLoggingInterceptor loggingInterceptor) {
    return new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)  // 添加日誌攔截器
            .connectTimeout(30, TimeUnit.SECONDS)  // 連接超時：30秒
            .readTimeout(30, TimeUnit.SECONDS)     // 讀取超時：30秒
            .writeTimeout(30, TimeUnit.SECONDS)    // 寫入超時：30秒
            .build();
}
```

### 日誌級別配置

當前配置為 `HttpLoggingInterceptor.Level.BODY`，會記錄：
- 請求和響應的標頭
- 請求和響應的主體內容

可選的日誌級別：
- `NONE`：無日誌
- `BASIC`：請求和響應行
- `HEADERS`：請求和響應行 + 標頭
- `BODY`：請求和響應行 + 標頭 + 主體

## 📊 日誌輸出範例

### GET 請求日誌
```
📥 執行 GET 請求...
使用 Spring Boot 管理的 OkHttpClient

✅ GET 請求成功！
狀態碼: 200
響應標頭數量: 12

📄 解析後的文章對象:
Post{userId=1, id=1, title='sunt aut facere...', body='quia et suscipit...'}
```

### POST 請求日誌
```
📤 執行 POST 請求...
使用 Spring Boot + LoggingInterceptor

📝 要發送的數據: {"userId":1,"title":"Spring Boot + OKHttp 測試","body":"這是在 Spring Boot 中使用 OKHttp 發送的 POST 請求"}

✅ POST 請求成功！
狀態碼: 201
響應標頭數量: 12

📄 伺服器創建的文章:
Post{userId=1, id=101, title='Spring Boot + OKHttp 測試', body='這是在 Spring Boot 中使用 OKHttp 發送的 POST 請求'}
```

## 🔧 自定義配置

### 修改超時設置
在 `OKHttpConfig.java` 中調整超時參數：

```java
.connectTimeout(60, TimeUnit.SECONDS)  // 修改為60秒
.readTimeout(60, TimeUnit.SECONDS)
.writeTimeout(60, TimeUnit.SECONDS)
```

### 添加自定義攔截器
```java
@Bean
public OkHttpClient okHttpClient() {
    return new OkHttpClient.Builder()
            .addInterceptor(new CustomInterceptor())  // 添加自定義攔截器
            .addInterceptor(loggingInterceptor())
            .build();
}
```

### 修改目標 API
在 `OKHttpService.java` 中修改請求 URL：

```java
String url = "https://your-api-endpoint.com/api/posts";
```

## 🧪 測試建議

### 使用 cURL 測試
```bash
# 測試 GET 請求
curl -X GET http://localhost:8080/api/okhttp/test-get

# 測試 POST 請求
curl -X POST http://localhost:8080/api/okhttp/test-post

# 測試異步請求
curl -X GET http://localhost:8080/api/okhttp/test-async

# 健康檢查
curl -X GET http://localhost:8080/api/okhttp/health
```

### 使用 Postman 或其他 API 測試工具
導入上述端點進行測試，觀察控制台輸出以查看詳細的請求和響應信息。

## 📝 學習要點

1. **依賴注入**：學習如何在 Spring Boot 中配置和注入 OKHttp 客戶端
2. **攔截器使用**：理解如何使用攔截器進行日誌記錄和請求處理
3. **同步 vs 異步**：掌握同步和異步請求的不同處理方式
4. **JSON 處理**：學習使用 Gson 進行 JSON 序列化和反序列化
5. **錯誤處理**：了解網絡請求的異常處理最佳實踐
6. **配置管理**：學習如何配置超時、攔截器等參數

## 🔍 故障排除

### 常見問題

1. **連接超時**
   - 檢查網絡連接
   - 調整超時設置
   - 確認目標 API 可訪問

2. **JSON 解析錯誤**
   - 確認響應格式是否為有效 JSON
   - 檢查數據模型類的字段是否匹配

3. **404 錯誤**
   - 確認 API 端點路徑正確
   - 檢查 Spring Boot 應用是否正常啟動

## 📚 延伸學習

- [OKHttp 官方文檔](https://square.github.io/okhttp/)
- [Spring Boot 官方指南](https://spring.io/guides/gs/spring-boot/)
- [Gson 用戶指南](https://github.com/google/gson/blob/master/UserGuide.md)

## 🤝 貢獻

歡迎提交問題和改進建議！

## 📄 許可證

本項目僅供學習和練習使用。