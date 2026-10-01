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

/**
 * 使用者與其待辦事項的 REST API；Todo 屬於使用者，所以建立與列表放在 /api/users/{id}/todos 子資源下。
 * 已知限制：目前直接回傳 User / Todo Entity（未轉成 Response DTO），Entity 的所有欄位都會被序列化輸出。
 * 不在這裡 try/catch，例外由 GlobalExceptionHandler 統一處理。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    private final TodoService todoService;

    public UserController(UserService userService, TodoService todoService) {
        this.userService = userService;
        this.todoService = todoService;
    }

    /** POST /api/users：建立使用者。201 + Location；驗證失敗 → 400。 */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.name());
        return ResponseEntity.created(URI.create("/api/users/" + user.getId())).body(user);
    }

    /**
     * GET /api/users/{id}：回傳使用者與其所有 Todo（一對多）。200；使用者不存在 → 404。
     * todos 是 LAZY 關聯，在 JSON 序列化時才載入（依賴 Open Session In View）。
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Integer id) {
        return userService.getUser(id);
    }

    /** GET /api/users/{id}/todos：取得使用者的所有 Todo。200（可能為空陣列）；使用者不存在 → 404。 */
    @GetMapping("/{id}/todos")
    public List<Todo> getUserTodos(@PathVariable Integer id) {
        return todoService.getTodosByUserId(id);
    }

    /**
     * POST /api/users/{id}/todos：為使用者建立 Todo。
     * 201 + Location（指向 /api/todos/{todoId}，Todo 有自己的查詢端點）；驗證失敗 → 400；使用者不存在 → 404。
     */
    @PostMapping("/{id}/todos")
    public ResponseEntity<Todo> createTodo(@PathVariable Integer id, @Valid @RequestBody CreateTodoRequest request) {
        Todo todo = todoService.createTodo(id, request.task());
        return ResponseEntity.created(URI.create("/api/todos/" + todo.getId())).body(todo);
    }
}
