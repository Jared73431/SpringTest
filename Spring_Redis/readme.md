# Spring Boot + Redis 練習項目

這是一個用於學習和練習 Redis 與 Spring Boot 整合的示例項目。

## 🚀 快速開始

### 1. 啟動 Redis 服務器

使用 Docker Compose 啟動 Redis 和 Redis Commander：

```bash
docker-compose up -d
```

這將啟動：
- Redis 服務器在端口 6379
- Redis Commander (Web UI) 在端口 8015

### 2. 運行 Spring Boot 應用

```bash
./gradlew bootRun
```

應用將在 `http://localhost:8080` 啟動。

### 3. 初始化測試數據

```bash
curl -X POST http://localhost:8080/redis/init-test-data
```

## 🛠️ 項目結構

```
src/main/java/com/example/demo/
├── config/
│   └── RedisConfig.java          # Redis 配置
├── controller/
│   └── RedisController.java      # REST API 控制器
├── entity/
│   └── User.java                 # 用戶實體
├── service/
│   └── RedisService.java         # Redis 服務類
└── RedisPracticeApplication.java # 主應用程序
```

## 📋 API 端點

### String 操作
- `GET /redis/string/{key}` - 獲取字符串值
- `POST /redis/string/{key}` - 設置字符串值
- `POST /redis/string/{key}/expire?value={value}&seconds={seconds}` - 設置帶過期時間的字符串

### Object 操作
- `GET /redis/user/{id}` - 獲取用戶對象
- `POST /redis/user/{id}` - 設置用戶對象

### Hash 操作
- `GET /redis/hash/{key}` - 獲取整個 Hash
- `GET /redis/hash/{key}/{field}` - 獲取 Hash 字段值
- `POST /redis/hash/{key}/{field}` - 設置 Hash 字段值

### List 操作
- `GET /redis/list/{key}` - 獲取整個列表
- `POST /redis/list/{key}/left` - 從左邊添加元素
- `POST /redis/list/{key}/right` - 從右邊添加元素
- `DELETE /redis/list/{key}/left` - 從左邊彈出元素
- `DELETE /redis/list/{key}/right` - 從右邊彈出元素

### Set 操作
- `GET /redis/set/{key}` - 獲取集合所有成員
- `POST /redis/set/{key}` - 添加元素到集合
- `GET /redis/set/{key}/contains/{value}` - 檢查元素是否存在

### ZSet (有序集合) 操作
- `GET /redis/zset/{key}` - 獲取有序集合所有成員
- `POST /redis/zset/{key}?value={value}&score={score}` - 添加元素到有序集合
- `GET /redis/zset/{key}/score/{value}` - 獲取元素分數

### 通用操作
- `POST /redis/expire/{key}?seconds={seconds}` - 設置過期時間
- `GET /redis/exists/{key}` - 檢查鍵是否存在
- `DELETE /redis/{key}` - 刪除鍵
- `GET /redis/ttl/{key}` - 獲取剩餘過期時間

## 🧪 測試示例

### 1. 字符串操作測試

```bash
# 設置字符串
curl -X POST http://localhost:8015/redis/string/hello \
  -H "Content-Type: application/json" \
  -d '"World"'

# 獲取字符串
curl http://localhost:8015/redis/string/hello

#