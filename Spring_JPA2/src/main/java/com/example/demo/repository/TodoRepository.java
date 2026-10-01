package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Todo;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Integer> {

    // 依 Todo.user.id 查詢（Spring Data 會從方法名稱推導出 JPQL）
    List<Todo> findByUserId(Integer userId);
}
