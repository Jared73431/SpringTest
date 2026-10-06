package com.example.demo.controller;

import java.net.URI;
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
import com.example.demo.dto.UserRequest;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

/**
 * 使用者 API（/api/users）。快取行為全部宣告在 UserService，Controller 不需要知道資料來自快取還是資料庫。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{id}")
	public UserDto findById(@PathVariable Long id) {
		return userService.findById(id).orElseThrow(() -> new UserNotFoundException(id));
	}

	@GetMapping("/email/{email}")
	public UserDto findByEmail(@PathVariable String email) {
		return userService.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
	}

	@GetMapping
	public List<UserDto> findAll() {
		return userService.findAll();
	}

	@PostMapping
	public ResponseEntity<UserDto> create(@Valid @RequestBody UserRequest request) {
		UserDto created = userService.create(request);
		return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public UserDto update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
		return userService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	/** 清除三個使用者快取（下一次查詢會重新讀取資料庫） */
	@DeleteMapping("/cache")
	public ResponseEntity<Void> clearCaches() {
		userService.clearAllCaches();
		return ResponseEntity.noContent().build();
	}
}
