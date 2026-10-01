package com.example.demo.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userDao;

    public UserService(UserRepository userDao) {
        this.userDao = userDao;
    }

    public Optional<User> getTodosByUserId(Integer id) {
        Optional<User> data = userDao.findById(id);
        return data;
    }

    public void saveUser(String name){
        User user = new User();
        user.setName(name);
        userDao.save(user);
    }

}
