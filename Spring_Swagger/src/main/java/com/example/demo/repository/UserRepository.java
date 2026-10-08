package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	// 修改時排除自己：沿用原本的帳號 / Email 不算重複
	boolean existsByUsernameAndIdNot(String username, Long id);

	boolean existsByEmailAndIdNot(String email, Long id);
}
