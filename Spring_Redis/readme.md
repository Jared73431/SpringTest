# Spring Redis - 資料結構與實務情境

使用 Spring Data Redis 操作 Redis，分成兩部分：

1. **五種資料結構**：String、Hash、List、Set、ZSet 的基本操作，以及物件（JSON）的存放方式
2. **三個實務情境**：排行榜、API 限流、分散式鎖（面試常見的 Redis 應用）

相關模組：[Spring_Cache](../Spring_Cache)（用 Redis 當 Spring Cache 的快取）。

這是 2025 年加入的練習，已完成現代化（Spring Boot 3.5 → 4.1），過程中修正了**不安全的反序列化設定**，詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Spring Data Redis 4、Lettuce） |
| JSON | Jackson 3（`JacksonJsonRedisSerializer`） |
| Redis | 7（Docker image `redis:7-alpine`） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（Redis）、TestRestTemplate |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/RedisConfig.java            # RedisTemplate<String, User>（明確型別的 JSON）
├── controller/                        # 每種資料結構一個 Controller
│   ├── StringController.java          #   /api/redis/strings/{key}
│   ├── UserController.java            #   /api/redis/users/{id}（物件存成 JSON）
│   ├── HashController.java            #   /api/redis/hashes/{key}
│   ├── ListController.java            #   /api/redis/lists/{key}
│   ├── SetController.java             #   /api/redis/sets/{key}
│   ├── ZSetController.java            #   /api/redis/zsets/{key}
│   ├── KeyController.java             #   /api/redis/keys/{key}（TTL、刪除）
│   └── SampleDataController.java      #   /api/redis/sample-data
├── scenario/                          # 實務情境
│   ├── LeaderboardService.java        #   排行榜（ZSet）
│   ├── RateLimiter.java               #   限流（INCR + EXPIRE，Lua script）
│   ├── DistributedLock.java           #   分散式鎖（SET NX EX + Lua 釋放）
│   └── ScenarioController.java
├── dto/Requests.java, Responses.java
├── exception/GlobalExceptionHandler.java
├── model/User.java                    # record
└── service/RedisService.java
```

## 執行方式

### Docker（建議）

```bash
docker compose up --build
curl -X POST http://localhost:8089/api/redis/sample-data     # 建立範例資料
```

| 服務 | Port | 說明 |
|---|---|---|
| `app` | `8089` | 本應用程式 |
| `redisinsight` | `5540` | Redis 官方管理介面：第一次開啟選「Add Redis database」，Host 填 `redis`、Port 填 `6379` |
| `redis` | 不開放 | 避免與本機的 Redis 衝突；需要時打開 compose 中的 `ports` |

### 本機執行

需要 JDK 21 與 Redis（`localhost:6379`）：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

| 環境變數 | 預設值 | 說明 |
|---|---|---|
| `REDIS_HOST` | `localhost` | Redis 位址 |
| `REDIS_PASSWORD` | （空） | 有設定密碼時才需要 |

## API

### 五種資料結構

| 資料結構 | Method | Path | Redis 指令 |
|---|---|---|---|
| **String** | `PUT` | `/api/redis/strings/{key}` | `SET`（帶 `ttlSeconds` 時為 `SET ... EX`） |
| | `GET` | `/api/redis/strings/{key}` | `GET` |
| **物件** | `PUT` / `GET` | `/api/redis/users/{id}` | `SET` / `GET`（JSON） |
| **Hash** | `GET` | `/api/redis/hashes/{key}` | `HGETALL` |
| | `PUT` / `GET` | `/api/redis/hashes/{key}/fields/{field}` | `HSET` / `HGET` |
| **List** | `GET` | `/api/redis/lists/{key}` | `LRANGE 0 -1` |
| | `POST` / `DELETE` | `/api/redis/lists/{key}/left`、`/right` | `LPUSH` `RPUSH` / `LPOP` `RPOP` |
| **Set** | `GET` / `POST` | `/api/redis/sets/{key}` | `SMEMBERS` / `SADD` |
| | `GET` | `/api/redis/sets/{key}/members/{member}` | `SISMEMBER` |
| **ZSet** | `GET` / `POST` | `/api/redis/zsets/{key}` | `ZRANGE ... WITHSCORES` / `ZADD` |
| | `GET` | `/api/redis/zsets/{key}/members/{member}` | `ZSCORE` |
| **Key** | `GET` | `/api/redis/keys/{key}` | `TTL` |
| | `PUT` | `/api/redis/keys/{key}/ttl` | `EXPIRE` |
| | `DELETE` | `/api/redis/keys/{key}` | `DEL` |
| | `POST` | `/api/redis/sample-data` | 建立範例資料 |

```bash
curl -X PUT http://localhost:8089/api/redis/strings/otp \
  -H "Content-Type: application/json" -d '{"value": "123456", "ttlSeconds": 300}'
curl http://localhost:8089/api/redis/strings/otp
# {"key":"otp","value":"123456","ttlSeconds":298}
```

### 實務情境

| 情境 | Method | Path |
|---|---|---|
| 排行榜：加分 | `POST` | `/api/redis/leaderboards/{board}/scores`（`{"player": "Amy", "points": 30}`） |
| 排行榜：前 N 名 | `GET` | `/api/redis/leaderboards/{board}?top=10` |
| 排行榜：查名次 | `GET` | `/api/redis/leaderboards/{board}/players/{player}` |
| 限流 | `GET` | `/api/redis/rate-limited`（header `X-Client-Id`） |
| 取得鎖 | `POST` | `/api/redis/locks/{name}`（`{"ttlSeconds": 30}`，回傳 token） |
| 釋放鎖 | `DELETE` | `/api/redis/locks/{name}`（header `X-Lock-Token`） |

### 錯誤回應

| 情境 | 狀態碼 |
|---|---|
| key、Hash 欄位、ZSet 成員不存在；List 已空 | `404` |
| Request Body 驗證失敗 | `400` |
| 對 key 使用錯誤的資料結構（Redis 回 `WRONGTYPE`，例如對 String 讀取 Hash） | `409` |
| 鎖已被他人持有；釋放鎖時 token 不符 | `409` |
| 超過限流次數 | `429` + `Retry-After` |
| 連不上 Redis | `503` |

所有錯誤皆為 ProblemDetail（`application/problem+json`）。

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**。Testcontainers 啟動臨時的 `redis:7-alpine`，以 `@ServiceConnection` 自動設定連線，不會連到本機的 Redis。

| 測試 | 內容 |
|---|---|
| `RedisDataApiTest` | 五種資料結構、TTL、404 / 400 / 409、JSON 中沒有 `@class` |
| `ScenarioApiTest` | 排行榜排序、限流 429 與 `Retry-After`、鎖的 token 驗證與自動過期、**10 個執行緒同時搶鎖只有 1 個成功** |

---

## Redis 教學

### 1. 五種資料結構與適用情境

| 結構 | 特性 | 常見用途 |
|---|---|---|
| **String** | 單一值，可設定過期 | 快取、驗證碼（OTP）、計數器（`INCR`）、分散式鎖 |
| **Hash** | 一個 key 底下多個 field | 物件的各個欄位、購物車（商品 → 數量） |
| **List** | 有順序、可重複、兩端進出 | 簡單佇列、最新 N 筆動態 |
| **Set** | 不重複、無順序 | 標籤、去重、共同好友（交集） |
| **ZSet** | 不重複、依分數排序 | 排行榜、延遲任務（分數 = 執行時間）、熱門排行 |

**key 命名慣例**：用冒號分層，例如 `user:1`、`user:profile:1`、`leaderboard:game`。RedisInsight 等工具會以資料夾方式顯示。

### 2. 物件要怎麼存：JSON 字串 vs Hash

| | JSON 字串（`UserController`） | Hash（`HashController`） |
|---|---|---|
| 讀寫 | 整個物件一次讀寫 | 可以只讀寫單一欄位 |
| 結構 | 可以有巢狀物件、陣列 | 只有一層 field-value |
| 適合 | 整筆一起使用的資料（快取） | 欄位常被個別更新（例如計數欄位 `HINCRBY`） |

### 3. 序列化與安全性：為什麼移除 default typing

**修正前**：

```java
objectMapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, ...);
```

存進 Redis 的 JSON 會帶上類別名稱，讀取時依照這個欄位建立物件：

```json
{"@class": "com.example.demo.entity.User", "name": "Amy", ...}
```

`LaissezFaire`（放任）代表**任何類別都允許**。這正是 Jackson 歷年反序列化漏洞的成因：只要有人能寫入 Redis（例如 Redis 沒設密碼、或其他服務被入侵），就能讓應用程式建立危險的類別（gadget chain），可能導致遠端執行程式碼。

**修正後**：讀取時**明確指定型別**，JSON 中不需要也不會帶類別名稱：

```java
template.setValueSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, User.class));
```

| 做法 | JSON 帶型別 | 安全性 | 適合 |
|---|---|---|---|
| 明確型別（本模組） | 否 | ✔ | 一個 key pattern 對應一種型別（大多數情況） |
| default typing + 白名單（`BasicPolymorphicTypeValidator` 只允許自己的套件） | 是 | 需謹慎設定 | 確實需要在同一個 key 存不同子類別 |
| default typing + `LaissezFaire`（修正前） | 是 | ✘ | **不要使用** |

> 純文字資料（String、Hash、List、Set、ZSet）使用 Spring Boot 自動建立的 `StringRedisTemplate`，在 redis-cli 或 RedisInsight 中直接就看得懂。修正前用 JSON serializer 存字串，會多一層引號（`"\"a\""`）。

### 4. 情境一：排行榜（ZSet）

```text
ZINCRBY leaderboard:game 30 Amy       → 加分（不存在時從 0 開始）
ZREVRANGE leaderboard:game 0 9 WITHSCORES → 前 10 名（REV：分數由高到低）
ZREVRANK leaderboard:game Amy          → 名次（從 0 開始）
```

用資料庫做排行榜通常要 `ORDER BY score DESC` 掃過整張表；ZSet 寫入時就維持排序，加分、查名次、取前 N 名都很快。

### 5. 情境二：API 限流（INCR + EXPIRE）

```text
第 1 次請求：INCR rate-limit:{client} → 1，同時設定 EXPIRE 60 秒
第 2～5 次： INCR → 2～5，允許
第 6 次：    INCR → 6，超過上限 → 429，Retry-After = key 剩餘秒數
60 秒後：    key 過期消失，重新計算
```

**為什麼要用 Lua script**：

```java
long count = redis.increment(key);      // ① INCR
if (count == 1) redis.expire(key, 60);  // ② EXPIRE
// 如果應用程式在 ① 與 ② 之間當掉，key 永遠沒有過期時間，這個呼叫端會被永久封鎖
```

Redis 執行 Lua script 時**不會穿插其他指令**，INCR 與 EXPIRE 一起完成：

```lua
local count = redis.call('INCR', KEYS[1])
if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
return count
```

> 這是「固定時間窗」演算法，簡單但在時間窗交界處可能短時間內通過 2 倍的請求。更平滑的做法有滑動視窗（ZSet 記錄每次請求的時間）與 Token Bucket；實務上也可以使用 Bucket4j、Resilience4j 或 Spring Cloud Gateway 內建的 `RequestRateLimiter`。

### 6. 情境三：分散式鎖

```text
取得：SET lock:{name} {隨機 token} NX EX 30
        NX：key 不存在才設定 → 同一時間只有一個人成功
        EX：30 秒後自動過期 → 持有者當掉時不會永久鎖死
釋放：GET 比對 token，相符才 DEL（Lua script，原子操作）
```

**為什麼要比對 token**：

```text
A 取得鎖（30 秒）→ A 處理太久，鎖過期 → B 取得鎖
→ A 處理完執行 DEL → 刪掉的是 B 的鎖 ✘
```

每次取得鎖都產生隨機 token，釋放時「比對 + 刪除」必須是原子操作（否則比對完、刪除前鎖剛好過期又被別人取得），因此也用 Lua script。

**實務上建議使用 [Redisson](https://redisson.org/)**：

| 問題 | 本模組 | Redisson |
|---|---|---|
| 處理時間超過 TTL | 鎖過期，其他人可能同時進入 | **watchdog 自動續期** |
| 同一執行緒重複取得 | 不支援 | 可重入鎖 |
| 等待鎖釋放 | 立即失敗（409） | 可設定等待時間、公平鎖 |

### 7. Redis 的錯誤：WRONGTYPE

每個 key 只能是一種資料結構。對 String 的 key 執行 `HGETALL`，Redis 會回傳錯誤：

```text
WRONGTYPE Operation against a key holding the wrong kind of value
```

Spring 將它轉成 `RedisSystemException`，本模組對應為 **409 Conflict**（請求與資源目前的狀態衝突）。

### 8. TTL 的特殊值

| `TTL key` 回傳 | 意義 | 本模組 API |
|---|---|---|
| 正數 | 剩餘秒數 | `ttlSeconds: 58` |
| `-1` | 存在但沒有設定過期 | `ttlSeconds: null` |
| `-2` | key 不存在 | `404` |

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 測試，固定原本行為（包含 JSON 帶 `@class`） |
| 2 | Java 17 → 21、Spring Boot 3.5.0 → 3.5.16、Gradle 8.14 → 8.14.3 |
| 3 | Spring Boot → 4.1.1（Spring Data Redis 4）、Gradle → 9.8.0、Testcontainers 2.0 |
| 4 | **安全性**：移除 `LaissezFaire` default typing，改為明確型別的 Jackson 3 serializer；移除 Jackson 2 依賴；`Duration` 取代 deprecated API |
| 5 | API 改為 `/api/redis/...`、JSON 回應、ProblemDetail（404 / 409 / 503）、Bean Validation；port 8015 → 8089 |
| 6 | 新增排行榜、限流、分散式鎖三個情境 |
| 7 | 新增 Dockerfile 與 docker-compose（Redis + RedisInsight），修正 `gradlew` 執行權限 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | `8015`（與 Spring_JPA 的 Docker port 衝突） | `8089` |
| 路徑 | `/redis/string/{key}`、`/redis/user/{id}`… | `/api/redis/strings/{key}`、`/api/redis/users/{id}`… |
| 回應 | 中文字串（「設置成功: ...」） | JSON |
| key 不存在 | `200` + 空內容 | `404` |
| Redis 中的物件 | JSON 帶 `@class` | 一般 JSON |
| Redis 中的字串 | JSON 字串（多一層引號） | 純文字 |
| 測試資料 | `POST /redis/init-test-data` | `POST /api/redis/sample-data` |

### 升級時學到的事

- **Testcontainers 與 Docker 版本**：Boot 3.5.0 管理的 Testcontainers 1.21.0 無法連線到 Docker 29（回傳 400、Docker 資訊全為空），1.21.4 起才相容。Boot 3.5.16 已管理 1.21.4。
- **Spring Data Redis 4 + Jackson 3**：`Jackson2JsonRedisSerializer` / `GenericJackson2JsonRedisSerializer` 標記為**將被移除**，改用 `JacksonJsonRedisSerializer` / `GenericJacksonJsonRedisSerializer`（`tools.jackson` 套件）。升級時先開啟 `-Xlint:deprecation`，依編譯警告逐一處理。
- **`set(key, value, long, TimeUnit)`、`expire(key, long, TimeUnit)` deprecated**：改用 `Duration` 版本。
- **沒有作用的連線池設定**：`spring.data.redis.lettuce.pool.*` 需要 `commons-pool2` 依賴才會生效，原本沒有加入，設定被默默忽略。Lettuce 的單一連線是執行緒安全的，一般情況不需要連線池。
- **README 要與實際一致**：原本提到的 docker-compose 並不存在，port 也前後不一。
