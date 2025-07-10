# Spring Boot R2DBC 汽車管理 API

使用 Spring Boot 3、R2DBC 和 PostgreSQL 建立的反應式 REST API，用於管理汽車庫存。

## 功能特色

- **反應式程式設計**: 使用 Spring WebFlux 和 R2DBC 實現非阻塞 I/O
- **資料庫**: PostgreSQL 配合 R2DBC 驅動程式
- **驗證**: 使用 Bean Validation 進行請求驗證
- **資料庫遷移**: 使用 Flyway 管理資料庫架構
- **錯誤處理**: 全域異常處理器提供一致的錯誤回應
- **搜尋功能**: 支援多條件進階搜尋
- **日誌記錄**: 使用 SLF4J 進行完整的日誌記錄

## 使用技術

- **Spring Boot 3.5.3**
- **Spring WebFlux** (反應式 Web)
- **Spring Data R2DBC** (反應式資料庫存取)
- **PostgreSQL** (資料庫)
- **Flyway** (資料庫遷移)
- **Lombok** (減少樣板程式碼)
- **Bean Validation** (請求驗證)

## 系統需求

- Java 17 或更高版本
- Docker 和 Docker Compose
- Gradle (或使用內建的 Gradle wrapper)

## 快速開始

### 1. 複製專案

```bash
git clone <repository-url>
cd spring-r2dbc-car-api
```

### 2. 使用 Docker 啟動 PostgreSQL

```bash
docker-compose up -d
```

### 3. 執行應用程式

```bash
./gradlew bootRun
```

應用程式將在 `http://localhost:8080` 啟動

## API 端點

### 汽車管理

| 方法 | 端點 | 描述 |
|------|------|------|
| GET | `/api/cars` | 取得所有汽車 |
| GET | `/api/cars/{id}` | 根據 ID 取得汽車 |
| POST | `/api/cars` | 建立新汽車 |
| PUT | `/api/cars/{id}` | 更新汽車資訊 |
| DELETE | `/api/cars/{id}` | 刪除汽車 |

### 搜尋功能

| 方法 | 端點 | 描述 |
|------|------|------|
| GET | `/api/cars/search` | 根據條件搜尋汽車 |
| GET | `/api/cars/count` | 計算指定品牌的汽車數量 |

### 搜尋參數

- `make`: 汽車品牌
- `model`: 汽車型號
- `year`: 年份
- `minPrice`: 最低價格
- `maxPrice`: 最高價格
- `yearFrom`: 年份起始範圍
- `yearTo`: 年份結束範圍

## 請求範例

### 建立汽車

```bash
curl -X POST http://localhost:8080/api/cars \
  -H "Content-Type: application/json" \
  -d '{
    "make": "Toyota",
    "model": "Camry",
    "year": 2023,
    "color": "White",
    "price": 30000.00
  }'
```

### 搜尋汽車

```bash
# 根據品牌搜尋
curl "http://localhost:8080/api/cars/search?make=Toyota"

# 根據價格範圍搜尋
curl "http://localhost:8080/api/cars/search?minPrice=20000&maxPrice=40000"

# 根據年份範圍搜尋
curl "http://localhost:8080/api/cars/search?yearFrom=2020&yearTo=2023"
```

## 資料庫架構

### Car 資料表

```sql
CREATE TABLE car (
    id BIGSERIAL PRIMARY KEY,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL CHECK (year > 1900 AND year <= 2100),
    color VARCHAR(30),
    price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 設定檔

### application.properties

```properties
# 伺服器設定
server.port=8080

# R2DBC 設定
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/test2
spring.r2dbc.username=postgres
spring.r2dbc.password=postgres

# Flyway 設定
spring.flyway.url=jdbc:postgresql://localhost:5432/test2
spring.flyway.user=postgres
spring.flyway.password=postgres
spring.flyway.schemas=public
spring.flyway.baseline-on-migrate=true

# 日誌設定
logging.level.org.springframework.r2dbc=DEBUG
logging.level.io.r2dbc.postgresql.QUERY=DEBUG
logging.level.org.flywaydb=INFO
```

## 驗證規則

### CarDto 驗證

- **make**: 必填，不可為空
- **model**: 必填，不可為空
- **year**: 必填，範圍 1900-2100
- **price**: 必須大於 0（如果提供）

## 錯誤處理

API 使用全域異常處理器，回傳標準化的錯誤格式：

```json
{
  "message": "Validation failed",
  "errors": {
    "make": "Make is required",
    "year": "Year must be greater than 1900"
  }
}
```

## 開發指南

### 本地開發

1. 確保 PostgreSQL 正在運行
2. 執行 `./gradlew bootRun`
3. 應用程式將自動執行 Flyway 遷移

### 測試

```bash
./gradlew test
```

### 建置

```bash
./gradlew build
```

## Docker 部署

使用 Docker Compose 進行完整部署：

```bash
docker-compose up --build
```

## 日誌

應用程式提供詳細的日誌記錄：

- **INFO**: 一般操作日誌
- **DEBUG**: SQL 查詢日誌
- **ERROR**: 錯誤和異常日誌

## 效能優化

- 使用 R2DBC 實現非阻塞資料庫存取
- 資料庫索引優化 (make, model, year)
- 反應式程式設計模式提高併發處理能力

## 貢獻指南

1. Fork 專案
2. 建立功能分支
3. 提交變更
4. 推送到分支
5. 建立 Pull Request

## 授權

本專案使用 MIT 授權。

## 支援

如有問題或建議，請建立 Issue 或聯繫維護人員。