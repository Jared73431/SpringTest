# Spring Quartz - 動態排程、持久化與叢集

Quartz 是比 `@Scheduled` 完整的排程框架，有三個主要的差別：

- 排程可以存在**資料庫**
- 可以在**執行中**新增、修改、暫停
- 停機時錯過的觸發可以**補做**（misfire），多台主機可以組成**叢集**

主程式是一個「提醒」服務：每個提醒是一個 Quartz Job + CronTrigger，透過 REST API 動態管理，排程存在 PostgreSQL。另外有 5 課學習測試。

> 本模組由原本的 `Spring_Quartz`、`Spring_Quartz-2`、`Spring_Quartz-3` **合併**而成。三個模組大量重疊（1、2 都是每秒印字），真正值得學的主題（misfire、叢集、並行控制）反而都沒有涵蓋。合併前的程式碼與 baseline 測試保留在 Git 歷史中，詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-quartz`） |
| Quartz | 2.5.2（Spring Boot 管理） |
| 資料庫 | PostgreSQL（資料庫名稱 `Quartz`），Flyway 建立 `qrtz_*` 資料表 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Awaitility、MockMvc、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_QuartzBasicsTest](src/test/java/com/example/demo/lesson/L01_QuartzBasicsTest.java) | Job / JobDetail / Trigger、Quartz cron 的 `?` 與**星期數字陷阱**、`6#3` |
| 2 | [L02_JobDataAndConcurrencyTest](src/test/java/com/example/demo/lesson/L02_JobDataAndConcurrencyTest.java) | JobDataMap、預設會同時執行 vs `@DisallowConcurrentExecution`、`@PersistJobDataAfterExecution` |
| 3 | [L03_JdbcJobStoreTest](src/test/java/com/example/demo/lesson/L03_JdbcJobStoreTest.java) | RAMJobStore 重新啟動後排程消失，JDBC JobStore 仍然存在 |
| 4 | [L04_MisfireTest](src/test/java/com/example/demo/lesson/L04_MisfireTest.java) | **Misfire**：停機期間錯過的觸發，補做一次、略過、還是全部補做 |
| 5 | [L05_DynamicScheduleApiTest](src/test/java/com/example/demo/lesson/L05_DynamicScheduleApiTest.java) | 動態排程 API（本模組的主程式） |
| 6 | [L06_ClusterTest](src/test/java/com/example/demo/lesson/L06_ClusterTest.java) | **叢集**：每次觸發只有一台執行，一台停止時另一台接手 |

## 專案結構

```text
src/main/java/com/example/demo/
├── reminder/
│   ├── ReminderJob             提醒的 Job：注入 Spring Bean、JobDataMap 透過 setter 注入
│   ├── ReminderService         用 Scheduler API 新增、修改、暫停、刪除排程
│   └── ReminderController      /api/reminders
├── summary/
│   ├── DailySummaryJob         固定排程的 Job
│   └── DailySummaryConfig      JobDetail + Trigger 宣告成 Bean，由 Spring Boot 自動註冊
└── notification/NotificationService   代替 Email：記錄發送內容

src/main/resources/
├── application.properties      spring.quartz.*（JDBC JobStore、叢集、misfire）
└── db/migration/V1__quartz_tables.sql   Quartz 內附的 PostgreSQL 建表 SQL
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers）。第 1、2、4 課使用記憶體的 JobStore，不需要資料庫。

```bash
./gradlew test
./gradlew test --tests '*L04*'      # 只跑第 4 課
```

### Docker Compose：兩個實例組成叢集

```bash
docker compose up --build

# 新增一個每 5 秒觸發的提醒
curl -H 'Content-Type: application/json' \
     -d '{"name":"demo","cron":"0/5 * * * * ?","recipient":"store-101","message":"打烊後盤點"}' \
     localhost:8100/api/reminders

docker compose logs -f app app-b    # 每次觸發只有其中一台印出 Send to store-101
docker compose kill app             # 模擬當機：約 10 秒後由 app-b 接手
```

> Windows 上 port 無法綁定時（Hyper-V / WSL 動態保留了 port），改用其他主機 port：`APP_PORT=8300 docker compose up --build`。

### 本機執行

```sql
CREATE DATABASE "Quartz";
```

```bash
./gradlew bootRun
```

## API

| 方法 | URL | 說明 |
|---|---|---|
| POST | `/api/reminders` | 新增（201 + `Location`）；名稱重複 409；cron 或時區不正確 400 |
| GET | `/api/reminders` | 全部提醒 |
| GET | `/api/reminders/{name}` | 單一提醒：cron、時區、狀態（`NORMAL` / `PAUSED`）、下次與上次觸發時間 |
| PUT | `/api/reminders/{name}/schedule` | 修改時間（`{"cron": "...", "timeZone": "..."}`） |
| POST | `/api/reminders/{name}/pause` | 暫停（204） |
| POST | `/api/reminders/{name}/resume` | 恢復（204） |
| POST | `/api/reminders/{name}/run` | 立刻執行一次（202） |
| DELETE | `/api/reminders/{name}` | 刪除（204） |
| GET | `/actuator/quartz/jobs` | Actuator：所有 Job 與 Trigger |

```json
{ "name": "store-101-closing", "cron": "0 30 22 * * ?", "timeZone": "Asia/Taipei",
  "recipient": "store-101@example.com", "message": "打烊後盤點" }
```

`timeZone` 可以省略（預設 `Asia/Taipei`），`name` 只能使用小寫英文、數字與 `-`。

## 設定

| 設定 | 值 | 說明 |
|---|---|---|
| `server.port` | `8100` | |
| `spring.quartz.job-store-type` | `jdbc` | 排程存在資料庫，使用 Spring 的 DataSource |
| `spring.quartz.jdbc.initialize-schema` | `never` | `qrtz_*` 資料表由 Flyway 建立 |
| `spring.quartz.overwrite-existing-jobs` | `true` | 程式中宣告的 Job 覆蓋資料庫中的舊設定 |
| `...scheduler.instanceId` | `AUTO` | 每個實例自動產生唯一的 ID |
| `...jobStore.isClustered` | `true` | 叢集 |
| `...jobStore.clusterCheckinInterval` | `5000` | 實例之間互相檢查的間隔（毫秒） |
| `...jobStore.misfireThreshold` | `60000` | 延遲超過 60 秒才算 misfire |
| `...threadPool.threadCount` | `5` | 執行 Job 的執行緒數 |

（`...` 是 `spring.quartz.properties.org.quartz` 的簡寫）

---

## Quartz 教學

### 1. Job、JobDetail、Trigger

| 名稱 | 意義 |
|---|---|
| Job | 要做的事。**每次觸發都會建立新的實例**，所以 Job 本身不能保存狀態 |
| JobDetail | 一個「工作」：Job 類別 + 名稱 + 資料（JobDataMap） |
| Trigger | 什麼時候觸發：`SimpleTrigger`（每隔多久、重複幾次）或 `CronTrigger` |

- 一個 JobDetail 可以有多個 Trigger。
- `SimpleTrigger` 的 `repeatCount = 2` 代表**重複** 2 次，總共執行 3 次。

**Quartz 的 cron 和 Spring `@Scheduled` 的不同：**

| | Spring `@Scheduled` | Quartz |
|---|---|---|
| 「日」與「星期」 | 可以都是 `*` | 其中一個**必須是 `?`**：`0 30 22 * * ?` |
| 星期的數字 | 0 / 7 = 星期日，**1 = 星期一** | **1 = 星期日**，2 = 星期一 |
| 年 | 無 | 第 7 個欄位（選填） |
| 特殊語法 | `L` | `L`、`W`（最接近的平日）、`#`（`6#3` = 第 3 個星期五） |

> 同樣寫 `2`，Quartz 是星期一，Spring 是星期二。**星期請用英文縮寫（`MON`）**，在兩邊的意義都一樣。

### 2. 在 Spring 中撰寫 Job

```java
@DisallowConcurrentExecution
public class ReminderJob extends QuartzJobBean {

    private final NotificationService notificationService;   // Spring Bean：建構子注入
    private String recipient;                                 // JobDataMap 的值：同名的 setter 自動注入

    public ReminderJob(NotificationService notificationService) { ... }
    public void setRecipient(String recipient) { ... }
}
```

Spring Boot 的 `SpringBeanJobFactory` 用 Spring 建立 Job，所以可以直接注入 Bean。修正前的 Spring_Quartz-3 另外寫了 `MyJobFactory` 做同一件事；Spring_Quartz-2 用匿名內部類別當成 Job 也能執行，正是因為 `SpringBeanJobFactory` 能把外部的設定類別注入給匿名類別的建構子（能運作，但很脆弱）。

**並行執行：**Quartz 預設**允許**同一個 Job 同時執行多次（`@Scheduled` 不會）。上一次還沒結束就不該開始下一次的工作（例如盤點），要加上 `@DisallowConcurrentExecution`。

### 3. JDBC JobStore

```properties
spring.quartz.job-store-type=jdbc
```

| | RAMJobStore（預設） | JDBC JobStore |
|---|---|---|
| 排程存在 | 記憶體 | `qrtz_*` 資料表 |
| 重新啟動後 | 用 API 新增的排程**全部消失** | 仍然存在 |
| 叢集 | 不支援 | 支援 |

Spring Boot 會直接使用應用程式的 DataSource（HikariCP）。修正前自己建立 `SchedulerFactoryBean`，另外用 c3p0 建立連線池，繞過了所有自動設定；而且使用的 **c3p0 0.9.5.2 有已知的安全漏洞**（CVE-2019-5427）。

`qrtz_*` 共 11 張資料表，建表 SQL 在 Quartz jar 中（`org/quartz/impl/jdbcjobstore/tables_postgres.sql`）。本模組把它放進 Flyway 管理。

### 4. Misfire：停機時錯過的觸發

這是 Quartz 和 `@Scheduled` 最重要的差別之一：`@Scheduled` 停機時錯過的執行就錯過了；Quartz 記得應該觸發的時間，恢復後依照策略決定要不要補做。

| 策略（CronTrigger） | 恢復後 | 適用 |
|---|---|---|
| `withMisfireHandlingInstructionFireAndProceed` | 立刻補做**一次**（預設） | 打烊後盤點：做一次就好 |
| `withMisfireHandlingInstructionDoNothing` | 不補做，等下一次 | 即時性的通知：過時了就沒有意義 |
| `withMisfireHandlingInstructionIgnoreMisfires` | 錯過幾次補做幾次 | 每小時統計：每一次都不能少 |

錯過的時間**超過 `misfireThreshold`**（預設 60 秒）才算 misfire。修正前設成 1 秒：GC 或資料庫稍微慢一點就會被誤判。

### 5. 動態排程

```java
scheduler.scheduleJob(jobDetail, trigger);         // 新增
scheduler.rescheduleJob(triggerKey, newTrigger);   // 修改時間（Job 與資料不變）
scheduler.pauseJob(jobKey);                        // 暫停
scheduler.resumeJob(jobKey);                       // 恢復
scheduler.triggerJob(jobKey);                      // 立刻執行一次
scheduler.deleteJob(jobKey);                       // 刪除
```

> **不要用 `scheduler.shutdown()` 當成「停止」。**`shutdown()` 是**永久關閉**整個 Scheduler，之後無法再 `start()`。修正前的 `/quartz/shutdown` 之後呼叫 `/quartz/start` 會回傳 500。要暫停請用 `standby()`（整個 Scheduler）或 `pauseJob()`（單一排程）。

### 6. 叢集

多個實例共用同一個資料庫，並設定 `isClustered=true`：

- **每次觸發只有一台執行**：取得下一個要觸發的 Trigger 時，會在 `qrtz_locks` 上加鎖。
- **一台停止時，其他台接手**：實例之間每隔 `clusterCheckinInterval` 互相檢查。

Docker 驗證的結果：

```text
10:51:45 ～ 10:51:55   app 執行每 5 秒的提醒（app-b 一次都沒輪到）
10:52:01              kill app
10:52:12              app-b 接手，補執行錯過的 10:52:05、10:52:10（延遲不到 60 秒，不算 misfire）
10:52:15 之後          app-b 照原本的排程執行
```

> Quartz 叢集保證「只有一台執行」，**不保證平均分配**。負載很輕時常常都是同一台。

**Quartz 叢集 vs ShedLock（[Spring_Scheduling](../Spring_Scheduling) 第 6 課）：**

| | `@Scheduled` + ShedLock | Quartz 叢集 |
|---|---|---|
| 運作方式 | 每台都觸發，搶到鎖的執行 | 只有一台被分配到這次觸發 |
| 排程定義 | 寫在程式或設定檔 | 存在資料庫，可以動態修改 |
| 停機時錯過的執行 | 不補做 | 依 misfire 策略 |
| 複雜度 | 低（1 張表） | 高（11 張表） |

### 延伸：速食店「打烊後 30 分鐘」的排程

每家門市的打烊時間不同時，Quartz 的做法是**每家門市一個 Trigger**：

- 新增門市時 `scheduleJob`，打烊時間改了就 `rescheduleJob`。
- 停機時錯過的盤點用 `FireAndProceed` 補做一次。
- 部署多台時用叢集。

這個情境會在獨立的作品集專案中，和「輪詢」、「事件驅動」等其他做法一起比較。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 三個模組各自的 baseline 測試（Quartz-3 使用 Testcontainers） |
| 2 | Spring_Quartz：Java 17 → 21、Spring Boot 3.0.2 → 3.5.16 → 4.1.1、Gradle 7.6 → 9.8.0 |
| 3 | 以 Spring_Quartz 為基礎重建：Spring Boot 的 Quartz 自動設定、JDBC JobStore、動態排程 API |
| 4 | 第 1～4、6 課 |
| 5 | 刪除 Spring_Quartz-2、Spring_Quartz-3 |
| 6 | Docker：兩個實例的叢集 |

### 合併前三個模組的內容

| 模組 | 內容 | 去向 |
|---|---|---|
| Spring_Quartz | 自己建立 `SchedulerFactoryBean` + `MethodInvokingJobDetailFactoryBean`，每秒印字 | 改用 Spring Boot 自動設定；這種 JobDetail 無法序列化，不能用在 JDBC JobStore |
| Spring_Quartz-2 | `JobDetail`、`Trigger` 宣告成 Bean，由 Boot 自動註冊 | 保留這個寫法：`DailySummaryConfig` |
| Spring_Quartz-3 | JDBC JobStore + REST API 動態新增 / 刪除 | 重寫成 `/api/reminders`，修正下面的問題 |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| **c3p0 0.9.5.2 有安全漏洞** | CVE-2019-5427；而且 Spring Boot 本來就有連線池，不需要 c3p0 |
| `/start` 在 `/shutdown` 之後失敗 | `shutdown()` 是永久關閉 |
| 重複新增仍然回傳 "ok" | 名稱寫死成 `myJob`，例外被 `printStackTrace()` 吞掉 |
| 改變狀態的操作都用 GET | `/quartz/add`、`/remove`、`/start`、`/shutdown` |
| 沒有建表機制 | `qrtz_*` 11 張表要手動建立 |
| 繞過 Spring Boot 的自動設定 | 自己建立 `SchedulerFactoryBean`、讀取原始 properties 檔、自訂 `MyJobFactory` |
| misfire 門檻 1 秒 | 太容易誤判 |
| 叢集沒有啟用 | `isClustered = false`，JDBC JobStore 最主要的價值沒有用到 |
| 其他 | 類別名稱拼錯（`QuartzConfigration`、`QusrtzTask`）；加了和 Quartz 無關的 `@EnableScheduling`；沒有使用的 `HelloJob`、`quartz-jobs`；簡體中文註解；資料庫網址寫死；測試會連到本機資料庫 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 模組 | 3 個 | 1 個 |
| Port | 8080 | 8100 |
| 本機資料庫 | `quartz` | `Quartz` |
| JobStore | 1、2 用記憶體；3 用 JDBC | JDBC（叢集） |
| API | `GET /quartz/add`、`/remove`、`/start`、`/shutdown` | `/api/reminders`（見「[API](#api)」） |
| 排程 | 每秒、每 2 秒印字 | 透過 API 動態新增的提醒 + 每天 23:00 的統計 |

### 升級時學到的事

- **Boot 4.1 使用 Quartz 2.5.2**：建表 SQL 要用對應版本 jar 內附的檔案；結尾的 `COMMIT;` 要拿掉，Flyway 會自己管理交易。
- **推論要用測試確認**：分析時推測 Spring_Quartz-2 的匿名內部類別 Job 無法執行，baseline 測試證明它可以，原因是 `SpringBeanJobFactory` 會注入建構子參數。
- **baseline 測試也不能連到本機資料庫**：Spring_Quartz-3 的資料庫網址寫死在設定檔中，測試透過覆寫 `quartzProperties` 這個 Bean 改指向 Testcontainers，並從 log 確認實際連線的是 container。
- **叢集不會平均分配**：Docker 驗證時，所有觸發都由同一台執行。要驗證接手，必須停掉**正在執行**的那一台。
