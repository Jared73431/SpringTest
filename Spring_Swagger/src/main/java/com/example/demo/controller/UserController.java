package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用戶管理", description = "用戶相關的 CRUD 操作")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(
            summary = "獲取所有用戶",
            description = "返回系統中所有用戶的列表"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功獲取用戶列表",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("獲取用戶列表成功", users));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "根據ID獲取用戶",
            description = "通過用戶ID獲取特定用戶的詳細信息"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功獲取用戶信息"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "用戶不存在"
            )
    })
    public ResponseEntity<ApiResponse<User>> getUserById(
            @Parameter(description = "用戶ID", example = "1", required = true)
            @PathVariable Long id) {

        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success("獲取用戶成功", user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
            summary = "創建新用戶",
            description = "創建一個新的用戶記錄"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "用戶創建成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "請求參數無效"
            )
    })
    public ResponseEntity<ApiResponse<User>> createUser(
            @Parameter(description = "用戶創建請求", required = true)
            @Valid @RequestBody CreateUserRequest request) {

        User createdUser = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("用戶創建成功", createdUser));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "更新用戶信息",
            description = "根據用戶ID更新用戶的詳細信息"
    )
    public ResponseEntity<ApiResponse<User>> updateUser(
            @Parameter(description = "用戶ID", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "用戶更新請求", required = true)
            @Valid @RequestBody CreateUserRequest request) {

        return userService.updateUser(id, request)
                .map(user -> ResponseEntity.ok(ApiResponse.success("用戶更新成功", user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "刪除用戶",
            description = "根據用戶ID刪除指定用戶"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "用戶刪除成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "用戶不存在"
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用戶ID", example = "1", required = true)
            @PathVariable Long id) {

        if (userService.deleteUser(id)) {
            return ResponseEntity.ok(ApiResponse.success("用戶刪除成功", null));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
