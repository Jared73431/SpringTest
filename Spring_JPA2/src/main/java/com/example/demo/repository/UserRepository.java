package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

/**
 * 使用者的 Repository。沒有自訂方法，JpaRepository 提供的 CRUD 已足夠；
 * Spring Data 會在啟動時自動產生實作，不需要撰寫 DAO 類別。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
