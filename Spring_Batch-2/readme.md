# Spring Batch 資料庫配置練習

這是第二個 Spring Batch 練習專案，重點關注資料庫配置和 schema 初始化，特別探討 `spring.sql.init.schema-locations` 屬性與 PostgreSQL 整合的應用。

## 專案概述

此 Spring Boot 應用程式示範：
- Spring Batch 與 PostgreSQL 資料庫的配置
- Spring Batch 元數據表的資料庫 schema 初始化
- 交易管理配置
- 基本批次作業和步驟實作

## 核心配置檔案

### 1. 應用程式屬性 (`application.properties`)

```properties
spring.application.name=Spring_Batch-2

## 資料來源配置
spring.datasource.url=jdbc:postgresql://localhost:5432/test
spring.datasource.username=postgres
spring.datasource.password=postgres

# Hibernate 配置
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto = update
spring.jpa.show-sql=true

# Spring Batch Schema 初始化 - 重點關注項目
spring.sql.init.schema-locations = classpath:org/springframework/batch/core/schema-postgresql.sql
spring.sql.init.mode = never
spring.batch.jdbc.initialize-schema=always
spring.batch.job.enable = false

Server.port=8888
```

### 2. 批次配置 (`HelloBatchConfig.java`)

配置類別定義：
- **交易管理器**：用於資料庫交易的 `DataSourceTransactionManager`
- **步驟**：簡單的 tasklet，印出 "Hello world2!"
- **作業**：將步驟組合成完整的批次作業

## 資料庫 Schema 配置 - 核心學習重點

### Schema 初始化屬性詳解

1. **`spring.sql.init.schema-locations`**
   - 指向 Spring Batch PostgreSQL schema 檔案
   - 位置：`classpath:org/springframework/batch/core/schema-postgresql.sql`
   - 此檔案包含建立 Spring Batch 元數據表的 DDL 語句

2. **`spring.sql.init.mode = never`**
   - 停用透過 `spring.sql.init` 的自動 schema 初始化
   - 改為依賴 Spring Batch 自己的初始化機制

3. **`spring.batch.jdbc.initialize-schema=always`**
   - 強制 Spring Batch 初始化其元數據表
   - 建立表格：`BATCH_JOB_INSTANCE`、`BATCH_JOB_EXECUTION`、`BATCH_STEP_EXECUTION` 等

### Spring Batch 元數據表

應用程式啟動時，會自動建立以下表格：

- `BATCH_JOB_INSTANCE` - 儲存作業實例資訊
- `BATCH_JOB_EXECUTION` - 儲存作業執行詳細資料
- `BATCH_JOB_EXECUTION_PARAMS` - 儲存作業參數
- `BATCH_STEP_EXECUTION` - 儲存步驟執行資訊
- `BATCH_JOB_EXECUTION_CONTEXT` - 儲存作業執行上下文
- `BATCH_STEP_EXECUTION_CONTEXT` - 儲存步驟執行上下文

## 前置需求

### 資料庫設定
1. 安裝 PostgreSQL
2. 建立名為 `test` 的資料庫
3. 確保 PostgreSQL 在 `localhost:5432` 上運行
4. 建立使用者 `postgres`，密碼為 `postgres`（或修改配置中的憑證）

### 相依性
專案使用 Spring Boot 3.4.4，包含：
- `spring-boot-starter-batch` - 核心 Spring Batch 功能
- `spring-boot-starter-data-jpa` - JPA 整合
- `spring-boot-starter-data-jdbc` - JDBC 支援
- `postgresql` - PostgreSQL 驅動程式

## 執行應用程式

1. 啟動 PostgreSQL 資料庫
2. 執行 Spring Boot 應用程式：
   ```bash
   ./gradlew bootRun
   ```
3. 應用程式將會：
   - 初始化 Spring Batch 元數據表
   - 在 8888 埠啟動
   - 作業執行預設為停用（`spring.batch.job.enable = false`）

## 測試批次作業

要啟用並執行批次作業，可以：
1. 在屬性中設定 `spring.batch.job.enable = true`
2. 或透過 REST 端點以程式方式觸發作業

## 核心學習成果

此專案示範：
1. **資料庫整合**：Spring Batch 如何與 PostgreSQL 整合
2. **Schema 管理**：Spring Batch 元數據表的自動建立
3. **交易管理**：資料庫交易的正確配置
4. **配置分離**：資料庫與應用程式配置關注點的分離

## 常見問題與解決方案

### Schema 初始化問題
- 確保 PostgreSQL 驅動程式在 classpath 中
- 驗證資料庫連線性
- 檢查 `spring.batch.jdbc.initialize-schema` 設定是否正確

### 連線問題
- 驗證 PostgreSQL 是否正在運行
- 檢查資料庫名稱、使用者名稱和密碼
- 確保防火牆/安全設定允許連線

## 下一步學習方向

- 探索不同的 schema 初始化模式
- 實作更複雜的批次作業，包含資料庫讀取器/寫入器
- 新增監控和管理端點
- 實作自訂批次監聽器進行資料庫操作

## 參考資料

- [Spring Batch 參考文件](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [Spring Boot 資料庫初始化](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization)
- [PostgreSQL Spring Boot 整合](https://spring.io/guides/gs/accessing-data-postgresql/)

## 配置重點說明

### 關鍵配置項目解析

**為什麼使用 `spring.sql.init.mode = never`？**
- 避免與 Spring Batch 自己的初始化機制衝突
- Spring Batch 有專門的初始化邏輯，更適合處理批次相關的表格

**`spring.batch.jdbc.initialize-schema=always` 的作用**
- 確保每次應用程式啟動時都檢查並建立必要的 Spring Batch 表格
- 在開發環境中特別有用，確保資料庫結構正確

**Schema 檔案位置的重要性**
- `classpath:org/springframework/batch/core/schema-postgresql.sql` 是 Spring Batch 提供的標準 PostgreSQL schema 檔案
- 包含了所有 Spring Batch 運作所需的表格定義和索引

這個配置組合確保了 Spring Batch 能夠正確地管理其元數據，同時避免了初始化衝突。