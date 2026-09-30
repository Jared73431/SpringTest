# Spring Data JPA - Book CRUD API

使用 Spring Data JPA + PostgreSQL 實作書籍的 CRUD REST API，並附有 Dockerfile 與 docker-compose 設定。

這是本 Repo 早期（2022）的 JPA 練習，已完成現代化（Spring Boot 2.7 → 4.1），過程中修正了更新功能的 Bug 並重新設計 API，詳見下方「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Hibernate | 7.4 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| Database | PostgreSQL |
| 測試 | JUnit 5、Testcontainers 2.0、TestRestTemplate |

## 架構

```text
Client
  ↓  JSON
BookController        /api/books，Request / Response 使用 DTO（Java Record）
  ↓
BookService           商業邏輯、交易邊界（@Transactional）
  ↓
BookRepo              Spring Data JPA（JpaRepository）
  ↓
PostgreSQL            資料表由 Hibernate 自動建立（ddl-auto=update）

GlobalExceptionHandler（@RestControllerAdvice）統一將錯誤轉為 ProblemDetail
```

## 專案結構

```text
src/main/java/com/example/demo/
├── controller/BookController.java        # REST API
├── dto/BookRequest.java                  # 新增 / 修改的 Request（含驗證）
├── dto/BookResponse.java                 # 回應格式
├── entity/Book.java                      # JPA Entity
├── exception/BookNotFoundException.java
├── exception/GlobalExceptionHandler.java # 404 → ProblemDetail
├── repository/BookRepo.java
└── service/BookService.java, impl/BookServiceImpl.java
```

## 執行方式

### 本機執行

需要 JDK 21 與 PostgreSQL（資料庫名稱 `test`）。

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- 預設 port：`8081`
- 資料庫連線方式請見下方「[資料庫連線設定](#資料庫連線設定)」

### Docker

```bash
./gradlew bootJar
docker compose up --build
```

- 使用 `docker` profile，port `8015`
- 需要事先啟動資料庫的 compose（建立 `database_net_postgres` 網路與 `postgres` 容器），完整步驟見「[Docker 容器化練習](#docker-容器化練習)」

## 資料庫連線設定

### 需要準備的資訊

| 項目 | 本機執行（預設） | Docker（`docker` profile） | 說明 |
|---|---|---|---|
| Host | `localhost` | `postgres` | 資料庫所在的主機；Docker 內使用容器名稱 |
| Port | `5432` | `5432` | PostgreSQL 預設 port |
| Database | `test` | `test` | 資料庫名稱，**需事先建立** |
| Username | `postgres` | `postgres` | 可用環境變數 `DB_USERNAME` 覆寫 |
| Password | `postgres` | `postgres` | 可用環境變數 `DB_PASSWORD` 覆寫 |

資料表（`book`）與 Sequence（`book_id_seq`）不需要手動建立，應用程式啟動時由 Hibernate 自動建立（見下方 `ddl-auto`）。

### JDBC URL 的組成

```text
jdbc:postgresql://localhost:5432/test
└──┬─┘└───┬────┘  └───┬───┘└┬─┘└┬─┘
  協定   資料庫種類     Host   Port Database
```

JDBC Driver 會依照 URL 中的 `postgresql` 自動選擇，不需要另外設定 `driver-class-name`。

### 快速準備一個 PostgreSQL

若本機沒有 PostgreSQL，可以用 Docker 啟動（`POSTGRES_DB=test` 會自動建立資料庫）：

```bash
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=test \
  postgres:15-alpine
```

若已有 PostgreSQL，只需建立資料庫：

```sql
CREATE DATABASE test;
```

### 如何修改連線資訊

帳號密碼透過環境變數提供，設定檔中只保留預設值：

```properties
# 有環境變數 DB_USERNAME 就用它的值，沒有就用冒號後面的預設值 postgres
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
```

> 注意：`.properties` 的註解必須獨立一行、以 `#` 開頭；寫在設定值後面的 `#` 會被當成值的一部分。

| 想修改的項目 | 做法 |
|---|---|
| 帳號 / 密碼 | 設定環境變數 `DB_USERNAME`、`DB_PASSWORD` |
| Host / Port / 資料庫名稱 | 設定環境變數 `SPRING_DATASOURCE_URL`（Spring Boot 會自動對應到 `spring.datasource.url`） |

```bash
# Git Bash / macOS / Linux
DB_USERNAME=myuser DB_PASSWORD=mypass ./gradlew bootRun

# PowerShell
$env:DB_USERNAME="myuser"; $env:DB_PASSWORD="mypass"; ./gradlew bootRun
```

> IntelliJ IDEA：Run → Edit Configurations → Environment variables，填入 `DB_USERNAME=myuser;DB_PASSWORD=mypass`

設定值的優先順序（上面的會覆蓋下面的）：

```text
命令列參數（--spring.datasource.url=...）
  ↓
環境變數（SPRING_DATASOURCE_URL、DB_PASSWORD ...）
  ↓
application-{profile}.properties（例如 application-docker.properties）
  ↓
application.properties
```

### 設定參數說明

#### `application.properties`（本機執行）

| 參數 | 目前值 | 說明 |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/test` | 資料庫連線位置 |
| `spring.datasource.username` | `${DB_USERNAME:postgres}` | 資料庫帳號 |
| `spring.datasource.password` | `${DB_PASSWORD:postgres}` | 資料庫密碼 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 啟動時如何處理資料表結構（見下表） |
| `spring.jpa.show-sql` | `true` | 在 Console 印出 Hibernate 執行的 SQL，方便學習與除錯；正式環境建議關閉 |
| `server.port` | `8081` | 應用程式的 HTTP port |
| `spring.mvc.problemdetails.enabled` | `true` | 所有錯誤回應（400、404、405…）統一使用 RFC 9457 ProblemDetail 格式 |

`spring.jpa.hibernate.ddl-auto` 的選項：

| 值 | 啟動時的行為 | 適用情境 |
|---|---|---|
| `none` | 不做任何事 | 正式環境（Schema 由 Flyway 等工具管理） |
| `validate` | 檢查 Entity 與資料表是否一致，不一致就啟動失敗 | 正式環境的保護 |
| `update` | 新增缺少的資料表與欄位；**不會刪除欄位、也不會修改欄位型別** | 開發、練習（本專案使用） |
| `create` | ⚠️ 刪除資料表後重建，**資料會消失** | 每次都要乾淨資料的開發 |
| `create-drop` | ⚠️ 同 `create`，且關閉時再刪除 | 測試 |

#### `application-docker.properties`（`docker` profile）

只在設定 `SPRING_PROFILES_ACTIVE=docker` 時載入，並覆蓋 `application.properties` 中相同的參數：

| 參數 | 值 | 與本機的差異 |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://postgres:5432/test` | Host 改為 Docker 網路中的容器名稱 `postgres` |
| `server.port` | `8015` | 配合 `docker-compose.yml` 的 port 對應 |

其餘參數與 `application.properties` 相同。

### 不需要設定的項目

以下由 Spring Boot / Hibernate 自動處理：

| 項目 | 說明 |
|---|---|
| JDBC Driver（`driver-class-name`） | 依 URL 自動判斷 |
| Hibernate Dialect（`hibernate.dialect`） | Hibernate 6 起自動偵測，手動設定反而會出現警告 |
| 連線池 | 預設使用 HikariCP，最多 10 條連線 |

### 測試時的資料庫

執行 `./gradlew test` 時**不會**使用上述設定。測試透過 Testcontainers 啟動臨時的 PostgreSQL 容器，並自動覆蓋連線資訊（見 `PostgresContainerTestBase`），因此不會影響本機資料庫。

### 常見連線錯誤

| 錯誤訊息 | 原因 | 解決方式 |
|---|---|---|
| `Connection to localhost:5432 refused` | PostgreSQL 沒有啟動，或 port 不對 | 確認資料庫已啟動、port 正確 |
| `password authentication failed for user "..."` | 帳號或密碼錯誤 | 檢查 `DB_USERNAME` / `DB_PASSWORD` |
| `database "test" does not exist` | 資料庫尚未建立 | 執行 `CREATE DATABASE test;` |

## API

Base URL：`http://localhost:8081/api/books`

| Method | Path | 說明 | 成功 | 失敗 |
|---|---|---|---|---|
| `GET` | `/api/books` | 查詢全部 | `200` | |
| `GET` | `/api/books/{id}` | 查詢一筆 | `200` | `404` |
| `POST` | `/api/books` | 新增 | `201` + `Location` header | `400` |
| `PUT` | `/api/books/{id}` | 修改 | `200` | `400` / `404` |
| `DELETE` | `/api/books/{id}` | 刪除 | `204` | `404` |

### Request Body（`POST` / `PUT`）

所有欄位皆為必填：

```json
{
  "isbn": 12345,
  "title": "Java",
  "author": "Tom",
  "year": 2020,
  "publisher": "OReilly",
  "cost": 450.5
}
```

### 範例

```bash
# 新增
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"isbn":12345,"title":"Java","author":"Tom","year":2020,"publisher":"OReilly","cost":450.5}'

# 查詢
curl http://localhost:8081/api/books/1

# 修改
curl -X PUT http://localhost:8081/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"isbn":12345,"title":"Java 2nd","author":"Tom","year":2021,"publisher":"OReilly","cost":500}'

# 刪除
curl -X DELETE http://localhost:8081/api/books/1
```

### 錯誤格式

所有錯誤回應皆使用 [RFC 9457 ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457)（`Content-Type: application/problem+json`）：

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Book not found: id=999",
  "instance": "/api/books/999"
}
```

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**。測試使用 Testcontainers 自動啟動臨時的 PostgreSQL 容器，測試結束後自動刪除，不會連到本機開發用的資料庫。

`BookControllerIntegrationTest` 透過真實 HTTP 呼叫測試所有 API，包含成功、驗證失敗、404、刪除等情境。

## Docker 容器化練習

這個模組也是「Docker 容器化 → Kubernetes → 上雲」學習路線的第一站（2025-04），主要練習 **Docker 網路**：讓應用程式容器與另一份 compose 建立的資料庫容器互相連線。

### 練習步驟

```text
步驟 0  資料庫 compose（放在 repo 外的 database 資料夾）
        啟動 postgres 容器，並建立網路 database_net_postgres
          │
步驟 1  本模組的 docker-compose.yml
        build: . → 用 Dockerfile 在本機建立 image
        加入外部網路 database_net_postgres，用名稱 "postgres" 連線資料庫
          │
步驟 2  docker tag / docker push
        將 image 命名為 jared73431/spring-jpa-app:1.0 並推上 Docker Hub
          │
步驟 3  另一份 compose 改用 image: jared73431/spring-jpa-app:1.0
        不再從原始碼 build，而是從 Registry 拉取 image 部署（K8s 的部署方式）
          │
步驟 4  （規劃中）Kubernetes → 上雲
```

步驟 1 到步驟 3 的差別，是從「在本機 build、在本機執行」變成「從 Registry 取得 image 執行」，這是走向 Kubernetes 的關鍵。

### 三個檔案的職責

| 檔案 | 職責 | 使用時機 |
|---|---|---|
| `Dockerfile` | **打包什麼**：把程式與執行環境包成 image | `docker build` |
| `docker-compose.yml` | **怎麼執行**：port、環境變數、網路 | `docker compose up` |
| `application-docker.properties` | **容器裡的程式設定**：資料庫 host、port | 程式啟動時（`docker` profile） |

#### `Dockerfile`

```dockerfile
# 基礎環境：只含 Java 21 執行環境（JRE），比 JDK 小
FROM eclipse-temurin:21-jre
# 容器內的工作目錄
WORKDIR /app
# 將本機 build 好的 jar 放入 image（需先執行 ./gradlew bootJar）
COPY build/libs/*.jar app.jar
# 說明程式使用 8015（僅為文件用途，實際對外開放由 compose 的 ports 決定）
EXPOSE 8015
# 容器啟動時執行的指令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Dockerfile 不知道資料庫在哪、也不決定對外 port。同一個 image 可以在本機、其他電腦或 Kubernetes 上執行，這就是容器化的核心。

#### `docker-compose.yml`

| 設定 | 作用 |
|---|---|
| `build: .` | 用同資料夾的 Dockerfile 建立 image |
| `container_name: spring-app` | 容器名稱 |
| `ports: "8015:8015"` | `主機 port:容器 port`，主機連 `localhost:8015` 會轉進容器 |
| `SPRING_PROFILES_ACTIVE=docker` | 啟用 `docker` profile，載入 `application-docker.properties` |
| `SPRING_DATASOURCE_*` | 資料庫連線資訊（環境變數優先於設定檔） |
| `networks` + `external: true` | 加入**另一份 compose 已建立**的網路，本 compose 不自行建立 |

#### `application-docker.properties`

只在 `docker` profile 啟用時載入，讓同一個 jar 在本機連 `localhost`、在容器內連 `postgres`（詳見「[設定參數說明](#設定參數說明)」）。

### Docker 網路重點

**1. 容器之間用「名稱」連線**

同一個 Docker 網路中，容器名稱就是它的網址（Docker 內建 DNS）。`jdbc:postgresql://postgres:5432/test` 中的 `postgres` 指的是名為 `postgres` 的容器。

> 在容器裡，`localhost` 指的是**容器自己**，不是主機，所以容器內不能用 `localhost` 連資料庫。

**2. 網路名稱 `database_net_postgres` 的由來**

Compose 會自動在網路名稱前加上**專案名稱**（預設為 compose 檔所在的資料夾名稱）：

```text
資料夾 database  +  網路 net_postgres  →  database_net_postgres
```

因此其他 compose 引用它時，必須寫完整名稱並標示 `external: true`。

**3. 為什麼拆成兩份 compose**

資料庫是長期運作、多個專案共用的基礎設施；應用程式則會反覆重新部署。拆開後，重建應用程式容器不會影響資料庫，這也與 Kubernetes 的設計觀念一致。

### 重現步驟

**1. 建立資料庫 compose**

在 repo 外建立資料夾 `database`（資料夾名稱會影響網路名稱），放入 `docker-compose.yml`：

```yaml
services:
  postgres:
    image: postgres
    container_name: postgres
    restart: always
    networks:
      - net_postgres
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${DB_PASSWORD:-postgres}
    volumes:
      - postgresql_data:/var/lib/postgresql/data

networks:
  net_postgres:
    driver: bridge

volumes:
  postgresql_data:
```

```bash
cd database
docker compose up -d
docker exec -it postgres psql -U postgres -c "CREATE DATABASE test;"
```

**2. 啟動應用程式**

```bash
cd Spring_JPA
./gradlew bootJar
docker compose up --build
```

**3. 確認**

```bash
curl http://localhost:8015/api/books
docker network inspect database_net_postgres   # 可看到 postgres 與 spring-app 兩個容器
```

### 對應到 Kubernetes

| Docker 練習 | Kubernetes |
|---|---|
| Dockerfile / image | 相同，Kubernetes 使用同一個 image |
| 推上 Docker Hub | Kubernetes 從 Registry 拉取 image |
| compose 的 service | **Deployment**（管理 Pod） |
| `ports: 8015:8015` | **Service**（NodePort / LoadBalancer）或 **Ingress** |
| `environment` | **ConfigMap**（一般設定）+ **Secret**（密碼） |
| `docker` profile | 同樣使用 `SPRING_PROFILES_ACTIVE`（例如新增 `k8s` profile） |
| Docker 網路 + 容器名稱 | **Service DNS**（例如 `postgres`），不需自行建立網路 |
| `restart: always` | Deployment 自動重啟 Pod、維持副本數 |
| volume | **PersistentVolumeClaim** |
| healthcheck | **liveness / readiness probe**（通常搭配 Actuator） |

### 目前的限制

| 項目 | 說明 |
|---|---|
| 需要先手動 build | Dockerfile 複製的是本機的 jar，忘記執行 `bootJar` 會包到舊版 |
| 依賴外部 compose | 本模組的 compose 沒有定義資料庫，必須先完成「重現步驟 1」 |
| `COPY build/libs/*.jar` | `./gradlew build` 會同時產生 `-plain.jar`，目前因檔名排序剛好留下正確的 jar，並非刻意設計 |
| 設定重複 | compose 的 `SPRING_DATASOURCE_URL` 與 `docker` profile 的值相同，保留其中一處即可 |

> 常見的改進方式是 **multi-stage build**（在 image 內用 JDK build、再只把 jar 複製到 JRE image），或使用 Spring Boot 內建的 `./gradlew bootBuildImage`（不需 Dockerfile）。

## 已知限制（刻意保留）

| 項目 | 說明 |
|---|---|
| `isbn` 使用 `Integer` | 練習用設計，無法存放真實的 13 碼 ISBN（超過 Integer 範圍會回 400） |
| `cost` 使用 `double` | 金額計算可能有浮點誤差，實務上建議使用 `BigDecimal` |
| `ddl-auto=update` | 由 Hibernate 自動建表；實務上建議使用 Flyway 管理 Schema（參考 [Spring_Flyway](../Spring_Flyway)） |
| 400 錯誤訊息 | 目前只顯示 `Invalid request content.`，未列出是哪個欄位驗證失敗 |

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 整合測試，固定原本行為（包含 Bug） |
| 2 | Gradle 7.5.1 → 8.14.3 |
| 3 | Java 11 → 21、Spring Boot 2.7.5 → 3.5.16、`javax` → `jakarta`、Hibernate 5 → 6 |
| 4 | Spring Boot 3.5.16 → 4.1.1（Hibernate 7）、Gradle → 9.8.0、Testcontainers 1.x → 2.x |
| 5 | Field Injection → Constructor Injection、Entity 移除 `@Data` |
| 6 | 修正更新 Bug、查無資料回 404（`@RestControllerAdvice` + ProblemDetail） |
| 7 | 重新設計為 REST API（`/api/books`、DTO、新增 DELETE） |
| 8 | 修正 Docker 設定（`eclipse-temurin:21-jre`、啟用 `docker` profile） |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| 查詢全部 | `GET /findall` | `GET /api/books` |
| 新增 | `POST /saveBook?ISBN=..&title=..`，回 `200` 無 body | `POST /api/books`（JSON），回 `201` + 新資料 |
| 查詢一筆 | `GET /getOneBook/{ID}` | `GET /api/books/{id}` |
| 修改 | `POST /updateBook?ID=..`，**實際上會新增一筆重複資料** | `PUT /api/books/{id}`，更新原資料 |
| 刪除 | 無 | `DELETE /api/books/{id}` |
| 查無資料 | `500` | `404` + ProblemDetail |
| Docker | Port 對不上（程式 8081、對外 8015），且 image 已停止維護。2025-04 練習時 port 為 8015 可正常運作；2025-05 為避免與 Spring_JPA2 衝突改為 8081，但 compose 未啟用 `docker` profile，導致容器版本失效 | 啟用 `docker` profile（固定 8015，不受本機 port 影響），改用 `eclipse-temurin:21-jre` |

### 升級時學到的事

- **`javax` → `jakarta`**：Java EE 移交 Eclipse 基金會後更名為 Jakarta EE，Spring Boot 3 起全面改用 `jakarta.*`。
- **Hibernate 6 自動偵測 Dialect**：不需要再設定 `hibernate.dialect`（否則會出現警告 `HHH90000025`）。
- **Hibernate 6.6 的 merge 行為改變**：對「資料庫中不存在的 id」執行 `save()`，Hibernate 5 會默默新增一筆，Hibernate 6.6 則拋出 `StaleObjectStateException`。舊行為正是造成更新 Bug 的原因之一。
- **Spring Boot 4 測試模組化**：`TestRestTemplate` 移至 `spring-boot-resttestclient`，需加上 `@AutoConfigureTestRestTemplate` 與 `spring-boot-restclient` 依賴。
- **JPA Entity 不要用 `@Data`**：它產生的 `equals` / `hashCode` 使用所有欄位，Entity 放入 `Set` 或 id 變動時容易出錯。
- **Constructor Injection**：依賴一目了然、可宣告為 `final`，也能在不啟動 Spring 的情況下撰寫單元測試。
- **測試不要連本機資料庫**：原本的 `contextLoads()` 會連到 `localhost:5432` 並觸發 `ddl-auto=update`；改用 Testcontainers 後測試環境與開發資料完全隔離。
