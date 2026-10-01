package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userDao;

    public UserService(UserRepository userDao) {
        this.userDao = userDao;
    }

    public User getTodosByUserId(Integer id) {
        return userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("使用者", id));
    }

    public void saveUser(String name){
        User user = new User();
        user.setName(name);
        userDao.save(user);
    }

}
