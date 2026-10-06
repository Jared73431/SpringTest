# Spring Cache - 以註解宣告快取（Redis）

使用 Spring Cache 的 `@Cacheable` / `@CachePut` / `@CacheEvict` / `@Caching` 宣告快取行為，快取存放在 Redis。以使用者 CRUD 為例，示範**怎麼讓快取與資料庫保持一致**。

相關模組：
- [Spring_Redis](../Spring_Redis)：直接用 `RedisTemplate` 操作 Redis
- [Spring_Caffeine](../Spring_Caffeine)：同一套註解與 UserService，改用本機記憶體快取 Caffeine（Redis vs Caffeine 的比較見該模組）

這是 2025 年加入的練習，已完成現代化（Spring Boot 3.5 → 4.1），過程中修正了多個**快取不一致**與**安全性**問題，詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（Spring Data Redis 4、Spring Data JPA） |
| 快取 | Redis 7（`RedisCacheManager`，Jackson 3 序列化） |
| 資料庫 | PostgreSQL |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL + Redis）、TestRestTemplate |

## 架構

```text
UserController（/api/users）
  ↓
UserService：@Cacheable / @CachePut / @CacheEvict（Spring 以 AOP proxy 攔截方法呼叫）
  ├─ 快取命中 → 直接回傳 Redis 中的資料，不執行方法
  └─ 快取未命中 → 執行方法查 PostgreSQL → 結果寫入 Redis
```

| 快取名稱 | key 範例 | 內容 | 寫入 | 清除 |
|---|---|---|---|---|
| `users` | `app-cache::users::1` | `UserDto` | 查詢、修改（`@CachePut`） | 刪除 |
| `usersByEmail` | `app-cache::usersByEmail::a@x.com` | `UserDto` | 查詢 | 修改、刪除 |
| `allUsers` | `app-cache::allUsers::all` | `List<UserDto>` | 查詢全部 | 新增、修改、刪除 |

所有快取 TTL 為 10 分鐘。

## 執行方式

### Docker（建議）

```bash
docker compose up --build
```

| 服務 | Port | 說明 |
|---|---|---|
| `app` | `8090` | 本應用程式 |
| `redisinsight` | `5540` | 觀察快取 key、內容與剩餘 TTL（Add Redis database：Host 填 `redis`、Port 填 `6379`） |
| `postgres`、`redis` | 不開放 | 避免與本機的服務衝突 |

```bash
curl -X POST http://localhost:8090/api/users -H "Content-Type: application/json" \
     -d '{"name":"Amy","email":"amy@example.com","age":20}'
curl http://localhost:8090/api/users/1
curl http://localhost:8090/api/users/1      # 第二次：log 中不會出現「從資料庫查詢」與 SQL（快取命中）
```

### 本機執行

需要 JDK 21、PostgreSQL（資料庫 `test2`）與 Redis：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

| 環境變數 | 預設值 | 說明 |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL 位址 |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` | 資料庫帳密 |
| `REDIS_HOST` | `localhost` | Redis 位址 |

## API

| Method | Path | 說明 | 快取 | 成功 | 失敗 |
|---|---|---|---|---|---|
| `GET` | `/api/users/{id}` | 查詢 | `@Cacheable` | `200` | `404` |
| `GET` | `/api/users/email/{email}` | 依 email 查詢 | `@Cacheable` | `200` | `404` |
| `GET` | `/api/users` | 查詢全部 | `@Cacheable` | `200` | |
| `POST` | `/api/users` | 新增 | 清除 `allUsers` | `201` + `Location` | `400` |
| `PUT` | `/api/users/{id}` | 修改 | 更新 `users`，清除另外兩個 | `200` | `400` / `404` |
| `DELETE` | `/api/users/{id}` | 刪除 | 清除三個快取 | `204` | `404` |
| `DELETE` | `/api/users/cache` | 清除所有使用者快取 | | `204` | |

Request Body（`POST` / `PUT`）：

```json
{ "name": "Amy", "email": "amy@example.com", "age": 20 }
```

錯誤皆為 ProblemDetail；Redis 連不上時回 `503`。

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**（Testcontainers 啟動 PostgreSQL 與 Redis，不會連到本機的服務）。

`UserCacheApiTest` 判斷「資料來自快取」的方式：查詢一次後**繞過 Service 直接修改資料庫**，再查詢時若仍是舊值，代表結果來自快取。修正前的每個 Bug 都有對應的測試證明已修正。

---

## Spring Cache 教學

### 1. 四個註解

| 註解 | 行為 | 用在 |
|---|---|---|
| `@Cacheable` | 快取有就直接回傳、**不執行方法**；沒有才執行並寫入 | 查詢 |
| `@CachePut` | **一定執行方法**，並用結果更新快取 | 修改 |
| `@CacheEvict` | 清除快取（`allEntries = true` 清除整個快取） | 新增、修改、刪除 |
| `@Caching` | 在同一個方法上組合多個上述註解 | 一次影響多個快取 |

```java
@Transactional
@Caching(
        put = @CachePut(cacheNames = "users", key = "#id"),
        evict = {
                @CacheEvict(cacheNames = "usersByEmail", allEntries = true),
                @CacheEvict(cacheNames = "allUsers", allEntries = true) })
public UserDto update(Long id, UserRequest request) { ... }
```

### 2. 最重要的原則：每個寫入操作都要處理「所有相關的快取」

修正前只處理了 `users`（依 id）這一個快取：

| 快取 | 新增 | 修改 | 刪除 | 修正前的結果 |
|---|---|---|---|---|
| 依 id | — | ✔ 更新 | ✔ 清除 | 正確 |
| 依 email | — | ✘ | ✘ | 修改、刪除後仍查到**舊資料** |
| 全部 | ✘ | ✘ | ✘ | 列表**一直不變**，直到 TTL 10 分鐘 |

**同一筆資料被快取在幾個地方，每個寫入操作就要處理幾個地方。** 新增一種查詢方式（多一個快取）時，所有寫入方法都要跟著檢查。

#### 註解的限制：舊 email 的 key

修改時 email 可能也改了，要清除的是**舊 email** 的快取，但註解只拿得到方法參數（新的值）。本模組用 `allEntries = true` 清除整個 `usersByEmail` 快取，簡單且正確，代價是其他使用者的 email 快取也被清掉。需要精準清除時，可以在方法中先查出舊資料，再用 `CacheManager` 手動 `evict(oldEmail)`。

### 3. 查不到資料：不要讓 null 進快取

修正前：

```java
@Cacheable(value = "users", key = "#id")
public User findById(Long id) {
    return userRepository.findById(id).orElse(null);   // 查不到回傳 null
}
// CacheManager 設定了 disableCachingNullValues()
```

Spring 嘗試把 null 寫入快取時拋出：

```text
IllegalArgumentException: Cache 'users' does not allow 'null' values;
Avoid storing null via '@Cacheable(unless="#result == null")' ...
```

結果**查無資料回 500**，Controller 的 `notFound()` 永遠不會執行。修正後：

```java
@Cacheable(cacheNames = "users", key = "#id", unless = "#result == null")
public Optional<UserDto> findById(Long id) { ... }
```

- 回傳 `Optional` 時，`#result` 指的是 `Optional` 裡面的值，查不到時 `#result == null`，不寫入快取
- Controller 以 `orElseThrow` 轉成 404

> **快取 null（快取穿透的防護）**：如果有人大量查詢不存在的 id，每次都會打到資料庫。這種情況可以改為快取「查無資料」的結果（允許 null + 較短的 TTL），或使用 Bloom Filter。本模組選擇不快取 null，以正確性為優先。

### 4. 快取什麼：DTO，不是 Entity

| | 快取 Entity（修正前） | 快取 DTO（修正後） |
|---|---|---|
| 內容 | 可能有 Hibernate 代理、lazy 欄位 | 單純的資料 |
| 與資料表的關係 | 資料表一改，快取中的舊 JSON 可能無法反序列化 | 只有對外需要的欄位 |
| 序列化 | 需要 `Serializable` 或型別資訊 | `record`，Jackson 直接處理 |

### 5. 序列化與安全性

修正前使用 `GenericJackson2JsonRedisSerializer`，JSON 帶有類別名稱（`@class`），讀取時允許建立任意類別；同時有一個 `POST /api/users/cache/{key}` 可以寫入**任意 key**：

```text
POST /api/users/cache/users::1   body: "hacked"
GET  /api/users/1                → ClassCastException: String cannot be cast to User（快取被竄改）
```

修正後：
- 移除可寫入任意 key 的 API（手動操作 Redis 的示範見 [Spring_Redis](../Spring_Redis)）
- 每個快取綁定明確型別，Redis 中只有一般的 JSON：

```java
.withCacheConfiguration("users", defaults.serializeValuesWith(
        SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, UserDto.class))))
```

> Spring Boot 4 的 `GenericJacksonJsonRedisSerializer`（Jackson 3）**預設不再帶型別資訊**；要恢復舊行為必須明確呼叫 `enableUnsafeDefaultTyping()`，方法名稱本身就標示了風險。

### 6. 設定的陷阱：自訂 CacheManager 後，`spring.cache.redis.*` 不會生效

修正前 `application.properties` 寫了：

```properties
spring.cache.redis.time-to-live=600000
spring.cache.redis.key-prefix=app-cache::
```

但專案同時自己定義了 `CacheManager` Bean，Spring Boot 的自動設定就**不會套用**這些屬性，`app-cache::` 前綴一直沒有生效，也沒有任何警告。設定只能選一個地方：本模組全部寫在 `RedisConfig`。

同一個設定檔還有其他沒有作用的設定：

| 設定 | 問題 |
|---|---|
| `Server.port:8015` | `S` 大寫，Spring 不認得 → 實際 port 是 8080 |
| `spring.redis.*` | Spring Boot 3 起改名為 `spring.data.redis.*` |
| `spring.redis.jedis.pool.*` | 專案使用 Lettuce，不是 Jedis |

> 檢查設定是否生效：Spring Boot Actuator 的 `/actuator/configprops`、`/actuator/env`，或在測試中直接驗證行為（例如本模組驗證 Redis key 的前綴）。

### 7. 快取與交易：`transactionAware()`

```java
RedisCacheManager.builder(cacheWriter)
        ...
        .transactionAware()
        .build();
```

在 `@Transactional` 方法中，快取的寫入與清除會**延後到交易提交之後**。交易失敗 rollback 時，快取不會留下沒有寫進資料庫的資料。

### 8. Spring Data Redis 4：清除快取預設是非同步的

升級後跑完整測試時，「清除快取」的測試**時好時壞**。追查原因：

- `@CacheEvict(allEntries = true)` 呼叫 `Cache.clear()`（`beforeInvocation = true` 時才呼叫立即的 `invalidate()`）
- Spring Data Redis 4 搭配 Lettuce 時，`clear()` 以**非同步**方式執行（回傳 `CompletableFuture`）
- 請求已經回應，快取卻還沒真的清掉，緊接著的查詢會讀到舊資料

解法：

```java
RedisCacheWriter cacheWriter = RedisCacheWriter.create(factory, writer -> writer.immediateWrites());
```

**時好時壞的測試通常代表真實的併發問題**，不要用「重跑一次」帶過。

### 9. 其他注意事項

- **自我呼叫不會觸發快取**：同一個類別內 `this.findById()` 不會經過 Spring 的 proxy，快取註解不會生效
- **TTL 是最後防線**：即使漏清快取，資料最多也只會舊 TTL 那麼久
- **快取的 key 要能唯一識別結果**：`findAll()` 沒有參數，本模組指定 `key = "'all'"`，比預設的 `SimpleKey []` 好讀

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上 Testcontainers 測試，證明快取不一致、查無資料 500、快取污染、key 前綴無效等問題 |
| 2 | Java 17 → 21、Spring Boot 3.5.3 → 3.5.16、Gradle 8.14.2 → 8.14.3 |
| 3 | Spring Boot → 4.1.1（Spring Data Redis 4）、Gradle → 9.8.0、Testcontainers 2.0；Jackson 2 serializer 改為 Jackson 3 |
| 4 | **安全性**：移除可寫入任意 key 的 API；快取 DTO 並綁定明確型別；`immediateWrites()` 修正非同步清除 |
| 5 | 修正快取不一致、查無資料 404、`transactionAware()`、清除無效設定、REST 語意；port → 8090 |
| 6 | 新增 Dockerfile 與 docker-compose（PostgreSQL + Redis + RedisInsight） |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Port | 設定 8015，因大小寫錯誤實際為 8080 | `8090` |
| 查無使用者 | `500` | `404` |
| 修改 / 刪除後依 email 查詢 | 舊資料 | 最新資料 |
| 新增 / 修改 / 刪除後查詢全部 | 舊列表 | 最新列表 |
| 修改 / 刪除不存在的使用者 | 500 / 204 | `404` |
| 新增 | `200` | `201` + `Location` |
| 清除快取 | `POST /api/users/cache/clear` | `DELETE /api/users/cache` |
| 讀寫任意 Redis key | `POST` / `GET /api/users/cache/{key}` | 移除 |
| Redis 中的快取 key | `users::1` | `app-cache::users::1` |
| Redis 中的快取內容 | JSON 帶 `@class` | 一般 JSON |

> 升級後第一次啟動時，Redis 中舊格式的快取（`users::1`）不會被讀取，等 TTL 到期自動消失；也可以直接清空 Redis。
