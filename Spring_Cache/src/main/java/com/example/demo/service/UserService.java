package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserDto createUser(User user) {
		log.info("創建新用戶: {}", user.getName());
		return UserDto.from(userRepository.save(user));
	}

	// @Cacheable：查詢時使用快取
	@Cacheable(value = "users", key = "#id")
	public UserDto findById(Long id) {
		log.info("從資料庫查詢用戶 ID: {}", id);
		return userRepository.findById(id).map(UserDto::from).orElse(null);
	}

	// @Cacheable：根據 email 查詢並快取
	@Cacheable(value = "users", key = "'email:' + #email")
	public UserDto findByEmail(String email) {
		log.info("從資料庫查詢用戶 Email: {}", email);
		return userRepository.findByEmail(email).map(UserDto::from).orElse(null);
	}

	// @CachePut：更新資料時同步更新快取
	@CachePut(value = "users", key = "#user.id")
	public UserDto updateUser(User user) {
		log.info("更新用戶並刷新快取: {}", user.getId());
		return UserDto.from(userRepository.save(user));
	}

	// @CacheEvict：刪除資料時清除快取
	@CacheEvict(value = "users", key = "#id")
	public void deleteUser(Long id) {
		log.info("刪除用戶並清除快取: {}", id);
		userRepository.deleteById(id);
	}

	// @CacheEvict：清除所有用戶快取
	@CacheEvict(value = "users", allEntries = true)
	public void clearAllCache() {
		log.info("清除所有用戶快取");
	}

	// 查詢所有用戶並快取結果
	@Cacheable(value = "allUsers")
	public List<UserDto> findAllUsers() {
		log.info("從資料庫查詢所有用戶");
		return userRepository.findAll().stream().map(UserDto::from).toList();
	}
}
