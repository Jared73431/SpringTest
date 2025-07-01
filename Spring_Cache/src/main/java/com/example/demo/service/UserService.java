package com.example.demo.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public User createUser(User user) {
        System.out.println("創建新用戶: " + user.getName());
        return userRepository.save(user);
    }

    // @Cacheable：查詢時使用快取
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        System.out.println("從資料庫查詢用戶 ID: " + id);
        return userRepository.findById(id).orElse(null);
    }

    // @Cacheable：根據 email 查詢並快取
    @Cacheable(value = "users", key = "'email:' + #email")
    public User findByEmail(String email) {
        System.out.println("從資料庫查詢用戶 Email: " + email);
        return userRepository.findByEmail(email).orElse(null);
    }

    // @CachePut：更新資料時同步更新快取
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        System.out.println("更新用戶並刷新快取: " + user.getId());
        return userRepository.save(user);
    }

    // @CacheEvict：刪除資料時清除快取
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        System.out.println("刪除用戶並清除快取: " + id);
        userRepository.deleteById(id);
    }

    // @CacheEvict：清除所有用戶快取
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllCache() {
        System.out.println("清除所有用戶快取");
    }

    // 手動操作 Redis 快取的範例
    public void setCustomCache(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    public Object getCustomCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 查詢所有用戶並快取結果
    @Cacheable(value = "allUsers")
    public List<User> findAllUsers() {
        System.out.println("從資料庫查詢所有用戶");
        return userRepository.findAll();
    }
}
