# Spring Boot Hello World 專案

一個簡單的 Spring Boot Hello World 應用程式，展示基本的 REST API 端點。

## 📋 專案資訊

- **框架**: Spring Boot 2.7.4
- **Java 版本**: 11
- **建構工具**: Gradle
- **作者**: Jared

## 🚀 功能特色

- 提供簡單的 `/hello` REST API 端點
- 返回 "Hello World" 訊息
- 包含控制台輸出範例
- 展示 Java 8 Stream API 使用

## 🛠️ 技術棧

- **Spring Boot**: 2.7.4
- **Spring Web**: 用於建立 REST API
- **Spring Boot DevTools**: 開發時熱重載
- **JUnit**: 單元測試框架

## 📦 專案結構

```
src/
├── main/
│   └── java/
│       └── com/
│           └── example/
│               └── demo/
│                   └── controller/
│                       └── HelloController.java
└── test/
    └── java/
```

## 🔧 環境需求

- Java 11 或更高版本
- Gradle 6.0 或更高版本

## 📥 安裝步驟

1. **複製專案**
   ```bash
   git clone <repository-url>
   cd demo
   ```

2. **建構專案**
   ```bash
   ./gradlew build
   ```

3. **執行應用程式**
   ```bash
   ./gradlew bootRun
   ```

   或者執行編譯後的 JAR 檔案：
   ```bash
   java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
   ```

## 🌐 API 端點

### GET /hello

返回簡單的問候訊息。

**請求**
```http
GET http://localhost:8080/hello
```

**回應**
```
Hello World
```

**範例使用**
```bash
# 使用 curl
curl http://localhost:8080/hello

# 使用瀏覽器
開啟 http://localhost:8080/hello
```

## 🧪 測試

執行所有測試：
```bash
./gradlew test
```

## 📝 程式碼說明

### HelloController.java

主要的控制器類別，包含：

- `@RestController` 註解：標示這是一個 REST 控制器
- `@GetMapping("/hello")` 註解：定義 GET 請求映射
- `hello()` 方法：處理 `/hello` 端點請求
- `main()` 方法：包含控制台輸出和 Stream API 範例

### 核心功能

1. **REST API 端點**: 提供 `/hello` 端點返回問候訊息
2. **控制台輸出**: 展示中文字符輸出
3. **Stream 處理**: 使用 Java 8 Stream API 過濾和處理字串列表

## 🔧 開發工具

專案包含 Spring Boot DevTools，提供：
- 自動重啟應用程式
- 即時重載靜態資源
- 開發時的額外除錯資訊

## 📚 相關文件

- [Spring Boot 官方文件](https://spring.io/projects/spring-boot)
- [Spring Web MVC 參考指南](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html)
- [Gradle 使用者指南](https://docs.gradle.org/current/userguide/userguide.html)

## 🤝 貢獻

歡迎提交 Pull Request 或回報 Issue！

## 📄 授權

此專案採用 [MIT License](LICENSE)。

---

**注意**: 此為學習用途的簡單範例專案，展示 Spring Boot 的基本用法。