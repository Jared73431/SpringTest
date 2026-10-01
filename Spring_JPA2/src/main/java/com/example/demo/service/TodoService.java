package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;

/**
 * 待辦事項的查詢與建立；每筆 Todo 都屬於一位 User。
 * 未標註 @Transactional：每個方法最多一次寫入，由 Repository 預設的交易處理即可。
 */
@Service
public class TodoService {

    private final TodoRepository todores;

    private final UserRepository userRepository;

    public TodoService(TodoRepository todores, UserRepository userRepository) {
        this.todores = todores;
        this.userRepository = userRepository;
    }

    /** 取得單筆待辦事項。不存在 → ResourceNotFoundException（404）。 */
    public Todo getTodo(Integer id) {
        return todores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待辦事項", id));
    }

    /**
     * 取得使用者的所有待辦事項。使用者不存在 → ResourceNotFoundException（404）；沒有待辦事項 → 空 List。
     */
    public List<Todo> getTodosByUserId(Integer userId) {
        // 先確認使用者存在，才能區分「使用者不存在」與「使用者沒有待辦事項」
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("使用者", userId);
        }
        return todores.findByUserId(userId);
    }

    /** 為使用者建立待辦事項。使用者不存在 → ResourceNotFoundException（404）。 */
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
