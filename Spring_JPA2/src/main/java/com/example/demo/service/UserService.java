package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;

/**
 * 使用者的查詢與建立。
 * 欄位名稱 userDao 是早期 DAO 寫法留下的命名，實際上是 Spring Data JPA 的 Repository。
 */
@Service
public class UserService {

    private final UserRepository userDao;

    public UserService(UserRepository userDao) {
        this.userDao = userDao;
    }

    /** 取得使用者（其 todos 在序列化時才 LAZY 載入）。不存在 → ResourceNotFoundException（404）。 */
    public User getUser(Integer id) {
        return userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("使用者", id));
    }

    /** 建立只有名稱的使用者，其他欄位維持預設值。 */
    public User createUser(String name) {
        User user = new User();
        user.setName(name);
        return userDao.save(user);
    }

}
