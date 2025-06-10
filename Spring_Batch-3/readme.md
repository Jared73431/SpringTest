# Spring Batch 練習專案 - 批次處理與定時調度

## 專案概述

這是一個基於Spring Boot和Spring Batch的批次處理應用程式，專注於CSV檔案的讀取、資料處理和資料庫寫入操作。本專案實現了完整的ETL流程，並包含定時調度功能。

## 主要功能

- **CSV檔案讀取**：從`sample-data.csv`檔案中讀取人員資料
- **資料處理**：將姓名轉換為大寫格式
- **資料庫寫入**：將處理後的資料儲存到資料庫
- **定時執行**：每5分鐘自動執行一次批次作業
- **執行監控**：完整的作業執行狀態監控和記錄

## 技術架構

### 核心技術棧
- **Spring Boot 3.x**
- **Spring Batch**
- **Spring Data JPA**
- **Lombok**
- **Log4j2**

### 資料庫
- 使用JPA進行資料持久化
- 支援多種關聯式資料庫

## 專案結構

```
src/main/java/com/example/demo/batch/
├── config/
│   └── BatchConfiguration.java      # 批次作業配置
├── dto/
│   └── PersonDTO.java              # 資料傳輸物件
├── entity/
│   └── Person.java                 # 資料庫實體類
├── listener/
│   └── JobCompletionNotificationListener.java  # 作業完成監聽器
├── processor/
│   └── PersonItemProcessor.java    # 資料處理器
├── repository/
│   └── PersonRepository.java       # 資料存取層
└── scheduled/
    └── BatchScheduler.java         # 定時調度器
```

## 核心組件說明

### 1. 資料模型
- **PersonDTO**: Record型別，用於CSV資料讀取
- **Person**: JPA實體類，對應資料庫表結構

### 2. 批次處理流程
1. **Reader**: `FlatFileItemReader` 讀取CSV檔案
2. **Processor**: `PersonItemProcessor` 將姓名轉換為大寫
3. **Writer**: `RepositoryItemWriter` 寫入資料庫

### 3. 定時調度
- 使用`@Scheduled`註解實現定時執行
- 每5分鐘（300000毫秒）執行一次
- 透過時間戳參數確保每次執行的唯一性

### 4. 監控機制
- 作業完成後自動驗證資料庫記錄
- 詳細的執行日誌記錄
- 執行狀態追蹤

## 配置要點

### 批次作業配置
```java
@Bean(name = "importUserJob4")
public Job importUserJob(JobRepository jobRepository, Step step1,
                         JobCompletionNotificationListener listener) {
    return new JobBuilder("importUserJob4", jobRepository)
            .listener(listener)
            .start(step1)
            .build();
}
```

### 步驟配置
- **Chunk Size**: 3（每次處理3筆記錄）
- **事務管理**: 使用JPA事務管理器
- **錯誤處理**: 內建Spring Batch錯誤處理機制

## 使用方式

### 1. 環境準備
確保您的`application.properties`或`application.yml`中包含資料庫連接配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/batch_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=create-drop
```

### 2. 準備資料檔案
在`src/main/resources/`目錄下放置`sample-data.csv`檔案：
```csv
firstName,lastName
John,Doe
Jane,Smith
Bob,Johnson
```

### 3. 執行應用程式
```bash
mvn spring-boot:run
```

### 4. 監控執行結果
應用程式啟動後會自動開始定時執行，您可以在日誌中看到執行狀態：
```
JOB FINISH! Time to verify the results
Total records in database: 3
FOUND <Person(id=1, firstName=JOHN, lastName=DOE)> in the database
```

## 執行流程

1. **應用程式啟動**
2. **定時調度器開始工作**（每5分鐘執行一次）
3. **批次作業執行**：
   - 讀取CSV檔案
   - 處理資料（轉換為大寫）
   - 寫入資料庫
4. **執行完成通知**：
   - 驗證資料庫記錄
   - 記錄執行結果

## 特色功能

### 資料處理
- 自動將姓名轉換為大寫格式
- 支援批次處理，提高效能
- 完整的資料驗證機制

### 錯誤處理
- Spring Batch內建的重試機制
- 詳細的錯誤日誌記錄
- 事務回滾保證資料一致性

### 監控與日誌
- 執行前後的資料對比記錄
- 資料庫記錄數量統計
- 完整的執行狀態追蹤

## 後續擴展

本專案為基礎版本，後續可以擴展的功能包括：

- **多執行緒並行處理**：提高大量資料的處理效能
- **不同作業的順序執行**：實現複雜的業務流程
- **條件式作業執行**：根據不同條件執行不同的處理邏輯
- **動態參數配置**：支援運行時參數調整
- **Web監控介面**：提供視覺化的作業監控面板

## 注意事項

1. **資料庫配置**：確保資料庫連接正確配置
2. **檔案路徑**：CSV檔案必須放在正確的classpath位置
3. **記憶體使用**：注意Chunk Size的設定，避免記憶體溢出
4. **並發執行**：目前版本不支援同時執行多個相同作業

## 故障排除

### 常見問題
1. **作業無法啟動**：檢查資料庫連接和CSV檔案是否存在
2. **資料未寫入**：確認事務管理器配置正確
3. **定時執行失效**：檢查`@EnableScheduling`註解是否生效

### 日誌等級調整
```properties
logging.level.com.example.demo.batch=DEBUG
logging.level.org.springframework.batch=INFO
```

## 版本資訊

- **專案版本**: 1.0.0
- **Spring Boot版本**: 3.x
- **Java版本**: 17+
- **建置工具**: Maven

---

*此專案為Spring Batch學習系列的第三個練習專案，專注於定時批次處理的實現。*