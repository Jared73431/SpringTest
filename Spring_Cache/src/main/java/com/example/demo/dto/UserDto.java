package com.example.demo.dto;

import com.example.demo.entity.User;

/**
 * 使用者的回應資料，也是<b>存進快取的型別</b>。
 * 不直接快取 JPA Entity：Entity 可能帶有 Hibernate 代理（lazy loading）、與資料表結構綁在一起，
 * 欄位一改就可能讓快取中的舊資料無法反序列化；DTO 是單純的資料，適合序列化存放。
 */
public record UserDto(Long id, String name, String email, Integer age) {

	public static UserDto from(User user) {
		return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getAge());
	}
}
