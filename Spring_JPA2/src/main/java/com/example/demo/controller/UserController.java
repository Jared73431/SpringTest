package com.example.demo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.request.CreateTodoRequest;
import com.example.demo.request.CreateUserRequest;
import com.example.demo.service.TodoService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    private final TodoService todoService;

    public UserController(UserService userService, TodoService todoService) {
        this.userService = userService;
        this.todoService = todoService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.name());
        return ResponseEntity.created(URI.create("/api/users/" + user.getId())).body(user);
    }

    // 回傳使用者與其所有 Todo（一對多）
    @GetMapping("/{id}")
    public User getUser(@PathVariable Integer id) {
        return userService.getUser(id);
    }

    @GetMapping("/{id}/todos")
    public List<Todo> getUserTodos(@PathVariable Integer id) {
        return todoService.getTodosByUserId(id);
    }

    @PostMapping("/{id}/todos")
    public ResponseEntity<Todo> createTodo(@PathVariable Integer id, @Valid @RequestBody CreateTodoRequest request) {
        Todo todo = todoService.createTodo(id, request.task());
        return ResponseEntity.created(URI.create("/api/todos/" + todo.getId())).body(todo);
    }
}
