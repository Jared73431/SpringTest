package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(CreateUserRequest request) {
        User user = new User(
                request.getUsername(),
                request.getName(),
                request.getEmail(),
                request.getAge()
        );
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, CreateUserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.getUsername());
                    user.setName(request.getName());
                    user.setEmail(request.getEmail());
                    user.setAge(request.getAge());
                    return userRepository.save(user);
                });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
