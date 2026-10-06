# Spring Caffeine - 本機記憶體快取

使用 [Caffeine](https://github.com/ben-manes/caffeine) 作為 Spring Cache 的快取實作。

這個模組與 [Spring_Cache](../Spring_Cache)（Redis）是一組對照：**`UserService`、快取註解、Controller、DTO 完全相同**，只有 `CacheManager` 不同。用來理解：

1. **Spring Cache 的抽象**：換快取實作，商業邏輯一行都不用改
2. **本機快取 vs 分散式快取**：速度、一致性、適用情境的取捨

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| 快取 | Caffeine（版本由 Spring Boot 管理） |
| 資料庫 | PostgreSQL（與 Spring_Cache 共用資料庫 `test2` 的 `users` 資料表） |
| 監控 | Spring Boot Actuator + Micrometer 快取指標 |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Testcontainers 2.0（PostgreSQL）、TestRestTemplate |

## 與 Spring_Cache 的差異

| 檔案 | Spring_Cache（Redis） | Spring_Caffeine |
|---|---|---|
| `UserService`、`UserController`、DTO、Entity | 相同 | 相同 |
| 快取設定 | `RedisConfig`：`RedisCacheManager` | `CacheConfig`：`CaffeineCacheManager` |
| 依賴 | `spring-boot-starter-data-redis` | `com.github.ben-manes.caffeine:caffeine` |
| 需要的服務 | PostgreSQL + **Redis** | PostgreSQL |

## 快取設定

| 快取 | 上限 | 存活時間 | 內容 |
|---|---|---|---|
| `users` | 1000 筆 | 10 分鐘 | `UserDto`（依 id） |
| `usersByEmail` | 1000 筆 | 10 分鐘 | `UserDto`（依 email） |
| `allUsers` | 1 筆 | 1 分鐘 | `List<UserDto>` |

可在 `application.properties` 調整：

```properties
app.cache.users.maximum-size=1000
app.cache.users.expire-after-write=10m
```

## 執行方式

### Docker（兩個實例）

```bash
docker compose up --build
```

會啟動 **兩個應用程式實例**（`app-a`：8091、`app-b`：8092），共用同一個資料庫，但各自有一份快取：

```bash
curl -X POST http://localhost:8091/api/users -H "Content-Type: application/json" \
     -d '{"name":"Amy","email":"amy@example.com","age":20}'
curl http://localhost:8091/api/users/1          # A 快取了 Amy
curl http://localhost:8092/api/users/1          # B 也快取了 Amy

curl -X PUT http://localhost:8091/api/users/1 -H "Content-Type: application/json" \
     -d '{"name":"Amy Lin","email":"amy@example.com","age":21}'
curl http://localhost:8091/api/users/1          # A：Amy Lin
curl http://localhost:8092/api/users/1          # B：仍是 Amy（要等 10 分鐘過期）
```

這就是**本機快取最大的代價**。換成 Spring_Cache（Redis）就不會發生，因為所有實例共用同一份快取。

### 本機執行

需要 JDK 21 與 PostgreSQL（資料庫 `test2`），不需要 Redis：

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

## API

與 Spring_Cache 相同，port 為 `8091`：

| Method | Path | 說明 |
|---|---|---|
| `GET` | `/api/users/{id}`、`/api/users/email/{email}`、`/api/users` | 查詢（`@Cacheable`） |
| `POST` | `/api/users` | 新增 |
| `PUT` / `DELETE` | `/api/users/{id}` | 修改 / 刪除 |
| `DELETE` | `/api/users/cache` | 清除所有使用者快取 |

### 監控（Actuator）

```bash
curl http://localhost:8091/actuator/caches                                            # 快取清單
curl "http://localhost:8091/actuator/metrics/cache.gets?tag=name:users&tag=result:hit"   # 命中次數
curl "http://localhost:8091/actuator/metrics/cache.gets?tag=name:users&tag=result:miss"  # 未命中次數
curl "http://localhost:8091/actuator/metrics/cache.evictions?tag=name:users"             # 淘汰次數
```

## 測試

```bash
./gradlew test
```

> ⚠️ 需要 **Docker 正在執行**（Testcontainers 啟動 PostgreSQL）。

| 測試 | 內容 |
|---|---|
| `UserCacheApiTest` | 與 Spring_Cache 相同的快取一致性案例：同一套註解，換成 Caffeine 後行為必須一致 |
| `CaffeineBehaviorTest` | Caffeine 特有：**容量上限淘汰**（上限 2 筆，查 5 筆後只剩 2 筆）、**過期**（1 秒後失效）、**命中統計**、**Actuator 指標** |

---

## Caffeine 教學

### 1. Spring Cache 是抽象層，Caffeine 是實作

```text
@Cacheable / @CachePut / @CacheEvict      ← 寫法：Spring Cache（抽象層）
                 │
            CacheManager                  ← 換這一層，就換了快取存放的位置
     ┌───────────┼───────────────┐
  Redis       Caffeine        Simple（ConcurrentHashMap）
```

### 2. Redis vs Caffeine

| | Redis（Spring_Cache） | Caffeine（本模組） |
|---|---|---|
| 存放位置 | 獨立的 Redis 伺服器 | 應用程式的 JVM 記憶體 |
| 速度 | 要經過網路，還要序列化 | **極快**：直接存取記憶體中的 Java 物件，不序列化 |
| 多台應用程式 | **共用同一份** | **各自一份**，修改後其他台在過期前會讀到舊資料 |
| 重啟後 | 資料還在 | 消失（需要重新暖機） |
| 容量 | Redis 伺服器的記憶體 | 佔用應用程式的 heap，**一定要設上限** |
| 額外的服務 | 需要架設、維運 Redis | 不需要 |
| 監控 | Redis 本身的工具 | `recordStats()` + Actuator |

**適合 Caffeine 的資料**：
- 很少變動、短暫不一致也沒關係：設定值、代碼表、分類清單、匯率
- 每個請求都會讀、量又不大的熱門資料
- 單一實例的應用程式

**不適合 Caffeine 的資料**：多台都必須立即看到最新值的資料（例如庫存、餘額），或資料量大到放不進 heap。

### 3. 一定要設上限：不要用 Simple 快取

只加 `@EnableCaching`、沒有任何快取依賴時，Spring Boot 使用 **Simple 快取**（`ConcurrentHashMap`）：

- **沒有容量上限**、**不會過期**
- 資料只會一直累積，最後 `OutOfMemoryError`

Caffeine 就是用來取代它的正式做法：

```java
Caffeine.newBuilder()
        .maximumSize(1000)                        // 最多 1000 筆
        .expireAfterWrite(Duration.ofMinutes(10)) // 寫入 10 分鐘後過期
        .recordStats()                            // 記錄命中率
        .build();
```

#### 淘汰演算法：W-TinyLFU

超過上限時要淘汰哪一筆？

| 演算法 | 依據 | 問題 |
|---|---|---|
| LRU | 最久沒被使用 | 一次大量掃描（例如批次查詢全部資料）會把常用的熱門資料擠掉 |
| LFU | 使用次數最少 | 過去很熱門、現在已經沒人用的資料會一直佔位 |
| **W-TinyLFU**（Caffeine） | 綜合近期與頻率，新資料要「比被淘汰的更常用」才能留下 | 命中率接近最佳，記憶體成本低 |

> 淘汰是**非同步**進行的，所以 `estimatedSize()` 可能短暫超過上限；測試中呼叫 `cleanUp()` 讓維護工作立即完成。

### 4. 本模組的設定重點

```java
CaffeineCacheManager caffeine = new CaffeineCacheManager();
caffeine.setAllowNullValues(false);                          // 不快取 null
caffeine.registerCustomCache("users", build(1000, 10 min));  // 每個快取各自的上限與存活時間
caffeine.setCacheNames(List.of());                           // 固定快取清單：名稱打錯字時立即出錯
return new TransactionAwareCacheManagerProxy(caffeine);      // 交易提交後才寫入 / 清除
```

| 設定 | 原因 |
|---|---|
| `registerCustomCache` | 不同資料適合不同的上限與存活時間；`spring.cache.caffeine.spec` 只能設定一組共用的規格 |
| `setCacheNames(List.of())` | 預設為動態模式：註解中出現沒見過的名稱時會自動建立，打錯字就會默默多一個快取 |
| `TransactionAwareCacheManagerProxy` | `CaffeineCacheManager` 沒有 Redis 版本的 `transactionAware()`，改用 Spring 提供的 proxy 包裝 |

> 只需要一組共用規格時，也可以只寫設定檔，不必自訂 `CacheManager`：
> ```properties
> spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m,recordStats
> spring.cache.cache-names=users,usersByEmail,allUsers
> ```

### 5. 快取的是物件本身：一定要不可變

Caffeine 不序列化，**快取中存放的就是那個 Java 物件**，所有請求拿到的是同一個實例。如果快取的是可變物件（例如 JPA Entity），有人呼叫 `setName()` 就會**直接改到快取中的資料**。本模組快取不可變的 `record`（`UserDto`），可以安全地共用。

### 6. 進階：兩層快取（L1 Caffeine + L2 Redis）

```text
請求 → Caffeine（L1，本機，極快）→ Redis（L2，共用）→ 資料庫
```

兼顧速度與多台共用，但要處理「某台修改後，通知其他台清除 L1」（常用 Redis Pub/Sub 廣播），複雜度高很多。現成方案有 JetCache、Redisson 的 local cache 等。

### 7. 其他 Caffeine 功能

| 功能 | 說明 |
|---|---|
| `expireAfterAccess` | 一段時間沒被讀取就過期（`expireAfterWrite` 是寫入後固定時間） |
| `refreshAfterWrite` | 過了指定時間後，下一次讀取時在背景重新載入，**先回傳舊值**，避免大量請求同時等待 |
| `weakKeys` / `softValues` | 交給 GC 決定何時回收，一般不建議（行為難預測） |
| `AsyncCache` | 非同步載入，回傳 `CompletableFuture` |
