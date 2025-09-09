Spring Boot + Swagger UI 用戶管理系統
這是一個使用 Spring Boot 2.7+ 和 SpringDoc OpenAPI 3 構建的 RESTful API 項目，提供完整的用戶管理功能和自動生成的 API 文檔。
🚀 功能特性

✅ 完整的用戶 CRUD 操作
✅ 自動生成 Swagger UI 文檔
✅ PostgreSQL 數據庫集成
✅ 數據驗證和異常處理
✅ 響應統一封裝
✅ 中文化 API 文檔
✅ 交互式 API 測試界面

📋 技術棧

框架: Spring Boot 2.7+
數據庫: PostgreSQL
ORM: Spring Data JPA + Hibernate
API 文檔: SpringDoc OpenAPI 3
構建工具: Maven
Java 版本: JDK 8+

🛠️ 環境要求
必需軟件

JDK 8 或更高版本
Maven 3.6+
PostgreSQL 12+

數據庫設置

安裝並啟動 PostgreSQL
創建數據庫：

sqlCREATE DATABASE test;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE test TO postgres;
🚀 快速開始
1. 克隆項目
   bashgit clone <your-repo-url>
   cd spring-swagger-demo
2. 配置數據庫
   編輯 src/main/resources/application.yml：
   yamlspring:
   datasource:
   url: jdbc:postgresql://localhost:5432/test
   username: postgres
   password: postgres
3. 運行應用
   bash# 使用 Maven
   mvn clean spring-boot:run

# 或者先打包再運行
mvn clean package
java -jar target/spring-swagger-demo-1.0.0.jar
4. 訪問應用

Swagger UI: http://localhost:8080/swagger-ui.html
API 文檔 JSON: http://localhost:8080/api-docs
應用根路徑: http://localhost:8080

📖 API 文檔
用戶管理 API
方法端點描述GET/api/users獲取所有用戶GET/api/users/{id}根據ID獲取用戶POST/api/users創建新用戶PUT/api/users/{id}更新用戶信息DELETE/api/users/{id}刪除用戶
示例請求
創建用戶
bashcurl -X POST "http://localhost:8080/api/users" \
-H "Content-Type: application/json" \
-d '{
"username": "john_doe",
"name": "John Doe",
"email": "john@example.com",
"age": 25
}'
獲取所有用戶
bashcurl -X GET "http://localhost:8080/api/users"
🏗️ 項目結構
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── SwaggerDemoApplication.java     # 主應用類
│   │       ├── config/
│   │       │   └── OpenApiConfig.java          # OpenAPI 配置
│   │       ├── controller/
│   │       │   └── UserController.java         # 控制器
│   │       ├── dto/
│   │       │   ├── ApiResponse.java            # 響應封裝
│   │       │   └── CreateUserRequest.java      # 請求 DTO
│   │       ├── entity/
│   │       │   └── User.java                   # 實體類
│   │       ├── exception/
│   │       │   └── GlobalExceptionHandler.java # 全局異常處理
│   │       ├── repository/
│   │       │   └── UserRepository.java         # 數據訪問層
│   │       └── service/
│   │           └── UserService.java            # 服務層
│   └── resources/
│       ├── application.yml                     # 主配置文件
│       └── application-test.yml                # 測試配置文件
└── test/
└── java/                                   # 測試代碼
🔧 配置說明
數據庫配置 (DataSourceAutoConfiguration & DataSourceProperties)
yamlspring:
datasource:
url: jdbc:postgresql://localhost:5432/test    # 數據庫連接地址
username: postgres                            # 數據庫用戶名
password: postgres                            # 數據庫密碼
driver-class-name: org.postgresql.Driver      # 數據庫驅動
JPA 配置
yamlspring:
jpa:
database-platform: org.hibernate.dialect.PostgreSQLDialect
hibernate:
ddl-auto: update                            # 自動更新表結構
show-sql: true                                # 顯示 SQL
properties:
hibernate:
format_sql: true                          # 格式化 SQL
Swagger 配置
yamlspringdoc:
api-docs:
path: /api-docs                               # API 文檔 JSON 端點
swagger-ui:
path: /swagger-ui.html                        # Swagger UI 訪問路徑
operationsSorter: method                      # 按方法排序
🧪 測試
運行測試
bashmvn test
測試環境配置
項目包含測試專用的配置文件 application-test.yml，使用 H2 內存數據庫：
bashmvn test -Dspring.profiles.active=test
🌍 多環境部署
開發環境
bashjava -jar app.jar --spring.profiles.active=dev
生產環境
bashjava -jar app.jar --spring.profiles.active=prod
🤝 貢獻指南

Fork 本項目
創建功能分支 (git checkout -b feature/AmazingFeature)
提交更改 (git commit -m 'Add some AmazingFeature')
推送到分支 (git push origin feature/AmazingFeature)
打開 Pull Request

📄 許可證
本項目使用 MIT License 許可證。
🆘 常見問題
Q: 數據庫連接失敗？
A: 請確認：

PostgreSQL 服務已啟動
數據庫 test 已創建
用戶名和密碼正確
防火牆設置允許連接

Q: Swagger UI 無法訪問？
A: 請檢查：

應用是否成功啟動
端口 8080 是否被占用
訪問正確的 URL：http://localhost:8080/swagger-ui.html

Q: 如何修改端口？
A: 在 application.yml 中修改：
yamlserver:
port: 9090