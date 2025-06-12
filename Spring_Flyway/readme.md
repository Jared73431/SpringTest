# Flyway 資料庫遷移專案練習

## 項目簡介

這是一個基於 Spring Boot 和 Flyway 的資料庫遷移練習專案，演示了如何使用 Flyway 進行資料庫版本管理和自動化遷移。

## 技術棧

- **Java 17**
- **Spring Boot 3.5.0**
- **Flyway Core** - 資料庫遷移工具
- **PostgreSQL** - 資料庫
- **Lombok** - 簡化 Java 代碼

## Flyway 遷移檔命名規則

Flyway 使用特定的檔命名約定來管理資料庫遷移，每個首碼都有其特殊含義：

### V - 版本化遷移 (Versioned Migrations)

**格式：** `V<版本號>__<描述>.sql` 或 `V<版本號>__<描述>.java`

**特點：**
- 只執行一次
- 按版本號循序執行
- 一旦應用就不能修改
- 用於結構性變更和初始資料

**項目中的示例：**
- `V1.0.0__create_javastack.sql` - 創建 t_javastack 表
- `V1.0.1__insert_javastack.sql` - 插入初始測試資料
- `V1.0.2__update_javastack.sql` - 更新標題欄位
- `V1.0.3__alter_javastack.sql` - 添加新列
- `V1.0.4__update_javastack.sql` - 更新新增欄位
- `V1_0_5__ComplexMigration.java` - 複雜 Java 遷移

### R - 可重複遷移 (Repeatable Migrations)

**格式：** `R__<描述>.sql`

**特點：**
- 每次檢測到檔變更時都會重新執行
- 在所有版本化遷移之後執行
- 適用於視圖、存儲過程、函數等可重複應用的物件

**項目中的示例：**
- `R__update_javastack.sql` - 可重複的資料更新操作

## 遷移執行順序

根據檔案名，遷移的執行順序為：

1. `V1.0.0__create_javastack.sql` - 創建基礎資料表結構
2. `V1.0.1__insert_javastack.sql` - 插入測試資料
3. `V1.0.2__update_javastack.sql` - 更新現有資料
4. `V1.0.3__alter_javastack.sql` - 修改表結構（添加列）
5. `V1.0.4__update_javastack.sql` - 更新新增的列
6. `V1_0_5__ComplexMigration.java` - 執行複雜的業務邏輯遷移
7. `R__update_javastack.sql` - 可重複執行的更新（如有變更）

## Java 遷移示例解析

`V1_0_5__ComplexMigration.java` 展示了複雜的 Java 遷移實現：

- **資料查詢與處理**：查詢現有資料並根據業務邏輯進行條件更新
- **批量處理**：逐條處理記錄並應用不同的業務規則
- **遷移驗證**：執行後驗證遷移結果的正確性
- **自訂映射**：使用 RowMapper 處理查詢結果

## ⚠️ Flyway Undo 功能已廢棄
Flyway Undo 是 Flway 的商業功能，社群版中不提供此功能。

### 什麼是 Undo 功能？

在早期版本的 Flyway 中，存在 Undo 功能，允許回滾已應用的遷移：

- **U首碼檔**：如 `U1.0.1__undo_something.sql`
- **回滾機制**：可以撤銷特定版本的遷移

### 為什麼被廢棄？

1. **複雜性**：資料庫回滾操作極其複雜，容易導致資料丟失
2. **風險性**：不當的回滾可能造成不可逆的資料損壞
3. **維護成本**：需要為每個遷移編寫對應的回滾腳本
4. **實際需求**：在生產環境中，回滾資料庫變更是極其罕見的操作

### 替代方案

現代資料庫遷移推薦使用以下策略：

1. **向前遷移**：通過新的遷移來修復問題，而不是回滾
2. **資料庫備份**：在重要變更前創建完整備份
3. **藍綠部署**：使用部署策略來降低風險
4. **漸進式遷移**：將大的變更分解為小的、可測試的步驟

## 配置說明

在 `application.properties` 中的完整 Flyway 配置：

```properties
## ==============================================================
## FLYWAY配置
## ==============================================================
spring.flyway.url=jdbc:postgresql://localhost:5432/Flyway
spring.flyway.user=Flyway
spring.flyway.password=123456
spring.flyway.locations=classpath:doc/migration/common,classpath:db/migration
spring.flyway.table=flyway_schema_history
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1.0
spring.flyway.out-of-order=true
spring.flyway.validate-on-migrate=true
spring.flyway.enabled=true
```

### 配置參數詳解

| 配置項 | 值 | 說明 |
|--------|----|----- |
| `spring.flyway.url` | `jdbc:postgresql://localhost:5432/Flyway` | 資料庫連接URL，指向名為"Flyway"的PostgreSQL資料庫 |
| `spring.flyway.user` | `Flyway` | 資料庫用戶名 |
| `spring.flyway.password` | `123456` | 資料庫密碼 |
| `spring.flyway.locations` | `classpath:doc/migration/common,classpath:db/migration` | 遷移檔位置，支援多個路徑 |
| `spring.flyway.table` | `flyway_schema_history` | Flyway中繼資料表名，記錄遷移歷史 |
| `spring.flyway.baseline-on-migrate` | `true` | 對於已有資料庫，首次運行時創建基線 |
| `spring.flyway.baseline-version` | `1.0` | 基線版本號 |
| `spring.flyway.out-of-order` | `true` | 允許亂序執行遷移（謹慎使用） |
| `spring.flyway.validate-on-migrate` | `true` | 遷移前驗證已應用的遷移 |
| `spring.flyway.enabled` | `true` | 啟用Flyway自動遷移 |

### 重要配置說明

**多路徑遷移檔：**
```
spring.flyway.locations=classpath:doc/migration/common,classpath:db/migration
```
- `doc/migration/common` - 通用遷移檔
- `db/migration` - 專案特定遷移檔

**亂序執行 (out-of-order)：**
- 設置為 `true` 允許版本號較小的遷移在較大版本之後執行
- 適用于並行開發場景，但生產環境需謹慎使用
- 可能導致不同環境間的遷移執行順序不一致

**基線遷移 (baseline-on-migrate)：**
- 適用於已存在資料的資料庫
- 首次運行時會在指定版本創建基線，跳過之前的遷移
- 對於全新資料庫建議設置為 `false`

## 運行項目

1. 確保 PostgreSQL 資料庫運行中
2. 配置資料庫連接資訊
3. 運行 Spring Boot 應用
4. Flyway 將自動執行所有待處理的遷移

## 最佳實踐

1. **命名規範**：使用清晰、描述性的檔案名
2. **版本管理**：按時間順序遞增版本號
3. **測試優先**：在測試環境中驗證所有遷移
4. **備份策略**：生產環境變更前必須備份
5. **向前相容**：設計遷移時考慮向前相容性
6. **文檔記錄**：為複雜遷移添加詳細注釋

## 注意事項

- 已應用的版本化遷移檔不應修改
- 可重複遷移會在每次應用啟動時檢查變更
- Java 遷移適用於複雜的資料處理邏輯
- 在生產環境中應謹慎使用資料修改操作

---

此專案展示了 Flyway 的核心功能和最佳實踐，為資料庫版本管理提供了完整的參考實現。

