# Spring Batch 3 - Chunk：CSV 匯入資料庫

參考 Spring 官方教學「Creating a Batch Service」做出的第一個「讀 → 處理 → 寫」批次：讀 `sample-data.csv`，把姓名轉成大寫，寫進 `person` 表。

```text
importPersonJob
 └─ importPersonStep（chunk = 3）
     Reader：FlatFileItemReader      一次讀一行 → PersonDTO
     Processor：PersonItemProcessor  一次處理一筆 → Person（大寫）
     Writer：JdbcBatchItemWriter     一次寫一整個 chunk，寫完 commit
```

這個模組是「**執行一次、觀察結果**」的練習：啟動一次就匯入一次，做完程式結束。為什麼不能再執行一次，見「[為什麼不能重新執行](#為什麼不能重新執行)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-batch-jdbc`） |
| Spring Batch | 6.0 |
| 資料庫 | PostgreSQL（資料庫名稱 `Batch3`），Flyway 建立 `BATCH_*` 表與 `person` 表 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_ChunkTest](src/test/java/com/example/demo/lesson/L01_ChunkTest.java) | chunk 大小決定 commit 次數；失敗時只 rollback 當下的 chunk |
| — | [SpringBatch3ApplicationTests](src/test/java/com/example/demo/SpringBatch3ApplicationTests.java) | 匯入結果；第二次執行被拒絕 |

Skip（跳過格式錯誤的行）、Retry、失敗後從中斷處繼續，在 Spring_Batch-4。

## 專案結構

```text
src/main/java/com/example/demo/
├── SpringBatch3Application                main：做完就結束，exit code = Job 結果
└── batch/
    ├── config/BatchConfiguration          Reader、Processor、Writer、Step、Job
    ├── dto/PersonDTO                      CSV 的一行（Reader 的輸出）
    ├── model/Person                       person 表的一列（Processor 的輸出）
    ├── processor/PersonItemProcessor      姓名轉大寫
    └── listener/JobCompletionNotificationListener   Job 結束後印出讀寫筆數

src/main/resources/
├── sample-data.csv
└── db/migration/
    ├── V1__batch_tables.sql               Spring Batch 6.0.5 內附的建表 SQL
    └── V2__person.sql
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers），不會連到本機的資料庫。每個測試開始前都會清空資料，可以重複執行。

```bash
./gradlew test
```

### Docker Compose

```bash
docker compose run --rm app        # 匯入 4 筆，exit code 0
docker compose exec postgres psql -U postgres -d Batch3 -c "select * from person"
docker compose run --rm app        # 再執行一次：被拒絕，exit code 1
docker compose down -v             # 刪除資料庫，重新開始
```

pgAdmin（http://localhost:5050）：`docker compose --profile tools up -d pgadmin`

### 本機執行

需要本機的 PostgreSQL 有 `Batch3` 資料庫，表由 Flyway 自動建立。

```bash
./gradlew bootRun
```

## 批次教學

### 1. Chunk

Chunk 是 Spring Batch 處理大量資料的方式：**一筆一筆讀、一筆一筆處理，累積到 chunk 大小才一次寫入，並在同一個交易中 commit**。

```text
chunk = 3，CSV 有 4 行：
  交易 1：read Jill、Joe、Justin → process ×3 → write 3 筆 → commit
  交易 2：read Jane、(結束)      → process ×1 → write 1 筆 → commit
```

| chunk 大小（4 筆資料） | 1 | 2 | 3 | 4 | 10 |
|---|---|---|---|---|---|
| commit 次數 | 4 | 2 | 2 | 1 | 1 |

chunk 大小是取捨：

- **越小**：commit 越多次，每次都要和資料庫來回，速度越慢。
- **越大**：失敗時 rollback 的範圍越大，也要在記憶體中保留越多筆資料。

實務上常從 100～1000 開始，依資料量和失敗的影響調整。

**失敗時只 rollback 當下的 chunk**：寫入第 2 個 chunk 時失敗，第 1 個 chunk 的 3 筆已經 commit，會留在資料庫。這是 chunk 的設計：已經完成的部分不必重做。但這也表示 Job 失敗時，資料庫裡可能有「一部分」的資料，重新執行時要考慮這件事（見 Spring_Batch-4）。

**[Learning] Spring Batch 6 的新實作**：`chunk(size, transactionManager)` 已標記為將移除，改用 `chunk(size).transactionManager(...)`，建立的是新的 `ChunkOrientedStep`。實測兩者的 commit 次數有差異：chunk 大小剛好整除資料筆數時（chunk = 1、2、4），舊實作最後會多一個「讀到檔案結尾、沒有資料」的空 chunk，也 commit 一次，分別是 5、3、2 次。

### 2. Reader、Processor、Writer 的型別

```java
.<PersonDTO, Person>chunk(3)
```

- Reader 輸出 `PersonDTO`，代表 CSV 的一行。
- Processor 把 `PersonDTO` 轉成 `Person`，代表資料庫的一列。
- Writer 接收 `Person`。

輸入和輸出的型別可以不同。Processor 回傳 `null` 表示「略過這一筆」，那一筆不會傳給 Writer。

### 3. JdbcBatchItemWriter vs JPA

| | 修正前：`RepositoryItemWriter` + JPA `save` | 修正後：`JdbcBatchItemWriter` |
|---|---|---|
| 寫入方式 | 一筆一個 `INSERT`，經過 JPA 的 persistence context | 一個 chunk 的 `INSERT` 用 JDBC batch 一次送出 |
| 需要 | Entity、Repository、Hibernate、`ddl-auto` 建表 | 一個 record 和一句 SQL |
| 適合 | 寫入時需要 JPA 的關聯、生命週期回呼 | 大量 insert / update |

`beanMapped()` 依照 `:firstName`、`:lastName` 的名稱，從 record 的 `firstName()`、`lastName()` 取值。

### 為什麼不能重新執行

Job 沒有任何 JobParameters，所以**每次都是同一個 JobInstance**（見 Spring_Batch-2 第 2 課）：

```text
第 1 次：importPersonJob + {} → 新的 JobInstance → 匯入 4 筆 → COMPLETED
第 2 次：importPersonJob + {} → 同一個 JobInstance，已經 COMPLETED → JobInstanceAlreadyCompleteException
```

這其實是保護機制：CSV 匯入**不是冪等的**，每做一次就多 4 筆。如果允許重新執行，資料就會重複。

**要重新觀察時，person 表和執行紀錄要一起清掉**：

- 只刪 `person` 表時，執行紀錄仍然記得「importPersonJob 已經完成」，還是會被拒絕。
- 只刪執行紀錄時，再匯入一次會讓資料重複。

所以 Docker 用 `docker compose down -v` 整個重來；測試則在每個測試開始前，把兩邊一起清空。

### 想要能重複執行：用時間戳區分

修正前，排程每次執行時加上 `time=System.currentTimeMillis()` 這個參數：

```java
new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters();
```

參數每次都不同，所以每次都是新的 JobInstance，**一定可以執行**。這是實務上常見的做法，取捨如下：

| | 時間戳 | 業務參數（例如日期、檔案名稱） |
|---|---|---|
| 重複執行 | 一定可以 | 同一組參數做完後會被拒絕 |
| 失敗之後 | 下一次是全新的執行，從頭開始 | 同一組參數再執行，從失敗處繼續 |
| 適合 | **Job 本身是冪等的**：重做幾次結果都一樣 | Job 不能做兩次，例如結算、匯入 |

關鍵在於 **Job 是不是冪等的**。例如資料庫批次更新：Reader 只讀「狀態 = 待處理」的資料，Writer 更新資料時一併改掉狀態。這樣不論執行幾次、在哪裡中斷，下一次都只會處理剩下的部分，用時間戳區分每次執行就沒有問題。

本模組的 CSV 匯入不是冪等的，修正前加上時間戳後，排程每 5 分鐘就把同一份 CSV 再匯入一次。時間戳搭配冪等 Job 的實作在 Spring_Batch-4。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試（Testcontainers）：排程匯入、每次執行都重複匯入 |
| 2 | Java 17 → 21、Spring Boot 3.4.4 → 3.5.16、Gradle 8.13 → 8.14.3 |
| 3 | Spring Boot 3.5.16 → 4.1.1、Gradle 8.14.3 → 9.8.0 |
| 4 | 重構：執行一次、`JdbcBatchItemWriter`、Flyway |
| 5 | 第 1 課 |
| 6 | Docker |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| 每 5 分鐘重複匯入同一份 CSV | `@Scheduled(fixedRate = 300000)` + 時間戳參數，匯入又不是冪等的：4 筆 → 8 筆 → 12 筆… |
| CSV 有一行格式錯誤時 | 實測：第 1 個 chunk 已經 commit、Job FAILED；下一次排程從頭再匯入，又在同一行失敗。重複資料不斷增加，錯誤行之後的資料永遠不會匯入 |
| 依賴資料庫中已經存在的 `BATCH_*` 表 | `initialize-schema=never`，表要先由別的模組建好 |
| 每次 Job 結束都 `findAll()` | 把整張表讀出來逐筆印出，資料越多越慢 |
| 多餘的設定 | 自訂 `JpaTransactionManager` Bean；`spring.sql.init.*` 沒有作用（`mode=never`）；batch 與 jdbc 的 log 層級為 DEBUG |
| 用不到的依賴 | data-jdbc、web、devtools；`SpringBatch3Application` 有十幾個沒有使用的 import |
| 其他 | 類別名稱拼錯（`JobCompleteion`）；簡體中文註解；JPA Entity 使用 Lombok `@Data`（equals / hashCode 包含會變動的 id）；測試連到本機資料庫 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 觸發 | 啟動後立刻執行，之後每 5 分鐘一次 | 啟動時執行一次，做完程式結束 |
| JobParameters | `time=時間戳` | 沒有參數 |
| 第二次執行 | 再匯入一次（資料重複） | 被拒絕，exit code 1 |
| 資料庫 | `test` | `Batch3` |
| 建表 | `person`：JPA `ddl-auto=update`；`BATCH_*`：沒有 | Flyway |
| Job / Step 名稱 | `importUserJob4` / `step14` | `importPersonJob` / `importPersonStep` |
| Port | 8000 | 無 |

### 升級時學到的事

- **Spring Batch 6 有大量 API 即將移除**：`JobLauncher` 改用 `JobOperator`；`chunk(size, transactionManager)` 改用 `chunk(size).transactionManager(...)`，而且兩者的 commit 次數不同（見上方第 1 點）。
- **探測測試可以用測試的 classpath 蓋掉檔案**：在 `src/test/resources` 放一份同名的 `sample-data.csv`，就能在不修改主程式的情況下，觀察 CSV 有錯誤行時的行為。
