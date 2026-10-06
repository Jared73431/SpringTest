# Spring Flyway - 資料庫版本控管

使用 [Flyway](https://documentation.red-gate.com/flyway) 管理資料庫結構的變更：每個變更都是一個有版本號的遷移檔，應用程式啟動時自動執行尚未執行過的遷移，所有環境的資料庫結構都能保持一致。

本模組**沒有業務 API**，重點在遷移檔本身；執行結果可以用 `/actuator/flyway` 或直接查詢資料庫觀察。

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.5 → 4.1、Flyway 12），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Flyway | 12（`spring-boot-starter-flyway` + `flyway-database-postgresql`） |
| 資料庫 | PostgreSQL（資料庫名稱 `Flyway`） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL） |

## 遷移檔

```text
src/main/resources/doc/migration/common/     ← spring.flyway.locations 的第一個位置（SQL）
  V1.0.0__create_javastack.sql                 建立資料表 t_javastack
  V1.0.1__insert_javastack.sql                 新增 5 筆資料
  V1.0.2__update_javastack.sql                 標題加上 'flyway:' 前綴
  V1.0.3__alter_javastack.sql                  新增 note、time 欄位
  V1.0.4__update_javastack.sql                 更新 note、time
  R__javastack_summary_view.sql                可重複：建立 view（正確用法）
  R__update_javastack.sql                      可重複：更新資料（不建議的用法，見下方）
src/main/java/db/migration/                  ← 第二個位置（Java）
  V1_0_5__ComplexMigration.java                依資料內容逐筆計算 note
```

執行順序（`flyway_schema_history`）：

| 順序 | 版本 | 描述 | 類型 |
|---|---|---|---|
| 1～5 | 1.0.0 ～ 1.0.4 | SQL 遷移 | SQL |
| 6 | 1.0.5 | ComplexMigration | JDBC（Java） |
| 7 | — | javastack summary view | SQL（可重複） |
| 8 | — | update javastack | SQL（可重複） |

> 遷移檔中的資料是簡體中文（`标题1`），而且**刻意保留**：已執行過的遷移檔不能修改，見「[checksum](#3-已執行的遷移檔一個字都不能改)」。

## 執行方式

### Docker（建議）

```bash
docker compose up --build                            # 啟動時自動執行所有遷移
curl http://localhost:8093/actuator/flyway           # 每個遷移的版本、狀態、checksum
docker compose exec postgres psql -U postgres -d Flyway -c "SELECT * FROM flyway_schema_history"
docker compose down -v                               # -v 刪除資料庫，下次啟動重新執行所有遷移
```

### 本機執行

需要 JDK 21 與 PostgreSQL，並先建立資料庫：

```sql
CREATE DATABASE "Flyway";   -- 名稱有大寫，要加雙引號；不加會變成小寫的 flyway，與連線網址不符
```

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

| 環境變數 | 預設值 | 說明 |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL 位址 |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` | 資料庫帳密 |

## 設定

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:5432/Flyway
spring.flyway.locations=classpath:doc/migration/common,classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=false
spring.flyway.baseline-on-migrate=false
management.endpoints.web.exposure.include=health,flyway
```

| 設定 | 說明 |
|---|---|
| `spring.datasource.*` | Flyway **預設使用主要的 DataSource**，不需要另外設定 `spring.flyway.url` |
| `spring.flyway.locations` | 遷移檔位置，可以多個；Java 遷移放在對應的 package（`db.migration` → `classpath:db/migration`） |
| `validate-on-migrate` | 執行前驗證已執行過的遷移檔沒有被修改（預設 `true`） |
| `out-of-order` / `baseline-on-migrate` | 見下方「[兩個要小心的設定](#5-兩個要小心的設定)」 |

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**。每次測試都由 Testcontainers 啟動乾淨的 PostgreSQL，從頭執行所有遷移，不會連到本機的資料庫。

| 測試 | 內容 |
|---|---|
| `MigrationBaselineTest` | 執行紀錄的順序與類型、資料表結構與資料、R__ 覆蓋 Java 遷移的結果、view |
| `ChecksumValidationTest` | **在已執行過的 V1.0.0 加上一行註解 → 驗證失敗**；檔案不變 → 驗證通過；再執行一次 → 沒有任何遷移 |
| `FlywayActuatorTest` | `/actuator/flyway` 列出所有遷移且狀態皆為 `SUCCESS` |

---

## Flyway 教學

### 1. 為什麼需要資料庫版本控管

| 做法 | 問題 |
|---|---|
| 手動執行 SQL | 不知道每個環境執行到哪一步，容易漏執行或重複執行 |
| `ddl-auto=update`（Hibernate 自動改表） | 只會新增欄位，**不會刪除或改名**；沒有紀錄、無法 review，不適合正式環境 |
| **Flyway** | 每個變更都是版本化的檔案，跟程式碼一起進 Git、一起 review；資料庫中記錄執行到哪一版 |

### 2. 遷移檔的命名規則

| 前綴 | 格式 | 執行時機 | 用途 |
|---|---|---|---|
| **V**（版本） | `V1.0.1__insert_javastack.sql` | 依版本號執行，**每個只執行一次** | 結構變更、資料轉換 |
| **R**（可重複） | `R__javastack_summary_view.sql` | 所有 V 之後；**內容改變才重新執行**；多個 R 依描述的字母順序 | view、function、procedure |
| Java | `V1_0_5__ComplexMigration.java` | 與 V 相同 | SQL 難以表達的資料轉換 |

- 版本與描述之間是**兩個底線** `__`
- Java 類別名稱不能有點，版本號用底線代替：`V1_0_5` = 版本 1.0.5

### 3. 已執行的遷移檔一個字都不能改

Flyway 在 `flyway_schema_history` 記錄每個檔案的 **checksum**。下次啟動時（`validate-on-migrate`）發現檔案內容與紀錄不同，就拒絕啟動：

```text
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 1.0.0
```

`ChecksumValidationTest` 示範了這一點：**只在 V1.0.0 加上一行註解，驗證就失敗**。

| 想做的事 | 正確做法 |
|---|---|
| 修正已執行的遷移 | **新增一個遷移**（例如 `V1.0.6__fix_xxx.sql`） |
| 只在本機開發、還沒有人執行過 | 可以修改，再清空本機資料庫重新執行 |
| 真的必須接受修改（例如只改了註解） | `flyway repair` 重新計算 checksum（要了解後果才使用） |

> 這也是本模組保留簡體中文資料的原因：把 `标题1` 改成 `標題1`，所有已經執行過的資料庫都會無法啟動。
>
> Java 遷移預設**不計算 checksum**，所以可以安全地修改註解或 log（本模組就是這樣做的）；也因此修改 Java 遷移的邏輯不會被偵測到，要特別小心。
>
> Flyway 計算 checksum 時以「行」為單位，Windows 的 CRLF 與 Linux 的 LF 不會造成差異。

### 4. 可重複遷移（R__）：用來放「重新執行也沒關係」的物件

**正確用法**（`R__javastack_summary_view.sql`）：

```sql
CREATE OR REPLACE VIEW v_javastack_summary AS
SELECT note, COUNT(*) AS total, MAX(time) AS last_updated
FROM t_javastack GROUP BY note;
```

修改 view 定義時直接改這個檔案，Flyway 偵測到內容改變就重新執行；`CREATE OR REPLACE` 讓重新執行的結果和第一次相同（冪等）。

**不建議的用法**（`R__update_javastack.sql`，練習時留下的範例，保留不動）：

```sql
UPDATE t_javastack SET note = 'flyway repeated ok5', time = NOW();
```

| 問題 | 說明 |
|---|---|
| 每次修改都會再更新一次資料 | `ok5` 看起來是為了讓它重新執行而改了 5 次 |
| **覆蓋了 Java 遷移的結果** | R__ 一定在所有 V 之後執行，`V1_0_5` 依資料計算出的 note（`特殊标题1`、`包含内容关键字`）**全部被覆蓋成固定值**，最後完全看不到 Java 遷移的效果 |

> 為什麼不直接刪除或改名這個檔案：已執行過的可重複遷移從本機消失，Flyway 驗證時會回報「已執行但找不到」，所有已經執行過的資料庫都會驗證失敗。

### 5. 兩個要小心的設定

#### `baseline-on-migrate`：導入既有資料庫時使用

資料庫**已經有資料表、但沒有 `flyway_schema_history`** 時，先在 `baseline-version` 建立一個基準點，該版本（含）以前的遷移視為已執行。

**修正前的陷阱**：

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1.0
```

Flyway 比較版本時 **`1.0` 等於 `1.0.0`**。如果這個資料庫裡原本就有其他資料表，Flyway 會在 1.0 建立基準點 → `V1.0.0`（建立 `t_javastack`）被**視為已執行而跳過** → `V1.0.1` 新增資料時資料表不存在，遷移失敗。

本模組使用專屬的空資料庫，不需要基準點，改回預設 `false`。

#### `out-of-order`：多人並行開發時使用

允許版本號**較小**的遷移晚於**較大**的版本執行。例如 A 先合併了 `V1.0.6`，B 稍後合併 `V1.0.5`：

| 設定 | 結果 |
|---|---|
| `false`（預設） | `V1.0.5` 被忽略，驗證時回報 |
| `true` | 執行 `V1.0.5`，但不同環境的執行順序可能不同 |

只在確實需要時開啟；一般建議團隊用時間戳記當版本號（例如 `V20261006_1200__xxx.sql`）避免衝突。

### 6. Java 遷移

```java
public class V1_0_5__ComplexMigration extends BaseJavaMigration {
    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        ...
    }
}
```

- 使用 Flyway 提供的連線，與 SQL 遷移在**同一個交易**中執行，失敗時整個遷移 rollback
- `SingleConnectionDataSource(..., true)`：`suppressClose = true`，不要關閉 Flyway 的連線
- **不能注入 Spring Bean**：遷移在 Spring 容器完成啟動前就執行，而且之後業務程式改了，舊的遷移邏輯也不應該跟著變

### 7. Flyway Undo：付費版功能

`U` 前綴的檔案（例如 `U1.0.1__undo_insert.sql`）可以撤銷特定版本的遷移，但這是 **Flyway Teams / Enterprise（付費版）** 的功能，社群版不支援。

> 修正前的 README 寫「Undo 功能已廢棄」，並不正確：它仍然存在，只是需要付費。

即使有 Undo，實務上也很少使用（刪除欄位的 Undo 無法還原資料），常見的做法是：

1. **向前修正**：新增一個遷移修正問題
2. **重要變更前備份**
3. **相容的變更**：例如欄位改名分成「新增新欄位 → 程式同時寫兩邊 → 搬資料 → 移除舊欄位」多個版本，任何一步都能回到上一版程式

### 8. Flyway vs Liquibase

| | Flyway | Liquibase |
|---|---|---|
| 遷移檔格式 | 一般 SQL（或 Java） | XML / YAML / JSON / SQL（changeset） |
| 學習成本 | 低，會寫 SQL 就能用 | 較高，但可以跨資料庫產生 SQL |
| Rollback | 付費版 | 社群版即支援（需要自己定義） |
| Spring Boot | `spring-boot-starter-flyway` | `spring-boot-starter-liquibase` |

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 測試（修正前的 `contextLoads` 會對本機資料庫執行遷移） |
| 2 | Java 17 → 21、Spring Boot 3.5.0 → 3.5.16、Gradle 8.13 → 8.14.3 |
| 3 | Spring Boot → 4.1.1（Flyway 12）、Gradle → 9.8.0；`flyway-core` → `spring-boot-starter-flyway` |
| 4 | 設定整理、移除未使用的 JPA、`/actuator/flyway`、Java 遷移改用 logger 與 record；port → 8093 |
| 5 | 新增正確用法的可重複遷移（view），以及 checksum 驗證的測試 |
| 6 | 新增 Dockerfile 與 docker-compose |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 應用程式 | 執行完遷移就結束（沒有 Web） | 持續執行，提供 `/actuator/flyway` |
| Port | 設定 8015，因大小寫錯誤（`Server.port`）不生效；實際上也沒有 Web | `8093` |
| `out-of-order` | `true` | `false`（預設） |
| `baseline-on-migrate` | `true`（`baseline-version=1.0`） | `false`（預設） |
| 遷移 | 7 個 | 8 個（新增 `R__javastack_summary_view.sql`）；原本的 7 個**完全沒有修改** |
| log | 整個應用程式 DEBUG | 預設 INFO |

> 已經在本機執行過的資料庫，下次啟動時只會新增執行 `R__javastack_summary_view.sql`。

### 升級時學到的事

- **Spring Boot 4 的 Flyway 需要 starter**：只引入 `flyway-core` 時，應用程式**正常啟動、沒有任何錯誤，但遷移完全沒有執行**（Flyway 的自動設定移到了獨立模組）。這次是由 baseline 測試發現的——如果沒有測試，可能要到正式環境發現資料表沒建才知道。
- **Flyway 10 起，資料庫支援拆成獨立模組**：PostgreSQL 需要 `flyway-database-postgresql`。
- **測試要同時覆蓋 Flyway 的連線**：修正前設定了 `spring.flyway.url`，測試只覆蓋 `spring.datasource.*` 的話，Flyway 仍會連到本機資料庫執行遷移。
- **沒用到的依賴要移除**：原本的 JPA 與 `ddl-auto=validate` 在沒有任何 Entity 的情況下什麼都沒驗證，只增加啟動時間與誤解。
