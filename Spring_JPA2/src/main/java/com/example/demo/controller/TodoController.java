package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Todo;
import com.example.demo.service.TodoService;

@RestController
public class TodoController {

    private final TodoService todoservice;

    public TodoController(TodoService todoservice) {
        this.todoservice = todoservice;
    }

    @GetMapping("/todo/{id}")
    public Todo getTodos(@PathVariable Integer id) {
        return todoservice.getTodos(id);
    }

    @PostMapping("/saveTodo")
    public void saveTodo (@RequestParam("task") String task, @RequestParam("Userid") Integer userid) {
        todoservice.saveTodo(task, userid);
    }
}
