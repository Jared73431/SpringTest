# Spring Batch 4 - 書店批次：多個 Job、Reader / Writer、Skip / Retry、重新執行

一家書店的三個批次，透過 REST API 依名稱啟動、查詢執行狀態：

| Job | 做什麼 | 識別參數 | 重點 |
|---|---|---|---|
| `bookReportJob` | 讀取所有書籍 → 分類 → 寫成 CSV 報表 | `timestamp`（自動） | Paging Reader、每次執行一個新檔案 |
| `priceUpdateJob` | 套用價格更新檔 → 再匯出新的報表 | `input.file` | Skip、Retry、條件流程、從中斷處繼續 |
| `orderShippingJob` | 把待出貨的訂單改成已出貨 | `timestamp`（自動） | 狀態欄位 + 時間戳：冪等的資料庫更新 |

另外有 6 課學習測試。

> 本模組由原本的 `Spring_Batch-4`、`Spring_Batch-5`、`Spring_Batch-6` **合併**而成。三個模組大量重疊（5 = 4 去掉一個 Runner；6 = 5 加上 Book），而且同一個專案裡混用了三種 Reader / Writer 寫法。合併前的程式碼、Batch-6 的 baseline 測試與升級紀錄保留在 Git 歷史中，詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-batch-jdbc`、`spring-boot-starter-webmvc`） |
| Spring Batch | 6.0 |
| 資料庫 | PostgreSQL（資料庫名稱 `Batch4`），Flyway 建立 `BATCH_*`、`book`、`book_order` |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、MockMvc、Mockito、Awaitility、Testcontainers 2.0（PostgreSQL）；JPA 只在第 1 課使用 |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_ReaderTest](src/test/java/com/example/demo/lesson/L01_ReaderTest.java) | FlatFile、JdbcCursor、JdbcPaging、JpaPaging、Repository 五種 Reader |
| 2 | [L02_WriterTest](src/test/java/com/example/demo/lesson/L02_WriterTest.java) | `JdbcBatchItemWriter` 的 `assertUpdates`；`@StepScope` 讓每次執行寫一個新檔案 |
| 3 | [L03_MultipleJobsTest](src/test/java/com/example/demo/lesson/L03_MultipleJobsTest.java) | 多個 Job 用名稱區分（REST、命令列 `spring.batch.job.name`）；條件流程 |
| 4 | [L04_SkipRetryTest](src/test/java/com/example/demo/lesson/L04_SkipRetryTest.java) | Skip 壞資料並記錄原因；Retry 暫時性錯誤；其他錯誤直接失敗 |
| 5 | [L05_RestartTest](src/test/java/com/example/demo/lesson/L05_RestartTest.java) | 失敗後用同一個檔案再執行，從最後 commit 的 chunk 繼續 |
| 6 | [L06_StatusFlagTest](src/test/java/com/example/demo/lesson/L06_StatusFlagTest.java) | 狀態欄位 + 時間戳；**OFFSET 分頁的 Reader 會漏資料** |
| — | [JobApiTest](src/test/java/com/example/demo/JobApiTest.java) | REST API |

## 專案結構

一個 Job 一個 package、一個 `XxxJobConfig`：

```text
src/main/java/com/example/demo/
├── book/
│   ├── BookReportJobConfig        bookReportJob：Reader、Writer、Step、Job
│   ├── BookReportProcessor        依出版年份分類（有商業邏輯 → 獨立類別）
│   └── Book、BookReportLine       record
├── price/
│   ├── PriceUpdateJobConfig       priceUpdateJob：skip、retry、條件流程
│   ├── PriceUpdateProcessor       檢查 ISBN 是否存在
│   ├── PriceUpdateSkipListener    記錄被跳過的行與原因
│   └── PriceUpdate、UnknownIsbnException
├── order/
│   ├── OrderShippingJobConfig     orderShippingJob：只讀 PENDING、更新成 SHIPPED
│   └── BookOrder
├── common/TimestampIncrementer    每次執行加上 timestamp
└── launch/
    ├── JobController              /api/jobs
    ├── JobLaunchService           依名稱找 Job、在背景執行
    ├── JobRegistryConfig          MapJobRegistry
    ├── JobExecutionView           回應用的 record
    └── JobExceptionHandler        ProblemDetail

src/main/resources/
├── db/migration/                  V1 BATCH_*、V2 book（8 本書）、V3 book_order（10 筆待出貨）
└── price-updates/price-update-2026-10-12.csv   刻意包含一行格式錯誤、一個不存在的 ISBN
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers），不會連到本機的資料庫。

```bash
./gradlew test
```

### Docker Compose

```bash
docker compose up --build -d
# Windows 保留了 8104 時：APP_PORT=8304 docker compose up --build -d

curl http://localhost:8104/api/jobs
curl -X POST http://localhost:8104/api/jobs/orderShippingJob/executions
curl -X POST http://localhost:8104/api/jobs/priceUpdateJob/executions \
     -H "Content-Type: application/json" \
     -d '{"input.file":"classpath:price-updates/price-update-2026-10-12.csv"}'
curl http://localhost:8104/api/jobs/executions/2

docker compose exec app ls /app/output       # 書籍報表
docker compose logs app | grep Skipped       # 被跳過的行
docker compose down -v                       # 刪除資料庫與報表
```

pgAdmin（http://localhost:5050）：`docker compose --profile tools up -d pgadmin`

### 本機執行

需要本機的 PostgreSQL 有 `Batch4` 資料庫，表由 Flyway 自動建立。報表寫到 `output/`（可用 `REPORT_DIRECTORY` 修改）。

```bash
./gradlew bootRun
```

## API

| 方法 | 路徑 | 說明 | 回應 |
|---|---|---|---|
| GET | `/api/jobs` | 所有 Job 的名稱 | 200 |
| POST | `/api/jobs/{jobName}/executions` | 在背景啟動；body 是 JobParameters（可省略） | 202 + `Location` |
| GET | `/api/jobs/executions/{id}` | 執行狀態與每個 Step 的讀、寫、跳過筆數 | 200 / 404 |

| 錯誤 | 狀態碼 |
|---|---|
| 沒有這個 Job | 404 |
| 缺少必要的參數（`priceUpdateJob` 沒有 `input.file`） | 400 |
| 這組參數已經完成 / 正在執行 | 409 |

沒有 body、而且 Job 有 incrementer（`bookReportJob`、`orderShippingJob`）時，自動加上 `timestamp`。

## 批次教學

### 1. 統一的寫法：Reader、Processor、Writer 放在哪裡？

這是 Batch-5 當初想解決的問題。網路上的教學有好幾種寫法，合併前的專案三種都有：

```java
// 寫法 1：@Component 包一個 createReader()，再由 @Bean 呼叫（PersonItemReader、HealthInsuranceItemReader）
@Component
public class PersonItemReader {
    public FlatFileItemReader<PersonDTO> createReader() { return new FlatFileItemReaderBuilder<>()...build(); }
}
@Bean public FlatFileItemReader<PersonDTO> personReader() { return personItemReader.createReader(); }

// 寫法 2：直接實作介面（PersonItemProcessor、BookItemWriter）
@Component
public class BookItemWriter implements ItemWriter<BookDTO> { ... }

// 寫法 3：在 @Configuration 裡用 Builder 建立（Batch-3、Batch-4 註解掉的舊版）
@Bean public FlatFileItemReader<PersonDTO> reader() { return new FlatFileItemReaderBuilder<>()...build(); }
```

本模組的規則：

| 元件 | 寫法 | 例子 |
|---|---|---|
| 現成的 Reader / Writer（讀檔、讀資料庫、寫資料庫、寫檔） | 在該 Job 的 `XxxJobConfig` 用 **Builder** 建立成 `@Bean` | `bookReader`、`priceUpdateWriter` |
| 有**自己的商業邏輯** | 寫成實作 `ItemProcessor` 的類別，在 `XxxJobConfig` 宣告成 `@Bean` | `BookReportProcessor`、`PriceUpdateProcessor` |
| 需要這次執行的資訊（JobParameters、jobExecutionId） | 加上 `@StepScope` | `priceUpdateReader`、`bookReportWriter` |
| 沒有邏輯 | 不寫 Processor | `orderShippingStep` |

為什麼：

- **寫法 1 多了一層沒有作用的包裝**：`createReader()` 每次呼叫都建立新物件，`@Component` 本身不做任何事。要看一個 Job 用了什麼，得在三個檔案之間跳來跳去。
- **寫法 2 適合有邏輯的元件**：如果 Reader / Writer 只是設定（讀哪個檔、寫哪張表），寫成類別只會把設定藏起來。
- **一個 Job 一個 Config**：打開 `PriceUpdateJobConfig` 就能看到這個 Job 的全部：讀什麼、怎麼處理、寫到哪、出錯怎麼辦、流程怎麼走。

### 2. Reader 怎麼選（第 1 課）

| Reader | 怎麼讀 | 適合 |
|---|---|---|
| `FlatFileItemReader` | 一次讀一行，記住讀到第幾行 | 檔案 |
| `JdbcCursorItemReader` | 一個查詢，用 cursor 往下讀；連線一直開著 | 大量資料、處理時間短 |
| `JdbcPagingItemReader` | 每次查一頁，下一頁用 `sortKey > 上一頁最後一筆` | **資料庫的預設選擇** |
| `JpaPagingItemReader` | 每次查一頁（JPQL，**OFFSET** 分頁），回傳 Entity | 已經有 Entity 和 JPQL |
| `RepositoryItemReader` | 呼叫 Repository 的分頁方法 | 已經有 Spring Data Repository |

`RepositoryItemReader` 呼叫的方法，最後一個參數必須是 `Pageable`，回傳 `Page`。實測：它會在參數最後加上 `PageRequest` 去找方法。修正前的 `findBooksAfterYear(Integer)` 回傳 `List`，執行時會 `NoSuchMethodException`；只是那個 Bean 沒有被任何 Step 使用，所以從未發現。

### 3. Writer（第 2 課）

- **`JdbcBatchItemWriter`**：一個 chunk 的 SQL 用 JDBC batch 一次送出。預設 `assertUpdates = true`：任何一句 UPDATE 沒有更新到資料就拋出 `EmptyResultDataAccessException`，不會默默略過。所以 `priceUpdateJob` 先在 Processor 檢查 ISBN。
- **`FlatFileItemWriter` + `@StepScope`**：修正前 Writer 是 singleton，檔名的時間戳在**應用程式啟動時**就決定了，之後每次執行都覆蓋同一個檔案（baseline 測試已確認）。加上 `@StepScope` 後，每次執行 Step 才建立 Writer，檔名用這次的 `jobExecutionId`。

### 4. 多個 Job 與流程（第 3 課）

**用名稱區分**：每個 Job 有自己的名稱。

| 方式 | 指定 Job |
|---|---|
| REST | `POST /api/jobs/{jobName}/executions`，用 `JobRegistry` 依名稱找 Job |
| 命令列 | `--spring.batch.job.name=orderShippingJob` |

啟動時自動執行（`spring.batch.job.enabled=true`），而且容器裡有多個 Job 時，Spring Boot 要求指定名稱，否則啟動失敗：`Job name must be specified in case of multiple jobs`。本模組是 web 應用程式，所以設定 `enabled=false`，由 API 啟動。

**[Learning] Boot 4 沒有自動建立 `JobRegistry`**：實測注入時找不到 Bean。Spring Batch 6 的 `MapJobRegistry` 宣告成 Bean 後，會自動註冊容器中的每一個 Job，不需要再設定 `JobRegistryBeanPostProcessor`。

**條件流程**：Step 結束時的 **exit status** 決定下一步。

```java
.start(priceUpdateStep)
    .on("FAILED").fail()
.from(priceUpdateStep).on("COMPLETED WITH SKIPS").to(skipNoticeStep).next(bookReportStep)
.from(priceUpdateStep).on("*").to(bookReportStep)
.end()
```

`COMPLETED WITH SKIPS` 不是內建的狀態，是 `SkipCountExitStatusListener` 在「完成但有跳過的行」時設定的 exit status。

注意 `.on("FAILED").fail()` 必須寫：只寫 `.on("*")` 的話，Step 失敗時也會走到報表，Job 顯示成功。

### 5. Skip 與 Retry（第 4 課）

| | 什麼時候用 | `priceUpdateStep` 的設定 |
|---|---|---|
| **Skip** | 這一筆有問題，重試也不會好 | 格式錯誤（`FlatFileParseException`）、ISBN 不存在，最多 10 筆 |
| **Retry** | 暫時性錯誤，再試一次可能就好 | `TransientDataAccessException`（deadlock、逾時），最多 3 次 |
| 都不是 | 需要人處理的錯誤 | Step 失敗 |

**跳過不是忽略**：`PriceUpdateSkipListener` 記錄每一筆跳過的資料與原因（`Skipped line 4 [9780321356680,abc]`），`BATCH_STEP_EXECUTION` 也記錄了 read / process / write 各跳過幾筆。

實測：

- 第一次寫入遇到 deadlock 時重試一次，Job 完成，Writer 被呼叫 2 次。
- 違反資料庫限制（`DataIntegrityViolationException`）時不重試，Step 直接失敗，Writer 只呼叫 1 次。

### 6. 從中斷處繼續 vs 狀態欄位（第 5、6 課）

失敗之後怎麼辦，取決於「處理進度」記錄在哪裡：

| | `priceUpdateJob`（第 5 課） | `orderShippingJob`（第 6 課） |
|---|---|---|
| 處理進度記錄在 | Reader 記住「讀到第幾行」（`BATCH_STEP_EXECUTION_CONTEXT`） | **資料本身**（`status`） |
| 識別參數 | `input.file`（業務參數） | `timestamp` |
| 失敗後 | 用**同一個檔案**再執行 → 同一個 JobInstance → 從最後 commit 的 chunk 繼續 | **再執行一次**（新的 JobInstance）→ 只讀得到剩下的 `PENDING` 訂單 |
| 重複執行 | 同一個檔案做完就不能再做（409） | 隨時都能執行，沒有待出貨的訂單時就讀 0 筆 |

實測：價格更新檔 6 行、chunk = 2，第 2 個 chunk 寫入時資料庫斷線。用同一個檔案再執行時，只讀了剩下的 4 行。

這就是 [Spring_Batch-3](../Spring_Batch-3) 提到的「時間戳 vs 業務參數」：**Job 是冪等的**（處理過的資料不會再被處理）時，用時間戳最方便。

**[Potential Bug] OFFSET 分頁的 Reader 會漏資料**：用狀態欄位篩選的 Job，Reader 不能用 OFFSET 分頁。實測：用 `JpaPagingItemReader`（pageSize 3）讀 `status = 'PENDING'`，Writer 同時把 status 改掉：

```text
第 1 頁 OFFSET 0：訂單 1、2、3 → 出貨
第 2 頁 OFFSET 3：剩下的 4～10 先跳過 3 筆 → 讀到 7、8、9 → 出貨
第 3 頁 OFFSET 6：剩下的 4、5、6、10 只有 4 筆 → 讀不到 → 結束
```

Job 顯示 **COMPLETED**，卻漏了 4 筆訂單。`JdbcPagingItemReader` 用 `id > 上一頁最後一筆` 查下一頁，不受影響，所以 10 筆都會處理。

### 7. 在背景執行

`POST /api/jobs/{jobName}/executions` 立刻回應 202，Job 在背景執行，再用 `Location` 查詢狀態。

- **修正前**：在 Controller 中同步執行，HTTP 請求要等 Job 做完才回應，Job 一久就逾時。Batch-4 原本的 `AsyncJobLauncherService` 用了 `@Async`，但沒有 `@EnableAsync`，所以其實也是同步執行。
- **現在**：`JobLaunchService` 用 `TaskExecutorJobOperator` 搭配 Spring Boot 的 `applicationTaskExecutor`。這個非同步的 JobOperator **不宣告成 Bean**，因為宣告成 Bean 會取代 Spring Boot 的 JobOperator，連測試裡啟動的 Job 都會變成非同步。

參數檢查（缺少 `input.file`），以及「這組參數已經完成」的檢查，都在送到背景**之前**執行，所以能直接回應 400 / 409。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Spring_Batch-6 的 baseline 測試（Testcontainers）：啟動時的 8 個 Job、各 Reader 的查詢結果、GET 同步執行、CSV 覆蓋同一個檔案 |
| 2 | Spring_Batch-6：Java 17 → 21、Spring Boot 3.5.3 → 3.5.16 → 4.1.1、Gradle → 9.8.0，baseline 全部通過 |
| 3 | 在 Spring_Batch-4 重建：書店的三個 Job、統一寫法、REST API、Flyway |
| 4 | 第 1～6 課 |
| 5 | 刪除 Spring_Batch-5、Spring_Batch-6 |
| 6 | Docker |

### 合併前三個模組的內容

| 模組 | 內容 | 去向 |
|---|---|---|
| Spring_Batch-4 | Person（CSV 匯入）、健保扣款檔（CSV 匯入）、Hello 三個 Job；`MultiJobRunner` 用 `@Async` 啟動 | Person 與 Hello 已在 Batch-3、Batch-1；健保扣款檔換成價格更新檔；改用 REST 啟動 |
| Spring_Batch-5 | Batch-4 去掉 `MultiJobRunner`，每個 Job 各自排程 | 排程在 Spring_Scheduling、Spring_Quartz |
| Spring_Batch-6 | Batch-5 + Book：三種 Reader、CSV 輸出、綜合流程、條件流程、REST 觸發 | Reader 比較 → 第 1 課；CSV 輸出 → `bookReportJob`；條件流程 → `priceUpdateJob` |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| 三種 Reader / Writer 寫法混用 | 見「批次教學」第 1 點 |
| 用 GET 觸發 Job、同步執行 | `/api/batch/books/jpa` 等；GET 不應該改變狀態，同步執行一久就逾時 |
| `@Async` 沒有作用 | 沒有 `@EnableAsync` |
| CSV 每次都覆蓋同一個檔案 | singleton Writer，檔名在啟動時決定 |
| 從未使用的 Bean | `bookRepositoryReaderCustom`（執行時一定失敗）、`bookJdbcReaderComplex`、`AsyncJobLauncherService`（Batch-6） |
| 排程中的 `Thread.sleep(30000)` | 每日報表依序執行三個 Job，中間各等 30 秒；應該改成一個 Job 的多個 Step |
| 啟動時執行所有 Job | 8 個 `@Scheduled(fixedRate)` 都沒有 `initialDelay`，加上時間戳參數，每次啟動都重新匯入 |
| `book` 表沒有建立機制 | 讀的是其他模組留下的表；Entity 欄位叫 `Id`、`ISBN`（大寫） |
| 依賴資料庫中已經存在的 `BATCH_*` 表 | `initialize-schema=never` |
| 健保扣款檔 | 檔案格式接近銀行實際的扣款檔，不適合放在公開的練習專案 |
| 其他 | 根目錄有一個 224 行、不會被編譯的 `batch_scheduler_architecture.java`；`JobCompleteion` 拼錯；簡體中文註解；DEBUG log；測試連到本機資料庫 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 模組 | 3 個（Batch-4、5、6） | 1 個 |
| Job | Person、健保、Hello、Book × 5 | `bookReportJob`、`priceUpdateJob`、`orderShippingJob` |
| 觸發 | 啟動時與每隔幾分鐘自動執行；GET 同步執行 | `POST /api/jobs/{jobName}/executions` 在背景執行 |
| 資料庫 | `test` | `Batch4` |
| 建表 | JPA `ddl-auto=update`；`BATCH_*`、`book` 沒有 | Flyway |
| Port | 8015 | 8104 |
| 報表 | `output/processed_books_{啟動時間}.csv`（覆蓋） | `{REPORT_DIRECTORY}/book-report-{jobExecutionId}.csv` |

### 升級時學到的事

- **`JobExecution.getJobId()` 在 Spring Batch 6 被移除**：它其實回傳的是 JobInstance 的 id，改用 `getJobInstance().getId()`。
- **`JdbcPagingItemReaderBuilder.build()` 宣告 `throws Exception`**：它要從 DataSource 判斷資料庫種類，產生分頁 SQL。
- **`JobRepository.getJobExecution(不存在的 id)`**：Spring Batch 6 的 JDBC 實作不是回傳 null，而是拋出 `EmptyResultDataAccessException`。
- **Boot 4 的 `TestRestTemplate`**：移到 `spring-boot-resttestclient`，需要 `@AutoConfigureTestRestTemplate`。
- **新的 Retry**：Spring Batch 6 的 `retry(...)` 使用 Spring Framework 7 的 `org.springframework.core.retry`。
