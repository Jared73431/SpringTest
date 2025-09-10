# Spring Boot + Swagger UI 用戶管理系統

這是一個使用 Spring Boot 2.7+ 和 SpringDoc OpenAPI 3 構建的 RESTful API 項目，提供完整的用戶管理功能和自動生成的 API 文檔。

## 🚀 功能特性

- ✅ 完整的用戶 CRUD 操作
- ✅ 自動生成 Swagger UI 文檔
- ✅ PostgreSQL 數據庫集成
- ✅ 數據驗證和異常處理
- ✅ 響應統一封裝
- ✅ 中文化 API 文檔
- ✅ 交互式 API 測試界面
- ✅ Lombok 代碼簡化
- ✅ 多環境配置支持
- ✅ 日誌記錄和監控
- ✅ 連線池優化

## 📋 技術棧

- **框架**: Spring Boot 2.7+
- **數據庫**: PostgreSQL / H2 (測試)
- **ORM**: Spring Data JPA + Hibernate
- **API 文檔**: SpringDoc OpenAPI 3
- **代碼簡化**: Lombok
- **構建工具**: Maven / Gradle
- **Java 版本**: JDK 8+

## 🛠️ 環境要求

### 必需軟件
- JDK 8 或更高版本
- Maven 3.6+ 或 Gradle 7.0+
- PostgreSQL 12+ （生產環境）
- IDE（推薦 IntelliJ IDEA 或 Eclipse）

### IDE 配置（Lombok 支持）
#### IntelliJ IDEA
1. 安裝 Lombok Plugin：`Settings` → `Plugins` → 搜索 `Lombok`
2. 啟用 Annotation Processing：`Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors` → ✅ `Enable annotation processing`

#### Eclipse
1. 下載 lombok.jar
2. 運行：`java -jar lombok.jar`
3. 選擇 Eclipse 安裝路徑並安裝

### 數據庫設置
1. 安裝並啟動 PostgreSQL
2. 創建數據庫：
```sql
CREATE DATABASE test;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE test TO postgres;
```

## 🚀 快速開始

### 1. 克隆項目
```bash
git clone <your-repo-url>
cd spring-swagger-demo
```

### 2. 配置數據庫
編輯 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test
    username: postgres
    password: postgres
```

### 3. 構建項目
```bash
# 使用 Maven
mvn clean install

# 使用 Gradle
./gradlew build
```

### 4. 運行應用
```bash
# 使用 Maven
mvn spring-boot:run

# 使用 Gradle  
./gradlew bootRun

# 或者先打包再運行
mvn clean package
java -jar target/spring-swagger-demo-1.0.0.jar
```

### 5. 訪問應用
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API 文檔 JSON**: http://localhost:8080/api-docs
- **健康檢查**: http://localhost:8080/actuator/health
- **應用資訊**: http://localhost:8080/actuator/info

## 📖 API 文檔

### 用戶管理 API

| 方法 | 端點 | 描述 | 請求體 |
|------|------|------|--------|
| GET | `/api/users` | 獲取所有用戶 | - |
| GET | `/api/users/{id}` | 根據ID獲取用戶 | - |
| POST | `/api/users` | 創建新用戶 | CreateUserRequest |
| PUT | `/api/users/{id}` | 更新用戶信息 | UpdateUserRequest |
| DELETE | `/api/users/{id}` | 刪除用戶 | - |

### 響應格式
所有 API 響應都遵循統一格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

### 示例請求

#### 創建用戶
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "name": "John Doe",
    "email": "john@example.com",
    "age": 25
  }'
```

#### 獲取所有用戶
```bash
curl -X GET "http://localhost:8080/api/users"
```

#### 更新用戶
```bash
curl -X PUT "http://localhost:8080/api/users/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "age": 26
  }'
```

## 🏗️ 項目結構

```
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── SwaggerDemoApplication.java     # 主應用類
│   │       ├── config/
│   │       │   └── OpenApiConfig.java          # OpenAPI 配置
│   │       ├── controller/
│   │       │   └── UserController.java         # REST 控制器
│   │       ├── dto/
│   │       │   ├── ApiResponse.java            # 統一響應格式
│   │       │   ├── CreateUserRequest.java      # 創建用戶請求
│   │       │   ├── UpdateUserRequest.java      # 更新用戶請求
│   │       │   └── UserResponse.java           # 用戶響應
│   │       ├── entity/
│   │       │   └── User.java                   # 用戶實體
│   │       ├── exception/
│   │       │   └── GlobalExceptionHandler.java # 全局異常處理
│   │       ├── repository/
│   │       │   └── UserRepository.java         # 數據訪問層
│   │       └── service/
│   │           └── UserService.java            # 業務邏輯層
│   └── resources/
│       ├── application.yml                     # 主配置文件
│       ├── application-dev.yml                 # 開發環境配置
│       ├── application-prod.yml                # 生產環境配置
│       └── application-test.yml                # 測試環境配置
└── test/
    └── java/                                   # 測試代碼
        └── com/example/demo/
            ├── UserControllerTest.java         # 控制器測試
            ├── UserServiceTest.java            # 服務測試
            └── UserRepositoryTest.java         # 資料庫測試
```

## 🔧 配置說明

### 資料來源配置 (DataSourceAutoConfiguration & DataSourceProperties)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test    # 數據庫連接地址
    username: postgres                            # 數據庫用戶名
    password: postgres                            # 數據庫密碼
    driver-class-name: org.postgresql.Driver      # 數據庫驅動
    hikari:                                       # HikariCP 連線池配置
      maximum-pool-size: 20                      # 最大連線數
      minimum-idle: 5                            # 最小空閒連線數
      connection-timeout: 30000                  # 連線超時時間 (ms)
      idle-timeout: 600000                       # 空閒超時時間 (ms)
      max-lifetime: 1800000                      # 連線最大生存時間 (ms)
```

### JPA 配置
```yaml
spring:
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update                            # 自動更新表結構
    show-sql: true                                # 顯示 SQL
    properties:
      hibernate:
        format_sql: true                          # 格式化 SQL
        jdbc:
          batch_size: 20                          # 批次處理大小
```

### Swagger 配置
```yaml
springdoc:
  api-docs:
    path: /api-docs                               # API 文檔 JSON 端點
  swagger-ui:
    path: /swagger-ui.html                        # Swagger UI 訪問路徑
    operations-sorter: method                     # 按方法排序
    tags-sorter: alpha                            # 按標籤字母排序
```

## 🧪 測試

### 運行所有測試
```bash
# Maven
mvn test

# Gradle
./gradlew test
```

### 運行特定測試
```bash
# Maven
mvn test -Dtest=UserControllerTest

# Gradle
./gradlew test --tests UserControllerTest
```

### 測試覆蓋率報告
```bash
# Maven (需要 JaCoCo plugin)
mvn jacoco:report

# Gradle
./gradlew jacocoTestReport
```

### 測試環境配置
項目包含測試專用的配置文件 `application-test.yml`，使用 H2 內存數據庫：
```bash
mvn test -Dspring.profiles.active=test
```

## 🌍 多環境部署

### 開發環境
```bash
java -jar app.jar --spring.profiles.active=dev
```

### 生產環境
```bash
java -jar app.jar --spring.profiles.active=prod \
  --DB_HOST=prod-db-host \
  --DB_PASSWORD=secure-password
```

### 使用 Docker
```dockerfile
FROM openjdk:8-jre-slim
COPY target/spring-swagger-demo-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t spring-swagger-demo .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod spring-swagger-demo
```

## 📝 開發指南

### Lombok 註解使用
- `@Data`: 生成 getter/setter/toString/equals/hashCode
- `@Builder`: 生成建構器模式
- `@Slf4j`: 生成 log 變量
- `@RequiredArgsConstructor`: 生成 final 字段的構造函數
- `@NoArgsConstructor`: 生成無參構造函數
- `@AllArgsConstructor`: 生成全參構造函數

### 新增 API 端點步驟
1. 在 `entity` 包中定義實體類
2. 在 `repository` 包中創建 Repository 接口
3. 在 `dto` 包中定義請求/響應 DTO
4. 在 `service` 包中實現業務邏輯
5. 在 `controller` 包中創建 REST 端點
6. 添加適當的 Swagger 註解
7. 編寫單元測試

### 數據庫遷移
使用 Flyway 進行數據庫版本管理：
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

## 🤝 貢獻指南

1. Fork 本項目
2. 創建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打開 Pull Request

### 代碼規範
- 遵循 Google Java Style Guide
- 使用 Lombok 簡化代碼
- 編寫完整的 Javadoc
- 確保測試覆蓋率 > 80%
- 使用有意義的提交消息

## 📊 性能監控

### Actuator 端點
- `/actuator/health` - 應用健康狀態
- `/actuator/info` - 應用資訊
- `/actuator/metrics` - 性能指標
- `/actuator/loggers` - 日誌配置

### 數據庫監控
```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 1000
```

## 🆘 常見問題

### Q: 數據庫連接失敗？
A: 請確認：
- PostgreSQL 服務已啟動
- 數據庫 `test` 已創建
- 用戶名和密碼正確
- 防火牆設置允許連接
- 檢查 `application.yml` 中的連接配置

### Q: Swagger UI 無法訪問？
A: 請檢查：
- 應用是否成功啟動
- 端口 8080 是否被占用
- 訪問正確的 URL：http://localhost:8080/swagger-ui.html
- 檢查 `springdoc.swagger-ui.enabled=true`

### Q: Lombok 註解不生效？
A: 請確認：
- IDE 已安裝 Lombok 插件
- 啟用了 Annotation Processing
- 重新構建項目 (`mvn clean compile`)
- 檢查 Lombok 版本兼容性

### Q: 如何修改端口？
A: 在 `application.yml` 中修改：
```yaml
server:
  port: 9090
```

### Q: 如何啟用 HTTPS？
A: 配置 SSL：
```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: password
    keyStoreType: PKCS12
    keyAlias: tomcat
```

### Q: 如何添加認證？
A: 添加 Spring Security：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

## 📄 許可證

本項目使用 [MIT License](LICENSE) 許可證。

## 📞 聯系方式

- **作者**: 開發團隊
- **郵箱**: dev@example.com
- **項目地址**: https://github.com/your-repo
- **問題報告**: https://github.com/your-repo/issues
- **Wiki**: https://github.com/your-repo/wiki

## 📚 相關資源

- [Spring Boot 官方文檔](https://spring.io/projects/spring-boot)
- [SpringDoc OpenAPI 3](https://springdoc.org/)
- [Lombok 官方文檔](https://projectlombok.org/)
- [PostgreSQL 文檔](https://www.postgresql.org/docs/)
- [Maven 官方文檔](https://maven.apache.org/guides/)

## 🚀 未來計劃

- [ ] 添加用戶認證和授權
- [ ] 實現分頁和排序
- [ ] 添加數據庫遷移支持
- [ ] 集成 Redis 緩存
- [ ] 實現檔案上傳功能
- [ ] 添加單元測試和集成測試
- [ ] Docker 容器化部署
- [ ] CI/CD 流水線配置

---

**感謝使用本項目！如果覺得有幫助，請給個 ⭐ Star！**

**Happy Coding! 🎉**