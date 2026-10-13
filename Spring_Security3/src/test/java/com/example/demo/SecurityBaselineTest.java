package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Baseline：鎖定重構前的行為（Spring Boot 2.7、WebSecurityConfigurerAdapter）。
 *
 * <p>
 * 設定看起來是從 token 驗證的樣板改來的，但 JWT filter、登入 API 都是註解；也沒有 formLogin、httpBasic、使用者。
 * 結果：需要登入的路徑，任何人都進不去。專案也沒有任何 Controller。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityBaselineTest {

	@Autowired
	private MockMvc mvc;

	// 沒有設定登入方式：沒有登入時不是 401 或導向登入頁，而是 403
	@Test
	void protectedPath_shouldBeForbidden_whenNotLoggedIn() throws Exception {
		mvc.perform(get("/anything")).andExpect(status().isForbidden());
	}

	// 沒有啟用 HTTP Basic，帶了帳密也一樣
	@Test
	void httpBasic_shouldNotBeAccepted() throws Exception {
		mvc.perform(get("/anything").with(httpBasic("user", "password"))).andExpect(status().isForbidden());
	}

	// 已登入（測試直接放入身分）才會通過 Security，但沒有 Controller：404
	@Test
	void protectedPath_shouldPassSecurity_whenAuthenticated() throws Exception {
		mvc.perform(get("/anything").with(user("someone"))).andExpect(status().isNotFound());
	}

	// GET *.html、*.css、*.js、/profile/** 不需要登入（沒有這些檔案：404）
	@Test
	void staticResources_shouldBePermitted() throws Exception {
		mvc.perform(get("/index.html")).andExpect(status().isNotFound());
		mvc.perform(get("/js/app.js")).andExpect(status().isNotFound());
		mvc.perform(get("/profile/avatar.png")).andExpect(status().isNotFound());
	}

	// /login 只允許「匿名」：沒有登入可以進入，已登入反而被拒絕
	@Test
	void login_shouldOnlyBeAllowedForAnonymousUsers() throws Exception {
		mvc.perform(get("/login")).andExpect(status().isNotFound());
		mvc.perform(get("/login").with(user("someone"))).andExpect(status().isForbidden());
	}

	// frameOptions().disable()：回應沒有 X-Frame-Options，頁面可以被其他網站用 iframe 嵌入（clickjacking）
	@Test
	void response_shouldNotHaveXFrameOptions() throws Exception {
		mvc.perform(get("/index.html")).andExpect(header().doesNotExist("X-Frame-Options"));
	}
}
