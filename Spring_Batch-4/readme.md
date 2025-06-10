# Spring Boot 多批次處理系統

## 專案概述

這是一個基於Spring Boot和Spring Batch的多批次處理系統，展示了如何同時執行多個批次作業。系統包含三個不同的批次作業：人員資料處理、健保資料處理和簡單的Hello World作業。

## 系統架構

### 核心組件

- **Spring Boot 3.x**
- **Spring Batch**
- **JPA/Hibernate**
- **Lombok**
- **Log4j2**

### 批次作業類型

1. **Person批次作業** (`importUserJob4`)
   - 讀取CSV文件中的人員資料
   - 將姓名轉換為大寫
   - 存儲到person資料表

2. **健保批次作業** (`healthInsuranceJob`)
   - 讀取健保保費CSV文件
   - 處理健保相關資料
   - 存儲到healthInsurance資料表

3. **Hello World批次作業** (`HelloJob8`)
   - 簡單的示範作業
   - 輸出"Hello world2!"訊息

## 專案結構

```
src/main/java/com/example/demo/batch/
├── config/                     # 批次配置類
│   ├── BatchConfiguration.java      # Person批次配置
│   ├── HealthInsuranceBatchConfig.java  # 健保批次配置
│   └── HelloBatchConfig.java       # Hello World批次配置
├── dto/                        # 資料傳輸物件
│   ├── PersonDTO.java
│   └── HealthInsuranceDTO.java
├── entity/                     # JPA實體類
│   ├── Person.java
│   └── HealthInsurance.java
├── listener/                   # 批次監聽器
│   └── JobCompleteionNotificationListener.java
├── processor/                  # 資料處理器
│   ├── PersonItemProcessor.java
│   └── HealthInsuranceItemProcessor.java
├── reader/                     # 資料讀取器
│   ├── PersonItemReader.java
│   └── HealthInsuranceItemReader.java
├── writer/                     # 資料寫入器
│   ├── PersonItemWriter.java
│   └── HealthInsuranceItemWriter.java
├── runner/                     # 批次執行器
│   └── MultiJobRunner.java
├── Scheduled/                  # 排程器
│   └── BatchScheduler.java
└── service/                    # 服務層
    └── AsyncJobLauncherService.java
```

## 功能特色

### 1. 多批次同時執行
- 使用`MultiJobRunner`實現多個批次作業的同時啟動
- 透過`AsyncJobLauncherService`提供異步執行能力
- 每個作業使用不同的時間戳參數確保獨立性

### 2. 排程執行
- `BatchScheduler`提供定時執行功能
- 每5分鐘自動執行所有批次作業
- 支援動態作業列表管理

### 3. 完整的ETL流程
- **Extract (讀取)**：從CSV文件讀取資料
- **Transform (轉換)**：使用自定義處理器轉換資料
- **Load (載入)**：將處理後的資料存儲到資料庫

### 4. 監控與日誌
- 作業完成監聽器提供執行狀態監控
- 詳細的日誌記錄便於問題追蹤
- 資料庫記錄數量統計

## 設定說明

### 必要的CSV文件
請確保在`src/main/resources/`目錄下放置以下文件：

1. `sample-data.csv` - 人員資料文件
   ```csv
   firstName,lastName
   John,Doe
   Jane,Smith
   ```

2. `UpdateHealthInsurancePremiumFromNH.csv` - 健保資料文件
   ```csv
   fileName,payType,nationalNo,cifNo,withholdDate,withhold,reasonType
   "file1","A","1234567890","CIF001","2024-01-01","1000","TYPE1"
   ```

### 資料庫設定
系統使用JPA自動建表，需要配置以下資料表：

- `person` - 存儲人員資料
- `healthInsurance` - 存儲健保資料

## 執行方式

### 1. 自動執行（推薦）
系統啟動後會自動執行所有批次作業：
```bash
mvn spring-boot:run
```

### 2. 排程執行
系統會每5分鐘自動執行一次所有批次作業

### 3. 手動執行特定作業
可以透過Spring Batch Admin或自定義端點執行特定作業

## 配置參數

### 批次處理參數
- **Chunk Size**: 3 (每次處理3筆記錄)
- **Transaction Manager**: 使用JPA事務管理器
- **Skip Lines**: 健保資料文件跳過標題行

### 異步配置
- 啟用`@EnableAsync`支援異步執行
- 使用Spring的預設線程池

## 日誌輸出範例

```
INFO  - Converting (PersonDTO[firstName=john, lastName=doe]) into (Person(id=null, firstName=JOHN, lastName=DOE))
INFO  - JOB FINISH! Time to verify the results
INFO  - Total records in database: 2
INFO  - FOUND <Person(id=1, firstName=JOHN, lastName=DOE)> in the database
```

## 錯誤處理

- 使用Spring Batch的內建重試機制
- 異步執行時的例外會被捕獲並記錄
- 每個作業獨立執行，單一作業失敗不影響其他作業

## 擴展功能

### 添加新的批次作業
1. 創建新的配置類繼承批次配置模式
2. 實現對應的Reader、Processor、Writer
3. 在Runner中註冊新作業

### 自定義排程
修改`BatchScheduler`中的`@Scheduled`註解參數：
```java
@Scheduled(fixedRate = 300000) // 5分鐘
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2點
```

## 注意事項

1. 確保CSV文件格式正確，特別是健保資料文件的引號處理
2. 資料庫連接需要正確配置
3. 多作業同時執行時注意資源競爭
4. 大量資料處理時建議調整chunk size和線程池配置

## 技術要點

- 使用Java Record作為DTO，簡化代碼
- 利用反射動態獲取Record字段名稱
- 組件化設計，Reader/Processor/Writer分離
- 使用Qualifier註解解決Bean命名衝突
- 異步執行提升系統吞吐量

## 依賴版本

- Spring Boot: 3.x
- Spring Batch: 5.x
- Java: 17+
- Lombok: 最新版本

---

## 聯絡資訊

如有問題或建議，請聯絡開發團隊。