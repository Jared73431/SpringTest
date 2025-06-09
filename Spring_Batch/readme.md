# Spring Batch 基礎範例專案

## 專案簡介

這是一個基於 Spring Boot 3.4.4 的 Spring Batch 基礎範例專案。由於網路上大部分的教學資源都是針對舊版本的 Spring Batch，此專案提供了最新版本的實作範例，展示了如何建立一個簡單的批次處理作業。

## 專案特色

- 使用最新的 Spring Boot 3.4.4 版本
- 採用 Java 17 語言版本
- 示範基本的 Spring Batch Job 和 Step 設定
- 使用 H2 記憶體資料庫作為作業執行的 JobRepository
- 提供簡潔易懂的程式碼結構

## 技術堆疊

- **Java**: 17
- **Spring Boot**: 3.4.4
- **Spring Batch**: 最新版本（透過 Spring Boot Starter 管理）
- **資料庫**: H2（記憶體資料庫）
- **建構工具**: Gradle
- **其他工具**: Lombok

## 專案結構

```
src/
├── main/
│   └── java/
│       └── com/example/demo/
│           └── SpringBatchApplication.java    # 主要應用程式類別
└── test/
    └── java/
        └── com/example/demo/
            └── ...                           # 測試檔案
```

## 核心組件說明

### SpringBatchApplication.java

主要的應用程式類別，包含以下重要組件：

1. **Step (sampleStep)**
   - 定義了一個名為 "helloStep2" 的步驟
   - 使用 Tasklet 模式執行簡單的 "Hello world!" 輸出
   - 執行完成後回傳 `RepeatStatus.FINISHED`

2. **Job (sampleJob)**
   - 定義了一個名為 "HelloJob" 的作業
   - 包含單一步驟 `sampleStep`

3. **配置**
   - 使用 `@Configuration` 和 `@Bean` 註解進行 Spring 配置
   - 自動注入 `JobRepository` 和 `DataSourceTransactionManager`

## 執行環境需求

- Java 17 或更高版本
- Gradle 7.0 或更高版本

## 如何執行

### 1. 複製專案
```bash
git clone [repository-url]
cd spring-batch-basic-example
```

### 2. 執行專案
```bash
./gradlew bootRun
```

或者使用 IDE 直接執行 `SpringBatchApplication` 的 main 方法。

### 3. 預期輸出
程式執行後會在控制台看到：
```
Hello world!
```

## 重要依賴項目

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    runtimeOnly 'com.h2database:h2'
    // ... 其他依賴
}
```

## 與舊版本的差異

相較於舊版本的 Spring Batch 教學，此專案採用了：

1. **新的建構器 API**：使用 `JobBuilder` 和 `StepBuilder` 替代舊版的 Factory 類別
2. **依賴注入改進**：直接注入 `JobRepository` 和 `DataSourceTransactionManager`
3. **Spring Boot 3.x 相容性**：確保與最新的 Spring 生態系統相容

## 後續學習路徑

這是一個系列專案的第一個範例，後續將包含更多進階情境：

- 檔案讀取和寫入範例
- 資料庫批次處理
- 條件式流程控制
- 平行處理和分片
- 異常處理和重試機制
- 監控和日誌記錄

## 常見問題

### Q: 為什麼使用 H2 資料庫？
A: H2 是一個輕量級的記憶體資料庫，適合用於開發和測試環境。Spring Batch 需要資料庫來儲存作業執行的中繼資料。

### Q: 如何查看作業執行歷史？
A: 可以透過 H2 Console 或者 Spring Boot Actuator 來查看作業執行狀態和歷史記錄。

### Q: 如何修改為使用其他資料庫？
A: 修改 `build.gradle` 中的資料庫依賴，並在 `application.properties` 中設定相對應的資料庫連線資訊。

## 貢獻指南

歡迎提交 Issue 和 Pull Request 來改善這個範例專案。請確保：

1. 程式碼風格一致
2. 包含適當的測試
3. 更新相關文件

## 授權

此專案採用 MIT 授權條款。

---

**注意**: 此專案僅供學習和參考使用，實際專案中請根據具體需求進行適當的調整和優化。