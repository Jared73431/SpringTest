# Spring Batch 多批次處理系統

一個基於 Spring Boot 3.4.4 和 Spring Batch 的多批次處理應用程式，支援定時執行多個獨立的批次作業。

## 功能概述

本系統包含三個獨立的批次作業：
- **Person 資料匯入作業** - 處理人員資料的 CSV 檔案匯入
- **健康保險資料匯入作業** - 處理健康保險資料的 CSV 檔案匯入  
- **Hello World 示範作業** - 簡單的 Hello World 輸出作業

## 技術棧

- **Java 17**
- **Spring Boot 3.4.4**
- **Spring Batch**
- **Spring Data JPA**
- **PostgreSQL**
- **Lombok**

## 專案結構

```
src/main/java/com/example/demo/batch/
├── config/                 # 批次作業配置
│   ├── BatchConfiguration.java
│   ├── HealthInsuranceBatchConfig.java
│   └── HelloBatchConfig.java
├── dto/                    # 資料傳輸物件
│   ├── PersonDTO.java
│   └── HealthInsuranceDTO.java
├── entity/                 # JPA 實體類別
│   ├── Person.java
│   └── HealthInsurance.java
├── listener/               # 批次作業監聽器
│   └── JobCompleteionNotificationListener.java
├── processor/              # 資料處理器
│   ├── PersonItemProcessor.java
│   └── HealthInsuranceItemProcessor.java
├── reader/                 # 資料讀取器
│   ├── PersonItemReader.java
│   └── HealthInsuranceItemReader.java
├── repository/             # 資料庫存取層
│   ├── PersonRepository.java
│   └── HealthInsuranceRepository.java
├── Scheduled/              # 定時任務
│   └── BatchScheduler.java
├── service/                # 服務層
│   └── AsyncJobLauncherService.java
└── writer/                 # 資料寫入器
    ├── PersonItemWriter.java
    └── HealthInsuranceItemWriter.java
```

## 批次作業詳細說明

### 1. Person 資料匯入作業 (importUserJob4)
- **執行頻率**: 每 5 分鐘執行一次
- **資料來源**: `sample-data.csv`
- **處理邏輯**: 將姓名轉換為大寫
- **目標資料表**: `person`
- **批次大小**: 3 筆記錄

### 2. 健康保險資料匯入作業 (healthInsuranceJob)
- **執行頻率**: 每 2 分鐘執行一次
- **資料來源**: `UpdateHealthInsurancePremiumFromNH.csv`
- **處理邏輯**: 直接資料轉換
- **目標資料表**: `healthInsurance`
- **批次大小**: 3 筆記錄

### 3. Hello World 示範作業 (HelloJob8)
- **執行頻率**: 每 1 分鐘執行一次
- **功能**: 輸出 "Hello world2!" 訊息
- **類型**: Tasklet 作業

## 資料模型

### Person 實體
```java
- id (Long) - 主鍵，自動產生
- firstName (String) - 名字
- lastName (String) - 姓氏
```

### HealthInsurance 實體
```java
- id (Long) - 主鍵，自動產生
- fileName (String) - 檔案名稱
- payType (String) - 付款類型
- nationalNo (String) - 國民編號
- cifNo (String) - CIF 編號
- withholdDate (String) - 代扣日期
- withhold (String) - 代扣金額
- reasonType (String) - 原因類型
```

## 環境設定

### 資料庫配置
本專案使用 PostgreSQL 資料庫，請確保已正確配置連線資訊：

```properties
# application.properties 範例
spring.datasource.url=jdbc:postgresql://localhost:5432/batch_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### CSV 檔案配置
請將以下 CSV 檔案放置在 `src/main/resources/` 目錄下：
- `sample-data.csv` - Person 資料檔案
- `UpdateHealthInsurancePremiumFromNH.csv` - 健康保險資料檔案

## 執行方式

### 1. 手動執行
```bash
./gradlew bootRun
```

### 2. 建置可執行 JAR
```bash
./gradlew build
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

## 監控與日誌

- 使用 `@Log4j2` 記錄批次處理過程
- `JobCompleteionNotificationListener` 監聽作業完成狀態
- 控制台輸出作業啟動和完成狀態

## 特殊功能

### 非同步作業執行
- 提供 `AsyncJobLauncherService` 支援非同步執行批次作業
- 使用 `@Async` 註解實現非阻塞執行

### 定時排程
- 使用 `@Scheduled` 註解設定不同的執行頻率
- 支援固定間隔時間執行
- 每次執行使用唯一的 JobParameters 避免重複執行

### 資料驗證
- 作業完成後自動驗證資料庫記錄數量
- 輸出詳細的處理結果日誌

## 依賴套件

主要依賴項目：
- `spring-boot-starter-batch` - Spring Batch 核心功能
- `spring-boot-starter-data-jpa` - JPA 資料存取
- `spring-boot-starter-web` - Web 功能支援
- `postgresql` - PostgreSQL 驅動程式
- `lombok` - 程式碼簡化工具

## 注意事項

1. **資料庫表格**: 系統會自動建立所需的資料表
2. **檔案路徑**: CSV 檔案必須放置在 classpath 中
3. **編碼格式**: 建議使用 UTF-8 編碼
4. **記憶體使用**: 批次大小設為 3，適合大量資料處理時的記憶體管理
5. **作業重複執行**: 系統使用時間戳記避免相同參數的重複執行

## 故障排除

### 常見問題
1. **CSV 檔案讀取失敗**: 檢查檔案是否存在於 resources 目錄
2. **資料庫連線失敗**: 驗證資料庫配置和連線參數
3. **作業執行失敗**: 查看應用程式日誌獲取詳細錯誤資訊

### 日誌級別調整
```properties
logging.level.com.example.demo.batch=DEBUG
logging.level.org.springframework.batch=INFO
```

## 擴展建議

1. **新增批次作業**: 參考現有配置類別建立新的批次作業
2. **客製化處理邏輯**: 修改 Processor 類別實現特定的資料轉換需求
3. **監控整合**: 可整合 Spring Boot Actuator 進行批次作業監控
4. **錯誤處理**: 可新增 Skip 和 Retry 機制處理異常情況