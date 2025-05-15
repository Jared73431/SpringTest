package com.example.demo.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("/users/{id}/todos")
    public ResponseEntity getTodosByUserId (@PathVariable Integer id) {
        Optional<User> todos = userService.getTodosByUserId(id);
        return ResponseEntity.status(HttpStatus.OK).body(todos);
    }

    @PostMapping("/saveUser")
    public void saveUserId (@RequestParam("name") String name) {
        userService.saveUser(name);
    }
}
