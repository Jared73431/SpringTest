package com.example.demo.dto;

import com.example.demo.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 新增 / 修改使用者的 Request Body。修正前直接接收 JPA Entity，呼叫端甚至可以帶入 id。
 */
public record UserRequest(
		@NotBlank String name,
		@NotBlank @Email String email,
		@PositiveOrZero Integer age) {

	public void applyTo(User user) {
		user.setName(name);
		user.setEmail(email);
		user.setAge(age);
	}
}
