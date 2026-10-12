# Spring Batch 2 - 批次的資料庫

[Spring_Batch](../Spring_Batch) 的執行紀錄存在 H2 記憶體資料庫，程式結束就消失。這個模組把 JobRepository 存進 **PostgreSQL**，觀察 Spring Batch 如何用資料庫記錄每一次執行，以及這些紀錄如何決定「這次能不能執行」：

- 6 張 `BATCH_*` 表各記錄了什麼
- **JobInstance** 怎麼識別：同一天的資料為什麼不能處理兩次
- 失敗之後**重新執行**：為什麼會從失敗的 Step 繼續

主程式和 Spring_Batch 一樣只有一個 Tasklet，重點在資料庫。另外有 3 課學習測試。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-batch-jdbc`） |
| Spring Batch | 6.0 |
| 資料庫 | PostgreSQL（資料庫名稱 `Batch2`），Flyway 建立 `BATCH_*` 表 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_MetadataTablesTest](src/test/java/com/example/demo/lesson/L01_MetadataTablesTest.java) | 執行一次 Job，6 張 `BATCH_*` 表各記錄了什麼 |
| 2 | [L02_JobInstanceTest](src/test/java/com/example/demo/lesson/L02_JobInstanceTest.java) | JobInstance = Job 名稱 + 識別參數；重複執行被拒絕、非識別參數、`RunIdIncrementer` |
| 3 | [L03_RestartTest](src/test/java/com/example/demo/lesson/L03_RestartTest.java) | 失敗後用同一組參數再執行：沿用 JobInstance、跳過已完成的 Step |
| — | [SpringBatch2ApplicationTests](src/test/java/com/example/demo/SpringBatch2ApplicationTests.java) | 用 `java -jar` 的方式啟動：命令列參數 → JobParameters、exit code |

## 專案結構

```text
src/main/java/com/example/demo/
├── SpringBatch2Application          main：做完就結束，exit code = Job 結果
└── configuration/HelloBatchConfig   helloJob → helloStep（Tasklet）

src/main/resources/
├── application.properties
└── db/migration/V1__batch_tables.sql   Spring Batch 6.0.5 內附的 PostgreSQL 建表 SQL
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers），不會連到本機的資料庫。

```bash
./gradlew test
```

### Docker Compose

批次做完就結束，所以用 `docker compose run` 執行，後面的 `key=value` 會成為 JobParameters：

```bash
docker compose run --rm app date=2026-10-12    # 成功，exit code 0
docker compose run --rm app date=2026-10-12    # 同一天再執行：被拒絕，exit code 1
docker compose run --rm app date=2026-10-13    # 新的一天：新的 JobInstance
```

查詢執行紀錄：

```bash
docker compose exec postgres psql -U postgres -d Batch2 \
  -c "select job_instance_id, job_name, job_key from batch_job_instance"
```

或啟動 pgAdmin（http://localhost:5050）：`docker compose --profile tools up -d pgadmin`

停止：`docker compose down`（加上 `-v` 會刪除資料庫）

### 本機執行

需要本機的 PostgreSQL 有 `Batch2` 資料庫，`BATCH_*` 表由 Flyway 自動建立。

```bash
./gradlew bootRun --args="date=2026-10-12"
```

## 批次教學

### 1. 6 張 BATCH_* 表

```text
BATCH_JOB_INSTANCE              哪一個 Job + 哪一組識別參數：同一組參數只會有一列
 └─ BATCH_JOB_EXECUTION         每「執行一次」一列：狀態、開始與結束時間、exit code
     ├─ BATCH_JOB_EXECUTION_PARAMS     這次執行的參數（型別、是否用來識別 JobInstance）
     ├─ BATCH_JOB_EXECUTION_CONTEXT    Job 層級的 ExecutionContext
     └─ BATCH_STEP_EXECUTION           每個 Step 每執行一次一列：讀、寫、commit 次數、失敗原因
         └─ BATCH_STEP_EXECUTION_CONTEXT   Step 層級的 ExecutionContext（例如讀到第幾筆）
```

批次失敗時，先查 `BATCH_STEP_EXECUTION` 的 `status` 與 `exit_message`，就知道是哪個 Step、因為什麼失敗。

### 2. JobInstance 與 JobParameters

**JobInstance = Job 名稱 + 識別參數**。`BATCH_JOB_INSTANCE.job_key` 是識別參數的 MD5。

| 情況 | 結果 |
|---|---|
| 同一組識別參數，已經 COMPLETED | 拒絕執行：`JobInstanceAlreadyCompleteException` |
| 識別參數不同 | 新的 JobInstance |
| 只有非識別參數（identifying=false）不同 | 仍然是同一個 JobInstance，一樣被拒絕 |
| Job 設定了 `RunIdIncrementer` | 每次自動加上 `run.id`，每次都是新的 JobInstance |
| 同一組識別參數，上次 FAILED | **重新執行**（下一節） |

「拒絕重複執行」是保護機制：用**業務日期**當識別參數，同一天的資料就不會被處理兩次。像「把 10/12 的訂單結算」這種不能做兩次的批次，應該用日期；只有「重複執行也沒關係」的批次（例如清除快取）才適合 `RunIdIncrementer`。

**命令列參數的格式**（Spring Batch 5 之後）：

```bash
java -jar app.jar date=2026-10-12                       # String，參與識別
java -jar app.jar date=2026-10-12,java.time.LocalDate   # LocalDate，參與識別
java -jar app.jar triggeredBy=manual,java.lang.String,false   # 不參與識別
```

### 3. 失敗之後重新執行

Job 失敗時 JobInstance 沒有完成，用**同一組參數**再執行一次就是重新執行：

- 沿用同一個 JobInstance，新增一筆 JobExecution
- 已經 COMPLETED 的 Step **跳過**，從失敗的 Step 繼續

```text
第 1 次：prepare COMPLETED → process FAILED（資料庫斷線）
第 2 次：prepare 跳過     → process COMPLETED
```

所以 Step 要切成「做完就不必再做」的單位。例如先備份、再更新：更新失敗後重新執行，不會再備份一次。

### 4. 建表交給 Flyway

**修正前**：`spring.batch.jdbc.initialize-schema=always`，每次啟動都執行 Spring Batch 內附的建表 SQL。

**問題**：它只會「建表」，不會執行 jar 內附的**升級 SQL**（`org/springframework/batch/core/migration/<版本>/migration-postgresql.sql`）。

**[Potential Bug]** 以 Spring Batch 6 為例，它把 sequence `BATCH_JOB_SEQ` 改名為 `BATCH_JOB_INSTANCE_SEQ`。Batch 5 時期建立的資料庫如果只靠 `initialize-schema=always` 升級，會發生以下事情：

1. 建表 SQL 執行到 `CREATE TABLE` 時，因為表已經存在而失敗，但會被略過。
2. `CREATE SEQUENCE BATCH_JOB_INSTANCE_SEQ` 執行成功，產生一個**從 1 開始**的新 sequence。
3. 下一個 JobInstance 拿到 id 1，和舊資料重複：**duplicate key，程式無法啟動**。

上面的過程已用 Testcontainers 驗證：先建立 Batch 5 的表和一筆舊資料，再啟動 Boot 4 版本。修正前的設定連到本機的 `test` 資料庫時，就會遇到這個問題。

**現代做法**：

- `V1__batch_tables.sql` 直接複製**目前版本的 jar 內附**的 `schema-postgresql.sql`。這是官方來源，和程式使用的 Spring Batch 版本一定一致，不必從網路上找。
- 升級 Spring Batch 時，把 jar 內附的升級 SQL 加成 `V2__...sql`。
- Flyway 的 `flyway_schema_history` 會記錄資料庫目前是哪一版。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試（Testcontainers）：同一個資料庫啟動兩次 |
| 2 | Java 17 → 21、Spring Boot 3.4.4 → 3.5.16、Gradle 8.13 → 8.14.3 |
| 3 | Spring Boot 3.5.16 → 4.1.1、Gradle 8.14.3 → 9.8.0 |
| 4 | 重構：Flyway、做完就結束、移除用不到的依賴 |
| 5 | 第 1～3 課 |
| 6 | Docker |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| **升級 Spring Batch 時 duplicate key** | `initialize-schema=always` 不會執行升級 SQL，見上方第 4 點 |
| `spring.batch.job.enable=false` 沒有作用 | 拼錯了，正確是 `enabled`。Job 仍然在啟動時執行 |
| `spring.sql.init.schema-locations` 沒有作用 | 同一個檔案裡設定了 `spring.sql.init.mode=never` |
| 用不到的依賴 | JPA、data-jdbc、web、Lombok、devtools，以及 Hibernate 設定（沒有任何 Entity） |
| 多餘的 `DataSourceTransactionManager` Bean | Spring Boot 本來就會建立 |
| Job 失敗時 exit code 仍然是 0 | 見 Spring_Batch |
| 測試連到本機資料庫 | `contextLoads` 需要本機的 `localhost:5432/test` |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 資料庫 | `test` | `Batch2` |
| 建表 | `initialize-schema=always` | Flyway |
| 執行後 | web server 持續執行（port 8888） | 程式結束 |
| Job / Step 名稱 | `HelloJob8` / `helloStep8` | `helloJob` / `helloStep`（和 Bean 名稱一致） |
| 輸出 | `Hello world2!` | `Hello world2! jobParameters={...}` |
| 同一組參數第 2 次啟動 | 新增一筆 `NOOP` 的 JobExecution，程式正常啟動（Boot 3） | 程式啟動失敗（Spring Batch 6 的行為，見下方） |

### 升級時學到的事

- **Spring Batch 6 改變了重複執行的行為**：同一組參數的 JobInstance 已經 COMPLETED 時，Batch 5 會新增一筆 exit code 為 `NOOP` 的 JobExecution，Step 不執行，程式正常啟動；Batch 6 直接拋出 `JobInstanceAlreadyCompleteException`，程式啟動失敗，`BATCH_*` 表沒有新紀錄。baseline 測試在升級時抓到這個差異。
- **Boot 3.4 的 Testcontainers 版本**：Boot 3.4 管理的 Testcontainers 1.20 不支援 Docker 29。只在依賴寫 `1.21.4` 時，核心模組仍然被 BOM 降回 1.20.6，要用 `ext['testcontainers.version']` 覆蓋。Boot 3.5 之後不需要。
- **Testcontainers 2.0**：artifact 改名為 `testcontainers-postgresql`，類別改為 `org.testcontainers.postgresql.PostgreSQLContainer`（沒有泛型）。
