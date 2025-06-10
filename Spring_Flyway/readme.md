# Flyway 数据库迁移项目练习

## 项目简介

这是一个基于 Spring Boot 和 Flyway 的数据库迁移练习项目，演示了如何使用 Flyway 进行数据库版本管理和自动化迁移。

## 技术栈

- **Java 17**
- **Spring Boot 3.5.0**
- **Flyway Core** - 数据库迁移工具
- **PostgreSQL** - 数据库
- **Lombok** - 简化 Java 代码

## Flyway 迁移文件命名规则

Flyway 使用特定的文件命名约定来管理数据库迁移，每个前缀都有其特殊含义：

### V - 版本化迁移 (Versioned Migrations)

**格式：** `V<版本号>__<描述>.sql` 或 `V<版本号>__<描述>.java`

**特点：**
- 只执行一次
- 按版本号顺序执行
- 一旦应用就不能修改
- 用于结构性变更和初始数据

**项目中的示例：**
- `V1.0.0__create_javastack.sql` - 创建 t_javastack 表
- `V1.0.1__insert_javastack.sql` - 插入初始测试数据
- `V1.0.2__update_javastack.sql` - 更新标题字段
- `V1.0.3__alter_javastack.sql` - 添加新列
- `V1.0.4__update_javastack.sql` - 更新新增字段
- `V1_0_5__ComplexMigration.java` - 复杂 Java 迁移

### R - 可重复迁移 (Repeatable Migrations)

**格式：** `R__<描述>.sql`

**特点：**
- 每次检测到文件变更时都会重新执行
- 在所有版本化迁移之后执行
- 适用于视图、存储过程、函数等可重复应用的对象

**项目中的示例：**
- `R__update_javastack.sql` - 可重复的数据更新操作

## 迁移执行顺序

根据文件名，迁移的执行顺序为：

1. `V1.0.0__create_javastack.sql` - 创建基础表结构
2. `V1.0.1__insert_javastack.sql` - 插入测试数据
3. `V1.0.2__update_javastack.sql` - 更新现有数据
4. `V1.0.3__alter_javastack.sql` - 修改表结构（添加列）
5. `V1.0.4__update_javastack.sql` - 更新新增的列
6. `V1_0_5__ComplexMigration.java` - 执行复杂的业务逻辑迁移
7. `R__update_javastack.sql` - 可重复执行的更新（如有变更）

## Java 迁移示例解析

`V1_0_5__ComplexMigration.java` 展示了复杂的 Java 迁移实现：

- **数据查询与处理**：查询现有数据并根据业务逻辑进行条件更新
- **批量处理**：逐条处理记录并应用不同的业务规则
- **迁移验证**：执行后验证迁移结果的正确性
- **自定义映射**：使用 RowMapper 处理查询结果

## ⚠️ Flyway Undo 功能已废弃

### 什么是 Undo 功能？

在早期版本的 Flyway 中，存在 Undo 功能，允许回滚已应用的迁移：

- **U前缀文件**：如 `U1.0.1__undo_something.sql`
- **回滚机制**：可以撤销特定版本的迁移

### 为什么被废弃？

1. **复杂性**：数据库回滚操作极其复杂，容易导致数据丢失
2. **风险性**：不当的回滚可能造成不可逆的数据损坏
3. **维护成本**：需要为每个迁移编写对应的回滚脚本
4. **实际需求**：在生产环境中，回滚数据库变更是极其罕见的操作

### 替代方案

现代数据库迁移推荐使用以下策略：

1. **向前迁移**：通过新的迁移来修复问题，而不是回滚
2. **数据库备份**：在重要变更前创建完整备份
3. **蓝绿部署**：使用部署策略来降低风险
4. **渐进式迁移**：将大的变更分解为小的、可测试的步骤

## 配置说明

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

### 配置参数详解

| 配置项 | 值 | 说明 |
|--------|----|----- |
| `spring.flyway.url` | `jdbc:postgresql://localhost:5432/Flyway` | 数据库连接URL，指向名为"Flyway"的PostgreSQL数据库 |
| `spring.flyway.user` | `Flyway` | 数据库用户名 |
| `spring.flyway.password` | `123456` | 数据库密码 |
| `spring.flyway.locations` | `classpath:doc/migration/common,classpath:db/migration` | 迁移文件位置，支持多个路径 |
| `spring.flyway.table` | `flyway_schema_history` | Flyway元数据表名，记录迁移历史 |
| `spring.flyway.baseline-on-migrate` | `true` | 对于已有数据库，首次运行时创建基线 |
| `spring.flyway.baseline-version` | `1.0` | 基线版本号 |
| `spring.flyway.out-of-order` | `true` | 允许乱序执行迁移（谨慎使用） |
| `spring.flyway.validate-on-migrate` | `true` | 迁移前验证已应用的迁移 |
| `spring.flyway.enabled` | `true` | 启用Flyway自动迁移 |

### 重要配置说明

**多路径迁移文件：**
```
spring.flyway.locations=classpath:doc/migration/common,classpath:db/migration
```
- `doc/migration/common` - 通用迁移文件
- `db/migration` - 项目特定迁移文件

**乱序执行 (out-of-order)：**
- 设置为 `true` 允许版本号较小的迁移在较大版本之后执行
- 适用于并行开发场景，但生产环境需谨慎使用
- 可能导致不同环境间的迁移执行顺序不一致

**基线迁移 (baseline-on-migrate)：**
- 适用于已存在数据的数据库
- 首次运行时会在指定版本创建基线，跳过之前的迁移
- 对于全新数据库建议设置为 `false`

## 运行项目

1. 确保 PostgreSQL 数据库运行中
2. 配置数据库连接信息
3. 运行 Spring Boot 应用
4. Flyway 将自动执行所有待处理的迁移

## 最佳实践

1. **命名规范**：使用清晰、描述性的文件名
2. **版本管理**：按时间顺序递增版本号
3. **测试优先**：在测试环境中验证所有迁移
4. **备份策略**：生产环境变更前必须备份
5. **向前兼容**：设计迁移时考虑向前兼容性
6. **文档记录**：为复杂迁移添加详细注释

## 注意事项

- 已应用的版本化迁移文件不应修改
- 可重复迁移会在每次应用启动时检查变更
- Java 迁移适用于复杂的数据处理逻辑
- 在生产环境中应谨慎使用数据修改操作

---

此项目展示了 Flyway 的核心功能和最佳实践，为数据库版本管理提供了完整的参考实现。