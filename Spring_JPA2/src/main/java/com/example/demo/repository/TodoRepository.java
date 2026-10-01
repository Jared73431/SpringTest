package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Todo;

/**
 * 待辦事項的 Repository，主鍵型別為 Integer（與 Todo.id 一致）。
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Integer> {

    // 依 Todo.user.id 查詢（Spring Data 會從方法名稱推導出 JPQL）
    List<Todo> findByUserId(Integer userId);
}
