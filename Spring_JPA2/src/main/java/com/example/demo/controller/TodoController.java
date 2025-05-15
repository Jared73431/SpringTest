package com.example.demo.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Todo;
import com.example.demo.service.TodoService;

@RestController
public class TodoController {

    @Autowired
    private TodoService todoservice;

    @GetMapping("/todo/{id}")
    public ResponseEntity getTodos (@PathVariable Integer id) {
        Optional<Todo> todos = todoservice.getTodos(id);
        return ResponseEntity.status(HttpStatus.OK).body(todos);
    }

    @PostMapping("/saveTodo")
    public void saveTodo (@RequestParam("task") String task, @RequestParam("Userid") Integer userid) {
        todoservice.saveTodo(task, userid);
    }
}
