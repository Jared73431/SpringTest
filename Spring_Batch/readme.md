# Spring Batch - 最簡單的批次

用最少的程式碼觀察 Spring Batch 的基本結構：**一個 Job、一個 Step、一個 Tasklet**，啟動後印出 `Hello world!`，做完程式就結束。

```text
Job（HelloJob）
 └─ Step（helloStep2）
     └─ Tasklet：做一件事，回傳 RepeatStatus.FINISHED 表示做完了
```

這是 Batch 系列的第一個模組，刻意只保留最小的設定。讀檔、寫資料庫、多個 Job 等主題放在後面的模組：

| 模組 | 觀察的主題 |
|---|---|
| **Spring_Batch**（本模組） | 最簡單的 Job / Step / Tasklet、批次是「做完就結束」的程式 |
| [Spring_Batch-2](../Spring_Batch-2) | JobRepository 存進資料庫後，`BATCH_*` 表如何記錄執行 |
| [Spring_Batch-3](../Spring_Batch-3) | Chunk：讀 CSV → 處理 → 寫進資料庫 |
| [Spring_Batch-4](../Spring_Batch-4) | 多個 Job、Reader / Processor / Writer 的寫法 |

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-batch-jdbc`） |
| Spring Batch | 6.0（Spring Boot 管理） |
| 資料庫 | H2 記憶體資料庫（只用來存 JobRepository 的執行紀錄） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Spring Boot Test |

## 專案結構

```text
src/main/java/com/example/demo/
└── SpringBatchApplication      Step + Job 的 Bean 定義、main

src/test/java/com/example/demo/
└── SpringBatchApplicationTests  啟動時執行 HelloJob、exit code、不是 web 應用程式
```

## 執行方式

### 測試

不需要 Docker，也不需要資料庫。

```bash
./gradlew test
```

### 本機執行

```bash
./gradlew bootRun
```

輸出（節錄）：

```text
Hello world!
Job: [SimpleJob: [name=HelloJob]] completed with the following parameters: [{}] and the following status: [COMPLETED]
```

印完之後程式就**結束**，不會一直停在那裡。

### Docker

沒有資料庫（H2 在記憶體）、沒有 port，所以只有 Dockerfile，不需要 compose。

```bash
docker build -t spring-batch .
docker run --rm spring-batch
echo $?        # Job COMPLETED 為 0
```

## 批次教學

### 1. Job、Step、Tasklet

```java
@Bean
public Step sampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("helloStep2", jobRepository).tasklet((contribution, chunkContext) -> {
        System.out.println("Hello world!");
        return RepeatStatus.FINISHED;
    }, transactionManager).build();
}

@Bean
public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
    return new JobBuilder("HelloJob", jobRepository).start(sampleStep).build();
}
```

| 名詞 | 意義 |
|---|---|
| Job | 一個完整的批次工作，由一個或多個 Step 組成 |
| Step | Job 裡的一個階段；有 **Tasklet**（做一件事）和 **Chunk**（讀 → 處理 → 寫，見 Spring_Batch-3）兩種 |
| Tasklet | 回傳 `FINISHED` 表示做完；回傳 `CONTINUABLE` 會再被呼叫一次 |
| JobRepository | 記錄每次執行的結果（成功、失敗、執行到哪裡），所以 Spring Batch 一定要有資料庫 |
| TransactionManager | 每次呼叫 Tasklet 都包在一個交易裡 |

**[Learning] Bean 名稱和 Step / Job 名稱是兩回事**：`sampleStep` 是 Spring 容器裡的 Bean 名稱，`helloStep2` 是 Spring Batch 存進 JobRepository 的名稱。兩者不一致不是錯誤，但容易混淆；Batch-2 之後的模組會讓它們一致。這裡維持原本的名稱，是為了不改變執行紀錄裡的名稱。

### 2. 誰執行了 Job？

程式裡沒有任何一行「執行 HelloJob」：Spring Boot 的 `JobLauncherApplicationRunner` 在啟動完成後，自動執行容器裡的 Job（`spring.batch.job.enabled` 預設為 `true`）。

容器裡有多個 Job 時，Spring Boot 3 之後必須用 `spring.batch.job.name` 指定要執行哪一個（Boot 2 會全部執行），這個主題在 Spring_Batch-4。

### 3. 批次是「做完就結束」的程式

和 web 應用程式不同，批次通常由外部排程（cron、Kubernetes CronJob、Jenkins）啟動，做完就結束，**exit code** 告訴排程這次成功還是失敗：

```java
public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(SpringBatchApplication.class, args)));
}
```

`SpringApplication.exit` 會詢問容器裡所有的 `ExitCodeGenerator`。Spring Boot 的 `JobExecutionExitCodeGenerator` 依照 Job 的結果回傳：

| Job 狀態 | exit code |
|---|---|
| COMPLETED | 0 |
| FAILED | 5 |

實測：讓 Tasklet 拋出例外，`java -jar` 的 exit code 是 5，剛好是 `BatchStatus.FAILED` 在 enum 中的位置（COMPLETED=0、STARTING=1、STARTED=2、STOPPING=3、STOPPED=4、FAILED=5）。

如果 main 只寫 `SpringApplication.run(...)`，Job 失敗時程式仍然以 0 結束，排程會以為成功。

### 4. 為什麼移除 web？

原本加了 `spring-boot-starter-web`，但沒有任何 Controller。web server 會讓程式一直執行，看不出批次「做完就結束」的特性，還佔用一個 port。

classpath 上沒有 web 時，Spring Boot 判斷為 `WebApplicationType.NONE`，不啟動 web server。

需要 REST API 觸發 Job 的情境，見 Spring_Batch-4。

### 5. 注入介面，不要注入實作

```java
// 舊寫法：指定具體類別
public Step sampleStep(JobRepository jobRepository, DataSourceTransactionManager transactionManager)

// 新寫法：只要求「一個 TransactionManager」
public Step sampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager)
```

Spring Boot 建立的是 `JdbcTransactionManager`（`DataSourceTransactionManager` 的子類別），所以舊寫法剛好可以執行。但如果之後加上 JPA，Boot 建立的會變成 `JpaTransactionManager`，舊寫法就找不到 Bean。注入介面可以不受實作變動影響。

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | baseline 測試：啟動時執行 HelloJob、Job 與 Step 的名稱 |
| 2 | Java 17 → 21、Spring Boot 3.4.4 → 3.5.16、Gradle 8.13 → 8.14.3 |
| 3 | Spring Boot 3.5.16 → 4.1.1、Gradle 8.14.3 → 9.8.0 |
| 4 | 重構：移除 web 與 Lombok、exit code、注入介面 |
| 5 | Docker |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| Job 失敗時 exit code 仍然是 0 | main 沒有使用 `SpringApplication.exit`，排程無法判斷失敗 |
| 沒有使用的依賴 | `spring-boot-starter-web`（沒有 Controller）、Lombok（沒有使用） |
| 注入具體類別 | `DataSourceTransactionManager`，見上方第 5 點 |
| 多餘的 `@Configuration` | `@SpringBootApplication` 已經包含 `@Configuration` |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 執行後 | 印出 Hello world! 後，web server 持續執行 | 印出 Hello world! 後**程式結束** |
| Port | 8888 | 無 |
| Job 失敗時的 exit code | 0 | 5 |

### 升級時學到的事

- **Boot 4 的 `starter-batch` 不含 JDBC**：要改用 `spring-boot-starter-batch-jdbc`。實測只用 `starter-batch` 時，容器裡沒有任何 `PlatformTransactionManager`，Step 無法建立，程式無法啟動。
- **Spring Batch 6 搬移了 package**：

  | 類別 | Spring Batch 5 | Spring Batch 6 |
  |---|---|---|
  | `Job` | `org.springframework.batch.core` | `org.springframework.batch.core.job` |
  | `Step` | `org.springframework.batch.core` | `org.springframework.batch.core.step` |
  | `JobParameters` | `org.springframework.batch.core` | `org.springframework.batch.core.job.parameters` |
  | `RepeatStatus` | `org.springframework.batch.repeat` | `org.springframework.batch.infrastructure.repeat` |
  | `ItemReader` / `ItemWriter` | `org.springframework.batch.item` | `org.springframework.batch.infrastructure.item` |

- **`JobExplorer` 已標記為將移除**：查詢執行紀錄改用 `JobRepository`（`getJobInstances`、`getJobExecutions`）。
- **推論要用測試確認**：原本的設定檔寫成 `Server.port:8888`（大寫 S），分析時推測沒有作用；baseline 測試證明 port 確實是 8888，因為 Spring Boot 的 relaxed binding 比對屬性名稱時**不分大小寫**。Spring_Cache、Spring_Flyway 的 README 因此一併更正。
