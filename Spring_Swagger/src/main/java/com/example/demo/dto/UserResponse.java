package com.example.demo.dto;

import com.example.demo.entity.User;

public record UserResponse(Long id, String username, String name, String email, Integer age) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getAge());
	}
}
