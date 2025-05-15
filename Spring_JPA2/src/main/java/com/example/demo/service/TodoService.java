package com.example.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;

@Service
public class TodoService {

    @Autowired
    private TodoRepository todores;

    @Autowired
    private UserRepository userRepository;

    public Optional<Todo> getTodos(Integer id) {
        Optional<Todo> data = todores.findById(id);
        return data;
    }

    public void saveTodo(String task, Integer userId){
        // 從資料庫獲取User實體（處於持久狀態）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到用戶 ID: " + userId));

        Todo todo = new Todo();
        todo.setTask(task);
        todo.setUser(user);

        todores.save(todo);
    }
}
