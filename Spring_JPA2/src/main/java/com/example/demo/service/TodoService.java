package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
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

    public Todo getTodo(Integer id) {
        return todores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待辦事項", id));
    }

    public List<Todo> getTodosByUserId(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("使用者", userId);
        }
        return todores.findByUserId(userId);
    }

    public Todo createTodo(Integer userId, String task) {
        // 從資料庫取得User實體（處於持久狀態）；使用者在 URL 中，查不到回 404
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("使用者", userId));

        Todo todo = new Todo();
        todo.setTask(task);
        todo.setUser(user);

        return todores.save(todo);
    }
}
