package com.example.demo.dto;

import com.example.demo.entity.User;

/**
 * 使用者的回應資料，也是<b>存進快取的型別</b>。
 * 不直接快取 JPA Entity：Caffeine 直接保存物件本身（不序列化），快取 Entity 等於讓多個請求共用同一個物件，
 * 有人修改它（例如 setName）就會改到快取中的資料，也可能帶著 Hibernate 代理離開交易。
 * record 是不可變的，多個執行緒共用也安全。
 */
public record UserDto(Long id, String name, String email, Integer age) {

	public static UserDto from(User user) {
		return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getAge());
	}
}
