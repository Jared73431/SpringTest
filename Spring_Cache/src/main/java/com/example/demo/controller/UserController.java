package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
		UserDto user = userService.findById(id);
		return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
	}

	@GetMapping("/email/{email}")
	public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
		UserDto user = userService.findByEmail(email);
		return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
	}

	@GetMapping
	public List<UserDto> getAllUsers() {
		return userService.findAllUsers();
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody User user) {
		user.setId(id);
		return ResponseEntity.ok(userService.updateUser(user));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/cache/clear")
	public ResponseEntity<String> clearCache() {
		userService.clearAllCache();
		return ResponseEntity.ok("快取已清除");
	}

	// 修正前的 POST / GET /api/users/cache/{key}（手動讀寫任意 Redis key）已移除：
	// 任何人都能寫入任意 key，包括快取使用的 key（例如 users::1），可以竄改快取內容（快取污染）。
	// 手動操作 Redis 的示範請見 Spring_Redis 模組。

	@PostMapping
	public ResponseEntity<UserDto> createUser(@RequestBody User user) {
		return ResponseEntity.ok(userService.createUser(user));
	}
}
