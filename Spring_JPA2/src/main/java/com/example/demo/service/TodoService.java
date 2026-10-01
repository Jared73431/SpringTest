package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;

@Service
public class TodoService {

    private final TodoRepository todores;

    private final UserRepository userRepository;

    public TodoService(TodoRepository todores, UserRepository userRepository) {
        this.todores = todores;
        this.userRepository = userRepository;
    }

    public Todo getTodos(Integer id) {
        return todores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待辦事項", id));
    }

    public void saveTodo(String task, Integer userId){
        // 從資料庫取得User實體（處於持久狀態）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("使用者不存在: " + userId));

        Todo todo = new Todo();
        todo.setTask(task);
        todo.setUser(user);

        todores.save(todo);
    }
}
