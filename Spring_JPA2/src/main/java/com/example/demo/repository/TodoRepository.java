package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Todo;

@Repository
public interface TodoRepository  extends JpaRepository<Todo, Integer> {
}
