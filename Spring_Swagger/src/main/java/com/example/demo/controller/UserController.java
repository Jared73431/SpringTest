package com.example.demo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 修正前：@RequestBody import 成 Swagger 的 io.swagger.v3.oas.annotations.parameters.RequestBody（只用來產生文件），
 * Spring 不認得它，把參數當成 query 參數綁定，JSON body 完全被忽略。兩個註解同名，IDE 自動 import 時很容易選錯。
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "使用者管理", description = "使用者的新增、查詢、修改與刪除")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@Operation(summary = "取得所有使用者")
	public List<UserResponse> getAllUsers() {
		return userService.getAllUsers();
	}

	@GetMapping("/{id}")
	@Operation(summary = "依 ID 取得使用者")
	public UserResponse getUserById(@PathVariable Long id) {
		return userService.getUserById(id);
	}

	@PostMapping
	@Operation(summary = "新增使用者")
	// 回傳 ResponseEntity 時 springdoc 推導不出狀態碼（會寫成 200），要明確標註實際的 201
	@ApiResponse(responseCode = "201", description = "已建立，Location 標頭是新資源的網址")
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
		UserResponse created = userService.createUser(request);
		return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "修改使用者")
	public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
		return userService.updateUser(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "刪除使用者")
	public void deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
	}
}
