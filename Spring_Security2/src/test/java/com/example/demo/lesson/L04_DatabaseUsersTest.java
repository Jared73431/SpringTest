package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 4 課：使用者存在資料庫。
 *
 * <pre>
 * users        username、password（BCrypt 雜湊值）、enabled
 * authorities  username、authority（ROLE_USER、ROLE_ADMIN）
 * </pre>
 *
 * 登入時 JdbcUserDetailsManager 用帳號查出這兩張表，PasswordEncoder 比對密碼。
 */
@SpringBootTest
@AutoConfigureMockMvc
class L04_DatabaseUsersTest extends PostgresContainerTestBase {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcUserDetailsManager userDetailsManager;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void deleteCreatedUser() {
		if (userDetailsManager.userExists("carol")) {
			userDetailsManager.deleteUser("carol");
		}
	}

	@Test
	void login_shouldWork_withThePasswordWhoseHashIsInTheDatabase() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("alice", "password"))).andExpect(status().isOk());
		mvc.perform(get("/api/me").with(httpBasic("alice", "wrong"))).andExpect(status().isUnauthorized());
	}

	// 資料庫裡只有雜湊值：就算資料庫外洩，也看不到原本的密碼
	@Test
	void database_shouldOnlyContainHashes() {
		assertThat(jdbc.queryForList("select password from users", String.class))
				.allSatisfy(password -> assertThat(password).startsWith("{bcrypt}$2a$10$").doesNotContain("password"));
	}

	// enabled = false：密碼正確也不能登入（停用帳號，不必刪除）
	@Test
	void disabledUser_shouldNotBeAbleToLogIn() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("bob", "password"))).andExpect(status().isUnauthorized());
	}

	// 資料庫存 ROLE_ADMIN；程式寫 hasRole("ADMIN")、roles("ADMIN")：Spring Security 會自動加上 ROLE_ 前綴。
	// FACTOR_PASSWORD 不在資料庫中：Spring Security 7 登入成功後自動加上，表示「這次是用密碼登入的」
	// （為多因素驗證設計，例如要求某些操作必須「密碼 + 一次性驗證碼」都通過；本專案不使用）
	@Test
	void roles_shouldBeStoredWithTheRolePrefix() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("admin", "password")))
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.authorities").value(
						org.hamcrest.Matchers.contains("FACTOR_PASSWORD", "ROLE_ADMIN", "ROLE_USER")));
	}

	// 新增使用者：存進資料庫前先用 PasswordEncoder 算出雜湊值
	@Test
	void newUser_shouldBeStoredWithAHashedPassword_andBeAbleToLogIn() throws Exception {
		userDetailsManager.createUser(User.withUsername("carol")
				.password(passwordEncoder.encode("secret"))
				.roles("USER")
				.build());

		assertThat(jdbc.queryForObject("select password from users where username = 'carol'", String.class))
				.startsWith("{bcrypt}");
		assertThat(jdbc.queryForObject("select authority from authorities where username = 'carol'", String.class))
				.isEqualTo("ROLE_USER");
		mvc.perform(get("/api/me").with(httpBasic("carol", "secret"))).andExpect(status().isOk());
	}
}
