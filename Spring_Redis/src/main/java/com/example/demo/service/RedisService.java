package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ========== String 操作 ==========

    /**
     * 設置字符串值
     */
    public void setString(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 設置字符串值並設置過期時間
     */
    public void setString(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 獲取字符串值
     */
    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 設置對象
     */
    public void setObject(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 設置對象並設置過期時間
     */
    public void setObject(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 獲取對象
     */
    public Object getObject(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // ========== Hash 操作 ==========

    /**
     * 設置 Hash 值
     */
    public void setHash(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 獲取 Hash 值
     */
    public Object getHash(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 獲取所有 Hash 值
     */
    public Map<Object, Object> getAllHash(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 刪除 Hash 字段
     */
    public void deleteHash(String key, Object... fields) {
        redisTemplate.opsForHash().delete(key, fields);
    }

    // ========== List 操作 ==========

    /**
     * 從左邊添加到列表
     */
    public void leftPushList(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 從右邊添加到列表
     */
    public void rightPushList(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 從左邊彈出列表元素
     */
    public Object leftPopList(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 從右邊彈出列表元素
     */
    public Object rightPopList(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 獲取列表範圍內的元素
     */
    public List<Object> getListRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    // ========== Set 操作 ==========

    /**
     * 添加到集合
     */
    public void addToSet(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 獲取集合所有成員
     */
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 檢查是否是集合成員
     */
    public boolean isSetMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * 從集合中移除元素
     */
    public void removeFromSet(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    // ========== ZSet (Sorted Set) 操作 ==========

    /**
     * 添加到有序集合
     */
    public void addToZSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 獲取有序集合範圍內的元素
     */
    public Set<Object> getZSetRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 獲取有序集合元素的分數
     */
    public Double getZSetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    // ========== 通用操作 ==========

    /**
     * 設置過期時間
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 檢查鍵是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 刪除鍵
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 獲取鍵的剩餘過期時間
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }
}
