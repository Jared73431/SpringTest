package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用者 Entity（資料表 users）。
 * 不直接放進快取，也不直接當作 API 回應：對外與快取一律使用 UserDto。
 * 只用 @Getter / @Setter，不用 @Data：@Data 產生的 equals / hashCode 使用所有欄位，Entity 放入 Set 或 id 改變時容易出錯。
 * 快取存放的是不可變的 UserDto，不是這個 Entity。
 */
@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String email;

	private Integer age;
}
