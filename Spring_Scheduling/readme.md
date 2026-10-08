# Spring Scheduling - @Scheduled 排程

Spring 內建的排程：`@Scheduled` 的 fixedRate / fixedDelay / cron，以及實務上一定會遇到的問題：

- 時區與夏令時間
- 預設只有 1 條執行緒
- 例外處理
- 部署多台主機時重複執行（ShedLock）

主程式是一個實際的排程：**每天凌晨 3 點（台北時間）清除過期的 session**。另外有 6 課學習測試。

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.0 → 4.1，模組名稱由 `Spring_Scheduleing` 更正為 `Spring_Scheduling`），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Scheduling、JDBC、Actuator） |
| ShedLock | 7.10.1（JDBC，鎖存在 `shedlock` 表） |
| 資料庫 | PostgreSQL（資料庫名稱 `Scheduling`），Flyway 建表 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Awaitility、Testcontainers 2.0（PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_FixedRateVsFixedDelayTest](src/test/java/com/example/demo/lesson/L01_FixedRateVsFixedDelayTest.java) | fixedRate vs fixedDelay：任務比間隔還久時的差別；initialDelay |
| 2 | [L02_CronExpressionTest](src/test/java/com/example/demo/lesson/L02_CronExpressionTest.java) | cron 運算式、時區、**夏令時間會跳過或重複執行**、跨午夜 |
| 3 | [L03_SingleThreadTest](src/test/java/com/example/demo/lesson/L03_SingleThreadTest.java) | 預設只有 1 條執行緒：慢任務拖住其他排程 |
| 4 | [L04_ExceptionTest](src/test/java/com/example/demo/lesson/L04_ExceptionTest.java) | 例外之後還會不會執行：Spring 會，JDK 的 `ScheduledExecutorService` 不會 |
| 5 | [L05_ConfigurationTest](src/test/java/com/example/demo/lesson/L05_ConfigurationTest.java) | cron 寫在設定檔、用 `-` 停用、`/actuator/scheduledtasks` |
| 6 | [L06_ShedLockTest](src/test/java/com/example/demo/lesson/L06_ShedLockTest.java) | 多台主機只執行一次：ShedLock、`lockAtMostFor` / `lockAtLeastFor` |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/SchedulingConfig            @EnableScheduling、@EnableSchedulerLock、LockProvider
└── session/
    ├── SessionCleanupTask             「什麼時候做」：@Scheduled + @SchedulerLock
    └── SessionCleanupService          「要做的事」：刪除過期的 session（可以直接呼叫、直接測試）

src/main/resources/
├── application.properties             cron、時區、執行緒池、Actuator
└── db/migration/V1__create_tables.sql shedlock 表、user_session 表
```

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers）。第 1～4 課不需要資料庫，也不啟動 Spring。

```bash
./gradlew test
./gradlew test --tests '*L02*'      # 只跑第 2 課
```

### Docker Compose：兩個實例觀察 ShedLock

```bash
docker compose up --build
docker compose logs -f app app-b
```

compose 會啟動**兩個應用程式實例**，並把清理排程改成每 10 秒一次。兩個實例都會觸發排程，但每個時間點只有搶到鎖的那一個會印出 `Deleted ... expired sessions`：

```text
app-1    16:21:20  Deleted 0 expired sessions
app-b-1  16:21:30  Deleted 0 expired sessions
app-1    16:21:40  Deleted 0 expired sessions
app-b-1  16:21:50  Deleted 0 expired sessions
```

```bash
curl http://localhost:8099/actuator/scheduledtasks   # 目前的排程與上次、下次執行時間
```

> Windows 上 port 無法綁定時（Hyper-V / WSL 動態保留了 port），改用其他主機 port：`APP_PORT=8300 docker compose up --build`。

### 本機執行

```sql
CREATE DATABASE "Scheduling";
```

```bash
./gradlew bootRun
```

## 設定

| 設定 | 值 | 說明 |
|---|---|---|
| `server.port` | `8099` | |
| `spring.task.scheduling.pool.size` | `4` | 排程的執行緒數，**預設只有 1**（第 3 課） |
| `spring.task.scheduling.thread-name-prefix` | `scheduling-` | log 中容易辨識 |
| `app.cleanup.cron` | `0 0 3 * * *` | 清理排程的 cron；設成 `-` 停用 |
| `app.cleanup.zone` | `Asia/Taipei` | cron 的時區 |
| `app.cleanup.lock-at-least-for` | `PT30S` | ShedLock 至少保留鎖的時間，**必須比排程間隔短** |
| `management.endpoints.web.exposure.include` | `health,scheduledtasks` | |

---

## 排程教學

### 1. fixedRate vs fixedDelay

| | 意義 | 任務 200ms、間隔 100ms 時，開始的間隔 |
|---|---|---|
| `fixedDelay = 100` | 上一次**結束**後等 100ms | 300ms |
| `fixedRate = 100` | 每 100ms **開始**一次 | 200ms（上一次結束後立刻開始） |

`fixedRate` 遇到任務執行得比間隔還久時，**不會同時執行兩次**，而是等上一次結束後立刻開始。所以「每 5 分鐘同步一次」這類可能變慢的工作，通常用 `fixedDelay`，避免一次接一次、完全沒有空檔。

### 2. cron 與時區

Spring 的 cron 有 **6 個欄位**（秒 分 時 日 月 星期），比 Linux crontab 多了「秒」。從 crontab 複製 5 個欄位的運算式，啟動時就會失敗。

| cron | 意義 |
|---|---|
| `0 0 3 * * *` | 每天 03:00:00 |
| `0 0 9 * * MON-FRI` | 平日 09:00 |
| `0 0 23 L * *` | 每月最後一天 23:00 |
| `@daily` | 每天 00:00 |

**用 `CronExpression.next()` 測試 cron**，不需要等待：

```java
CronExpression.parse("0 0 9 * * MON-FRI").next(星期六中午)   // → 星期一 09:00
```

**時區**：沒有指定 `zone` 時使用伺服器的時區，而容器裡通常是 UTC。排在「台北凌晨 3 點」的工作要寫 `zone = "Asia/Taipei"`。`zone` **只對 cron 有效**：修正前寫在 fixedDelay 上，被默默忽略。

**夏令時間**（台灣沒有，但海外的系統要注意）：

| 日期（美國洛杉磯） | 發生什麼事 | `0 30 2 * * *` / `0 30 1 * * *` 的結果 |
|---|---|---|
| 2026-03-08 春季調快 | 02:00 直接跳到 03:00，02:30 不存在 | **這一天不執行** |
| 2026-11-01 秋季調慢 | 01:00～02:00 出現兩次 | **執行兩次** |

所以批次工作要能**補做**，也要有**冪等性**（重複執行不會造成錯誤的結果）。

**跨午夜**：23:45 打烊的門市，「打烊後 30 分鐘」是隔天 00:15，但處理的是**前一天**的營業資料。

### 3. 預設只有 1 條執行緒

Spring Boot 預設的排程執行緒池大小是 **1**，所有 `@Scheduled` 共用。一個任務卡住（例如查詢很慢的資料庫、呼叫沒有逾時的外部 API），**其他所有排程都要等它**。

| 解法 | 說明 |
|---|---|
| `spring.task.scheduling.pool.size=4` | 增加執行緒（本模組的設定） |
| `spring.threads.virtual.enabled=true` | Java 21：每次執行使用新的 Virtual Thread |
| 排程只負責觸發 | 實際工作交給 `@Async` 或 Spring Batch |

### 4. 例外處理

| 排程器 | 任務丟出例外後 |
|---|---|
| Spring（`@Scheduled`、`ThreadPoolTaskScheduler`） | 記錄 log（`ErrorHandler`），下一次照常執行 |
| JDK `ScheduledExecutorService` | **後續的執行全部被取消，而且沒有任何 log** |

自己用 `Executors.newScheduledThreadPool` 寫排程時，任務內一定要 try / catch。

### 5. 把「什麼時候做」和「做什麼」分開

```java
@Scheduled(cron = "${app.cleanup.cron}", zone = "${app.cleanup.zone}")
@SchedulerLock(name = "session-cleanup", lockAtLeastFor = "${app.cleanup.lock-at-least-for}")
public void cleanupExpiredSessions() {
    int deleted = cleanupService.deleteExpired();      // 業務邏輯在 Service
    log.info("Deleted {} expired sessions", deleted);
}
```

- 業務邏輯（`SessionCleanupService`）可以直接呼叫、直接測試，不必等到凌晨 3 點，也可以由管理 API 手動觸發。
- cron 寫在設定檔，不同環境可以不同；設成 `-` 就停用。
- 部署後用 `/actuator/scheduledtasks` 確認排程設定是否生效。

### 6. 多台主機：ShedLock

`@Scheduled` 只管自己這個程序。部署 2 台，每天的清理就會執行 **2 次**。

ShedLock 在執行前先到資料庫的 `shedlock` 表搶鎖：搶到的主機才執行，沒搶到的**直接跳過**（不會排隊等待）。

| 設定 | 意義 |
|---|---|
| `lockAtMostFor` | 鎖最多保留多久：執行到一半主機當機時，過了這段時間鎖會自動失效 |
| `lockAtLeastFor` | 鎖至少保留多久：任務很快結束時，避免時鐘稍有誤差的其他主機在同一個時間點再執行一次 |
| `usingDbTime()` | 用資料庫的時間判斷鎖是否到期，不受各台主機時鐘誤差的影響 |

> **`lockAtLeastFor` 必須比排程的間隔短。**Docker 驗證時，排程每 10 秒一次，但 `lockAtLeastFor` 是 30 秒：下兩次觸發時鎖還沒釋放，**兩台主機都跳過**，35 秒內只執行了 1 次。而 `/actuator/scheduledtasks` 仍然顯示 `SUCCESS`：Actuator 看不出 ShedLock 跳過了執行。

### @Scheduled、Quartz、Spring Batch 怎麼選

| | `@Scheduled` + ShedLock | Quartz | Spring Batch |
|---|---|---|---|
| 解決的問題 | 什麼時候執行 | 什麼時候執行 | 怎麼處理大量資料 |
| 排程定義 | 寫在程式或設定檔 | 可以存在資料庫，執行中動態新增 / 修改 | 本身不負責排程 |
| 停機時錯過的執行 | 不會補做 | misfire 策略可以補做 | 失敗的 Job 可以從中斷點重跑 |
| 適合 | 固定時間的簡單工作 | 每個對象各自的排程、需要動態調整 | 讀取 → 處理 → 寫入的大量資料 |

實務上常常組合使用：用 `@Scheduled` 或 Quartz **觸發** Spring Batch 的 Job。下一步請見 [Spring_Quartz](../Spring_Quartz) 與 [Spring_Batch](../Spring_Batch)。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Baseline 測試：鎖定原本註冊的 3 個排程 |
| 2 | 模組名稱 `Spring_Scheduleing` → `Spring_Scheduling`（`git mv`，保留歷史紀錄） |
| 3 | Java 17 → 21、Spring Boot 3.0.2 → 3.5.16、Gradle 7.6 → 8.14.3 |
| 4 | Spring Boot 4.1.1、Gradle 9.8.0 |
| 5 | 示範任務改成實際的清理排程：PostgreSQL、Flyway、ShedLock、執行緒池、Actuator |
| 6 | 第 1～6 課 |
| 7 | Docker：兩個實例觀察 ShedLock，`lockAtLeastFor` 改成可以設定 |

### 修正前發現的問題

| 問題 | 說明 |
|---|---|
| `zone` 沒有作用 | `@Scheduled(fixedDelay = 60000L, zone = "timeZone")`：zone 只對 cron 有效，而且 `"timeZone"` 是字串、不是旁邊宣告的 `TimeZone` 欄位（那個欄位從未使用）。不會報錯，只是被默默忽略 |
| 所有排程共用 1 條執行緒 | 沒有設定執行緒池 |
| 多台主機會重複執行 | 沒有分散式鎖 |
| 任務只有印字 | `System.out` 每秒印兩行，沒有實際內容，也沒有可以測試的邏輯 |
| 測試時排程在背景一直跑 | `contextLoads` 啟動後，每秒的排程持續執行 |
| 其他 | 模組名稱拼字錯誤；方法名稱大寫開頭；引入沒有使用的 starter-web 與 Lombok |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 模組名稱 | `Spring_Scheduleing` | `Spring_Scheduling` |
| Port | 8080 | 8099 |
| 排程 | 3 個只會印字的任務（每秒、每秒、每分鐘） | 每天 03:00（台北）清除過期 session，ShedLock 保護 |
| 執行緒池 | 1 | 4 |
| 資料庫 | 無 | PostgreSQL `Scheduling` |

### 升級時學到的事

- **Gradle 7.6 無法載入 Boot 3.5 的 plugin**：先升級 wrapper，再修改 Boot 的版本（Spring_R2DBC 也遇過）。
- **Windows 上重新命名資料夾可能失敗**：IDE 開著裡面的檔案時 `git mv` 整個資料夾會出現 Permission denied，改成逐一 `git mv` 被追蹤的檔案。
- **探測的結果比想像中更重要**：夏令時間會讓 cron 跳過一天或執行兩次，`lockAtLeastFor` 太長會讓排程默默少跑。這兩件事都是實際執行之後才發現的。
