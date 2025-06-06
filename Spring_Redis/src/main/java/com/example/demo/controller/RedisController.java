package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.User;
import com.example.demo.service.RedisService;

@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    // ========== String 操作測試 ==========

    @PostMapping("/string/{key}")
    public String setString(@PathVariable String key, @RequestBody String value) {
        redisService.setString(key, value);
        return "設置成功: " + key + " = " + value;
    }

    @GetMapping("/string/{key}")
    public String getString(@PathVariable String key) {
        return redisService.getString(key);
    }

    @PostMapping("/string/{key}/expire")
    public String setStringWithExpire(@PathVariable String key,
                                      @RequestParam String value,
                                      @RequestParam long seconds) {
        redisService.setString(key, value, seconds, TimeUnit.SECONDS);
        return "設置成功並設置過期時間: " + key + " = " + value + " (過期時間: " + seconds + "秒)";
    }

    // ========== Object 操作測試 ==========

    @PostMapping("/user/{id}")
    public String setUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        redisService.setObject("user:" + id, user);
        return "用戶保存成功: " + user.toString();
    }

    @GetMapping("/user/{id}")
    public Object getUser(@PathVariable Long id) {
        return redisService.getObject("user:" + id);
    }

    // ========== Hash 操作測試 ==========

    @PostMapping("/hash/{key}/{field}")
    public String setHash(@PathVariable String key,
                          @PathVariable String field,
                          @RequestBody String value) {
        redisService.setHash(key, field, value);
        return "Hash 設置成功: " + key + "." + field + " = " + value;
    }

    @GetMapping("/hash/{key}/{field}")
    public Object getHash(@PathVariable String key, @PathVariable String field) {
        return redisService.getHash(key, field);
    }

    @GetMapping("/hash/{key}")
    public Map<Object, Object> getAllHash(@PathVariable String key) {
        return redisService.getAllHash(key);
    }

    // ========== List 操作測試 ==========

    @PostMapping("/list/{key}/left")
    public String leftPushList(@PathVariable String key, @RequestBody String value) {
        redisService.leftPushList(key, value);
        return "從左邊添加到列表: " + key + " <- " + value;
    }

    @PostMapping("/list/{key}/right")
    public String rightPushList(@PathVariable String key, @RequestBody String value) {
        redisService.rightPushList(key, value);
        return "從右邊添加到列表: " + key + " -> " + value;
    }

    @DeleteMapping("/list/{key}/left")
    public Object leftPopList(@PathVariable String key) {
        return redisService.leftPopList(key);
    }

    @DeleteMapping("/list/{key}/right")
    public Object rightPopList(@PathVariable String key) {
        return redisService.rightPopList(key);
    }

    @GetMapping("/list/{key}")
    public List<Object> getList(@PathVariable String key) {
        return redisService.getListRange(key, 0, -1);
    }

    // ========== Set 操作測試 ==========

    @PostMapping("/set/{key}")
    public String addToSet(@PathVariable String key, @RequestBody String[] values) {
        redisService.addToSet(key, (Object[]) values);
        return "添加到集合成功: " + key;
    }

    @GetMapping("/set/{key}")
    public Set<Object> getSet(@PathVariable String key) {
        return redisService.getSetMembers(key);
    }

    @GetMapping("/set/{key}/contains/{value}")
    public boolean containsInSet(@PathVariable String key, @PathVariable String value) {
        return redisService.isSetMember(key, value);
    }

    // ========== ZSet 操作測試 ==========

    @PostMapping("/zset/{key}")
    public String addToZSet(@PathVariable String key,
                            @RequestParam String value,
                            @RequestParam double score) {
        redisService.addToZSet(key, value, score);
        return "添加到有序集合成功: " + key + " - " + value + " (分數: " + score + ")";
    }

    @GetMapping("/zset/{key}")
    public Set<Object> getZSet(@PathVariable String key) {
        return redisService.getZSetRange(key, 0, -1);
    }

    @GetMapping("/zset/{key}/score/{value}")
    public Double getZSetScore(@PathVariable String key, @PathVariable String value) {
        return redisService.getZSetScore(key, value);
    }

    // ========== 通用操作測試 ==========

    @PostMapping("/expire/{key}")
    public String setExpire(@PathVariable String key, @RequestParam long seconds) {
        redisService.expire(key, seconds, TimeUnit.SECONDS);
        return "設置過期時間成功: " + key + " (過期時間: " + seconds + "秒)";
    }

    @GetMapping("/exists/{key}")
    public boolean exists(@PathVariable String key) {
        return redisService.hasKey(key);
    }

    @DeleteMapping("/{key}")
    public String delete(@PathVariable String key) {
        redisService.delete(key);
        return "刪除成功: " + key;
    }

    @GetMapping("/ttl/{key}")
    public Long getTTL(@PathVariable String key) {
        return redisService.getExpire(key);
    }

    // ========== 測試數據初始化 ==========

    @PostMapping("/init-test-data")
    public Map<String, String> initTestData() {
        Map<String, String> result = new HashMap<>();

        // 初始化字符串數據
        redisService.setString("test:string", "Hello Redis!");
        result.put("string", "test:string = Hello Redis!");

        // 初始化用戶數據
        User user = new User(1L, "張三", "zhangsan@example.com", 25);
        redisService.setObject("user:1", user);
        result.put("user", "user:1 = " + user.toString());

        // 初始化 Hash 數據
        redisService.setHash("user:profile:1", "name", "張三");
        redisService.setHash("user:profile:1", "age", "25");
        redisService.setHash("user:profile:1", "city", "台北");
        result.put("hash", "user:profile:1 包含 name, age, city");

        // 初始化 List 數據
        redisService.rightPushList("todo:list", "學習 Redis");
        redisService.rightPushList("todo:list", "寫代碼");
        redisService.rightPushList("todo:list", "測試應用");
        result.put("list", "todo:list 包含 3 個待辦事項");

        // 初始化 Set 數據
        redisService.addToSet("tags", "Java", "Spring", "Redis", "NoSQL");
        result.put("set", "tags 包含技術標籤");

        // 初始化 ZSet 數據
        redisService.addToZSet("leaderboard", "Alice", 100.0);
        redisService.addToZSet("leaderboard", "Bob", 85.0);
        redisService.addToZSet("leaderboard", "Charlie", 95.0);
        result.put("zset", "leaderboard 包含排行榜數據");

        return result;
    }
}
