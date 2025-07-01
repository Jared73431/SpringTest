# Spring Redis Cache 練習專案

這是一個使用 Spring Boot 整合 Redis 快取的練習專案，展示了如何在 Spring 應用程式中使用各種快取註解來提升應用程式效能。

## 專案概述

本專案實現了一個簡單的使用者管理系統，包含：
- 使用者的 CRUD 操作
- Redis 快取整合
- Spring Cache 註解的使用
- 手動快取操作

## 技術棧

- **Spring Boot** - 主要框架
- **Spring Data JPA** - 資料庫操作
- **Redis** - 快取資料庫
- **Spring Cache** - 快取抽象層
- **Lombok** - 簡化程式碼

## 專案結構

```
src/main/java/com/example/demo/
├── config/
│   └── RedisConfig.java          # Redis 配置
├── controller/
│   └── UserController.java       # REST API 控制器
├── entity/
│   └── User.java                 # 使用者實體
├── repository/
│   └── UserRepository.java       # 資料庫操作接口
└── service/
    └── UserService.java          # 業務邏輯服務
```

## 快取註解說明

### @Cacheable

**用途**：用於查詢操作，將方法的返回值儲存到快取中。如果快取中已存在相同 key 的資料，則直接返回快取中的資料，不執行方法。

**屬性**：
- `value` 或 `cacheNames`：指定快取的名稱
- `key`：指定快取的 key（支援 SpEL 表達式）
- `condition`：快取的條件（滿足條件才快取）
- `unless`：排除快取的條件（滿足條件則不快取）

**範例**：
```java
// 根據 ID 查詢使用者，快取 key 為 users::1
@Cacheable(value = "users", key = "#id")
public User findById(Long id) {
    return userRepository.findById(id).orElse(null);
}

// 根據 email 查詢，自訂快取 key
@Cacheable(value = "users", key = "'email:' + #email")
public User findByEmail(String email) {
    return userRepository.findByEmail(email).orElse(null);
}
```

### @CachePut

**用途**：用於更新操作，每次都會執行方法並將結果更新到快取中。與 @Cacheable 不同，@CachePut 不會跳過方法執行。

**屬性**：與 @Cacheable 相同

**範例**：
```java
// 更新使用者資料並同步更新快取
@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

### @CacheEvict

**用途**：用於刪除操作，從快取中移除指定的資料。

**屬性**：
- `value` 或 `cacheNames`：指定快取的名稱
- `key`：指定要刪除的快取 key
- `allEntries`：是否清除所有快取項目（預設為 false）
- `beforeInvocation`：是否在方法執行前清除快取（預設為 false）

**範例**：
```java
// 刪除特定使用者的快取
@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}

// 清除所有使用者快取
@CacheEvict(value = "users", allEntries = true)
public void clearAllCache() {
    System.out.println("清除所有用戶快取");
}
```

### @Caching

**用途**：組合多個快取註解，可以在一個方法上同時使用多個快取操作。

**屬性**：
- `cacheable`：@Cacheable 註解陣列
- `put`：@CachePut 註解陣列
- `evict`：@CacheEvict 註解陣列

**範例**：
```java
// 同時快取多個 key 並清除舊快取
@Caching(
    put = {
        @CachePut(value = "users", key = "#user.id"),
        @CachePut(value = "users", key = "'email:' + #user.email")
    },
    evict = {
        @CacheEvict(value = "allUsers", allEntries = true)
    }
)
public User updateUserWithMultipleCache(User user) {
    return userRepository.save(user);
}

// 根據不同條件快取
@Caching(
    cacheable = {
        @Cacheable(value = "users", key = "#id", condition = "#id > 0"),
        @Cacheable(value = "userDetails", key = "#id", condition = "#id > 100")
    }
)
public User findUserWithConditions(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

### @CacheConfig

**用途**：類別級別的快取配置，可以為整個類別設定共通的快取配置。

**範例**：
```java
@Service
@CacheConfig(cacheNames = "users")
public class UserService {
    
    @Cacheable(key = "#id")  // 不需要再指定 value
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

## API 端點

### 使用者管理
- `GET /api/users/{id}` - 根據 ID 查詢使用者
- `GET /api/users/email/{email}` - 根據 email 查詢使用者
- `GET /api/users` - 查詢所有使用者
- `POST /api/users` - 建立新使用者
- `PUT /api/users/{id}` - 更新使用者
- `DELETE /api/users/{id}` - 刪除使用者

### 快取管理
- `POST /api/users/cache/clear` - 清除所有使用者快取
- `POST /api/users/cache/{key}` - 設定自訂快取
- `GET /api/users/cache/{key}` - 取得自訂快取

## 配置說明

### Redis 配置 (RedisConfig.java)

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用 JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))  // 10分鐘過期
                .disableCachingNullValues();       // 不快取 null 值
        
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
```

## 快取策略最佳實踐

1. **查詢操作使用 @Cacheable**
   - 適用於讀取頻繁且資料變化不大的場景
   - 注意快取穿透問題

2. **更新操作使用 @CachePut**
   - 確保快取與資料庫資料一致性
   - 避免髒讀問題

3. **刪除操作使用 @CacheEvict**
   - 及時清除過期的快取資料
   - 考慮是否需要清除相關的快取

4. **複雜場景使用 @Caching**
   - 一次操作影響多個快取時
   - 需要同時進行多種快取操作時

## 測試方法

1. 啟動 Redis 服務
2. 啟動 Spring Boot 應用程式
3. 使用 Postman 或 curl 測試 API 端點
4. 觀察控制台輸出，確認快取是否生效

## 注意事項

- 確保 Redis 服務正在運行
- 快取的 key 需要具有唯一性
- 注意快取雪崩和快取穿透問題
- 合理設定快取過期時間
- 被快取的物件必須可序列化

## 依賴項

## 依賴項

```gradle
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-cache'
	implementation 'org.springframework.boot:spring-boot-starter-data-jdbc'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-data-redis'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	compileOnly 'org.projectlombok:lombok'
	developmentOnly 'org.springframework.boot:spring-boot-devtools'
	runtimeOnly 'org.postgresql:postgresql'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## 擴展功能

- 快取監控和統計
- 分散式快取一致性
- 快取預熱機制
- 快取降級策略