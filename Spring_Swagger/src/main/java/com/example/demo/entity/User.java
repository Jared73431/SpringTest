package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 修正前：
 * <ul>
 * <li>User(username, name, email, age) 建構子的內容是空的，Service 用它建立的使用者欄位全部是 null</li>
 * <li>Lombok 的 @Data（JPA Entity 不適合：equals / hashCode 用到所有欄位、toString 可能觸發 lazy loading）</li>
 * <li>重複了一份驗證註解與 @Schema：Entity 直接當成 API 回應，資料表的變更會直接影響 API</li>
 * </ul>
 * 現在驗證放在 UserRequest，回應用 UserResponse；Entity 只負責對應資料表。
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private Integer age;

	protected User() {
		// JPA 需要無參數建構子
	}

	public User(String username, String name, String email, Integer age) {
		this.username = username;
		this.name = name;
		this.email = email;
		this.age = age;
	}

	public void update(String username, String name, String email, Integer age) {
		this.username = username;
		this.name = name;
		this.email = email;
		this.age = age;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public Integer getAge() {
		return age;
	}
}
